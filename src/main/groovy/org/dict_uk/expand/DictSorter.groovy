package org.dict_uk.expand

import java.util.Collection;
import java.util.List
import java.util.Map;
import java.util.regex.*

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovyx.gpars.ParallelEnhancer
import static groovyx.gpars.GParsPool.withPool

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import org.dict_uk.common.DicEntry
import org.dict_uk.common.UkDictComparator


class DictSorter {
	static Logger log = LogManager.getFormatterLogger(DictSorter.class);

	static final String DERIV_PADDING="  "

	static final Map<String,String> GEN_ORDER = [
		"m": "0",
		"f": "1",
		"n": "3",
		"s": "4",
		"p": "5"
	]

	static final Map<String,String> VIDM_ORDER = [
		"v_naz": "10",
		"v_rod": "20",
		"v_dav": "30",
		"v_zna": "40",
		"v_oru": "50",
		"v_mis": "60",
		"v_kly": "70"
	]

	static final Map<String,String> vb_tag_key_map = [
		"inf": 1,
		"inz": 2,
		//		"inf:coll": 3,
		//		"inz:coll": 4,
		"impr": 5,
		"pres": 6,
		"futr": 7,
		"past": 8,
		"impers": 9
	]

	static final Pattern re_verb_tense = Pattern.compile("(in[fz]|impr|pres|futr|past|impers)")
	
	//	static final Pattern re_person_name_key_tag = Pattern.compile("^([^:]+(?::anim|:inanim|:perf|:imperf)?)(.*?)(:lname|:fname|:patr)")
	static final Pattern re_person_name_key_tag = Pattern.compile("^(noun:anim)(.*?)(:lname|:fname|:patr)")

	static final Pattern re_xv_sub = Pattern.compile("^([^:]+)(.*)(:x.[1-9])")

	static final Pattern GEN_RE = Pattern.compile(/:([mfnsp])(:|$)/)
	static final Pattern VIDM_RE = Pattern.compile(/:(v_...)((:alt|:rare|:coll)*)/) // |:short

	//	@CompileStatic
	@TypeChecked
	def tag_sort_key(String tags, String word) {
		if( tags.contains(":v-u") ) {
			tags = tags.replace(":v-u", "")
		}

		if( tags.contains("v_") ) {
			def vidm_match = VIDM_RE.matcher(tags)

			if( vidm_match.find() ) {
				String vidm = vidm_match.group(1)
				String vidm_order = VIDM_ORDER[vidm]

				// moving alt, rare, coll... after standard forms
				if( vidm_match.group(3) ) {
					vidm_order = vidm_order.replace("0", vidm_match.group(2).count(":").toString())
				}

				tags = VIDM_RE.matcher(tags).replaceFirst(":"+vidm_order)
			}
		}

		if( tags.startsWith("adj:") ) {
			if( ! tags.contains(":comp") && ! tags.contains(":supe") ) {
				// make sure :short without :combp sorts ok with adjective base that has compb
				if( tags.contains(":short") ) {
					tags = tags.replace(":short", "").replace("adj:", "adj:compc")
				}
				else {
					tags = tags.replace("adj:", "adj:compb:")
				}
			}
		}
		else if( tags.startsWith("noun") ) {
			if (tags.contains("name") || tags.contains("patr") ) {
				tags = re_person_name_key_tag.matcher(tags).replaceAll('$1$3$2')
				if ( tags.contains("lname") || tags.contains("patr") && tags.contains(":f:") ) {// && ! ":nv" in tags:    // to put Адамишин :f: after Адамишини :p) {
					tags = tags.replace(":f:", ":9:")
				}
			}

			if( tags.contains(":nv") ) {
				tags = tags.replace(":nv", "").replace("anim", "anim:nv")
			}

			if( tags.contains(":np") || tags.contains(":ns") ) {
				tags = tags.replace(":np", "").replace(":ns", "")
			}
		}
		else if( tags.startsWith("verb") ) {
			def verb_match = re_verb_tense.matcher(tags)
			if( verb_match.find() ) {
				def tg = verb_match.group(0)
				def order = vb_tag_key_map[tg]
				if( tags.contains(":coll") ) {
					order += 1
				}
				tags = tags.replace(tg, "_"+order)
			}
			else {
				log.error("no verb match: " + tags)
			}
		}

		def gen_match = GEN_RE.matcher(tags)

		if( gen_match.find() ) {
			def gen = gen_match.group(1)
			tags = GEN_RE.matcher(tags).replaceFirst(":"+GEN_ORDER[gen]+'$2')
		}

		if( tags.contains(":x") ) {
			tags = re_xv_sub.matcher(tags).replaceAll('$1$3$2')
		}

		return tags
	}


	def derived_plural(key, prev_key) {
		return key.contains("name") && key.contains(":p:") &&
				prev_key =~ ":[mf]:" && prev_key.replaceFirst(":[mf]:", ":p:") == key
	}


	static final Pattern re_key = Pattern.compile(" ([^ ]+ [^:]+(?::rev)?(?::(?:anim|inanim|perf|imperf))?)")
	static final Pattern re_key_name = Pattern.compile(" ([^ ]+ noun:anim:[fmnp]:).*?(lname|fname|patr)")

	@TypeChecked
	List<String> indent_lines(List<String> lines) {
		List<String> out_lines = []
		String prev_key = ""

		for( line in lines ){
			String[] parts = line.split()
			String word = parts[0]
			String lemma = parts[1]
			String tags = parts[2]
			String key

			try {
				if( tags.contains("name") || tags.contains("patr") ) {
					def key_rr = re_key_name.matcher(line)
					key_rr.find()
					key = key_rr.group(1) + key_rr.group(2)
				}
				else {
					def key_rr = re_key.matcher(line)
					key_rr.find()
					key = key_rr.group(1)
				}

				if( line.contains(":x") ) {
					int x_idx = line.indexOf(":x")
					key += line[x_idx..<x_idx+4]
				}
			}
			catch(Exception e) {
				throw new RuntimeException("Failed to find tag key in " + line, e)
			}

			if( line.contains(":nv") ) {
				key += ":nv"
			}

			if( key != prev_key && ! derived_plural(key, prev_key) ) {
				prev_key = key
				line = word + " " + tags
			} else {
				line = DERIV_PADDING + word + " " + tags
			}

			out_lines.add(line)
		}

		return out_lines
	}

	def line_key(txt) {
		try {
			def (word, lemma, tags) = txt.split()

			if( tags.contains("verb:rev") && tags.contains(":inf") \
					&& (word.endsWith("сь") || word.endsWith("ться")) ) {
				tags = tags.replace("inf", "inz")
			}

			return UkDictComparator.getSortKey(lemma) + "_" + tag_sort_key(tags, word) + "_" + UkDictComparator.getSortKey(word)
		}
		catch(Exception e) {
			throw new Exception("failed on " + txt, e)
		}
	}

	List<String> sort_all_lines(Collection<String> all_lines) {
		ParallelEnhancer.enhanceInstance(all_lines)
		def entries = all_lines.collectParallel {
			[(line_key(it)): it]
		}

		def map = entries.collectEntries {
			it
		}

		map = map.sort()

		return map.values().toList()
	}

	static def quickUkSort(collection) {
		ParallelEnhancer.enhanceInstance(collection)

		def entries = collection.collectParallel {
			[ (UkDictComparator.getSortKey(it)): it]
		}

		def map = entries.collectEntries {
			it
		}

		return map.sort().values()
	}

}

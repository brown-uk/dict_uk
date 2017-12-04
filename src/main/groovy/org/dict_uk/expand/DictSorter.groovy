package org.dict_uk.expand

import java.util.regex.Pattern

import org.dict_uk.common.DicEntry
import org.dict_uk.common.UkDictComparator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.CompileStatic
import groovyx.gpars.ParallelEnhancer


class DictSorter {
	static Logger log = LoggerFactory.getLogger(DictSorter.class);

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

	static final Map<String, Integer> vb_tag_key_map = [
		"inf": 10,
		"inz": 20,
		//		"inf:coll": 3,
		//		"inz:coll": 4,
		"impr": 50,
		"pres": 60,
		"futr": 70,
		"past": 80,
		"impers": 90
	]

	static final Pattern re_verb_tense = Pattern.compile("(in[fz]|impr|pres|futr|past|impers)")

	//	static final Pattern re_person_name_key_tag = Pattern.compile("^([^:]+(?::anim|:inanim|:perf|:imperf)?)(.*?)(:lname|:fname|:patr)")
	static final Pattern re_person_name_key_tag = Pattern.compile("^(noun:anim)(.*?)(:[lfp]name)")

	static final Pattern re_xv_sub = Pattern.compile("^([^:]+)(.*)(:x.[1-9])")
	static final Pattern re_pron_sub = Pattern.compile("^([^:]+)(.*)(:&pron:[^:]+)")

	static final Pattern GEN_RE = Pattern.compile(/:([mfnsp])(:|$)/)
	static final Pattern VIDM_RE = Pattern.compile(/:(v_...)((:alt|:rare|:coll)*)/) // |:short

	@CompileStatic
	String tag_sort_key(String tags, String word) {
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
			if( ! tags.contains(":comp") ) {
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
			if( tags.contains("name") ) {
				tags = re_person_name_key_tag.matcher(tags).replaceAll('$1$3$2')
				if ( (tags.contains("lname") || tags.contains("pname"))
						&& tags.contains(":f:") ) {// && ! ":nv" in tags:    // to put Адамишин :f: after Адамишини :p) {
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
				tags = tags.replace(":1", ":10")
				if( tags.contains(":subst") ) {
				    tags = tags.replace(":p:10", ":p:11")
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
		if( tags.contains(":&pron:") ) {
			tags = re_pron_sub.matcher(tags).replaceAll('$1$3$2')
		}

		return tags
	}


	@CompileStatic
	boolean derived_plural(String key, String prev_key) {
		return key.contains("name") \
				&& key.contains(":p:") \
				&& prev_key =~ ":[mf]:" \
				&& prev_key.replaceFirst(":[mf]:", ":p:") == key
	}


	static final Pattern re_key = Pattern.compile("^[^:]+(?::rev)?(?::(?:anim|inanim|perf|imperf))?")
	static final Pattern re_key_pron = Pattern.compile(":&pron:[^:]+")
	static final Pattern re_key_name = Pattern.compile("^(noun:anim:[fmnp]:).*?([flp]name)")

	@CompileStatic
	List<String> indent_lines(List<DicEntry> lines) {
		List<String> out_lines = []
		String prev_key = ""

		for(DicEntry line in lines ){
			String word = line.word
			String lemma = line.lemma
			String tags = line.tagStr
			String key

			try {
				if( tags.contains("name") ) {
					def key_rr = re_key_name.matcher(line.tagStr)
					key_rr.find()
					key = lemma + " " + key_rr.group(1) + key_rr.group(2)
				}
				else {
					def key_rr = re_key.matcher(line.tagStr)
					key_rr.find()
					key = lemma + " " + key_rr.group(0)
					
					if( tags.contains(":&pron:") ) {
					    def pron_rr = re_key_pron.matcher(line.tagStr)
					    pron_rr.find()
					    key += pron_rr.group(0)
					}
					
				}

				int x_idx = line.tagStr.indexOf(":x")
				if( x_idx != -1 ) {
					key += line.tagStr[x_idx..<x_idx+4]
				}
			}
			catch(Exception e) {
				throw new RuntimeException("Failed to find tag key in " + line, e)
			}

			if( line.tagStr.contains(":nv") ) {
				key += ":nv"
			}

			String outLine
			if( key != prev_key && ! derived_plural(key, prev_key) ) {
				prev_key = key
				outLine = word + " " + tags
			} else {
				outLine = DERIV_PADDING + word + " " + tags
			}

			if( line.comment ) {
				outLine += "    # " + line.comment
			}
			
			out_lines.add(outLine)
		}

		return out_lines
	}

	@CompileStatic
	String line_key(DicEntry entry) {
		try {
			String tags = entry.tagStr

			if( entry.tagStr.startsWith("verb:rev") && entry.tagStr.contains(":inf") \
					&& (entry.word.endsWith("сь") || entry.word.endsWith("ться")) ) {
				tags = tags.replace("inf", "inz")
			}

			return UkDictComparator.getSortKey(entry.lemma) + "_" + tag_sort_key(tags, entry.word) + "_" + UkDictComparator.getSortKey(entry.word)
		}
		catch(Exception e) {
			throw new Exception("Failed to find line key for " + entry, e)
		}
	}

	List<DicEntry> sortEntries(Collection<DicEntry> allEntries) {
		ParallelEnhancer.enhanceInstance(allEntries)
		
		def entryMap = allEntries.collectParallel { entry ->
			[(line_key(entry)): entry]
		}

		def map = entryMap.collectEntries {
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

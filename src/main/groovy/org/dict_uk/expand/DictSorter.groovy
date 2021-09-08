package org.dict_uk.expand

import java.util.regex.Pattern
import java.util.stream.Collectors

import org.dict_uk.common.DicEntry
import org.dict_uk.common.UkDictComparator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.CompileStatic


class DictSorter {
	static Logger log = LoggerFactory.getLogger(DictSorter.class);

	static final String DERIV_PADDING="  "

	static final Map<String,String> GEN_ORDER = [
		"m": "00",
		"f": "10",
		"n": "30",
		"s": "40",
		"p": "50"
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

	static final Map<String, String> vb_tag_key_map = [
		"inf": "10",
		"inz": "20",
		//		"inf:coll": 3,
		//		"inz:coll": 4,
		"impr": "50",
		"pres": "60",
		"futr": "70",
		"past": "80",
		"impers": "90"
	]

	static final Pattern re_verb_tense = Pattern.compile("(in[fz]|impr|pres|futr|past|impers)")

	static final Pattern re_person_name_key_tag = Pattern.compile("^(noun:anim)(.*?)(:[lfp]name)")

	static final Pattern re_xv_sub = Pattern.compile("^([^:]+)(.*)(:x.[1-9])")
	static final Pattern re_pron_sub = Pattern.compile("^([^:]+)(.*)(:&pron:[^:]+)")

	static final Pattern LOWERING_TAGS_RE = Pattern.compile(/:(alt|rare|arch|vulg|coll|subst|bad|var|short|long|ua_....)/)
	static final Pattern GEN_RE = Pattern.compile(/:([mfnsp])(:|$)/)
	static final Pattern VIDM_RE = Pattern.compile(/:(v_...)/)

	@CompileStatic
	String tag_sort_key(String tags, String word) {
//		if( tags.contains(":v-u") ) {
//			tags = tags.replace(":v-u", "")
//		}
		
		if( tags.indexOf('&') > 0 ) {
			tags = tags.replaceAll(/:&(insert|predic)/, '')
		}

		int offset = 0

		// moving alt, rare, coll... after standard forms
		def loweringMatch = LOWERING_TAGS_RE.matcher(tags)
		while( loweringMatch.find() ) {
			offset += 1 //loweringMatch.groupCount().toString()
		}
		if( offset ) {
			tags = loweringMatch.replaceAll('')
		}

		boolean hasGender = false
		boolean hasVidm = false
		
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
			
			if( tags.contains(":ranim") ) {
				tags = tags.replace(':ranim', '')
			}
			else if( tags.contains(":rinanim") ) {
				tags = tags.replace(':rinanim', '')
				offset += 1
			}

			hasGender = true
			hasVidm = true
		}
		else if( tags.startsWith("noun") ) {
			if( tags.startsWith("noun:anim") && tags.contains("name") ) {
				tags = re_person_name_key_tag.matcher(tags).replaceAll('$1$3$2')
				// move feminine last name to its own lemma
				// to put Адамишин :f: after Адамишини :p)
				if ( (tags.contains("lname") || tags.contains("pname"))
						&& tags.contains(":f:") ) {
						// && ! tags.contains(":nv") {
					tags = tags.replace(":f:", ":90:")
				}
			}

			// sort nv and non-nv separately (e.g. авто)
			if( tags.contains(":nv") ) {
				tags = tags.replace(":nv", "").replace("anim", "anim:nv")
			}

			if( tags.contains(":np") || tags.contains(":ns") ) {
				tags = tags.replace(":np", "").replace(":ns", "")
			}

			hasGender = true
			hasVidm = true
		}
		else if( tags.startsWith("verb") ) {
			def verb_match = re_verb_tense.matcher(tags)
			if( verb_match.find() ) {
				def tg = verb_match.group(0)
				def order = vb_tag_key_map[tg]
				
				tags = verb_match.replaceFirst(order)
				tags += ':' + offset
			}
			else {
				log.error("no verb match: " + tags)
			}
			hasGender = true
		}
		else if( tags.startsWith("numr") ) {
			hasGender = true
			hasVidm = true
		}
		else if( tags.startsWith("adv:compc") ) {
			tags += ':' + offset
		}

		if( hasGender ) {
			def gen_match = GEN_RE.matcher(tags)

			if( gen_match.find() ) {
				def gen = gen_match.group(1)
				def order = GEN_ORDER[gen]
				tags = GEN_RE.matcher(tags).replaceFirst(":"+order+'$2')
			}
		}

		if( hasVidm /*tags.contains("v_")*/ ) {
			def vidm_match = VIDM_RE.matcher(tags)

			if( vidm_match.find() ) {
				String vidm = vidm_match.group(1)
				String vidm_order = VIDM_ORDER[vidm]
				vidm_order = vidm_order[0..<-1] + offset

				tags = vidm_match.replaceFirst(vidm_order)
			}
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


	static final Pattern re_key = Pattern.compile("^[^:]+(?::rev)?(?::(?:anim|inanim|perf|imperf|coord|subord))?")
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
			StringBuilder key = new StringBuilder(32)

			try {
				if( tags.contains("name") ) {
					def key_rr = re_key_name.matcher(line.tagStr)
					key_rr.find()
					key.append(lemma).append(" ").append(key_rr.group(1)).append(key_rr.group(2))
				}
				else {
					def key_rr = re_key.matcher(line.tagStr)
					key_rr.find()
					key.append(lemma).append(" ").append(key_rr.group(0))
					
					if( tags.contains(":&pron:") ) {
					    def pron_rr = re_key_pron.matcher(line.tagStr)
					    pron_rr.find()
					    key.append(pron_rr.group(0))
					}
					
				}

				int x_idx = line.tagStr.indexOf(":x")
				if( x_idx != -1 ) {
					key.append(line.tagStr[x_idx..<x_idx+4])
				}
			}
			catch(Exception e) {
				throw new RuntimeException("Failed to find tag key in " + line, e)
			}

			if( line.tagStr.contains(":nv") ) {
				key.append(":nv")
			}

			String keyStr = key.toString()
			String outLine
			if( keyStr != prev_key && ! derived_plural(keyStr, prev_key) ) {
				prev_key = keyStr
				outLine = "$word $tags"
			} else {
				outLine = DERIV_PADDING + "$word $tags"
			}

			if( line.comment ) {
				outLine += "    # ${line.comment}"
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

	@CompileStatic
	List<DicEntry> sortEntries(Collection<DicEntry> allEntries) {
		List<DicEntry> list = allEntries.parallelStream()
			.map { (Map.Entry)new MapEntry(line_key(it), it) }
			.sorted(Map.Entry.comparingByKey())
			.map { Map.Entry it -> it.getValue() }
			.distinct()
			.collect(Collectors.toList())

		return list
	}

	@CompileStatic
	static List<String> quickUkSort(Collection<String> collection) {

		List<String> list = collection.parallelStream()
			.map { (Map.Entry)new MapEntry(UkDictComparator.getSortKey(it), it) }
			.sorted(Map.Entry.comparingByKey())
			.map { Map.Entry it -> it.getValue() }
			.collect(Collectors.toList()) 
			
		return list
	}

}

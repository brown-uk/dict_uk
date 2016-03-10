package org.dict_uk.expand

import java.util.List;
import java.util.regex.*

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovyx.gpars.ParallelEnhancer
import static groovyx.gpars.GParsPool.withPool

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import org.dict_uk.common.DicEntry
import org.dict_uk.common.UkDictComparator


class Util {
	static Logger log = LogManager.getFormatterLogger(BaseTags.class);

	static {
		Locale.setDefault(new Locale("uk" , "UA"))
		String.metaClass.isCase = { delegate.contains(it) }	// pythonize
		assert "b" in "abc"
	}

	final String DERIV_PADDING="  "


	Map<String, Pattern> regex_map = [:]

	def re_sub(regex, repl, txt) {
		//	dbg(regex, repl, txt)
		if( ! (regex in regex_map) ) {
			regex_map[regex] = Pattern.compile(regex)
		}
		return regex_map[regex].matcher(txt).replaceAll(repl)
	}


	def tail_tag(line, tags) {
		for( tag in tags ){
			//        tag = ":" + tag
			if( tag in line && ! line.endsWith(tag) ) {
				line = line.replace(tag, "") + tag
			}
		}
		return line
	}

	def bacteria(allAffixFlags) {
		return ">>" in allAffixFlags
	}

	def istota(allAffixFlags) {
		return "patr" in allAffixFlags || "<" in allAffixFlags
	}

	def person(allAffixFlags) {
		return "patr" in allAffixFlags || ("<" in allAffixFlags && ! (">" in allAffixFlags) )
	}

	@TypeChecked
	def firstname(String word, String allAffixFlags) {
		return ("patr" in allAffixFlags || ("<" in allAffixFlags && ! (">" in allAffixFlags))) && ! ("+" in allAffixFlags) \
        && word.charAt(0).isUpperCase() && ! word.charAt(1).isUpperCase()
		//and affixFlag != "p" \
	}

	def DUAL_LAST_NAME_PATTERN = ~ ".*(о|ич|ук|юк|як|аш|яш|сь|ун|ин|сон) "
	def dual_last_name_ending(line) {
		return "+d" in line || DUAL_LAST_NAME_PATTERN.matcher(line)
	}

	// dictionary-related methods


	static final Map POS_MAP = [
		"adj": "adj",
		"numr": "numr",
		"n": "noun",
		// "vi": "verb:imperf",
		// "vp": "verb:perf",
		"vr": "verb",
		"v": "verb",
		"<": "noun"
	]


	def POS_PATTERN = ~ /[\._].*/
	def get_pos(posFlag, modifiers) {
		posFlag = POS_PATTERN.matcher(posFlag).replaceAll("")
		//    logger.info("\t\t"  + posFlag + ", " + modifiers)

		def pos
		if( false && "pos" in modifiers ) {
			pos = modifiers["pos"]
		}
		else {
			if( posFlag in POS_MAP ) {
				posFlag = posFlag
			}
			else if( posFlag.size() >=3 && posFlag[0..<3] in POS_MAP ) {
				posFlag = posFlag[0..<3]
			}
			else if( posFlag[0..<2] in POS_MAP ) {
				posFlag = posFlag[0..<2]
			}
			else {
				posFlag = posFlag[0]
			}

			pos = POS_MAP[posFlag]

			return pos
		}
	}

	static final List<String> GEN_LIST=["m", "f", "n", "p"]
	static final List<String> VIDM_LIST=[
		"v_naz",
		"v_rod",
		"v_dav",
		"v_zna",
		"v_oru",
		"v_mis",
		"v_kly"
	]
	static final Pattern re_nv_vidm=Pattern.compile("(noun):[mfn]:(.*)")

	//@profile
	@TypeChecked
	List<String> expand_nv(List<String> in_lines) {
		def lines = []

		for( line in in_lines ){
			if (("noun" in line || "numr" in line) && ":nv" in line && ! (":v_" in line) ) {
				def parts = line.split(":nv")
				def part2 = parts.size() > 1 ? parts[1] : ""


				for( v in VIDM_LIST ){
					if( v == "v_kly" && (! (":anim" in line) || ":lname" in line) )
						continue

					lines.add(parts[0] + ":" + v + ":nv" + part2)
				}

				if( "noun" in line ) {
					if( ! (":p" in line) && ! (":np" in line) && ! (":lname" in line)) {
						for( v in VIDM_LIST ) {
							if( v != "v_kly" || "anim" in line) {
								lines.add(re_nv_vidm.matcher(line).replaceAll('$1:p:' + v + ':$2'))
							}
						}
					}
				}
			}
			//        print("expand_nv", in_lines, "\n", lines, file=sys.stderr)
			else if ("adj" in line && ":nv" in line && ! (":v_" in line) ) {
				def parts = line.split(":nv")

				def gens
				if( parts[0] ==~ /.*:[mnfp]/) {
					gens = parts[0][-1..-1]
					parts[0] = parts[0][0..<-2]
				}
				else {
					gens = GEN_LIST
				}

				for( g in gens ){
					for( v in VIDM_LIST ){
						if( v == "v_kly" && (! (":anim" in line) || ":lname" in line) )    // TODO: include v_kly? but ! for abbr like кв.
							continue

						lines.add(parts[0] + ":" + g + ":" + v + ":nv" + (parts.size()>1 ? parts[1] :""))
					}
				}
			} else {
				lines.add(line)
			}
		}
		return lines
	}



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

	static final Pattern re_verb = Pattern.compile("(in[fz]:coll|in[fz]|impr|pres|futr|past|impers)")
	static final Map<String,String> vb_tag_key_map = [
		"inf": "_1",
		"inz": "_2",
		"inf:coll": "_3",
		"inz:coll": "_4",
		"impr": "_5",
		"pres": "_6",
		"futr": "_7",
		"past": "_8",
		"impers": "_9"
	]

	static final Pattern GEN_RE = Pattern.compile(/:([mfnsp])(:|$)/)
	static final Pattern VIDM_RE = Pattern.compile(/:(v_...)((:alt|:rare|:coll)*)/) // |:short

	static final Pattern re_person_name_key_tag = Pattern.compile("^([^:]+(?::anim|:inanim|:perf|:imperf)?)(.*?)(:lname|:fname|:patr)")

	static final Pattern re_key = Pattern.compile(" ([^ ]+ [^:]+(?::rev)?(?::(?:anim|inanim|perf|imperf))?)")
	static final Pattern re_key_name = Pattern.compile(" ([^ ]+ noun:anim:[fmnp]:).*?(lname|fname|patr)")


	def derived_plural(key, prev_key) {
		return "name" in key && ":p:" in key &&
				prev_key =~ ":[mf]:" && prev_key.replaceFirst(":[mf]:", ":p:") == key
	}

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
				if( "name" in tags || "patr" in tags) {
					def key_rr = re_key_name.matcher(line)
					key_rr.find()
					key = key_rr.group(1) + key_rr.group(2)
				}
				else {
					def key_rr = re_key.matcher(line)
					key_rr.find()
					key = key_rr.group(1)
				}
				
				if( ":x" in line) {
					int x_idx = line.indexOf(":x")
					key += line[x_idx..<x_idx+4]
				}
			}
			catch(Exception e) {
				throw new RuntimeException("Failed to find tag key in " + line, e)
			}

			if( ":nv" in line) {
				key += ":nv"
			}
			if( key != prev_key && ! derived_plural(key, prev_key)) {
				prev_key = key
				line = word + " " + tags
				//            dbg("new key", key)
			} else {
				line = DERIV_PADDING + word + " " + tags

				//            if "m:v_naz" in line {
				//                sub_stat("adj", "super", line, sub_pos_stat)
				//                sub_stat("adj", "compr", line, sub_pos_stat)
				// for -corp compr/super are now separe lemmas
				//                if ":compr" in line || ":super" in line {
				//                    cnt_std += 1
			}
			out_lines.add(line)
		}

		return out_lines
	}

	def sub_stat(String pos, String sub_pos, line, sub_pos_stat) {
		if( ":" + sub_pos in line) {
			if( ! (pos in sub_pos_stat) ) {
				sub_pos_stat[pos] = [:].withDefault{ 0 }
			}
			sub_pos_stat[pos][sub_pos] += 1
		}
	}

	def stat_keys = [
		"inanim",
		"anim",
		"lname",
		"fname",
		"patr",
		"nv",
		"perf",
		"imperf",
		"compb"]

	def print_stats(List<String> lines, int double_form_cnt) {
		def pos_stat = [:].withDefault { 0 }
		def sub_pos_stat = [:].withDefault { [:] }
		def letter_stat = [:].withDefault { 0 }
		def cnt = 0
		def cnt_std = 0
		def proper_noun_cnt = 0

		for( line in lines ) {
			if( line[0] == " ")
				continue

			cnt += 1
			if( ! ("advp" in line) ) {
				cnt_std += 1
			}

			if( line.charAt(0).isUpperCase() && ! line.charAt(1).isUpperCase() ) {
				proper_noun_cnt += 1
			}

			try {
				def (word, tags) = line.split()

				def pos_tag = tags.split(":", 2)[0]
				pos_stat[pos_tag] += 1
				letter_stat[word[0].toLowerCase()] += 1

				if( tags.startsWith("adj") ) {
					sub_stat("adj", "super", line, sub_pos_stat)
					sub_stat("adj", "compr", line, sub_pos_stat)
				}

				for( sub_pos in stat_keys ) {
					sub_stat(pos_tag, sub_pos, line, sub_pos_stat)
				}
			}
			catch(Exception e) {
				throw new Exception("Choked on " + line, e)
			}
		}

		log.info("Всього лем: %d\n", cnt)

		new File("dict_stats.txt").withWriter("utf-8") { stat_f ->
			stat_f.printf("Всього лем: %d\n", cnt)
			stat_f.printf("  словникових лем (без advp, без омонімів imperf/perf та adjp/adj) %d\n", (cnt_std - double_form_cnt))
			stat_f.print("\nЧастоти за тегами:\n")

			def ordered_pos_freq = pos_stat.keySet().toList().sort()
			for( pos in ordered_pos_freq ){
				stat_f.println(pos + " " + pos_stat[pos])

				Map current_sub_pos_stat = sub_pos_stat[pos]
				def sub_pos_keys = current_sub_pos_stat.keySet().toList().sort()
				for( sub_pos in sub_pos_keys ) {
					stat_f.println("     " + sub_pos + " " + current_sub_pos_stat[sub_pos])
				}
			}
			stat_f.printf("\nВласних назв (без абревіатур): %d\n", proper_noun_cnt)

			//			stat_f.print("\nЧастоти літер на початку слова\n")
			//
			//			def letter_map = letter_stat.sort { -it.value }
			//			for( e in letter_map ){
			//				stat_f.println(e.key + " " + e.value)
			//			}
		}
	}

	static final Pattern re_xv_sub = Pattern.compile("^([^:]+)(.*)(:x.[1-9])")

	//	@CompileStatic
	@TypeChecked
	def tag_sort_key(String tags, String word) {
		if( tags.contains(":v-u") ) {
			tags = tags.replace(":v-u", "")
		}

		if( "v_" in tags) {
			def vidm_match = VIDM_RE.matcher(tags)

			if( vidm_match.find() ) {
				String vidm = vidm_match.group(1)
				String vidm_order = VIDM_ORDER[vidm]

				if( vidm_match.group(3)) {
					vidm_order = vidm_order.replace("0", vidm_match.group(2).count(":").toString())
				}

				tags = VIDM_RE.matcher(tags).replaceFirst(":"+vidm_order)
			}
		}

		if( tags.startsWith("adj:") ) {
			if( ! (":comp" in tags) && ! (":supe" in tags) ) {
				// make sure :short without :combp sorts ok with adjective base that has compb
				if( ":short" in tags) {
					tags = tags.replace(":short", "").replace("adj:", "adj:compc")
				}
				else {
					tags = tags.replace("adj:", "adj:compb:")
				}
			}
			//			else {
			// відокремлюємо різні порівняльні форми коли сортуємо: гладкий/гладкіший
			//				if( ":super" in tags)
			//					tags = re_sub("(:super)(.*)(:xx.)", '$3$1$2', tags)
			//				if( ":compr" in tags)
			//					tags = re_sub("(:compr)(.*)(:xx.)", '$3$1$2', tags)
			//
			//				if( ":super" in tags) {
			//					if( word.startsWith("що"))
			//						tags = tags.replace(":super", ":supes")
			//					else if( word.startsWith("як"))
			//						tags = tags.replace(":super", ":supet")
			//				}
			//			}
		}
		//		else if( tags.startsWith("advp") ) {
		//			tags = tags.replace("advp", "verz")  // put advp after verb
		//			if( ":coll" in tags )
		//				tags = tags.replace("perf", "perz")
		//		}
		else if( tags.startsWith("noun")) {
			if ("name" in tags || "patr" in tags) {
				tags = re_person_name_key_tag.matcher(tags).replaceAll('$1$3$2')
				if ( ("lname" in tags || "patr" in tags) && ":f:" in tags ) {// && ! ":nv" in tags:    // to put Адамишин :f: after Адамишини :p) {
					tags = tags.replace(":f:", ":9:")
				}
			}
			if( ":nv" in tags ) {
				tags = tags.replace(":nv", "").replace("anim", "anim:nv")
			}

			if( ":np" in tags || ":ns" in tags ) {
				tags = tags.replace(":np", "").replace(":ns", "")
			}
		}
		else if( tags.startsWith("verb")) {
			def verb_match = re_verb.matcher(tags)
			if( verb_match.find() ) {
				def tg = verb_match.group(0)
				tags = tags.replace(tg, vb_tag_key_map[tg])
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

		if( ":x" in tags ) {
			tags = re_xv_sub.matcher(tags).replaceAll('$1$3$2')
		}
		return tags
	}

	def line_key(txt) {
		try {
			def (word, lemma, tags) = txt.split()

			if( "verb:rev" in tags && ":inf" in tags && word.endsWith("сь") ) {
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

	def quickUkSort(collection) {
		ParallelEnhancer.enhanceInstance(collection)

		def entries = collection.collectParallel {
			[ (UkDictComparator.getSortKey(it)): it]
		}

		def map = entries.collectEntries {
			it
		}

		return map.sort().values()
	}

	//	@TypeChecked
	void print_word_list(List<String> sorted_lines) {
		log.info("Collecting words, lemmas, and tags...")

		def time1 = System.currentTimeMillis()

		HashSet<String> words = new HashSet<>()
		HashSet<String> spell_words = new HashSet<>()
		HashSet<String> lemmas = new HashSet<>()
		HashSet<String> tags = new HashSet<>()

		for( line in sorted_lines) {
			DicEntry dicEntry = DicEntry.fromLine(line)
			def word = dicEntry.word
			def lemma = dicEntry.lemma
			def tag = dicEntry.tagStr

			words.add(dicEntry.word)
			lemmas.add(dicEntry.lemma)

			if( ! (":bad" in tag) && ! (":alt" in tag) && ! (":uncontr" in tag) && ! (word.endsWith(".")) \
			        && ! (":inanim" in tag && ":v_kly" in tag) ) {
				spell_words.add(word)
			}

			tags.add(dicEntry.tagStr)
		}

		def lemmaList = quickUkSort(lemmas)
		new File("lemmas.txt").withWriter("utf-8") { f ->
			for(lemma in lemmaList) {
				f << lemma << "\n"
			}
		}

		def wordList = quickUkSort(words)

		new File("words.txt").withWriter("utf-8") { f ->
			for(word in wordList) {
				f << word << "\n"
			}
		}

		def spellWordList = quickUkSort(spell_words)

		new File("words_spell.txt").withWriter("utf-8") { f ->
			for(word in spellWordList) {
				f << word << "\n"
			}
		}

		def tagList = tags.toList().toSorted()
		new File("tags.txt").withWriter("utf-8") { f ->
			for(tag in tagList) {
				f << tag << "\n"
			}
		}

		if( Args.args.time ) {
			def time2 = System.currentTimeMillis()
			log.info("Word list time: %,d\n", (time2-time1))
		}

	}

	def log_usage(affix) {
		def affixMap = affix.affixMap.sort()
		new File("affix_usage.txt").withWriter("utf-8") { usageFile ->
			for( e in affixMap ) {
				def affixGroups = e.value
				def s1 = sprintf("Flag %s has %d groups\n", e.key, affixGroups.size())
				usageFile.print(s1)

				for( ent in affixGroups ) {
					def match = ent.key
					def affixGroup = ent.value
					def s2 = sprintf("\t%s : %d\t\t%d patterns\n", match, affixGroup.counter, affixGroup.affixes.size())
					usageFile.print(s2)
				}
			}
		}
	}

}

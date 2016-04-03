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
	static Logger log = LogManager.getFormatterLogger(Util.class);


	def tail_tag(line, tags) {
		for( tag in tags ) {
			//        tag = ":" + tag
			if( line.contains(tag) && ! line.endsWith(tag) ) {
				line = line.replace(tag, "") + tag
			}
		}
		return line
	}

	def bacteria(allAffixFlags) {
		return allAffixFlags.contains(">>")
	}

	def istota(allAffixFlags) {
		return allAffixFlags.contains("<")
	}

	def person(allAffixFlags) {
		return allAffixFlags.contains("<") && ! allAffixFlags.contains(">")
	}

	@TypeChecked
	def firstname(String word, String allAffixFlags) {
		return (allAffixFlags.contains("<") && ! allAffixFlags.contains(">")) \
			&& ! allAffixFlags.contains("<+") \
			&& word.charAt(0).isUpperCase() && ! word.charAt(1).isUpperCase()
	}

	def DUAL_LAST_NAME_PATTERN = ~ ".*(о|ич|ук|юк|як|аш|яш|сь|ун|ин|сон) "
	def dual_last_name_ending(line) {
		return line.contains("+d") || DUAL_LAST_NAME_PATTERN.matcher(line)
	}

	// dictionary-related methods


	static final Map POS_MAP = [
		"adj": "adj",
		"numr": "numr",
		"n": "noun",
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
			if ( ( line.contains("noun") || line.contains("numr") ) && line.contains(":nv") && ! line.contains(":v_") ) {
				def parts = line.split(":nv")
				def part2 = parts.size() > 1 ? parts[1] : ""


				for( v in VIDM_LIST ){
//					if( v == "v_kly" && (! (":anim" in line) || ":lname" in line) )
					if( v == "v_kly" && line.contains(". ") )
						continue

					lines.add(parts[0] + ":" + v + ":nv" + part2)
				}

				if( line.contains("noun") ) {
					if( ! line.contains(":p") && ! line.contains(":np") && ! line.contains(":lname") ) {
						for( v in VIDM_LIST ) {
        					if( v == "v_kly" && line.contains(". ") )
        					    continue
//							if( v != "v_kly" || "anim" in line) {
							lines.add(re_nv_vidm.matcher(line).replaceAll('$1:p:' + v + ':$2'))
//							}
						}
					}
				}
			}
			//        print("expand_nv", in_lines, "\n", lines, file=sys.stderr)
			else if (line.contains("adj") && line.contains(":nv") && ! line.contains(":v_") ) {
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
//						if( v == "v_kly" && (! (":anim" in line) || ":lname" in line) )    // TODO: include v_kly? but ! for abbr like кв.
        				if( v == "v_kly" && (line.contains(". ") || line.contains("&pron")) )
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



	def sub_stat(String pos, String sub_pos, String line, sub_pos_stat) {
		if( line.contains(":" + sub_pos) ) {
			if( ! (pos in sub_pos_stat) ) {
				sub_pos_stat[pos] = [:].withDefault{ 0 }
			}
			sub_pos_stat[pos][sub_pos] += 1
		}
	}

	def stat_keys = [
		"inanim",
		"anim",
		"prop",
		"lname",
		"fname",
		"patr",
		"nv",
		"perf",
		"imperf",
		"compb"
	]

	def print_stats(List<String> lines, int double_form_cnt) {
		def pos_stat = [:].withDefault { 0 }
		def sub_pos_stat = [:].withDefault { [:] }
		def letter_stat = [:].withDefault { 0 }
		def cnt = 0
		def cnt_std = 0

		for( line in lines ) {
			if( line[0] == " ")
				continue

			cnt += 1
			if( ! line.contains("advp") ) {
				cnt_std += 1
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

			//			stat_f.print("\nЧастоти літер на початку слова\n")
			//
			//			def letter_map = letter_stat.sort { -it.value }
			//			for( e in letter_map ){
			//				stat_f.println(e.key + " " + e.value)
			//			}
		}
	}


	//	@TypeChecked
	void print_word_list(List<String> sorted_lines) {
		log.info("Collecting words, lemmas, and tags...")

		def time1 = System.currentTimeMillis()

		HashSet<String> words = new HashSet<>()
		HashSet<String> spell_words = new HashSet<>()
		HashSet<String> lemmas = new HashSet<>()
		HashSet<String> tags = new HashSet<>()

		for( line in sorted_lines ) {
			DicEntry dicEntry = DicEntry.fromLine(line)
			def word = dicEntry.word
			def lemma = dicEntry.lemma
			def tag = dicEntry.tagStr

			words.add(dicEntry.word)
			lemmas.add(dicEntry.lemma)

			if( ! tag.contains(":bad") && ! tag.contains(":alt") && ! tag.contains(":uncontr") && ! word.endsWith(".") \
			        && ! tag.contains(":coll") && ! (tag.contains(":inanim") && tag.contains(":v_kly") ) ) {
				spell_words.add(word)
			}

			tags.add(dicEntry.tagStr)
		}

		def lemmaList = DictSorter.quickUkSort(lemmas)
		new File("lemmas.txt").withWriter("utf-8") { f ->
			for(lemma in lemmaList) {
				f << lemma << "\n"
			}
		}

		def wordList = DictSorter.quickUkSort(words)

		new File("words.txt").withWriter("utf-8") { f ->
			for(word in wordList) {
				f << word << "\n"
			}
		}
		log.info("%d total word forms", wordList.size())

		def spellWordList = DictSorter.quickUkSort(spell_words)

		new File("words_spell.txt").withWriter("utf-8") { f ->
			for(word in spellWordList) {
				f << word << "\n"
			}
		}
		log.info("%d spelling word forms", spellWordList.size())

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

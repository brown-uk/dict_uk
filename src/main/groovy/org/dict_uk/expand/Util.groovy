package org.dict_uk.expand

import java.util.Map
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.*

import org.dict_uk.common.DicEntry
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.CompileStatic


class Util {
	private static final Logger log = LoggerFactory.getLogger(Util.class);

	@CompileStatic
	DicEntry tail_tag(DicEntry line, List<String> tags) {
		for(String tag in tags ) {
			//        tag = ":" + tag
			if( line.tagStr.contains(tag) && ! line.tagStr.endsWith(tag) ) {
				line.tagStr = line.tagStr.replace(tag, "") + tag
			}
		}
		return line
	}

	@CompileStatic
	boolean bacteria(String allAffixFlags) {
		return allAffixFlags.contains(">>")
	}

	@CompileStatic
	boolean istota(String allAffixFlags) {
		return allAffixFlags.contains("<")
	}

	@CompileStatic
	boolean person(String allAffixFlags) {
		return allAffixFlags.contains("<") && ! allAffixFlags.contains(">")
	}

//	@CompileStatic
//	boolean firstname(String word, String allAffixFlags) {
//        // decided to leave animal names with :fname
//		return (allAffixFlags.contains("<") \
//                /* && ! allAffixFlags.contains(">")*/ ) \
//			&& ! allAffixFlags.contains("<+") \
//			&& word.charAt(0).isUpperCase() && ! word.charAt(1).isUpperCase() \
//			&& ! allAffixFlags.contains(":prop")    // Всевишній - :prop but not :fname
//	}
//
//    @CompileStatic
//    boolean animalname(String word, String allAffixFlags) {
//        return allAffixFlags.contains("<>") \
//            && word.charAt(0).isUpperCase() && ! word.charAt(1).isUpperCase() \
//            && ! allAffixFlags.contains(":prop")
//    }

	static final Pattern DUAL_LAST_NAME_PATTERN = ~ ".*(о|ій|ай|ич|ач|ик|ук|юк|як|ак|аш|яш|ар|яр|ун|ян|ин|[їі]в|[сцтн]ь|сон|сен|ес|ез) "

	@CompileStatic
	def dual_last_name_ending(String line) {
		return line.contains("+d") || DUAL_LAST_NAME_PATTERN.matcher(line)
	}

	// dictionary-related methods


	static final Map POS_MAP = [
		"adj": "adj",
		"numr": "numr",
		"n": "noun",
		"vr": "verb",
		"v": "verb",
		"patr": "noun",
		"<": "noun"
	]


	private static final Pattern POS_PATTERN = ~ /[\._].*/

	@CompileStatic
	String get_pos(String posFlag, Map<String,String> modifiers) {
		posFlag = POS_PATTERN.matcher(posFlag).replaceAll("")
//		log.debug("\t\t"  + posFlag + ", " + modifiers)

		String pos
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

	private static final String GEN_LIST = "mfnp"
	private static final List<String> VIDM_LIST = [
		"v_naz",
		"v_rod",
		"v_dav",
		"v_zna",
		"v_oru",
		"v_mis",
		"v_kly"
	]
	static final Pattern re_nv_vidm = Pattern.compile("(noun(?::anim)?):[mfn]:(.*)")

	@CompileStatic
	List<DicEntry> expand_nv(List<DicEntry> in_lines) {
		List<DicEntry> lines = []

		for(DicEntry entry in in_lines) {
			String lineTagStr = entry.tagStr

			if( ! lineTagStr.contains(":nv") ) {
				lines << entry
			}
            else {
                if( lineTagStr =~ /:v_...:nv/ ) {
                    lines << entry
                    continue
                }
            }
			
			if ( ( lineTagStr.startsWith("noun") || lineTagStr.startsWith("numr") ) && ! lineTagStr.contains(":v_") ) {
				def parts = lineTagStr.split(":nv")
				def part2 = parts.size() > 1 ? parts[1] : ""

				// add singular
				for( v in VIDM_LIST ){
					if( v == "v_kly" && skipVkly(entry) )
						continue
                        
					String tag = parts[0] + ":" + v + ":nv" + part2
					lines.add(new DicEntry(entry.word, entry.lemma, tag, entry.comment))
				}

				// add plural
				if( lineTagStr.contains("noun") ) {
					if( ! lineTagStr.contains(":p") && ! lineTagStr.contains(":np") && ! lineTagStr.contains(":lname") ) {
						for( v in VIDM_LIST ) {
        					if( v == "v_kly" && skipVkly(entry) )
        					    continue

							def newTagStr = re_nv_vidm.matcher(lineTagStr).replaceAll('$1:p:' + v + ':$2')
							String comment = entry.comment && DicEntry.isPossibleLemma(newTagStr) ? entry.comment : null
							lines.add(new DicEntry(entry.word, entry.lemma, newTagStr, comment))
						}
					}
				}
			}
			else if (lineTagStr.startsWith("adj") && ! lineTagStr.contains(":v_") ) {
                def req = ""
                if( lineTagStr.contains(":rinanim") ) {
                      req = "rinanim"
                      lineTagStr = lineTagStr.replace(":rinanim", "")
                }
                else if( lineTagStr.contains(":ranim") ) {
                      req = "ranim"
                      lineTagStr = lineTagStr.replace(":ranim", "")
                }

                List<String> parts = lineTagStr.split(":nv") as List

				String gens
				if( parts[0] ==~ /.*:[mnfp]/) {
					gens = parts[0][-1..-1]
					parts[0] = parts[0][0..<-2]
				}
				else if( parts[0] ==~ /.*:[mnf]p/) {
					gens = parts[0][-2..-1]
					parts[0] = parts[0][0..<-3]
				}
				else {
					gens = GEN_LIST
				}
                
				for(def gen in gens) {
					for(String v in VIDM_LIST){
        				if( v == "v_kly" && (skipVkly(entry) || lineTagStr.contains("&pron")) )
							continue
                            
                        if( v == "v_zna" && "mp".contains(gen) ) {
                            v += ":rinanim"
                        }
    
                        String newTagStr = parts[0] + ":" + gen + ":" + v + ":nv" + (parts.size()>1 ? parts[1] :"")
                        String comment = entry.comment && DicEntry.isPossibleLemma(newTagStr) && gen == gens[0] ? entry.comment : null

                        if( req != "ranim" || ! v.contains("rinanim") ) {
                            lines.add(new DicEntry(entry.word, entry.lemma, newTagStr, comment))
                        }
                    
                        if( v == "v_zna:rinanim") {
                            if( req != "rinanim" ) {
                                v = "v_zna:ranim"
                                newTagStr = parts[0] + ":" + gen + ":" + v + ":nv" + (parts.size()>1 ? parts[1] :"")
                                lines.add(new DicEntry(entry.word, entry.lemma, newTagStr, comment))
                            }
                        }
					}
				}
			}
		}
		return lines
	}

    static boolean skipVkly(DicEntry entry) {
        entry.word.endsWith(".") || entry.tagStr.contains(":abbr")
    }
    

	void sub_stat(String pos, String sub_pos, String line, sub_pos_stat) {
		if( line.contains(":" + sub_pos) ) {
			if( ! (pos in sub_pos_stat) ) {
				sub_pos_stat[pos] = [:].withDefault{ 0 }
			}
			sub_pos_stat[pos][sub_pos] += 1
		}
	}

	private static final List<String> stat_keys = [
		"inanim",
		"anim",
		"prop",
		"lname",
		"fname",
		"pname",
		"geo",
		"nv",
		"perf",
		"imperf",
		"compb"
	]

	def print_stats(List<String> lines, int double_form_cnt) {
		def pos_stat = [:].withDefault { 0 }
		def sub_pos_stat = [:].withDefault { [:] }
		def letter_stat = [:].withDefault { 0 }
		int cnt = 0
		int cnt_std = 0

		for(String line in lines ) {
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

		log.info("Всього лем: {}\n", cnt)


        if( ! new File("stats").isDirectory() ) {
            log.info "No stats/ directory, skipping dict_stats.txt"
            return
        }

		new File("stats", "dict_stats.txt").withWriter("utf-8") { stat_f ->
			stat_f.printf("Всього лем: %d\n", cnt)
			stat_f.printf("  словникових лем (без advp/bad/slang/alt, без омонімів imperf/perf) %s\n", (cnt_std - double_form_cnt))
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


	@CompileStatic
	void print_word_list(List<DicEntry> sortedEntries) {
		log.info("Collecting words and lemmas...")

		def time1 = System.currentTimeMillis()

		HashSet<String> words = new HashSet<>()
		HashSet<String> spellWords = new HashSet<>()
		HashSet<String> lemmas = new HashSet<>()
		HashSet<String> tags = new HashSet<>()

		for(DicEntry dicEntry in sortedEntries) {
			def word = dicEntry.word
			def lemma = dicEntry.lemma
			def tag = dicEntry.tagStr

			words.add(dicEntry.word)
			lemmas.add(dicEntry.lemma)

			if( ! word.endsWith(".")
				&& ( ! (tag =~ /:(bad|subst|alt|short|long|slang|arch|vulg|obsc)/) \
				|| tag =~ /&insert:short/ \
				|| tag =~ /adj:m:v_(naz|zna).*:short/ \
				|| word =~ /^((що(як)?|як)?най)?(більш|менш|скоріш|перш)$/)
					//&& ! (tag.contains(":inanim") && tag.contains(":v_kly") )
					 ) {
				spellWords.add(word)
			}

			tags.add(dicEntry.tagStr)
		}

		ExecutorService executor = Executors.newWorkStealingPool();
		
//		executor.execute( { 
//			def lemmaList = DictSorter.quickUkSort(lemmas)
//			new File("lemmas.txt").withWriter("utf-8") { f ->
//				for(lemma in lemmaList) {
//					f << lemma << "\n"
//				}
//			}
//			log.info("{} total unique lemmas", lemmaList.size())
//		})

		executor.execute( { 
			def wordList = DictSorter.quickUkSort(words)

			new File("words.txt").withWriter("utf-8") { f ->
				for(word in wordList) {
					f << word << "\n"
				}
			}
			log.info("{} total word forms", wordList.size())
		})

		executor.execute( {
			def spellWordList = DictSorter.quickUkSort(spellWords)

			new File("words_spell.txt").withWriter("utf-8") { f ->
				for(word in spellWordList) {
					f << word << "\n"
				}
			}
			log.info("{} spelling word forms", spellWordList.size())
		})
		
//			executor.execute( {
//				def tagList = tags.toList().toSorted()
//				new File("tags.txt").withWriter("utf-8") { f ->
//					for(tag in tagList) {
//						f << tag << "\n"
//					}
//				}
//			}
		
		executor.shutdown()
		executor.awaitTermination(300, TimeUnit.SECONDS)
		
		log.info("Done collecting words")
	}

	void log_usage(Affix affix) {
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

#!/usr/bin/env groovy

// This script loads hunspell-like affixes && allows to perform some actions
package org.dict_uk.expand

import groovy.transform.TypeChecked
import groovyx.gpars.ParallelEnhancer
import groovyx.gpars.GParsPool

import java.util.regex.*

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import org.dict_uk.common.*


class Expand {
	static Logger log = LogManager.getFormatterLogger(Expand.class);

	private final Util util = new Util()
	private final DictSorter dictSorter = new DictSorter()
	private final Affix affix = new Affix()
	private final BaseTags base_tags = new BaseTags()
	private final OutputValidator validator = new OutputValidator()
	private final List<String> limitedVerbLemmas = ["житися", "забракнуло", "зберігти", "зберігтись", "зберігтися"];


	Pattern cf_flag_pattern = ~ /(vr?)[1-4]\.cf/	 // v5.cf is special
	Pattern imprs_pattern = ~ /(vr?)[1-9]\.imprs/
	Pattern pattr_pattern = ~ /n[0-9]+\.patr/

	@TypeChecked
	def adjustCommonFlag(String affixFlag2) {
		if( affixFlag2.contains(".cf") ) {
			affixFlag2 = cf_flag_pattern.matcher(affixFlag2).replaceFirst('$1.cf')
		}
		if( affixFlag2.contains(".imprs") ) {
			affixFlag2 = imprs_pattern.matcher(affixFlag2).replaceFirst('$1.imprs')
		}
		if( affixFlag2.contains(".patr") ) {
			affixFlag2 = pattr_pattern.matcher(affixFlag2).replaceFirst('n.patr')
		}
		return affixFlag2
	}

	@TypeChecked
	List<String> expand_suffixes(String word, String affixFlags, Map<String,String> modifiers, String extra) {
		//		log.info("%s %s %s %s\n", word, affixFlags, modifiers, extra)

		def affixSubGroups = affixFlags.split("\\.")
		def mainGroup = affixSubGroups[0]

		String pos = util.get_pos(mainGroup, modifiers)
		def base_tag = base_tags.get_base_tags(word, "", affixFlags, extra)
		//2      base_word = word + " " + pos + base_tag
		def base_word = word + " " + word + " " + pos + base_tag
		def words = [base_word]

		if( affixFlags[0] == "<" )
			return words


		def appliedCnt = 0
		Map<String, Integer> appliedCnts = [:]
		def affixFlags2 = []

		for( affixFlag2 in affixSubGroups) {
			if( affixFlag2.contains("<") || affixFlag2 == "@")
				continue

			if( affixFlag2 != mainGroup) {
//				if( ! (affixFlag2 in ["v2", "vr2"]) ) {  // курликати /v1.v2.cf       задихатися /vr1.vr2
					affixFlag2 = mainGroup + "." + affixFlag2
					if( affixFlag2 == "v3.advp")
						affixFlag2 = "v1.advp"
					else if( affixFlag2 == "v3.imprt0" )
						affixFlag2 = "v1.imprt0"
//				}

				affixFlag2 = adjustCommonFlag(affixFlag2)
			}

			appliedCnts[affixFlag2] = 0

			//util.dbg(affix.affixMap.keySet())
			if( ! (affixFlag2 in affix.affixMap.keySet()) ) {
				throw new Exception("Could not find affix flag " + affixFlag2)
			}


			Map<String, SuffixGroup> affixGroupMap = affix.affixMap[affixFlag2]

			for( Map.Entry<String, SuffixGroup> ent in affixGroupMap.entrySet() ) {
				def match = ent.key
				SuffixGroup affixGroup = ent.value

				if( affixGroup.matches(word) ) {

					for( affix_item in affixGroup.affixes) {
						// DL - не додавати незавершену форму дієприслівника для завершеної форми дієслова
						if( pos.startsWith("verb") && extra.contains(":perf")
						&& (affix_item.tags.startsWith("advp:imperf")
						|| affix_item.tags.startsWith("advp:rev:imperf"))) {
							appliedCnts[ affixFlag2 ] = 1000
							continue
						}

						String deriv = affix_item.apply(word)
						String tags = affix_item.tags

						if( deriv =~ /[а-яіїєґ][a-z0-9]/ )
							assert false : "-- latin mix in " + deriv

						if( affixFlag2 == "n.patr") {
							tags += ":prop:patr"
						}

						words.add(deriv + " " + word + " " + tags)
						appliedCnt += 1
						appliedCnts[affixFlag2] += 1

						//util.debug("applied %s to %s", affixGroup, word)
					}
					affixGroup.counter += 1

					//      print("DEBUG: applied", affixFlags, "for", word, "got", appliedCnts, file=sys.stderr)
				}
			}

			if( appliedCnts[ affixFlag2 ] == 0 ) {
				throw new Exception("Flag " + affixFlag2 + " of " + affixFlags + " not applicable to " + word)
			}
		}

		def dups = words.findAll { words.count(it) > 1 }.unique()
		if( dups.size() > 0) {
		    if( ! (affixFlags =~ /p1\.p2|p[12]\.piv/) ) {
			    log.warn("duplicates: " + dups + " for " + word + " " + affixFlags)
			}
		}

		return words
	}

	Map<String,String> get_modifiers(mod_flags, flags, word) {

		def mods = [:]

		if( flags.contains("/adj") && flags.contains("<") ) {
			mods["pos"] = "noun"

			if( ! mod_flags.contains("=") ) {
				if( flags.contains("<+") ) {
					if( word.endsWith("а"))
						mods["gen"] = "f"
					else
						mods["gen"] = "mfp"
						
					return mods
				}
				
				if( flags.contains("<") ) {
					if( word.endsWith("а"))
						mods["gen"] = "fp"
					else
						mods["gen"] = "mp"
						
					return mods
				}
			}
			
			if( ! mod_flags.contains("=") ) {
				mods["gen"] = "mfp"
				return mods
			}
		}

		def mod_set = mod_flags.split()

		for( mod in mod_set) {
			if( mod[0] == "^") {
				if( mod.startsWith("^adjp")) {
					mods["pos"] = mod[1..-1]
				}
				else {
					def mod_tags = mod[1..-1].split(":")
					mods["pos"] = mod_tags[0]
					if( mod_tags && mod_tags[0] == "noun") {
						if( mod_tags.size() > 1 ) {
							assert mod_tags[1].size() == 1 : "Bad gender override: " + mod + " -- " + mod_tags

							mods["force_gen"] = mod_tags[1]
						}
					}
				}
			}
			else if( mod.size() >= 2 && mod[0..<2] == "g=" )
				mods["gen"] = mod.replaceFirst(/g=([^ ])/, '$1')    //mod[2:3]
			else if( mod.size() >= 2 && mod[0..<2] == "p=" )
				mods["pers"] = mod[2..2]
			else if( mod.startsWith("tag=") )
				mods["tag"] = mod[4..-1]
		}
		if( flags.contains("<+m") || flags.contains("<m") ) {
			mods["force_gen"] = "m"
			if( flags.contains("n2adj") ) {
				mods["gen"] = "m"
			}
		}

		//    util.debug("mods %s for %s && %s", str(mods), flags, mod_flags)

		return mods
	}

	@TypeChecked
	boolean filter_word(String w, Map modifiers) {
		if( "gen" in modifiers) {
			//        util.dbg("filter by gen", modifiers, w)
			if( ! (w =~ (":[" + modifiers["gen"] + "]:") ) )
				return false
		}
		if( "pers" in modifiers && ! ( w =~ ":(inf|past)") ) {
			if( ! (w =~ ":[" + modifiers["pers"] + "]") )
				return false
		}
		if( "tag" in modifiers) {
			if( ! (w =~ modifiers["tag"]) )
				return false
		}
		return true
	}

	@TypeChecked
	List<String> modify(List<String> lines, Map modifiers) {
		//    util.dbg("mods", modifiers)
		if( modifiers.size() == 0)
			return lines

		def out = []
		for( line in lines) {

			if( ! filter_word(line, modifiers)) {
				//            util.debug("skip %s %s", line, modifiers)
				continue
			}
			if( "pos" in modifiers) {
				line = line.replaceAll(" [^ :]+:", " " + modifiers["pos"] + ":")
				//            util.debug("pos repl %s in %s", modifiers["pos"], line)
			}
			if( "force_gen" in modifiers && ! line.contains(":patr") ) {
				def force_gen = modifiers["force_gen"]
				line = line.replaceAll(/:[mfn](:|$)/,  ":" + force_gen + /$1/)
				//            util.debug("gen repl: %s in %s", force_gen, line)
			}

			out.add(line)
		}

		assert out.size() > 0 : "emtpy output for "+ lines + " && " + modifiers

		return out
	}

	@TypeChecked
	String get_extra_flags(String flags) {
		def extra_flags = ""

		if( flags.contains(" :") ) {
			def matcher = Pattern.compile(" (:[^ ]+)").matcher(flags)
			if( ! matcher.find() )
				throw new Exception("Not found extra flags in " + flags)
			extra_flags = matcher.group(1)
		}
		
		if( flags.contains("<") ) {
		    if( flags.contains(">>") ) {
    			extra_flags += ":unanim"
			}
			else {
			    extra_flags += ":anim"
			}
		}
		
		if( flags.contains("<+") ) {
			extra_flags += ":prop:lname"
		}

		return extra_flags
	}
	
	Pattern perf_imperf_pattern = ~ ":(im)?perf"
	Pattern and_adjp_pattern = ~ /:&_?adjp(:pasv|:actv|:perf|:imperf)+/

	@TypeChecked
	List<String> post_expand(List<String> lines, String flags) {
		if( lines.size() == 0)
			throw new Exception("emtpy lines")

		String extra_flags = get_extra_flags(flags)


		if( extra_flags) {
			def first_name_base = util.firstname(lines[0], flags)

			def out_lines = []
			def extra_out_lines = []

			for( line in lines) {
				String extra_flags2 = extra_flags

				if( first_name_base && ! line.contains(":patr") ) {
					extra_flags2 += ":prop:fname"
				}
				if( line.contains(" advp") ) {
					if( line.contains(":imperf") )
						extra_flags2 = perf_imperf_pattern.matcher(extra_flags2).replaceFirst("")
					else
						line = line.replace(":perf", "")
				}
				else if( flags.contains("adj.adv") && line.contains(" adv") )
					extra_flags2 = and_adjp_pattern.matcher(extra_flags2).replaceFirst("")
				else if( extra_flags.contains(":+m") ) {
					extra_flags2 = extra_flags2.replace(":+m", "")

					if( line.contains(":f:") ) {
						def masc_line = line.replace(":f:", ":m:") + extra_flags2
						extra_out_lines.add(masc_line)
					}
					else if( line.contains(":n:") ) {
						def masc_line = line.replace(":n:", ":m:") + extra_flags2

						if( util.istota(flags)) {
							if( masc_line.contains("m:v_rod") ) {
								def masc_line2 = masc_line.replace("m:v_rod", "m:v_zna")
								extra_out_lines.add(masc_line2)
							}
							else if( masc_line.contains("m:v_zna") ) {
								masc_line = ""
							}
							if( masc_line.contains("m:v_kly") ) {
								//                            word, lemma, tags = masc_line.split()
								DicEntry dicEntry = DicEntry.fromLine(masc_line)
								masc_line = dicEntry.word[0..<-1]+"е " + dicEntry.lemma + " " + dicEntry.tagStr
							}
						}
						if( masc_line ) {
							extra_out_lines.add(masc_line)
						}
					}
				}
				else if( extra_flags.contains(":+f") ) {
					extra_flags2 = extra_flags2.replace(":+f", "")

					if( line.contains(":m:") ) {
						def masc_line = line.replace(":m:", ":f:") + extra_flags2
						extra_out_lines.add(masc_line)
					}
					else if( line.contains(":n:") ) {
						def masc_line = line.replace(":n:", ":f:") + extra_flags2

						//                     if( util.istota(flags)) {
						//                         if( "m:v_rod" in masc_line) {
						//                             masc_line2 = masc_line.replace("m:v_rod", "m:v_zna")
						//                             extra_out_lines.add(masc_line2)
						//                         else if "m:v_zna" in masc_line:
						//                             masc_line = ""

						if( masc_line) {
							extra_out_lines.add(masc_line)
						}
					}
				}
				else if( line.contains(":patr") && extra_flags2.contains(":anim") ) {
					line = line.replace(":patr", ":anim:patr")
					extra_flags2 = extra_flags2.replace(":anim", "")
				}
				out_lines.add(line + extra_flags2)
			}
			out_lines.addAll(extra_out_lines)

			return out_lines
		}
		return lines
	}

	@TypeChecked
	List<String> adjust_affix_tags(List<String> lines, String main_flag, String flags, Map<String,String> modifiers) {
		def lines2 = []

		for( line in lines) {
			// DL-
			if( main_flag[1] == "n" ) {

				def word
				String base_word
				if( main_flag.startsWith("/n2") && main_flag =~ "^/n2[01234]" ) {
					base_word = line.split()[1]

					if( util.istota(flags)) {
						if( line.contains("m:v_rod") && ! line.contains("/v_zna") ) {
							line = line.replace("m:v_rod", "m:v_rod/v_zna")
						}
					}
					if( ! "аеєиіїоюя".contains(base_word[-1..-1]) && ! flags.contains(".a") ) {
						word = line.split()[0]
						if( "ую".contains(word[-1..-1]) ) {
							line = line.replace("v_dav", "v_rod/v_dav")
						}
					}
				}
				else if( main_flag.startsWith("/n2adj") ) {
					if( ! util.istota(flags)) {
						if( line.contains("v_rod/v_zna") ) {
							line = line.replace("/v_zna", "")
						}
					}
				}
				else if( main_flag.startsWith("/n2nm") ) {
					if( util.istota(flags)) {
						if( line.contains("m:v_rod") && ! line.contains("/v_zna") ) {
							line = line.replace("m:v_rod", "m:v_rod/v_zna")
						}
					}
				}
				
				if( main_flag.startsWith("/n2") && flags.contains("@") ) {
					word = line.split(" ", 2)[0]
					if( "ая".contains(word[-1..-1]) && line.contains("m:v_rod") ) {
						line = line.replace("m:v_rod", "m:v_rod/v_zna")
					}
				}
				
				if( ! main_flag.contains("np") && ! main_flag.contains(".p") && ! flags.contains("n2adj") ) {
					if( line.contains(":p:") ) {
						// log.debug("skipping line with p: " + line)
					}
					else if( line.contains("//p:") ) {
						line = line.replaceAll("//p:.*", "")
						// log.debug("removing //p from: " + line)
					}
				}
				
				if( line.contains("/v_kly") ) {
					if( main_flag.startsWith("/n1")) { // Єремія /n10.ko.patr.<
						base_word = line.split()[1]
					}
					
					if( //("<+" in flags && ! (":p:" in line)) \
//					    || (main_flag =~ "/n2n|/n4" && ! util.istota(flags)) \
					     //(! (main_flag =~ "/n2n|/n2adj1|/n4") && ! util.person(flags) ) \
                         ( (flags.contains(".ko") || flags.contains(".ke")) && ! line.contains(":patr") ) \
                        || (line.contains(":m:") && flags.contains("<+") ) \
                        ) {//|| (main_flag.startsWith("/n20") && base_word.endsWith("ло") && "v_dav" in line) ) {
						//log.info("removing v_kly from: %s, %s", line, flags)
						line = line.replace("/v_kly", "")
					}
				}
				
				if( main_flag.contains(".p") || main_flag.contains("np") ) {
//					if( util.person(flags)) {
					if( main_flag.contains(".") ) {
						line = line.replace("p:v_naz", "p:v_naz/v_kly")
					}
					
					if( util.istota(flags) ) {
						line = line.replace("p:v_rod", "p:v_rod/v_zna")
						if( flags.contains(">") ) { // animal
							line = line.replace("p:v_naz", "p:v_naz/v_zna")
						}
					}
					else {
						line = line.replace("p:v_naz", "p:v_naz/v_zna")
					}
				}
			}
			else if( flags.contains(":perf") && line.contains(":pres") ) {
				line = line.replace(":pres", ":futr")
			}
			else if( main_flag.startsWith("/adj") ) {
				if( flags.contains("<") || flags.contains("^noun") ) {
					if( line.contains(":uncontr") )
						continue
				}

				if( flags.contains(":&pron") ) {
					line = line.replace("/v_kly", "")
				}

				
				if( flags.contains("<") ) {
					if( ! flags.contains(">") && line.contains(":p:v_naz/v_zna") )
						line = line.replace("v_naz/v_zna", "v_naz")
//					if( ":m:v_naz" in line /*&& ! ("<+" in flags)*/)
//						line = line.replace("v_naz", "v_naz/v_kly")
				}
				else if( flags.contains("^noun") ) {
					if( line.contains(":m:v_rod/v_zna") ) {
						line = line.replace("v_rod/v_zna", "v_rod")
					}
					else if( line.contains(":p:v_rod/v_zna") ) {
						line = line.replace("v_rod/v_zna", "v_rod")
					}
				}

			}
			
			lines2.add(line)
		}
		return lines2
	}

	@TypeChecked
	List<String> expand(String word, String flags) {
		String[] flag_set = flags.split(" ", 2)

		String main_flag = flag_set[0]

		String extra = flag_set.size() > 1 ? flag_set[1] : ""

		Map<String,String> modifiers = get_modifiers(extra, flags, word)

		List<String> sfx_lines
		if( main_flag[0] == "/") {
			def inflection_flag = main_flag[1..-1]
			sfx_lines = expand_suffixes(word, inflection_flag, modifiers, extra)
			sfx_lines = adjust_affix_tags(sfx_lines, main_flag, flags, modifiers)
		}
		else {
			sfx_lines = [
				word + " " + word + " " + flags
			]
		}

		sfx_lines = affix.expand_alts(sfx_lines, "//")  // TODO: change this to some single-char splitter?
		sfx_lines = affix.expand_alts(sfx_lines, "/")

		if( flags.contains("/adj") ) {
			def out_lines = []
			for( line in sfx_lines) {
				if( line.contains("v_zn1") ) { // v_zna == v_rod
                    if( flags.contains("<") ) {
                        line = line.replace("v_zn1", "v_zna")
                    }
                    else if( flags.contains("^noun") ) {
                        continue
					}
					else {
						line = line.replace("v_zn1", "v_zna:ranim")
					}
				}
				else if( line.contains("v_zn2") ) { // v_zna = v_naz
					if( flags.contains("<") && (! flags.contains(">") || line.contains(":m:")) ) {
						continue
					}
					else if( flags.contains("^noun") ) {
						line = line.replace("v_zn2", "v_zna")
					}
					else {
						line = line.replace("v_zn2", "v_zna:rinanim")
					}
				}
				out_lines.add(line)
			}
			sfx_lines = out_lines
		}
		
		if( main_flag[0] != "/") {
			sfx_lines = util.expand_nv(sfx_lines)
		}
		
		sfx_lines = modify(sfx_lines, modifiers)

		if( flags.contains("\\") ) {
			sfx_lines = sfx_lines.collect { it + ":compb" }
		}
		
		def words = post_expand(sfx_lines, flags)

		words.each {
			if( it =~ /[а-яіїєґ][a-z0-9]/ )
				throw new Exception("latin mix in " + it) 
		}

		return words
	}

	private static final Pattern tag_split0_re = Pattern.compile(/[^ ]+$/)

	@TypeChecked
	List<String> preprocess(String line) {
		def lines
		if( line.count(" /") > 1) {
			String[] parts = line.split(" ")
			def line1 = parts[0..<2]
			if( parts.size()>3 )
				line1 += parts[3..-1]
			def line2 = parts[0..<1] + parts[2..-1]
			lines = [
				line1.join(" "),
				line2.join(" ")
			]
		}
		else {
			lines = affix.expand_alts([line], "|")
		}

		def out_lines = []
		for( line2 in lines) {
			out_lines.addAll(preprocess2(line2))
		}
		return out_lines
	}

	final Pattern plus_f_pattern = ~ "/<\\+?f?( (:[^ ]+))?"
	final Pattern plus_m_pattern = ~ "/<\\+?m?( (:[^ ]+))?"
	
	@TypeChecked
	def preprocess2(String line) {
		def out_lines = []

		// patronym plurals
		// TODO: and we need to split lemmas
//		if( line.contains(".patr") ) {
//			line = line.replace(".patr", ".patr.patr_pl")
//		}

		
		if( line.contains("/<") ) {
		
			def extra_tag = ":anim"
			if( line.contains("<+") ) {
				extra_tag += ":prop:lname"
			}
			else {
			    if( Character.isUpperCase(line.charAt(0)) ) {
				    extra_tag += ":prop:fname"
			    }
			}

			if( ! line.contains("<m") && ! line.contains("<+m") ) {
				def tag = "noun:f:nv:np"
				def line1 = plus_f_pattern.matcher(line).replaceFirst(tag + extra_tag + '$2')
				out_lines.add(line1)
			}
			if( ! line.contains("<f") && ! line.contains("<+f") ) {
				def tag = "noun:m:nv:np"
				def line1 = plus_m_pattern.matcher(line).replaceFirst(tag + extra_tag + '$2')
				out_lines.add(line1)
			}
		}
		else if( line.contains("/n2") && line.contains("<+") ) {
			if( ! line.contains("<+m") && util.dual_last_name_ending(line)) {
				out_lines.add(line)
				def line_fem_lastname = line.split()[0] + " noun:f:nv:np:anim:prop:lname"
				
                if( line.contains(" :") ) {
                    def matcher = line =~ /:[^ ]+/
                    matcher.find()
                    def extra_tag2 = matcher.group(0).replaceAll(/:xp\d/, '')
                    line_fem_lastname += extra_tag2
                }

				out_lines.add(line_fem_lastname)
			}
			else {
				out_lines = [line]
			}
		}
		else if( line.contains("/n1") && line.contains("<+") ) {
			if( ! line.contains("<+f") && ! line.contains("<+m") ) {
				out_lines.add(line)
				def line_masc_lastname = line.replace("<+", "<+m")
				out_lines.add(line_masc_lastname)
			}
			else {
				out_lines = [line]
			}
		}
		else if( line.contains("/n10") || line.contains("/n3") ) {
			if( /*line.contains(".<") && ! line.contains(">") &&*/ ! line.contains(".k") && ! line.contains("ще ") ) {
			    def parts = line.split()
			    parts[1] += line.contains("/n10") ? ".ko" : ".ke"
			    line = parts.join(" ")
			}
			out_lines = [line]
		}
		else if( line =~ ' /n2[0-4]' && ! line.contains(".k") ) {
			if( line =~ '[бвджзлмнпстфц] /n2' ) {
			    def parts = line.split()
			    parts[1] += ".ke"
			    line = parts.join(" ")
		    }
			out_lines = [line]
		}
		else if( line.contains("/np") ) {
			def space = " "
			if( line.contains(" :") || ! line.contains(" /") ) {
				space = ""
			}
			line = line + space + ":ns"
			out_lines = [line]
		}
		else if( line.contains(":imperf:perf") ) {
			def line1 = line.replace(":perf", "")
			def line2 = line.replace(":imperf", "").replace(".cf", "").replace(".adv ", " ") // so we don't duplicate cf and adv
			//.replace(".advp")  // so we don"t get two identical advp:perf lines
			out_lines = [line1, line2]
		}
		else if( line.contains(":&adj") && ! line.contains(" :&adj") ) {
			line = line.replace(":&adj", " :&adj")
			out_lines = [line]
		}
		else {
			out_lines = [line]
		}
		
		return out_lines
	}

	Pattern PATTR_BASE_LEMMAN_PATTERN = ~ ":[mf]:v_naz:.*patr"
	
	@TypeChecked
	List<String> post_process_sorted(List<String> lines) {
		def out_lines = []

		def prev_line = ""
		def last_lema
		for( line in lines) {
			if( line.contains("patr") ) {
				if( PATTR_BASE_LEMMAN_PATTERN.matcher(line).find() ) {
					last_lema = line.split()[0]
					//                System.err.printf("promoting patr to lemma %s for %s\n", last_lema, line)
				}
				line = replace_base(line, last_lema)
			}
			else if( line.contains("lname") && line.contains(":f:") && ! line.contains(":nv") ) {
				if( line.contains(":f:v_naz") ) {
					last_lema = line.split()[0]
					//                System.err.printf("promoting f name to lemma %s for %s\n", last_lema, line)
				}
				line = replace_base(line, last_lema)
			}

			if( prev_line == line && (line.contains("advp:perf") || line.contains("advp:rev:perf")) )
				continue

			prev_line = line
			out_lines.add(line)
		}
		return out_lines
	}


	@TypeChecked
	def promote(String line) {
		def lemma = line.split(/ /, 2)[0]
		//    System.err.printf("promote %s -> %s\n", line, lemma)
		line = replace_base(line, lemma)
		return line
	}

	boolean isRemoveLine(String line) {
		for( removeWithTag in Args.args.removeWithTags ) {
			if( line.contains(":" + removeWithTag) )
				return true
		}
		
		if( Args.args.removeWithRegex && Args.args.removeWithRegex.matcher(line) )
			return true
			
		return false
	}

	private String removeTags(String line) {
		for( removeTag in Args.args.removeTags ) {
			if( line.contains(":" + removeTag) ) {
				line = line.replace(":" + removeTag, "")
			}
		}
		return line
	}

	private String promoteLemmaForTags(String line) {
		for( lemmaTag in Args.args.lemmaForTags ) {
			if( lemmaTag == "advp" && line.contains(lemmaTag) ) {
				line = promote(line)
			}
		}
		return line
	}


	Pattern imperf_move_pattern = ~/(verb(?::rev)?)(.*)(:(im)?perf)/
	Pattern reorder_comp_with_adjp = ~/ (adj:.:v_...(?::ranim|:rinanim)?)(.*)(:compb)(.*)/
	Pattern any_anim = ~/:([iu]n)?anim/

	//@TypeChecked
	List<String> post_process(List<String> lines) {
		def out_lines = []

		for( line in lines) {

			if( line.contains(" adv") && ! line.contains("advp") && ! line.contains(":compr") && ! line.contains(":super") ) {
				line = promote(line)
			}

			if( isRemoveLine(line) )
				continue

			line = removeTags(line)

			line = promoteLemmaForTags(line)

			if( line.contains("noun") ) {
			    def anim_matcher = any_anim.matcher(line)
				if( anim_matcher ) {
					line = anim_matcher.replaceFirst("").replace("noun", "noun" + anim_matcher[0][0])
				}
                else if( ! line.contains("&pron") ) {
                    line = line.replace("noun:", "noun:inanim:")
                }
			}
			else
			if( line.contains("verb") ) {
				line = imperf_move_pattern.matcher(line).replaceFirst('$1$3$2')
			}
			else
			if( line.contains(" adj") || line.contains(" numr") ) {
				if( line.contains(":&_adjp") && line.contains(":comp") ) {
					line = reorder_comp_with_adjp.matcher(line).replaceFirst(' $1$3$2$4')
				}

				if( line.contains("v_zn1") ) {
					line = line.replace("v_zn1", "v_zna:ranim")
				}
				else if( line.contains("v_zn2") ) {
					line = line.replace("v_zn2", "v_zna:rinanim")
				}
			}

			out_lines.add(line)
		}

		out_lines = out_lines.collect { util.tail_tag(it, [
				":v-u",
				":bad",
				":slang",
				":rare",
				":coll",
				":abbr"
			]) }   // TODO: add ") {alt"

		return out_lines
	}

	@TypeChecked
	def replace_base(String line, String base) {
		def ws = line.split()
		return ws[0] + " " + base + " " + ws[2]
	}

	//@TypeChecked
	def expand_subposition(String main_word, String line, String extra_tags, int idx_) {
		String idx = ""

		if( line.startsWith(" +cs")) {
			String word

			if( line.contains(" +cs=") ) {
				Matcher matcher = (line =~ / \+cs=([^ ]+)/)
				def m1 = matcher[0]
				word = m1[1]
			}
			else
				word = main_word[0..<-2] + "іший"

			if( extra_tags.contains("&_adjp") ) {
				extra_tags = and_adjp_pattern.matcher(extra_tags).replaceFirst('')
			}

			def word_forms = expand(word, "/adj :compr" + idx + extra_tags)

			word = "най" + word
			def word_forms_super = expand(word, "/adj :super" + idx + extra_tags)
			word_forms.addAll(word_forms_super)

			def word_scho = "що" + word
			word_forms_super = expand(word_scho, "/adj :super" + idx + extra_tags)
			word_forms.addAll(word_forms_super)

			def word_jak = "як" + word
			word_forms_super = expand(word_jak, "/adj :super" + idx + extra_tags)
			word_forms.addAll(word_forms_super)

			if( "comp" in Args.args.lemmaForTags ) {
				word_forms = word_forms.collect { replace_base(it, main_word) }
			}

			return word_forms
		}

		assert false, "Unknown subposition for " + line + "(" + main_word + ")"
	}

	@TypeChecked
	def compose_compar(String word, String main_word, String tags) {
		if( ! ("comp" in Args.args.lemmaForTags) )
			main_word = word
		return word + " "  + main_word + " " + tags
	}

	@TypeChecked
	def expand_subposition_adv_main(String main_word, String line, String extra_tags) {
		log.debug("expanding sub " + main_word + ": " + line + " extra tags: " + extra_tags)
		if( line.startsWith(" +cs")) {
			String word
			if( line.contains(" +cs=") ) {
				def matcher = Pattern.compile(" \\+cs=([^ ]+)").matcher(line)
				matcher.find()
				word = matcher.group(1)
			}
			else {
				word = main_word[0..<-1] + "іше"
			}

			def adv_compr = compose_compar(word, main_word, "adv:compr" + extra_tags)
			def adv_super = compose_compar("най" + word, main_word, "adv:super" + extra_tags)
			def adv_super2 = compose_compar("щонай" + word, main_word, "adv:super" + extra_tags)
			def adv_super3 = compose_compar("якнай" + word, main_word, "adv:super" + extra_tags)

			return [
				adv_compr,
				adv_super,
				adv_super2,
				adv_super3
			]
		}
		throw new Exception("Unknown subposition for " + line + "(" + main_word + ")")
	}

	@TypeChecked
	def expand_subposition_adv(String last_adv, String line, String extra_tags, String main_word) {
		def out_lines = []

		String word
		if( line.contains(" +cs=") ) {
			def matcher = Pattern.compile(/ \+cs=([^ ]+)/).matcher(line)
			matcher.find()
			word = matcher.group(1)
			word = word[0..<-2] + "е"
		}
		else {
			word = main_word[0..<-2] + "е"
		}

		if( extra_tags.contains("adjp") ) {
			extra_tags = and_adjp_pattern.matcher(extra_tags).replaceFirst('')
		}

		def w1 = compose_compar(word, last_adv, "adv:compr" + extra_tags)
		out_lines.add( w1 )

		def adv_super = compose_compar("най" + word, last_adv, "adv:super" + extra_tags)
		def adv_super2 = compose_compar("щонай" + word, last_adv, "adv:super" + extra_tags)
		def adv_super3 = compose_compar("якнай" + word, last_adv, "adv:super" + extra_tags)
		out_lines.addAll( [
			adv_super,
			adv_super2,
			adv_super3]
		)

		return out_lines
	}


	final Pattern word_lemma_re = Pattern.compile(" [а-яіїєґА-ЯІЇЄҐ]", Pattern.CASE_INSENSITIVE)


	@TypeChecked
	List<String> expand_line(String line_) {
		List<String> lines = preprocess(line_)

		def main_word = ""
		def out_lines = []

		for( line in lines) {
			List<String> sub_lines = []

			//  +cs
			if( line.contains("\\ +") ) {
				//            line, *sub_lines = line.split("\\")
				def parts = line.split("\\\\")
				line = parts[0]
				sub_lines = parts[1..-1]

				//			line = line.rstrip()
				line = line.replaceAll(/\s+$/, "")

				if( line.contains(" :") || ! line.contains(" /") ) {
					line += ":compb"
				}
				else {
					line += " :compb"
				}

			}
			// word lemma tags
			else if( word_lemma_re.matcher(line).find() ) {
				def exp_lines

				if( line.contains("/") ) {
					exp_lines = affix.expand_alts([line], "//")  // TODO: change this to some single-char splitter?
					exp_lines = affix.expand_alts(exp_lines, "/")
				}
				else {
					exp_lines = [line]
				}

				if( line.contains(":nv") && ! line.contains("v_") ) {
					exp_lines = util.expand_nv(exp_lines)
				}

				out_lines.addAll( exp_lines )

				continue
			}
			// word tags
			// word /flags [mods] [tags]

			String word, flags
			try {
				def parts = line.split(" ", 2)
				word = parts[0]
				flags = parts[1]
			}
			catch(Exception e) {
				throw new Exception("Failed to find flags in " + line, e)
			}

			main_word = word

			if( flags.contains("/v5") || flags.contains("/vr5") || line.contains(" p=") || line.contains(" tag=") ) {
				limitedVerbLemmas.add(word)
			}
			
			def inflected_lines = expand(word, flags)

			if( sub_lines) {
				def idx = 0
				for( sub_line in sub_lines) {
					String extra_flags = ""
					if( flags.startsWith("adv:")) {
						extra_flags = flags[3..-1].replace(":compb", "")
						//                util.dbg("sub_lines: %s, %s", flags, extra_flags)
					}
					else if( flags.contains(" :") || flags.startsWith(":") ) {
						def matcher = Pattern.compile("(^| )(:[^ ]+)").matcher(flags)
						matcher.find()
						extra_flags = matcher.group(2).replace(":compb", "")
						//                 util.dbg("===", extra_flags)
					}

					def sublines
					if( line.contains(" adv") ) {
						sublines = expand_subposition_adv_main(main_word, sub_line, extra_flags)
					}
					else {
						sublines = expand_subposition(main_word, sub_line, extra_flags, idx)
					}
					out_lines.addAll( sublines )

					if( line.contains(".adv") && line.contains("/adj") ) {
						for( inflected_line in inflected_lines) {
							if( inflected_line.contains(" adv") ) {
								def last_adv = inflected_line.split()[0]
								def cs_lines = expand_subposition_adv(last_adv, sub_line, extra_flags, main_word)
								out_lines.addAll(cs_lines)
								break
								//                    print(".adv", last_adv, file=sys.stderr)
							}
						}
					}
					idx += 1
				}
			}
			out_lines.addAll( inflected_lines )

			for( l in inflected_lines) {
				if( ! l.trim())
					throw new Exception("empty liner for " + inflected_lines)
			}
		}
		return post_process(out_lines)
	}

	int fatalErrorCount = 0
	int nonFatalErrorCount = 0

	

	//	@TypeChecked
	List<String> process_input(List<String> in_lines) {
		def time1 = System.currentTimeMillis()

		def multiline = ""
		def all_lines = []
		def double_form_cnt = 0
		def prepared_lines = []

		in_lines.each{ String line ->
			if( line.contains("#") ) {
				line = line.replaceFirst("#.*", "")
			}

//			if( "#" in line)
//				line = line.split("#")[0]

			if( ! line.trim())
				return // continue

			//        line = line.rstrip()
			line = line.replaceAll(/\s+$/, "")

			if( line.endsWith("\\")) {
				multiline += line  //.replace("\\", "")
				return // continue
			}
			else {
				if( multiline) {
					line = multiline + line
				}
				multiline = ""
			}
			
			if( line.contains("/v") && line.contains(":imperf:perf") ) {
				//                || ("/adjp" in line && "&adj" in line) ) {
				double_form_cnt += 1
			}


			if( Args.args.flush ) {
				try {
					def tag_lines = expand_line(line)
					
					validator.check_lines(tag_lines)

					def sorted_lines = dictSorter.sort_all_lines(tag_lines)
					
					List<String> indented_lines = dictSorter.indent_lines(sorted_lines)
				
	    			validator.check_indented_lines(indented_lines, limitedVerbLemmas)
				
		    		if( Args.args.indent ) {
				        sorted_lines = indented_lines
			       	}

					println(sorted_lines.join("\n") + "\n")
					
					System.out.flush()
					return
				}catch(Exception e) {
					log.error("Failed to expand \"" + line + "\": " + e.getMessage())
				}
			}
			else {
				prepared_lines << line
			}
		}

		if( Args.args.flush )
			return
			

		ParallelEnhancer.enhanceInstance(prepared_lines)

		all_lines = prepared_lines.collectParallel { String line ->

			try {
				def tag_lines = expand_line(line)
				validator.check_lines(tag_lines)

				tag_lines

			}
			catch(Exception e) {
//				throw new Exception("Exception in line: \"" + line + "\"", e)
				log.error("Failed to expand: \"" + line + "\": " + e.getMessage())
				fatalErrorCount++
			}

		}.flatten()


		if( fatalErrorCount > 0 ) {
			log.fatal(String.format("%d fatal errors found, see above, exiting...", fatalErrorCount))
			System.exit(1)
		}


		def time2
		if( Args.args.time ) {
			time2 = System.currentTimeMillis()
			log.info("Total out_lines %,d\n", all_lines.size())
			log.info("Processing time: %,d", (time2-time1))
		}

		if( ! Args.args.flush) {
			List<String> sorted_lines = dictSorter.sort_all_lines(all_lines)
			sorted_lines = post_process_sorted(sorted_lines)

			def time3
			if( Args.args.time ) {
				time3 = System.currentTimeMillis()
				log.info("Sorting time 1: %,d", (time3-time2))
			}

			if( Args.args.indent ) {
				// to sort newely promoted lemmas
				// sorted_lines.unique() is really slow
				sorted_lines = dictSorter.sort_all_lines( sorted_lines.toSet() )

				if( Args.args.time ) {
					def time4 = System.currentTimeMillis()
					log.info("Sorting time 2: %,d", (time4-time3))
				}
			}

			if( Args.args.wordlist ) {
				util.print_word_list(sorted_lines)
			}

			if( Args.args.indent ) {
				if( Args.args.mfl ) {
					new File("dict_corp_lt.txt").withWriter("utf-8") {  f ->
						sorted_lines.each{ f.println(it) }
					}
				}
				sorted_lines = dictSorter.indent_lines(sorted_lines)

				validator.check_indented_lines(sorted_lines, limitedVerbLemmas)

				if( nonFatalErrorCount > 0 ) {
					log.fatal(String.format("%d non-fatal errors found, see above", nonFatalErrorCount))
				}

				if( Args.args.stats ) {
					util.print_stats(sorted_lines, double_form_cnt)
				}
			}

			if( Args.args.log_usage ) {
				util.log_usage(affix)
			}

			return sorted_lines
		}
	}


	//----------
	// main code
	//----------
	@TypeChecked
	static void main(String[] argv) {
		Args.parse(argv)

		def expand = new Expand()
		Util util = new Util()
		
		expand.affix.load_affixes(Args.args.affixDir)

		log.info("Введіть слово з прапорцями...")


		System.in.eachLine { line->
			if( line.trim() ) {
				if( line == "exit" )
					System.exit(0)
				
				expand.process_input([line])
			}

		}
	}

}

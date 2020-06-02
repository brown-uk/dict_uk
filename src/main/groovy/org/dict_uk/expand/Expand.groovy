#!/usr/bin/env groovy

package org.dict_uk.expand

import java.util.regex.*
import java.util.stream.Collectors

import org.dict_uk.common.*

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked


class Expand {
	static Logger log = LoggerFactory.getLogger(Expand.class);

	private final Util util = new Util()
	private final DictSorter dictSorter = new DictSorter()
	private final BaseTags base_tags = new BaseTags()
	private final OutputValidator validator = new OutputValidator()
	private final List<String> limitedVerbLemmas = []
	private final Map<String, List<String>> additionalTags = [:]
	private final List<String> additionalTagsUnused = []
	final Affix affix = new Affix()
	

	static final Pattern cf_flag_pattern = ~ /(vr?)[1-6]\.cf/	 // no v5
	static final Pattern is_pattern = ~ /(vr?)[1-9]\.is/
//	Pattern default_kly_u_pattern = ~ /([^бвджзлмнпстфц]|[аеиу]р)$/
	static final Pattern default_kly_u_pattern = ~ /[^бвджзлмнпрстфц]$/ // гґйкрхчшщ
//	static final Pattern default_kly_u_soft_pattern = ~ /[аеиу]р$/
	static final Pattern default_kly_e_pattern = ~ /([бвджзлмнпстфц]|[^аи]р)$/
	
	@CompileStatic
    private static boolean isDefaultKlyU(String word, String flags) {
        return word =~ default_kly_u_pattern //\
//            || (flags.contains("n24") && word =~ default_kly_u_soft_pattern)
    }

	@CompileStatic
    private static boolean isDefaultKlyE(String word, String flags) {
        return word =~ default_kly_e_pattern
//        return ! (word =~ default_kly_u_pattern) \
//            || (!flags.contains("n24") && word =~ default_kly_u_soft_pattern)
    }


    public Expand() {
		this(true)
    }

	public Expand(boolean loadAdditionalTags) {
		if( loadAdditionalTags ) {
			new File(Args.args.dictDir + "/add_tag.add").eachLine { String line ->
				line = line.replaceFirst(/ *#.*/, '')
				if( ! line )
					return
	
				def parts = line.split(/[ :]/)
				additionalTags[parts[0]] = parts[1..-1]
				additionalTagsUnused << parts[0] + ' ' + parts[1]
			}
			log.info("Got {} additional tags", additionalTags.size())
		}
	}

	@CompileStatic
	private String adjustCommonFlag(String affixFlag2) {
		if( affixFlag2.contains(".cf") ) {
			affixFlag2 = cf_flag_pattern.matcher(affixFlag2).replaceFirst('$1.cf')
		}
		if( affixFlag2.contains(".is") ) {
			affixFlag2 = is_pattern.matcher(affixFlag2).replaceFirst('$1.is')
		}
		return affixFlag2
	}

	private static final Pattern verb_no_advp_pattern = ~/(ити|діти|слати)(ся)?$/
	
	@CompileStatic
	List<DicEntry> expand_suffixes(String word, String affixFlags, Map<String,String> modifiers, String extra) {
		//		log.info("{} {} {} {}\n", word, affixFlags, modifiers, extra)

		def affixSubGroups = affixFlags.split("\\.")
		def mainGroup = affixSubGroups[0]

		String pos = util.get_pos(mainGroup, modifiers)
		assert pos != null, "invalid $word: $mainGroup, $modifiers"
		def base_tag = base_tags.get_base_tags(word, "", affixFlags, extra)

		DicEntry base_word = new DicEntry(word, word, pos + base_tag)
		List<DicEntry> words = [base_word]
		
		if( affixFlags.startsWith("v") ) {
			String shortWord = word.replaceFirst(/ти(ся)?$/, 'ть$1')
			words.add( new DicEntry(shortWord, word, pos + base_tag + ":short") )
		}

		if( affixFlags[0] == "<" )
			return words


		def appliedCnt = 0
		Map<String, Integer> appliedCnts = [:]
		def affixFlags2 = []

		for(String affixFlag2 in affixSubGroups) {
			if( affixFlag2.contains("<") || affixFlag2 == "@" || affixFlag2 == "ikl" )
				continue

			if( affixFlag2 == "ku" && affixFlags ==~ /n2[04].*/ )
				continue
	
			if( affixFlag2 != mainGroup ) {
//				if( ! (affixFlag2 in ["v2", "vr2"]) ) {  // курликати /v1.v2.cf       задихатися /vr1.vr2
					affixFlag2 = mainGroup + "." + affixFlag2
					if( affixFlag2 == "v3.advp" && ! (verb_no_advp_pattern.matcher(word)) ) {
						affixFlag2 = "v1.advp"
					}
					else if( affixFlag2 == "v3.it0" ) {
						affixFlag2 = "v1.it0"
					}
//				}

				affixFlag2 = adjustCommonFlag(affixFlag2)
			}

			appliedCnts[affixFlag2] = 0

			//util.dbg(affix.affixMap.keySet())
			if( ! (affixFlag2 in affix.affixMap.keySet()) ) {
				throw new Exception("Could not find affix flag " + affixFlag2)
			}


			Map<String, SuffixGroup> affixGroupMap = affix.affixMap[affixFlag2]

			for(Map.Entry<String, SuffixGroup> ent in affixGroupMap.entrySet() ) {
				def match = ent.key
				SuffixGroup affixGroup = ent.value

				if( affixGroup.matches(word) ) {

					for(Suffix affixItem in affixGroup.affixes) {
						// DL - не додавати незавершену форму дієприслівника для завершеної форми дієслова
						assert pos != null
						assert extra != null, "$pos + $extra"
						if( pos.startsWith("verb") && extra.contains(":perf")
								&& (affixItem.tags.startsWith("advp:imperf")
								|| affixItem.tags.startsWith("advp:rev:imperf"))) {
							appliedCnts[ affixFlag2 ] = -1000
							continue
						}

						String deriv = affixItem.apply(word)
						String tags = affixItem.tags

						words.add(new DicEntry(deriv, word, tags))
						appliedCnt += 1
						appliedCnts[affixFlag2] += 1

						//util.debug("applied {} to {}", affixGroup, word)
					}
					affixGroup.incrementCounter()

					//      print("DEBUG: applied", affixFlags, "for", word, "got", appliedCnts, file=sys.stderr)
				}
			}

			if( appliedCnts[ affixFlag2 ] == 0 ) {
				throw new Exception("Flag " + affixFlag2 + " of " + affixFlags + " not applicable to " + word)
			}
		}

		List<DicEntry> dups = words.findAll { words.count(it) > 1 }.unique()
		if( dups.size() > 0 ) {
		    if( ! (affixFlags =~ /p1\.p2|p[12]\.piv/) ) {
			    log.warn("duplicates: " + dups + " for " + word + " " + affixFlags)
			}
		}

		return words
	}


	@CompileStatic
	private Map<String,String> get_modifiers(String mod_flags, String flags, String word) {

		def mods = [:]

		String[] mod_set = mod_flags.split(" ")

		for(String mod in mod_set) {
			if( mod.startsWith("^") ) {
				def mod_tags = mod[1..-1].split(":")
				mods["pos"] = mod_tags[0]
				if( mod_tags && mod_tags[0] == "noun") {
					if( mod_tags.size() > 1 ) {
						assert mod_tags[1].size() == 1 : "Bad gender override: " + mod + " -- " + mod_tags

						mods["force_gen"] = mod_tags[1]
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

		//    util.debug("mods {} for {} && {}", str(mods), flags, mod_flags)

		return mods
	}

	@CompileStatic
	private boolean filter_word(DicEntry entry, Map modifiers, String flags) {
		String tagStr = entry.tagStr
		
		if( "gen" in modifiers) {
			if( ! (tagStr =~ (":[" + modifiers["gen"] + "]:") ) )
				return false
		}
		
		if( "pers" in modifiers && ! ( tagStr =~ ":(inf|past:n|impers)") ) {
			def prs = ":[" + modifiers["pers"] + "]"
			if( modifiers["pers"] == "3" ) {
				prs = ":s" + prs
			}
			else { // p=4
				prs = ":3|:past|:impr:[sp]:2"
				if( flags.contains('.advp') ) {
				    prs += '|advp'
				}
			}
			if( ! (tagStr =~ prs) )
				return false
		}

		if( "tag" in modifiers ) {
			if( ! (tagStr =~ modifiers["tag"]) )
				return false
		}
		
		return true
	}

	@CompileStatic
	private List<DicEntry> modify(List<DicEntry> lines, Map<String, String> modifiers, String flags) {
		if( modifiers.size() == 0)
			return lines

		def out = []
		for(DicEntry line in lines) {

			if( ! filter_word(line, modifiers, flags)) {
				//            util.debug("skip %s %s", line, modifiers)
				continue
			}
			if( "pos" in modifiers) {
				line.tagStr = line.tagStr.replaceAll("^[^:]+", modifiers["pos"])
				//            util.debug("pos repl %s in %s", modifiers["pos"], line)
			}
			if( "force_gen" in modifiers && ! line.tagStr.contains(":pname") ) {
				def force_gen = modifiers["force_gen"]
				line.tagStr = line.tagStr.replaceAll(/:[mfn](:|$)/,  ":" + force_gen + /$1/)
				//            util.debug("gen repl: %s in %s", force_gen, line)
			}

			out.add(line)
		}

		assert out.size() > 0 : "emtpy output for "+ lines + " && " + modifiers

		return out
	}

	private static final Pattern extraFlagsPattern = ~/ (:[^ ]+)/
	
	@CompileStatic
	private String get_extra_flags(String flags) {
		def extra_flags = ""

		if( flags.contains(" :") ) {
			def matcher = extraFlagsPattern.matcher(flags)
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
	
	private static final Pattern perf_imperf_pattern = ~ ":(im)?perf"
	private static final Pattern and_adjp_pattern = ~ /:&&?adjp(:pasv|:actv|:perf|:imperf)+/

	@CompileStatic
	List<DicEntry> post_expand(List<DicEntry> lines, String flags) {
		if( lines.size() == 0)
			throw new Exception("emtpy lines")

		String extra_flags = get_extra_flags(flags)


		if( extra_flags ) {
			boolean first_name_base = util.firstname(lines[0].word, flags)

			List<DicEntry> out_lines = []
			List<DicEntry> extra_out_lines = []

			for(DicEntry line in lines) {
				String extra_flags2 = extra_flags

				if( first_name_base && ! line.tagStr.contains("pname") && ! flags.contains(":pname") ) {
					extra_flags2 += ":prop:fname"
				}
				if( line.tagStr.startsWith("advp") ) {
					if( line.tagStr.contains(":imperf") )
						extra_flags2 = perf_imperf_pattern.matcher(extra_flags2).replaceFirst("")
					else
						line.tagStr = line.tagStr.replace(":perf", "")
				}
				else if( flags.contains("adj.adv") && line.tagStr.startsWith("adv") ) {
					extra_flags2 = and_adjp_pattern.matcher(extra_flags2).replaceFirst("")
				}
				else if( extra_flags.contains(":+m") ) {
					extra_flags2 = extra_flags2.replace(":+m", "")

					if( line.tagStr.contains(":f:") ) {
						String  mascLineTags2 = line.tagStr.replace(":f:", ":m:") + extra_flags2
						extra_out_lines.add(new DicEntry(line.word, line.lemma, mascLineTags2, line.comment))
					}
					else if( line.tagStr.contains(":n:") ) {
						String mascLineTags = line.tagStr.replace(":n:", ":m:") + extra_flags2

						if( util.istota(flags)) {
							if( mascLineTags.contains("m:v_rod") ) {
								def mascLineTags2 = mascLineTags.replace("m:v_rod", "m:v_zna")
								extra_out_lines.add(new DicEntry(line.word, line.lemma, mascLineTags2, line.comment))
							}
							else if( mascLineTags.contains("m:v_zna") ) {
								mascLineTags = ""
							}

//							if( mascLineTags.contains("m:v_kly") ) {
//								extra_out_lines << new DicEntry(line.word[0..<-1] + "е", line.lemma, mascLineTags)
//								mascLineTags = null
//							}
						}
						if( mascLineTags ) {
							extra_out_lines << new DicEntry(line.word, line.lemma, mascLineTags, line.comment)
						}
					}
				}
				else if( extra_flags.contains(":+f") ) {
					extra_flags2 = extra_flags2.replace(":+f", "")

					if( line.tagStr.contains(":m:") ) {
						String masc_line = line.tagStr.replace(":m:", ":f:") + extra_flags2
						extra_out_lines.add(new DicEntry(line.word, line.lemma, masc_line, line.comment))
					}
					else if( line.tagStr.contains(":n:") ) {
						String masc_line = line.tagStr.replace(":n:", ":f:") + extra_flags2

						if( masc_line) {
							extra_out_lines.add(new DicEntry(line.word, line.lemma, masc_line, line.comment))
						}
					}
				}
				else if( line.tagStr.contains(":pname") && extra_flags2.contains(":anim") ) {
					line.tagStr = line.tagStr.replace(":pname", ":anim:pname")
					extra_flags2 = extra_flags2.replace(":anim", "")
				}
				out_lines.add(new DicEntry(line.word, line.lemma, line.tagStr + extra_flags2, line.comment))
			}

			out_lines.addAll(extra_out_lines)

			lines = out_lines
		}

		lines = adjustForGeo2019(lines, flags)
				
		return lines
	}

	static final Pattern GEO_ONLY_A = ~/([сц]ьк|ець|бур[гґ]|град|город|піль|поль|мир|слав|фурт)$/
	static final Pattern GEO_POSS_DUAL = ~/([ії]в|[еєо]в|[иі]н|[аи]ч)$/
	
	@CompileStatic
	List<DicEntry> adjustForGeo2019(List<DicEntry> lines, String flags) {
		if( lines[0].tagStr.contains(':town') ) {
			// правопис-2019: назви міст р.в. з -у
			if( lines[0].tagStr =~ /noun:m:.*?:geo.*/ ) { 
				if( GEO_POSS_DUAL.matcher(lines[0].lemma).find() ) {
					// лише присвійні суфікси не мають подвійного р.в.
					if( flags.contains(".a.u") ) {
						lines = lines.collect { l ->
							if( l.tagStr.contains("v_rod") && l.word =~ /[ую]$/ ) {
								new DicEntry(l.word, l.lemma, l.tagStr + ":ua_2019")
							}
							else {
								l
							}
						}
					}
				}
				else if( ! lines[0].tagStr.contains(':towna') && ! GEO_ONLY_A.matcher(lines[0].lemma).find() ) {
					def vRod = lines.find {
						l -> l.tagStr =~ /noun:m:v_rod.*?:geo.*/ \
							&& ! l.tagStr.contains(":nv") \
							&& ! l.lemma.endsWith("о") \
							&& l.word =~ /[ая]$/
					}
					if( vRod ) {
						def tag = vRod.tagStr
						if( ! tag.contains(":ua_2019") ) {
							tag += ":ua_2019"
						}
						def word = vRod.word.replaceFirst(/а$/, 'у').replaceFirst(/я$/, 'ю')
						lines.add(new DicEntry(word, vRod.lemma, tag))
					}
				}
			}
			lines = lines.collect { l -> new DicEntry(l.word, l.lemma, l.tagStr.replaceFirst(/:towna?/, '')) }
		}
		return lines
	}
	
	@CompileStatic
	private List<DicEntry> adjust_affix_tags(List<DicEntry> lines, String main_flag, String flags, Map<String,String> modifiers) {
		List<DicEntry> lines2 = []

		for(DicEntry line in lines) {
			// DL-
			if( main_flag[1] == "n" ) {

				String word
				String base_word
				if( main_flag.startsWith("/n2") ) {
					if( main_flag =~ "^/n2[01234]" ) {
						base_word = line.lemma

						if( util.istota(flags)) {
							if( line.tagStr.contains("m:v_rod") && ! line.tagStr.contains("/v_zna") ) {
								line.setTagStr( line.tagStr.replace("m:v_rod", "m:v_rod/v_zna") )
							}
						}
						if( ! "аеєиіїоюя".contains(base_word[-1..-1]) && ! flags.contains(".a") ) {
							word = line.word
							if( "ую".contains(word[-1..-1]) ) {
								line.setTagStr( line.tagStr.replace("v_dav", "v_rod/v_dav") )
							}
						}
					}
					else if( main_flag.startsWith("/n2adj") ) {
						if( ! util.istota(flags) ) {
							if( line.tagStr.contains("v_rod/v_zna") ) {
								line.tagStr = line.tagStr.replace("/v_zna", "")
							}
						}
						else {
						/*
							if( flags.contains("<+") && ! flags.contains(".k") ) { // && line.lemma.endsWith("ів")
								if( line.tagStr.contains("v_kly") )
								    if( ! line.tagStr.contains("/v_kly") )
								        continue;
								    else
								        line.tagStr	 = line.tagStr.replace("/v_kly", "")
							}
                        */
							if( util.person(flags) ) {
								if( line.tagStr.contains("noun:p:v_zna") )
									continue

								line.tagStr = line.tagStr.replace("p:v_naz/v_zna", "p:v_naz")
							}
						}
					}
					else if( main_flag.startsWith("/n2nm") ) {
						if( util.istota(flags)) {
							if( line.tagStr.contains("m:v_rod") && ! line.tagStr.contains("/v_zna") ) {
								line.tagStr = line.tagStr.replace("m:v_rod", "m:v_rod/v_zna")
							}
						}
					}

					if( flags.contains("@") ) {
						word = line.word
						if( "ая".contains(word[-1..-1]) && line.tagStr.contains("m:v_rod") ) {
							lines2.add(line)
							String newTag = line.tagStr.replace("m:v_rod", "m:v_zna:var")
							lines2.add(new DicEntry(line.word, line.lemma, newTag))
							continue
						}
					}
				}

				
				if( ! main_flag.contains("np") && ! main_flag.contains(".p") \
				        && ! flags.contains("n2adj") && ! main_flag.contains("numr") ) {
					if( line.tagStr.contains(":p:") ) {
						// log.debug("skipping line with p: " + line)
					}
					else if( line.tagStr.contains("//p:") ) {
						line.setTagStr( line.tagStr.replaceAll("//p:.*", "") )
						// log.debug("removing //p from: " + line)
					}
				}
				
				if( line.tagStr.contains("/v_kly") ) {
					
					boolean klyKeKo = flags.contains(".ko") || flags.contains(".ke")
					if( klyKeKo ) {
						if( ! flags.contains('.ku') ) {
							line.setTagStr( line.tagStr.replace('/v_kly', '') )
						}
					}
					else {
						if( (! flags.contains(".ku") && main_flag =~ /n2[0-4]/ && ! isDefaultKlyU(base_word, flags))
                            	/*|| (line.tagStr.contains(":m:") && flags.contains("<+") )*/ ) {
//						log.info("removing v_kly from: %s, %s", line, flags)
							line.setTagStr( line.tagStr.replace("/v_kly", "") )
						}
					}
				}
				
				if( main_flag.contains(".p") || main_flag.contains("np") ) {
//					if( util.person(flags)) {
					if( main_flag.contains(".") ) {
						line.setTagStr( line.tagStr.replace("p:v_naz", "p:v_naz/v_kly") )
					}
					
					if( util.istota(flags) ) {
						line.setTagStr( line.tagStr.replace("p:v_rod", "p:v_rod/v_zna") )
						if( flags.contains(">") ) { // animal
							line.setTagStr( line.tagStr.replace("p:v_naz", "p:v_naz/v_zna") )
						}
					}
					else {
						line.setTagStr( line.tagStr.replace("p:v_naz", "p:v_naz/v_zna") )
					}
				}
			}
			else if( flags.contains(":perf") && line.tagStr.contains(":pres") ) {
				line.setTagStr( line.tagStr.replace(":pres", ":futr") )
			}
			else if( main_flag.startsWith("/adj") ) {
				if( flags.contains("<") || flags.contains("^noun") ) {
					if( line.tagStr.contains(":uncontr") )
						continue
				}

				if( flags.contains(":&pron") && ! (line.lemma in ["мій", "твій", "наш", "ваш"]) ) {
					line.setTagStr( line.tagStr.replace("/v_kly", "") )
				}

				
				if( flags.contains("<") ) {
					if( ! flags.contains(">") && line.tagStr.contains(":p:v_naz/v_zna") )
						line.setTagStr( line.tagStr.replace("v_naz/v_zna", "v_naz") )
//					if( ":m:v_naz" in line /*&& ! ("<+" in flags)*/)
//						line = line.replace("v_naz", "v_naz/v_kly")
				}
				else if( flags.contains("^noun") ) {
					if( line.tagStr.contains(":m:v_rod/v_zna") ) {
						line.setTagStr( line.tagStr.replace("v_rod/v_zna", "v_rod") )
					}
					else if( line.tagStr.contains(":p:v_rod/v_zna") ) {
						line.setTagStr( line.tagStr.replace("v_rod/v_zna", "v_rod") )
					}
				}

			}
			
			lines2.add(line)
		}
		return lines2
	}
	
	private static final Pattern unexpectedChars = Pattern.compile(/[^а-яіїєґА-ЯІЇЄҐ'.-]/)

	@CompileStatic
	List<DicEntry> expand(String word, String flags) {
		String[] flag_set = flags.split(" ", 2)

		String main_flag = flag_set[0]

		String extra = flag_set.size() > 1 ? flag_set[1] : ""

		Map<String,String> modifiers = get_modifiers(extra, flags, word)

		List<DicEntry> sfx_lines

		if( main_flag[0] == "/" ) {
			def inflection_flag = main_flag[1..-1]
			sfx_lines = expand_suffixes(word, inflection_flag, modifiers, extra)
			sfx_lines = adjust_affix_tags(sfx_lines, main_flag, flags, modifiers)
		}
		else {
			sfx_lines = [
				new DicEntry(word, word, flags)
			]
		}

		sfx_lines = AffixUtil.expand_alts(sfx_lines, "//")  // TODO: change this to some single-char splitter?
		sfx_lines = AffixUtil.expand_alts(sfx_lines, "/")

		if( flags.contains("/adj") ) {
			def out_lines = []
			for( line in sfx_lines) {
				if( line.tagStr.contains("v_zn1") ) { // v_zna == v_rod
                    if( flags.contains("<") ) {
                        line.tagStr = line.tagStr.replace("v_zn1", "v_zna")
                    }
                    else if( flags.contains("^noun") ) {
                        continue
					}
					else {
						line.tagStr = line.tagStr.replace("v_zn1", "v_zna:ranim")
					}
				}
				else if( line.tagStr.contains("v_zn2") ) { // v_zna = v_naz
					if( flags.contains("<") && (! flags.contains(">") || line.tagStr.contains(":m:")) ) {
						continue
					}
					else if( flags.contains("^noun") ) {
						line.tagStr = line.tagStr.replace("v_zn2", "v_zna")
					}
					else {
						line.tagStr = line.tagStr.replace("v_zn2", "v_zna:rinanim")
					}
				}
				out_lines.add(line)
			}
			sfx_lines = out_lines
		}
		
		if( main_flag[0] != "/" ) {
			sfx_lines = util.expand_nv(sfx_lines)
		}
		
		sfx_lines = modify(sfx_lines, modifiers, flags)

		List<DicEntry> entries = post_expand(sfx_lines, flags)

		entries.each {
			if( unexpectedChars.matcher(it.word) || unexpectedChars.matcher(it.lemma) )
				throw new Exception("unexpected characters in " + it)
		}

		return entries
	}

	// Дієприслівники, утворені від зворотних дієслів, мають постфікс -сь сміючи́сь, узя́вшись; рідше — -ся: сміючи́ся, узя́вшися.
	// https://r2u.org.ua/pravopys/pravXXI/93.html
	@CompileStatic
    private static List<DicEntry> getRareAdvp(List<DicEntry> entries) {
		return entries.findAll {
			it.tagStr.startsWith('advp:rev')
		}
		.collect {
			String tag = it.tagStr
			if( ! tag.contains(':rare') ) {
				tag += ':rare'
			}
			String word = it.word.replaceFirst(/сь$/, 'ся')
			new DicEntry(word, it.lemma, tag)
		}
	}

	// this method adds some special tags to the word forms generated by expand
	@CompileStatic
    private void applyAdditionalTags(List<DicEntry> dicEntries) {
        for(DicEntry dicEntry: dicEntries) {

			if( ! (dicEntry.word in additionalTags) ) {
				continue
			}

			List<String> addTags = additionalTags[dicEntry.word]
			if( addTags[0] in ['noun', 'adj']
					&& ! addTags.contains("ua_2019")
					&& ! (dicEntry.tagStr =~ /(noun|adj).*:[mfn]:v_naz/) )
				continue

			String xpTag = addTags.find { it.startsWith 'xp' }
			if( xpTag ) {
				if( ! dicEntry.tagStr.contains(xpTag) )
					continue

				addTags.remove(xpTag)
			}

			String addTagsStr = ':' + addTags[1..-1].join(':')
			log.debug("Applying {} to {}", addTagsStr, dicEntry.toFlatString())
			dicEntry.tagStr += addTagsStr
			additionalTagsUnused.remove(dicEntry.word + ' ' + dicEntry.tags[0])
        }
    }


//	private static final Pattern tag_split0_re = Pattern.compile(/[^ ]+$/)

	@CompileStatic
	public List<LineGroup> preprocess(LineGroup lineGroup) {
		List<LineGroup> lineGroups

		if( lineGroup.line.count(" /") > 1 ) {
			String[] parts = lineGroup.line.split(" ")

			def line1 = parts[0..<2]
			if( parts.size() > 3 ) {
				line1 += parts[3..-1]
			}
			
			def line2 = parts[0..<1] + parts[2..-1]
			
			lineGroups = [
				new LineGroup(lineGroup, line1.join(" ")),
				new LineGroup(lineGroup, line2.join(" "))
			]
		}
		else if( lineGroup.line.contains("|") && ! lineGroup.line.contains(" tag=") ) {
			def parts = lineGroup.line.split(/ /)
			def base = parts[0..-2].join(" ")
			lineGroups = Arrays.asList(parts[-1].split(/\|/)).collect{ new LineGroup(lineGroup, base + " " + it) }
		}
		// split patr into separate lemma groups
		else if ( lineGroup.line.contains(".patr") ) {
			lineGroups = [new LineGroup(lineGroup, lineGroup.line.replaceFirst(/\.patr/, ''))]

			try {
				String[] parts = lineGroup.line.split(" ", 2)
				def extra = parts[1].contains(" :") 
					?  parts[1].replaceFirst(/.*? :/, ':').replaceAll(/:xp[0-9]/, '').replaceFirst(/ *#.*/, '') 
					: ""

				def expanded = expand_suffixes(parts[0], "patr.<", [:], "")
				
				def mascPatrGroups = expanded
				.findAll{ it.tagStr.contains(':m:v_naz') }
				.collect { new LineGroup(lineGroup, it.word + " /n20.a.< :prop:pname" + extra, null) }
				assert mascPatrGroups.size() >= 1 && mascPatrGroups.size() <= 2

				lineGroups += mascPatrGroups
				
				def femPatrGroups = expanded
				.findAll{ it.tagStr.contains(':f:v_naz') }
				.collect { new LineGroup(lineGroup, it.word + " /n10.< :prop:pname" + extra, null) }

				assert femPatrGroups.size() == 1

				lineGroups += femPatrGroups
			}
			catch(e) {
				e.printStackTrace()
			}
		}
		else {
			lineGroups = [lineGroup]
		}

		def out_lines = []
		for(LineGroup lineGroup2 in lineGroups) {
			out_lines.addAll(
				preprocess2(lineGroup2.line)
				.collect { String line ->
					new LineGroup(lineGroup2, line)
				}
			)
		}

		return out_lines
	}

	private static final Pattern plus_f_pattern = ~ "/<\\+?f?( (:[^ ]+))?"
	private static final Pattern plus_m_pattern = ~ "/<\\+?m?( (:[^ ]+))?"
	
	@CompileStatic
	private List<String> preprocess2(String line) {
		List<String> out_lines = []

		String[] lineParts = line.split()
		String flags = lineParts[1]
		String word = lineParts[0]
		
		
		if( flags.startsWith("/<") ) {
		
			def extra_tag = ":anim"
			if( flags.contains("<+") ) {
				extra_tag += ":prop:lname"
			}
			else {
			    if( Character.isUpperCase(line.charAt(0)) ) {
				    extra_tag += ":prop:fname"
			    }
			}

			if( ! flags.contains("<m") && ! flags.contains("<+m") ) {
				def tag = "noun:f:nv:np"
				def line1 = plus_f_pattern.matcher(line).replaceFirst(tag + extra_tag + '$2')
				out_lines.add(line1)
			}
			if( ! flags.contains("<f") && ! flags.contains("<+f") ) {
				def tag = "noun:m:nv:np"
				def line1 = plus_m_pattern.matcher(line).replaceFirst(tag + extra_tag + '$2')
				out_lines.add(line1)
			}
		}
		else if( flags.startsWith("/n2") && flags.contains("<+") ) {
			if( ! flags.contains("<+m") && util.dual_last_name_ending(line)) {
				out_lines.add(line)
				def line_fem_lastname = word + " noun:f:nv:np:anim:prop:lname"
				
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
		else if( flags.startsWith("/n1") && flags.contains("<+") ) {
			if( ! flags.contains("<+f") && ! flags.contains("<+m") ) {
				out_lines.add(line)
				def line_masc_lastname = line.replace("<+", "<+m")
				out_lines.add(line_masc_lastname)
			}
			else {
				out_lines = [line]
			}
		}
		else if( flags.startsWith("/n10") || flags.startsWith("/n3") ) {
			if( ! flags.contains(".k") ) {
			    lineParts[1] += flags.startsWith("/n10") ? ".ko" : ".ke"
			    line = lineParts.join(" ")
			}
			out_lines = [line]
		}
		else if( flags =~ '^/n2[0-4]' && ! flags.contains(".k") ) {
			if( isDefaultKlyE(word, flags) ) {
//			    System.err.println(" .ke == " + line)
			    lineParts[1] += ".ke"
			    line = lineParts.join(" ")
		    }
			out_lines = [line]
		}
		else if( flags.startsWith("/np") ) {
			if( ! line.contains(" :") ) {
				line += " "
			}
			line = line + ":ns"
			out_lines = [line]
		}
		else if( lineParts.length > 2 && ! line.contains("adjp") && line.contains(":imperf:perf") ) {
			def line1 = line.replace(":perf", "")
			def line2 = line.replace(":imperf", "").replace(".cf", "").replace(".adv ", " ") // so we don't duplicate cf and adv
			out_lines = [line1, line2]
		}
		else {
			out_lines = [line]
		}
		
		return out_lines
	}

//	private static final Pattern PATTR_BASE_LEMMAN_PATTERN = ~ ":[mf]:v_naz:.*?pname"
	
	@CompileStatic
	private List<DicEntry> post_process_sorted(List<DicEntry> lines) {
		def out_lines = []

		def prev_line = ""
		def last_lemma
		for(DicEntry line in lines) {
//			if( line.tagStr.contains(":pname") ) {
//				if( PATTR_BASE_LEMMAN_PATTERN.matcher(line.tagStr).find() ) {
//					last_lemma = line.word //split()[0]
//					//                System.err.printf("promoting patr to lemma %s for %s\n", last_lema, line)
//				}
////				line = replace_base(line, last_lemma)
//				line.lemma = last_lemma
//			}
//			else 
			if( line.tagStr.contains("lname")
					&& line.tagStr.contains(":f:") 
					&& ! line.tagStr.contains(":nv") ) {
				if( line.tagStr.contains(":f:v_naz") ) {
					last_lemma = line.word //split()[0]
					//                System.err.printf("promoting f name to lemma %s for %s\n", last_lema, line)
				}
//				line = replace_base(line, last_lemma)
				line.lemma = last_lemma
			}

			if( prev_line == line 
					&& (line.tagStr.contains("advp:perf") || line.tagStr.contains("advp:rev:perf")) )
				continue

			prev_line = line
			out_lines.add(line)
		}
		return out_lines
	}


	@CompileStatic
	private DicEntry promote(DicEntry line) {
		//    System.err.printf("promote %s -> %s\n", line, lemma)
		return new DicEntry(line.word, line.word, line.tagStr, line.comment)
	}

	@CompileStatic
	private boolean isRemoveLine(DicEntry line) {
		for( removeWithTag in Args.args.removeWithTags ) {
			if( line.tagStr.contains(":" + removeWithTag) )
				return true
		}
		
		if( Args.args.removeWithRegex && Args.args.removeWithRegex.matcher(line.tagStr) )
			return true
			
		return false
	}

	@CompileStatic
	private DicEntry removeTags(DicEntry line) {
		for(String removeTag in Args.args.removeTagsWithColons) {
			if( line.tagStr.contains(removeTag) ) {
				line.tagStr = line.tagStr.replace(removeTag, "")
			}
		}
		return line
	}

	@CompileStatic
	private DicEntry promoteLemmaForTags(DicEntry line) {
		for(String lemmaTag in Args.args.lemmaForTags ) {
			if( lemmaTag == "advp" && line.tagStr.contains(lemmaTag) ) {
				line = promote(line)
			}
		}
		return line
	}


	private static final Pattern imperf_move_pattern = ~/(verb(?::rev)?)(.*)(:(im)?perf)/
	private static final Pattern reorder_comp_with_adjp = ~/^(adj:.:v_...(?::ranim|:rinanim)?)(.*)(:compb)(.*)/
	private static final Pattern any_anim = ~/:([iu]n)?anim/

	@CompileStatic
	private List<DicEntry> post_process(List<DicEntry> lines) {
		List<DicEntry> out_lines = []

		for(DicEntry line in lines) {

			if( line.tagStr.startsWith("adv") 
					&& ! line.tagStr.contains("advp") 
					&& ! line.tagStr.contains(":compc") 
					&& ! line.tagStr.contains(":comps") ) {
				line = promote(line)
			}

			if( isRemoveLine(line) )
				continue

			line = removeTags(line)

			line = promoteLemmaForTags(line)

			if( line.tagStr.startsWith("noun") ) {
			    Matcher anim_matcher = any_anim.matcher(line.tagStr)
				if( anim_matcher.find() ) {
					line.tagStr = anim_matcher.replaceFirst("").replace("noun", "noun" + anim_matcher.group(0))
				}
                else if( ! line.tagStr.contains("&pron") ) {
                    line.tagStr = line.tagStr.replace("noun:", "noun:inanim:")
                }
			}
			else
			if( line.tagStr.contains("verb") ) {
				line.tagStr = imperf_move_pattern.matcher(line.tagStr).replaceFirst('$1$3$2')
			}
			else
			if( line.tagStr.startsWith("adj") || line.tagStr.startsWith("numr") ) {
				if( line.tagStr.contains(":&&adjp") && line.tagStr.contains(":comp") ) {
					line.tagStr = reorder_comp_with_adjp.matcher(line.tagStr).replaceFirst('$1$3$2$4')
				}

				if( line.tagStr.contains("v_zn1") ) {
					line.tagStr = line.tagStr.replace("v_zn1", "v_zna:ranim")
				}
				else if( line.tagStr.contains("v_zn2") ) {
					line.tagStr = line.tagStr.replace("v_zn2", "v_zna:rinanim")
				}
			}

			out_lines.add(line)
		}

		out_lines = out_lines.collect { util.tail_tag(it, tagsOrdered) }   // TODO: add ") {alt"

		return out_lines
	}
	
	private static final List<String> tagsOrdered = [
	            ":&numr",
	            ":&insert",
	            ":&predic",
//				":v-u",
				":prop",
				":geo", ":abbr", ":fname", ":lname", ":pname",
				":bad", ":subst", ":slang", ":rare", ":coll", ":alt", ":ua_2019", ":ua_1992", ":short",
				":xp1", ":xp2", ":xp3", ":xp4",
			]

	@CompileStatic
	private DicEntry replace_base(DicEntry line, String base) {
		return new DicEntry(line.word, base, line.tagStr, line.comment)
	}

	@CompileStatic
	private List<DicEntry> expand_subposition(String main_word, String line, String extra_tags, int idx_) {
		String idx = ""

		if( line.startsWith(" +cs") ) {
			String word

            if( extra_tags.contains(":&numr") ) {
                extra_tags = extra_tags.replace(':&numr', '')
            }

			if( line.contains(" +cs=") ) {
				Matcher matcher = (line =~ / \+cs=([^ ]+)/)
				matcher.find()
				word = matcher.group(1)
				
				if( word.endsWith('е') && main_word.endsWith('й') ) {
					log.error "bad +cs: $word for $main_word"
					System.exit(1)
				}
			}
			else {
				word = main_word[0..<-2] + "іший"
            }
            
			if( extra_tags.contains("&adjp") ) {
				extra_tags = and_adjp_pattern.matcher(extra_tags).replaceFirst('')
			}

            List<DicEntry> forms = []

            if( word.startsWith('най') ) {
			    forms += expand(word, "/adj :comps" + idx + extra_tags)
            }
            else {
			    forms += expand(word, "/adj :compc" + idx + extra_tags)

			    word = "най" + word
			    forms += expand(word, "/adj :comps" + idx + extra_tags)
		    }

            forms += expand("що" + word, "/adj :comps" + idx + extra_tags)
			forms += expand("як" + word, "/adj :comps" + idx + extra_tags)
			forms += expand("щояк" + word, "/adj :comps" + idx + extra_tags)

			if( "comp" in Args.args.lemmaForTags ) {
				forms = forms.collect { DicEntry entry -> 
					replace_base(entry, main_word) 
				}
			}

			return forms
		}

		assert false, "Unknown subposition for " + line + " (" + main_word + ")"
	}

	@CompileStatic
	private DicEntry compose_compar(String word, String main_word, String tags) {
		if( ! ("comp" in Args.args.lemmaForTags) ) {
			main_word = word
		}
		
		return new DicEntry(word, main_word, tags)
	}

	@CompileStatic
	private List<DicEntry> expand_subposition_adv_main(String main_word, String line, String extra_tags) {
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

            List<DicEntry> forms = []

            if( word.startsWith('най') ) {
    			forms += compose_compar(word, main_word, "adv:comps" + extra_tags.replaceAll(':&insert', ''))
            }
            else {
			    forms += compose_compar(word, main_word, "adv:compc" + extra_tags.replaceAll(':&insert', ''))
			    word = 'най' + word
    			forms += compose_compar(word, main_word, "adv:comps" + extra_tags.replaceAll(':&insert', ''))
			}
			forms += compose_compar("що" + word, main_word, "adv:comps" + extra_tags.replaceAll(':&(insert|predic)', ''))
			forms += compose_compar("як" + word, main_word, "adv:comps" + extra_tags.replaceAll(':&(insert|predic)', ''))

			return forms
		}

		throw new Exception("Unknown subposition for " + line + "(" + main_word + ")")
	}

	@CompileStatic
	private List<DicEntry> expand_subposition_adv(String last_adv, String line, String extra_tags, String main_word) {

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

		List<DicEntry> forms = []

        if( word.startsWith('най') ) {
		    forms += compose_compar(word, last_adv, "adv:comps" + extra_tags)
        }
        else {
		    forms +=  compose_compar(word, last_adv, "adv:compc" + extra_tags)
		    word = 'най' + word
		    forms += compose_compar(word, last_adv, "adv:comps" + extra_tags)
		}


		forms += compose_compar("що" + word, last_adv, "adv:comps" + extra_tags)
		forms += compose_compar("як" + word, last_adv, "adv:comps" + extra_tags)

		return forms
	}


	private final static Pattern word_lemma_re = Pattern.compile(" [а-яіїєґА-ЯІЇЄҐ]")

	@CompileStatic
	List<DicEntry> expand_line(String line) {
		return expand_line(new LineGroup(line))
	}

	@CompileStatic
	List<DicEntry> expand_line(LineGroup lineGroup) {
		List<LineGroup> lines = preprocess(lineGroup)

		def main_word = ""
		List<DicEntry> outEntries = []

		for(LineGroup lineGroup2 in lines) {
			List<String> sub_lines = []

			//  +cs
			if( lineGroup2.extraLines ) {
				sub_lines = lineGroup2.extraLines
				
				if( lineGroup2.line.contains(" :") || ! lineGroup2.line.contains(" /") ) {
					lineGroup2.line += ":compb"
				}
				else {
					lineGroup2.line += " :compb"
				}

			}
			// word lemma tags
			else if( word_lemma_re.matcher(lineGroup2.line).find() ) {
				List<DicEntry> exp_lines

				if( lineGroup2.line.contains("/") ) {
					exp_lines = AffixUtil.expand_alts([DicEntry.fromLine(lineGroup2.line, lineGroup2.comment)], "//")
					exp_lines = AffixUtil.expand_alts(exp_lines, "/")
				}
				else {
					exp_lines = [DicEntry.fromLine(lineGroup2.line, lineGroup2.comment)]
				}

				outEntries.addAll( exp_lines )

				continue
			}

			// word tags
			// word /flags [mods] [tags]

			String word, flags
			try {
				String[] parts = lineGroup2.line.split(" ", 2)
				word = parts[0]
				flags = parts[1]
			}
			catch(Exception e) {
				throw new Exception("Failed to find flags in " + lineGroup2, e)
			}

			main_word = word

			if( flags.contains("/v5") || flags.contains("/vr5") || lineGroup2.line.contains(" p=") || lineGroup2.line.contains(" tag=") ) {
				limitedVerbLemmas.add(word)
			}
			
			List<DicEntry> inflected_lines = expand(word, flags)
			inflected_lines[0].comment = lineGroup.comment

			if( sub_lines ) {
				def idx = 0
				for(String sub_line in sub_lines) {
					
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

					List<DicEntry> sublines
					if( lineGroup2.line.contains(" adv") ) {
						sublines = expand_subposition_adv_main(main_word, sub_line, extra_flags)
					}
					else {
						sublines = expand_subposition(main_word, sub_line, extra_flags, idx)
					}
					outEntries.addAll( sublines )

					if( lineGroup2.line.contains(".adv") && lineGroup2.line.contains("/adj") ) {
						for( inflected_line in inflected_lines) {
							if( inflected_line.tagStr.startsWith("adv") ) {
								def last_adv = inflected_line.word
								def cs_lines = expand_subposition_adv(last_adv, sub_line, extra_flags, main_word)
								outEntries.addAll(cs_lines)
								break
								//                    print(".adv", last_adv, file=sys.stderr)
							}
						}
					}
					idx += 1
				}
			}

			outEntries.addAll( inflected_lines )

			for(DicEntry l in inflected_lines) {
				if( ! l.isValid() )
					throw new Exception("empty liner for " + inflected_lines)
			}
		}
			
        applyAdditionalTags(outEntries)

		outEntries.addAll(getRareAdvp(outEntries))

		outEntries = post_process(outEntries)
		
		
		return outEntries
	}

	
	private int fatalErrorCount = 0
	private int nonFatalErrorCount = 0
	private int double_form_cnt = 0

	

	@TypeChecked
	List<DicEntry> process_input(List<String> inputLines) {
		List<LineGroup> prepared_lines = []
		LineGroup lineGroup = new LineGroup()
		
		inputLines.each { String line ->
			String cmnt = null
			
			if( line.contains("#") ) {
				String[] parts = line.split("#", 2)
				line = parts[0]
//				cmnt = parts[1].replaceFirst(/rv_...(:rv_...)*/, '').trim()
				cmnt = parts[1].trim()
			}

			line = line.replaceAll(/\s+$/, '')		// .rstrip()

			if( ! line )
				return

			if( line.startsWith(' +cs=') ) {
			    assert lineGroup.extraLines != null, "Failed to find proper base for comparative at '$line' with base: '${lineGroup.line}'"

//			    println ":: " + line
				lineGroup.extraLines << line.replaceFirst(/\s*\\.*/, '')
				return
			}
			
			lineGroup = new LineGroup(line)
			lineGroup.comment = cmnt ? cmnt : null
			
			if( line.endsWith("\\") ) {
//    			println "** " + line
				lineGroup.extraLines = []
				lineGroup.line = lineGroup.line.replaceFirst(/\s*\\.*/, '')
			}
			
			if( line.contains("/v") && line.contains(":imperf:perf") ) {
				double_form_cnt += 1
			}
			if( line =~ / \/[a-z].*:bad|:slang|:alt/ ) {
				double_form_cnt += 1
			}

			prepared_lines << lineGroup
		}

		List<DicEntry> allEntries  = processInParallel(prepared_lines)


        fatalErrorCount += allEntries.count({ it == null})

        if( fatalErrorCount > 0 )
            return allEntries

		if( additionalTagsUnused ) {
			log.error("Additional tags not used: " + additionalTagsUnused)
		}
			
		return sortAndPostProcess(allEntries)
	}

	@CompileStatic
	private List<DicEntry> processInParallel(List<LineGroup> preparedLines) {
		List<DicEntry> allEntries = preparedLines.parallelStream().map { LineGroup lineGroup ->

			try {
				List<DicEntry> taggedEntries = expand_line(lineGroup)

				if( validator.checkEntries(taggedEntries) > 0 ) {
					taggedEntries = null
				}

				return taggedEntries
			}
			catch(Exception e) {
				log.error("Failed to expand: \"" + lineGroup.line + "\": " + e.getMessage())
				return null
			}

		}
		.flatMap{ s -> s.stream() }
		.collect(Collectors.toList())
	}


	@CompileStatic
	private List<DicEntry> sortAndPostProcess(List allEntries) {
		if( Args.args.time ) {
			log.info("Sorting...\n")
		}

		List<Long> times = []
		times << System.currentTimeMillis()
		
		// fisrt sort so post-process can see lemmas togther
		List<DicEntry> sortedEntries = dictSorter.sortEntries(allEntries)

		sortedEntries = post_process_sorted(sortedEntries)

		// we need to sort again after we post-processed
		sortedEntries = dictSorter.sortEntries(sortedEntries)

		if( Args.args.time ) {
			def time = System.currentTimeMillis()
			log.info("Sorting time: {}", (time-times[-1]))
			times << time
		}
		
		return sortedEntries
	}

	@CompileStatic
	void processInputAndPrint(List<String> inputLines) {
		List<Long> times = []
		times << System.currentTimeMillis()

		List<DicEntry> sortedEntries = process_input(inputLines)
		
		if( fatalErrorCount > 0 ) {
			log.error("{} fatal errors found, see above, exiting...", fatalErrorCount)
			System.exit(1)
		}


		if( Args.args.time ) {
			def time = System.currentTimeMillis()
			log.info("Total out_lines {}\n", sortedEntries.size())
			log.info("Processing time: {}", (time-times[-1]))
			times << time
		}

		if( Args.args.wordlist ) {
			util.print_word_list(sortedEntries)
			
			if( Args.args.time ) {
				def time = System.currentTimeMillis()
				log.info("Word list time: {}", (time-times[-1]))
				times << time
			}
		}

		log.info("Writing output files...")

		if( Args.args.mfl ) {
			new File("dict_corp_lt.txt").withWriter("utf-8") { Writer f ->
				sortedEntries.each{
					f.write(it.toFlatString())
					f.write('\n')
				}
			}
			if( Args.args.time ) {
				def time = System.currentTimeMillis()
				log.info("Write dict_corp_lt time: {}", (time-times[-1]))
				times << time
			}
		}

		if( Args.args.indent ) {

			List<String> indentedLines = dictSorter.indent_lines(sortedEntries)

			validator.check_indented_lines(indentedLines, limitedVerbLemmas)

			if( Args.args.time ) {
				def time = System.currentTimeMillis()
				log.info("Indent lines time: {}", (time-times[-1]))
				times << time
			}

			if( nonFatalErrorCount > 0 ) {
				log.error("{} non-fatal errors found, see above", nonFatalErrorCount)
			}

			new File("dict_corp_vis.txt").withWriter("utf-8") { Writer f ->
				indentedLines.each { String it ->
					f.write(it)
					f.write('\n')
				}
			}

			if( Args.args.time ) {
				def time = System.currentTimeMillis()
				log.info("Write dict_corp_vis time: {}", (time-times[-1]))
				times << time
			}

			if( Args.args.stats ) {
				util.print_stats(indentedLines, double_form_cnt)
			}
		}

		if( Args.args.log_usage ) {
			util.log_usage(affix)
		}

	}
		
	@CompileStatic
	void processLineByLine(String line) {
		try {
			List<DicEntry> taggedEntries = expand_line(line)
			
			validator.checkEntries(taggedEntries)

			List<DicEntry> sortedLines = dictSorter.sortEntries(taggedEntries)

			List<String> outLines

			if( Args.args.indent ) {
				List<String> indented_lines = dictSorter.indent_lines(sortedLines)
				validator.check_indented_lines(indented_lines, limitedVerbLemmas)
				outLines = indented_lines
			   }
			else {
				outLines = sortedLines.collect { it.toFlatString() }
			}

			println(outLines.join("\n") + "\n")
			
			System.out.flush()
			return
		}catch(Exception e) {
			log.error("Failed to expand \"" + line + "\": " + e.getMessage())
		}

	}


	//----------
	// main code
	//----------
	@TypeChecked
	static void main(String[] argv) {
		Args.parse(argv)

		def expand = new Expand(false)
		Util util = new Util()
		
		expand.affix.load_affixes(Args.args.affixDir)

		log.info("Введіть слово з прапорцями...")


		System.in.eachLine { line->
			if( line.trim() ) {
				if( line == "exit" )
					System.exit(0)
				
				expand.processLineByLine(line)
			}

		}
	}

}

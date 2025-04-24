#!/usr/bin/env groovy

package org.dict_uk.expand

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.*
import java.util.stream.Collectors

import org.dict_uk.common.*

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked


@CompileStatic
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
//	private final Map<String, Set<String>> derivs = [:].withDefault { new HashSet<>() }
    private final Map<String, Set<String>> derivatives = java.util.Collections.synchronizedMap([:].withDefault { new HashSet<>() })
    

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
	String adjustCommonFlag(String affixFlag2) {
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
		
		// sometimes it0 is implicit
//		if( affixFlags.contains("it1b") )
//		  assert affixFlags.contains("it0"), "Mismatch it1b/it0 in $word $affixFlags"
		if( affixFlags.contains("it0b") )
		  assert affixFlags.contains("it1"), "Mismatch it1b/it0 in $word $affixFlags"

		String pos = util.get_pos(mainGroup, modifiers)
		assert pos != null, "invalid $word: $mainGroup, $modifiers"
		def base_tag = base_tags.get_base_tags(word, "", affixFlags, extra)

		DicEntry base_word = new DicEntry(word, word, pos + base_tag)
		List<DicEntry> words = [base_word]
		
//		if( affixFlags.startsWith("v") && word =~ /.[аеєиіїоуюя]ти(ся)?$/ ) {
//			String shortWord = word.replaceFirst(/ти(ся)?$/, 'ть$1')
//			words.add( new DicEntry(shortWord, word, pos + base_tag + ":short") )
//		}

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
					else if( affixFlag2 == "n2nm.ovi" ) {
						affixFlag2 = "n2n.ovi"
					}
					else if( affixFlag2 == "n2nf.ovi" ) {
						affixFlag2 = "n2n.ovi"
					}
//					else if ( affixFlag2 == "adj_pron.long" ) {
//						affixFlag2 = "adj.long"
//					}
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

						// do not generate :coll and :rare forms for :bad
//						if( extra.contains(":bad") 
//								&& word == "заняти"
//								&& affixItem.tags =~ /:(short|long|subst)/ ) {
//							appliedCnts[affixFlag2] += 1
//							continue
//						}
		
						String deriv = affixItem.apply(word)
						String tags = affixItem.tags
						String comment = null

						// надоїсти та надоїти генерують надоївши, але від першого має бути :bad
						// TODO: we should also add comment to the original advp too - and this does not work with multithreading :(
//						if( tags.startsWith("advp") 
//								&& ! affixFlags.contains(":imperf:perf") && ! affixFlags.contains(":xp") ) {
//							int toPos = tags.indexOf("perf")+4
//							String key = deriv + "/" + tags[0..<toPos]
//
//							if( derivs.containsKey(key) ) {
//								if( ! derivs[key].contains("від " + word) ) {
//									tags += ":xp" + derivs[key].size()
//									comment = "від " + word
//									derivs[key] << "від " + word 
//								}
//							}
//							else {
//								derivs[key] << "від " + word
//							} 
//						}

						// using a hack instead
						if( tags.startsWith("advp") && deriv == "надоївши" ) {
							if( word == "надоїти" ) {
								tags += ":xp1"
							}
							else {
								tags += ":xp2"
							}
							comment = "від $word"	
						}
						// TODO: do it better
						else if( tags == "noun:anim:m:v_naz:bad" && deriv == "член-кореспондент" ) {
							continue
						}

						words.add(new DicEntry(deriv, word, tags, comment))
						appliedCnt += 1
						appliedCnts[affixFlag2] += 1

						//util.debug("applied {} to {}", affixGroup, word)
					}
					affixGroup.incrementCounter()

					//      print("DEBUG: applied", affixFlags, "for", word, "got", appliedCnts, file=sys.stderr)
				}
			}

			if( appliedCnts[ affixFlag2 ] == 0 ) {
				throw new Exception("Flag $affixFlag2 of $affixFlags not applicable to $word")
			}
		}

		List<DicEntry> dups = words.findAll { words.count(it) > 1 }.unique()
		if( dups.size() > 0 ) {
		    if( ! (affixFlags =~ /p1\.p2|p[12]\.piv/) ) {
			    log.warn("duplicates: $dups for $word $affixFlags")
			}
		}

		return words
	}


	@CompileStatic
	private Map<String,String> get_modifiers(String mod_flags, String flags, String word) {

		Map<String,String> mods = [:]

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
			else if( modifiers["pers"] == "4" ) {
				prs = ":3|:past|:impr:[sp]:2"
				if( flags.contains('.advp') ) {
				    prs += '|advp'
				}
			}
			else if( modifiers["pers"] == "5" ) {
				prs = ":3|:past|:futr:(p:|s:3)|:impr:p|advp"
//				if( flags.contains('.advp') ) {
//				    prs += '|advp'
//				}
			}
			else
				throw new IllegalArgumentException("invalid p= in " + entry.tagStr)
				
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

		List<DicEntry> out = []
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
		StringBuilder extra_flags = new StringBuilder()

		if( flags.contains(" :") ) {
			def matcher = extraFlagsPattern.matcher(flags)
			if( ! matcher.find() )
				throw new Exception("Not found extra flags in " + flags)

			extra_flags.append(matcher.group(1))
		}
		
		if( flags.contains("<") ) {
		    if( flags.contains(">>") ) {
    			extra_flags.append(":unanim")
			}
			else {
			    extra_flags.append(":anim")
			}
		}
		
		if( flags.contains("<+") ) {
			extra_flags.append(":prop:lname")
		}

		return extra_flags.toString()
	}
	
	private static final Pattern perf_imperf_pattern = ~ ":(im)?perf"
	private static final Pattern and_adjp_pattern = ~ /:&&?adjp(:pasv|:actv|:perf|:imperf)+/

	@CompileStatic
	List<DicEntry> post_expand(List<DicEntry> lines, String flags) {
		if( lines.size() == 0)
			throw new Exception("emtpy lines")

		String extra_flags = get_extra_flags(flags)


		if( extra_flags ) {
//			boolean first_name_base = util.firstname(lines[0].word, flags)

			List<DicEntry> out_lines = []
			List<DicEntry> extra_out_lines = []

			for(DicEntry line in lines) {
				String extra_flags2 = extra_flags

				if( line.tagStr.contains("pname") || flags.contains(":pname") ) {
					extra_flags2 = extra_flags2.replace(":prop:fname", "")
				}

//				if( first_name_base && ! line.tagStr.contains("pname") && ! flags.contains(":pname") ) {
//					extra_flags2 += ":prop:fname"
//				}
//                else if( util.animalname(lines[0].word, flags) ) {
//                    extra_flags2 += ":prop"
//                }
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
		
		// з.в. мн. з в/у: кандидат в президенти
		List<DicEntry> extra_out_lines = []
		for(DicEntry line in lines) {
            if( line.tagStr.startsWith('noun')
                    && line.tagStr.contains(':anim') 
                    && line.tagStr.contains(':p:v_naz') 
                    && ! (line.tagStr =~ /prop|abbr|pron|:nv/) 
                    && ! flags.contains('>')
                    && ! flags.contains('^noun:p') ) { // for composites
                    
                def tagsVzna = line.tagStr.replace("p:v_naz", "p:v_zna:rare")
                extra_out_lines.add(new DicEntry(line.word, line.lemma, tagsVzna, Z_V_U_COMMENT))
            }
        }
        lines += extra_out_lines

		lines = adjustForGeo2019(lines, flags)
				
		return lines
	}

    static final String Z_V_U_COMMENT = 'з в/у'
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
						// log.debug("skipping line with p: {} ", line)
					}
					else if( line.tagStr.contains("//p:") ) {
						line.setTagStr( line.tagStr.replaceAll("//p:.*", "") )
						// log.debug("removing //p from: {}", line)
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
            registerDerivatives(sfx_lines, word)
		}
		else {
			sfx_lines = [
				new DicEntry(word, word, flags)
			]
		}

		sfx_lines = AffixUtil.expand_alts(sfx_lines, "//")  // TODO: change this to some single-char splitter?
		sfx_lines = AffixUtil.expand_alts(sfx_lines, "/")

		if( flags.contains("/adj") ) {
			List<DicEntry> out_lines = []
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

    void registerDerivatives(List<DicEntry> entries, String word) {
        if( entries[0].tagStr =~ /verb.*inf/ ) {
            def derivAdvp = entries.findAll { e -> e.tags[0] == "advp" }
            derivAdvp.each{ e -> derivatives[e.word] << entries[0].word }
//            println ":: deriv: ${entries[0]} -> $derivAdvp"
        }
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
			if( ! tag.contains(':long') ) {
				tag += ':long'
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
					&& ! (addTags =~ /ua_/)
					&& ! (dicEntry.tagStr =~ /(noun|adj).*:[mfn]:v_(naz|oru)/) )
				continue

			String xpTag = addTags.find { it =~ /^xp|slang|perf|imperf/ }
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

		List<LineGroup> out_lines = []
		for(LineGroup lineGroup2 in lineGroups) {
		
          if( lineGroup2.line =~ /\.(advp|cf)/ && ! lineGroup2.line.contains(":imperf") ) {
            throw new IllegalArgumentException(".advp or .cf without :imperf for ${lineGroup2.line}")
          }

            // for adjp..:imperf:perf only :perf gets compb and .adv(:compb) 
            boolean imperPerfAdj = lineGroup2.line =~ /adj.*:imperf:perf/
			out_lines.addAll(
				preprocess2(lineGroup2.line)
    				.collect { String line ->
    					imperPerfAdj && line.contains(":imperf") ? new LineGroup(line) : new LineGroup(lineGroup2, line)
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
        // set fem lemma for substantivated adjectives - хвора
        else if( flags.startsWith("/n2adj1") && flags.contains(".f") ) {
            def line1 = line.replaceFirst(/\.f\b/, "")
            out_lines.add(line1)
            def line2 = line.replace(/\.p\b/, '')
                .replaceFirst(/ий (\/n2adj1)\.f/, 'а $1')
                .replaceFirst(/ій (\/n2adj1)\.f/, 'я $1')
                .replaceFirst(/\.p\b/, '')
            out_lines.add(line2)
        }
		else if( (flags.startsWith("/n10") || flags.startsWith("/n3")) && ! line.contains(":abbr") ) {
			if( ! (flags =~ /\.k[oue]/) ) {
			    lineParts[1] += flags.startsWith("/n10") 
                    ? (line.contains(".<") && word =~ /^[А-ЯІЇЄҐ].*[дзлнртсц]я$/) || word =~ /ся$/  
                        ? ".ku" : ".ko" 
                    : ".ke"
			    line = lineParts.join(" ")
			}
			out_lines = [line]
		}
		else if( flags =~ '^/n2[0-4]' && ! flags.contains(".k") && ! line.contains(":abbr") ) {
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
		else if( lineParts.length > 2 /*&& ! line.contains("adjp")*/ && line.contains(":imperf:perf") ) {
            // so we don't duplicate cf and adv
			def line1 = line.replace(":perf", "").replace(".adv ", " ")
			def line2 = line.replace(":imperf", "").replace(".cf", "")
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
		List<DicEntry> out_lines = []

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
		String lemma = line.word
		if( line.tagStr.startsWith("advp:rev") && line.tagStr.contains(":long") ) {
			lemma = lemma[0..-3]+"сь" 
		}
//		else if( line.tagStr.startsWith("adv:compc") && line.tagStr.contains(":short") ) {
//			lemma = lemma + "е"
//		}
//		log.info("lemma: ${lemma}")
		return new DicEntry(line.word, lemma, line.tagStr, line.comment)
	}

	@CompileStatic
	private boolean isRemoveLine(DicEntry line) {
		for( removeWithTag in Args.args.removeWithTags ) {
			if( line.tagStr.contains(":$removeWithTag") )
				return true
		}
		
		if( Args.args.removeWithRegex && Args.args.removeWithRegex.matcher(line.tagStr) )
			return true
		
        if( line.tagStr.contains(":bad") ) { 
            if( line.tagStr.contains(":inf:short") ) // || line.tagStr.contains(":subst") )
                return true
        }
    
		return false
	}

	@CompileStatic
	private DicEntry removeTags(DicEntry line) {
		for(String removeTag in Args.args.removeTagsWithColons) {
			if( line.tagStr.contains(removeTag) ) {
				line.tagStr = line.tagStr.replace(removeTag, "")
			}
		}
        if( line.tagStr.contains("&&") ) {
            line.tagStr = line.tagStr.replace("&&", "&")
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

			if( line.tagStr.startsWith("adv") ) {
				if( ! line.tagStr.contains("advp") 
						&& ! line.tagStr.contains(":compc") 
						&& ! line.tagStr.contains(":comps") ) {
					line = promote(line)
				}
				// fold довш into довше lemma
				if( line.tagStr.startsWith("adv:comp") && line.tagStr.contains(":short") ) {
					line.lemma = line.lemma + "е"
				}
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
//				":v-u",
                ":abbr",
				":prop",
				":geo", ":fname", ":lname", ":pname",
                ":ua_2019", ":ua_1992", ":short", ":long",
                ":&insert", ":&predic",
				":bad", ":slang", ":rare", ":arch", ":vulg", ":obsc", ":subst", ":coll", ":alt",
				":xp1", ":xp2", ":xp3", ":xp4",
			]

	@CompileStatic
	private DicEntry replace_base(DicEntry line, String base) {
		return new DicEntry(line.word, base, line.tagStr, line.comment)
	}

	@CompileStatic
	private List<DicEntry> expand_subposition(String main_word, String line, String extra_tags, int idx_, String extraFlags) {
		String idx = ""

		if( line.startsWith(" +cs") ) {
			String word

            if( extra_tags.contains(":&numr") ) {
                extra_tags = extra_tags.replace(':&numr', '')
            }

			if( line.contains(" +cs=") ) {
				Matcher matcher = (line =~ / \+cs=([^ ]+)( (:[^ ]+))?/)
				matcher.find()
				word = matcher.group(1)
				
				String compExtraTags = matcher.group(3)
				if( compExtraTags ) {
					extra_tags += compExtraTags
				}
				
//				if( word.endsWith('е') && main_word.endsWith('й') ) {
				if( ! word.endsWith('ий') && main_word.endsWith('й') 
					    || ! (word =~ /[іеш]$/) && main_word =~ /[ео]$/ ) {
					log.error "bad +cs: $word for $main_word"
					throw new RuntimeException()
				}
			}
			else {
				word = main_word[0..<-2] + "іший"
            }
            
			if( extra_tags.contains("&adjp") ) {
				extra_tags = and_adjp_pattern.matcher(extra_tags).replaceFirst('')
			}

            List<DicEntry> forms = []

			List<DicEntry> nayForms
            if( word.startsWith('най') ) {
				nayForms = expand(word, "/adj$extraFlags :comps" + idx + extra_tags)
			    forms += nayForms
            }
            else {
			    forms += expand(word, "/adj$extraFlags :compc" + idx + extra_tags)

				if( ! word.startsWith("й") ) {
					word = "най" + word
					nayForms = expand(word, "/adj$extraFlags :comps" + idx + extra_tags)
			    	forms += nayForms
				}
		    }

//            forms += expand("що" + word, "/adj$extraFlags :comps" + idx + extra_tags)
//			forms += expand("як" + word, "/adj$extraFlags :comps" + idx + extra_tags)
//			forms += expand("щояк" + word, "/adj$extraFlags :comps" + idx + extra_tags)
			
			if( nayForms ) {
                if( ! word.startsWith("й") ) {
    				forms += createSimilar(nayForms, "що")
    				forms += createSimilar(nayForms, "як")
    				forms += createSimilar(nayForms, "щояк")
                }
			}
			
//			forms = forms.collect { DicEntry entry -> 
//				replace_base(entry, main_word) 
//			}

			return forms
		}

		assert false, "Unknown subposition for " + line + " (" + main_word + ")"
	}

	@CompileStatic
	private List<DicEntry> createSimilar(List<DicEntry> forms, String prefix) {
		forms.collect{ DicEntry it -> new DicEntry(prefix+it.word, prefix+it.lemma, it.tagStr) }
	}
	
	@CompileStatic
	private List<DicEntry> expand_subposition_adv_main(String main_word, String line, String extra_tags) {
		log.debug("expanding sub {}: {} extra tags: {}", main_word, line, extra_tags)
		
		if( line.startsWith(" +cs")) {
			String word
			if( line.contains(" +cs=") ) {
				Matcher matcher = (line =~ / \+cs=([^ ]+)( (:[^ ]+))?/)
				matcher.find()
				word = matcher.group(1)

				String compExtraTags = matcher.group(3)
				if( compExtraTags ) {
					extra_tags += compExtraTags
				}

				if( ! (word =~ /[іеш]$/) ) {
					log.error("invalid +cs for adv: {}", line)
                    throw new RuntimeException()
//					System.exit(1)
				}
			}
			else {
				word = main_word[0..<-1] + "іше"
			}

            List<DicEntry> forms = expandSubposAdv(word, extra_tags, line)

			return forms
		}

		throw new Exception("Unknown subposition for $line ($main_word)")
	}

    @CompileStatic
	private List<DicEntry> expandSubposAdv(String word, String extraTags, String line) {
		List<DicEntry> forms = []
		String origWord = word

		String newExtraTags = extraTags.replace(':&insert', '')
		if( word.endsWith('ш') ) {
			newExtraTags += ":short"
		}

		if( word.startsWith('най') ) {
			forms += composeComparAdv(word, "adv:comps" + newExtraTags)
		}
		else {
			forms += composeComparAdv(word, "adv:compc" + newExtraTags)
			if( ! word.startsWith("й") ) {
				word = 'най' + word
				forms += composeComparAdv(word, "adv:comps" + newExtraTags)
			}
		}

		newExtraTags = newExtraTags.replace(':&predic', '')
        if( ! word.startsWith("й") ) {
            forms += composeComparAdv("що" + word, "adv:comps" + newExtraTags)
            forms += composeComparAdv("як" + word, "adv:comps" + newExtraTags)
        }
        //			if( word =~ /[^тд]ше$/ ) {
		if( origWord.endsWith('ше') && ! (line =~ /slang|alt|arch|vulr|bad/) ) {
			String advIsh = origWord.replaceFirst(/ше$/, 'ш')
			forms += expandSubposAdv(advIsh, extraTags, line)
		}
		
		return forms
	}
	
	@CompileStatic
	private DicEntry composeComparAdv(String word, String tags) {
		return new DicEntry(word, word, tags)
	}

	@CompileStatic
	private List<DicEntry> expand_subposition_adv(String last_adv, String line, String extra_tags, String main_word) {

		String word
		if( line.contains(" +cs=") ) {
			def matcher = Pattern.compile(/ \+cs=([^ ]+)/).matcher(line)
			matcher.find()
			word = matcher.group(1)
			
			if( ! (word =~ /ий$/) ) {
				log.error("invalid +cs for adj: " + line)
                throw new RuntimeException()
//				System.exit(1)
			}
			
			word = word[0..<-2] + "е"
		}
		else {
			word = main_word[0..<-2] + "е"
		}

		if( extra_tags.contains("adjp") ) {
			extra_tags = and_adjp_pattern.matcher(extra_tags).replaceFirst('')
		}

		List<DicEntry> forms = expandSubposAdv(word, extra_tags, line)

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
            
            // validate
            assert ! (lineGroup2.line =~ /[^.\/]</)
            String normLine = lineGroup2.line.replaceAll(/\^[^ ]+|tag=[^ ]+/, '') 
            assert ! (normLine =~ /^[^^]+:.* :/)
            

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
            
            DicEntry lemmaLine = inflected_lines.find{ it.tagStr =~ /^noun.*:m:v_naz/ }
            lemmaLine = lemmaLine ?: inflected_lines[0]
            lemmaLine.comment = lineGroup.comment

			if( sub_lines ) {
				def idx = 0
				def extraFlags = lineGroup2.line.contains(".long") ? ".long" : ""
				
				for(String sub_line in sub_lines) {
					
					String extraTags = ""
					if( flags.startsWith("adv:")) {
						extraTags = flags[3..-1].replace(":compb", "")
						//                util.dbg("sub_lines: %s, %s", flags, extra_flags)
					}
					else if( flags.contains(" :") || flags.startsWith(":") ) {
						def matcher = Pattern.compile("(^| )(:[^ ]+)").matcher(flags)
						matcher.find()
						extraTags = matcher.group(2).replace(":compb", "")
						//                 util.dbg("===", extra_flags)
					}

					List<DicEntry> sublines
					if( lineGroup2.line.contains(" adv") ) {
						sublines = expand_subposition_adv_main(main_word, sub_line, extraTags)
					}
					else {
						sublines = expand_subposition(main_word, sub_line, extraTags, idx, extraFlags)
					}
					outEntries.addAll( sublines )

					if( lineGroup2.line.contains(".adv") && lineGroup2.line.contains("/adj") ) {
						for( inflected_line in inflected_lines) {
							if( inflected_line.tagStr.startsWith("adv") ) {
								def last_adv = inflected_line.word
								def cs_lines = expand_subposition_adv(last_adv, sub_line, extraTags, main_word)
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
			log.error("Additional tags not used: {}", additionalTagsUnused)
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
				log.error("Failed to expand: \"${lineGroup.line}\": {}", e.getMessage())
				return null
			}

		}
		.flatMap{ s-> 
//			if( ! s ) {
//                throw new RuntimeException()
//				System.exit(1)
//			}
			s.stream() 
		}
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
		
		ExecutorService executor = Executors.newWorkStealingPool();
		
		if( Args.args.mfl ) {
			executor.execute( {
				new File("dict_corp_lt.txt").withWriter("utf-8") { Writer f ->
					sortedEntries.each {
						f.write(it.toFlatString())
						f.write('\n')
					}
				}
				if( Args.args.time ) {
					def time = System.currentTimeMillis()
					log.info("Write dict_corp_lt time: {}", (time-times[-1]))
					times << time
				}
			})
		}

		if( Args.args.indent ) {

			executor.execute( { 
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
					indentedLines.each { String line ->
						f.write(line)
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
			})
		}

        new File("../data/dict/exceptions.lst").readLines('utf-8')
            .findAll{ line -> line.contains(" advp:") }
            .each { line ->
                String[] parts = line.split(" ")
                derivatives[parts[0]] << parts[1]
            }

        new File("derivats.txt").withWriter("utf-8") { Writer f ->
            def comparator = new UkDictComparator()
            derivatives.toSorted{ e1,e2 -> comparator.compare(e1.key,e2.key) }
            .each { String k, v ->
                String s = v.join(":")
                f.write("$k $s\n")
            }
        }

		executor.shutdown()
		executor.awaitTermination(300, TimeUnit.SECONDS)

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

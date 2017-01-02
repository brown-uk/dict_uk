#!/usr/bin/env groovy

package org.dict_uk.expand

import org.dict_uk.expand.TaggedWordlist
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.CompileStatic

import org.dict_uk.expand.Expand
import org.dict_uk.expand.ExpandComps

import org.dict_uk.common.DicEntry
import org.dict_uk.expand.Args

class ExpandAll {
	static final Logger log = LoggerFactory.getLogger(ExpandAll.class);
	

	@CompileStatic
	static void main(String[] argv) {
		Args.parse(argv)

		Expand expand = new Expand()
		expand.affix.load_affixes(Args.args.affixDir)


		def out_lines = []

		def dictFilePattern = ~/.*\.lst/
		new File(Args.args.dictDir).eachFileMatch(dictFilePattern) { dic_file ->
			List<String> out
			if( dic_file.getName() == "composite.lst" ) {
				def expand_comps = new ExpandComps(expand)
				def dic_file_reader = dic_file.withReader("utf-8") { reader ->
					def lines = reader.readLines()
					def entries = expand_comps.process_input(lines)
					out = entries.collect { DicEntry it ->
						 it.toFlatString() 
					}
				}
				//                out = sorted(out, key=locale.strxfrm)   // just to have consistent output in word_list.txt
			}
			else {
				def tagged_wordlist = new TaggedWordlist()
				out = tagged_wordlist.process_input([dic_file.getAbsolutePath()])
			}

			log.info("Processing file {}, {} lines", dic_file.getName(), out.size())
			out_lines.addAll(out)
		}

		if( out_lines.size() == 0 ) {
			log.error("No valid input lines found in \"{}\"", Args.args.dictDir)
			System.exit(1)
		}
		
		log.info("Expanding {} lines", out_lines.size())
		//    with open("word_list.txt", "w") as out_file:
		//        out_file.write("\n".join(out_lines))

//		List<DicEntry> entries = expand.process_input(out_lines)

		expand.processInputAndPrint(out_lines)
		
	}

}
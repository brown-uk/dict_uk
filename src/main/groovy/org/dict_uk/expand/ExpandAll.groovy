#!/usr/bin/env groovy

package org.dict_uk.expand

import org.dict_uk.expand.TaggedWordlist
import org.dict_uk.expand.Expand
import org.dict_uk.expand.ExpandComps

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
 
import org.dict_uk.expand.Args

//@groovy.util.logging.Log4j2
class ExpandAll {
	static Logger log = LogManager.getFormatterLogger(ExpandAll.class);
	
	static void main(argv) {
		Args.parse(argv)

		def expand = new Expand()
		expand.affix.load_affixes(Args.args.affixDir)


		def out_lines = []

		def dictFilePattern = ~/.*\.lst/
		def dic_files = new File(Args.args.dictDir).eachFileMatch(dictFilePattern) { dic_file ->
			def out
			if( dic_file.getName() == "composite.lst" ) {
				def expand_comps = new ExpandComps(expand)
				def dic_file_reader = dic_file.withReader("utf-8") { reader ->
					out = expand_comps.process_input(reader.readLines())
				}
				//                out = sorted(out, key=locale.strxfrm)   // just to have consistent output in word_list.txt
			}
			else {
				def tagged_wordlist = new TaggedWordlist()
				out = tagged_wordlist.process_input([dic_file.getAbsolutePath()])
			}

			log.info("Processing file %s, got %d lines", dic_file.getName(), out.size())
			out_lines.addAll(out)
		}

		log.info("Expanding %d lines", out_lines.size())
		//    with open("word_list.txt", "w") as out_file:
		//        out_file.write("\n".join(out_lines))

		out_lines = expand.process_input(out_lines, false)

		def filename
		if( Args.args.corp ) {
			filename = "dict_corp_vis.txt"
		}
		else {
			filename = "dict_rules_lt.txt"
		}

		new File(filename).withWriter("utf-8") { out_file ->
			for( line in out_lines ) {
				out_file.write(line + "\n")
			}
		}

	}

}
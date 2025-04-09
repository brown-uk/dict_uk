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

		List<String> outLines = []

		def dictFilePattern = ~/.*\.lst/
		new File(Args.args.dictDir).eachFileMatch(dictFilePattern) { file ->

			List<String> out
			def filename = file.getName()
			if( filename.contains('composite.lst') ) {
				def expand_comps = new ExpandComps(expand)
				def fileReader = file.withReader("utf-8") { reader ->
					def lines = reader.readLines()

					def entries = expand_comps.process_input(lines, filename)
					out = entries.collect { DicEntry it ->
						if( filename.contains("invalid") ) {
							if( ! it.tagStr.contains(":bad") ) {
								it.tagStr += ":bad"
							}
						}
						else if( filename.contains("geo") ) {
							if( ! it.tagStr.contains(":geo") ) {
								it.tagStr += ":geo"
							}
						}

						it.toFlatString() 
					}
				}
				//                out = sorted(out, key=locale.strxfrm)   // just to have consistent output in word_list.txt
			}
			else {
				def tagged_wordlist = new TaggedWordlist()
				out = tagged_wordlist.processInput([file.getAbsolutePath()])
			}

			log.info("Processing file {}, {} lines", filename, out.size())

			outLines.addAll(out)
		}

		if( outLines.size() == 0 ) {
			log.error("No valid input lines found in \"{}\"", Args.args.dictDir)
			System.exit(1)
		}
		
		log.info("Expanding {} lines", outLines.size())

        try {
            expand.processInputAndPrint(outLines)
        }
        catch(Exception e) {
            log.error("Error: ", e)
            System.exit(1)
        }
	}

}
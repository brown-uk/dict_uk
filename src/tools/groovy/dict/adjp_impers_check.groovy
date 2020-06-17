#!/usr/bin/env groovy


// Checks for mismatch of perf/imperf between adjp and impers
// Usage: adjp_impers_check.groovy < dict_corp_lt.txt

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.TypeChecked;

import org.dict_uk.common.DicEntry


def adjpMap = [:]
def advpMap = [:]
def impersMap = [:]
def dicEntryMap = [:]

def corpus = new File("../../../../out/toadd/unknown_table.u.txt").readLines().collect { it.replaceFirst(/\s.*/, '') } as Set


def add(String line, Map<String, List> theMap) {
    def dicEntry = DicEntry.fromLine(line)

    theMap[dicEntry.word] = dicEntry.tags.grep(["imperf", "perf"])

    return dicEntry
}


System.in.readLines().each {
    if( it.contains("impers" ) ) {
        def dicEntry = add(it, impersMap)
        dicEntryMap[dicEntry.word] = dicEntry
    }

    if( it.contains("adjp") && it.contains("m:v_naz") ) {
        def dicEntry = add(it, adjpMap)
        dicEntryMap[dicEntry.word] = dicEntry
    }

    if( it.contains("advp") ) {
        def dicEntry = add(it, advpMap)
        dicEntryMap[dicEntry.word] = dicEntry
    }
}


println ""
println "got " + adjpMap.size() + ", " + impersMap.size()
println ""

for ( item in adjpMap ) {
    def adj = item.key

    if( dicEntryMap[adj].tags.contains("actv") ) {

      def advp = item.key[0..<-1]
      def advpRev = advp + "сь"

      if( advpMap[advp] || advpRev ) {
        for( form in item.value ) {
            if( advpMap[advp] && ! advpMap[advp].contains(form) 
                || advpMap[advpRev] && ! advpMap[advpRev].contains(form) ) {
                    println "not found adjp " + adj + " : " + form + " in advp"
            }
        }
      }
      else {
        if( adj in corpus ) print "* "
        println "== not found advp for adjp " + adj
      }
      continue
    }

    imp = item.key[0..<-2] + "о"
    if( impersMap[imp] ) {
        for( form in item.value ) {
            if( ! impersMap[imp].contains(form) ) {
                if( adj in corpus ) print "* "
                println "not found adjp " + adj + " : " + form + " in impers"
            }
        }
    }
    else {
        if( ! adj.startsWith("не") ) {
            if( adj in corpus ) print "* "
            println "== not found impers for adjp " + adj + " : " + dicEntryMap[adj].tags.grep(["perf", "imperf", "bad"])
        }
    }
}

for ( item in impersMap ) {
    def imp = item.key
    def adj = item.key[0..<-1] + "ий"

    if( adjpMap[adj] ) {
        for( form in item.value ) {
            if( ! adjpMap[adj].contains(form) ) {
                if( item.key in corpus ) print "* "
                println "not found impers " + item.key + " : " + form + " in adj"
            }
        }
    }
    else {
        def formTags = dicEntryMap[imp].tags.grep(["perf", "imperf"])
        def suggestion = imp[0..<-1] + "ий /adj :adjp:pasv:" + formTags.join(':')

        if( imp in corpus ) print "* "
        println "== not found adjp for impers " + imp + " / " + dicEntryMap[imp].lemma + " ==> " + suggestion
    }
}

//for ( item in advpMap ) {
//	def advp = item.key
//	
//	if( advp.endsWith("сь") )
//		continue
//		
//	def adj = item.key + "й"
//	
//	if( adjpMap[adj] ) {
//		for( form in item.value ) {
//			if( ! adjpMap[adj].contains(form) ) {
//				println "not found advp " + item.key + " : " + form + " in adj"
//			}
//		}
//	}
//	else {
//    	def formTags = dicEntryMap[advp].tags.grep(["imperf"]) // , "perf" - немає в укр. мові
//		if( formTags ) {
//			def suggestion = advp + "й/V ^adjp:actv:" + formTags.join(":") + " (:bad)"
//
//			println "== not found adjp for advp " + advp + " / " + dicEntryMap[advp].lemma + " ==> " + suggestion
//		}
//	}
//}
//

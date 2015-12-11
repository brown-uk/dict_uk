#!/home/arysin/bin/groovy

// Checks for mismatch of perf/imperf between adjp and impers
// Usage: adjp_impers_check.groovy < dict_corp_lt.txt

import groovy.transform.CompileStatic

@CompileStatic
class DicEntry {
    final String word
    final String lemma
    final List<String> tags

    public DicEntry(String word, String lemma, List<String> tags) {
	    this.word = word
	    this.lemma = lemma
	    this.tags = tags
	}

    
    public static DicEntry fromLine(String line) {
      def parts = line.split()
      
      return new DicEntry(parts[0], parts[1], Arrays.asList(parts[2].split(":")))
    }
}

def adjpMap = [:]
def advpMap = [:]
def impersMap = [:]
def dicEntryMap = [:]


@CompileStatic
def add(String line, Map<String, List> theMap) {
    def dicEntry = DicEntry.fromLine(line)

    theMap[dicEntry.word] = []
    if( "imperf" in dicEntry.tags )
        theMap[dicEntry.word] << "imperf"
    if( "perf" in dicEntry.tags )
        theMap[dicEntry.word] << "perf"

	return dicEntry        
}


System.in.readLines().each {
    if( it ==~ /.*impers.*/ ) {
        def dicEntry = add(it, impersMap)
        dicEntryMap[dicEntry.word] = dicEntry
    }

    if( it ==~ /.* adjp.*/ && it.contains("m:v_naz") ) {
        def dicEntry = add(it, adjpMap)
        dicEntryMap[dicEntry.word] = dicEntry
    }

    if( it ==~ /.*advp.*/ ) {
        def dicEntry = add(it, advpMap)
        dicEntryMap[dicEntry.word] = dicEntry
    }
}


println ""
println "got " + adjpMap.size() + ", " + impersMap.size()
println ""

for ( item in adjpMap ) {
    adj = item.key

	if( dicEntryMap[adj].tags.contains("actv") ) {

      advp = item.key[0..<-1]
      advpRev = advp + "сь"
      if( advpMap[advp] || advpRev ) {
        for( form in item.value ) {
            if( advpMap[advp] && ! advpMap[advp].contains(form) 
                || advpMap[advpRev] && ! advpMap[advpRev].contains(form) ) {
                    println "not found adjp " + adj + " : " + form + " in advp"
            }
        }
      }
      else {
        println "== not found advp for adjp " + adj
      }
		continue
	}

    imp = item.key[0..<-2] + "о"
    if( impersMap[imp] ) {
        for( form in item.value ) {
            if( ! impersMap[imp].contains(form) ) {
                println "not found adjp " + adj + " : " + form + " in impers"
            }
        }
    }
    else {
        if( ! adj.startsWith("не") ) {
            println "== not found impers for adjp " + adj + " : " + dicEntryMap[adj].tags.findAll{ it.endsWith("perf") }
        }
    }
}

for ( item in impersMap ) {
    imp = item.key
    adj = item.key[0..<-1] + "ий"
    if( adjpMap[adj] ) {
        for( form in item.value ) {
            if( ! adjpMap[adj].contains(form) ) {
                println "not found impers " + item.key + " : " + form + " in adj"
            }
        }
    }
    else {
    	def formTags = ""
    	if( "imperf" in dicEntryMap[imp].tags )
    		formTags += ":imperf"
    	if( "perf" in dicEntryMap[imp].tags )
    		formTags += ":perf"
    	def suggestion = imp[0..<-1] + "ий/V ^adjp:pasv" + formTags 
    	
        println "== not found adjp for impers " + imp + " / " + dicEntryMap[imp].lemma + " ==> " + suggestion
    }
}


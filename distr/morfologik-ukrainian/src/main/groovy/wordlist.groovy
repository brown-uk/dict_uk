#!/bin/env groovy

def freqMin = 60
def freqMinCap = 150
def words = [:].withDefault{0}

//def allTagged = new File('../../../build/tmp/all.tagged.tmp').readLines()
//	.findAll{ ! (it =~ /:(bad|slang|subst|short|long)/) \
//	    || it =~ /insert:short/ \
//	    || it =~ /^((що(як)?)?най)?(більш|менш|скоріш|перш)\s/
//	    }
//	.collect{ it.split(/\t/, 2)[0] } as Set
//println "Unqiue tagged: " + allTagged.size()

def spellWords = new File('../../../build/tmp/spell.words.tmp').readLines() as Set
println "Spell words: " + spellWords.size()

new File('all.tagged.freq.txt').eachLine { String line ->
    if( ! line.trim() )
        return

    String[] parts = line.split(/\t/)

    try {
        String w = parts[1]

    if( ! (w ==~ /^[а-яіїєґА-ЯІЇЄҐ]([а-яіїєґА-ЯІЇЄҐ'-]*[а-яіїєґА-ЯІЇЄҐ])?$/) )
        return

    int f = parts[0] as int

    if( w in spellWords ) {
        words[w] += f
    }
    w = w.toLowerCase()
    if( w in spellWords ) {
        words[w] += f
    }
    }
    catch(e) {
        println "Failed in $line"
        throw e
    }
}

boolean all = "-f" in args || "--full" in args

def out = new File(all ? 'uk_wordlist_all.xml' : 'uk_wordlist.xml')
out.text = '<wordlist locale="uk_UA" description="Ukrainian" date="'+new Date().getTime()+'" version="1">\n'

def sortedWords = words.toSorted{ -it.value }
sortedWords.each {
    if( it.key =~ /^[А-ЯІЇЄҐ]/ && it.value < freqMinCap )
        return
    if( it.value < freqMin )
        return

    out << "<w f=\"" + Math.round(it.value) + "\">${it.key}</w>\n"
}

if( all ) {
	sortedWords.each {
		if( it.value >= freqMin )
			return
	
		out << "<w f=\"" + Math.round(it.value) + "\">${it.key}</w>\n"
	}
	spellWords.each {
		if( ! (it in words) ) {
			out << "<w f=\"0\">${it}</w>\n"
		}	
	}
}


out << '</wordlist>\n'

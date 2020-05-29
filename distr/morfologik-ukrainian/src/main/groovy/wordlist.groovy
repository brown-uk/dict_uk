#!/bin/env groovy

def freqMin = 10
def words = [:].withDefault{0}

def allTagged = new File('../../../build/tmp/all.tagged.tmp').readLines().collect{ it.split(/\t/, 2)[0] } as Set
println "Unqiue tagged: " + allTagged.size()

new File('all.tagged.freq.txt').eachLine { String line ->
    if( ! line.trim() )
        return

    String[] parts = line.split(/\t/)

    try {
        String w = parts[1]

    if( ! (w ==~ /^[а-яіїєґА-ЯІЇЄҐ]([а-яіїєґА-ЯІЇЄҐ'-]*[а-яіїєґА-ЯІЇЄҐ])?$/) )
        return

    int f = parts[0] as int

    if( w in allTagged ) {
        words[w] += f
    }
    w = w.toLowerCase()
    if( w in allTagged ) {
        words[w] += f
    }
    }
    catch(e) {
        println "Failed in $line"
        throw e
    }
}


def out = new File('uk_wordlist.xml')
out.text = '<wordlist locale="uk_UA" description="Ukrainian" date="'+new Date().getTime()+'" version="1">\n'

words.toSorted{ -it.value }.each {
    if( it.value < freqMin )
        return

    out << "<w f=\"" + Math.round(it.value) + "\">${it.key}</w>\n"
}

out << '</wordlist>\n'

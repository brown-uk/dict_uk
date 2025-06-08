#!/bin/env groovy

package org.dict_uk.check

import org.dict_uk.expand.DictSorter

DictSorter dictSorter = new DictSorter()

def lemmas = [:].withDefault { [] }
def lemmaKeys = [:].withDefault { [] }
new File("out/dict_corp_vis.txt")
    .readLines('UTF-8')
    .findAll { ! it.startsWith(' ') }
    .each { 
        it = it.trim()
        def (lemma, tags) = it.split(' ')
//        def tagKey = dictSorter.getLineKey(lemma, tags, null)
        def tagKey = "$lemma " + tags.replaceFirst(/:.*/, '') 
        lemmas[lemma] << tags
        lemmaKeys[tagKey] << tags
//        "$tagKey"
    }

    
//def dbg = lemmaKeys.take(15).collect{ "'$it'" }
//println ":: $dbg"
//println ":: " + lemmaKeys["аахенський adj"]
//println ":: " + lemmaKeys[0].getClass()

//System.exit(1)

//def lemmaKey1 = "adj аахенський"
//if( ! lemmaKeys.contains("$lemmaKey1") ) println "failed 2: $lemmaKey1: ${lemmaKey1.class}"

def dictDir = new File("data/sem")
def files = dictDir.listFiles().findAll { it.name.endsWith('.csv') }
    
files.each { File file ->
    def lines = file.readLines('UTF-8')
        .findAll { ! it.trim().startsWith("#") }

    println "${file.name}: ${lines.size()} lines"
    
    def tagBase = file.name.replace('.csv', '')
    
        def prevLine = ""
        lines.eachWithIndex { line, idx ->
            if( idx == 0 ) it = line.replace('\ufeff', '')
            line = line.trim().replaceFirst(/\s*#.*/, '')
            if( prevLine == line ) {
                println "Duplicate line: $line"
            }
            prevLine = line
                
            def parts = line.split(',')
            def word = parts[0]
            def semTag = parts[1]
            def extraTag = parts.size() > 2 ? parts[2].trim() : ""
            
            if( extraTag && ! extraTag.startsWith(":") ) {
                System.err.println "Extra tag does not start with : for \"$line\""
            }
            
            def lemmaKey = "$word $tagBase".toString()

//            if( word.endsWith(".") ) lemmaKey += ":nv" 

            def matches = lemmaKeys[lemmaKey]
            
            if( ! extraTag ) {
                if( tagBase == "noun" ) {
                    if( semTag =~ /:hum:group|:org/ ) {
                        extraTag = ":anim|:inanim"
                    }
                    else if( semTag =~ /:hum|:supernat/ ) {
                        extraTag = ":anim"
                    }
                    else if( semTag =~ /:animal/ ) {
                        extraTag = ":anim|:unanim"
                    }
                    else if( semTag.contains(':loc') && Character.isUpperCase(word.charAt(0)) ) {
                        extraTag = ":geo"
                    }
                    else if( semTag =~ /:deictic/ ) {
                        extraTag = ":pron"
                    }
                    else {
                        extraTag = ":inanim"
                    }
                }
            }
            
            if( extraTag ) {
                matches = matches.findAll{ it =~ extraTag }
            }

            if( ! matches ) {
                println "Unmatched $line ($lemmaKey)"
                println "    lemma matches: " + lemmas[word]
            }
            else {
                if( semTag.contains('hum') && Character.isUpperCase(word.charAt(0)) ) {
                    if( matches.find { it =~ /^noun:anim:m.*name/ } ) {
                        matches.removeAll { it =~ /^noun:anim:f.*name/ }
                    }
                    if( matches.find { it =~ /^noun:anim:.*fname/ } ) {
                        matches.removeAll { it =~ /^noun:anim:.*[pl]name/ }
                    }
                    if( matches.find { it =~ /^noun:anim:.*lname/ } ) {
                        matches.removeAll { it =~ /^noun:anim:.*pname/ }
                    }
                }
                else if( semTag.contains('loc') && Character.isUpperCase(word.charAt(0)) ) {
                    if( matches.find { it =~ /geo:.*xp1/ } ) {
                        matches.removeAll { it =~ /geo:.*xp2/ }
                    }
                }

                if( matches.size() > 1 ) {
                    def matches2 = matches.collect{ it.replaceAll(/:nv|:up..|:alt|:imperf|:perf/, '') }.unique()
                    
                    if( matches2.size() > 1 ) {
                        println "Multiple match for $line: " + matches
                    }
                }
            } 
        }
}

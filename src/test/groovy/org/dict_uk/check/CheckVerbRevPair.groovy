#!/bin/env groovy

package org.dict_uk.check

//assert ["1:imperf", "1:perf"].contains(["1:imperf"])

def toIgnore = ["добігати", "долітати", "дослухати", "дотягати", "задихати", "закупати", "замикати",
    "звірити", "зміняти", "наганяти", "накупати", "налазити", "наповзати", "наскіпати", "ослизнути",
    "позбувати", "присікати", "розходити", "скупити", "залупати", "височити", "врубати", "доводити", 
    "роз'їздити", "вишити"]

def dirVerb = [:]

def dictDir = new File("data/dict")

dictDir.eachFile { file ->
    if( ! file.name.endsWith(".lst") ) return

        def fileName = null
    file.eachLine { line ->
        if( line.trim().startsWith('#')) return

            line = line.replaceFirst(/ *#.*/, '')

        if( line.contains("ти /v") ) {
            def (lemma, others) = line.split(" ", 2)
            def flags = extractFlag(others)

            if( ! (lemma in dirVerb) ) dirVerb[lemma] = [] as Set
            dirVerb[lemma] += flags
        }
        else if( line.contains("тися /vr") ) {
            def (lemma, others) = line.split(" ", 2)
            def flags = extractFlag(others)

            def lemmaDir = lemma.replaceFirst(/ся$/, '')

            flags.each { flag ->
                def dirVerbFlags = dirVerb[lemmaDir]
                if( dirVerbFlags ) {
                    if( ! dirVerbFlags.contains(flag) && ! toIgnore.contains(lemmaDir)) {
                        //          println " ${dirVerbFlags.class} ${flags.class}"
                        if( ! fileName ) {
                            fileName = file.name
                            println "File: $file.name"
                        }

                        println "Mismatch: $lemmaDir $dirVerbFlags != $lemma $flags"
                    }
                    //dirVerb.remove(lemma)
                }
            }
        }
    }
}


def extractFlag(String line) {
    def v = line.replaceFirst(/\/vr?([1-9]).*(?:\/vr?   ([1-9]))?.*/, '$1 $2').split(' ')
    def vids = []
    if( line.contains(':imperf') ) {
        vids += "imperf"
    }
    if( line.contains(':perf') ) {
        vids += "perf"
    }
    def x = v.collect { v_ -> vids.collect { "$v_:$it" }}.flatten() as Set
    //    println x
    x
}

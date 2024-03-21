#!/bin/env groovy

package org.dict_uk.check;

def dictDir = new File("data/dict")

def files = dictDir.listFiles().findAll { it.name.endsWith('.lst') || it.name.endsWith('replace.txt') }

assert files

def replWords = files.collect { it.text.split("\n")  } \
    .flatten() \
    .findAll { it.contains(" #> ") }
    .collect {
        def replStr = it.split(" #> ")[1].trim()
        def replItems = replStr.split(/\|/)
        if( replItems.size() > 5 && false )
          println "WARNING: Too many replacements ${replItems.size()} > 5 for\n\t$it"
        
        def repls = replStr.split(/[;, \|]+/)
        def word = it.split(' ', 2)[0]
        if( word in repls && ! (it =~ /(?iu) - [а-яіїєґ].* #> /) ) {
            println "WARNING: Replacement is same as word: $word:\n\t$it"
        }
        
        def dups = replItems.countBy{it}.grep{it.value > 1}.collect{it.key}
        if( dups ) {
            println "WARNING: Duplicate replacements: $dups:\n\t$it"
        }
        
        repls
    }
    .flatten()
    .collect { it.replaceAll(/[()]/, '').replaceFirst(/-таки$/, '') }
    .findAll { it =~ /[а-яіїєґ]/ }
    .unique().sort()

def spellWords = new File("out/words_spell.txt").text.split('\n')
spellWords += new File("data/dict/slang.lst").text.split('\n').collect{ it.replaceFirst(/ .*/, '') }
spellWords += new File("data/dict/arch.lst").text.split('\n').collect{ it.replaceFirst(/ .*/, '') }

println "Unique replacement words: ${replWords.size()}"

replWords.removeAll(spellWords)

replWords.removeIf{ w -> (w =~ /[.-]$|'дно|X-/) as Boolean }
replWords.removeAll(["здавання-приймання", "передання-прийняття", "прийняття-передання"])

println "Unknown:\n" + replWords.join("\n")
println "Total uknown: ${replWords.size()}"

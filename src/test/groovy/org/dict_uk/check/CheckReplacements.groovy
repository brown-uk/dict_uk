#!/bin/env groovy

package org.dict_uk.check;

def dictDir = new File("data/dict")

def files = dictDir.listFiles().findAll { it.name.endsWith('.lst') || it.name.endsWith('replace.txt') }

assert files

def koatuuIgnore = new File(dictDir, "geo-ukr-koatuu.lst").readLines()
    .findAll { it.startsWith("#- ") }
    .collect { it.split()[1] } //as Set
    
def replWords = files.collect { File file ->
        file.readLines()  
    }
    .flatten() \
    .findAll { it =~ /^[^#].* #>>? / }
//    .findAll { (! it.contains("#>>")) || (! koatuuIgnore.contains(it.split()[0])) }
    .collect { String line ->
        def replStr = line.split(/ #>>? /)[1].trim()
        def replItems = replStr.split(/\|/)
        if( replItems.size() > 5 && false )
          println "WARNING: Too many replacements ${replItems.size()} > 5 for\n\t$line"
        
        def repls = replStr.split(/[;, \|]+/)
        
        if( ! line.contains(' - ') ) {
            def word = line.split(' ', 2)[0]
            if( word in repls && ! (line =~ /(?iu) - [а-яіїєґ].* #> /) ) {
                println "WARNING: Replacement is same as word: $word:\n\t$line"
            }
        }
        
        def dups = replItems.countBy{it}.grep{it.value > 1}.collect{it.key}
        if( dups ) {
            println "WARNING: Duplicate replacements: $dups:\n\t$line"
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
replWords.removeAll(koatuuIgnore)

replWords.removeIf{ w -> (w =~ /[.-]$|'дно|X-/) as Boolean }
replWords.removeAll(["здавання-приймання", "передання-прийняття", "прийняття-передання"])
replWords.removeAll(["Вама", "Велика", "Великий", "Гуляй", "Капу", "Крута",
    "Кьоґенеш", "Нові", "Німецька", "Радісний", "Середня", "Чумацький", "верхньошироківський"])

println "Unknown:\n" + replWords.join("\n")
println "Total uknown: ${replWords.size()}"

#!/bin/env groovy

package org.dict_uk.check

def dictDir = new File("data/dict")

def files = dictDir.listFiles().findAll { it.name.endsWith('.lst') \
    && ! (it.name in ['dot-abbr.lst', 'geo-ukr-hydro.lst', 'geo-ukr-koatuu.lst']) }

def lines = files.collect { File f ->
            String extraTag = f.name.contains('geo') ? " :geo"
              : f.name.contains('slang') ? " :slang" : ""

            f.readLines()
            .findAll { ! it.startsWith('#') && ! it.startsWith(' +cs=') } 
            .collect { it
//            .replaceFirst(/^ \+cs=/, '')
                .trim()
                .replaceAll(/\s*#.*/, '')
                .replaceAll(/( \/[a-z0-9]+)(\s+|$)/, '$1...')
                .replaceAll(/\.[^<.: ]+/, '...')
                .replaceAll(/\s*:coll/, '')
                .replaceAll(/:&adjp:(actv|pasv)(:(imperf|perf))+/, '')
            }
            .collect { line ->
                line = "${line}${extraTag}"
            }
        }
        .flatten()
        .grep { it }

//    lines = lines.grep { ! it.startsWith('#') }

//new File("z_du.txt").text = lines.join("\n")

def dups = lines.countBy{it}.grep{it.value > 1 }.collect{it.key.toString()}

java.text.Collator coll = java.text.Collator.getInstance(new Locale("uk", "UA"));
coll.setStrength(java.text.Collator.IDENTICAL)
coll.setDecomposition(java.text.Collator.NO_DECOMPOSITION)

println dups.toSorted(coll).join("\n")
println "Found ${dups.size()} dups"

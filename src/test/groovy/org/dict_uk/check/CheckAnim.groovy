#!/bin/env groovy

package org.dict_uk.check;

def dictDir = new File("data/dict")

def files = dictDir.listFiles().findAll { it.name =~ /.*anim.*\.lst/ }

assert files

def lines = files.collect { File f ->
    f.readLines()
            .findAll { it =~ /^[А-ЯІЇЄҐ]'?[а-яіїєґ](?!.*\/adj)/ && ! (it =~ /[.\/]<|-(фонд|фест)/) }
}
.flatten()
.grep { it }

java.text.Collator coll = java.text.Collator.getInstance(new Locale("uk", "UA"));
coll.setStrength(java.text.Collator.IDENTICAL)
coll.setDecomposition(java.text.Collator.NO_DECOMPOSITION)

println lines.toSorted(coll).join("\n")
println "Found ${lines.size()} suspicious anim"

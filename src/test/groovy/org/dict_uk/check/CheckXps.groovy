#!/bin/env groovy

package org.dict_uk.check;

def dictDir = new File("data/dict")

def files = dictDir.listFiles().findAll { it.name.endsWith('.lst') }

assert files

def xps = [:].withDefault{ [] }

def lines = files.collect {  it.readLines()  }.flatten() \
          .collect { it.replaceFirst(/#.*/, '') }
.findAll{ it.contains(":xp") \
        && ! it.startsWith("+cs") && ! it.startsWith('#') \
        && ! (it =~ /verb(?!.*inf)|noun(?!.*(:[mnf]:v_naz|:p:v_naz:ns))/) }
.collect { it
    it.replaceAll(/^([^ ]+)\h.*?(:xp.).*/, '$1 $2')
}

lines.each{
    def (base, xp) = it.split(/ /)
    xps[base] << xp
}

xps.each { k,v -> v.sort() }

xps.each { k,v ->
    if( v[0] != ':xp1' ) println "out of order: $k: $v"
    else if( v.size() == 1 ) println "single: $k: $v"
    else {
        def dups = v.countBy{it}.grep{it.value > 1}.collect{it.key}
        if( dups ) println "dups: $k: $dups"
    }
}

//      java.text.Collator coll = java.text.Collator.getInstance(new Locale("uk", "UA"));
//      coll.setStrength(java.text.Collator.IDENTICAL)
//      coll.setDecomposition(java.text.Collator.NO_DECOMPOSITION)

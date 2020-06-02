#!/bin/env groovy

def lines = new File('geo-ukr-koatuu.lst').readLines()

def newLines = lines.collect {
    if( it =~ /^[^#]+(\/|noun)/ ) {
        it
    }
    else {
        '# ' + it
    }

}

new File('geo-ukr-koatuu.lst.new').text = newLines.join('\n')
#!/bin/env groovy

System.in.eachLine { line ->
    if( ! line.contains("#") || line.startsWith('#') ) {
        println line
        return
    }

    String[] parts = line.split(/\s*#\s*/)
    
    String newLine = parts[0]
    String rv = ""
    String repl  = ""
    String other = ""
    
    parts[1..-1].each { String part ->
        if( part.startsWith('>') )
            repl = part
        else if( part.startsWith('rv_') )
            rv = part
        else  // if( part.startsWith('rv_') )
            other = part
    }

    if( other ) {
        newLine = newLine.padRight(40) + "  # $other"
    }
    if( rv ) {
        newLine = newLine.padRight(60) + "  # $rv"
    }
    if( repl ) {
        newLine = newLine.padRight(76) + "  #$repl"
    }
    
    println newLine
}

#!/bin/env groovy

System.in.eachLine { line ->
    if( ! line.contains("#") || line.startsWith('#') ) {
        println line
        return
    }


    if( line.contains("#>") ) {
        def (left, right) = line.split("#>")

        left = left.replaceFirst(/\s+$/, '').padRight(48)

        println left + "#>" + right
    }
    else
    if( line.contains("#") ) {
        def (left, right) = line.split("#", 2)

        left = left.replaceFirst(/\s+$/, '').padRight(48)

        if( ! left.endsWith(' ') ) {
            left += '  '
        }

        println left + "#" + right
    }
}

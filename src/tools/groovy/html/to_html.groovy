#!/bin/env groovy

String main=""
new File('dict_corp_vis.txt').eachLine { String line ->
    line = line.replace("'", '\u02BC')   // u02BC
//    line = line.replaceFirst(/ {4}#/, '&nbsp;&nbsp;&nbsp;&nbsp;#')

    if( line.startsWith(' ') ) {
//        line = '<br>&nbsp;&nbsp;' + line.trim()
        line = line.replaceFirst(/^\h*/, '•')
    }
    else {
        line = line.replaceFirst(/xv[1-9]/, '(багатозначне слово)')
        line = line.replaceFirst(/ # rv_/, '&nbsp;&nbsp;&nbsp;&nbsp;$0')
        if( main )
            println "@$main@1"

        def spaceIdx = line.indexOf(' ')
        main = line.substring(0, spaceIdx)
    }
    print line
}
println "@$main@1"

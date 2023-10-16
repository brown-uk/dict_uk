#!/bin/env groovy

package org.dict_uk.tools

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher
import java.util.regex.Pattern


class Stemmer {
    def roots = [:].withDefault { [] as Set }
    def props = [] as Set
    
    static void main(String[] args) {
        def file = new File("out/dict_corp_vis.txt")
//        def outFile = new File("out/roots.txt")
        
        new Stemmer().findRoots(file)
        println "Getting roots for ${file.name}..."
	}
    
    void readGeos() {
        def geoFiles = ["geo-other.lst", "geo-ukr-koatuu.lst", "lang.lst", "geo-ukr-hydro.lst", "names-anim.lst", "names-other.lst"]
        geoFiles.each { f ->
            def lst = new File("data/dict/$f").readLines('utf-8')
                .collect{ line -> line.replaceFirst(/ .*/, '') } as Set
            props += lst
        }
        
        println "props: ${props.size()}: \"${props[0]}\""
    }
    
	void findRoots(File file) {
        readGeos()

		def dir = Paths.get(file.getAbsolutePath())
		def inFile = dir.resolve(file.name)
        def outFile = new File(file.name + ".roots")
        outFile.text = ''
        def outFileTodo = new File(file.name + ".roots.todo")
        outFileTodo.text = ''

		file.readLines('UTF-8')
            .each { line ->
                if( ! line.startsWith(" ") )
                    findStem([line])
            }

        println "Found roots: ${roots.size()} (total infl: ${inflCnt})"
        
        todo.each { w ->
            outFileTodo << w << '\n'    
        }
        
//        int cntGes = 0
        roots.each { k,v ->
//            boolean geoYes, geoNo
//            v.each { x -> def y = (x in props); geoYes |= y; geoNo |= !y }
//            def sss = geoYes & geoNo ? "*" : ""
//            if( sss ) cntGes++
//            def vvv = v.collect { it -> it in props ? "$it*" : it }.join(" ")
//            outFile << "$k -$sss ${vvv}\n"
            def vvv = v.join(" ")
            outFile << "$k - ${vvv}\n"
        }
//        println "ges: $cntGes"
	}

    
    static Pattern PREFIX = Pattern.compile(/^(по|над|про|за)/)
    static Map<String, Map<Pattern, String>> SUFFIXES = Map.of(
//        "/vr", Pattern.compile(/(тися)$/),
//        "/v", Pattern.compile(/(ти)$/),
        " adj", [
                (Pattern.compile(/(.{4})(уватий|(из)?ований|(из)?(ув)?альний|а?т[уо]рний|(из)?[а]ційний|(ат)?и[вч]ний|иний|ісінький|н?(ес)?енький)$/)): '$1', // дивакуватий
                (Pattern.compile(/(л?ен|[іоеу]вськ|ь?[цс]ьк|к?ов|ь?ницьк|инський|іоністськ|ійськ|[иії]стськ|ст|яч|ійн|ь?[нчк]|н)?([іи]й|ів)$/)) : ''
//                (Pattern.compile(/(нин)$/)) : 'а'
                ],
        " noun", [
                (Pattern.compile(/(.{4})([юу]ватість|(из)?ованість|(из)?([ую]ва)?ння|[ую]вальни(к|ця)|ат(ка)?|(из)?[ая]?ція|ь?ство|ика|'ятко|еня(тко)?|иність)$/)): '$1', // дивакуватість, байкерство
                    (Pattern.compile(/(а)ч$/)): '$1',
                    (Pattern.compile(/(ер)$/)): '$1', // швагер
                    (Pattern.compile(/(.{4})([аоя])ч(ок|ка|атура)$/)): '$1', // (але тачка)
                    (Pattern.compile(/(.{4})(і[вй]ка|івець|ієць|тор)$/)): '$1', // крад/ійка (але дівк/а)
                    (Pattern.compile(/(.{4})(єць)$/)): '$1й', // малаєць
                    (Pattern.compile(/([вдзжклмнрстчщ])[ео]$/)): '$1',
                    (Pattern.compile(/([дзлстчш])\1я$/)): '$1',
                    (Pattern.compile(/([еиоуджлртсшч])я$/)): '$1',
                    (Pattern.compile(/(стість|ійність|ь?[клнт]ість|([тджл])\3я|[цс]?тво|ько|[чт]ко|е?н[еє]|те|стя|'я|[иі]ще|ь?це|іше)$/)): '', // :n:
                    (Pattern.compile(/(иса|ч?иня|ь?[нчщ]?иця|ня|чка|ька)$/)): '', // :f:
                    (Pattern.compile(/((ен)?ь?[нчщ]?ик|ин|ич|ок|ко|ія|а|ор|[иії]ст|[иії]зм|([іиї]ст)?ка|іонізм|унізм|няк|ій|ий|ит/
                            +/|іоніст|івець|ець|ья|ь|ив|вля|ця|ьо)$/)): '',
                    (Pattern.compile(/(ен|ат|ів|об|ар|[ае]й|яр|ац|ент|аж|[иеє]р|ез|є[нмф]|и[длцпc]|[іи]нг|ант|ов|он|ог|оз|ой|[уюя]й|[внжт]р/
                        + /|[бвгґджзйклмнпртсхфчшь][бвгґджзклмнпрстфхцчшщ]|[аеуиіїоуюя][бвдгґзжнклмнпрстфцчхшщ])$/)): '$1',
                    ],
//        " noun", [Pattern.compile(/(ен)$/), '$1'],
        )

//        static Map<String, Map<Pattern, String>> SUFFIXES2 = Map.of(
//                    " adj", [
//                            (Pattern.compile(/(.{4})(уватий|(из)?ований|(из)?(ув)?альний|а?т[уо]рний|(из)?[а]ційний|(ат)?и[вч]ний|иний|ісінький|н?(ес)?енький)$/)): '$1', // дивакуватий
//                            (Pattern.compile(/(л?ен|[іоеу]вськ|ь?[цс]ьк|к?ов|ь?ницьк|инський|іоністськ|ійськ|[иії]стськ|ст|яч|ійн|ь?[нчк]|н)?([іи]й|ів)$/)) : ''
//            //                (Pattern.compile(/(нин)$/)) : 'а'
//                            ],
//                    " noun", [
//                            (Pattern.compile(/(.{4})([юу]ватість|(из)?ованість|(из)?([ую]ва)?ння|[ую]вальни(к|ця)|ат(ка)?|(из)?[ая]?ція|ь?ство|ика|'ятко|еня(тко)?|иність)$/)): '$1', // дивакуватість, байкерство
//                       ]
//        )
                                        
    int inflCnt = 0
    def todo = []
    
    String findStem(List<String> lines) {
        
        def line1 = lines[0]
        if( (line1 =~ /-|abbr|&pron|alt|arch|bad|slang|subst|[lp]name|comp[cs]|:nv|:ns/) )
            return null

//        if( lines[0] =~ /^((що)?як)?най.*? adj/ )
//            return null
            
        if( line1 =~ / (noun|adj)/ )
            inflCnt++
        else
            return null

        lines = lines.collect{ it.replaceFirst(/ *#.*/, '') }
        line1 = lines[0]

        def w = line1.replaceFirst(/ .*/, '')
        if( w in props )
            return null
        if( w.length() < 5 )
            return w
        
        Map<Pattern, String> replPair = SUFFIXES.find{ k, v -> line1.contains(k) }.value

        def pToS1 = null
        for(def e: replPair.entrySet()) {
            def m = e.key.matcher(w)
            if( m ) { 
                pToS1 = [(m): e.value]
                break
            }
        }
//        def pToS = replPair.collectEntries { k, v -> 
//            [(k.matcher(w)) : v]
//        }.find { it.key.find() }
        
        if( ! pToS1 ) {
            if( ! (line1 =~ /prop|adj/) ) {
                todo << line1
            }
            return null
        }
        
        def pToS = pToS1.iterator().next()
        def root = pToS.key.replaceFirst(pToS.value)

        if( ! root ) {
            todo << line1
            return null;
        } // root, "no root in $line1"
//        root = sfx.value[0].matcher(root).replaceFirst(sfx.value[1])

        if( root ) {
//            if( w in geos ) {
//                w = "$w/geo"
//            }
            roots[root.toLowerCase()] << w
        }
                
//        println "$line1 => $root"
        return root
    }
}

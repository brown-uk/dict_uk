#!/bin/env groovy

package org.dict_uk.tools

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher
import java.util.regex.Pattern


class Stemmer {
    def roots = [:].withDefault { [] as Set }
    def preStems = [:]
    def props = [] as Set
    
    Stemmer() {
        loadPre()
    }
    
    static void main(String[] args) {
        def file = new File("out/dict_corp_vis.txt")
//        def outFile = new File("out/roots.txt")
        
        new Stemmer().findRoots(file)
        println "Getting roots for ${file.name}..."
	}
    
    void loadPre() {
        new File(getClass().getResource('stems.txt').toURI()).readLines()
            .each { line ->
                def (stem, words) = line.split(/ - /)
                words.split(/ /).each { w -> preStems[w] = stem }
            }
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

    static String noprefixes = "автор|автоген|авіатор|антипод"
    static String prefixes = "авто|а[ву]діо|авіа|анти|багато"
    static String preReg = /^($prefixes)?+/
    
//    static Pattern PREFIX = Pattern.compile(/^(по|над|про|за)/)
    static Map<String, Map<Pattern, String>> SUFFIXES = Map.of(
        " adj", [
                (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])н)?[ую]ва[нт]ий|(из|й)?ований|(из)?(ув)?альний|а?т[уо]рний)$/)): '$1', // дивакуватий
                (Pattern.compile(/(.{3,}?)(((ер)?из)?[а]ційний|ерний|ерський|ез(н|ійн|[іи]чн)ий|(ист)?ійний|(ат|ист)?[іи][вч]ний|[еі]йський|ейний|иний)$/)): '$1', // дивакуватий
                (Pattern.compile(/(.{3,}?)(н?ісінький|н?(ес)?енький)$/)): '$1', // дивнесенький
                (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])н(ений|івський)|івницький)$/)): '$1', // вивласнений
                (Pattern.compile(/(.{3,}?)(л?ен|[іоеу]вськ|ь?[цс]ьк|к?ов|ь?ницьк|инський|іоністськ|ійськ|[иії]стськ|ст|яч|ь?[нчк]|н|ер)?([іи]й)$/)) : '$1',
                (Pattern.compile(/(.{3,}?)ч?ів$/)) : '$1'
//                (Pattern.compile(/(нин)$/)) : 'а'
                ],
        " noun", [
                (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])ле(ність|ння)$/)): '$1', // надщербленість
                (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])н)?[ую]ва[тн]ість|(из|й)?ованість|иність|(из|ер)?(іон)?[ую]вання|((?<![аеєиіїоуюя])н)?[ую]вання|а?ння)$/)): '$1', // дивакуватість, байкерство
                (Pattern.compile(/(.{3,}?)(ез(ія|ійність|ичність)?)$/)): '$1', // біогенез
                (Pattern.compile(/(.{3,}?)([ую]вальни(к|ця)|ерка|ат(ка)?|((ер)?из)?[ая]?ція|[еі]йник|[еі]йство|(ер)?ь?ство|'ятко|еня(тко)?)$/)): '$1', // дивакуватість, байкерство
                (Pattern.compile(/(.{3,}?)(а)ч$/)): '$1',
                (Pattern.compile(/(.{3,}?)(ів(ня|ник|ниця)?)$/)): '$1', // мандрівник
                (Pattern.compile(/(.{3,}?)(ер(ня|ник|ниця|ія)?)$/)): '$1', // швагер, парфумерія
                (Pattern.compile(/(.{3,}?)(ист)?(очка|ика|ія)$/)): '$1',
                (Pattern.compile(/(.{3,}?)([аоя])ч(ок|ка|атура)$/)): '$1', // (але тачка)
                (Pattern.compile(/(.{3,}?)(і[вй]ка|івець|іоніст(ка)?|ець|ієць|тор|ей|ерик)$/)): '$1', // крад/ійка (але дівк/а)
                (Pattern.compile(/(.{3,}?)(єць)$/)): '$1й', // малаєць
                (Pattern.compile(/(.{3,}?)(стість|[еі]йність|(у?ал)?ь?[клнт]ість|ість|([тджл])\3я|(ів)?(ни)?[цс]?тво|ько|[чт]ко|е?н[еє]|те|стя|'я|[иі]ще|ь?це|іше)$/)): '$1', // :n:
                (Pattern.compile(/([вдзжклмнрстчщ])[ео]$/)): '$1',
                (Pattern.compile(/([дзлстчш])\1я$/)): '$1',
                (Pattern.compile(/([еиоуджлртсшч])я$/)): '$1',
                (Pattern.compile(/(.{3,}?)(иса|ч?иня|(ій)?ь?[нчщ]?иця|ня|чка|ька)$/)): '$1', // :f:
                (Pattern.compile(/((ен)?ь?[нчщ]?ик|ин|ич|ок|ко|ія|а|ор|[иії]ст|[иії]зм|(іал)?([іиї]ст)?ка|іонізм|унізм|няк|ій|ий|ит/
                        +/|ья|ь|ив|вля|ця|ьо)$/)): '',
                (Pattern.compile(/(ен|ат|ів|об|ар|[ае]й|яр|ац|ент|аж|[иеє]р|ез|є[нмф]|и[длцпc]|[іи]нг|ант|ов|он|ог|оз|ой|[уюя]й|[внжт]р/
                    + /|[бвгґджзйклмнпртсхфчшь][бвгґджзклмнпрстфхцчшщ]|[аеуиіїоуюя][бвдгґзжнклмнпрстфцчхшщ])$/)): '$1',
                ],
//        " noun", [Pattern.compile(/(ен)$/), '$1'],
        )

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
            
        if( w in preStems ) {
//            println "predef: $w"
            return preStems[w]
        }
        
        def origW = w
        def pref = ""
            
        if( ! Pattern.compile(/^($noprefixes)/).matcher(w).find() ) {
            def m = Pattern.compile(/^($prefixes)/).matcher(w)
            if( m ) {
                pref = m[0][0]
                w = m.replaceFirst('')
            }
        }
        
        if( w.length() < 4 ) {
            def root = pref + w
            roots[root.toLowerCase()] << origW
            return origW
        }

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
        String root = pToS.key.replaceFirst(pToS.value)

        if( ! root ) {
            todo << line1
            return null
        } // root, "no root in $line1"
//        root = sfx.value[0].matcher(root).replaceFirst(sfx.value[1])

        if( root ) {
//            if( w in geos ) {
//                w = "$w/geo"
//            }
            root = pref + root
            roots[root.toLowerCase()] << origW
        }
                
//        println "$line1 => $root"
        return root
    }
}

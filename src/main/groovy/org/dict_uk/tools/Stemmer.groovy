#!/bin/env groovy

package org.dict_uk.tools

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher
import java.util.regex.Pattern

import groovy.transform.CompileStatic


class Stemmer {
    Map<String, Set<String>> roots = [:].withDefault { [] as Set }
    Map<String, Set<String>> rootsPref = [:].withDefault { [] as Set }
    def preStems = [:]
    def props = [] as Set
    def baseDir = new File(".").absolutePath.replaceFirst(/(dict_uk).*/, '$1')
    
    Stemmer() {
        println "Base dir: $baseDir"
        loadPre()
    }
    
    static void main(String[] args) {
        new Stemmer().findRoots()
	}
    
    void loadPre() {
        new File(getClass().getResource('stems.txt').toURI()).readLines()
            .each { line ->
                try {
                def (stem, words) = line.split(/ - /)
                words.split(/ /).each { w -> preStems[w] = stem }
                }
                catch(e) {
                    System.err.println "Failed to parse $line"
                    System.exit(1)
                }
                
            }
    }
    
    void readGeos() {
        def geoFiles = ["geo-other.lst", "geo-ukr-koatuu.lst", "lang.lst", "geo-ukr-hydro.lst", "names-anim.lst", "names-other.lst"]
        geoFiles.each { f ->
            def lst = new File(baseDir, "data/dict/$f").readLines('utf-8')
                .collect{ line -> line.replaceFirst(/ .*/, '') } as Set
            props += lst
        }
        
        println "props: ${props.size()}: \"${props[0]}\""
    }
    
	void findRoots() {
        def file = new File(baseDir, "out/dict_corp_vis.txt")
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
            outFile << "$k - ${vvv}"
            def withpref = rootsPref[k]
            if( withpref ) outFile << "    ${withpref.join(' ')}" 
            outFile << "\n"
        }
//        println "ges: $cntGes"
	}

    static String noprefixes = "автор|автоміст|автоген|авіарій|авіатор|антипод|мотор|супереч|суперни[кц]"
    static String prefixes = "авто|а[ву]діо|авіа|анти|багато|без|вело|взаємо|відео|віце|напів'?|кібер|кіно|мото|нано|псевдо|спец|фото" \
        +"|нео(?=[аеєиіїоуюя])|супер|двох?|тр(и|ьох)|чотир(и|ьох)|п'ят(и|ьох)|шести|семи|восьми|дев'яти|не"
    static String preReg = /^($prefixes)?+/
    
//    static Pattern PREFIX = Pattern.compile(/^(по|над|про|за)/)
    static Map<String, Map<Pattern, String>> SUFFIXES = Map.of(
        " adj", [
                (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])н)?[ую]ва[нт]ий|([іи]з|й)?ований|([іи]з)?(ув)?альний|[іи]зат[уо]рний|а?т[уо]рний)$/)): '$1', // дивакуватий
                (Pattern.compile(/(річний)$/)): 'річ', // дивнесенький
                (Pattern.compile(/(.{3,}?)(((ер)?[іи]з)?[а]ційний|ерний|ерський|ез(н|ійн|[іи]чн)ий|(ист)?ійний|(ат|[іи]ст)?[іи][вч]ний|[еі]йський|ейний|иний|лив(еньк)?ий)$/)): '$1', // дивакуватий
                (Pattern.compile(/(.{3,}?)(н?ісінький|н?(ес)?енький)$/)): '$1', // дивнесенький
                (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])н(ений|івський)|івницький)$/)): '$1', // вивласнений
                (Pattern.compile(/(.{3,}?)(аний|((?<![аеєиіїоуюя])к)?овий)$/)): '$1', // намотаний
                (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])л)?ен|[іоеу]вськ|ь?[цс]ьк|к?ов|ь?ницьк|инський|іоністськ|ійськ|[иії]стськ|ст|яч|ь?[нчк]|н|ер)?([іи]й)$/)) : '$1',
                (Pattern.compile(/(.{3,}?)ч?ів$/)) : '$1'
//                (Pattern.compile(/(нин)$/)) : 'а'
                ],
        " noun", [
                (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[нл]е(ність|ння)$/)): '$1', // надщербленість
                (Pattern.compile(/(.{3,}?)ли(вість|виця|вець|вка|вчик|вство)$/)): '$1',
                (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])н)?[ую]ва[тн]ість|([іи]з|й)?ованість|([іи]ст)?ичність|[аи]ність|([іи]з|ер)?(іон)?[ую]вання|((?<![аеєиіїоуюя])н)?[ую]вання|а?ння)$/)): '$1', // дивакуватість, байкерство
                (Pattern.compile(/(.{3,}?)(ез(ія|ійність|ичність)?)$/)): '$1', // біогенез
                (Pattern.compile(/(.{3,}?)([ую]вальни(к|ця)|ерка|ікат|ат(ка)?|((ер)?[иі]з)?[ая]?ція|[еі]йник|[еі]йство|(ер)?ь?ство|'ятко|еня(тко)?)$/)): '$1', // дивакуватість, байкерство
                (Pattern.compile(/(.{3,}?)(ач|онька|істика)$/)): '$1',
                (Pattern.compile(/(.{3,}?)(ів(ня|ник|ниця)?)$/)): '$1', // мандрівник
                (Pattern.compile(/(.{3,}?)(ер(ня|ник|ниця|ія)?)$/)): '$1', // швагер, парфумерія
                (Pattern.compile(/(.{3,}?)(ист)?(очка|ика|ія)$/)): '$1',
                (Pattern.compile(/(.{3,}?)([аоя])ч(ок|ка|атура)$/)): '$1', // (але тачка)
                (Pattern.compile(/(.{3,}?)(і[вй]ка|івець|іоніст(ка)?|ець|ієць|тор|ей|ерик|ік(иня)?)$/)): '$1', // крад/ійка (але дівк/а)
                (Pattern.compile(/(.{3,}?)(єць)$/)): '$1й', // малаєць
                (Pattern.compile(/(.{3,}?)(иса|ч?иня|(ій)?ь?[нчщ]?иця|ня|чка|ька)$/)): '$1', // :f:
                (Pattern.compile(/(.{3,}?)(стість|[еі]йність|(у?ал)?ь?[клнт]ість|ість|([тджл])\3я|(ів)?(ни)?[цс]?тво|ько|[чт]ко|е?н[еє]|те|стя|'я|[иі]ще|ь?це|іше)$/)): '$1', // :n:
                (Pattern.compile(/(.{2,}?)([вдзжклмнрстчщ])[ео]$/)): '$1$2',
                (Pattern.compile(/(.{2,}?)([дзлстчш])\2я$/)): '$1$2',
                (Pattern.compile(/([еиоуджлртсшч])я$/)): '$1',
                (Pattern.compile(/((ен)?ь?[нчщ]?ик|ин|ич|ок|ко|ія|а|ор|[иії]ст|[иії]зм|(іал)?([іиї]ст)?ка|іонізм|унізм|няк|ій|ий|ит/
                        +/|ья|ь|ив|вля|ця|ьо)$/)): '',
                (Pattern.compile(/(.{2,}?)(ен|ат|ів|об|ар|[ае]й|яр|ац|ент|аж|[иеє]р|ез|є[нмф]|и[длцпc]|[іи]нг|ант|ов|он|ог|оз|ой|[уюя]й|[внжт]р/
                    + /|[бвгґджзйклмнпртсхфчшь][бвгґджзклмнпрстфхцчшщ]|[аеуиіїоуюя][бвдгґзжнклмнпрстфцчхшщ])$/)): '$1$2',
                ],
//        " noun", [Pattern.compile(/(ен)$/), '$1'],
        )

    int inflCnt = 0
    List<String> todo = []
    
//    @CompileStatic
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
            addRoot(preStems[w], w)
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
            addRoot(root, origW)
            return origW
        }

        Map<Pattern, String> replPair = SUFFIXES.find{ k, v -> line1.contains(k) }.value

        Map.Entry<Matcher, String> pToS1 = null
        for(def e: replPair.entrySet()) {
            def m = e.key.matcher(w)
            if( m ) { 
                pToS1 = Map.entry(m, e.value)
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
        
        Map.Entry<Matcher, String> pToS = pToS1 //.iterator().next()
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
            addRoot(root, origW)
        }
                
//        println "$line1 => $root"
        return root
    }

    @CompileStatic    
    private void addRoot(String root, String origW) {
        if( root.endsWith("'") )
            root = root.substring(0, root.length()-1)
        
        def root2 = removePrefixes(root)
        if( root2 != root ) {
            rootsPref[root2.toLowerCase()] << origW
        }
        else {
            roots[root2.toLowerCase()] << origW
        }
    }

    @CompileStatic    
    private String removePrefixes(String root) {
        if( ! (/^($noprefixes)/ =~ root ) ) {
            def p = Pattern.compile(/^(авіа|взаємо|напів'?|кібер|кіно|мото|нано|псевдо|спец|фото|нео(?=[аеєиіїоуюя])|супер|двох?|тр(и|ьох)/
                            +/|чотир(и|ьох)|п'ят(и|ьох)|шести|семи|восьми|дев'яти|без|не)/)
            root = p.matcher(root).replaceFirst('')
        } 
        return root
    }
}

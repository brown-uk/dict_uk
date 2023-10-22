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
        def outFileBad = new File(file.name + ".roots.bad")
        outFileBad.text = ''

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
        roots.each { root,v ->
//            boolean geoYes, geoNo
//            v.each { x -> def y = (x in props); geoYes |= y; geoNo |= !y }
//            def sss = geoYes & geoNo ? "*" : ""
//            if( sss ) cntGes++
//            def vvv = v.collect { it -> it in props ? "$it*" : it }.join(" ")
//            outFile << "$k -$sss ${vvv}\n"
            def vvv = v.join(" ")
            outFile << "$root - ${vvv}"
            
            if( root.length() > 3 && root =~ /[аеєиіїоуюяь\']$/ ) {
                outFileBad << "Invalid root $root - ${vvv}\n"
            }
            
            def withpref = rootsPref[root]
            if( withpref ) outFile << "    ${withpref.join(' ')}" 
            outFile << "\n"
        }
//        println "ges: $cntGes"
	}

    static String noprefixes = "автор|автоміст|автоген|авіарій|авіатор|антипод|міністер|мотор|спеціаліст|супереч|суперни[кц]"

    static String prefixes = "все|усе|авіа|авто|а[ву]діо|агро|аеро|аква|анти|багато|біо|вело|взаємо|високо|відео|віце|внутрішньо|гідро|гіро|загально|квазі|кібер|кіно" \
        + "|мало|мега|мікро|міні|мото|мульти|нано|напів'?|псевдо|середньо|радіо|спец|теле|турбо|ультра|фото" \
        +"|нео(?=[аеєиіїоуюя])|супер|двох?|тр(и|ьох)|чотир(и|ьох)|п'ят(и|ьох)|шести|семи|восьми|дев'яти|без|не"
    final Pattern PREFIX_REMOVE = Pattern.compile(/^(авіа|авто|а[ву]діо|агро|аеро|аква|анти|багато|біо|вело|взаємо|високо|відео|віце|внутрішньо|гідро|гіро|загально|квазі|кібер|кіно/
        + /|мало|мега|мікро|міні|мото|мульти|нано|напів'?|псевдо|середньо|радіо|спец|теле|турбо|ультра|фото/
        + /|нео(?=[аеєиіїоуюя])|супер|двох?|тр(и|ьох)|чотир(и|ьох)|п'ят(и|ьох)|шести|семи|восьми|дев'яти|без|не)/)

    static String preReg = /^($prefixes)?+/
    
//    static Pattern PREFIX = Pattern.compile(/^(по|над|про|за)/)
    static Map<String, Map<Pattern, String>> SUFFIXES = Map.of(
        " adj", [
                (Pattern.compile(/(річний)$/)): 'річ',
                (Pattern.compile(/(бу|секре|компози|зекуц|бі|ди|ститу|моц)(ційний|торний)$/)): '$1т',
                (Pattern.compile(/(ст)иційний$/)): '$1',
                (Pattern.compile(/(.{3,}?)і[ая]льний$/)): '$1',
                (Pattern.compile(/(.{3,}?)еїст(ичний|ський)$/)): '$1',
                (Pattern.compile(/(.{2,}?)ллястий$/)): '$1л',
                (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])((ув)?[аяю]цький|[яю]ковий|[яю]куватий|[ая]стий)$/)): '$1',
                
                (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])н)?[ую]ва[нт]ий|([іи]з|й|ь)?ований|([іи]з|(?<![аеєиіїоуюя])(л|і))?([ую]в)?альний|(([іи]з)?)?[аея]?т[уо]рний)$/)): '$1', // дивакуватий
                (Pattern.compile(/(.{3,}?)(((ер)?[іи]з)?[а]ційний|ерний|ерський|ез(н|ійн|[іи]чн)ий|(ист)?ійний|[іия]стий|(ат|[іїи]ст)?[іи][вч]ний|[еі]йський|ейний|иний|лив(еньк)?ий)$/)): '$1', // дивакуватий
                (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])н)?(ісінький|(ес)?енький))$/)): '$1', // дивнесенький
                (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])н(ений|івський)|івницький)$/)): '$1',
                (Pattern.compile(/(.{3,}?)(аний|((?<![аеєиіїоуюя])к|ь)?овий)$/)): '$1', // намотаний
                (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])л)?ен|[іоеу]вськ|ь?[цс]ьк|к?ов|ь?ницьк|инський|іоністськ|ійськ|[иії]стськ|ст|яч|(?<![аеєиіїоуюя])ь?[нчк]|ер)?([іи]й)$/)) : '$1',
                (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])ч?ів$/)) : '$1'
//                (Pattern.compile(/(нин)$/)) : 'а'
                ],
        " noun", [
                (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[нл]е(ність|ння)$/)): '$1',
                (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])л?іння(чко)?$/)): '$1',
                (Pattern.compile(/(.{2,}?)([лт])\2я(чко)?$/)): '$1$2',
                (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])([яію]ка?|л?([юу]в)?[аяю](цтво|ч(ка)?)|(ен|')?ят(оч)?ко|ечко|овиння|[вл]?[ея]ння)$/)): '$1',
                (Pattern.compile(/(.{3,}?)(ея|еїст(ка)?|еїстика|еїзм)$/)): '$1',
                (Pattern.compile(/(.{3,}?)([ая])ка$/)): '$1$2',
                (Pattern.compile(/(.{3,}?)ли(вість|виця|вець|вка|вчик|вство)$/)): '$1',
                (Pattern.compile(/(.{3,}?)[іиая]стість$/)): '$1',
                (Pattern.compile(/(.{3,}?)(ичка)$/)): '$1',
                
                (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])н)?[ую]ва([тн]ість|льність|ння)|([іи]з|й)?ованість|([іїи]ст)?[іи]чність|[аи]ність|([іи]з|ер)?(іон)?[ую]вання|а?ння)$/)): '$1', // дивакуватість, байкерство
                (Pattern.compile(/(.{3,}?)(ез(ія|ійність|ичність)?)$/)): '$1', // біогенез
                (Pattern.compile(/(бу|секре|компози|зекуц|бі|ди|ститу|моц)(ція|ційність|тор)$/)): '$1т',
                (Pattern.compile(/(ст)иц(ія|ійність)$/)): '$1', // інвестиція
                (Pattern.compile(/(.{3,}?)([ую]вальни(к|ця)|ерка|ікат|і?ат(ка)?|(?<![аеєиіїоуюя])ція|((ер)?[иі]з)?[ая]ція|[еі]йник|[еі]йство|(ер)?ь?ство|'ятко|еня(тко)?)$/)): '$1', // дивакуватість, байкерство
                (Pattern.compile(/(.{3,}?)(([ую]в)?ач(ка)?|онька|[ії]стика)$/)): '$1',
                (Pattern.compile(/(.{3,}?)(ів(ня|ник|ниця)?)$/)): '$1', // мандрівник
                (Pattern.compile(/(.{3,}?)(ер(ня|ник|ниця|ія)?)$/)): '$1', // швагер, парфумерія
                (Pattern.compile(/(.{3,}?)([аеєиіїоуюя]ц)ія$/)): '$1$2',
                (Pattern.compile(/(.{3,}?)(і[вй]ка)$/)): '$1',
                (Pattern.compile(/(.{3,}?)((іон)?[їіи]ст)?(очка|ка|ика|ія)$/)): '$1',
                (Pattern.compile(/(.{3,}?)([аоя])ч(ок|ка|атура)$/)): '$1', // (але тачка)
                (Pattern.compile(/(.{3,}?)(івець|іоніст(ка)?|ець|ієць|ятор|([іи]з)?а?тор|ей|ерик|ік(иня)?)$/)): '$1', // крад/ійка (але дівк/а)
                (Pattern.compile(/(.{3,}?)(єць)$/)): '$1й', // малаєць
                (Pattern.compile(/(.{3,}?)(иса|ч?иня|(ій)?(?<![аеєиіїоуюя])ь?[нчщ]?иця|ня|(?<![аеєиіїоуюя])чка|ька)$/)): '$1', // :f:
                (Pattern.compile(/(.{3,}?)(стість|[еі]йність|(у?ал)?(?<![аеєиіїоуюя])ь?[клнт]ість|ість|([тджл])\3я|(ів)?(ни)?[цс]?тво|ько|[чт]ко|е?н[еє]|стя|'я|[иі]ще|ь?це|іше)$/)): '$1', // :n:
                (Pattern.compile(/(.{2,}?)([вдзгжклмнрстхцчщ])[ео]$/)): '$1$2',
                (Pattern.compile(/(.{2,}?)([дзлстчш])\2я$/)): '$1$2',
                (Pattern.compile(/([еиоудзжлнртсшч])я$/)): '$1',

                (Pattern.compile(/(.{2,}?)(?<![аеєиіїоуюя])(к)а$/)): '$1',
                (Pattern.compile(/(.{2,}?)(к)а$/)): '$1$2',
                (Pattern.compile(/(.{2,}?)([дзлнстчш])е$/)): '$1$2',

                (Pattern.compile(/((ен)?(?<![аеєиіїоуюя])ь?[нчщ]?ик|ин|ич|ок|ко|ія|а|ор|[иії]ст|[иії]зм|(іал)?([іиї]ст)?ка|іонізм|унізм|няк|ій|ий|ит/
                        +/|ья|ь|ив|вля|ця|ьо)$/)): '',
                (Pattern.compile(/(.{2,}?)(ен|ат|ів|об|ар|[ае]й|яр|ац|ент|аж|[иеє]р|ез|є[нмф]|и[длцпc]|[іи]нг|ант|ов|он|ог|оз|ой|[уюя]й|[внжт]р/
                    + /|[бвгґджзйклмнпртсхфчшь][бвгґджзклмнпрстфхцчшщ]|[аеуиіїоуюя][бвдгґзжнклмнпрстфцчхшщ])$/)): '$1$2',

                ],
//        " noun", [Pattern.compile(/(ен)$/), '$1'],
        )
    final Pattern NON_PREFIXES_PATTERN = Pattern.compile(/^($noprefixes)/)
    final PREFIXES_PATTERN = Pattern.compile(/^($prefixes)/)
    
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
            
        if( ! NON_PREFIXES_PATTERN.matcher(w).find() ) {
            def m = PREFIXES_PATTERN.matcher(w)
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

//            if( w in geos ) {
//                w = "$w/geo"
//            }
        if( root.length() < 3 ) {
            root = w
        }
        root = pref + root
        addRoot(root, origW)
                
//        println "$line1 => $root"
        return root
    }

    @CompileStatic    
    private void addRoot(String root, String origW) {
        if( root =~ /['ь]$/ )
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
            root = PREFIX_REMOVE.matcher(root).replaceFirst('')
        } 
        return root
    }
}

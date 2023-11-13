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
    def counts = [:].withDefault { 0 }
    
    Stemmer() {
//        println "Base dir: $baseDir"
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
                    System.err.println "Failed to parse \"$line\""
                    System.exit(1)
                }
            }
            
        assert 'біоніка' in preStems
    }
    
    void readGeos() {
        def geoFiles = ["geo-other.lst", "geo-ukr-koatuu.lst", "lang.lst", "geo-ukr-hydro.lst", "names-anim.lst", "names-other.lst", "pharm.lst"]
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
                if( ! line.startsWith(" ") ) {
                    findStem([line])
                }
            }

        println "Found roots: ${roots.size()} (total infl: ${inflCnt})"
        
        todo.each { w ->
            outFileTodo << w << '\n'    
        }
        
        java.text.Collator coll = java.text.Collator.getInstance(new Locale("uk", "UA"));
        coll.setStrength(java.text.Collator.IDENTICAL)
        coll.setDecomposition(java.text.Collator.NO_DECOMPOSITION)
  
        roots.toSorted{ e1,e2 -> coll.compare(e1.key, e2.key) }
            .each { root,v ->
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
        println counts
	}

    static String noprefixes = /автор|автоміст|автоген|авіарій|авіатор|антипод|міністер|мотор|спеціаліст|супереч|суперни[кц]/

    // TODO: над, про, фін
    static String prefixesBase = /авіа|авто|а[ву]діо|агро|аеро|аква|анти|багато|біло|біо|бого/ \
        + /|важко|велико|вело|вібро|взаємо|високо|відео|віце|внутрішньо|водо|вугле|вузько/ \
        + /|газо|гідро|гіро|гіпер|держ|еко(?!ном|лог)|екс(?!порт|нен|ном|тен[зс]|тер|тра|трем)|електро|етно|енерго/ \
        + /|євро|загально|звуко|зоо|квазі|кібер|кіно|контр|коротко|легко|лже/ \
        + /|магн[еі]то|макро|мало|мега|медіа|між|мікро|міні|моно|мото|мульти/ \
        + /|навколо|нано|напів'?|нафто|низько|ново|одно|пара|парт|пізньо|після|пневмо|полі|пост|псевдо|проти|прото/ \
        + /|радіо|ранньо|рівно|різно|само|середньо|слабк?о|соц(іо)?|спец|спів|старо|стерео|суб|теле|тепло|термо|тех|тонко|турбо/ \
        + /|ультра|фіто|фото|чорно/ \
        + /|нео(?=[аеєиіїоуюя])|супер|двох?|тр(и|ьох)|чотир(и|ьох)|п'ят(и|ьох)|шести|семи|восьми|дев'яти|без|не/
    static String prefixes = "все|усе|$prefixesBase"
    static final Pattern PREFIX_REMOVE = Pattern.compile(/^($prefixesBase)/)

    static String preReg = /^($prefixes)?+/
    
//    static Pattern PREFIX = Pattern.compile(/^(по|над|про|за)/)
    static Map<String, Map<Pattern, String>> SUFFIXES = Map.of(
        // TODO: актуальнішати/актуальніший + ішання
        // дж -> д, жч -> ж, тч -> т
        // [бвдпс]л -> $1
        // ст -> щ
        // [бд]н - приладнаний, [бдтч]н - блідніти
        // виняток для кльов
        " verb", [
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])н?ішати(ся)?$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])онути(ся)?$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[у]ювати(ся)?$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])(і)ювати(ся)?$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])ствувати(ся)?$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])(([иі]з)?(ов)?ува|л?юва|ну|[аеєиіїоуюя])?ти(ся)?$/)): '$1'
            ],
        " adj", [
            (Pattern.compile(/(річний)$/)): 'річ',
            (Pattern.compile(/(руськ|поган|багат)(е(се)?ньк)?ий$/)): '$1',
            (Pattern.compile(/([мр]отор|креатив|мистець|турець|грець|хвост)([кн]ий)$/)): '$1',
            (Pattern.compile(/(креац)ій([кн]ий)$/)): '$1',
            (Pattern.compile(/((?<!бо)реал)ьний$/)): '$1',
            (Pattern.compile(/([нгрх])еальний$/)): '$1',
            (Pattern.compile(/(бу|секре|компози|зекуц|бі|ди|ститу|моц)(ційний|торний)$/)): '$1т',
            (Pattern.compile(/((?<!нс)тру)(юва(ль)?ний|й(ова)?ний)$/)): '$1й',
            (Pattern.compile(/(.{3})[ое]подібний$/)): '$1',

            (Pattern.compile(/(.{3,}?)([нт])\2євий$/)): '$1$2',
            
            (Pattern.compile(/(.{3,}?)(ивістський|ивістичний|ивізова?ний)$/)): '$1',
            (Pattern.compile(/(ст)иційний|(?<![аеєиіїоуюя])ці(йний|оністський)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<!міт)[іи]нг(ов(ан)?ий|увальний)$/)): '$1',
            (Pattern.compile(/(.{3,}?)[і]євий$/)): '$1',
            (Pattern.compile(/(.{3,}?)(ю[щч]ий)$/)): '$1',
            (Pattern.compile(/(.{2,}?)([іо])ян(ий)$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)(л)яційний$/)): '$1$2',
            (Pattern.compile(/(.{3,}?)[уі][ая](льний|тивний|ційний)$/)): '$1',
            (Pattern.compile(/(.{3,}?)[ауіе]їст(ичний|ський)$/)): '$1',
            (Pattern.compile(/(.{3,}?)([о])їст(ичний|ський)$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)(?<!п)леч(ковий|ний)$/)): '$1',
            (Pattern.compile(/(.{2,}?)ллястий$/)): '$1л',
            (Pattern.compile(/(.{2,}?)((ени)цький|еський)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])((ув)?[аеяю]цький|(ув)?альницький)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])([яю]ковий|[яю]куватий|[ая]стий)$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[лн][ея](ний)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[лн]юва(ний)$/)): '$1',
            
            (Pattern.compile(/(.{2,}?)([оеа])[ую]ва((ль)?н|т)ий$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)овува(ль)?ний$/)): '$1',
            (Pattern.compile(/(.{2,}?)([а])ячий$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])([лн]|[іи]з|і)[ую]вальний$/)): '$1',
            (Pattern.compile(/(.{3,}?)і?[ую]вальний$/)): '$1',
            (Pattern.compile(/(.{2,}?)([оа])йований$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)[уі]йований$/)): '$1',
//            (Pattern.compile(/(.{3,}?)[у]юваний$/)): '$1',
            (Pattern.compile(/(.{2,}?)їстий$/)): '$1й',
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])[ін]?[ую]в)?атий$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])нутий$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])чатий$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])(([іи]з)?і?[яеа])?тор(ний|ський)|іатурний)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])н)?[ую]ва[нт]ий|іюваний|іюватий)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(([іи]з|й|ь)?ований|([іи]з|(?<![аеєиіїоуюя])(л|і))?([ую]в)?альний)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(((ер)?[іи]з)?[а]ційний|ез(н|ійн|[іи]чн)ий|(ист)?ійний|[іия]стий|(ат|а?[іїи]ст)?[іи][вч]ний|лив(еньк)?ий)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(ерний|ейний|ерський|[еі]йський|иний)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])н)?([ію]сінький|(ат)?(ес)?енький))$/)): '$1', // дивнесенький
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])н(ений|івський)|івницький)$/)): '$1',
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])л)?ений$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])лий$/)): '$1', // (еньк|ов)? - too many FP
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])ев(еньк|уват)?ий$/)): '$1',
            (Pattern.compile(/(.{3,}?)(аний|((?<![аеєиіїоуюя])к|ь)?овий)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(ік)?(?<!ом)овний$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])н?и[сц]ький$/)): '$1',
            (Pattern.compile(/(.{3,}?)ерий$/)): '$1',
            (Pattern.compile(/(.{3,}?)([іоеу]вськ|ь?[цс]ьк|к?ов|ь?ницьк|инський|іоністськ|[иії]стськ|істов|ст|яч|(?<![аеєиіїоуюя])ь?[нчк])?([іи]й)$/)) : '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])ч?ів$/)) : '$1'
//                (Pattern.compile(/(нин)$/)) : 'а'
                ],
        " noun", [
            (Pattern.compile(/([рм]отор|креатив)(ність|ник|ниця)?$/)): '$1',
            (Pattern.compile(/(креац)(ій(ність|ник|ниця)|ія)?$/)): '$1',
            (Pattern.compile(/(мистец)(ь|тво)?$/)): '$1',
            (Pattern.compile(/(річка)$/)): 'річ',
            (Pattern.compile(/(еат)(ка|ство)?$/)): '$1',
            (Pattern.compile(/(поган)е$/)): '$1',
            (Pattern.compile(/(реал)(іст(ка)?|ізм|ьність)$/)): '$1',
            (Pattern.compile(/((?<!нс)тру)(юваність|й(ова)?ність)$/)): '$1й',
            (Pattern.compile(/(.{3})[ео]подібність$/)): '$1',

            (Pattern.compile(/(.{3,}?)([тн])\2є(вість|вик)$/)): '$1$2',
            (Pattern.compile(/(?<![аеєиіїоуюя])ну(тість|ття)$/)): '',
//            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])(ант(ка)?|ативність|аційність)$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)ю([щч]ість|ча|чок|ченя|чисько|чиння|чище|чник|ччя)$/)): '$1',
//            (Pattern.compile(/(ор(юв)?)ане$/)): '$0',
            (Pattern.compile(/(а)(не)$/)): '', // спродане
            (Pattern.compile(/(.{2,}?)((ени)цтво|ество|еньк[ао]|енко)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(уація|уйованість|уювання|уатор(ка)?)$/)): '$1',

            (Pattern.compile(/(.{3,}?)(?<!міт)[іи]нг(іст(ка)?|ізм|ування)?$/)): '$1',
            (Pattern.compile(/(?<![аеєиіїоуюя])ці(оніст(ка)?|онізм)$/)): '',
            
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[лн]?[еія](ння(чко)?)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[лн][ея](ність)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[вл]?[ея](ння)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])е[нв](ість|е)$/)): '$1',
            
            (Pattern.compile(/(.{2,}?)([джлстч])\2я(чко)?$/)): '$1$2',
            (Pattern.compile(/(.{2,}?)[єї](ння|стість)$/)): '$1й',
            (Pattern.compile(/(.{2,}?)([іо])ян(ня|ість|(оч)?ка|ин)$/)): '$1$2й',
            
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])люва(ння|(ль)?ність)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])іюва(ння|(ль)?ність)$/)): '$1',
            (Pattern.compile(/(.{2,}?)([оеа])[ую]ва(ння|ність)$/)): '$1$2й',
            
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[лн][ея](ник|н(оч)?ка)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])н[ую]вач$/)): '$1',
            (Pattern.compile(/(.{3,}?)([лмн])(?<!пл)ічка$/)): '$1$2',

//            (Pattern.compile(/(.{3,}?)((?<!н)[ио])цтво$/)): '$1$2к',
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])((ів)?н)?и[сц]тво|ивність|ивіст(ка)?|ивістика|ивізм|ивізація|ивізатор(ка)?|ивчик)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])(ь?щина|атина)$/)): '$1',
            (Pattern.compile(/(.{2,}?)ея$/)): '$1ей',

            (Pattern.compile(/(.{2,}?)(?<![аеєиіїоуюя])ч((а|еня)(т(оч)?ко)?|ище)$/)): '$1',
            (Pattern.compile(/(.{3,}?)('ятко|еня(тко)?|исько)$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)(овуван(ість|ня)|овувач(ка)?|овання)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])(л?([юу]в)?[аеяю](цтво|ч(ка)?))$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])([яію]ка?)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])(((ен|')?я|а)т(оч)?ко|ечко|овиння)$/)): '$1',
            (Pattern.compile(/(.{3,}?)[уе](я|їст(ка)?|їстика|їстичність|їзм)$/)): '$1',
            (Pattern.compile(/(.{3,}?)([еоя])(їст(ка)?|їстика|їстичність|їзм)$/)): '$1$2й',
//            (Pattern.compile(/(.{3,}?)([ая])ка$/)): '$1',
            (Pattern.compile(/(.{3,}?)ли(вість|виця|вець|вка|вчик|вство)$/)): '$1',
            (Pattern.compile(/(.{3,}?)[іиая]стість$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])(ув)?ал(ка|ьня|ьце|о|ення|ювання)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(ичка|иха|юга|юра|ятина)$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)(а)юватість$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])[ін]?[ую]в)?атість$/)): '$1',
            (Pattern.compile(/(.{3,}?)([уо]н)ня$/)): '$1$2',
            (Pattern.compile(/(.{2,}?)([оа])йованість$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])[ні])?[ую]ва([тн]ість|льність|ння)|([іи]з|і?й)?ованість|([іи]з|ер)?(іон)?[ую]вання|(а?[іїи]ст)?[іи]чність|[аи]ність)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(ез(ія|ійність|ичність)?)$/)): '$1', // біогенез
            (Pattern.compile(/(.{3,}?)(ік)?(?<!ом)овність$/)): '$1',
            (Pattern.compile(/(бу|б'ю|секре|компози|зекуц|бі|ди|ститу|моц)(ція|ційність|тор)$/)): '$1т',
            (Pattern.compile(/(ст)иц(ія|ійність)$/)): '$1', // інвестиція

            (Pattern.compile(/(.{3,}?)[иа]?ння(чко)?$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)(еса|ерка|ікат|(?<![аеєиіїоуюя])і?ат(ка)?)$/)): '$1',
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])ція|((ер)?[иі]з|і)?[ая]ція|(яц)?[еі](йник|йність))$/)): '$1',
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])(([іи]з)?і?[яеа])?тор(ка|ство|ій|ник|ниця)?|ей|ерик|ік(иня)?)$/)): '$1',
            (Pattern.compile(/(.{3,}?)([еі]йство|(ер)?ь?ство|(ів)?(ни)?[цс]?тво)$/)): '$1', // байкерство
            (Pattern.compile(/(.{3,}?)(стість|([іу]?ал)?(?<![аеєиіїоуюя])ь?[клнт]ість|ість)$/)): '$1', // :n:
            (Pattern.compile(/(.{3,}?)(([тджл])\2я)$/)): '$1',
            (Pattern.compile(/(.{3,}?)((і?[ую]в)?альни(к|ця)|(і?[ую]в)?ач(ка)?|онька)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(ів(ня|ник|ниця)?)$/)): '$1', // мандрівник
            (Pattern.compile(/(.{3,}?)(ер(ня|ник|ниця|ія)?)$/)): '$1', // швагер, парфумерія
            (Pattern.compile(/(.{3,}?)([аеєиіїоуюя]ц)ія$/)): '$1$2',
            (Pattern.compile(/(.{3,}?)(і[вй]ка)$/)): '$1',
            (Pattern.compile(/(.{3,}?)((іон|і|а)?[іиї]ст((оч)?ка)?|(іон|і|а)?[іиї]ст(ика|ія)|(іон|і|а)?[иії]зм)$/)): '$1',
            (Pattern.compile(/(.{3,}?)ика$/)): '$1',
            (Pattern.compile(/(.{3,}?)([аоія])ч(ок|ка|атура)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(івець|ець|ієць)$/)): '$1',

            (Pattern.compile(/(.{3,}?)(єць)$/)): '$1й', // малаєць
            (Pattern.compile(/(.{2,}?)(?<![аеєиіїоуюя'])ячко$/)): '$1',
            (Pattern.compile(/(.{3,}?)(ько|[чт]ко|е?н[еє]|[і]стя|'я|[иі]ще|ь?це|іше)$/)): '$1', // :n:
            (Pattern.compile(/(.{3,}?)(иса|ч?иня|(ій)?(?<![аеєиіїоуюя])ь?[нчщ]?иця|(?<![аеєиіїоуюя])чка|ька)$/)): '$1', // :f:
            
            (Pattern.compile(/(.{2,}?)([дзлнстчш])\2я$/)): '$1$2',
            (Pattern.compile(/(.{2,}?[аеєиіїоуюя])(ня)$/)): '$1н',
            (Pattern.compile(/(.{2,}?[^аеєиіїоуюя])(ня)$/)): '$1',
            (Pattern.compile(/([еиоудзжлнртсшч])я$/)): '$1',

            (Pattern.compile(/(.{2,}?)([вдзгжклмнрстхцчщ])[ео]$/)): '$1$2',
            (Pattern.compile(/(.{2,}?)([дзлнстчш])е$/)): '$1$2',

            (Pattern.compile(/(.{2,}?)(?<![аеєиіїоуюя])(к)а$/)): '$1',
            (Pattern.compile(/(.{2,}?)(к)а$/)): '$1$2',

            (Pattern.compile(/((ен)?(?<![аеєиіїоуюя])ь?[нчщ]?ик|ин|ич|ок|ко|ія|а|ор|няк|ій|ий|ит/
                    +/|ья|ь|ив|вля|ця|ьо)$/)): '',
            (Pattern.compile(/(.{2,}?)(ів|ент|є[нмрф]|ант|[аеоуюя]й/
                + /|[бвгґджзйклмнпртсхфчшь][бвгґджзклмнпрстфхцчшщ]|[аеуиіїоуюя][бвдгґзжнклмнпрстфцчхшщ])$/)): '$1$2',

//            (Pattern.compile(/(.{1,}?[^аеєиіїоуюя])(ка)$/)): '$1',
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
            
        if( line1.contains(" noun") ) counts['noun'] += 1
        else if( line1.contains(" adj") ) counts['adj'] += 1
        else if( line1.contains(" verb") ) counts['verb'] += 1
        else return null
        
        inflCnt++
        
//        lines = lines.collect{ it.replaceFirst(/ *#.*/, '') }
        line1 = lines[0].replaceFirst(/ *#.*/, '')

        def w = line1.replaceFirst(/ .*/, '')
        if( w in props )
            return null
            
        if( w in preStems ) {
            addRoot(preStems[w], w, true)
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
            addRoot(root, origW, false)
            return origW
        }

        Map<Pattern, String> replPair = line1 =~ /ий noun/
            ? SUFFIXES[' adj']
            : SUFFIXES.find{ k, v -> line1.contains(k) }.value

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
        addRoot(root, origW, false)
                
//        println "$line1 => $root"
        return root
    }

    @CompileStatic    
    private void addRoot(String root, String origW, boolean prestem) {
        if( root =~ /['ь]$/ ) {
            root = root.substring(0, root.length()-1)
        }
        else if( !prestem && root =~ /(?<!мене|коле)(дж)$/ ) {
            root = root.substring(0, root.length()-1)
        }
    
        def root2 = !prestem ? removePrefixes(root) : root
        def root2Lower = root2.toLowerCase()
        if( root2 != root || root.take(2) != origW.take(2) ) { //2nd for predef with prefix
            rootsPref[root2Lower] << origW
        }
        else {
            roots[root2Lower] << origW
        }
    }

    @CompileStatic    
    private String removePrefixes(String root) {
        if( ! (/^($noprefixes)/ =~ root ) ) {
//        if( ! (NON_PREFIXES_PATTERN.matcher(root).find()) ) {
            root = PREFIX_REMOVE.matcher(root).replaceFirst('')
        } 
        return root
    }
}

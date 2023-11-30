#!/bin/env groovy

package org.dict_uk.tools

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors

import groovy.transform.CompileStatic


class Stemmer {
    Map<String, Set<String>> roots = [:].withDefault { [] as Set }.asSynchronized()
    Map<String, Set<String>> rootsPref = [:].withDefault { [] as Set }.asSynchronized()
    Map<String, String> preStems = [:]
    Set<String> props = [] as Set
    Set<String> lemmas = [] as Set
    Set<String> lemmasPl = [] as Set
    Set<String> lemmasWithoutPrefix = ([] as Set).asSynchronized()
    def baseDir = new File(".").absolutePath.replaceFirst(/(dict_uk).*/, '$1')
    Map<String, Integer> counts = [:].withDefault { 0 }
    
    Stemmer() {
//        println "Base dir: $baseDir"
        loadPre()

        lemmasWithoutPrefix += [ 'грама', 'графія' ]
    }
    
    static void main(String[] args) {
        new Stemmer().findRoots()
	}
    
    void loadPre() {
        Set<String> stemSet = new HashSet<>()
        new File(getClass().getResource('stems.txt').toURI()).readLines()
            .each { line ->
                if( ! line ) return
                try {
                    def (stem, words) = line.split(/ - /)
                    if( stem in stemSet ) { 
                        println "Duplicate stem: $stem" 
                    } 
                    else { 
                        stemSet << stem 
                    }

                    words.split(/ /).findAll{ it }.each { w ->
                        if( w in preStems ) println "duplicate word: $w"
                        preStems[w] = stem 
                    }
                }
                catch(e) {
                    System.err.println "Failed to parse \"$line\""
                    e.printStackTrace()
                    System.exit(1)
                }
            }
            
        assert 'біоніка' in preStems
    }
    
    void readProps() {
        def geoFiles = ["geo-other.lst", "geo-ukr-koatuu.lst", "lang.lst", "geo-ukr-hydro.lst", "names-anim.lst", "names-other.lst", "pharm.lst"]
        geoFiles.each { f ->
            def lst = new File(baseDir, "data/dict/$f").readLines('utf-8')
                .collect{ line -> line.replaceFirst(/ .*/, '') } as Set
            props += lst
        }
        
        println "props: ${props.size()}: \"${props[0]}\""
    }
    
    Set<String> readWords() {
        def readGeosF = CompletableFuture.supplyAsync{ readProps() }

        def file = new File(baseDir, "out/dict_corp_vis.txt")

        Pattern discardPattern = Pattern.compile(/-|abbr|&pron|alt|arch|bad|slang|subst|[lp]name|comp[cs]|:nv/)

        def allLines = file.readLines('UTF-8')

        def readLemmasPl = CompletableFuture.supplyAsync{ 
            allLines.parallelStream()
                .filter{ line ->
                    line =~ / noun.*:p:/ && \
                        ! discardPattern.matcher(line).find()
                }
                .map { line -> line.trim().split(/ /)[0] }
                .collect(Collectors.toSet())
        }

        def lines = allLines.parallelStream()
            .filter { line -> 
                if( line.startsWith(" ") ) return false
                line = line.replaceFirst(/#.*/, '')
                if( discardPattern.matcher(line).find() )
                    return false
                return true
            }
            .toList()
        
        lemmas = lines.collect { line -> line.split(/ /)[0] } as Set
        readGeosF.whenComplete{}
        lemmasPl = readLemmasPl.get()
        println "lemmas: ${lemmas.size()}, lemmasPl: ${lemmasPl.size()}"
        assert "витрати" in lemmasPl
        
        lines
   }
    
	void findRoots() {
        
        def file = new File(baseDir, "out/dict_corp_vis.txt")
		def dir = Paths.get(file.getAbsolutePath())
		def inFile = dir.resolve(file.name)
        def outFile = new File(file.name + ".roots")
        outFile.text = ''
        def outFileTodo = new File(file.name + ".roots.todo")
        outFileTodo.text = ''
        def outFileBad = new File(file.name + ".roots.bad")
        outFileBad.text = ''

        def lines = readWords()
         
        lines.parallelStream().forEach { line ->
                findStem(line)
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
            def vvv = v.toSorted(coll).join(" ")
            outFile << "$root - ${vvv}"
            
            if( root.length() > 3 && root =~ /[аеєиіїоуюяь\']$/ ) {
                outFileBad << "Invalid root $root - ${vvv}\n"
            }
            
            def withpref = rootsPref[root].toSorted(coll)
            if( withpref ) outFile << "    ${withpref.join(' ')}" 
            outFile << "\n"
        }
//        println "ges: $cntGes"
        println counts
	}

    static String noprefixes = /автор|автоміст|автоген|авіарій|авіатор|антипод|міністер|мотор|спеціаліст|супереч|суперни[кц]/
    
    // TODO: без, багато
    static String prefixesBaseStrong = /без|багато|багато|біло|без/
    static final Pattern PREFIX_REMOVE_STRONG = Pattern.compile(/^($prefixesBaseStrong)/)

    // TODO: над, про, фін, пра
    static String prefixesBase = /авіа|авто|а[ву]діо|агро|аеро|аква|анти|біо|бого/ \
        + /|важко|велико|вело|вібро|взаємо|високо|відео|віце|внутрішньо|водо|вугле|вузько/ \
        + /|газо|гідро|гіро|гіпер|держ|еко(?!ном|лог)|екс(?!порт|нен|ном|тен[зс]|тер|тра|трем)|електро|етно|енерго/ \
        + /|євро|загально|звуко|зоо|квазі|кібер|кіно|контр|коротко|легко|лже/ \
        + /|магн[еі]то|макро|мало|мега|медіа|між|мікро|міні|моно|мото|мульти/ \
        + /|навколо|нано|напів'?|нафто|низько|ново|одно|пара|парт|пізньо|після|пневмо|політ?|пост|порно|псевдо|проти|прото/ \
        + /|радіо|ранньо|рівно|різно|само|середньо|слабк?о|соц(іо)?|спец|спів|старо|стерео|суб|теле|тепло|термо|тех|тонко|турбо/ \
        + /|ультра|фіто|фото|чорно/ \
        + /|нео(?=[аеєиіїоуюя])|супер|двох?|тр(и|ьох)|чотир(и|ьох)|п'ят(и|ьох)|шести|семи|восьми|дев'яти|десяти|не/
    static String prefixes = "все|усе|$prefixesBase"
    static final Pattern PREFIX_REMOVE = Pattern.compile(/^($prefixesBase)/)

    static String prefixesBaseSoft = /агіт|адмін|арт|архі|броне|веб|вет|вибухо|вітро|вогне|волого|все/ \
        + /|гастро|геліо|гемо|гео|гетеро|гіпо|гостро|двадцяти|дванадцяти|де|дип|довго|дрібно|ізо|імуно|інвест|інформ|інфо/ \
        + /|кардіо|крипто|культуро|культ|лімфо|лінгво|лісо|літ|мед|метало|мета|метео|над|нарко|нейро|обл|онко/ \
        + /|пів(тора)?|по(за|на|по)|пра(во|пра)?|при|проф?|психо|рентгено|ретро|свіжо|світло|сільгосп|спорт|транс|фарм(ако)?|фін|хім|хлібо|швидко|широко/
    static final Pattern PREFIX_REMOVE_SOFT = Pattern.compile(/^($prefixesBaseSoft)'?/)
    
    
    static String preReg = /^($prefixes)?+/
    
//    static Pattern PREFIX = Pattern.compile(/^(по|над|про|за)/)
    static Map<String, Map<Pattern, String>> SUFFIXES = Map.of(
        // TODO: актуальнішати/актуальніший + ішання
        // ст -> щ
        // изна
        " verb", [
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])(?<!сол)овіти(ся)?$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])н?ішати(ся)?$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])онути(ся)?$/)): '$1',
            (Pattern.compile(/(.{3,}?)([вм])л([яи]|юва)ти(ся)?$/)): '$1$2',
//            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])янути(ся)?$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[у]ювати(ся)?$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])([іояае])ювати(ся)?$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])ствувати(ся)?$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])(([иі]з)?(ов)?ува|л?юва|ну|[аеєиіїоуюя])?ти(ся)?$/)): '$1'
            ],
        " adj", [
            (Pattern.compile(/(річн|хвост|голов|зерн(ист)?)ий$/)): '$1',
            (Pattern.compile(/(терап)евтичний$/)): '$1',
            (Pattern.compile(/(руськ|поган|багат)(е(се)?ньк)?ий$/)): '$1',
            (Pattern.compile(/([мр]отор|креатив|мистець|турець|грець|хвост)([кн]ий)$/)): '$1',
            (Pattern.compile(/(креац|над)ій([кн]ий)$/)): '$1',
            (Pattern.compile(/((?<!бо)реал)ьний$/)): '$1',
            (Pattern.compile(/([нгрх])еальний$/)): '$1',
//            (Pattern.compile(/(мебл|магл)ьований/)): '$1',
            (Pattern.compile(/(бу|секре|компози|зекуц|бі|ди|ститу|моц)(ційний|торний)$/)): '$1т',
            (Pattern.compile(/((?<!нс)тру)(юва(ль)?ний|й(ова)?ний)$/)): '$1й',
            (Pattern.compile(/(.{3})((ов)?о|е)подібний$/)): '$1',

            (Pattern.compile(/(.{3})([нт])\2євий$/)): '$1$2',

            (Pattern.compile(/(.{3})(?<!гот)овчий$/)): '$1',
            (Pattern.compile(/(.{3})уш(ков|ечн|н)ий?$/)): '$1',
            (Pattern.compile(/(.{3})(нісний|ущий)$/)): '$1',
//            (Pattern.compile(/(.{3})ярний$/)): '$1',
//            (Pattern.compile(/(.{3})атичний$/)): '$1',
            (Pattern.compile(/(.{3})час?тий$/)): '$1',

//            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])в[лн]яльний$/)): '$1в',

            (Pattern.compile(/(.{3})ив(істський|істичний|ізова?ний)$/)): '$1',
            (Pattern.compile(/(ст)иційний|(?<![аеєиіїоуюя])ці(йний|оністський)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<!міт)[іи]нг(ов(ан)?ий|увальний)$/)): '$1',
            (Pattern.compile(/(.{3,}?)[і]євий$/)): '$1',
            (Pattern.compile(/(.{3,}?)(ю[щч]ий)$/)): '$1',
            (Pattern.compile(/(.{2,}?)([іоае])[яє]н(ий)$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)(л)яційний$/)): '$1$2',
            (Pattern.compile(/(.{3,}?)[уі][ая](льний|тивний|ційний)$/)): '$1',
            (Pattern.compile(/(.{3,}?)[ауіе]їст(ичний|ський)$/)): '$1',
            (Pattern.compile(/(.{3,}?)([о])їст(ичний|ський)$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)(?<!п)леч(ковий|ний)$/)): '$1',
            (Pattern.compile(/(.{2,}?)ллястий$/)): '$1л',
            (Pattern.compile(/(.{2,}?)((ени)цький|еський)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])([оу]в)?([аеяю]цький|альницький)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])([яю]ковий|[яю]куватий|[ая]стий)$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[лн][ея]ний$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[лн]юваний$/)): '$1',

            
            (Pattern.compile(/(.{2,}?)([оеа])[ую]ва((ль)?н|т)ий$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)овува(ль)?ний$/)): '$1',
            (Pattern.compile(/(.{2,}?)([а])ячий$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])([лн]|[іи]з|і)[ую]вальний$/)): '$1',
            (Pattern.compile(/(.{3,}?)і?[ую]вальний$/)): '$1',
            (Pattern.compile(/(.{2,}?)([оа])йований$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)[уі]йований$/)): '$1',
            (Pattern.compile(/(.{2,}?)їстий$/)): '$1й',
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])[ін]?[ую]в)?атий$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])нутий$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])чатий$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])(?<!цв|зр|мл|пр|сп)ілий$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])(([іи]з)?і?[яеа])?тор(ний|ський)|іатурний)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])н)?[ую]ва[нт]ий|іюваний|іюватий)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(([іи]з|й|ь)?ований|([іи]з|(?<![аеєиіїоуюя])(л|і))?([ую]в)?альний)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(((ер)?[іи]з)?[а]ційний|ез(н|ійн|[іи]чн)ий|(ист)?ійний|[іия]стий|(а?[іїи]ст)?[іи][вч]ний|лив(еньк)?ий)$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)(ерний|ейний|ерський|[еі]йський|иний)$/)): '$1',
            (Pattern.compile(/(.{3})ерий$/)): '$1',
            (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])н)?([ію]сінький|(ат)?(ес)?енький))$/)): '$1', // дивнесенький
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])н(ений|івський)|івницький)$/)): '$1',
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])л)?ений$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])лий$/)): '$1', // (еньк|ов)? - too many FP
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])ев(еньк|уват)?ий$/)): '$1',

            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])(?<!м'|сц|слов')ян(ськ|ков)?ий$/)): '$1',
            (Pattern.compile(/(.{2}[аеєиіїоуюя][^аеєиіїоуюя])(?<!вор)отний$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)(аний|((?<![аеєиіїоуюя])к|ь)?овий)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(ік)?(?<!ом)овний$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])н?и[сц]ький$/)): '$1',
            (Pattern.compile(/(.{3,}?)([іоеу]вськ|ь?[цс]ьк|к?ов|ь?ницьк|инський|іоністськ|[иії]стськ|істов|ст|яч|(?<![аеєиіїоуюя])ь?[нчк])?([іи]й)$/)) : '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])ч?ів$/)) : '$1'
                ],
                //TODO: идло учий куватий иченька ьоха уваннячко
        " noun", [
            (Pattern.compile(/(хвост|голов|(?<!фре)зерн(ист)?)ість$/)): '$1',
            (Pattern.compile(/([рм]отор|креатив)(ність|ник|ниця)?$/)): '$1',
            (Pattern.compile(/(креац|над)(ій(ність|ник|ниця)|ія)?$/)): '$1',
            (Pattern.compile(/(мистец)(ь|тво)?$/)): '$1',
            (Pattern.compile(/(терап)евт(ка)?$/)): '$1',
            (Pattern.compile(/(річка)$/)): 'річ',
            (Pattern.compile(/(еат)(ка|ство)?$/)): '$1',
            (Pattern.compile(/(поган)е$/)): '$1',
            (Pattern.compile(/(буд)ова$/)): '$1',
            (Pattern.compile(/(реал)(іст(ка)?|ізм|ьність)$/)): '$1',
            (Pattern.compile(/((?<!нс)тру)(юваність|й(ова)?ність)$/)): '$1й',
            (Pattern.compile(/(.{3})((ов)?о|е)подібність$/)): '$1',

            (Pattern.compile(/(.{3})нісність?$/)): '$1',
            (Pattern.compile(/(?<![аеєиіїоуюя])ну(тість|ття)$/)): '',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])(івна|чук)$/)): '$1',
//            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])(ант(ка)?|ативність|аційність)$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)нша$/)): '$1н',
            (Pattern.compile(/(.{3,}?)ю([щч]ість|ча|чок|ченя|чисько|чиння|чище|чник|ччя)$/)): '$1',
//            (Pattern.compile(/(ор(юв)?)ане$/)): '$0',
            (Pattern.compile(/(а)(не)$/)): '', // спродане
//            (Pattern.compile(/(.{3})ема$/)): '$1',
            (Pattern.compile(/(.{3,}?)((ени)цтво|ество|еньк[ао]|енко)$/)): '$1',
            (Pattern.compile(/(.{3,}?)у(ація|йованість|ювання|атор(ка)?)$/)): '$1',

            (Pattern.compile(/(.{3,}?)(?<!міт)[іи]нг(іст(ка)?|ізм|ування)?$/)): '$1',
            (Pattern.compile(/(?<![аеєиіїоуюя])ціон(іст(ка)?|ізм)$/)): '',
            (Pattern.compile(/(.{3})(ущість)$/)): '$1',
            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])(ськість|ська)$/)): '$1',
//            (Pattern.compile(/(.{3,}?)изна$/)): '$1',

            (Pattern.compile(/(.{3})час?тість$/)): '$1',
            
            // :ns
            (Pattern.compile(/(.{2})(?<![аеєиіїоуюя])([ое]ньки|чики|[іи]вки|[аи]?ки|[ео]чки|(ин)?и|енята|ц?ята)$/)): '$1',
            
            (Pattern.compile(/(.{3})(уш(еч)?ка|анина|икиня|(?<![аеєиіїоуюя])нішання)$/)): '$1', // |ун(чик|ець|ка|ство)
//            (Pattern.compile(/(.{3})ат$/)): '$1',
            
            // вл/ея
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[лн][ея](ність)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[лн][ея](ни(чо)?к|н(оч)?ка)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[лн]?[еія](ння(чко)?)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])[вл]?[ея](ння)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])е[нв](ість|е)$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)([тн])\2є(вість|вик)$/)): '$1$2',
            (Pattern.compile(/(.{2,}?)([джлстч])\2я(чко)?$/)): '$1$2',
            
            // й
            (Pattern.compile(/(.{2,}?)[єї](ння|стість)$/)): '$1й',
            (Pattern.compile(/(.{2,}?)([іоае])[яє]н(ня|ість|(оч)?ка|ин)$/)): '$1$2й',
            (Pattern.compile(/(.{2})([оеа])[ую]ва(ння|ність)$/)): '$1$2й',

            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])н[ую]вач$/)): '$1',
            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])айло$/)): '$1',
            
            (Pattern.compile(/(.{2})ея$/)): '$1ей',

            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])чин((онь|оч)?к)?а$/)): '$1',
            
            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])[лі]юва(ння|(ль)?ність)$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)([лмн])(?<!пл)ічка$/)): '$1$2',
            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])(?<!м'|сц|слов')ян((оч)?ка|иця|ик|ин|ість|ство)$/)): '$1',
            
            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])((ів)?н)?и[сц]тво$/)): '$1',
            // ив
            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])ив(ність|іст(ка)?|істика|ізм|ізація|ізатор(ка)?|чик)$/)): '$1',
            
            
            (Pattern.compile(/(.{3})(?<!устан|гол|гот|зам|засн|стан|здор|підк)ов(н?ик|н?ичок|ичка|н?иця|ець|ка|ня|щина|ство|ізація)$/)): '$1',
            (Pattern.compile(/(.{3,}?)о(вуван(ість|ня)|вувач(ка)?|вання|вість)$/)): '$1',
            
            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])(ь?щина|атина)$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])(л?([юу]в)?[аеяю](цтво|ч(ка)?))$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])([яію]ка?)$/)): '$1',
            (Pattern.compile(/(.{3,}?)[уе](їст(ка)?|їстика|їстичність|їзм)$/)): '$1',
            (Pattern.compile(/(.{3,}?)([еоя])(їст(ка)?|їстика|їстичність|їзм)$/)): '$1$2й',
//            (Pattern.compile(/(.{3,}?)([ая])ка$/)): '$1',
            (Pattern.compile(/(.{3,}?)ли(вість|виця|вець|вка|вчик|вство)$/)): '$1',
            (Pattern.compile(/(.{3,}?)[іиая]стість$/)): '$1',
            (Pattern.compile(/(.{3,}?)(?<![аеєиіїоуюя])(ув)?ал(ка|ьня|ьце|о|ення|ювання)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(ичка|иха|юга|юра|ятина)$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)(а)юватість$/)): '$1$2й',
            (Pattern.compile(/(.{2,}?)([оа])йованість$/)): '$1$2й',
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])[ін]?[ую]в)?атість$/)): '$1',
            (Pattern.compile(/(.{3,}?)([уо]н)ня$/)): '$1$2',
            (Pattern.compile(/(.{3,}?)(((?<![аеєиіїоуюя])[ні])?[ую]ва([тн]ість|льність|ння)|([іи]з|і?й)?ованість|([іи]з|ер)?(іон)?[ую]вання|(а?[іїи]ст)?[іи]чність|[аи]ність)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(ез(ія|ійність|ичність)?)$/)): '$1', // біогенез
            (Pattern.compile(/(.{3,}?)(ік)?(?<!ом)овність$/)): '$1',
            (Pattern.compile(/(бу|б'ю|секре|компози|зекуц|бі|ди|ститу|моц)(ція|ційність|тор)$/)): '$1т',
            (Pattern.compile(/(ст)иц(ія|ійність)$/)): '$1', // інвестиція

            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])(?<!цв|зр|мл|пр|сп)ілість$/)): '$1',
            
            (Pattern.compile(/(.{3,}?)(еса|ерка|ікат|(?<![аеєиіїоуюя])іат(ка)?)$/)): '$1',
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])ція|((ер)?[иі]з|і)?[ая]ція|(яц)?[еі](йник|йність))$/)): '$1',
            (Pattern.compile(/(.{3,}?)((?<![аеєиіїоуюя])(([іи]з)?і?[яеа])?тор(ка|ство|ій|ник|ниця)?|ей|ерик|ік(иня)?)$/)): '$1',
            (Pattern.compile(/(.{3,}?)([еі]йство|(ер)?ь?ство|(ів)?(ни)?[цс]?тво)$/)): '$1', // байкерство
            (Pattern.compile(/(.{3,}?)(стість|([іу]?ал)?(?<![аеєиіїоуюя])ь?[клнт]ість|ість)$/)): '$1', // :n:
            (Pattern.compile(/(.{3,}?)(([тджл])\2я)$/)): '$1',
            (Pattern.compile(/(.{3,}?)((і?[ую]в)?альни(к|ця)|(і?[ую]в)?ач(ка)?|онька)$/)): '$1',
            (Pattern.compile(/(.{3,}?)(і[вй](ня|ник|ниця|ка)?)$/)): '$1', // мандрівник
            (Pattern.compile(/(.{3,}?)(ер(ня|ник|ниця|ія)?)$/)): '$1', // швагер, парфумерія
            (Pattern.compile(/(.{3,}?)([аеєиіїоуюя]ц)ія$/)): '$1$2',
            (Pattern.compile(/(.{3,}?)(іон|і|а)?[іиї](ст((оч)?ка)?|ст(ика|ія)|зм)$/)): '$1',
            (Pattern.compile(/(.{3,}?)ика$/)): '$1',
            (Pattern.compile(/(.{3,}?)([аоія])ч(ок|ка)$/)): '$1',

            (Pattern.compile(/(.{2}[аеєиіїоуюя][^аеєиіїоуюя])(?<!вор)от(а|иння|не)$/)): '$1',
            
//            (Pattern.compile(/(.{3}і)йка$/)): '$1й',
            (Pattern.compile(/(.{3})(івець|ець|ієць)$/)): '$1',
            (Pattern.compile(/(.{2})(єць)$/)): '$1й', // боєць
            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])(ечко|ов[іи]ння|овість|овисько|(?<!сх)овище)$/)): '$1',
            (Pattern.compile(/(.{2})(?<![аеєиіїоуюя])ч((а|еня)(т(оч)?ко)?|ище)$/)): '$1',
            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])(((ен|')?я|а)т(оч)?ко)$/)): '$1',
            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])('яга|еня(тко)?|ч?исько)$/)): '$1',
            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])[иа]?ння(чко)?$/)): '$1',
            (Pattern.compile(/(.{3})(ячко|ько|[чт]ко|е?н[еє]|[і]стя|'я|[иі]ще|ь?це|іше)$/)): '$1', // :n:
            (Pattern.compile(/(.{3})(?<![аеєиіїоуюя])(иса|ч?иня|(ій)?ь?[нчщ]?и(к|ця)|чка|ька)$/)): '$1', // :f:
            
            (Pattern.compile(/(.{2})([дзлнстчш])\2я$/)): '$1$2',
            (Pattern.compile(/(.{2}[аеєиіїоуюя])(ня)$/)): '$1н',
            (Pattern.compile(/(.{2}[^аеєиіїоуюя])(ня)$/)): '$1',
            (Pattern.compile(/([еиоудзжлнртсшч])я$/)): '$1',

            (Pattern.compile(/(.{2})([вдзгжклмнрстхцчшщ])[ео]$/)): '$1$2',
            
            (Pattern.compile(/(.{2})(?<![аеєиіїоуюя])(к)а$/)): '$1',
            (Pattern.compile(/(.{2})(к)а$/)): '$1$2',
            (Pattern.compile(/(.{3})ичок$/)): '$1',
            
            (Pattern.compile(/((ен)?(?<![аеєиіїоуюя])ь?[нчщ]?ик|ин|ич|ок|ко|ія|а|ор|няк|[иі]й|ит|ья|ь|ив|вля|ця|ьо)$/)): '',
            (Pattern.compile(/(.{2})(ів|ент|є[нмрф]|ант|[аеиоуюя]й)$/)): '$1$2',
            (Pattern.compile(/(.{2})([бвгґджзйклмнпртсхфчшь][бвгґджзклмнпрстфхцчшщ]|[аеуиіїоуюя][бвдгґзжнклмнпрстфцчхшщ])$/)): '$1$2',
                ],
        )
    final Pattern NON_PREFIXES_PATTERN = Pattern.compile(/^($noprefixes)/)
    final Pattern PREFIXES_PATTERN = Pattern.compile(/^($prefixes)/)
    
    int inflCnt = 0
    List<String> todo = []

    @CompileStatic
    String findStem(List<String> lines) {
        findStem(lines[0])
    }
        
    @CompileStatic
    String findStem(String line1) {

//        if( lines[0] =~ /^((що)?як)?най.*? adj/ )
//            return null
            
        if( line1.contains(" noun") ) counts['noun'] += 1
        else if( line1.contains(" adj") ) counts['adj'] += 1
        else if( line1.contains(" verb") ) counts['verb'] += 1
        else return null
        
        inflCnt++
        
//        lines = lines.collect{ it.replaceFirst(/ *#.*/, '') }
        line1 = line1.replaceFirst(/ *#.*/, '')

        String w = line1.replaceFirst(/ .*/, '')
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
                pref = m.group(0)
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
        root = addRoot(root, origW, false)

//        println "$line1 => $root"
        return root
    }

    @CompileStatic
    private String addRoot(String root, String origW, boolean prestem) {
        if( root =~ /['ь]$/ ) {
            root = root[0..-2]
        }

        def root2 = !prestem ? removePrefixes(root, origW) : root
        def prefixRemoved = root2.take(3) != origW.take(3)

        if( ! prestem ) {
          if(  root2 =~ /(?<!мене|коле)(дж)$/ ) {
             root2 = root2[0..-2]
          }
          else if( root2 =~ /[жт]ч$/
              && ! (root2 =~ /(диспетч|дзижч|матч|отч|притч|бряжч)$/) ) {
            root2 = root2[0..-2]
          }
          else if( root2.length() > 3
              && root2 =~ /[бвгґджзклмнпрстфхцчшщ]н$/
              && ! (root2 =~ /(асиг|баг|маг|валтор|вап|вес|верес|вестер|відчиз|вов|єд|зер|зна|гар|гі[мв]|гор|дог|естер|ет|ет|із|іс|пір|тем|фав|чор|яс)н$/)
              && root2[-2..-1] != origW[-2..-1] ) {
            root2 = root2[0..-2]
          }
        }

        def root2Lower = root2.toLowerCase()
        if( prefixRemoved ) { //2nd for predef with prefix
            rootsPref[root2Lower] << origW
        }
        else {
            roots[root2Lower] << origW
        }
        return root2Lower
    }

    @CompileStatic
    private String removePrefixes(String root, String origW) {
        if( ! (/^($noprefixes)/ =~ root ) ) {
            def m = PREFIX_REMOVE_STRONG.matcher(root)
            if( m ) {
                root = m.replaceFirst('')
                lemmasWithoutPrefix << PREFIX_REMOVE_STRONG.matcher(origW).replaceFirst('')
            }
            else {
                def mW = PREFIX_REMOVE.matcher(origW)
                if( ! mW ) {
                    mW = PREFIX_REMOVE_SOFT.matcher(origW)
                    m = PREFIX_REMOVE_SOFT.matcher(root)
                }
                else {
                    m = PREFIX_REMOVE.matcher(root)
                }

                if( mW ) {
                    def w2 = mW.replaceFirst('')
                    if( w2.length() > 2 ) {
                        if( recognize(w2) ) {
                            root = m.replaceFirst('')
                            lemmasWithoutPrefix << w2
                        }
                    }
                }
            }
        } 
        return root
    }
    
    @CompileStatic
    boolean recognize(String w2) {
        return w2 in lemmas || (w2.endsWith("и") && w2 in lemmasPl) || w2 in lemmasWithoutPrefix
    }
}

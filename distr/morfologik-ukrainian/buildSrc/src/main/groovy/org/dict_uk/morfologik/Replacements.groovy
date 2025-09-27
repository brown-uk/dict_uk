package org.dict_uk.morfologik

import groovy.transform.CompileStatic

public class Replacements {
    static { println new File(".").absolutePath }
    
    private static final int MAX_REPLACEMENTS = 5
    private static Map<String,String> DERIVATS
    
    private static void loadDerivats(String srcDir) {
        if( DERIVATS != null ) 
            return

        DERIVATS = new File("$srcDir/../../out/derivats.txt").readLines().collectEntries{
            def (advp, verb)=it.split(" ")
            [(verb):advp]
        }
        println "Loaded ${DERIVATS.size()} advp derivats"
    }

    @CompileStatic
    public static List<String> getReplacements(String srcDir, List<File> files, Closure filter) {
        loadDerivats(srcDir)
        
        List<String> outLines = []
        List<String> outDerivats = []
        
        files.each{ srcFile ->
            int tooManyReplacementsCount = 0
            List<String> rvLines = new File("$srcDir/$srcFile.name").readLines()
                    .findAll {
                        ! it.startsWith('#') && filter(it)
                    }.collect{ line ->
                        def it = line
                        it = it.replaceFirst(/\s*# rv[^\s]+/, '')
                        
                        if( srcFile.name.contains('composite') ) {
                            it = it.replaceFirst(/^([а-яіїєґА-ЯІЇЄҐ'-]+).*? - ([а-яіїєґА-ЯІЇЄҐ'-]+).*#>>? *(.*)/, '$1-$2=$3')
                        }
                        else {
                            it = it.replace(' +cs=', '')
                            it = it.replaceFirst(/^([а-яіїєґА-ЯІЇЄҐ'-]+).*#>>? *(.*)(#ok:.*)?/, '$1=$2')
                        }
                        
                        String[] lineParts = it.split("=")
                        String replStr = lineParts[1]
                        String[] parts = replStr.split(/\|/)
                        if( parts.length > MAX_REPLACEMENTS ) {
                            it = lineParts[0] + "=" + parts[0..3].join("|") + "|" + parts[4..-1].join("; ")
                            tooManyReplacementsCount++
//                            if( srcFile.name == "base.lst")
//                            println "Adjusted to $it"
                        }
                        
                        if( line =~ / \/v[1-5].*/ ) {
                            def lemma = lineParts[0]
                            def deriv = DERIVATS[lemma]
//                            println ":: $lemma = $deriv"

                            if( deriv ) {
                                String derivLine = it.replaceFirst(/^$lemma=/, "$deriv=")
                                def prs = derivLine.split(/=/)
                                def advpLemma = prs[0]
                                def verbReplStr = prs[1] 
                                def verbRepls = ((String)verbReplStr).split(/\|/) as List
                                verbRepls = verbRepls.collect { DERIVATS[it] ?: it }
                                derivLine = "$advpLemma=${verbRepls.join('|')}".toString() 
                                outDerivats += derivLine
                            }
                        }

                        it
                    }

            outLines.addAll(rvLines)
            
            if( tooManyReplacementsCount ) {
                println "INFO: merged ${tooManyReplacementsCount} replacements to fit into 5 for ${srcFile.name}"
            }
        }

        outLines += outDerivats
        println "INFO: found ${outDerivats.size()} derivat replacements"
        
        java.text.Collator coll = java.text.Collator.getInstance(new Locale("uk", "UA"))
        coll.setStrength(java.text.Collator.IDENTICAL)
        coll.setDecomposition(java.text.Collator.NO_DECOMPOSITION)
        Collections.sort(outLines, coll)

        outLines
    }

}
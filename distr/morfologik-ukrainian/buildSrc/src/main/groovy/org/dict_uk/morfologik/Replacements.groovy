package org.dict_uk.morfologik

import groovy.transform.CompileStatic

public class Replacements {
    private static final int MAX_REPLACEMENTS = 5

    @CompileStatic
    public static List<String> getReplacements(String srcDir, List<File> files, Closure filter) {
        List<String> outLines = []

        files.each{ srcFile ->
            int tooManyReplacementsCount = 0
            List<String> rvLines = new File("$srcDir/$srcFile.name").readLines()
                    .findAll {
                        ! it.startsWith('#') && filter(it)
                    }.collect{
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
                        it
                    }

            outLines.addAll(rvLines)
            if( tooManyReplacementsCount ) {
                println "INFO: merged ${tooManyReplacementsCount} replacements to fit into 5 for ${srcFile.name}"
            }
        }

        java.text.Collator coll = java.text.Collator.getInstance(new Locale("uk", "UA"))
        coll.setStrength(java.text.Collator.IDENTICAL)
        coll.setDecomposition(java.text.Collator.NO_DECOMPOSITION)
        Collections.sort(outLines, coll)

        outLines
    }

}
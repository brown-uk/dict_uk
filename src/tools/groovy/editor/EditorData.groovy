package editor

import org.dict_uk.expand.Expand

import groovy.transform.CompileStatic

@CompileStatic
public class EditorData {
    class EntryInfo {
        String word
        String flags
        String dicts
        String comment
        int cnt
        java.util.List<String> context = []
    }
    
    java.util.List<EntryInfo> inputData = []
    def media = []
    List<String> newWords = []
    Expand expand = new Expand(false)
    
    Map<String,String> dictLines = [:]
    
    String inputFile
    
    public EditorData(String[] args) {
        inputFile = args.length >= 1 ? args[0] : 'out/toadd/unknown_lemmas.txt'
        load();
    }

    private void load() {
        EntryInfo currentEntry = null
        new File(inputFile).readLines("UTF-8")
                .each{
                    if( ! it.trim() ) return
                    
                    if( it.startsWith(' ') ) { // context
                        assert currentEntry, "Invalid indented context"
                        currentEntry.context << it
                    }
                    else {
                        currentEntry = new EntryInfo()
                        def firstMetaLine = []

                        if( it.contains('[') ) {
                            def prts = it.split(/\[/, 2)
                            it = prts[0].trim()
                            currentEntry.dicts = '[' + prts[1].trim()
                            firstMetaLine << currentEntry.dicts
                        }

                        if( it.contains('#') ) {
                            def prts = it.split('#', 2)
                            it = prts[0].trim()
                            currentEntry.comment = prts[1].trim()
                        }
                        
                        def parts = it.split(/\s+/)
                        def idx = 0
                        if( parts[0] ==~ /[0-9]+/ ) {
                            currentEntry.cnt = parts[0] as Integer
                            idx += 1
                            firstMetaLine.add(0, parts[0])
                        }
                        currentEntry.word = parts[idx]
                        idx += 1
                        if( parts.size() > idx && parts[idx] =~ / \/|:/ ) {
                            currentEntry.flags = parts[idx]
                            idx += 1
                        }

                        if( parts.size() > idx && parts[idx] ) {
                            firstMetaLine << parts[idx..-1]
                            idx += 1
                        }
                        if( firstMetaLine ) {
                            currentEntry.context << firstMetaLine.join(' ')
                        }
                        inputData << currentEntry
                    }
                }

        println "Loaded ${inputData.size()} new words"

        println "Loading existing words..."

        new File('data/dict')
                .listFiles()
                .each { File file ->
                    if( file.name.endsWith('.lst') ) {
                        def tag = file.name != "base.lst" ? file.name : ""
                        file.readLines("UTF-8").each {
                            dictLines[it] = tag
                        }
                    }
                }

        def newLemmaFile = new File('out/toadd/media_src.txt')
        if( newLemmaFile.exists() ) {
            println "Loading media_src..."
            media = newLemmaFile.readLines("UTF-8").collectEntries {
                //      def parts = it.split('@@@')
                def parts = it.split(/ +/, 2)
                [ (parts[0]): parts.length > 1 ? parts[1..-1] : ["---"] ]
            }
        }

        println "Input data: dict lines: ${dictLines.size()}"

        expand.affix.load_affixes('data/affix')
    }
    
    void save() {
        new File('new_words.lst') << newWords.join('\n') + '\n'
        newWords.clear()
    }
    
    static def getDefaultTxt(String word) {
        String word_txt = word
        switch( word ) {
            case ~/^[А-ЯІЇЄҐ]{2,}$/:
                word_txt += ' noun:X:nv:prop              # <пояснення>     #=> base-abbr'
                break;
            case ~/.*[иі]й$/:
                word_txt += ' /adj'
                break;
                
            case ~/.*(ість)$/:
                word_txt += ' /n30'
                break;
            case ~/.*([еє]ць)$/:
                word_txt += ' /n22.a.p'
                break;
            case ~/.*(олог)$/:
                word_txt += ' /n20.a.p.<'
                break;
            case ~/.*(знавство)$/:
                word_txt += ' /n2n'
                break;
            case ~/.*(метр)$/:
                word_txt += ' /n20.a.p.ke'
                break;
            case ~/.*([^аеєиіїоуюя])$/:
                word_txt += ' /n20.p'
                if( word.endsWith('р') )
                    word_txt += '.ke'
                break;
    
            case ~/.*(ння|ття|сся|ззя|тво|ще)$/:
                word_txt += ' /n2n.p1'
                break;
    
            case ~/.*(ччя)$/:
                word_txt += ' /n2n'
                break;
    
            case ~/.*[ую]вати$/:
                word_txt += ' /v1 :imperf'
                break;
            case ~/.*[ую]ватися$/:
                word_txt += ' /vr1 :imperf'
                break;
            case ~/.*ти$/:
                word_txt += ' /v1 :imperf'
                break;
            case ~/.*тися$/:
                word_txt += ' /vr1 :imperf'
                break;
                
                
            case ~/.*(огія)$/:
                word_txt += ' /n10'
                break;
            case ~/.*([аеєиіїоуюя]ка|[^к]а|ія|я)$/:
                word_txt += ' /n10.p1'
                break;
            case ~/.*([^аеєиіїоуюя]ка)$/:
                word_txt += ' /n10.p2'
                break;
                
            case ~/.*и$/:
                word_txt += ' /np2'
                break;
    
            case ~/.*о$/:
                word_txt += ' adv'
                break;
        }
    
        word_txt
    }
    
}

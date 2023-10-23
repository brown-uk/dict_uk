package org.dict_uk.tools

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

import groovy.transform.CompileStatic

class StemmerTest {
    Stemmer stemmer =  new Stemmer()
    
    @Test
    void test() {
//        assertEquals "став", Roots.findRoot("поставати поставати verb:imperf:inf\n"
//                + "  постає поставати verb:imperf:pres:s:3")
//        assertEquals "аахен", stemmer.findStem(["аахенський аахенський adj:m:v_naz"])
        
        
        assertEquals "вдув", stemmer.findStem(["вдувальний вдувальний adj:m:v_naz"])
        assertEquals "загой", stemmer.findStem(["загоюваний загоюваний adj:m:v_naz"])
        assertEquals "вчен", stemmer.findStem(["вчений вчений adj:m:v_naz"])
        assertEquals "бад", stemmer.findStem(["баддя баддя noun:inanim:f:v_naz"])
        assertEquals "автомит", stemmer.findStem(["автомито автомито noun:inanim:n:v_naz"])
        assertEquals "абон", stemmer.findStem(["абонування абонування noun:inanim:f:v_naz"])
        assertEquals "авіаімпер", stemmer.findStem(["авіаімперія авіаімперія noun:inanim:f:v_naz"])
        assertEquals "ареліг", stemmer.findStem(["арелігійний арелігійний adj:m:v_naz"])
        assertEquals "акц", stemmer.findStem(["акційний акційний adj:m:v_naz"])
        assertEquals "постав", stemmer.findStem(["поставлений поставлений adj:m:v_naz"])

        assertEquals "дів", stemmer.findStem(["дівка дівка noun:anim:f:v_naz"])
        assertEquals "канад", stemmer.findStem(["канадійка канадійка noun:anim:f:v_naz"])
        assertEquals "авдіофайл", stemmer.findStem(["авдіофайл авдіофайл noun:inanim:m:v_naz"])
        
//        assertEquals "стел", Roots.findRoot("простелити /v1")
        assertEquals "лікар", stemmer.findStem(["лікарський лікарський adj:m:v_naz"])
        
        assertEquals "морож", stemmer.findStem(["морожений морожений adj:m:v_naz"])
        
    }
    
    @Test
    void testFile() {
        def lines = new File("data/stem/stems.lst").readLines('utf-8').take(200)
        
        lines.each { l ->
            def (stem, wordss) = l.split(/ - /)
            def words = wordss.split(/ /)
            
            words.each { w ->
                def pos = w =~ /(ий|ів)$/ ? "adj:m:v_naz" : "noun:inanim:m:v_naz"
                String got = stemmer.findStem(["$w $w $pos".toString()])
                println "got: $got"
                assertEquals stem, got,  "for $w"
            }
        }
        
    }
}

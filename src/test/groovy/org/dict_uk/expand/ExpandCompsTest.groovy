package org.dict_uk.expand


import static org.junit.jupiter.api.Assertions.assertEquals

import org.dict_uk.common.DicEntry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import groovy.transform.CompileStatic

@CompileStatic
public class ExpandCompsTest {
	
	static ExpandComps expandComps
	
	@BeforeEach
	void setUp() {
        if( expandComps == null ) {
            def affixDir = new File("data/affix").isDirectory() ? "data/affix" : "../../../data/affix"
            Args.parse(["--aff", affixDir, "--dict", ""].toArray(new String[0]))

            def expand = new Expand(false)
            expand.affix.load_affixes(affixDir)
            expandComps = new ExpandComps(expand)
        }
	}
	
	
	def fullComps =
'''
Афанасьєв-Чужбинський Афанасьєв-Чужбинський noun:anim:m:v_naz:prop:lname
Афанасьєве-Чужбинський Афанасьєв-Чужбинський noun:anim:m:v_kly:prop:lname
Афанасьєва-Чужбинського Афанасьєв-Чужбинський noun:anim:m:v_rod:prop:lname
Афанасьєва-Чужбинського Афанасьєв-Чужбинський noun:anim:m:v_zna:prop:lname
Афанасьєву-Чужбинському Афанасьєв-Чужбинський noun:anim:m:v_dav:prop:lname
Афанасьєву-Чужбинському Афанасьєв-Чужбинський noun:anim:m:v_mis:prop:lname
Афанасьєві-Чужбинському Афанасьєв-Чужбинський noun:anim:m:v_mis:prop:lname
Афанасьєвим-Чужбинським Афанасьєв-Чужбинський noun:anim:m:v_oru:prop:lname
Афанасьєву-Чужбинськім Афанасьєв-Чужбинський noun:anim:m:v_mis:prop:lname
Афанасьєві-Чужбинськім Афанасьєв-Чужбинський noun:anim:m:v_mis:prop:lname
'''.trim()
	
	@Test
	void testNounName() {
		def input = ["Афанасьєв /n2adj2.<+ - Чужбинський /adj.<+ g=m"]
//		assert expandComps.process_input(input) == fullComps.split(/\n/).collect { DicEntry.fromLine(it) }
		assertEquals(fullComps, ExpandTest.join(expandComps.process_input(input)))
	}
	
	def fullComps2 =
'''
такий-сякий такий-сякий adj:m:v_naz
такий-сякий такий-сякий adj:m:v_zna:rinanim
такий-сякий такий-сякий adj:m:v_kly
такого-сякого такий-сякий adj:m:v_rod
такого-сякого такий-сякий adj:m:v_zna:ranim
такого-сякого такий-сякий adj:n:v_rod
такому-сякому такий-сякий adj:m:v_dav
такому-сякому такий-сякий adj:m:v_mis
такому-сякому такий-сякий adj:n:v_dav
такому-сякому такий-сякий adj:n:v_mis
таким-сяким такий-сякий adj:m:v_oru
таким-сяким такий-сякий adj:n:v_oru
таким-сяким такий-сякий adj:p:v_dav
такім-сякім такий-сякий adj:m:v_mis
такім-сякім такий-сякий adj:n:v_mis
така-сяка такий-сякий adj:f:v_naz
така-сяка такий-сякий adj:f:v_kly
такої-сякої такий-сякий adj:f:v_rod
такій-сякій такий-сякий adj:f:v_dav
такій-сякій такий-сякий adj:f:v_mis
таку-сяку такий-сякий adj:f:v_zna
такою-сякою такий-сякий adj:f:v_oru
таке-сяке такий-сякий adj:n:v_naz
таке-сяке такий-сякий adj:n:v_zna
таке-сяке такий-сякий adj:n:v_kly
такі-сякі такий-сякий adj:p:v_naz
такі-сякі такий-сякий adj:p:v_zna:rinanim
такі-сякі такий-сякий adj:p:v_kly
таких-сяких такий-сякий adj:p:v_rod
таких-сяких такий-сякий adj:p:v_zna:ranim
таких-сяких такий-сякий adj:p:v_mis
такими-сякими такий-сякий adj:p:v_oru
'''.trim()
		
	@Test
	void testAdjPron() {
		def input = ["такий /adj - сякий /adj"]
		assertEquals(fullComps2, ExpandTest.join(expandComps.process_input(input)))
	}

def fullComps25 = 
"""
один-єдиний один-єдиний adj:m:v_naz:&numr
один-єдиний один-єдиний adj:m:v_zna:rinanim:&numr
одного-єдиного один-єдиний adj:m:v_rod:&numr
одного-єдиного один-єдиний adj:m:v_zna:ranim:&numr
одного-єдиного один-єдиний adj:n:v_rod:&numr
одному-єдиному один-єдиний adj:m:v_dav:&numr
одному-єдиному один-єдиний adj:m:v_mis:&numr
однім-єдиному один-єдиний adj:m:v_mis:&numr
одному-єдиному один-єдиний adj:n:v_dav:&numr
одному-єдиному один-єдиний adj:n:v_mis:&numr
однім-єдиному один-єдиний adj:n:v_mis:&numr
одним-єдиним один-єдиний adj:m:v_oru:&numr
одним-єдиним один-єдиний adj:n:v_oru:&numr
одним-єдиним один-єдиний adj:p:v_dav:&numr
одному-єдинім один-єдиний adj:m:v_mis:&numr
однім-єдинім один-єдиний adj:m:v_mis:&numr
одному-єдинім один-єдиний adj:n:v_mis:&numr
однім-єдинім один-єдиний adj:n:v_mis:&numr
одна-єдина один-єдиний adj:f:v_naz:&numr
одної-єдиної один-єдиний adj:f:v_rod:&numr
однієї-єдиної один-єдиний adj:f:v_rod:&numr
одній-єдиній один-єдиний adj:f:v_dav:&numr
одній-єдиній один-єдиний adj:f:v_mis:&numr
одну-єдину один-єдиний adj:f:v_zna:&numr
однією-єдиною один-єдиний adj:f:v_oru:&numr
одною-єдиною один-єдиний adj:f:v_oru:&numr
одне-єдине один-єдиний adj:n:v_naz:&numr
одно-єдине один-єдиний adj:n:v_naz:&numr
одне-єдине один-єдиний adj:n:v_zna:&numr
одно-єдине один-єдиний adj:n:v_zna:&numr
одні-єдині один-єдиний adj:p:v_naz:&numr
одні-єдині один-єдиний adj:p:v_zna:rinanim:&numr
одних-єдиних один-єдиний adj:p:v_rod:&numr
одних-єдиних один-єдиний adj:p:v_zna:ranim:&numr
одних-єдиних один-єдиний adj:p:v_mis:&numr
одними-єдиними один-єдиний adj:p:v_oru:&numr
""".trim()

    @Test
    void testNumr() {
        def input = ["один /numr ^adj - єдиний /adj :&numr"]
        assertEquals(fullComps25, ExpandTest.join(expandComps.process_input(input)))
    }

    def fullComps3 =
'''
дівка-дзиґа дівка-дзиґа noun:anim:f:v_naz
дівки-дзиґи дівка-дзиґа noun:anim:f:v_rod
дівкою-дзиґою дівка-дзиґа noun:anim:f:v_oru
дівку-дзиґу дівка-дзиґа noun:anim:f:v_zna
дівці-дзизі дівка-дзиґа noun:anim:f:v_dav
дівці-дзизі дівка-дзиґа noun:anim:f:v_mis
дівці-дзидзі дівка-дзиґа noun:anim:f:v_dav
дівці-дзидзі дівка-дзиґа noun:anim:f:v_mis
дівки-дзиґи дівка-дзиґа noun:anim:p:v_naz
дівки-дзиґи дівка-дзиґа noun:anim:p:v_kly
дівок-дзиґ дівка-дзиґа noun:anim:p:v_rod
дівок-дзиґ дівка-дзиґа noun:anim:p:v_zna
дівкам-дзиґам дівка-дзиґа noun:anim:p:v_dav
дівками-дзиґами дівка-дзиґа noun:anim:p:v_oru
дівках-дзиґах дівка-дзиґа noun:anim:p:v_mis
дівко-дзиґо дівка-дзиґа noun:anim:f:v_kly
дівки-дзиґи дівка-дзиґа noun:anim:p:v_zna:rare
'''.trim()
    
    @Test
    void testNounAnimInanim() {
        def input = ["дівка /n10.p2.< - дзиґа /n10.p1.<"]
        assertEquals(fullComps3, ExpandTest.join(expandComps.process_input(input)))
    }
    
    def fullComps4 =
'''
дід-баба дід-баба noun:anim:p:v_naz
діда-баби дід-баба noun:anim:p:v_rod
дідом-бабою дід-баба noun:anim:p:v_oru
діда-бабу дід-баба noun:anim:p:v_zna
дідові-бабі дід-баба noun:anim:p:v_dav
діду-бабі дід-баба noun:anim:p:v_dav
дідові-бабі дід-баба noun:anim:p:v_mis
діді-бабі дід-баба noun:anim:p:v_mis
діду-бабі дід-баба noun:anim:p:v_mis
діду-бабо дід-баба noun:anim:p:v_kly
'''.trim()
    
    @Test
    void testNounAnimAnim() {
        def input = ["дід /n20.a.ku.< - баба /n10.< ^noun:p"]
        assertEquals(fullComps4, ExpandTest.join(expandComps.process_input(input)))
    }


def fullComps5 = 
"""
Буда-Голубієвичі Буда-Голубієвичі noun:inanim:f:v_naz:prop
Буду-Голубієвичі Буда-Голубієвичі noun:inanim:f:v_zna:prop
Будо-Голубієвичі Буда-Голубієвичі noun:inanim:f:v_kly:prop
Буди-Голубієвичів Буда-Голубієвичі noun:inanim:f:v_rod:prop
Буді-Голубієвичам Буда-Голубієвичі noun:inanim:f:v_dav:prop
Будою-Голубієвичами Буда-Голубієвичі noun:inanim:f:v_oru:prop
Буді-Голубієвичах Буда-Голубієвичі noun:inanim:f:v_mis:prop
""".trim()

    @Test
    void testGeoComp() {
        def input = ["Буда /n10 - Голубієвичі /np2"]
        assertEquals(fullComps5, ExpandTest.join(expandComps.process_input(input)))
    }

    def fullComps6 =
"""
вежі-близнюки вежі-близнюки noun:inanim:p:v_naz:ns
вежі-близнюки вежі-близнюки noun:inanim:p:v_zna:ns
вежі-близнюки вежі-близнюки noun:inanim:p:v_kly:ns
веж-близнюків вежі-близнюки noun:inanim:p:v_rod:ns
вежам-близнюкам вежі-близнюки noun:inanim:p:v_dav:ns
вежами-близнюками вежі-близнюки noun:inanim:p:v_oru:ns
вежах-близнюках вежі-близнюки noun:inanim:p:v_mis:ns
""".trim()
    
    @Test
    void testPlural() {
        def input = ["вежі /np1 - близнюки /np2"]
        assertEquals(fullComps6, ExpandTest.join(expandComps.process_input(input)))
    }
    
def fullComps7 =
"""
Бульба-Боровець Бульба-Боровець noun:anim:m:v_naz:prop:lname
Бульбі-Боровцю Бульба-Боровець noun:anim:m:v_dav:prop:lname
Бульбі-Боровцю Бульба-Боровець noun:anim:m:v_mis:prop:lname
Бульбо-Боровцю Бульба-Боровець noun:anim:m:v_kly:prop:lname
Бульбі-Боровцеві Бульба-Боровець noun:anim:m:v_dav:prop:lname
Бульбі-Боровцеві Бульба-Боровець noun:anim:m:v_mis:prop:lname
Бульбою-Боровцем Бульба-Боровець noun:anim:m:v_oru:prop:lname
Бульбі-Боровці Бульба-Боровець noun:anim:m:v_mis:prop:lname
Бульби-Боровця Бульба-Боровець noun:anim:m:v_rod:prop:lname
Бульбу-Боровця Бульба-Боровець noun:anim:m:v_zna:prop:lname
""".trim()

    @Test
    void testLname() {
        def input = ["Бульба /n10.ko.<+m - Боровець /n22.a.<+m"]
        assertEquals(fullComps7, ExpandTest.join(expandComps.process_input(input)))
    }

    def fullComps8 =
"""
ясла-садок ясла-садок noun:inanim:p:v_naz:ns
ясла-садок ясла-садок noun:inanim:p:v_zna:ns
яслам-садку ясла-садок noun:inanim:p:v_dav:ns
яслах-садку ясла-садок noun:inanim:p:v_mis:ns
ясла-садку ясла-садок noun:inanim:p:v_kly:ns
яслами-садком ясла-садок noun:inanim:p:v_oru:ns
яслам-садкові ясла-садок noun:inanim:p:v_dav:ns
яслах-садкові ясла-садок noun:inanim:p:v_mis:ns
ясел-садка ясла-садок noun:inanim:p:v_rod:ns
""".trim()
    
    @Test
    void testPluralMix() {
        def input = ["ясла /np3 - садок /n22.a"]
        assertEquals(fullComps8, ExpandTest.join(expandComps.process_input(input)))
    }
}

package org.dict_uk.expand


import static org.junit.jupiter.api.Assertions.assertEquals

import org.dict_uk.common.DicEntry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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

}

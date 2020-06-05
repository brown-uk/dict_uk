package org.dict_uk.expand


import static org.junit.jupiter.api.Assertions.assertEquals

import org.dict_uk.common.DicEntry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

public class ExpandCompsTest {
	
	ExpandComps expandComps
	
	@BeforeEach
	void setUp() {
		def affixDir = new File("data/affix").isDirectory() ? "data/affix" : "../../../data/affix"
		Args.parse(["--aff", affixDir, "--dict", ""].toArray(new String[0]))
		
		def expand = new Expand(false)
		expand.affix.load_affixes(affixDir)
		expandComps = new ExpandComps(expand)
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
	void test() {
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
		void test2() {
			def input = ["такий /adj - сякий /adj"]
			assertEquals(fullComps2, ExpandTest.join(expandComps.process_input(input)))
		}
	
}
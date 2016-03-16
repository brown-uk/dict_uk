package org.dict_uk.expand

import static org.junit.Assert.*;

import org.junit.Test;

public class ExpandCompsTest extends GroovyTestCase {
	
	ExpandComps expandComps
	
	@Override
	void setUp() {
		def affixDir = new File("data/affix").isDirectory() ? "data/affix" : "../../../data/affix"
		Args.parse(["-aff", affixDir, "-dict", ""].toArray(new String[0]))
		
		def expand = new Expand()
		expand.affix.load_affixes(affixDir)
		expandComps = new ExpandComps(expand)
	}
	
	
	def fullComps =
'''
Афанасьєв-Чужбинський Афанасьєв-Чужбинський noun:anim:m:v_naz:lname
Афанасьєва-Чужбинського Афанасьєв-Чужбинський noun:anim:m:v_rod:lname
Афанасьєва-Чужбинського Афанасьєв-Чужбинський noun:anim:m:v_zna:lname
Афанасьєву-Чужбинському Афанасьєв-Чужбинський noun:anim:m:v_dav:lname
Афанасьєву-Чужбинському Афанасьєв-Чужбинський noun:anim:m:v_mis:lname
Афанасьєві-Чужбинському Афанасьєв-Чужбинський noun:anim:m:v_mis:lname
Афанасьєвим-Чужбинським Афанасьєв-Чужбинський noun:anim:m:v_oru:lname
Афанасьєву-Чужбинськім Афанасьєв-Чужбинський noun:anim:m:v_mis:lname
Афанасьєві-Чужбинськім Афанасьєв-Чужбинський noun:anim:m:v_mis:lname
'''.trim()
	
	@Test
	void test() {
		def input = ["Афанасьєв /n2adj2.<+ - Чужбинський /adj.<+ g=m"]
		assert expandComps.process_input(input).join("\n") == fullComps
	}
	
	def fullComps2 =
'''
такий-cякий такий-cякий adj:m:v_naz
такий-cякий такий-cякий adj:m:v_zna:rinanim
такий-cякий такий-cякий adj:m:v_kly
такого-cякого такий-cякий adj:m:v_rod
такого-cякого такий-cякий adj:m:v_zna:ranim
такого-cякого такий-cякий adj:n:v_rod
такому-cякому такий-cякий adj:m:v_dav
такому-cякому такий-cякий adj:m:v_mis
такім-cякому такий-cякий adj:m:v_mis
такому-cякому такий-cякий adj:n:v_dav
такому-cякому такий-cякий adj:n:v_mis
такім-cякому такий-cякий adj:n:v_mis
таким-cяким такий-cякий adj:m:v_oru
таким-cяким такий-cякий adj:n:v_oru
таким-cяким такий-cякий adj:p:v_dav
такому-cякім такий-cякий adj:m:v_mis
такім-cякім такий-cякий adj:m:v_mis
такому-cякім такий-cякий adj:n:v_mis
такім-cякім такий-cякий adj:n:v_mis
така-cяка такий-cякий adj:f:v_naz
такая-cяка такий-cякий adj:f:v_naz
така-cяка такий-cякий adj:f:v_kly
такая-cяка такий-cякий adj:f:v_kly
така-cякая такий-cякий adj:f:v_naz
такая-cякая такий-cякий adj:f:v_naz
така-cякая такий-cякий adj:f:v_kly
такая-cякая такий-cякий adj:f:v_kly
такої-cякої такий-cякий adj:f:v_rod
такій-cякій такий-cякий adj:f:v_dav
такій-cякій такий-cякий adj:f:v_mis
таку-cяку такий-cякий adj:f:v_zna
такую-cяку такий-cякий adj:f:v_zna
таку-cякую такий-cякий adj:f:v_zna
такую-cякую такий-cякий adj:f:v_zna
такою-cякою такий-cякий adj:f:v_oru
таке-cяке такий-cякий adj:n:v_naz
такеє-cяке такий-cякий adj:n:v_naz
таке-cяке такий-cякий adj:n:v_zna
такеє-cяке такий-cякий adj:n:v_zna
таке-cяке такий-cякий adj:n:v_kly
такеє-cяке такий-cякий adj:n:v_kly
таке-cякеє такий-cякий adj:n:v_naz
такеє-cякеє такий-cякий adj:n:v_naz
таке-cякеє такий-cякий adj:n:v_zna
такеє-cякеє такий-cякий adj:n:v_zna
таке-cякеє такий-cякий adj:n:v_kly
такеє-cякеє такий-cякий adj:n:v_kly
такі-cякі такий-cякий adj:p:v_naz
такії-cякі такий-cякий adj:p:v_naz
такі-cякі такий-cякий adj:p:v_zna:rinanim
такії-cякі такий-cякий adj:p:v_zna:rinanim
такі-cякі такий-cякий adj:p:v_kly
такії-cякі такий-cякий adj:p:v_kly
такі-cякії такий-cякий adj:p:v_naz
такії-cякії такий-cякий adj:p:v_naz
такі-cякії такий-cякий adj:p:v_zna:rinanim
такії-cякії такий-cякий adj:p:v_zna:rinanim
такі-cякії такий-cякий adj:p:v_kly
такії-cякії такий-cякий adj:p:v_kly
таких-cяких такий-cякий adj:p:v_rod
таких-cяких такий-cякий adj:p:v_zna:ranim
таких-cяких такий-cякий adj:p:v_mis
такими-cякими такий-cякий adj:p:v_oru
'''.trim()
		
		@Test
		void test2() {
			def input = ["такий /adj - cякий /adj"]
			println expandComps.process_input(input).join("\n")
			assert expandComps.process_input(input).join("\n") == fullComps2
		}
	
}
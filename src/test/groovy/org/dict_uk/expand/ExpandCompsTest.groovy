package org.dict_uk.expand

import static org.junit.Assert.*;

import org.junit.Test;

public class ExpandCompsTest extends GroovyTestCase {
	
	ExpandComps expandComps
	
//	@Setup
	@Override
	void setUp() {
		def expand = new Expand()
		def affixDir = new File("data/affix").isDirectory() ? "data/affix" : "../../../data/affix"
		expand.affix.load_affixes(affixDir)
		expandComps = new ExpandComps(expand)
	} 
	
	
	def fullComps =
'''
Афанасьєв-Чужбинський Афанасьєв-Чужбинський noun:m:v_naz:anim:lname
Афанасьєва-Чужбинського Афанасьєв-Чужбинський noun:m:v_rod:anim:lname
Афанасьєва-Чужбинського Афанасьєв-Чужбинський noun:m:v_zna:anim:lname
Афанасьєву-Чужбинському Афанасьєв-Чужбинський noun:m:v_dav:anim:lname
Афанасьєву-Чужбинському Афанасьєв-Чужбинський noun:m:v_mis:anim:lname
Афанасьєві-Чужбинському Афанасьєв-Чужбинський noun:m:v_mis:anim:lname
Афанасьєвим-Чужбинським Афанасьєв-Чужбинський noun:m:v_oru:anim:lname
Афанасьєву-Чужбинськім Афанасьєв-Чужбинський noun:m:v_mis:anim:lname
Афанасьєві-Чужбинськім Афанасьєв-Чужбинський noun:m:v_mis:anim:lname
'''.trim()
	
	@Test
	void test() {
		def input = ["Афанасьєв /n2adj2.<+ - Чужбинський /adj.<+ g=m"]
		assert expandComps.process_input(input).join("\n") == fullComps
	}
}
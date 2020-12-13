package org.dict_uk.expand;

import static org.junit.jupiter.api.Assertions.*;

import org.dict_uk.common.DicEntry
import org.junit.jupiter.api.Test;

class UtilTest {

	def nvOut = '''
грінго грінго noun:anim:m:v_naz:nv
грінго грінго noun:anim:m:v_rod:nv
грінго грінго noun:anim:m:v_dav:nv
грінго грінго noun:anim:m:v_zna:nv
грінго грінго noun:anim:m:v_oru:nv
грінго грінго noun:anim:m:v_mis:nv
грінго грінго noun:anim:m:v_kly:nv
грінго грінго noun:anim:p:v_naz:nv
грінго грінго noun:anim:p:v_rod:nv
грінго грінго noun:anim:p:v_dav:nv
грінго грінго noun:anim:p:v_zna:nv
грінго грінго noun:anim:p:v_oru:nv
грінго грінго noun:anim:p:v_mis:nv
грінго грінго noun:anim:p:v_kly:nv
'''.trim()
	
	@Test
	void test() {
		def nvEntry = new DicEntry("грінго", "грінго", "noun:anim:m:nv")
		def out = new Util().expand_nv(Arrays.asList(nvEntry))
		assertEquals(nvOut, out.collect { e-> e.toFlatString() }.join("\n"))
	}

}

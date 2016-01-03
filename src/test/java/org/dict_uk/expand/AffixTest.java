package org.dict_uk.expand;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class AffixTest {

	List<String> expanded = Arrays.asList(
"себе себе noun:m:v_rod:&pron:refl",
"себе себе noun:m:v_zna:&pron:refl",
"себе себе noun:n:v_rod:&pron:refl",
"себе себе noun:n:v_zna:&pron:refl");
	
	@Test
	public void testExpandAlts() {
		Affix affix = new Affix();
		
		List<String> lines = Arrays.asList("себе себе noun:m:v_rod/v_zna//n:v_rod/v_zna:&pron:refl");
		List<String> out1 = affix.expand_alts(lines, "//");
//		assertEquals(expanded, );
		List<String> out2 = affix.expand_alts(out1, "/");
		assertEquals(expanded, out2);
	}

}

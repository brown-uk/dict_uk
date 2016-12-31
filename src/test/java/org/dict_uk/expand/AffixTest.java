package org.dict_uk.expand;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dict_uk.common.AffixUtil;
import org.dict_uk.common.DicEntry;
import org.junit.Test;

public class AffixTest {

	List<String> expanded_ = Arrays.asList(
"себе себе noun:m:v_rod:&pron:refl",
"себе себе noun:m:v_zna:&pron:refl",
"себе себе noun:n:v_rod:&pron:refl",
"себе себе noun:n:v_zna:&pron:refl");
	
	@Test
	public void testExpandAlts() {
//		Affix affix = new Affix();

		List<DicEntry> expanded = new ArrayList<>();
		for (String string : expanded_) {
			expanded.add(DicEntry.fromLine(string));
		}

		List<DicEntry> lines = Arrays.asList(DicEntry.fromLine("себе себе noun:m:v_rod/v_zna//n:v_rod/v_zna:&pron:refl"));
		List<DicEntry> out1 = AffixUtil.expand_alts(lines, "//");
//		assertEquals(expanded, );
		List<DicEntry> out2 = AffixUtil.expand_alts(out1, "/");
		assertEquals(expanded, out2);
	}

}

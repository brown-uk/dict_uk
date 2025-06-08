package org.dict_uk.expand;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.dict_uk.common.AffixUtil;
import org.dict_uk.common.DicEntry;
import org.junit.jupiter.api.Test;

public class AffixTest {

	List<String> expanded_ = Arrays.asList(
"себе себе noun:m:v_rod:pron:refl",
"себе себе noun:m:v_zna:pron:refl",
"себе себе noun:n:v_rod:pron:refl",
"себе себе noun:n:v_zna:pron:refl");
	
	List<String> expanded1 = Arrays.asList( 
"себе себе noun:m:v_rod/v_zna:pron:refl",
"себе себе noun:n:v_rod/v_zna:pron:refl");
	
	@Test
	public void testExpandAlts() {
		List<DicEntry> lines = Arrays.asList(DicEntry.fromLine("себе себе noun:m:v_rod/v_zna//n:v_rod/v_zna:pron:refl"));

		List<DicEntry> out1 = AffixUtil.expand_alts(lines, "//");
		assertEquals(expanded1, join(out1));

		List<DicEntry> out2 = AffixUtil.expand_alts(out1, "/");
		assertEquals(expanded_, join(out2));
	}

	static final List<String> join(List<DicEntry> entries) {
		return entries.stream().map(it -> it.toFlatString()).collect(Collectors.toList()); //joining("\n"));
	}

}

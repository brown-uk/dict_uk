package org.dict_uk.expand

import static org.junit.Assert.*;
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

public class BaseTagsTest {
	
	BaseTags baseTags
	
	@BeforeEach
	void setUp() {
		baseTags = new BaseTags()
	} 
	
	@Test
	void test() {
		assert baseTags.get_base_tags("готувати", "v1", "v1", "") == ":inf"
		assert baseTags.get_base_tags("цирк", "n20", "n20.p", "") == ":m:v_naz/v_zna"
		assert baseTags.get_base_tags("баба", "n10", "n10.p", "") == ":f:v_naz"
	}
}
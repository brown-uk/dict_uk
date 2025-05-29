package org.dict_uk.expand

import static org.junit.jupiter.api.Assertions.assertEquals

import org.dict_uk.tools.Autogen
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

public class AutogenTest {
	
//	Autogen autogen
	
	@BeforeEach
	void setUp() {
//		autogen = new Autogen()
	} 
	
	@Test
	void test() {
		String[] words = ["екс-піжон /n20 :up92", "екс-єпископ /n20 :up92", "камер-юнкер /n20 :up92"]
		String[] expected = [
			["експіжон /n20 :up19", "екс'єпископ /n20 :up19", "камер'юнкер /n20 :up19"], 
			[
				Autogen.addReplace("екс-піжон /n20 :up92", "експіжон"), 
				Autogen.addReplace("екс-єпископ /n20 :up92", "екс'єпископ"), 
				Autogen.addReplace("камер-юнкер /n20 :up92", "камер'юнкер")]
			]
		def res = Autogen.enforceNoDash2019(words)
		
		assertEquals(expected[0].toString(), res[0].toString())
		assertEquals(expected[1].toString(), res[1].toString())
		
		words = ["хімія /n10", "гліфосат /n20 		  # хімічна"]
		
		res = Autogen.replaceLetters(words)
		expected = [
			["хемія /n10 :alt"],
			[]
			]
		assertEquals(expected[0].toString(), res[0].toString())
		assertEquals(expected[1].toString(), res[1].toString())

		words = ["хімічити /v1 :imperf:coll"]
		
		res = Autogen.replaceLetters(words)
		expected = [
			["хемічити /v1 :imperf:coll:alt"],
			[]
			]
		assertEquals(expected[0].toString(), res[0].toString())
		assertEquals(expected[1].toString(), res[1].toString())

	}
}
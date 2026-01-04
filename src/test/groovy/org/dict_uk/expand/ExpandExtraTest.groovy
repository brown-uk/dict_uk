
package org.dict_uk.expand

import static org.assertj.core.api.Assertions.assertThat

import org.dict_uk.common.DicEntry
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import groovy.transform.CompileStatic

@CompileStatic
public class ExpandExtraTest {
	
	static Expand expand
	
	@BeforeAll
	static void classSetUp() {
		String[] args = ["--aff", ".", "--dict", "data/dict"].toArray(new String[0])
		Args.parse(args)
		
		expand = new Expand(true)
		def affixDir = new File("data/affix").isDirectory() ? "data/affix" : "../../../data/affix"
		expand.affix.load_affixes(affixDir)
	}

	
    @Test
    void testAddTag() {
        List<String> lines = [
            "капець /n22.a.p       # ка ́пець", 
            "капець /n22.a :slang  # капе ́ць", 
            "сер /n20.a.<"
        ]

        def expanded = join(expand.process_input(lines))
        
        assertThat(expanded).contains("капець капець noun:inanim:m:v_naz")

        assertThat(expanded).contains("капець капець noun:inanim:m:v_naz:predic:slang")

        assertThat(expanded).contains("сере сер noun:anim:m:v_kly:up92")
    }
    

	static final String join(List<DicEntry> entries) {
		return entries.collect{ it.toFlatString() }.join("\n") //.replaceAll(/[<>]/, '')
	}
    	
}


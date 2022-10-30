package org.dict_uk.expand

import static org.junit.jupiter.api.Assertions.assertEquals

import org.dict_uk.common.DicEntry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

public class DictSorterTest {
	
	DictSorter dictSorter
	
	@BeforeEach
	void setUp() {
		dictSorter = new DictSorter()
	} 
	
	@Test
	void testTagSortKey() {
		assertEquals "noun:inanim:0030", dictSorter.tag_sort_key("noun:inanim:m:v_dav", "порт")
		assertEquals "noun:inanim:0031", dictSorter.tag_sort_key("noun:inanim:m:v_dav:rare", "порт")
		assertEquals "noun:anim:lname:0010", dictSorter.tag_sort_key("noun:anim:m:v_naz:lname", "Адамишин")
		assertEquals "noun:anim:lname:9010", dictSorter.tag_sort_key("noun:anim:f:v_naz:lname", "Адамишин")
		assertEquals "numr:1010", dictSorter.tag_sort_key("numr:f:v_naz", "один")
		assertEquals "adj:compb:0040", dictSorter.tag_sort_key("adj:m:v_zna:ranim", "азотистоводневого")
		assertEquals "adj:compb:0041", dictSorter.tag_sort_key("adj:m:v_zna:rinanim", "азотистоводневий")
		assertEquals "verb:imperf:10:0", dictSorter.tag_sort_key("verb:imperf:inf", "порт")
		assertEquals "verb:imperf:10:2", dictSorter.tag_sort_key("verb:imperf:inf:coll:rare", "порт")
		assertEquals "verb:xp1:perf:80:00:0", dictSorter.tag_sort_key("verb:perf:past:m:xp1", "вернув")
		assertEquals "verb:xp1:perf:10:0", dictSorter.tag_sort_key("verb:perf:inf:xp1", "вернути")
        assertEquals "noun:inanim:nv:&numr:1010:abbr", dictSorter.tag_sort_key("noun:inanim:f:v_naz:nv:&numr:abbr", "тис.")
        assertEquals "noun:inanim:nv:&numr:5010:abbr", dictSorter.tag_sort_key("noun:inanim:p:v_naz:nv:&numr:abbr", "тис.")
        assertEquals "noun:inanim:nv:3010:abbr", dictSorter.tag_sort_key("noun:inanim:n:v_naz:nv:abbr", "тис.")
        assertEquals "noun:inanim:nv:5010:abbr", dictSorter.tag_sort_key("noun:inanim:p:v_naz:nv:abbr", "тис.")
	}
    
    @Test
    void testGetLineKey() {
        assertEquals "тис. noun:inanim:nv:&numr", dictSorter.getLineKey("тис.", "noun:inanim:f:v_naz:nv:&numr:abbr", new DicEntry("", "", ""))
        assertEquals "тис. noun:inanim:nv", dictSorter.getLineKey("тис.", "noun:inanim:n:v_naz:nv:abbr", new DicEntry("", "", ""))
    }

	def linesToSort =
'''
А-Ба-Ба-Га-Ла-Ма-Га А-Ба-Ба-Га-Ла-Ма-Га noun:n:v_naz:nv:np
А-Ба-Ба-Га-Ла-Ма-Га А-Ба-Ба-Га-Ла-Ма-Га noun:n:v_rod:nv:np
А-Ба-Ба-Га-Ла-Ма-Га А-Ба-Ба-Га-Ла-Ма-Га noun:n:v_dav:nv:np
А-Ба-Ба-Га-Ла-Ма-Га А-Ба-Ба-Га-Ла-Ма-Га noun:n:v_zna:nv:np
А-Ба-Ба-Га-Ла-Ма-Га А-Ба-Ба-Га-Ла-Ма-Га noun:n:v_oru:nv:np
А-Ба-Ба-Га-Ла-Ма-Га А-Ба-Ба-Га-Ла-Ма-Га noun:n:v_mis:nv:np
а-ба-ба-га-ла-ма-га а-ба-ба-га-ла-ма-га noun:n:v_naz:nv:np
а-ба-ба-га-ла-ма-га а-ба-ба-га-ла-ма-га noun:n:v_rod:nv:np
а-ба-ба-га-ла-ма-га а-ба-ба-га-ла-ма-га noun:n:v_dav:nv:np
а-ба-ба-га-ла-ма-га а-ба-ба-га-ла-ма-га noun:n:v_zna:nv:np
а-ба-ба-га-ла-ма-га а-ба-ба-га-ла-ма-га noun:n:v_oru:nv:np
а-ба-ба-га-ла-ма-га а-ба-ба-га-ла-ма-га noun:n:v_mis:nv:np
Абалкін Абалкін noun:m:v_naz:anim:lname
Абалкіна Абалкін noun:m:v_rod:anim:lname
Абалкіну Абалкін noun:m:v_dav:anim:lname
Абалкіна Абалкін noun:m:v_zna:anim:lname
Абалкіним Абалкін noun:m:v_oru:anim:lname
Абалкіні Абалкін noun:m:v_mis:anim:lname
Абалкіну Абалкін noun:m:v_mis:anim:lname
азотистоводневий азотистоводневий adj:m:v_naz
азотистоводневого азотистоводневий adj:m:v_rod
азотисто-водневий азотисто-водневий adj:m:v_naz
азотисто-водневого азотисто-водневий adj:m:v_rod
тис. тис. noun:inanim:f:v_naz:nv:&numr:abbr
тис. тис. noun:inanim:f:v_rod:nv:&numr:abbr
тис. тис. noun:inanim:p:v_naz:nv:&numr:abbr
тис. тис. noun:inanim:n:v_naz:nv:abbr
тис. тис. noun:inanim:n:v_rod:nv:abbr
тис. тис. noun:inanim:p:v_naz:nv:abbr
'''.trim().split("\n")
	
	
	@Test
	void testLineSortKey() {
		def prevKey = ""
		linesToSort.each{ line ->
			def key = dictSorter.line_key(DicEntry.fromLine(line))
			assert key.compareTo(prevKey) > 0
			prevKey = key 
		}
	}
	
	@Test
	void testAllLines() {
		def entries = DicEntry.fromLines(["а а part", "а а intj"])
		assertEquals(["а а intj", "а а part"].join('\n'), ExpandTest.join(dictSorter.sortEntries(entries)))
	}
}

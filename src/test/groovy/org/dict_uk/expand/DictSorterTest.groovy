package org.dict_uk.expand

import static org.junit.Assert.*;

import org.dict_uk.common.DicEntry
import org.junit.Test;

public class DictSorterTest extends GroovyTestCase {
	
	DictSorter dictSorter
	
//	@Setup
	@Override
	void setUp() {
		dictSorter = new DictSorter()
	} 
	
	@Test
	void testTagSortKey() {
		assert dictSorter.tag_sort_key("noun:m:v_dav", "порт") == "noun:0:30"
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
		assert ["а а intj", "а а part"].join('\n') == ExpandTest.join(dictSorter.sortEntries(entries))
	}
}

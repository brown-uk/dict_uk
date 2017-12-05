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
		assert dictSorter.tag_sort_key("noun:inanim:m:v_dav", "порт") == "noun:inanim:00:30"
		assert dictSorter.tag_sort_key("noun:inanim:m:v_dav:rare", "порт") == "noun:inanim:00:31"
		assert dictSorter.tag_sort_key("noun:anim:m:v_naz:lname", "Адамишин") == "noun:anim:lname:00:10"
		assert dictSorter.tag_sort_key("noun:anim:f:v_naz:lname", "Адамишин") == "noun:anim:lname:90:10"
		assert dictSorter.tag_sort_key("numr:f:v_naz", "один") == "numr:10:10"
		assert dictSorter.tag_sort_key("adj:m:v_zna:ranim", "азотистоводневого") == "adj:compb:00:40"
		assert dictSorter.tag_sort_key("adj:m:v_zna:rinanim", "азотистоводневий") == "adj:compb:00:41"
		assert dictSorter.tag_sort_key("verb:imperf:inf", "порт") == "verb:imperf:10:0"
		assert dictSorter.tag_sort_key("verb:imperf:inf:coll:rare", "порт") == "verb:imperf:10:2"
		assert dictSorter.tag_sort_key("verb:perf:past:m:xp1", "вернув") == "verb:xp1:perf:80:00:0"
		assert dictSorter.tag_sort_key("verb:perf:inf:xp1", "вернути") == "verb:xp1:perf:10:0"
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

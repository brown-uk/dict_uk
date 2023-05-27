
package org.dict_uk.expand

import static org.junit.jupiter.api.Assertions.assertEquals

import org.dict_uk.common.DicEntry
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import groovy.transform.CompileStatic

@CompileStatic
public class ExpandTest {
	
	static Expand expand
	
	@BeforeAll
	static void classSetUp() {
		String[] args = ["--aff", ".", "--dict", "."].toArray(new String[0])
		Args.parse(args)
		
		expand = new Expand(false)
		def affixDir = new File("data/affix").isDirectory() ? "data/affix" : "../../../data/affix"
		expand.affix.load_affixes(affixDir)
	}
	
	@BeforeEach
	void setUp() {
//		expand = new Expand()
//		expand.affix.load_affixes("../data/affix")
//		println(expand.affix.affixMap.keySet())
	} 
	
	@Test
	void testAdjustCommonFlag() {
		assert expand.adjustCommonFlag("v2.cf") == "v.cf"
	}

	
	def port = 
'''
порт порт noun:m:v_naz/v_zna
портові порт noun:m:v_dav/v_mis
портом порт noun:m:v_oru
порті порт noun:m:v_mis
порту порт noun:m:v_dav/v_mis/v_kly
портів порт noun:p:v_rod
портам порт noun:p:v_dav
портами порт noun:p:v_oru
портах порт noun:p:v_mis
порти порт noun:p:v_naz
'''.trim() //.split("\n")
	
	@Test
	void testExpandAffix() {
		assert join(expand.expand_suffixes("порт", "n20.p", [:], ":rare")) == port
	}

	def portFull =
'''
порт порт noun:inanim:m:v_naz:rare
порту порт noun:inanim:m:v_rod:rare
портові порт noun:inanim:m:v_dav:rare
порту порт noun:inanim:m:v_dav:rare
порт порт noun:inanim:m:v_zna:rare
портом порт noun:inanim:m:v_oru:rare
порті порт noun:inanim:m:v_mis:rare
портові порт noun:inanim:m:v_mis:rare
порту порт noun:inanim:m:v_mis:rare
порте порт noun:inanim:m:v_kly:rare
порти порт noun:inanim:p:v_naz:rare
портів порт noun:inanim:p:v_rod:rare
портам порт noun:inanim:p:v_dav:rare
порти порт noun:inanim:p:v_zna:rare
портами порт noun:inanim:p:v_oru:rare
портах порт noun:inanim:p:v_mis:rare
порти порт noun:inanim:p:v_kly:rare
'''.trim() //.split("\n")
	
	@Test
	void testProcessInput() {
		def lines = ["порт /n20.p :rare"]
		assert join(expand.process_input(lines)) == portFull
	}

	def zhabaFull =
	'''
жаба жаба noun:anim:m:v_naz
жаби жаба noun:anim:m:v_rod
жабі жаба noun:anim:m:v_dav
жабу жаба noun:anim:m:v_zna
жабою жаба noun:anim:m:v_oru
жабі жаба noun:anim:m:v_mis
жабо жаба noun:anim:m:v_kly
жаби жаба noun:anim:p:v_naz
жаб жаба noun:anim:p:v_rod
жабам жаба noun:anim:p:v_dav
жаб жаба noun:anim:p:v_zna
жаби жаба noun:anim:p:v_zna
жабами жаба noun:anim:p:v_oru
жабах жаба noun:anim:p:v_mis
жаби жаба noun:anim:p:v_kly
'''.trim() //.split("\n")

    @Test
    void testZhaba() {
        def lines = ["жаба /n10.p1.<> ^noun:m"]
        assert expand.process_input(lines).join("\n").replaceAll(/[<>]/, '') == zhabaFull
    }

    def golovaFull =
    '''
голова голова noun:anim:m:v_naz # посада
голови голова noun:anim:m:v_rod
голові голова noun:anim:m:v_dav
голову голова noun:anim:m:v_zna
головою голова noun:anim:m:v_oru
голові голова noun:anim:m:v_mis
голово голова noun:anim:m:v_kly
голова голова noun:anim:f:v_naz
голови голова noun:anim:f:v_rod
голові голова noun:anim:f:v_dav
голову голова noun:anim:f:v_zna
головою голова noun:anim:f:v_oru
голові голова noun:anim:f:v_mis
голово голова noun:anim:f:v_kly
голови голова noun:anim:p:v_naz
голів голова noun:anim:p:v_rod
головам голова noun:anim:p:v_dav
голів голова noun:anim:p:v_zna
голови голова noun:anim:p:v_zna:rare # з в/у
головами голова noun:anim:p:v_oru
головах голова noun:anim:p:v_mis
голови голова noun:anim:p:v_kly'''.trim() //.split("\n")


    @Test
    void testGolova() {
        def lines = ["голова /n10.p2.< :+m      # посада"]
        assertEquals golovaFull, expand.process_input(lines).join("\n").replaceAll(/[<>]/, '')
    }

	
	def adjLastName =
'''
Аверянова Аверянова noun:anim:f:v_naz:prop:lname
Аверянової Аверянова noun:anim:f:v_rod:prop:lname
Аверяновій Аверянова noun:anim:f:v_dav:prop:lname
Аверянову Аверянова noun:anim:f:v_zna:prop:lname
Аверяновою Аверянова noun:anim:f:v_oru:prop:lname
Аверяновій Аверянова noun:anim:f:v_mis:prop:lname
Аверянова Аверянова noun:anim:f:v_kly:prop:lname
'''.trim()

	@Test
	void testAdjLastName() {
		def lines = ["Аверянова /n2adj1.<+"]
		assertEquals(join(expand.process_input(lines)), adjLastName)
//		assertEquals(DicEntry.fromLines(adjLastName.split(/\n/)), join(expand.process_input(lines)))
	}

def aFull =
'''
а а conj:coord
а а intj
а а part
'''.trim()

	@Test
	void testFullAlt() {
		def lines = ["а conj:coord|part|intj"]
		assertEquals(join(expand.process_input(lines)), aFull)
	}

def multilineFull =
'''
вагомий вагомий adj:m:v_naz:compb
вагомого вагомий adj:m:v_rod:compb
вагомому вагомий adj:m:v_dav:compb
вагомого вагомий adj:m:v_zna:ranim:compb
вагомий вагомий adj:m:v_zna:rinanim:compb
вагомим вагомий adj:m:v_oru:compb
вагомім вагомий adj:m:v_mis:compb
вагомому вагомий adj:m:v_mis:compb
вагомий вагомий adj:m:v_kly:compb
вагома вагомий adj:f:v_naz:compb
вагомої вагомий adj:f:v_rod:compb
вагомій вагомий adj:f:v_dav:compb
вагому вагомий adj:f:v_zna:compb
вагомою вагомий adj:f:v_oru:compb
вагомій вагомий adj:f:v_mis:compb
вагома вагомий adj:f:v_kly:compb
вагоме вагомий adj:n:v_naz:compb
вагомого вагомий adj:n:v_rod:compb
вагомому вагомий adj:n:v_dav:compb
вагоме вагомий adj:n:v_zna:compb
вагомим вагомий adj:n:v_oru:compb
вагомім вагомий adj:n:v_mis:compb
вагомому вагомий adj:n:v_mis:compb
вагоме вагомий adj:n:v_kly:compb
вагомі вагомий adj:p:v_naz:compb
вагомих вагомий adj:p:v_rod:compb
вагомим вагомий adj:p:v_dav:compb
вагомих вагомий adj:p:v_zna:ranim:compb
вагомі вагомий adj:p:v_zna:rinanim:compb
вагомими вагомий adj:p:v_oru:compb
вагомих вагомий adj:p:v_mis:compb
вагомі вагомий adj:p:v_kly:compb
вагоміший вагоміший adj:m:v_naz:compc
вагомішого вагоміший adj:m:v_rod:compc
вагомішому вагоміший adj:m:v_dav:compc
вагомішого вагоміший adj:m:v_zna:ranim:compc
вагоміший вагоміший adj:m:v_zna:rinanim:compc
вагомішим вагоміший adj:m:v_oru:compc
вагомішім вагоміший adj:m:v_mis:compc
вагомішому вагоміший adj:m:v_mis:compc
вагоміший вагоміший adj:m:v_kly:compc
вагоміша вагоміший adj:f:v_naz:compc
вагомішої вагоміший adj:f:v_rod:compc
вагомішій вагоміший adj:f:v_dav:compc
вагомішу вагоміший adj:f:v_zna:compc
вагомішою вагоміший adj:f:v_oru:compc
вагомішій вагоміший adj:f:v_mis:compc
вагоміша вагоміший adj:f:v_kly:compc
вагоміше вагоміший adj:n:v_naz:compc
вагомішого вагоміший adj:n:v_rod:compc
вагомішому вагоміший adj:n:v_dav:compc
вагоміше вагоміший adj:n:v_zna:compc
вагомішим вагоміший adj:n:v_oru:compc
вагомішім вагоміший adj:n:v_mis:compc
вагомішому вагоміший adj:n:v_mis:compc
вагоміше вагоміший adj:n:v_kly:compc
вагоміші вагоміший adj:p:v_naz:compc
вагоміших вагоміший adj:p:v_rod:compc
вагомішим вагоміший adj:p:v_dav:compc
вагоміших вагоміший adj:p:v_zna:ranim:compc
вагоміші вагоміший adj:p:v_zna:rinanim:compc
вагомішими вагоміший adj:p:v_oru:compc
вагоміших вагоміший adj:p:v_mis:compc
вагоміші вагоміший adj:p:v_kly:compc
найвагоміший найвагоміший adj:m:v_naz:comps
найвагомішого найвагоміший adj:m:v_rod:comps
найвагомішому найвагоміший adj:m:v_dav:comps
найвагомішого найвагоміший adj:m:v_zna:ranim:comps
найвагоміший найвагоміший adj:m:v_zna:rinanim:comps
найвагомішим найвагоміший adj:m:v_oru:comps
найвагомішім найвагоміший adj:m:v_mis:comps
найвагомішому найвагоміший adj:m:v_mis:comps
найвагоміший найвагоміший adj:m:v_kly:comps
найвагоміша найвагоміший adj:f:v_naz:comps
найвагомішої найвагоміший adj:f:v_rod:comps
найвагомішій найвагоміший adj:f:v_dav:comps
найвагомішу найвагоміший adj:f:v_zna:comps
найвагомішою найвагоміший adj:f:v_oru:comps
найвагомішій найвагоміший adj:f:v_mis:comps
найвагоміша найвагоміший adj:f:v_kly:comps
найвагоміше найвагоміший adj:n:v_naz:comps
найвагомішого найвагоміший adj:n:v_rod:comps
найвагомішому найвагоміший adj:n:v_dav:comps
найвагоміше найвагоміший adj:n:v_zna:comps
найвагомішим найвагоміший adj:n:v_oru:comps
найвагомішім найвагоміший adj:n:v_mis:comps
найвагомішому найвагоміший adj:n:v_mis:comps
найвагоміше найвагоміший adj:n:v_kly:comps
найвагоміші найвагоміший adj:p:v_naz:comps
найвагоміших найвагоміший adj:p:v_rod:comps
найвагомішим найвагоміший adj:p:v_dav:comps
найвагоміших найвагоміший adj:p:v_zna:ranim:comps
найвагоміші найвагоміший adj:p:v_zna:rinanim:comps
найвагомішими найвагоміший adj:p:v_oru:comps
найвагоміших найвагоміший adj:p:v_mis:comps
найвагоміші найвагоміший adj:p:v_kly:comps
щонайвагоміший щонайвагоміший adj:m:v_naz:comps
щонайвагомішого щонайвагоміший adj:m:v_rod:comps
щонайвагомішому щонайвагоміший adj:m:v_dav:comps
щонайвагомішого щонайвагоміший adj:m:v_zna:ranim:comps
щонайвагоміший щонайвагоміший adj:m:v_zna:rinanim:comps
щонайвагомішим щонайвагоміший adj:m:v_oru:comps
щонайвагомішім щонайвагоміший adj:m:v_mis:comps
щонайвагомішому щонайвагоміший adj:m:v_mis:comps
щонайвагоміший щонайвагоміший adj:m:v_kly:comps
щонайвагоміша щонайвагоміший adj:f:v_naz:comps
щонайвагомішої щонайвагоміший adj:f:v_rod:comps
щонайвагомішій щонайвагоміший adj:f:v_dav:comps
щонайвагомішу щонайвагоміший adj:f:v_zna:comps
щонайвагомішою щонайвагоміший adj:f:v_oru:comps
щонайвагомішій щонайвагоміший adj:f:v_mis:comps
щонайвагоміша щонайвагоміший adj:f:v_kly:comps
щонайвагоміше щонайвагоміший adj:n:v_naz:comps
щонайвагомішого щонайвагоміший adj:n:v_rod:comps
щонайвагомішому щонайвагоміший adj:n:v_dav:comps
щонайвагоміше щонайвагоміший adj:n:v_zna:comps
щонайвагомішим щонайвагоміший adj:n:v_oru:comps
щонайвагомішім щонайвагоміший adj:n:v_mis:comps
щонайвагомішому щонайвагоміший adj:n:v_mis:comps
щонайвагоміше щонайвагоміший adj:n:v_kly:comps
щонайвагоміші щонайвагоміший adj:p:v_naz:comps
щонайвагоміших щонайвагоміший adj:p:v_rod:comps
щонайвагомішим щонайвагоміший adj:p:v_dav:comps
щонайвагоміших щонайвагоміший adj:p:v_zna:ranim:comps
щонайвагоміші щонайвагоміший adj:p:v_zna:rinanim:comps
щонайвагомішими щонайвагоміший adj:p:v_oru:comps
щонайвагоміших щонайвагоміший adj:p:v_mis:comps
щонайвагоміші щонайвагоміший adj:p:v_kly:comps
щоякнайвагоміший щоякнайвагоміший adj:m:v_naz:comps
щоякнайвагомішого щоякнайвагоміший adj:m:v_rod:comps
щоякнайвагомішому щоякнайвагоміший adj:m:v_dav:comps
щоякнайвагомішого щоякнайвагоміший adj:m:v_zna:ranim:comps
щоякнайвагоміший щоякнайвагоміший adj:m:v_zna:rinanim:comps
щоякнайвагомішим щоякнайвагоміший adj:m:v_oru:comps
щоякнайвагомішім щоякнайвагоміший adj:m:v_mis:comps
щоякнайвагомішому щоякнайвагоміший adj:m:v_mis:comps
щоякнайвагоміший щоякнайвагоміший adj:m:v_kly:comps
щоякнайвагоміша щоякнайвагоміший adj:f:v_naz:comps
щоякнайвагомішої щоякнайвагоміший adj:f:v_rod:comps
щоякнайвагомішій щоякнайвагоміший adj:f:v_dav:comps
щоякнайвагомішу щоякнайвагоміший adj:f:v_zna:comps
щоякнайвагомішою щоякнайвагоміший adj:f:v_oru:comps
щоякнайвагомішій щоякнайвагоміший adj:f:v_mis:comps
щоякнайвагоміша щоякнайвагоміший adj:f:v_kly:comps
щоякнайвагоміше щоякнайвагоміший adj:n:v_naz:comps
щоякнайвагомішого щоякнайвагоміший adj:n:v_rod:comps
щоякнайвагомішому щоякнайвагоміший adj:n:v_dav:comps
щоякнайвагоміше щоякнайвагоміший adj:n:v_zna:comps
щоякнайвагомішим щоякнайвагоміший adj:n:v_oru:comps
щоякнайвагомішім щоякнайвагоміший adj:n:v_mis:comps
щоякнайвагомішому щоякнайвагоміший adj:n:v_mis:comps
щоякнайвагоміше щоякнайвагоміший adj:n:v_kly:comps
щоякнайвагоміші щоякнайвагоміший adj:p:v_naz:comps
щоякнайвагоміших щоякнайвагоміший adj:p:v_rod:comps
щоякнайвагомішим щоякнайвагоміший adj:p:v_dav:comps
щоякнайвагоміших щоякнайвагоміший adj:p:v_zna:ranim:comps
щоякнайвагоміші щоякнайвагоміший adj:p:v_zna:rinanim:comps
щоякнайвагомішими щоякнайвагоміший adj:p:v_oru:comps
щоякнайвагоміших щоякнайвагоміший adj:p:v_mis:comps
щоякнайвагоміші щоякнайвагоміший adj:p:v_kly:comps
якнайвагоміший якнайвагоміший adj:m:v_naz:comps
якнайвагомішого якнайвагоміший adj:m:v_rod:comps
якнайвагомішому якнайвагоміший adj:m:v_dav:comps
якнайвагомішого якнайвагоміший adj:m:v_zna:ranim:comps
якнайвагоміший якнайвагоміший adj:m:v_zna:rinanim:comps
якнайвагомішим якнайвагоміший adj:m:v_oru:comps
якнайвагомішім якнайвагоміший adj:m:v_mis:comps
якнайвагомішому якнайвагоміший adj:m:v_mis:comps
якнайвагоміший якнайвагоміший adj:m:v_kly:comps
якнайвагоміша якнайвагоміший adj:f:v_naz:comps
якнайвагомішої якнайвагоміший adj:f:v_rod:comps
якнайвагомішій якнайвагоміший adj:f:v_dav:comps
якнайвагомішу якнайвагоміший adj:f:v_zna:comps
якнайвагомішою якнайвагоміший adj:f:v_oru:comps
якнайвагомішій якнайвагоміший adj:f:v_mis:comps
якнайвагоміша якнайвагоміший adj:f:v_kly:comps
якнайвагоміше якнайвагоміший adj:n:v_naz:comps
якнайвагомішого якнайвагоміший adj:n:v_rod:comps
якнайвагомішому якнайвагоміший adj:n:v_dav:comps
якнайвагоміше якнайвагоміший adj:n:v_zna:comps
якнайвагомішим якнайвагоміший adj:n:v_oru:comps
якнайвагомішім якнайвагоміший adj:n:v_mis:comps
якнайвагомішому якнайвагоміший adj:n:v_mis:comps
якнайвагоміше якнайвагоміший adj:n:v_kly:comps
якнайвагоміші якнайвагоміший adj:p:v_naz:comps
якнайвагоміших якнайвагоміший adj:p:v_rod:comps
якнайвагомішим якнайвагоміший adj:p:v_dav:comps
якнайвагоміших якнайвагоміший adj:p:v_zna:ranim:comps
якнайвагоміші якнайвагоміший adj:p:v_zna:rinanim:comps
якнайвагомішими якнайвагоміший adj:p:v_oru:comps
якнайвагоміших якнайвагоміший adj:p:v_mis:comps
якнайвагоміші якнайвагоміший adj:p:v_kly:comps
'''.trim()
	
	@Test
//	@Ignore
	void testMultiline() {
		def lines = ["вагомий /adj \\", " +cs=вагоміший"]
//		assert join(expand.process_input(lines)) == multilineFull
		assertEquals(multilineFull, join(expand.process_input(lines)))
	}

	
	def multilineFull2 =
	'''
вагоміше вагоміше adv:compc
вагоміш вагоміше adv:compc:short
вагомо вагомо adv:compb
найвагоміше найвагоміше adv:comps
найвагоміш найвагоміше adv:comps:short
щонайвагоміше щонайвагоміше adv:comps
щонайвагоміш щонайвагоміше adv:comps:short
якнайвагоміше якнайвагоміше adv:comps
якнайвагоміш якнайвагоміше adv:comps:short
'''.trim()
//TODO: щоякнайвагоміше щоякнайвагоміше adv:comps

		
    @Test
    void testMultilineWithTag() {
        def lines = ["вагомо adv \\", " +cs=вагоміше \\", " +cs=вагоміш"]
        assertEquals(multilineFull2, join(expand.process_input(lines)))
    }

	def multilineIndent =
'''
вагоміше adv:compc
  вагоміш adv:compc:short
вагомо adv:compb
найвагоміше adv:comps
  найвагоміш adv:comps:short
щонайвагоміше adv:comps
  щонайвагоміш adv:comps:short
якнайвагоміше adv:comps
  якнайвагоміш adv:comps:short
'''.trim()
			
	@Test
	void testMultilineWithIndent() {
		def lines = ["вагомо adv \\", " +cs=вагоміше \\", " +cs=вагоміш"]
		assertEquals(multilineIndent, new DictSorter().indent_lines(expand.process_input(lines)).join("\n"))
	}

    
    def multilineFull21 =
'''
вагоміше вагоміше adv:compc:slang
вагомо вагомо adv:compb
найвагоміше найвагоміше adv:comps:slang
щонайвагоміше щонайвагоміше adv:comps:slang
якнайвагоміше якнайвагоміше adv:comps:slang
'''.trim()
            
    @Test
    void testMultilineWithExtraTag2() {
        def lines = ["вагомо adv \\", " +cs=вагоміше :slang"]
        assertEquals(multilineFull21, join(expand.process_input(lines)))
    }

            
def multilineFull22 =
'''
ймовірніше ймовірніше adv:compc
ймовірніш ймовірніше adv:compc:short
ймовірно ймовірно adv:compb
'''.trim()
    
    @Test
    void testMultilineWithException() {
        def lines = ["ймовірно adv \\", " +cs=ймовірніше"]
        assertEquals(multilineFull22, join(expand.process_input(lines)))
    }

	
String[] taggedIn = 
'''
абичий абичий adj:m:v_naz/v_zna:&pron:ind
абичийого абичий adj:m:v_rod/v_zna//n:v_rod:&pron:ind
абичийому абичий adj:m:v_dav/v_mis//n:v_dav/v_mis:&pron:ind
абичиєму абичий adj:m:v_dav/v_mis//n:v_dav/v_mis:&pron:ind
абичиїм абичий adj:m:v_mis//n:v_oru//p:v_dav:&pron:ind
абичия абичий adj:f:v_naz:&pron:ind
абичиєї абичий adj:f:v_rod:&pron:ind
абичиїй абичий adj:f:v_dav/v_mis:&pron:ind
абичию абичий adj:f:v_zna:&pron:ind
абичиєю абичий adj:f:v_oru:&pron:ind
абичиї абичий adj:p:v_naz/v_zna:&pron:ind
абичиїх абичий adj:p:v_rod/v_zna/v_mis:&pron:ind
абичиїми абичий adj:p:v_oru:&pron:ind
'''.trim().split("\n")
		
def taggedOut = 
'''
абичий абичий adj:m:v_naz:&pron:ind
абичийого абичий adj:m:v_rod:&pron:ind
абичиєму абичий adj:m:v_dav:&pron:ind
абичийому абичий adj:m:v_dav:&pron:ind
абичий абичий adj:m:v_zna:&pron:ind
абичийого абичий adj:m:v_zna:&pron:ind
абичиєму абичий adj:m:v_mis:&pron:ind
абичиїм абичий adj:m:v_mis:&pron:ind
абичийому абичий adj:m:v_mis:&pron:ind
абичия абичий adj:f:v_naz:&pron:ind
абичиєї абичий adj:f:v_rod:&pron:ind
абичиїй абичий adj:f:v_dav:&pron:ind
абичию абичий adj:f:v_zna:&pron:ind
абичиєю абичий adj:f:v_oru:&pron:ind
абичиїй абичий adj:f:v_mis:&pron:ind
абичийого абичий adj:n:v_rod:&pron:ind
абичиєму абичий adj:n:v_dav:&pron:ind
абичийому абичий adj:n:v_dav:&pron:ind
абичиїм абичий adj:n:v_oru:&pron:ind
абичиєму абичий adj:n:v_mis:&pron:ind
абичийому абичий adj:n:v_mis:&pron:ind
абичиї абичий adj:p:v_naz:&pron:ind
абичиїх абичий adj:p:v_rod:&pron:ind
абичиїм абичий adj:p:v_dav:&pron:ind
абичиї абичий adj:p:v_zna:&pron:ind
абичиїх абичий adj:p:v_zna:&pron:ind
абичиїми абичий adj:p:v_oru:&pron:ind
абичиїх абичий adj:p:v_mis:&pron:ind
'''.trim()	

	@Test
	void testTaggedLine() {
		assert join(expand.process_input(Arrays.asList(taggedIn))) == taggedOut
	}

	
	
	def strilyatyFull =
'''
стрілявши стрілявши advp:imperf
стріляти стріляти verb:imperf:inf
стрілять стріляти verb:imperf:inf:short
стріляй стріляти verb:imperf:impr:s:2
стріляймо стріляти verb:imperf:impr:p:1
стріляйте стріляти verb:imperf:impr:p:2
стріляю стріляти verb:imperf:pres:s:1
стріляєш стріляти verb:imperf:pres:s:2
стріляє стріляти verb:imperf:pres:s:3
стріляємо стріляти verb:imperf:pres:p:1
стріляєм стріляти verb:imperf:pres:p:1:subst
стріляєте стріляти verb:imperf:pres:p:2
стріляють стріляти verb:imperf:pres:p:3
стрілятиму стріляти verb:imperf:futr:s:1
стрілятимеш стріляти verb:imperf:futr:s:2
стрілятиме стріляти verb:imperf:futr:s:3
стрілятимем стріляти verb:imperf:futr:p:1
стрілятимемо стріляти verb:imperf:futr:p:1
стрілятимете стріляти verb:imperf:futr:p:2
стрілятимуть стріляти verb:imperf:futr:p:3
стріляв стріляти verb:imperf:past:m
стріляла стріляти verb:imperf:past:f
стріляло стріляти verb:imperf:past:n
стріляли стріляти verb:imperf:past:p
стріляно стріляти verb:imperf:impers
'''.trim()

	@Test
	void testStrilyaty() {
		def lines = ["стріляти /v2.cf.isNo :imperf    # comment"]
		assertEquals(strilyatyFull, join(expand.process_input(lines)))
	}

	
	def strilyatyFullIndented =
'''
стрілявши advp:imperf
стріляти verb:imperf:inf    # comment
  стрілять verb:imperf:inf:short
  стріляй verb:imperf:impr:s:2
  стріляймо verb:imperf:impr:p:1
  стріляйте verb:imperf:impr:p:2
  стріляю verb:imperf:pres:s:1
  стріляєш verb:imperf:pres:s:2
  стріляє verb:imperf:pres:s:3
  стріляємо verb:imperf:pres:p:1
  стріляєм verb:imperf:pres:p:1:subst
  стріляєте verb:imperf:pres:p:2
  стріляють verb:imperf:pres:p:3
  стрілятиму verb:imperf:futr:s:1
  стрілятимеш verb:imperf:futr:s:2
  стрілятиме verb:imperf:futr:s:3
  стрілятимем verb:imperf:futr:p:1
  стрілятимемо verb:imperf:futr:p:1
  стрілятимете verb:imperf:futr:p:2
  стрілятимуть verb:imperf:futr:p:3
  стріляв verb:imperf:past:m
  стріляла verb:imperf:past:f
  стріляло verb:imperf:past:n
  стріляли verb:imperf:past:p
  стріляно verb:imperf:impers
'''.trim()

	@Test
	void testStrilyatyIndented() {
		def lines = ["стріляти /v2.cf.isNo :imperf    # comment"]
		assertEquals(strilyatyFullIndented, new DictSorter().indent_lines(expand.process_input(lines)).join("\n"))
	}


    def strilyatyFullIndentedWithProp =
    '''
АПБ noun:inanim:f:v_naz:nv:abbr:prop    # Академія пожежної безпеки
  АПБ noun:inanim:f:v_rod:nv:abbr:prop
  АПБ noun:inanim:f:v_dav:nv:abbr:prop
  АПБ noun:inanim:f:v_zna:nv:abbr:prop
  АПБ noun:inanim:f:v_oru:nv:abbr:prop
  АПБ noun:inanim:f:v_mis:nv:abbr:prop
  АПБ noun:inanim:f:v_kly:nv:abbr:prop
АПБ noun:inanim:m:v_naz:nv:abbr    # агропромисловий банк
  АПБ noun:inanim:m:v_rod:nv:abbr
  АПБ noun:inanim:m:v_dav:nv:abbr
  АПБ noun:inanim:m:v_zna:nv:abbr
  АПБ noun:inanim:m:v_oru:nv:abbr
  АПБ noun:inanim:m:v_mis:nv:abbr
  АПБ noun:inanim:m:v_kly:nv:abbr
'''.trim()


    @Disabled
    @Test
    void testIndentedWithProp() {
        def lines = [
'''
АПБ noun:f:nv:np:prop:abbr:xp2                       # Академія пожежної безпеки
АПБ noun:m:nv:np:abbr:xp1                        # агропромисловий банк
'''.trim()
                ]
            assertEquals(strilyatyFullIndentedWithProp, new DictSorter().indent_lines(expand.process_input(lines)).join("\n"))
        }
    

	String stryvatyFull =
'''
стривати стривати verb:imperf:inf
стривать стривати verb:imperf:inf:short
стривай стривати verb:imperf:impr:s:2
стриваймо стривати verb:imperf:impr:p:1
стривайте стривати verb:imperf:impr:p:2
'''.trim()

@Test
void testStryvaty() {
	List<String> lines = ["стривати /v2 tag=:impr|:inf :imperf"]
	assertEquals(stryvatyFull, join(expand.process_input(lines)))
}

	
	String adjNvFull =
'''
супер-пупер супер-пупер adj:m:v_naz:nv
супер-пупер супер-пупер adj:m:v_rod:nv
супер-пупер супер-пупер adj:m:v_dav:nv
супер-пупер супер-пупер adj:m:v_zna:ranim:nv
супер-пупер супер-пупер adj:m:v_zna:rinanim:nv
супер-пупер супер-пупер adj:m:v_oru:nv
супер-пупер супер-пупер adj:m:v_mis:nv
супер-пупер супер-пупер adj:m:v_kly:nv
супер-пупер супер-пупер adj:f:v_naz:nv
супер-пупер супер-пупер adj:f:v_rod:nv
супер-пупер супер-пупер adj:f:v_dav:nv
супер-пупер супер-пупер adj:f:v_zna:nv
супер-пупер супер-пупер adj:f:v_oru:nv
супер-пупер супер-пупер adj:f:v_mis:nv
супер-пупер супер-пупер adj:f:v_kly:nv
супер-пупер супер-пупер adj:n:v_naz:nv
супер-пупер супер-пупер adj:n:v_rod:nv
супер-пупер супер-пупер adj:n:v_dav:nv
супер-пупер супер-пупер adj:n:v_zna:nv
супер-пупер супер-пупер adj:n:v_oru:nv
супер-пупер супер-пупер adj:n:v_mis:nv
супер-пупер супер-пупер adj:n:v_kly:nv
супер-пупер супер-пупер adj:p:v_naz:nv
супер-пупер супер-пупер adj:p:v_rod:nv
супер-пупер супер-пупер adj:p:v_dav:nv
супер-пупер супер-пупер adj:p:v_zna:ranim:nv
супер-пупер супер-пупер adj:p:v_zna:rinanim:nv
супер-пупер супер-пупер adj:p:v_oru:nv
супер-пупер супер-пупер adj:p:v_mis:nv
супер-пупер супер-пупер adj:p:v_kly:nv
супер-пупер супер-пупер adv
'''.trim()


	@Test
	void testAdjAd() {
		List<String> lines = ["супер-пупер adj:nv:coll|adv:coll"]
		assertEquals(adjNvFull, join(expand.process_input(lines)))
	}

    String adjNvFull2 =
'''
кв. кв. adj:m:v_naz:nv
кв. кв. adj:m:v_rod:nv
кв. кв. adj:m:v_dav:nv
кв. кв. adj:m:v_zna:rinanim:nv
кв. кв. adj:m:v_oru:nv
кв. кв. adj:m:v_mis:nv
кв. кв. adj:f:v_naz:nv
кв. кв. adj:f:v_rod:nv
кв. кв. adj:f:v_dav:nv
кв. кв. adj:f:v_zna:nv
кв. кв. adj:f:v_oru:nv
кв. кв. adj:f:v_mis:nv
кв. кв. adj:n:v_naz:nv
кв. кв. adj:n:v_rod:nv
кв. кв. adj:n:v_dav:nv
кв. кв. adj:n:v_zna:nv
кв. кв. adj:n:v_oru:nv
кв. кв. adj:n:v_mis:nv
кв. кв. adj:p:v_naz:nv
кв. кв. adj:p:v_rod:nv
кв. кв. adj:p:v_dav:nv
кв. кв. adj:p:v_zna:rinanim:nv
кв. кв. adj:p:v_oru:nv
кв. кв. adj:p:v_mis:nv
'''.trim()
    
    
    @Test
    void testAdj2() {
        List<String> lines = ["кв. adj:nv:rinanim"]
        assertEquals(adjNvFull2, join(expand.process_input(lines)))
    }
        
    String adjNvFull3 =
'''
преп. преп. adj:m:v_naz:nv
преп. преп. adj:m:v_rod:nv
преп. преп. adj:m:v_dav:nv
преп. преп. adj:m:v_zna:ranim:nv
преп. преп. adj:m:v_oru:nv
преп. преп. adj:m:v_mis:nv
преп. преп. adj:f:v_naz:nv
преп. преп. adj:f:v_rod:nv
преп. преп. adj:f:v_dav:nv
преп. преп. adj:f:v_zna:nv
преп. преп. adj:f:v_oru:nv
преп. преп. adj:f:v_mis:nv
преп. преп. adj:n:v_naz:nv
преп. преп. adj:n:v_rod:nv
преп. преп. adj:n:v_dav:nv
преп. преп. adj:n:v_zna:nv
преп. преп. adj:n:v_oru:nv
преп. преп. adj:n:v_mis:nv
преп. преп. adj:p:v_naz:nv
преп. преп. adj:p:v_rod:nv
преп. преп. adj:p:v_dav:nv
преп. преп. adj:p:v_zna:ranim:nv
преп. преп. adj:p:v_oru:nv
преп. преп. adj:p:v_mis:nv
'''.trim()
        
        
    @Test
    void testAdj3() {
        List<String> lines = ["преп. adj:nv:ranim"]
        assertEquals(adjNvFull3, join(expand.process_input(lines)))
    }
    
def expectedNvNumr = """
півчвертаста півчвертаста numr:p:v_naz:nv:rare
півчвертаста півчвертаста numr:p:v_rod:nv:rare
півчвертаста півчвертаста numr:p:v_dav:nv:rare
півчвертаста півчвертаста numr:p:v_zna:nv:rare
півчвертаста півчвертаста numr:p:v_oru:nv:rare
півчвертаста півчвертаста numr:p:v_mis:nv:rare
півчвертаста півчвертаста numr:p:v_kly:nv:rare
""".trim()
    
    @Test
    void testNvNumr() {
        assertEquals(expectedNvNumr, join(expand.process_input(Arrays.asList("півчвертаста numr:p:nv:rare"))))
    }
	
    
    @Test
    void testNvAbbr() {
        List<String> lines = ["авт. noun:m:v_naz:nv:anim"]
        assertEquals('авт. авт. noun:anim:m:v_naz:nv', join(expand.process_input(lines)))
    }

    @Test
    void testTagOrder() {
        List<String> lines = ["ладен ладний adj:m:v_naz:short:&predic"]
        assertEquals('ладен ладний adj:m:v_naz:short:&predic', join(expand.process_input(lines)))
    }
    
//	def strilyatyBad =
//	'''
//стрілявши advp:imperf:bad
//стріляти verb:imperf:inf:bad
//  стріляй verb:imperf:impr:s:2:bad
//  стріляймо verb:imperf:impr:p:1:bad
//  стріляйте verb:imperf:impr:p:2:bad
//  стріляю verb:imperf:pres:s:1:bad
//  стріляєш verb:imperf:pres:s:2:bad
//  стріляє verb:imperf:pres:s:3:bad
//  стріляємо verb:imperf:pres:p:1:bad
//  стріляєте verb:imperf:pres:p:2:bad
//  стріляють verb:imperf:pres:p:3:bad
//  стріляв verb:imperf:past:m:bad
//  стріляла verb:imperf:past:f:bad
//  стріляло verb:imperf:past:n:bad
//  стріляли verb:imperf:past:p:bad
//'''.trim()
//	
//	@Test
//	void testStrilyatyBad() {
//		def lines = ["стріляти /v2 :imperf:bad"]
//		assertEquals(strilyatyBad, new DictSorter().indent_lines(expand.process_input(lines)).join("\n"))
//	}
	
	

	static final String join(List<DicEntry> entries) {
		return entries.collect{ it.toFlatString() }.join("\n") //.replaceAll(/[<>]/, '')
	}


    	
}


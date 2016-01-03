//import static org.junit.Assert

package org.dict_uk.expand

import org.junit.Before
import org.junit.Ignore;
import org.junit.Test;

public class ExpandTest extends GroovyTestCase {
	
	static Expand expand
	
	static
	{
		String[] args = {"-mfl"}
		Args.parse(args)
		
		expand = new Expand()
		def affixDir = new File("data/affix").isDirectory() ? "data/affix" : "../../../data/affix"
		expand.affix.load_affixes(affixDir)
	}
	
	@Override
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
		assert expand.expand_suffixes("порт", "n20.p", [:], ":rare").join("\n") == port
	}

	def portFull =
'''
порт порт noun:m:v_naz:rare
порту порт noun:m:v_rod:rare
портові порт noun:m:v_dav:rare
порту порт noun:m:v_dav:rare
порт порт noun:m:v_zna:rare
портом порт noun:m:v_oru:rare
порті порт noun:m:v_mis:rare
портові порт noun:m:v_mis:rare
порту порт noun:m:v_mis:rare
порти порт noun:p:v_naz:rare
портів порт noun:p:v_rod:rare
портам порт noun:p:v_dav:rare
порти порт noun:p:v_zna:rare
портами порт noun:p:v_oru:rare
портах порт noun:p:v_mis:rare
'''.trim() //.split("\n")
	
	@Test
	void testProcessInput() {
		def lines = ["порт /n20.p :rare"]
		assert expand.process_input(lines, false).join("\n") == portFull
	}
	
	def zhabaFull =
	'''
жаба жаба noun:m:v_naz:anim
жаби жаба noun:m:v_rod:anim
жабі жаба noun:m:v_dav:anim
жабу жаба noun:m:v_zna:anim
жабою жаба noun:m:v_oru:anim
жабі жаба noun:m:v_mis:anim
жаби жаба noun:p:v_naz:anim
жаб жаба noun:p:v_rod:anim
жабам жаба noun:p:v_dav:anim
жаб жаба noun:p:v_zna:anim
жаби жаба noun:p:v_zna:anim
жабами жаба noun:p:v_oru:anim
жабах жаба noun:p:v_mis:anim
'''.trim() //.split("\n")

	@Test
	void testZhaba() {
		def lines = ["жаба /n10.p1.<> ^noun:m"]
		assert expand.process_input(lines, false).join("\n") == zhabaFull
	}

	def stryvatyFull =
'''
стривати стривати verb:inf:imperf
стривай стривати verb:impr:s:2:imperf
стриваймо стривати verb:impr:p:1:imperf
стривайте стривати verb:impr:p:2:imperf
'''.trim()

	@Test
	void testStryvaty() {
		def lines = ["стривати /v2 tag=:impr|:inf :imperf"]
		assert expand.process_input(lines, false).join("\n") == stryvatyFull
	}

def aFull =
'''
а а conj:coord
а а excl
а а part
'''.trim()

	@Test
	void testFullAlt() {
		def lines = ["а conj:coord|part|excl"]
		assert expand.process_input(lines, false).join("\n") == aFull
	}

def multilineFull =
'''
вагомий вагомий adj:m:v_naz:compb
вагомого вагомий adj:m:v_rod:compb
вагомому вагомий adj:m:v_dav:compb
вагомий вагомий adj:m:v_zna:compb
вагомого вагомий adj:m:v_zna:compb
вагомим вагомий adj:m:v_oru:compb
вагомім вагомий adj:m:v_mis:compb
вагомому вагомий adj:m:v_mis:compb
вагома вагомий adj:f:v_naz:compb
вагомої вагомий adj:f:v_rod:compb
вагомій вагомий adj:f:v_dav:compb
вагому вагомий adj:f:v_zna:compb
вагомою вагомий adj:f:v_oru:compb
вагомій вагомий adj:f:v_mis:compb
вагоме вагомий adj:n:v_naz:compb
вагомого вагомий adj:n:v_rod:compb
вагомому вагомий adj:n:v_dav:compb
вагоме вагомий adj:n:v_zna:compb
вагомим вагомий adj:n:v_oru:compb
вагомім вагомий adj:n:v_mis:compb
вагомому вагомий adj:n:v_mis:compb
вагомі вагомий adj:p:v_naz:compb
вагомих вагомий adj:p:v_rod:compb
вагомим вагомий adj:p:v_dav:compb
вагомих вагомий adj:p:v_zna:compb
вагомі вагомий adj:p:v_zna:compb
вагомими вагомий adj:p:v_oru:compb
вагомих вагомий adj:p:v_mis:compb
вагоміший вагомий adj:m:v_naz:compr
вагомішого вагомий adj:m:v_rod:compr
вагомішому вагомий adj:m:v_dav:compr
вагоміший вагомий adj:m:v_zna:compr
вагомішого вагомий adj:m:v_zna:compr
вагомішим вагомий adj:m:v_oru:compr
вагомішім вагомий adj:m:v_mis:compr
вагомішому вагомий adj:m:v_mis:compr
вагоміша вагомий adj:f:v_naz:compr
вагомішої вагомий adj:f:v_rod:compr
вагомішій вагомий adj:f:v_dav:compr
вагомішу вагомий adj:f:v_zna:compr
вагомішою вагомий adj:f:v_oru:compr
вагомішій вагомий adj:f:v_mis:compr
вагоміше вагомий adj:n:v_naz:compr
вагомішого вагомий adj:n:v_rod:compr
вагомішому вагомий adj:n:v_dav:compr
вагоміше вагомий adj:n:v_zna:compr
вагомішим вагомий adj:n:v_oru:compr
вагомішім вагомий adj:n:v_mis:compr
вагомішому вагомий adj:n:v_mis:compr
вагоміші вагомий adj:p:v_naz:compr
вагоміших вагомий adj:p:v_rod:compr
вагомішим вагомий adj:p:v_dav:compr
вагоміших вагомий adj:p:v_zna:compr
вагоміші вагомий adj:p:v_zna:compr
вагомішими вагомий adj:p:v_oru:compr
вагоміших вагомий adj:p:v_mis:compr
найвагоміший вагомий adj:m:v_naz:super
найвагомішого вагомий adj:m:v_rod:super
найвагомішому вагомий adj:m:v_dav:super
найвагоміший вагомий adj:m:v_zna:super
найвагомішого вагомий adj:m:v_zna:super
найвагомішим вагомий adj:m:v_oru:super
найвагомішім вагомий adj:m:v_mis:super
найвагомішому вагомий adj:m:v_mis:super
найвагоміша вагомий adj:f:v_naz:super
найвагомішої вагомий adj:f:v_rod:super
найвагомішій вагомий adj:f:v_dav:super
найвагомішу вагомий adj:f:v_zna:super
найвагомішою вагомий adj:f:v_oru:super
найвагомішій вагомий adj:f:v_mis:super
найвагоміше вагомий adj:n:v_naz:super
найвагомішого вагомий adj:n:v_rod:super
найвагомішому вагомий adj:n:v_dav:super
найвагоміше вагомий adj:n:v_zna:super
найвагомішим вагомий adj:n:v_oru:super
найвагомішім вагомий adj:n:v_mis:super
найвагомішому вагомий adj:n:v_mis:super
найвагоміші вагомий adj:p:v_naz:super
найвагоміших вагомий adj:p:v_rod:super
найвагомішим вагомий adj:p:v_dav:super
найвагоміших вагомий adj:p:v_zna:super
найвагоміші вагомий adj:p:v_zna:super
найвагомішими вагомий adj:p:v_oru:super
найвагоміших вагомий adj:p:v_mis:super
щонайвагоміший вагомий adj:m:v_naz:super
щонайвагомішого вагомий adj:m:v_rod:super
щонайвагомішому вагомий adj:m:v_dav:super
щонайвагоміший вагомий adj:m:v_zna:super
щонайвагомішого вагомий adj:m:v_zna:super
щонайвагомішим вагомий adj:m:v_oru:super
щонайвагомішім вагомий adj:m:v_mis:super
щонайвагомішому вагомий adj:m:v_mis:super
щонайвагоміша вагомий adj:f:v_naz:super
щонайвагомішої вагомий adj:f:v_rod:super
щонайвагомішій вагомий adj:f:v_dav:super
щонайвагомішу вагомий adj:f:v_zna:super
щонайвагомішою вагомий adj:f:v_oru:super
щонайвагомішій вагомий adj:f:v_mis:super
щонайвагоміше вагомий adj:n:v_naz:super
щонайвагомішого вагомий adj:n:v_rod:super
щонайвагомішому вагомий adj:n:v_dav:super
щонайвагоміше вагомий adj:n:v_zna:super
щонайвагомішим вагомий adj:n:v_oru:super
щонайвагомішім вагомий adj:n:v_mis:super
щонайвагомішому вагомий adj:n:v_mis:super
щонайвагоміші вагомий adj:p:v_naz:super
щонайвагоміших вагомий adj:p:v_rod:super
щонайвагомішим вагомий adj:p:v_dav:super
щонайвагоміших вагомий adj:p:v_zna:super
щонайвагоміші вагомий adj:p:v_zna:super
щонайвагомішими вагомий adj:p:v_oru:super
щонайвагоміших вагомий adj:p:v_mis:super
якнайвагоміший вагомий adj:m:v_naz:super
якнайвагомішого вагомий adj:m:v_rod:super
якнайвагомішому вагомий adj:m:v_dav:super
якнайвагоміший вагомий adj:m:v_zna:super
якнайвагомішого вагомий adj:m:v_zna:super
якнайвагомішим вагомий adj:m:v_oru:super
якнайвагомішім вагомий adj:m:v_mis:super
якнайвагомішому вагомий adj:m:v_mis:super
якнайвагоміша вагомий adj:f:v_naz:super
якнайвагомішої вагомий adj:f:v_rod:super
якнайвагомішій вагомий adj:f:v_dav:super
якнайвагомішу вагомий adj:f:v_zna:super
якнайвагомішою вагомий adj:f:v_oru:super
якнайвагомішій вагомий adj:f:v_mis:super
якнайвагоміше вагомий adj:n:v_naz:super
якнайвагомішого вагомий adj:n:v_rod:super
якнайвагомішому вагомий adj:n:v_dav:super
якнайвагоміше вагомий adj:n:v_zna:super
якнайвагомішим вагомий adj:n:v_oru:super
якнайвагомішім вагомий adj:n:v_mis:super
якнайвагомішому вагомий adj:n:v_mis:super
якнайвагоміші вагомий adj:p:v_naz:super
якнайвагоміших вагомий adj:p:v_rod:super
якнайвагомішим вагомий adj:p:v_dav:super
якнайвагоміших вагомий adj:p:v_zna:super
якнайвагоміші вагомий adj:p:v_zna:super
якнайвагомішими вагомий adj:p:v_oru:super
якнайвагоміших вагомий adj:p:v_mis:super
'''.trim()
	
//	@Test
//	@Ignore
//	void testMultiline() {
//		def lines = ["вагомий /adj \\", " +cs=вагоміший"]
//		assert expand.process_input(lines, false).join("\n") == multilineFull
//	}

def taggedIn = 
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
		assert expand.process_input(Arrays.asList(taggedIn), false).join("\n") == taggedOut
		
	}
	
}

//import static org.junit.Assert

package org.dict_uk.expand

import org.dict_uk.common.DicEntry
import org.junit.Before
import org.junit.Ignore;
import org.junit.Test;

public class ExpandTest extends GroovyTestCase {
	
	static Expand expand
	
	static
	{
		def args = ["-corp", "-aff", ".", "-dict", "."].toArray(new String[0])
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

	
	def strilyatyFull =
'''
стрілявши стрілявши advp:imperf
стріляти стріляти verb:imperf:inf
стріляй стріляти verb:imperf:impr:s:2
стріляймо стріляти verb:imperf:impr:p:1
стріляйте стріляти verb:imperf:impr:p:2
стріляю стріляти verb:imperf:pres:s:1
стріляєш стріляти verb:imperf:pres:s:2
стріляє стріляти verb:imperf:pres:s:3
стріляєм стріляти verb:imperf:pres:p:1
стріляємо стріляти verb:imperf:pres:p:1
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
		def lines = ["стріляти /v2.cf.isNo :imperf"]
		assert join(expand.process_input(lines)) == strilyatyFull
	}

	
	def stryvatyFull =
'''
стривати стривати verb:imperf:inf
стривай стривати verb:imperf:impr:s:2
стриваймо стривати verb:imperf:impr:p:1
стривайте стривати verb:imperf:impr:p:2
'''.trim()

	@Test
	void testStryvaty() {
		def lines = ["стривати /v2 tag=:impr|:inf :imperf"]
		assert join(expand.process_input(lines)) == stryvatyFull
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
		def lines = ["Аверянова /adj.<+"]
		assert join(expand.process_input(lines)) == adjLastName
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
		assert join(expand.process_input(lines)) == aFull
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
вагоміший вагоміший adj:m:v_naz:compr
вагомішого вагоміший adj:m:v_rod:compr
вагомішому вагоміший adj:m:v_dav:compr
вагомішого вагоміший adj:m:v_zna:ranim:compr
вагоміший вагоміший adj:m:v_zna:rinanim:compr
вагомішим вагоміший adj:m:v_oru:compr
вагомішім вагоміший adj:m:v_mis:compr
вагомішому вагоміший adj:m:v_mis:compr
вагоміший вагоміший adj:m:v_kly:compr
вагоміша вагоміший adj:f:v_naz:compr
вагомішої вагоміший adj:f:v_rod:compr
вагомішій вагоміший adj:f:v_dav:compr
вагомішу вагоміший adj:f:v_zna:compr
вагомішою вагоміший adj:f:v_oru:compr
вагомішій вагоміший adj:f:v_mis:compr
вагоміша вагоміший adj:f:v_kly:compr
вагоміше вагоміший adj:n:v_naz:compr
вагомішого вагоміший adj:n:v_rod:compr
вагомішому вагоміший adj:n:v_dav:compr
вагоміше вагоміший adj:n:v_zna:compr
вагомішим вагоміший adj:n:v_oru:compr
вагомішім вагоміший adj:n:v_mis:compr
вагомішому вагоміший adj:n:v_mis:compr
вагоміше вагоміший adj:n:v_kly:compr
вагоміші вагоміший adj:p:v_naz:compr
вагоміших вагоміший adj:p:v_rod:compr
вагомішим вагоміший adj:p:v_dav:compr
вагоміших вагоміший adj:p:v_zna:ranim:compr
вагоміші вагоміший adj:p:v_zna:rinanim:compr
вагомішими вагоміший adj:p:v_oru:compr
вагоміших вагоміший adj:p:v_mis:compr
вагоміші вагоміший adj:p:v_kly:compr
найвагоміший найвагоміший adj:m:v_naz:super
найвагомішого найвагоміший adj:m:v_rod:super
найвагомішому найвагоміший adj:m:v_dav:super
найвагомішого найвагоміший adj:m:v_zna:ranim:super
найвагоміший найвагоміший adj:m:v_zna:rinanim:super
найвагомішим найвагоміший adj:m:v_oru:super
найвагомішім найвагоміший adj:m:v_mis:super
найвагомішому найвагоміший adj:m:v_mis:super
найвагоміший найвагоміший adj:m:v_kly:super
найвагоміша найвагоміший adj:f:v_naz:super
найвагомішої найвагоміший adj:f:v_rod:super
найвагомішій найвагоміший adj:f:v_dav:super
найвагомішу найвагоміший adj:f:v_zna:super
найвагомішою найвагоміший adj:f:v_oru:super
найвагомішій найвагоміший adj:f:v_mis:super
найвагоміша найвагоміший adj:f:v_kly:super
найвагоміше найвагоміший adj:n:v_naz:super
найвагомішого найвагоміший adj:n:v_rod:super
найвагомішому найвагоміший adj:n:v_dav:super
найвагоміше найвагоміший adj:n:v_zna:super
найвагомішим найвагоміший adj:n:v_oru:super
найвагомішім найвагоміший adj:n:v_mis:super
найвагомішому найвагоміший adj:n:v_mis:super
найвагоміше найвагоміший adj:n:v_kly:super
найвагоміші найвагоміший adj:p:v_naz:super
найвагоміших найвагоміший adj:p:v_rod:super
найвагомішим найвагоміший adj:p:v_dav:super
найвагоміших найвагоміший adj:p:v_zna:ranim:super
найвагоміші найвагоміший adj:p:v_zna:rinanim:super
найвагомішими найвагоміший adj:p:v_oru:super
найвагоміших найвагоміший adj:p:v_mis:super
найвагоміші найвагоміший adj:p:v_kly:super
щонайвагоміший щонайвагоміший adj:m:v_naz:super
щонайвагомішого щонайвагоміший adj:m:v_rod:super
щонайвагомішому щонайвагоміший adj:m:v_dav:super
щонайвагомішого щонайвагоміший adj:m:v_zna:ranim:super
щонайвагоміший щонайвагоміший adj:m:v_zna:rinanim:super
щонайвагомішим щонайвагоміший adj:m:v_oru:super
щонайвагомішім щонайвагоміший adj:m:v_mis:super
щонайвагомішому щонайвагоміший adj:m:v_mis:super
щонайвагоміший щонайвагоміший adj:m:v_kly:super
щонайвагоміша щонайвагоміший adj:f:v_naz:super
щонайвагомішої щонайвагоміший adj:f:v_rod:super
щонайвагомішій щонайвагоміший adj:f:v_dav:super
щонайвагомішу щонайвагоміший adj:f:v_zna:super
щонайвагомішою щонайвагоміший adj:f:v_oru:super
щонайвагомішій щонайвагоміший adj:f:v_mis:super
щонайвагоміша щонайвагоміший adj:f:v_kly:super
щонайвагоміше щонайвагоміший adj:n:v_naz:super
щонайвагомішого щонайвагоміший adj:n:v_rod:super
щонайвагомішому щонайвагоміший adj:n:v_dav:super
щонайвагоміше щонайвагоміший adj:n:v_zna:super
щонайвагомішим щонайвагоміший adj:n:v_oru:super
щонайвагомішім щонайвагоміший adj:n:v_mis:super
щонайвагомішому щонайвагоміший adj:n:v_mis:super
щонайвагоміше щонайвагоміший adj:n:v_kly:super
щонайвагоміші щонайвагоміший adj:p:v_naz:super
щонайвагоміших щонайвагоміший adj:p:v_rod:super
щонайвагомішим щонайвагоміший adj:p:v_dav:super
щонайвагоміших щонайвагоміший adj:p:v_zna:ranim:super
щонайвагоміші щонайвагоміший adj:p:v_zna:rinanim:super
щонайвагомішими щонайвагоміший adj:p:v_oru:super
щонайвагоміших щонайвагоміший adj:p:v_mis:super
щонайвагоміші щонайвагоміший adj:p:v_kly:super
якнайвагоміший якнайвагоміший adj:m:v_naz:super
якнайвагомішого якнайвагоміший adj:m:v_rod:super
якнайвагомішому якнайвагоміший adj:m:v_dav:super
якнайвагомішого якнайвагоміший adj:m:v_zna:ranim:super
якнайвагоміший якнайвагоміший adj:m:v_zna:rinanim:super
якнайвагомішим якнайвагоміший adj:m:v_oru:super
якнайвагомішім якнайвагоміший adj:m:v_mis:super
якнайвагомішому якнайвагоміший adj:m:v_mis:super
якнайвагоміший якнайвагоміший adj:m:v_kly:super
якнайвагоміша якнайвагоміший adj:f:v_naz:super
якнайвагомішої якнайвагоміший adj:f:v_rod:super
якнайвагомішій якнайвагоміший adj:f:v_dav:super
якнайвагомішу якнайвагоміший adj:f:v_zna:super
якнайвагомішою якнайвагоміший adj:f:v_oru:super
якнайвагомішій якнайвагоміший adj:f:v_mis:super
якнайвагоміша якнайвагоміший adj:f:v_kly:super
якнайвагоміше якнайвагоміший adj:n:v_naz:super
якнайвагомішого якнайвагоміший adj:n:v_rod:super
якнайвагомішому якнайвагоміший adj:n:v_dav:super
якнайвагоміше якнайвагоміший adj:n:v_zna:super
якнайвагомішим якнайвагоміший adj:n:v_oru:super
якнайвагомішім якнайвагоміший adj:n:v_mis:super
якнайвагомішому якнайвагоміший adj:n:v_mis:super
якнайвагоміше якнайвагоміший adj:n:v_kly:super
якнайвагоміші якнайвагоміший adj:p:v_naz:super
якнайвагоміших якнайвагоміший adj:p:v_rod:super
якнайвагомішим якнайвагоміший adj:p:v_dav:super
якнайвагоміших якнайвагоміший adj:p:v_zna:ranim:super
якнайвагоміші якнайвагоміший adj:p:v_zna:rinanim:super
якнайвагомішими якнайвагоміший adj:p:v_oru:super
якнайвагоміших якнайвагоміший adj:p:v_mis:super
якнайвагоміші якнайвагоміший adj:p:v_kly:super
'''.trim()
	
	@Test
//	@Ignore
	void testMultiline() {
		def lines = ["вагомий /adj \\", " +cs=вагоміший"]
//		assert join(expand.process_input(lines)) == multilineFull
		assertEquals(multilineFull, join(expand.process_input(lines)))
	}

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
		assert join(expand.process_input(Arrays.asList(taggedIn))) == taggedOut
	}
	

	static final String join(def entries) {
		return entries.join("\n").replaceAll(/[<>]/, '')
	}
}


//import static org.junit.Assert

package org.dict_uk.expand

import org.dict_uk.common.DicEntry
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

public class OutputValidatorTest {
	
	static OutputValidator outputValidator = new OutputValidator()
	
	
	def portLines = 
'''
Адамишин noun:anim:m:v_naz:prop:lname
  Адамишина noun:anim:m:v_rod:prop:lname
  Адамишину noun:anim:m:v_dav:prop:lname
  Адамишина noun:anim:m:v_zna:prop:lname
  Адамишиним noun:anim:m:v_oru:prop:lname
  Адамишині noun:anim:m:v_mis:prop:lname
  Адамишину noun:anim:m:v_mis:prop:lname
  Адамишини noun:anim:p:v_naz:prop:lname
  Адамишиних noun:anim:p:v_rod:prop:lname
  Адамишиним noun:anim:p:v_dav:prop:lname
  Адамишиних noun:anim:p:v_zna:prop:lname
  Адамишиними noun:anim:p:v_oru:prop:lname
  Адамишиних noun:anim:p:v_mis:prop:lname
ч t
'''.trim().split("\n") as List
	
    @Disabled
	@Test
	void testValidatorNoun() {
		assert 0 == outputValidator.check_indented_lines(portLines, [])
	}

    def nounLines2 =
    '''
бабище noun:anim:f:v_naz
  бабищі noun:anim:f:v_rod
  бабищі noun:anim:f:v_dav
  бабище noun:anim:f:v_zna
  бабищею noun:anim:f:v_oru
  бабищі noun:anim:f:v_mis
  бабище noun:anim:f:v_kly
  бабище noun:anim:n:v_naz
  бабища noun:anim:n:v_rod
  бабищеві noun:anim:n:v_dav
  бабищу noun:anim:n:v_dav
  бабище noun:anim:n:v_zna
  бабищем noun:anim:n:v_oru
  бабищеві noun:anim:n:v_mis
  бабищі noun:anim:n:v_mis
  бабищу noun:anim:n:v_mis
  бабище noun:anim:n:v_kly
  бабища noun:anim:p:v_naz
  бабищі noun:anim:p:v_naz
  бабищ noun:anim:p:v_rod
  бабищам noun:anim:p:v_dav
  бабищ noun:anim:p:v_zna
  бабищами noun:anim:p:v_oru
  бабищах noun:anim:p:v_mis
  бабища noun:anim:p:v_kly
  бабищі noun:anim:p:v_kly
'''.trim().split("\n") as List
        
    @Disabled
    @Test
    void testValidatorNoun2() {
        assert 1 == outputValidator.check_indented_lines(nounLines2, [])
    }
    
	def verbLines = 
'''
бачити verb:imperf:inf
  бач verb:imperf:impr:2
  бачмо verb:imperf:impr:1
  бачте verb:imperf:impr:2:&insert
  бачу verb:imperf:pres:s:1:&insert
  бачиш verb:imperf:pres:s:2:&insert
  бачить verb:imperf:pres:s:3
  бачимо verb:imperf:pres:p:1
  бачим verb:imperf:pres:p:1:subst
  бачите verb:imperf:pres:p:2:&insert
  бачать verb:imperf:pres:p:3
  бачитиму verb:imperf:futr:1
  бачитимеш verb:imperf:futr:2
  бачитиме verb:imperf:futr:3
  бачитимем verb:imperf:futr:1
  бачитимемо verb:imperf:futr:1
  бачитимете verb:imperf:futr:2
  бачитимуть verb:imperf:futr:2
  бачив verb:imperf:past:m
  бачила verb:imperf:past:f
  бачило verb:imperf:past:n
  бачили verb:imperf:past:p
  бачено verb:imperf:impers
x t
'''.trim().split("\n") as List
	
	@Test
	void testValidatorVerb() {
		assert 0 == outputValidator.check_indented_lines(verbLines, [])
	}
}


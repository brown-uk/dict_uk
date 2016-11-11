package org.dict_uk.expand

import groovy.transform.Field
import groovy.transform.TypeChecked

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import java.util.regex.*


class BaseTags {
	static Logger log = LogManager.getFormatterLogger(BaseTags.class);

	static private final Pattern ending_i_nnia_re = Pattern.compile(/.*(([бвгджзклмнпрстфхцчшщ])\2|\'|[джлрт]|рн)я$/)

	static private final Util util = new Util()


	@TypeChecked
	String  get_base_tags(String word, String affixFlag, String allAffixFlags, String extra) {
		affixFlag = allAffixFlags

		def tag = ""

		if( affixFlag[0..<2] == "vr" )
			tag = ":rev:inf"
		else if( affixFlag[0] == "v" )
			tag = ":inf"

		if( tag )
			return tag

		def v_zna_for_inanim = ""

		if( ! util.istota(allAffixFlags) || util.bacteria(allAffixFlags) ) {
			v_zna_for_inanim = "/v_zna"
		}

		if( affixFlag.startsWith("adj") ) {

			if( word.endsWith("е") || word.endsWith("є") ) {
				tag = ":n:v_naz/v_zna/v_kly"
		    }
			else if( word.endsWith("і") ) {
				tag = ":p:v_naz/v_zn2/v_kly:ns"
			}
			else if( word.endsWith("а") || word.endsWith("я") ) {
				tag = ":f:v_naz/v_kly"
			}
			else if( word.endsWith("ій") || word.endsWith("їй") ) {
				if( affixFlag.startsWith("adj_pron") ) {
					tag = ":m:v_naz/v_zn2/v_kly"
				}
				else {
					tag = ":m:v_naz/v_zn2/v_kly//f:v_dav/v_mis"
				}
			}
			else {
				tag = ":m:v_naz/v_zn2/v_kly"
			}

			return tag
		}

		if( affixFlag == "numr" ) {
		    if( word.endsWith("ин") ) {
		        tag = ":m:v_naz/v_zn2"
		    }
		    else {
			    tag = ":p:v_naz/v_zna"
			}
			return tag
		}

		if( affixFlag.startsWith("n2n") ) {
		    if( affixFlag.startsWith("n2nm") ) { // сутяжище /n2nm.p.<
			    tag = ":m:v_naz" + v_zna_for_inanim
			    tag += "/v_kly"
		    }
		    else if( affixFlag.startsWith("n2nf") ) { // сутяжище /n2nf.p.<
			    tag = ":f:v_naz" + v_zna_for_inanim
			    tag += "/v_zna/v_kly"
		    }
		    else if( ending_i_nnia_re.matcher(word).matches() ) {
				tag = ":n:v_naz/v_rod/v_zna/v_kly//p:v_naz/v_kly"
			}
			else {
				tag = ":n:v_naz/v_zna/v_kly"
			}
		}
		else if( affixFlag.startsWith("np") ) {
			tag = ":p:v_naz/v_kly" // + v_zna_for_inanim
		}
		else if( affixFlag.startsWith("n2adj1") ) {
		    if( word.endsWith("е") || word.endsWith("є") ) {
			    tag = ":n:v_naz/v_kly" + v_zna_for_inanim
			}
		    else if( word.endsWith("а") || word.endsWith("я") ) {
			    tag = ":f:v_naz/v_kly"
			}
		    else if( word.endsWith("і") ) {
			    tag = ":p:v_naz/v_kly"
			}
			else {
    			tag = ":m:v_naz/v_kly" + v_zna_for_inanim
			}
	    }
		else if( affixFlag.startsWith("n2adj2") ) {
   			tag = ":m:v_naz" + v_zna_for_inanim
		}
		else if( affixFlag[0..<2] == "n2" ) {
			tag = ":m:v_naz" + v_zna_for_inanim
//			if( affixFlag.startsWith("n20") && util.person(allAffixFlags) && (word[-2..-1] == "ло") && ! allAffixFlags.contains(".k") ) {
//				tag += "/v_kly"
//			}
		}
		else if( affixFlag[0..<2] == "n1" ) {
			tag = ":f:v_naz"
		}
		else if( affixFlag[0..<2] == "n4" ) {
			tag = ":n:v_naz/v_zna/v_kly"
		}
		else if( affixFlag[0..<2] == "n3" ) {
			tag = ":f:v_naz/v_zna"
		}
		else
			assert "Unkown base for " + word + " " + allAffixFlags

		return tag

	}

}

package org.dict_uk.expand

import groovy.transform.Field
import groovy.transform.TypeChecked;

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import java.util.regex.*;

class BaseTags {
	static Logger log = LogManager.getFormatterLogger(BaseTags.class);

	static private final Pattern ending_i_nnia_re = Pattern.compile(/.*(([бвгджзклмнпрстфхцчшщ])\2|\'|[джлрт])я$/)

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
		def v_kly_for_anim = ""

		if( ! util.istota(allAffixFlags) ) {
			v_zna_for_inanim = "/v_zna"
		}
		else {
			if( ! allAffixFlags.contains(".ko") && ! allAffixFlags.contains(".ke") )
				v_kly_for_anim = "/v_kly"
		}

		//        if( affixFlag == "U" && "+" in allAffixFlags )
		//            tag = word + " " + word + " noun )m:v_naz"
		//        else if( affixFlag == "U" && word.endsWith("ий") )
		//            tag = word + " " + word + " adj )m:v_naz/v_zna )np"
		if( affixFlag.startsWith("adj") ) {
			if( word.endsWith("е") )
				tag = ":n:v_naz/v_zna"
			else if( word.endsWith("і") )
				tag = ":p:v_naz/v_zna:ns"
			else if( word.endsWith("а") ) {
				tag = ":f:v_naz"
				if( ! allAffixFlags.contains("<+") )
					tag += v_kly_for_anim
			}
			else if( word.endsWith("ій") ) {
				tag = ":m:v_naz/v_zna//f:v_dav/v_mis"
				if( util.istota(allAffixFlags) )
					tag = tag.replace(":m:v_naz/v_zna", ":m:v_naz")
				else
					tag = tag.replace("v_zna", "v_zn2")
			}
			else {
				if( util.istota(allAffixFlags) )
					tag = ":m:v_naz"
				else {
					tag = ":m:v_naz/v_zna"
					if( ! extra.contains("^noun") )
						tag = tag.replace("v_zna", "v_zn2")
				}
				//        if( "\\" in extra )
				//            tag += ":compb"
			}

			return tag
		}

		if( affixFlag == "numr" ) {
			tag = ":p:v_naz/v_zna"
			return tag
		}

		if( affixFlag.startsWith("n2n") ) {
			if( ending_i_nnia_re.matcher(word).matches() ) {
				tag = ":n:v_naz/v_rod/v_zna//p:v_naz"
			}
			else {
				tag = ":n:v_naz/v_zna"
				if( util.person(allAffixFlags) && word[-2..-1] in ["ще", "ко", "ло"] )// && not util.lastname(allAffixFlags) )
					tag += "/v_kly"
				//        if( affixFlag in "bfox":
			}
		}
		else if( affixFlag.startsWith("np") )
			tag = ":p:v_naz" // + v_zna_for_inanim + v_kly_for_anim
		else if( affixFlag.startsWith("n2adj1") && word.endsWith("е") )
			tag = ":n:v_naz" + v_zna_for_inanim
		else if( affixFlag.startsWith("n2adj") )
			tag = ":m:v_naz"
		else if( affixFlag[0..<2] == "n2" ) {
			tag = ":m:v_naz" + v_zna_for_inanim
			if( affixFlag.startsWith("n20") && util.person(allAffixFlags) && (word[-2..-1] == "ло") )// && not util.lastname(allAffixFlags) )
				tag += "/v_kly"
		}
		else if( affixFlag[0..<2] == "n1" )
			tag = ":f:v_naz"
		else if( affixFlag[0..<2] == "n4" )
			tag = ":n:v_naz/v_zna" + v_kly_for_anim
		else if( affixFlag[0..<2] == "n3" )
			tag = ":f:v_naz/v_zna"
		else
			//        tag = word + " " + word + " unknown"
			//        print(tag, "---", word, affixFlag)
			assert "Unkown base for " + word + " " + allAffixFlags

		return tag

	}

}

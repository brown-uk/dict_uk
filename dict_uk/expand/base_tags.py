#!/usr/bin/env python3

import sys
import re

import util


ending_i_nnia_re = re.compile(r".*(([бвгджзклмнпрстфхцчшщ])\2|\'|[джлрт])я$")

def get_base_tags(word, affixFlag, allAffixFlags, extra):
    affixFlag = allAffixFlags
#    print("**", word, allAffixFlags, file=sys.stderr)
    
    tag = ""

    if affixFlag[0:2] == "vr":
        tag = ":rev:inf"
    elif affixFlag[0] == "v":
        tag = ":inf"

    if tag:
        return tag

    v_zna_for_inanim = ""
    v_kly_for_anim = ""
    
    if not util.istota(allAffixFlags):
        v_zna_for_inanim = "/v_zna";
    else:
        if ".ko" not in allAffixFlags and ".ke" not in allAffixFlags:
            v_kly_for_anim = "/v_kly"


#        if affixFlag == "U" and "+" in allAffixFlags:
#            tag = word + " " + word + " noun:m:v_naz"
#        elif affixFlag == "U" and word.endswith("ий"):
#            tag = word + " " + word + " adj:m:v_naz/v_zna:np"
    if affixFlag.startswith("adj"):
        if word.endswith("е"):
            tag = ":n:v_naz/v_zna"
        elif word.endswith("і"):
            tag = ":p:v_naz/v_zna:ns"
        elif word.endswith("а"):
            tag = ":f:v_naz"
            if not "<+" in allAffixFlags:
                tag += v_kly_for_anim
        elif word.endswith("ій"):
            tag = ":m:v_naz/v_zna//f:v_dav/v_mis"
            if util.istota(allAffixFlags):
                tag = tag.replace(":m:v_naz/v_zna", ":m:v_naz")
        else:
            if util.istota(allAffixFlags):
                tag = ":m:v_naz"
            else:
                tag = ":m:v_naz/v_zna"
        
#        if "\\" in extra:
#            tag += ":compb"
        
        return tag
        
    
    if affixFlag == "numr":
        tag = ":p:v_naz/v_zna"
        return tag
        
    if affixFlag.startswith("n2n"):
        if ending_i_nnia_re.match(word):
            tag = ":n:v_naz/v_rod/v_zna//p:v_naz"
        else:
            tag = ":n:v_naz/v_zna"
            if util.person(allAffixFlags) and (word[-2:] in "ще", "ко", "ло"):# and not util.lastname(allAffixFlags):
                tag += "/v_kly"
#        if affixFlag in "bfox":
    elif affixFlag.startswith("np"):
        tag = ":p:v_naz" # + v_zna_for_inanim + v_kly_for_anim
    elif affixFlag.startswith("n2adj1") and word.endswith("е"):
        tag = ":n:v_naz" + v_zna_for_inanim
    elif affixFlag.startswith("n2adj"):
        tag = ":m:v_naz"
    elif affixFlag[:2] == "n2":
        tag = ":m:v_naz" + v_zna_for_inanim
        if affixFlag.startswith("n20") and util.person(allAffixFlags) and (word[-2:] in "ло"):# and not util.lastname(allAffixFlags):
                tag += "/v_kly"
    elif affixFlag[:2] == "n1":
        tag = ":f:v_naz"
    elif affixFlag[:2] == "n4":
        tag = ":n:v_naz/v_zna" + v_kly_for_anim
    elif affixFlag[:2] == "n3":
        tag = ":f:v_naz/v_zna"

    else:
#        tag = word + " " + word + " unknown"
#        print(tag, "---", word, affixFlag)
        raise Exception("Unkown base for " + word + " " + allAffixFlags)

    return tag


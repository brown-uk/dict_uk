#!/usr/bin/python3

# -*- coding: utf-8 -*-
# This script loads hunspell-like affixes and allows to perform some actions

import sys
import re
import logging
import locale
import collections
import time

import affix
from affix import SuffixGroup
from affix import Suffix
from affix import affixMap
import base_tags

from util import re_sub
from util import re_search
from util import re_match
import util



logging.basicConfig(filename="expand.log", level=logging.DEBUG, filemode="w")
logger = logging.getLogger("expand")
#logger.setLevel(logging.DEBUG)

locale.setlocale(locale.LC_ALL, "uk_UA")

DERIV_PADDING="  "



#@profile
def adjustCommonFlag(affixFlag2):
    if ".cf" in affixFlag2:
        affixFlag2 = re_sub("(vr?)[1-4]\.cf", "\\1.cf", affixFlag2) # v5.cf is special
    if ".impers" in affixFlag2:
        affixFlag2 = re_sub("(vr?)[1-9]\.impers", "\\1.impers", affixFlag2)
    if ".patr" in affixFlag2:
        affixFlag2 = re_sub("n[0-9]+\.patr", "n.patr", affixFlag2)
    return affixFlag2


#@profile
def expand_suffixes(word, affixFlags, modifiers, extra):

    affixSubGroups = affixFlags.split(".")
    mainGroup = affixSubGroups[0]
    
    pos = util.get_pos(mainGroup, modifiers)
    base_tag = base_tags.get_base_tags(word, "", affixFlags, extra)
    #2      base_word = word + " " + pos + base_tag
    base_word = word + " " + word + " " + pos + base_tag
    words = [ base_word ]
    
    if affixFlags[0] == "<":
        return words
    
    
    appliedCnt = 0
    appliedCnts = {}
    affixFlags2 = []
    
    for affixFlag2 in affixSubGroups:
      if "<" in affixFlag2 or affixFlag2 == "@":
        continue
    
      if affixFlag2 != mainGroup:
          if affixFlag2 not in ("v2", "vr2"):  # курликати /v1.v2.cf       задихатися /vr1.vr2
              affixFlag2 = mainGroup + "." + affixFlag2
              if affixFlag2 == "v3.advp":
                  affixFlag2 = "v1.advp"
              elif affixFlag2 == "v3.imprt0":
                  affixFlag2 = "v1.imprt0"
           
    
          affixFlag2 = adjustCommonFlag(affixFlag2)
    
    
      appliedCnts[affixFlag2] = 0
      
      if not affixFlag2 in affixMap:
         raise Exception("could not find affix flag " + affixFlag2)
    
    
      affixGroupMap = affixMap[affixFlag2]
    
      for match, affixGroup in affixGroupMap.items():
        if affixGroup.matches(word):
    
            for affix in affixGroup.affixes:
              # DL - не додавати незавершену форму дієприслівника для завершеної форми дієслова
                if pos.startswith("verb") and ":perf" in extra and (affix.tags.startswith("advp:imperf") or affix.tags.startswith("advp:rev:imperf")):
                    appliedCnts[ affixFlag2 ] = 1000
                    continue
                
                deriv = affix.apply(word)
                tags = affix.tags
                
                if affixFlag2 == "n.patr":
                    tags += ":patr"
                  
                #2              words.append(DERIV_PADDING + deriv + " " + tags)
                words.append(deriv + " " + word + " " + tags)
                appliedCnt += 1
                appliedCnts[affixFlag2] += 1
            
              #logger.debug("applied %s to %s", affixGroup, word)
            
            affixGroup.counter += 1
      
    #      print("DEBUG: applied", affixFlags, "for", word, "got", appliedCnts, file=sys.stderr)
    #      logger.debug("applied %s for %s got %s", affixFlags, word, str(appliedCnts))

#    if appliedCnt == 0: # or appliedCnts[ affixFlag ] == 0:
      if appliedCnts[ affixFlag2 ] == 0:
            #print("ERROR: Flag", affixFlag2, "not applicable to \"" + word + "\"", file=sys.stderr)
            raise Exception("FATAL: Flag " + affixFlag2 + " of " + affixFlags + " not applicable to " + word)
    if True:
        dups = [x for x, y in collections.Counter(words).items() if y > 1]
        if len(dups) > 0:
            print("WARNING: duplicates:", dups, file=sys.stderr)
    
    return words


#@profile
def get_modifiers(mod_flags, flags, word):
#    if not "^" in mod_flags and "/adj" in flags and "<" in flags:
#        mod_flags = "^noun " + mod_flags

    mods = {}

    if "/adj" in flags and "<" in flags:
        mods["pos"] = "noun"
        
        if not "=" in mod_flags:
            if "<+" in flags:
                if word.endswith("а"):
                    mods["gen"] = "f"
                else:
                    mods["gen"] = "mfp"
                return mods
                
            if "<" in flags:
                if word.endswith("а"):
                    mods["gen"] = "fp"
                else:
                    mods["gen"] = "mp"
                return mods
    
        if not "=" in mod_flags:
            mods["gen"] = "mfp"
            return mods

    
    mod_set = mod_flags.split()
    
    for mod in mod_set:
      if mod[0] == "^":
          if mod.startswith("^adjp"):
            mods["pos"] = mod[1:]
          else:
            mod_tags = mod[1:].split(":")
            mods["pos"] = mod_tags[0]
            if len(mod_tags) > 1 and mod_tags[0] == "noun":
                if len(mod_tags[1]) != 1:
                    raise Exception("Bad gender override: " + str(mod) + " -- " + str(mod_tags))
                mods["force_gen"] = mod_tags[1]
                 
        #else:
         #     mods["pos"] = POS_MAP[ mod[1:2] ]
      elif mod[:2] == "g=":
        mods["gen"] = re_sub("g=([^ ])", "\\1", mod)    #mod[2:3]
      elif mod[:2] == "p=":
        mods["pers"] = mod[2:3]
      elif mod.startswith("tag="):
        mods["tag"] = mod[4:]
      
    if "<+m" in flags or "<m" in flags:
        mods["force_gen"] = "m"
        if "n2adj" in flags:
            mods["gen"] = "m"
    

#    logger.debug("mods %s for %s and %s", str(mods), flags, mod_flags)
    
    return mods



def filter_word(w, modifiers):
    if "gen" in modifiers:
#        util.dbg("filter by gen", modifiers, w)
        if not re_search(":[" + modifiers["gen"] + "]:", w):
            return False
        
    if "pers" in modifiers and not re.search(":(inf|past)", w):
        if not re_search(":[" + modifiers["pers"] + "]", w):
            return False

    if "tag" in modifiers:
        if not re.search(modifiers["tag"], w):
            return False

    return True


#@profile
def modify(lines, modifiers):
#    util.dbg("mods", modifiers)
    if len(modifiers) == 0:
        return lines
    
    out = []
    for line in lines:

        if not filter_word(line, modifiers):
            logger.debug("skip %s %s", line, modifiers)
            continue
          
        if "pos" in modifiers:
            line = re_sub(" [^ :]+:", " " + modifiers["pos"] + ":", line)
#            logger.debug("pos repl %s in %s", modifiers["pos"], line)
      
        if "force_gen" in modifiers and not ":patr" in line:
            force_gen = modifiers["force_gen"]
            line = re_sub(":[mfn](:|$)",  ":" + force_gen + "\\1", line)
            logger.debug("gen repl: %s in %s", force_gen, line)
            
    
        out.append(line)
    

    if len(out) == 0:
        raise Exception("emtpy output for "+ str(lines) + " and " + str(modifiers))

    return out


def get_extra_flags(flags):
    extra_flags = ""
    if " :" in flags:
        extra_flags = re_search(" (:[^ ]+)", flags).group(1)
    if "<" in flags or "patr" in flags:
        extra_flags += ":anim"
    if "<+" in flags:
        extra_flags += ":lname"
    
    return extra_flags


def post_expand(lines, flags):
    if len(lines) == 0:
        raise Exception("emtpy lines")

    extra_flags = get_extra_flags(flags)
    
    
    if extra_flags:
        first_name_base = util.firstname(lines[0], flags)
        
        out_lines = []
        extra_out_lines = []
        
        for line in lines:
            extra_flags2 = extra_flags
    
            if first_name_base and not ":patr" in line:
                extra_flags2 += ":fname"
    
            if " advp" in line:
                if ":imperf" in line:
                    extra_flags2 = re_sub(":(im)?perf", "", extra_flags2)
                else:
                    line = line.replace(":perf", "")
            elif "adj.adv" in flags and " adv" in line:
                extra_flags2 = re_sub(r":&?adjp(:pasv|:actv|:pres|:past|:perf|:imperf)+", "", extra_flags2)
            elif ":+m" in extra_flags:
                extra_flags2 = extra_flags2.replace(":+m", "")
                
                if ":f:" in line:
                    masc_line = line.replace(":f:", ":m:") + extra_flags2
                    extra_out_lines.append(masc_line)
                elif ":n:" in line:
                    masc_line = line.replace(":n:", ":m:") + extra_flags2
                    
                    if util.istota(flags):
                        if "m:v_rod" in masc_line:
                            masc_line2 = masc_line.replace("m:v_rod", "m:v_zna")
                            extra_out_lines.append(masc_line2)
                        elif "m:v_zna" in masc_line:
                            masc_line = ""
                        if "m:v_kly" in masc_line:
                            word, lemma, tags = masc_line.split()
                            masc_line = word[:-1]+"е " + lemma + " " + tags
                    
                    if masc_line:
                        extra_out_lines.append(masc_line)
            elif ":+f" in extra_flags:
                extra_flags2 = extra_flags2.replace(":+f", "")
                
                if ":m:" in line:
                    masc_line = line.replace(":m:", ":f:") + extra_flags2
                    extra_out_lines.append(masc_line)
                elif ":n:" in line:
                    masc_line = line.replace(":n:", ":f:") + extra_flags2
                    
#                     if util.istota(flags):
#                         if "m:v_rod" in masc_line:
#                             masc_line2 = masc_line.replace("m:v_rod", "m:v_zna")
#                             extra_out_lines.append(masc_line2)
#                         elif "m:v_zna" in masc_line:
#                             masc_line = ""
                    
                    if masc_line:
                        extra_out_lines.append(masc_line)
            elif ":patr" in line and ":anim" in extra_flags2:
                line = line.replace(":patr", ":anim:patr")
                extra_flags2 = extra_flags2.replace(":anim", "")
    
            out_lines.append(line + extra_flags2)
    
        out_lines.extend(extra_out_lines)
        
        return out_lines
    
    return lines


def adjust_affix_tags(lines, main_flag, flags, modifiers):
    lines2 = []
  
    for line in lines:
        # DL-
        if main_flag[1] == "n":
                
            if main_flag.startswith("/n2") and re_search("^/n2[01234]", main_flag):
#                base_word = lines[0].split()[0]
                base_word = line.split()[1]
                
                if util.istota(flags):
                    if "m:v_rod" in line and not "/v_zna" in line:
                        line = line.replace("m:v_rod", "m:v_rod/v_zna")
        
                if not base_word[-1:] in "аеєиіїоюя" and not ".a" in flags:
#                    util.dbg("```", main_flag, line)
                    word = line.split()[0]
                    if word[-1:] in "ую":
                        logger.debug("u/rod %s - %s", line, base_word)
                        line = line.replace("v_dav", "v_rod/v_dav")
                        
            if main_flag.startswith("/n2") and "@" in flags:
                word = line.split(" ", 1)[0]
                if word[-1:] in "ая" and "m:v_rod" in line:
                    line = line.replace("m:v_rod", "m:v_rod/v_zna")
        
            if not "np" in main_flag and not ".p" in main_flag and not "n2adj" in flags:
                if ":p:" in line:
                    logger.debug("skipping line with p: " + line)
                elif "//p:" in line:
                    line = re_sub("//p:.*", "", line)
                    logger.debug("removing //p from: " + line)
        
            if "/v_kly" in line:
                if main_flag.startswith("/n1"): # Єремія /n10.ko.patr.<
                    base_word = line.split()[1]

                if ("<+" in flags and not ":p:" in line) or not util.person(flags) \
                        or (not ":patr" in line and re_search("\\.k[eo]", flags)) \
                        or (":m:" in line and ("<+" in flags)) \
                        or (main_flag.startswith("/n20") and base_word.endswith("ло") and "v_dav" in line):
                    logger.debug("removing v_kly from: %s, %s", line, flags)
                    line = line.replace("/v_kly", "")

            if ".p" in main_flag or "np" in main_flag:
                if util.person(flags):
                    line = line.replace("p:v_naz", "p:v_naz/v_kly")
    
                if util.istota(flags):
                    line = line.replace("p:v_rod", "p:v_rod/v_zna")
                    if ">" in flags: # animal
                        line = line.replace("p:v_naz", "p:v_naz/v_zna")
                else:
                    line = line.replace("p:v_naz", "p:v_naz/v_zna")

            
        elif ":perf" in flags and ":pres" in line:
            line = line.replace(":pres", ":futr")
        elif main_flag.startswith("/adj"):
            if "<" in flags:
                if not ">" in flags and ":p:v_naz/v_zna" in line:
                    line = line.replace("v_naz/v_zna", "v_naz/v_kly")
                if ":m:v_naz" in line and not "<+" in flags:
                    line = line.replace("v_naz", "v_naz/v_kly")
            elif "^noun" in flags:
                if ":m:v_rod/v_zna" in line:
                    line = line.replace("v_rod/v_zna", "v_rod")
                elif ":p:v_rod/v_zna" in line:
                    line = line.replace("v_rod/v_zna", "v_rod")
    
#            if "<" in flags:
#                if util.person(flags):
#                    line = line.replace("p:v_naz", "p:v_naz/v_kly")
#    
#                if util.istota(flags):
#                    line = line.replace("p:v_rod", "p:v_rod/v_zna")
#                    if ">" in flags: # animal
#                        line = line.replace("p:v_naz", "p:v_naz/v_zna")
#                else:
#                    line = line.replace("p:v_naz", "p:v_naz/v_zna")

        lines2.append(line)

    return lines2


#@profile
def expand(word, flags, flush_stdout):

    flag_set = flags.split(" ", 1)
    main_flag = flag_set[0]
  
    if len(flag_set) > 1:
        extra = flag_set[1]
    else:
        extra = ""

    modifiers = get_modifiers(extra, flags, word)

    if main_flag[0] == "/":
        inflection_flag = main_flag[1:]
        sfx_lines = expand_suffixes(word, inflection_flag, modifiers, extra)
        sfx_lines = adjust_affix_tags(sfx_lines, main_flag, flags, modifiers)
    else:
        sfx_lines = [word + " " + word + " " + flags]


    sfx_lines = affix.expand_alts(sfx_lines, "//")  # TODO: change this to some single-char splitter?
    sfx_lines = affix.expand_alts(sfx_lines, "/")

    if main_flag[0] != "/":
        sfx_lines = util.expand_nv(sfx_lines)

    sfx_lines = modify(sfx_lines, modifiers)


    if "\\" in flags:
        for i in range(0, len(sfx_lines)):
            sfx_lines[i] = sfx_lines[i] + ":compb"

    words = post_expand(sfx_lines, flags)

    return words


tag_split0_re = re.compile("[^ ]+$")

def preprocess(line):
    if line.count(" /") > 1:
        parts = line.split(" ")
        line1 = parts[:2] + parts[3:]
        line2 = parts[:1] + parts[2:]
        lines = [" ".join(line1), " ".join(line2)]
    else:
        lines = affix.expand_alts([line], "|")
    
    
    
    out_lines = []
    for line in lines:
        out_lines.extend(preprocess2(line))
    
    return out_lines

def preprocess2(line):
    out_lines = []

    if "/v-u" in line or ".v-u" in line:
        if "/v-u" in line:
            line = re_sub(r"(?i)^([а-яіїєґ\"-]+) /v-u ?\^?", "\\1 ", line).replace(" :", ":")
        else:
            line = re_sub("\.v-u", "", line)
            
        space = " "
        if " :" in line or not " /" in line:
            space = ""
        line = line + space + ":v-u"
        line1 = re_sub("(^| )в", "\\1у", line)
        out_lines = [line, line1]
        logger.debug("v-u: " + str(out_lines))

    elif "/<" in line:
        if "<+" in line:
            extra_tag = ":anim:lname"
        else:
            extra_tag = ":anim:fname"
        
        if not "<m" in line and not "<+m" in line:
#            tag = "noun:f:v_naz/v_rod/v_dav/v_zna/v_oru/v_mis/k_kly"
            tag = "noun:f:nv:np"
            line1 = re_sub("/<\\+?f?", tag + extra_tag, line)
            out_lines.append(line1)
        if not "<f" in line and not "<+f" in line:
            tag = "noun:m:nv:np"
            line1 = re_sub("/<\\+?m?", tag + extra_tag, line)
            out_lines.append(line1)

    elif "/n2" in line and "<+" in line:
        if not "<+m" in line and util.dual_last_name_ending(line):
            out_lines.append(line)
            line_fem_lastname = line.split()[0] + " noun:f:nv:np:anim:lname"
            out_lines.append(line_fem_lastname)
        else:
            out_lines = [line]
    elif "/n1" in line and "<+" in line:
        if not "<+f" in line and not "<+m" in line:
            out_lines.append(line)
            line_masc_lastname = line.replace("<+", "<+m")
            out_lines.append(line_masc_lastname)
        else:
            out_lines = [line]
    elif "/np" in line:
        space = " "
        if " :" in line or not " /" in line:
            space = ""
        line = line + space + ":ns"
        out_lines = [line]
    elif ":imperf:perf" in line:
        line1 = line.replace(":perf", "")
        line2 = line.replace(":imperf", "").replace(".cf", "")  #.replace(".advp")  # so we don"t get two identical advp:perf lines
        out_lines = [line1, line2]
    elif ":&adj" in line and not " :&adj" in line:
        line = line.replace(":&adj", " :&adj")
        out_lines = [line]
    else:
        out_lines = [line]

#     out_lines2 = []
#     for out_line in out_lines:
#         
#         if ":+f" in out_line:
#             out_line = out_line.replace(":f", "")
#             f_line = out_line + ""
#             out_lines2.append(f_line)
# 
#         out_lines2.append(out_line)
#    print("--", "\n++ ".join(out_lines), file=sys.stderr)  
    return out_lines

def post_process_sorted(lines):
    out_lines = []
    
#    print("\n".join(lines), file=sys.stderr)
    
    prev_line = ""
    for line in lines:
        if "patr" in line:
            if re_search(":[mf]:v_naz:.*patr", line):
                logger.debug("promoting patr lemma %s", line)
                last_lema = line.split()[0]
            line = replace_base(line, last_lema)
        elif "lname" in line and ":f:" in line and not ":nv" in line:
            if ":f:v_naz" in line:
                logger.debug("promoting f lname lemma %s", line)
                last_lema = line.split()[0]
            line = replace_base(line, last_lema)
#        elif " adv" in line and not " advp" in line and not ":combr" in line and not ":super" in line:
#            logger.debug("promoting adv lemma %s", line)
#            line = replace_base(line, line.split()[0])

        if prev_line == line and ("advp:perf" in line or "advp:rev:perf" in line):
            continue
        
        prev_line = line
        out_lines.append(line)

    return out_lines



def promote(line):
    lemma = line.split(maxsplit=1)[0]
#    logger.debug("promote %s -> %s", line, lemma)
    line = replace_base(line, lemma)
    return line

#@profile
def post_process(lines):
    out_lines = []
        
    for line in lines:

        if " adv" in line and not "advp" in line and not ":compr" in line and not ":super" in line:
            line = promote(line)

        if "-corp" in sys.argv:
            if "advp" in line:
                line = promote(line)
            elif "noun" in line:
                if ":anim" in line:
                    line = line.replace(":anim", "").replace("noun:", "noun:anim:")
                elif ":inanim" in line:
                    line = line.replace(":inanim", "").replace("noun:", "noun:inanim:")
                elif not "&pron" in line:
                    line = line.replace("noun:", "noun:inanim:")
            elif "verb" in line:
                line = re_sub("(verb(?::rev)?)(.*)(:(im)?perf)", "\\1\\3\\2", line)
            elif "adj" in line:
                if ":comp" in line or ":super" in line:
                    line = re_sub(" (adj:)(.*):(comp[br]|super)(.*)", " \\1\\3:\\2\\4", line)

                if ":&adjp" in line:
                    adjp_line = re.sub(" (adj(?::compb|:compr|:super)?)(.*):&(adjp(?::pasv|:actv|:past|:pres|:perf|:imperf)+)(.*)", " \\3\\2\\4", line)
                    out_lines.append(adjp_line)

                    line = re.sub(":&adjp(:pasv|:actv|:past|:pres|:perf|:imperf)+", "", line)
#                    util.dbg("-1-", line)

#            if "advp" in line:
#                line = re_sub("(.*) .* (advp.*)", "\\1 \\1 \\2", line)
        else:
            if ":&adjp" in line and ":comp" in line:
              #  if ":comp" in line or ":super" in line:
                line = re_sub(" (adj:.:v_...:)(.*):(comp[br]|super)(.*)", " \\1\\3:\\2\\4", line)

         #   out_lines.append(line)

# TODO: extra :coll
#            if "сь advp" in line:
#                other_line = re_sub("(.*)сь (.*сь) (advp.*)", "\\1ся \\2 \\3:coll", line)
#                out_lines.append(other_line)
#
#            if "verb:" in line and ":inf" in line and ("ти " in line): # or "тися " in line):
#                other_line = re_sub("^(.*)ти((?:ся)? [^ ]+) (verb:.*)", "\\1ть\\2 \\3:coll", line)
#                out_lines.append(other_line)
        #else:
        out_lines.append(line)

    out_lines = [ util.tail_tag(line, (":v-u", ":bad", ":slang", ":rare", ":coll", ":abbr")) for line in out_lines ]    # TODO: add ":alt"

    return out_lines


main_word=""
main_flag=""

def replace_base(line, base):
    ws = line.split()
    return ws[0] + " " + base + " " + ws[2]


def expand_subposition(main_word, line, extra_tags, idx):
    idx = ":xs" + str(idx)
    logger.debug("expanding sub " + idx + " " + main_word + ": " + line)
    if line.startswith(" +cs"):
        if " +cs=" in line:
            word = re_match(" \\+cs=([^ ]+)", line).group(1)
        else:
            word = main_word[:-2] + "іший"
            
        if "&adjp" in extra_tags:
            extra_tags = re_sub(r":&adjp(:pasv|:actv|:pres|:past|:perf|:imperf)+", "", extra_tags)


        word_forms = expand(word, "/adj :compr" + idx + extra_tags, flush_stdout)
#        word_forms[0] = DERIV_PADDING + word_forms[0]
        
        word = "най" + word
        word_forms_super = expand(word, "/adj :super" + idx + extra_tags, flush_stdout)
        word_forms.extend(word_forms_super)

        word_scho = "що" + word
        word_forms_super = expand(word_scho, "/adj :super" + idx + extra_tags, flush_stdout)
        word_forms.extend(word_forms_super)

        word_jak = "як" + word
        word_forms_super = expand(word_jak, "/adj :super" + idx + extra_tags, flush_stdout)
        word_forms.extend(word_forms_super)

        word_forms = [ replace_base(line, main_word) for line in word_forms ]
        
        return word_forms
 
    raise "Unknown subposition for " + line + "(" + main_word + ")"


def expand_subposition_adv_main(main_word, line, extra_tags):
    logger.debug("expanding sub " + main_word + ": " + line + " extra tags: " + extra_tags)
    if line.startswith(" +cs"):
        if " +cs=" in line:
            word = re_match(" \\+cs=([^ ]+)", line).group(1)
        else:
            word = main_word[:-1] + "іше"
            

        adv_compr = word + " "  + main_word + " adv:compr" + extra_tags
        adv_super = "най" + word + " "  + main_word + " adv:super" + extra_tags
        adv_super2 = "щонай" + word + " "  + main_word + " adv:super" + extra_tags
        adv_super3 = "якнай" + word + " "  + main_word + " adv:super" + extra_tags

        return [adv_compr, adv_super, adv_super2, adv_super3]
 
    raise "Unknown subposition for " + line + "(" + main_word + ")"


def expand_subposition_adv(last_adv, line, extra_tags):
    print("+.adv", last_adv, file=sys.stderr)
    
    out_lines = []
    
    if " +cs=" in line:
        word = re_match(r" \+cs=([^ ]+)", line).group(1)
        word = word[:-2] + "е"
    else:
        word = main_word[:-2] + "е"

    if "adjp" in extra_tags:    
        extra_tags = re_sub(r":&?adjp(:pasv|:actv|:pres|:past|:perf|:imperf)+", "", extra_tags)

    
    w1 = word + " " + last_adv + " adv:compr" + extra_tags
    out_lines.append( w1 )
    
    adv_super = "най" + word + " " + last_adv + " adv:super" + extra_tags
    adv_super2 = "щонай" + word + " "  + last_adv + " adv:super" + extra_tags
    adv_super3 = "якнай" + word + " "  + last_adv + " adv:super" + extra_tags
    out_lines.extend( (adv_super, adv_super2, adv_super3) )
    
    print("...", w1, adv_super, file=sys.stderr)
    
    return out_lines



word_lemma_re = re.compile(" [а-яіїєґ]", re.IGNORECASE)


#@profile
def expand_line(line, flush_stdout):
    global main_word
    global main_flag
    global last_adv

    if "-d" in sys.argv:
        print("@", line)

    lines = preprocess(line)
    
    out_lines = []

    for line in lines:
        sub_lines = []
        
        #  +cs
        if "\\ +" in line:
            
            line, *sub_lines = line.split("\\")
            line = line.rstrip()
            if " :" in line or not " /" in line:
                line += ":compb"
            else:
                line += " :compb"

#            print(" \\+", line, file=sys.stderr)
                    
#            main_word = line
#            sublines = expand_subposition(main_word, line)
#            out_lines.extend( sublines )
            
        # word lemma tags
        elif word_lemma_re.search(line):
            if "/" in line:
                exp_lines = affix.expand_alts([line], "//")  # TODO: change this to some single-char splitter?
                try:
                  exp_lines = affix.expand_alts(exp_lines, "/")
                except:
                  print("Failed to expand", exp_lines, file=sys.stderr)
                  raise
            else:
                exp_lines = [ line ]

            if ":nv" in line and not "v_" in line:
                exp_lines = util.expand_nv(exp_lines)
                
            out_lines.extend( exp_lines )
            
            continue
        
        # word tags
        # word /flags [mods] [tags]
        try:
            word, flags = line.split(" ", 1)
        except:
            print("Failed to find flags in", line, file=sys.stderr)
            raise
          
        main_word = word
        
        inflected_lines = expand(word, flags, flush_stdout)
        
        if sub_lines:
            idx = 0
            for sub_line in sub_lines:
                if flags.startswith("adv:"):
                    extra_flags = flags[3:].replace(":compb", "")
    #                util.dbg("sub_lines: %s, %s", flags, extra_flags)
                elif " :" in flags or flags.startswith(":"):
                    extra_flags = re_search("(^| )(:[^ ]+)", flags).group(2).replace(":compb", "")
    #                 util.dbg("===", extra_flags)
                else:
                    extra_flags = ""
            
                if " adv" in line:
                    sublines = expand_subposition_adv_main(main_word, sub_line, extra_flags)
                else:
                    sublines = expand_subposition(main_word, sub_line, extra_flags, idx)
                    
                out_lines.extend( sublines )
            
                if ".adv" in line and "/adj" in line:
                    for i, inflected_line in enumerate(inflected_lines):
                        if " adv" in inflected_line:
                            last_adv = inflected_line.split()[0]
                            cs_lines = expand_subposition_adv(last_adv, sub_line, extra_flags)
                            out_lines.extend(cs_lines)
                            break
                    print(".adv", last_adv, file=sys.stderr)

                idx += 1
        
        out_lines.extend( inflected_lines )
        
        for l in inflected_lines:
            if not l.strip():
                raise Exception("empty liner", inflected_lines)

    return post_process(out_lines)


def cleanup(line):
    return re_sub(":xs.", "", line)


def log_usage():
    with open("usage.txt", "w") as f:
        for affixFlag, affixGroups in affixMap.items():
            print("Flag", affixFlag, "has", len(affixGroups), "groups", file=f)
            for match, affixGroup in affixGroups.items():
                print("\t", match, ":", affixGroup.counter, "\t\t", len(affixGroup.affixes), "patterns", file=f)




#----------
# main code
#----------
if __name__ == "__main__":

    flush_stdout = False
    mode = "expand"
    arg_idx = 1
    
    if len(sys.argv) > arg_idx and sys.argv[arg_idx] in ["-f", "--flush"]:
        flush_stdout=True
        arg_idx += 1
    
    aff_arg_idx = sys.argv.index("-aff") if "-aff" in sys.argv else -1
    if aff_arg_idx != -1:
        affix_filename = sys.argv[aff_arg_idx+1]
    else:
    #    affix_filename = os.path.dirname(os.path.abspath(__file__)) + "/affix/all.aff"
        affix_filename = "affix/all.aff"
    
    affix.load_affixes(affix_filename)
    
    time1 = time.time()
    
    multiline = ""
    all_lines = []
    for line in sys.stdin:
        if "#" in line:
            line = line.split("#")[0]
          
        if not line.strip():
            continue
          
        line = line.rstrip()
        
        if line.endswith("\\"):
            multiline += line  #.replace("\\", "")
            continue
        else:
            if multiline:
                line = multiline + line
            multiline = ""
        
        try:
            tag_lines = expand_line(line, flush_stdout)
        except:
            print("Exception in line: "" + line + """, file=sys.stderr)
            raise
        
        
        if flush_stdout:
            sorted_lines = util.sort_all_lines(tag_lines)
            print("\n".join(sorted_lines))
            sys.stdout.flush()
        else:
            all_lines.extend(tag_lines)



    time2 = time.time()
    print("Total lines", len(all_lines), file=sys.stderr)
    print("Processing time: {0:.3f}".format(time2-time1), file=sys.stderr)
    
    
    if not flush_stdout:
    #        print("\n".join(line for line in all_lines if "adv:" in line), file=sys.stderr)
#        print("sort 1", file=sys.stderr)
        sorted_lines = util.sort_all_lines(all_lines)
    #        print("\n".join(line for line in sorted_lines if "adv:" in line), file=sys.stderr)
        sorted_lines = post_process_sorted(sorted_lines)



        time3 = time.time()
        print("Sorting time 1: {0:.3f}".format(time3-time2), file=sys.stderr)
    
        if "-indent" in sys.argv:
            # to sort newely promoted lemmas
            sorted_lines = util.sort_all_lines(list(set(sorted_lines)))

            time4 = time.time()
            print("Sorting time 2: {0:.3f}".format(time4-time3), file=sys.stderr)
          
    #        print ("\n-- ".join( ln for ln in sorted_lines if ln.startswith("Венед") ), file=sys.stderr)

        sorted_lines = [ cleanup(line) for line in sorted_lines ]

        if "-indent" in sys.argv:
            if "-mfl" in sys.argv:
                with open("dict_corp_lt.txt", "w") as f:
                    f.write("\n".join(sorted_lines))
        
            sorted_lines = util.indent_lines(sorted_lines)
          
        print("\n".join(sorted_lines))
    
    if "--log-usage" in sys.argv:
        log_usage()


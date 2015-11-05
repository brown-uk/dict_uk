#!/usr/bin/python3

import re
import sys
import locale
import collections

# TODO: this should be set by the caller
locale.setlocale(locale.LC_ALL, "uk_UA")

DERIV_PADDING="  "


# generic util methods

def dbg(*args):
    print("---", args, file=sys.stderr)


regex_map = {}

def re_sub(regex, repl, txt):
    if not regex in regex_map:
        regex_map[regex] = re.compile(regex)
      
    return regex_map[regex].sub(repl, txt)


def re_search(regex, txt):
    if not regex in regex_map:
        regex_map[regex] = re.compile(regex)
      
    return regex_map[regex].search(txt)


def re_match(regex, txt):
    if not regex in regex_map:
        regex_map[regex] = re.compile(regex)
      
    return regex_map[regex].match(txt)


def tail_tag(line, tags):
    for tag in tags:
#        tag = ":" + tag
        if tag in line and not line.endswith(tag):
            line = line.replace(tag, "") + tag
    return line


def istota(allAffixFlags):
    return "patr" in allAffixFlags or "<" in allAffixFlags

def person(allAffixFlags):
    return "patr" in allAffixFlags or ("<" in allAffixFlags and not ">" in allAffixFlags)

def firstname(word, allAffixFlags):
    return ("patr" in allAffixFlags or ("<" in allAffixFlags and not ">" in allAffixFlags)) and not "+" in allAffixFlags \
        and word[0].isupper() and not word[1].isupper()
        #and affixFlag != "p" \

def dual_last_name_ending(line):
    return "+d" in line or re_match(".*(о|ич|ук|юк|як|аш|яш|сь|ун|ин|сон) ", line)


# dictionary-related methods


POS_MAP = {
 "adj": "adj",
 "numr": "numr",
 "n": "noun",
# "vi": "verb:imperf",
# "vp": "verb:perf",
 "vr": "verb",
 "v": "verb",
 "<": "noun"
}


def get_pos(posFlag, modifiers):
    posFlag = re_sub("[\._].*", "", posFlag)
#  logger.debug("\t\t"  + posFlag + ", " + str(modifiers))

    if False and "pos" in modifiers:
        pos = modifiers["pos"]
    else:
        if posFlag in POS_MAP:
            posFlag = posFlag
        elif posFlag[:3] in POS_MAP:
            posFlag = posFlag[:3]
        elif posFlag[:2] in POS_MAP:
            posFlag = posFlag[:2]
        else:
            posFlag = posFlag[0]
    
        pos = POS_MAP[posFlag]
    
    return pos


GEN_LIST=["m", "f", "n", "p"]
VIDM_LIST=["v_naz", "v_rod", "v_dav", "v_zna", "v_oru", "v_mis", "v_kly"]
re_nv_vidm=re.compile("(noun):[mfn]:(.*)")

#@profile
def expand_nv(in_lines):
    lines = []
    
    for line in in_lines:
        if ("noun" in line or "numr" in line) and ":nv" in line and not ":v_" in line:
            parts = line.split(":nv")
            
            
            for v in VIDM_LIST:
                if v == "v_kly" and (not ":anim" in line or ":lname" in line):
                    continue
                lines.append(parts[0] + ":" + v + ":nv" + parts[1])
            
            if "noun" in line:
                if not ":p" in line and not ":np" in line and not ":lname" in line:
                    for v in VIDM_LIST:
                        if v != "v_kly" or "anim" in line:
                            lines.append(re_nv_vidm.sub("\\1:p:" + v + ":\\2", line))
        
        #        print("expand_nv", in_lines, "\n", lines, file=sys.stderr)
        elif ("adj" in line) and ":nv" in line and not ":v_" in line:
            parts = line.split(":nv")
            
            if re_match(".*:[mnfp]", parts[0]):
                gens = parts[0][-1:]
                parts[0] = parts[0][:-2]
            else:
                gens = GEN_LIST
        
            for g in gens:    
                for v in VIDM_LIST:
                    if v == "v_kly" and (not ":anim" in line or ":lname" in line):    # TODO: include v_kly? but not for abbr like кв.
                        continue
                    lines.append(parts[0] + ":" + g + ":" + v + ":nv" + parts[1])
        else:
            lines.append(line)
    
    return lines





GEN_ORDER = {
    "m": "0", 
    "f": "1", 
    "n": "3", 
    "s": "4", 
    "p": "5"
}

VIDM_ORDER = {
   "v_naz": "10",
   "v_rod": "20",
   "v_dav": "30",
   "v_zna": "40",
   "v_oru": "50",
   "v_mis": "60",
   "v_kly": "70"
}

re_verb = re.compile("(in[fz]:coll|in[fz]|impr|pres|futr|past|impers)")
vb_tag_key_map = {
 "inf": "_1",
 "inz": "_2",
 "inf:coll": "_3",
 "inz:coll": "_4",
 "impr": "_5",
 "pres": "_6",
 "futr": "_7", 
 "past": "_8",
 "impers": "_9"
}

GEN_RE = re.compile(":([mfnsp])(:|$)")
VIDM_RE = re.compile(":(v_...)((:alt|:rare|:coll)*)") # |:contr




re_person_name_key_tag = re.compile("^([^:]+(?::anim|:inanim|:perf|:imperf)?)(.*?)(:lname|:fname|:patr)")

re_key = re.compile(" ([^ ]+ [^:]+(?::rev)?(?::(?:anim|inanim|perf|imperf))?)")
re_key_name = re.compile(" ([^ ]+ noun:anim:[fmnp]:).*?(lname|fname|patr)")


def derived_plural(key, prev_key):
    return "name" in key and ":p:" in key and \
        re.search(":[mf]:", prev_key) and re.sub(":[mf]:", ":p:", prev_key) == key

def sub_stat(pos, sub_pos, line, sub_pos_stat):
    if ":" + sub_pos in line:
        if not pos in sub_pos_stat:
            sub_pos_stat[pos] = collections.defaultdict(int)
        sub_pos_stat[pos][sub_pos] += 1

def indent_lines(lines):
    pos_stat = collections.defaultdict(int)
    sub_pos_stat = collections.defaultdict(list)
    letter_stat = collections.defaultdict(int)
    cnt = 0
    cnt_std = 0
    out_lines = []
    prev_key = ""
    
    for line in lines:
        word, lemma, tags = line.split()
        
        try:
            if "name" in tags or "patr" in tags:
                key_rr = re_key_name.search(line)
                key = key_rr.group(1) + key_rr.group(2)
            else:
                key_rr = re_key.search(line)
                key = key_rr.group(1)
        except:
            print("Failed to fine tag key", line, file=sys.stderr)

        if ":x" in line:
            x_idx = line.index(":x")
            key += line[x_idx: x_idx+4]

        if ":nv" in line:
            key += ":nv"
        
        if key != prev_key and not derived_plural(key, prev_key):
            prev_key = key
            line = word + " " + tags
            
            cnt += 1
            if not "advp" in line:
                cnt_std += 1 
            
#            dbg("new key", key)
        else:
            line = DERIV_PADDING + word + " " + tags

            if "m:v_naz" in line:
                sub_stat("adj", "super", line, sub_pos_stat)
                sub_stat("adj", "compr", line, sub_pos_stat)
# for -corp compr/super are now separe lemmas
#                if ":compr" in line or ":super" in line:
#                    cnt_std += 1
            
        out_lines.append(line)
        
        if line[0] != " ":
            pos_tag = tags.split(":", 1)[0]
            pos_stat[pos_tag] += 1
            letter_stat[word[0].lower()] += 1

            for sub_pos in ["inanim", "anim", "lname", "fname", "patr", "nv", "perf", "imperf", "compb"]:
                sub_stat(pos_tag, sub_pos, line, sub_pos_stat)
            
    
    if "-stats" in sys.argv:
        print_stats(cnt, cnt_std, pos_stat, sub_pos_stat, letter_stat)
        

    return out_lines


def print_stats(cnt, cnt_std, pos_stat, sub_pos_stat, letter_stat):
    with open("dict_stats.txt", "w") as stat_f:
        print("Всього лем:", cnt, file=sys.stderr)
        print("Всього лем:", cnt, file=stat_f)
        print("  словникових лем (без advp, з compr/super)", cnt_std, file=stat_f)

        print("\nЧастоти за тегами:", file=stat_f)
    
        ordered_pos_freq = sorted(pos_stat.keys())
        for pos in ordered_pos_freq:
            print(pos, pos_stat[pos], file=stat_f)
            
            current_sub_pos_stat = sub_pos_stat[pos]
            for sub_pos in sorted(current_sub_pos_stat):
                print("    ", sub_pos, current_sub_pos_stat[sub_pos], file=stat_f)

        print("\nЧастоти за літерами:", cnt, file=stat_f)

        keys = sorted(letter_stat, key=letter_stat.get, reverse=True)
        for k in keys:
            print(k, letter_stat[k], file=stat_f)


re_xv_sub = re.compile("^([^:]+)(.*)(:x.[1-9])")

#@profile
def tag_sort_key(tags, word):
    if ":v-u" in tags:
        tags = tags.replace(":v-u", "")

    if "v_" in tags:
        vidm_match = VIDM_RE.search(tags)
        if vidm_match:
            vidm = vidm_match.group(1)
            vidm_order = VIDM_ORDER[vidm]
        
            if vidm_match.group(3):
                vidm_order = vidm_order.replace("0", str(vidm_match.group(2).count(":")))

            tags = VIDM_RE.sub(":"+vidm_order, tags)

    if tags.startswith("adj:"):
        if not ":comp" in tags and not ":supe" in tags:
            # make sure :contr without :combp sorts ok with adjective base that has compb
            if ":contr" in tags:
                tags = tags.replace(":contr", "").replace("adj:", "adj:compc")
            else:
                tags = tags.replace("adj:", "adj:compb:")
        else:
            # відокремлюємо різні порівняльні форми коли сортуємо: гладкий/гладкіший
            if ":super" in tags:
                tags = re.sub("(:super)(.*)(:xx.)", "\\3\\1\\2", tags)
            if ":compr" in tags:
                tags = re.sub("(:compr)(.*)(:xx.)", "\\3\\1\\2", tags)

            if ":super" in tags:
                if word.startswith("що"):
                    tags = tags.replace(":super", ":supes")
                elif word.startswith("як"):
                    tags = tags.replace(":super", ":supet")
    elif tags.startswith("advp"):
        tags = tags.replace("advp", "verz")  # put advp after verb
        if ":coll" in tags:
            tags = tags.replace("perf", "perz")
    elif tags.startswith("noun"):
        if ("name" in tags or "patr" in tags):
            tags = re_person_name_key_tag.sub("\\1\\3\\2", tags)
            if ("lname" in tags or "patr" in tags) and ":f:" in tags:# and not ":nv" in tags:    # to put Адамишин :f: after Адамишини :p:
                tags = tags.replace(":f:", ":9:")
        
        if ":nv" in tags:
            tags = tags.replace(":nv", "").replace("anim", "anim:nv")
        
        if ":np" in tags or ":ns" in tags:
            tags = tags.replace(":np", "").replace(":ns", "")
        
    elif tags.startswith("verb"):
        verb_match = re_verb.search(tags)
        if verb_match:
            tg = verb_match.group(0)
            tags = tags.replace(tg, vb_tag_key_map[tg])
        else:
            if not re.match("verb:(rev:)?(im)?perf:unknown", tags):
                print("no verb match", tags, file=sys.stderr)

    gen_match = GEN_RE.search(tags)
    if gen_match:
        gen = gen_match.group(1)
        tags = GEN_RE.sub(":"+GEN_ORDER[gen]+"\\2", tags, count=1)

    if ":x" in tags:
        tags = re_xv_sub.sub("\\1\\3\\2", tags)

    return tags


def line_key(txt):
    try:
        word, lemma, tags = txt.split()
    except:
        print("Failed on", txt, file=sys.stderr)
        raise
    
    if "verb:rev" in tags and "inf" in tags and word.endswith("сь"):
        tags = tags.replace("inf", "inz")
        
    if "-" in lemma:
        lemma += "я"

    if "'" in lemma:
        lemma += "я"

    return locale.strxfrm(lemma) + "_" + tag_sort_key(tags, word) + "_" + locale.strxfrm(word)

#@profile
def sort_all_lines(all_lines):
    sorted_lines = sorted(all_lines, key=line_key)
    return sorted_lines




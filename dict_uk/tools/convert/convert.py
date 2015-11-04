#!/usr/bin/python3

import sys
import re
import locale
import collections

from compar_forms import COMPAR_FORMS
from compar_forms import COMPAR_SOFT_BASE

locale.setlocale(locale.LC_ALL, 'uk_UA')


RE_DIR=re.compile("[ACEGIKMOQSW680-3]")
RE_REV=re.compile("[BDFHJLNPR79]")

def combine(word, out_flags, flags, extra):

    if 'X' in flags:
        out_flags.append('v-u')
        flags.remove('X')

    line = word + ' /' + '.'.join(out_flags)
    
    if flags:
      #line += ' '
      flags_left = []
      for f in flags:
        if f in "<>+@mf" or (f == 'd' and "<+d" in ''.join(flags)):
            if f == flags[0]:
                line += "."
            line += f
        else:
            flags_left.append(f)

    if extra:
        line += ' ' + ' '.join(extra)

    if flags and flags_left:
        line += " [" + ''.join(flags_left) + "]"
    
    return line


def rep_a(word, flags):
    out_flags = []
    extra = []

    if "V" in flags and "4" in flags:
        out_flags.append("adj")
        extra.append("g=fp")
        flags.remove("4")
        flags.remove("V")

    if "9" in flags: out_flags.append("adj"); extra.append("g=f"); flags.remove("9")
    
    if "V" in flags: 
        out_flags.append("adj")
        if '<' in flags and not '+' in flags and word.endswith('ий'):
            extra.append('g=mfp')
        elif word.endswith("а"):
            extra.append("g=f")
        flags.remove("V")
        
    if "U" in flags:
        if word.endswith("ів"): out_flags.append("adj_ev"); flags.remove("U")
        elif word.endswith("ий"): out_flags.append("adj"); extra.append("g=m"); flags.remove("U")
        else: out_flags.append("n2adj2"); flags.remove("U")
        if "h" in flags: out_flags.append("ke"); flags.remove("h")

    if "Y" in flags: out_flags.append("supr"); flags.remove("Y")

    if "W" in flags: out_flags.append("adv"); flags.remove("W")

    return combine(word, out_flags, flags, extra)


def rep_adj_n(word, flags):
    out_flags = []
    extra = []

    if "ij" in ''.join(flags):
        out_flags.append("adj")
        if word.endswith('а'):
            extra.append("^noun")
            extra.append("g=fp")
        else:
            extra.append("^noun")
            extra.append("g=mp")
        flags.remove('i')
        flags.remove('j')
    elif "i" in flags:
        if "<" in flags and word.endswith("ін"):
            out_flags.append("n2adj1")
            if "h" in flags: out_flags.append("ke"); flags.remove("h")
        elif "i<" in ''.join(flags):
            out_flags.append("adj")
            if word.endswith('а'):
                extra.append("g=f")
            else:
                extra.append("g=m")
        else:
            out_flags.append("adj")
            extra.append("^noun")
            if word.endswith('а'):
                extra.append("g=f")
            else:
                extra.append("g=m")
        flags.remove("i")
    elif "l" in flags:
        out_flags.append("n2adj2")
        flags.remove("l")
    elif flags[0] == "j":
        if re.search('[тк]а$', word):
            out_flags.append("np1")
        else:
            out_flags.append("adj")
            extra.append("^noun")
            extra.append("g=p")
        
        flags.remove("j")
        print('@@@', word, flags, combine(word, out_flags, flags, extra), file=sys.stderr)

    return combine(word, out_flags, flags, extra)



def rep_n10(word, flags):
    out_flags = []
    extra = []

    if flags[0] == 'b':
        if re.search("([вдкнстр]|ка|сла|[сжвм]на)$", word): out_flags.append("np3")
        elif re.search("а$", word): out_flags.append("np1")
        else: out_flags.append("np3")
        flags.remove('b')
        
    elif flags[0] == 'o':
        if word[-1:] in 'ая':
            out_flags.append("np3")
        else:
            out_flags.append("np1")
        flags.remove('o')
        
    if "a" in flags: out_flags.append("n10"); flags.remove("a")
    if "b" in flags:
        if re.search("([ст]ла|[сжвм]на|озна|[^аеєиіїоуюя]ка|йня|ьня|[^аеєиіїйоуюякнхь]ня|[^аеєиіїоуюялск]ля)$", word):
            out_flags.append("p2")
        else:
            out_flags.append("p1")
            if word.endswith("роділля"):
                flags.remove("j")
        flags.remove("b")
    if "o" in flags: out_flags.append("p2"); flags.remove("o")
    if "f" in flags: 
        if word[-1:] in "ая":
            out_flags.append("p2") # суддя/afd<
        out_flags.append("piv")
        flags.remove("f")
    
    if 'd' in flags and re.match(".*[аяь]$", word):
        out_flags.append('ko')
        flags.remove('d')
    
    if "p" in flags: out_flags.append("patr"); flags.remove('p')
     
    return combine(word, out_flags, flags, extra)


ending_a_numr_re = re.compile('.*(ять|сят|сто)$')

def rep_non10(word, flags):
    out_flags = []
    out_flags_2 = []
    extra = []
    
    if "a" in flags:
        if ending_a_numr_re.match(word):
            out_flags.append("numr")
        elif re.search('(о[вклнмр]|[еєо][нцт]ь|[ео]ль|вугіль|[^к]інь|е[йлнрст])$', word):
            out_flags.append("n22")
        else:
            out_flags.append("n21")
        flags.remove('a')
        
        if "e" in flags:
            out_flags_2 = ["n20"]
            flags.remove("e")
        
    if "b" in flags: out_flags.append("p"); flags.remove('b')
    if "c" in flags: out_flags.append("a"); flags.remove('c')
    if "d" in flags: out_flags.append("ke"); flags.remove('d')
    if "p" in flags: out_flags.append("patr"); flags.remove('p')
    if "h" in flags: out_flags.append("ke"); flags.remove('h')

    return combine2(word, flags, out_flags, out_flags_2, extra, out_flags)


def rep_n20(word, flags):
    flags_str = ''.join(flags)

    out_flags = []
    extra = []
      
    if flags[0] == 'f': out_flags.append("np2"); flags.remove('f')
    if "e" in flags: out_flags.append("n20"); flags.remove("e")
    if "g" in flags: out_flags.append("a"); flags.remove("g")
    if "f" in flags: out_flags.append("p"); flags.remove("f")
    if "j" in flags: out_flags.append("pyn"); flags.remove("j")

    if "o" in flags and re.search("[гґкх]$", word): out_flags.append("zi"); flags.remove("o")
    elif "o" in flags and re.search("и$", word): out_flags.append("np1"); flags.remove("o")
#    if "o" in flags and re.search("[и]$", word): out_flags.append(""); flags.remove("o")

    if "h" in flags:
        out_flags.append("ke")
        flags.remove("h")
    
    if not "<+d" in flags_str:
        if "d" in flags: out_flags.append("ke"); flags.remove("d")
    if "p" in flags: out_flags.append("patr"); flags.remove('p')
    
    return combine(word, out_flags, flags, extra)


def rep_n23(word, flags):
    out_flags = []
    extra = []
      
    if "u" in flags: out_flags.append("n23"); flags.remove('u')
    if "v" in flags: out_flags.append("p"); flags.remove('v')
    if "x" in flags: out_flags.append("a"); flags.remove('x')
    if "d" in flags: out_flags.append("ke"); flags.remove('d')

    return combine(word, out_flags, flags, extra)


def rep_n24(word, flags):
    out_flags = []
    extra = []

    if "l" == flags[0]: out_flags.append("n24"); flags.remove("l")
    if "q" in flags: out_flags.append("a"); flags.remove("q")
    elif word.endswith("ок"): out_flags.append("a")
    if "m" in flags: out_flags.append("p"); flags.remove("m")

    if "h" in flags: out_flags.append("ke"); flags.remove("h")
    
    if not "a" in out_flags and word.endswith("ець"):
        out_flags.append("a")

    if "p" in flags: out_flags.append("patr"); flags.remove('p')
    
    return combine(word, out_flags, flags, extra)

def rep_n2n(word, flags, extra_flags):
    out_flags = []
    extra = []
    flags_str = ''.join(flags)
      
    if "i" in flags: 
        if re.search('[вкнт]е$', word):
            out_flags.append("n2adj1")
        else:
            out_flags.append("n2n")
        flags.remove("i")
    elif "e" in flags: 
        out_flags.append("n2n")
        out_flags.append("ovi")
        flags.remove("e")

    if "j" in flags:
        if word.endswith("ікло"):
            out_flags.append("piv")
        elif re.search('([^аеоиіуь]к|сл)о$', word):
            out_flags.append("p2")
        else:
            out_flags.append("p1")
        flags.remove("j")

    if "t" in flags: 
        out_flags.append("ovi")
        flags.remove("t")

    if "f" in flags: 
        if word.endswith("о"):
            out_flags.append("piv")
        else:
            out_flags.append("p2")
        flags.remove("f")
    if "o" in flags: 
        out_flags.append("p2")
        flags.remove("o")

    masc_flags = ''
    
    if word.endswith("о"):
      if "^noun:m" in extra_flags:
        out_flags = [ "n20", "po" ]
        if "h" in flags:
            out_flags.append("ke")
            flags.remove("h")
        extra_flags = extra_flags.replace("^noun:m", "").rstrip()
      elif ":+m" in extra_flags:
        extra_flags = extra_flags.replace(":+m", "").rstrip()

        masc_out_flags = [ "n20", "po" ]

        if "h" in flags:
#            if not "ejh<" in flags_str or not ":+m" in extra_flags:
            masc_out_flags.append("ke")
            flags.remove("h")

        masc_line = combine(word, masc_out_flags, flags, extra)
        masc_flags = " ".join(masc_line.split()[1:])

    line = combine(word, out_flags, flags, extra)
    
    if masc_flags:
        parts = line.split()
        line = " ".join(parts[:2]) + " " + masc_flags + " ".join(parts[2:])

    return line, extra_flags


def rep_n3(word, flags):
    out_flags = []
    extra = []
      
    if "l" in flags: out_flags.append("n30"); flags.remove("l")
    if "m" in flags: out_flags.append("p"); flags.remove("m")
    if "i" in flags: out_flags.append("n32"); flags.remove("i")
    if "j" in flags: out_flags.append("p"); flags.remove("j")
    if "n" in flags: out_flags.append("ke"); flags.remove("n")
    if "d" in flags: out_flags.append("ke"); flags.remove('d')
    
    return combine(word, out_flags, flags, extra)

def rep_n32(word, flags):
    out_flags = []
    extra = []
      
#    if "l" in flags: out_flags.append("n30"); flags.remove("l")
#    if "m" in flags: out_flags.append("p"); flags.remove("m")
    if "i" in flags: out_flags.append("n32"); flags.remove("i")
    if "j" in flags: out_flags.append("p"); flags.remove("j")
    if "d" in flags: out_flags.append("ke"); flags.remove('d')
    
    return combine(word, out_flags, flags, extra)

def rep_n31(word, flags):
    out_flags = []
    extra = []
      
    if "r" in flags: out_flags.append("n31"); flags.remove("r")
    if "s" in flags: out_flags.append("p"); flags.remove("s")
#    if "t" in flags: out_flags.append("ke"); flags.remove('d')
    
    return combine(word, out_flags, flags, extra)

def rep_n4(word, flags):
    out_flags = []
    extra = []
      
    if "l" in flags: out_flags.append("n40"); flags.remove("l")
    if "m" in flags: out_flags.append("p"); flags.remove("m")
    
    return combine(word, out_flags, flags, extra)


def rep_v(word, flags):
    lines = []
    
    if RE_REV.search(''.join(flags)) and not flags[0] == "S":
        if word.endswith("ти"):
            rev_word = re.sub('ти$', 'тися', word)
            rev_flags = list(RE_DIR.sub('', ''.join(flags)))
            flags = list(RE_REV.sub('', ''.join(flags)))
        else:
            rev_word = word
            rev_flags = flags
        
        cnv = rep_v_rev(rev_word, rev_flags)
        lines.append(cnv)
       
    if RE_DIR.search(''.join(flags)):
        cnv = rep_v_dir(word, flags)
        lines.insert(0, cnv)

    return lines


def rep_v_rev(word, flags):
    out_flags_A = []
    out_flags_I = []
    extra = []
    
    if "B" in flags: out_flags_A.append("vr1"); flags.remove("B")
    if "J" in flags: out_flags_I.append("vr2"); flags.remove("J")
    if "L" in flags: out_flags_A.append("vr3"); flags.remove("L")
    if "N" in flags: out_flags_A.append("vr4"); flags.remove("N")
    if "T" in flags: out_flags_A.append("vr5"); flags.remove("T")
    if "9" in flags: out_flags_A.append("vr6"); flags.remove("9")

    if out_flags_A:
        main_out_flags = out_flags_A
    else:    
        main_out_flags = out_flags_I
   
    if "D" in flags: out_flags_A.append("imprt0"); flags.remove("D")
    if "F" in flags: out_flags_A.append("imprt1"); flags.remove("F")
    if "H" in flags: main_out_flags.append("cf"); flags.remove("H")
    
    if "P" in flags: out_flags_A.append("advp"); flags.remove("P")
    if "R" in flags: out_flags_I.append("advp"); flags.remove("R")
    if "7" in flags: out_flags_A.append("advp"); flags.remove("7")

    line = combine2(word, flags, out_flags_A, out_flags_I, extra, main_out_flags)
    
    return line
      
    

def combine2(word, flags, out_flags_A, out_flags_I, extra, main_out_flags):
    if out_flags_A and out_flags_I:
        line_I = combine(word, out_flags_I, flags, extra)
        flags_I = " ".join(line_I.split()[1:])
    line = combine(word, main_out_flags, flags, extra)
    if out_flags_A and out_flags_I:
        parts = line.split()
        line = " ".join(parts[:2]) + " " + flags_I + " ".join(parts[2:])
    return line

def rep_v_dir(word, flags):
    out_flags_A = []
    out_flags_I = []
     
    extra = []
    #      print("dir", line, file=sys.stderr)
    
    if "A" in flags: out_flags_A.append("v1"); flags.remove("A")
    if "I" in flags: out_flags_I.append("v2"); flags.remove("I")
    if "K" in flags: out_flags_A.append("v3"); flags.remove("K")
    if "M" in flags: out_flags_A.append("v4"); flags.remove("M")
    if "8" in flags: out_flags_A.append("v6"); flags.remove("8")
    if "S" in flags:
        if word.endswith("ся"):
            out_flags_A.append("vr5")
        else:
            out_flags_A.append("v5")
        flags.remove("S")
        if "5" in flags: out_flags_A.append("cf"); flags.remove("5")
        if "R" in flags: out_flags_A.append("advp"); flags.remove("R")
    
    if out_flags_A:
        main_out_flags = out_flags_A
    else:    
        main_out_flags = out_flags_I
    
    if "C" in flags: 
        if not out_flags_A and flags[0] == "C":
            out_flags_I.append("v2")
            extra.append("tag=:impr|:inf")
        else:
            out_flags_A.append("imprt0")
        flags.remove("C")
    if "E" in flags: out_flags_A.append("imprt1"); flags.remove("E")
    if "G" in flags: main_out_flags.append("cf"); flags.remove("G")
    
    if "O" in flags: out_flags_A.append("advp"); flags.remove("O")
    if "Q" in flags: out_flags_I.append("advp"); flags.remove("Q")
    if "6" in flags: out_flags_A.append("advp"); flags.remove("6")
    
    for xx in "W01234":
        if xx in flags: main_out_flags.append("impers"+xx); flags.remove(xx)
    
    line = combine2(word, flags, out_flags_A, out_flags_I, extra, main_out_flags)
    
    return line


def convert(line):
    main, *extra = line.split(' ')
    extra = ' '.join(extra)
    
    word, flags = main.split('/')
    flags = list(flags)
      
      
    if flags[0] == "X":
        line = combine(word, [], flags, [])
    elif re.search("/[VU4]", line):
        line = rep_a(word, flags)
    elif re.search("[иі]й/ij?|а/ij?|/j|ів/l|ін/i", line):
        line = rep_adj_n(word, flags)
    elif re.search("[ая]/a|[аиіїя]/[bo]", line):
        line = rep_n10(word, flags)
    elif re.search("[^ая]/a", line):
        line = rep_non10(word, flags)
    elif re.search("[оея]/i|о/ej", line):
        line, extra = rep_n2n(word, flags, extra)
    elif re.search("/[uv]", line):
        line = rep_n23(word, flags)
    elif re.search("ко/eo", line):
        line = rep_n2n(word, flags, extra)
    elif re.search("/[ef]", line):
        line = rep_n20(word, flags)
    elif re.search("р/l|[іо][дкнрт]/l|ень/l|[еє]ць/l|осен/l", line):
        line = rep_n24(word, flags)
    elif re.search("ість/l", line) or re.search("[ьчжшщ]/i", line) or re.search("[ьчжшщ]/l", line):
        line = rep_n3(word, flags)
    elif re.search("матір/r|ь/r", line):
        line = rep_n31(word, flags)
    elif re.search("[^ії][бвфм]/i|[чь]/l|[рш]/i", line):
        line = rep_n32(word, flags)
    elif re.search("[ая]/l", line):
        line = rep_n4(word, flags)
    elif re.search("/[A-T6789]", line):
        lines = rep_v(word, flags)
        line = (' '+extra+'\n').join(lines)

    line = line.replace("  ", " ")
    return line + ' ' + extra



adj_to_cs = collections.defaultdict(list)


def append_combined_cs(base, lemma, base_with_compars):
        try:
            base_adj = delayed_adj[base]
        except:
            print('Failed find compb for lemma:', lemma, file=sys.stderr)
            raise
        
        cs_append_line = ' \\\n' + ' +cs=' + delayed_adj_cs[lemma].replace(" /adj.supr", "")
        
        adj_to_cs[base].append(lemma)
        
        base_with_compars[base].append(cs_append_line)



def post_process_adj(delayed_adj, delayed_adj_cs):
    print("Delayed adj", len(delayed_adj), len(delayed_adj_cs), file=sys.stderr)
    
    out_adj_lines = []
    delayed_adj2 = dict(delayed_adj)
    base_with_compars = collections.defaultdict(list)
    
    for lemma in sorted(delayed_adj_cs.keys()):
        line = delayed_adj_cs[lemma]
        if lemma in COMPAR_FORMS:
            base = COMPAR_FORMS[lemma]
        elif lemma in COMPAR_SOFT_BASE:
            # try non-soft too: дружний/дружній
            base = re.sub('іший$', 'ий', lemma)
            if base in delayed_adj:
                append_combined_cs(base, lemma, base_with_compars)
                if base in delayed_adj2:
                    del delayed_adj2[base]

            base = re.sub('іший$', 'ій', lemma)
        else:
            base = re.sub('іший$', 'ий', lemma)
        
        append_combined_cs(base, lemma, base_with_compars)
        
        if base in delayed_adj2:
            del delayed_adj2[base]
    
    for line in delayed_adj2.values():
        out_adj_lines.append(line)
    
    for base, line in base_with_compars.items():
        out_line = delayed_adj[base] + ''.join(base_with_compars[base]) 
        out_adj_lines.append(out_line)
    
    for i, out_line in enumerate(out_adj_lines):
        word = out_line.split(" ")[0]
        if word in delayed_comment:
            if "\\" in out_line:
                out_adj_lines[i] = out_line.replace("\\", "\\" + delayed_comment[word], 1)
            else:
                out_adj_lines[i] += delayed_comment[word]
    
    return out_adj_lines


def append_delayed_comment(line):
        word = line.split(" ")[0]
        if word in delayed_comment:
            return delayed_comment[word]
        return ""


def post_process_adv(delayed_adv, delayed_adj, delayed_adj_cs):
    print("Delayed adv", len(delayed_adv), "adj", len(delayed_adj), "adj_cs", len(delayed_adj_cs), file=sys.stderr)
#    print("Delayed adv", delayed_adv, delayed_adj, adj_to_cs, delayed_adj_cs, file=sys.stderr)

    out_lines = []
    
    for lemma, line in delayed_adv.items():
        if lemma[-1:] not in "ое":
            out_lines.append(line)
            continue
    
        # далеко -> далекий
        adj = lemma[:-1] + 'ий'
#        print('adv-adj', lemma, adj, file=sys.stderr)
        
        if adj not in adj_to_cs:
        
            # давно -> давній
            adj = lemma[:-1] + 'ій'
            if adj not in adj_to_cs:

                # путньо -> путній
                adj = lemma[:-2] + 'ій'
                if adj not in adj_to_cs:
 
                    if lemma == "рано":
                        adj = "ранній"
                    else:
                        out_lines.append(line)
                        continue

        out_line = line
        for adj_to_cs_ in adj_to_cs[adj]:
            adj_comp = adj_to_cs_
            adv_comp = adj_comp[:-2] + "е"

#            print('-1-', out_line, file=sys.stderr)
            out_line += " \\" + append_delayed_comment(out_line)
            # in LT compr is based of base which has correction suggestions
#            adv_comp = append_delayed_comment(adv_comp)

            out_line += "\n" + " +cs=" + adv_comp
        out_lines.append(out_line)

    return out_lines


RE_EXTRA_FLAGS=re.compile('(<\+?[mf]?)')
RE_EXTRA_FLAGS_SPACE=re.compile(' (<\+?[mf]?)')


def preprocess(line):

    if " ^adjp" in line and "&adj" in line:
#        print('000', line, file=sys.stderr)
        line = re.sub(r" \^(adjp(?::pasv|:actv|:pres|:past|:perf|:imperf)+)(.*)(:&adj)", r" \2:&\1", line)
        line = re.sub(r"( :.*) (:.*)", r"\1\2", line)

    if line.endswith("чи") and not " " in line:
        out_line = line + ' ' + line + " advp:imperf"
        return [out_line]
    if line.endswith("чись") and not " " in line:
        out_line = line + ' ' + line + " advp:rev:imperf"
        return [out_line]

    if "Z" in line:
        line1 = line.replace("Z", "")
        line2 = "не" + line1.replace(':v-u', '').replace('X', '')
        
        line1 = re.sub("/($| )\^?", "\\1", line1)
        line2 = re.sub("/($| )\^?", "\\1", line2)
        
        lines = [line1, line2]
    else:
        if "/Y" in line:
            lines = []  # TODO: вище, нижче
#             ...
        else:
            lines = [line]
    
    out_lines = []
    for line in lines:
        if "X" in line:
            line1 = line.replace("X", "")
            line1 = re.sub("/($| )\^?", "\\1", line1)
            line1 = line1.replace("|^", ":v-u|")
            line1 = re.sub("( [a-z]+) :", "\\1:", line1)
            
            if " :" in line1 or not re.search("[а-яА-ЯіїґєІЇЄҐ]/", line1):
                vu = ":v-u"
            else: 
                vu = " :v-u"
            line1 += vu

            if ":rare:v-u" in line1:
                line1 = line1.replace(":rare:v-u", ":v-u:rare")
        
            pos = 1
            if line.startswith("в'"):
                pos = 2
            line2 = "у" + line1[pos:]

            out_lines.extend([line1, line2])
        else:
            out_lines.append(line)
    
    lines = out_lines

    return lines


def convert_composite_line(line, out_lines):
    word, *comment = line.split(" ")
    parts = word.split("-")
    
    try:
      if "/" in parts[0]:
        out_part1 = convert(parts[0])
      else:
        out_part1 = parts[0]

      if "/" in parts[1]:
        out_part2 = convert(parts[1])
      else:
        out_part2 = parts[1]
    except:
        print('Failed at', line, file=sys.stderr)
        raise

    if comment:
      extra = " " + " ".join(comment)
    else:
      extra = ""
    out_line = out_part1.strip() + ' - ' + out_part2.strip() + extra
    out_lines.append(out_line)


delayed_adj = {}
delayed_adj_cs = {}
delayed_adv = {}
delayed_comment = {}

def convert_line(line, out_lines):
      if "#" in line: # or re.search(" [^:^<a-z]+$", line):
          line, comment = line.split("#", 1)
          line = line.rstrip()
          comment = "    #" + comment
    #      if "adv" in line and not "advp" in line:
    #        print('-5-', line, "-", comment, file=sys.stderr)
      else:
          comment = ""
    
      try:
          lines = preprocess(line)
      except:
          print('Failed on ', line, file=sys.stderr)
          raise

    
      for line in lines:
        if "/" in line and re.search("[а-яіїєґА-ЯІЇЄҐ]/", line):
            out_line = convert(line)

            # TODO: remove next line as it's handled in combine()?
            if RE_EXTRA_FLAGS_SPACE.search(out_line):
                out_line = RE_EXTRA_FLAGS_SPACE.sub(".\\1", out_line)
            out_line = out_line.strip()
        else:
            out_line = line
            if RE_EXTRA_FLAGS_SPACE.search(out_line):
                out_line = RE_EXTRA_FLAGS_SPACE.sub(" /\\1", out_line)


        if "/V" in line and not "<" in line and (not " ^adjp" in line or ":&adj" in line) and not "&pron" in line:
            w = out_line.split(' ', 1)[0]
            if "Y" in line:
                delayed_adj_cs[w] = out_line
            else:
                delayed_adj[w] = out_line

            if comment:
                delayed_comment[w] = comment
        elif re.search('[ ^]adv($|[ :])', line):
            w = out_line.split(' ', 1)[0]
            delayed_adv[w] = line

            if comment:
                delayed_comment[w] = comment
        else:
            out_lines.append(out_line + comment)


#----------
# main code
#----------
if __name__ == "__main__":

    out_lines = []
    
    for line in sys.stdin:
    
      line = line.strip()
#      if not line:
#          continue

#      if line.lstrip().startswith("#") or not re.search("[а-яіїєґА-ЯІЇЄҐ](/[a-zA-Z]|-? <|[а-яіїєґ] [a-z:]+ [а-яіїєґ])", line):
      if line.lstrip().startswith("#"):
          out_lines.append(line)
          continue

      if "spell-only" in line:
          continue
    
      if line.endswith("-"):
          line = line[:-1]
      elif "- " in line:
          line = line.replace("- ", " ")
    
      if "-comp" in sys.argv:
        convert_composite_line(line, out_lines)
      else:
        convert_line(line, out_lines)
    
    out_adj_lines = post_process_adj(delayed_adj, delayed_adj_cs)
    out_lines.extend(out_adj_lines)

    out_adv_lines = post_process_adv(delayed_adv, delayed_adj, delayed_adj_cs)
    out_lines.extend(out_adv_lines)
    
    if not "-nosort" in sys.argv:
        out_lines = sorted(out_lines, key=locale.strxfrm)
    
    re_bad = re.compile('[а-яїієґ]/', re.IGNORECASE)
    for line in out_lines:
        if not line.startswith("#") and re_bad.search(line):
            raise Exception("Failed to convert " + line)
        
        print(line)

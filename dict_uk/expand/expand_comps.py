#!/usr/bin/python3

# -*- coding: utf-8 -*-

import sys
import re
import os
import locale

import affix
import expand




tags_re = re.compile("(.*:)[mfnp]:v_...(.*)")

def match_comps(lefts, rights):
    outs = []
    left_v = {}
    left_gen = ""
    mixed_gen = False
    
    for ln in lefts:
        parts = ln.split(" ")
        rrr = re.search(":(.:v_...)", parts[2])
        if not rrr:
            print("composite: ignoring left", ln, file=sys.stderr)
            continue
        
        vidm = rrr.group(1)
        if vidm[0] in "mfn":
            if not left_gen:
                left_gen = vidm[0]
            else:
                if left_gen != vidm[0]:
                    mixed_gen = True

        if not vidm in left_v:
            left_v[vidm] = []

        left_v[vidm].append(parts[0])
        left_wn = parts[1]
        left_tags = parts[2]

    if mixed_gen:
        left_gen = ""
#     print("left_gen", left_gen, "mixed_gen", mixed_gen, file=sys.stderr)

    for rn in rights:
        parts = rn.split(" ")
        rrr = re.search(":(.:v_...)", rn)
        if not rrr:
            print("composite: ignoring right", rn, file=sys.stderr)
            continue

        vidm = rrr.group(1)
        if left_gen != "" and vidm[0] in "mfn":
          vidm = left_gen + vidm[1:]
        
#         print("-", vidm, file=sys.stderr)
        
        if not vidm in left_v:
            continue
        
        for left_wi in left_v[vidm]:
            w_infl = left_wi + "-" + parts[0]
            lemma = left_wn + "-" + parts[1]
            if "-spell" in sys.argv:
                str = w_infl
                if not str in outs:
                    outs.append(str)
            else:
                str = w_infl + " " + lemma + " " + tags_re.sub("\\1"+vidm+"\\2", left_tags)
                outs.append(str)

    return outs


def expand_composite_line(line):
        if not " - " in line:
          return [line]
        
        if " :" in line:
          parts_all = line.split(" :")
          line = parts_all[0]
          parts_all[1] = ":" + parts_all[1]
        elif " ^" in line:
          parts_all = line.split(" ^")
          line = parts_all[0]
          parts_all[1] = "^" + parts_all[1]
        else:
          parts_all = [line]
        
        parts = line.split(" - ")
        print(parts, file=sys.stderr)
        
        if not "/" in parts[1]:
          parts[1] += " noun:m:nv"
        
        if len(parts_all) > 1:
            extra_tags = parts_all[1]
            parts[0] += " " + extra_tags
            parts[1] += " " + extra_tags
            
            
        lefts = expand.expand_line(parts[0], True)
        
        rights = expand.expand_line(parts[1], True)

        comps = match_comps(lefts, rights)

        return comps



# --------------
# main code
# --------------

if __name__ == "__main__":


    aff_arg_idx = sys.argv.index("-aff") if "-aff" in sys.argv else -1
    if aff_arg_idx != -1:
        affix_filename = sys.argv[aff_arg_idx+1]
    else:
    #    affix_filename = os.path.dirname(os.path.abspath(__file__)) + "/affix/all.aff"
        affix_filename = "affix/all.aff"
    
    affix.load_affixes(affix_filename)

    
    if True or "-" in sys.argv:
      ifile = sys.stdin
      ofile = sys.stdout
    
    for line in ifile:
    
        line = line.strip()
        if len(line) == 0 or line[0] == "#":
          continue
        
        try:
          comps = expand_composite_line(line)
        except:
          print("Failed at", line, file=sys.stderr)
          raise

        ofile.write("\n".join(comps) + "\n")


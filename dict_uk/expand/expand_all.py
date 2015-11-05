#!/usr/bin/python3

import sys
import os
import locale

import expand
import affix
import expand_comps
import tagged_wordlist


if __name__ == "__main__":
    usage = "Usage: expand_all.py -aff <affix_dir> -dict <dictionary dir>"

    if "-aff" in sys.argv:
        aff_arg_idx = sys.argv.index("-aff")
        affix_filename = sys.argv[aff_arg_idx + 1]
    else:
        raise Exception(usage)
    
    if "-dict" in sys.argv:
        dic_arg_idx = sys.argv.index("-dict")
        dict_dirname = sys.argv[dic_arg_idx + 1]
    else:
        raise Exception(usage)
    

    affix.load_affixes(affix_filename)
    
    dic_files = [ f for f in os.listdir(dict_dirname) if os.path.isfile(os.path.join(dict_dirname, f)) and f.endswith(".lst") ]

    out_lines = []
    for dic_filename in dic_files:

        print("Processing file", dic_filename, file=sys.stderr)
        
        fullname = os.path.join(dict_dirname, dic_filename)
        
        if "composite.lst" in dic_filename:
            with open(fullname, "r", encoding="utf-8") as dic_file:
                out = expand_comps.process_input(dic_file)
                out = sorted(out, key=locale.strxfrm)   # just to have consistent output in word_list.txt
        else:
            out = tagged_wordlist.process_input([fullname])
            
        print("\tgot {} lines".format(len(out)), file=sys.stderr)
        out_lines.extend(out)


    print("Expanding {} lines".format(len(out_lines)), file=sys.stderr)
    with open("word_list.txt", "w") as out_file:
        out_file.write("\n".join(out_lines))

    out_lines = expand.process_input(out_lines, flush_stdout_=False)

    if "-corp" in sys.argv:
        filename = "dict_corp_vis.txt"
    else:
        filename = "dict_rules_lt.txt"

    with open(filename, "w", encoding="utf-8") as out_file:
       for line in out_lines:
            out_file.write(line + "\n")

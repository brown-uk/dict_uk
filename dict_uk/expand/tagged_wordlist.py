#!/usr/bin/env python3

# -*- coding: utf-8 -*-

import sys
import re


except_base_tag = ""

def process_line_exceptions(line):
    global except_base_tag

    if not " " in line or re.match(".*[а-яіїєґА-ЯІЇЄҐ] /.*", line):
        return [line]
      
    if re.match("^[^ ]+ [^ ]+ [^:]?[a-z].*$", line):
        return [line]

    if line.startswith("# !"):
        except_base_tag = re.findall("![a-z:-]+", line)[0][1:] + ":"
        return []
    
    base = re.findall("^[^ ]+", line)[0]
    
    except_base_tag2 = except_base_tag
    if base.endswith("ся"):
        except_base_tag2 = except_base_tag.replace("verb:", "verb:rev:")
      
    out_line = re.sub("([^ ]+) ?", "\\1 " + base + " " + except_base_tag2 + "unknown\n", line)
    
    if except_base_tag in ("verb:imperf:", "verb:perf:"):
        out_line = re.sub("(verb:(?:rev:)?)((im)?perf:)", "\\1inf:\\2", out_line, 1)
      
        out_lines = out_line.split("\n")
        out_lines[0] = out_lines[0].replace(":unknown", "")
        out_line = "\n".join(out_lines)
    
    return out_line[:-1].split("\n")


def process_line(line, extra_tags):
    line = re.sub(" *#.*$", "", line) # remove comments
    
    line = re.sub("-$", "", line)
    
    if not " " in line or re.match(".*[а-яіїєґА-ЯІЇЄҐ] /.*", line):
        out_line = line
    elif re.match("^[^ ]+ [^ ]+ [^:]?[a-z].*$", line):
        out_line = line
    elif re.match("^[^ ]+ [:^<a-z0-9_].*$", line):
        out_line = re.sub("^([^ ]+) ([^<a-z].*)$", "\\1 \\1 \\2", line)
    else:
        print("hit-", line, file=sys.stderr)
        base = re.findall("^[^ ]+", line)[0]
        out_line = re.sub("([^ ]+) ?", "\\1 " + base + " unknown" + extra_tags + "\n", line)
        return out_line[:-1]
    
    #  if extra_tags != "" and not re.match(".* [a-z].*$", out_line):
    if extra_tags != "" and (not re.search(" [:a-z]", out_line) or "g=" in out_line):
        extra_tags = " " + extra_tags
    elif line.startswith(" +"):
        extra_tags = ""
      
    if "|" in out_line:
        out_line = out_line.replace("|", extra_tags + "|")
    
    #  if not "/" in out_line and not re.match("^[^ ]+ [^ ]+ [^ ]+$", out_line + extra_tags):
    #    print("bad line:", out_line + extra_tags, file=sys.stderr)
    
    #  if len(out_line)> 100:
    #      print(out_line, file=sys.stderr)
    #      sys.exit(1)
    
    out_line = out_line + extra_tags
    if " \ " in out_line:
        out_line = out_line.replace(" \\ ", " ") + " \\"
    elif " \:" in out_line:
        out_line = out_line.replace(" \\:", ":") + " \\"
      
    return out_line



extra_tag_map = {
  "base-abbr.lst": ":abbr",
  "dot-abbr.lst": ":abbr",
  "twisters.lst": ":bad",
  "ignored.lst": ":bad",
  "rare.lst": ":rare",
  "slang.lst": ":slang",
  "alt.lst": ":alt"
}


def process_input(files):
    out_lines = []
    for filename in files:
    
        fn = re.sub(".*/", "", filename)
        if fn in extra_tag_map:
            extra_tags = extra_tag_map[fn]
        else:
            extra_tags = ""
    
        with open(filename, "r", encoding="utf-8") as f:
            for line in f:
            
                line = line.rstrip()
                xline = line.lstrip()
                if xline == "" or (xline.startswith("#") and not xline.startswith("# !")):
                    continue
                
                if line.startswith(" +"):
                    if extra_tags:
                        line += " " + extra_tags
                    out_lines.append( line )
                    continue
            
                if filename.endswith( "exceptions.lst" ):
                    lines = process_line_exceptions(line)
                    if lines:
                        out_lines.extend( lines )
                else:
                    out_line = process_line(line, extra_tags)
                    if out_line.strip():
                        out_lines.append( out_line )
    
    return out_lines
    


if __name__ == "__main__":

    out_lines = process_input(sys.argv[1:])
    for out_line in out_lines:
        print(out_lines)


#!/usr/bin/python3

# -*- coding: utf-8 -*-

import sys
import re
import os
import logging


logger = logging.getLogger("affix")
#logger.setLevel(logging.DEBUG)

class PrefixGroup(object):
    #@profile
    def __init__(self, match_):
        self.match = match_
        self.affixes = []
        self.match_start_re = re.compile("^"+match_)
        self.counter = 0

    def matches(self, word):
        return self.match_start_re.match(word)

class SuffixGroup(object):
    #@profile
    def __init__(self, match_, neg_match_=None):
        self.match = match_
        self.neg_match = neg_match_
        self.affixes = []
        try:
            self.match_ends_re = re.compile(match_+"$")
            self.neg_match_ends_re = re.compile(neg_match_+"$") if neg_match_ else None
        except:
            print("Failed to compile match " + match_+"$", file=sys.stderr)
            raise
        self.counter = 0

    def matches(self, word):
        return self.match_ends_re.search(word) \
          and (self.neg_match == None or not self.neg_match_ends_re.search(word))


def convert0(part):
  if part == "0":
    return ""
  return part


class Prefix(object):

    #@profile
    def __init__(self, from_, to_, tags_):
        self.fromm = convert0(from_)
        self.to = convert0(to_)
        self.tags = tags_   # optional tags field for POS dictionary
        
        self.sub_from_len = len(self.fromm)
#          self.sub_from_pfx = re.compile("^"+self.fromm)

    def apply(self, word):
        return self.to + word[self.sub_from_len:]


class Suffix(object):

    #@profile
    def __init__(self, from_, to_, tags_):
        self.fromm = convert0(from_)
        self.to = convert0(to_)
        self.tags = tags_   # optional tags field for POS dictionary
        self.sub_from_len = -len(self.fromm) if self.fromm != "" else 100
        self.sub_from_sfx = re.compile(self.fromm+"$")

    def apply(self, word):
#      return word[:self.sub_from_len] + self.to
#      print("applying:", self.sub_from_sfx, self.to, word, file=sys.stderr)
      ret, repl_cnt = re.subn(self.sub_from_sfx, self.to, word)
      if repl_cnt == 0:
           raise Exception("Failed to apply {} -> {} to {}".format(self.fromm, self.to, word))
      return ret


prefixes = []
affixMap = {}


#@profile
def expand_prefixes(word, affixFlags):
    words = [ word ]

    for affixFlag in affixFlags:
      if affixFlag not in prefixes:
        continue
          
      appliedCnt = 0
      affixGroupMap = affixMap[affixFlag]
      for affixGroup in affixGroupMap.values():
        if affixGroup.matches(word):
            for affix in affixGroup.affixes:
#                wrd = affix.sub_from_pfx.sub(affix.to, word)
                wrd = affix.apply(word)
                words.append( wrd )
                appliedCnt += 1
            affixGroup.counter += 1

      if appliedCnt == 0:
        print("WARNING: Flag", affixFlag, "not applicable to", word, file=sys.stderr)
    
    return words


re_alts_slash = re.compile("^([^/]+:)([^:]+)(:[^/]+)?$")
re_alts_vert = re.compile("^(.* )(.*)$")
re_alts_dbl_slash = re.compile("^(.* .+?:)((?:.:(?:nv|v_...)(?:/(?:nv|v_...))*)(?://.:(?:nv|v_...)(?:/(?:nv|v_...))*)+)(:[^/]+)?$")

def expand_alts(lines, splitter):
  out = []

  for line in lines:

    if not splitter in line:
       out.append( line )
       continue
            
    if splitter == "/":
      groups = re_alts_slash.match(line).groups()
    elif splitter == "|":
      groups = re_alts_vert.match(line).groups()
    else:
      groups = re_alts_dbl_slash.match(line).groups()

    split1 = groups[1].split(splitter)
    base = groups[0]
    end = ""
    if len(groups) > 2 and groups[2] :
      end = groups[2]

    for split_ in split1:
      out.append( base + split_ + end )

  return out



def log_usage():
    for affixFlag, affixGroups in affixMap.items():
      print(affixFlag, ":", len(affixGroups), file=sys.stderr)
      for match, affixGroup in affixGroups.items():
          print("\t", match, ":", affixGroup.counter, "\t\t(", len(affixGroup.affixes), ")", file=sys.stderr)


re_whitespace=re.compile("[ \t]+")

def load_affix_file(aff_file):
  for line in aff_file:

    line = line.strip()

    is_pfx = line.startswith("PFX")

    if line == "" or line[0] == "#":
        continue

    if "group " in line:
      affixFlag = line.split(" ")[1]
      affixGroupMap = {}
      affixMap[ affixFlag ] = affixGroupMap
      continue

    if line.endswith(":"):
      match = line[:-1]
      if is_pfx:
        affixGroup = PrefixGroup(match)
      else:
        if " -" in match:
          match1, neg_match = match.split(" -")
#          print("xx", match, "yy", match1, neg_match)
          affixGroup = SuffixGroup(match1[:-1], neg_match)
        else:
          affixGroup = SuffixGroup(match)

      if match in affixGroupMap:
        print("WARNING: overlapping match", match, "in", affixFlag, ":\n\t", line, file=sys.stderr)
        affixGroup = affixGroupMap[match]
      else:
        affixGroupMap[match] = affixGroup
      continue


    halfs = line.split("@")
    affixes = halfs[0].strip()

    if "#" in affixes:
      affixes = affixes.split("#")[0].strip()

    parts = re_whitespace.split(affixes)

    if len(parts) > 2:
      match = parts[2]
      if not match in affixGroupMap:
        affixGroup = SuffixGroup(match)
        affixGroupMap[match] = affixGroup
      else:
        affixGroup = affixGroupMap[match]


    if len(parts) > 3:
      print("WARNING: extra fields in suffix description", parts, file=sys.stderr)

    if len(halfs) > 1:
        tags = halfs[1].strip()
    else:
        tags = ""
        if not is_pfx:
            print("Empty tags", line, file=sys.stderr)

    fromm = parts[0]
    to = parts[1]

    if is_pfx:
        affixObj = Prefix(fromm, to, tags)
    else:
        affixObj = Suffix(fromm, to, tags)

    affixGroup.affixes.append(affixObj)





#@profile

def check_files(dirname, aff_files):
    verb_affix_files = {}
    for aff_filename in aff_files:
        if aff_filename.startswith("v"):
            verb_affix_files[aff_filename] = os.path.getmtime(os.path.join(dirname, aff_filename))
    
    if verb_affix_files["v.aff"] > verb_affix_files["vr.aff"] \
            or verb_affix_files["v_advp.aff"] > verb_affix_files["vr_advp.aff"]:
        raise Exception("Reverse verb affix files older than the direct ones!")


def load_affixes(filename):

    
    if os.path.isdir(filename):
        aff_files = [ f for f in os.listdir(filename) if os.path.isfile(os.path.join(filename, f)) and f.endswith(".aff") ]
        print("Loading affixes from directory", filename, file=sys.stderr)

        check_files(filename, aff_files)
    
        for aff_filename in aff_files:
            aff_filename = os.path.join(filename, aff_filename)
            with open(aff_filename, "r", encoding="utf-8") as aff_file:
                load_affix_file(aff_file)
                
    else:
        with open(filename, "r", encoding="utf-8") as aff_file:
            load_affix_file(aff_file)
    
    
    if len(affixMap) == 0:
        print("ERROR: Failed to load affixes from", filename, file=sys.stderr)
        sys.exit(1)
    
    #  print("Loaded", len(affixMap), "affixes", ", prefixes:", prefixes, file=sys.stderr)
    #  logger.debug("Loaded: %s", affixMap)
    print("Loaded: ", affixMap.keys(), file=sys.stderr)


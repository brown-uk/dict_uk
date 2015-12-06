#!/usr/bin/env python3

"""
Created on Aug 29, 2015

@author: arysin
"""

import re
import sys

class VerbReverse(object):
    """
    Generates affix file for reverse verb forms 
    """


    def __init__(self):
        """
        Constructor
        """
        self.RE_GENERIC = re.compile(".*([аяийіїоуюв]|те)$")
        self.RE_ADVP = re.compile(".*[чш]и$")
        
    def generate_rev(self, line):
        line = line.strip()

        line_parts = line.split("#")
        left = line_parts[0]
        
        if len(line_parts) > 1:
            comment = line_parts[1]
        else:
            comment = ""

        if "group v" in left:
            line = line.replace("group v", "group vr")
            if left.startswith("group v"):
                line += "\n\n# Зворотня форма дієслів (-ся та -сь)\n"
                line += "ся\tсь	тися		#  ~тися  ~тись    @ verb:rev:inf\n\n"

            return line

        if left.strip().endswith(":"):
            return line.replace("ти:", "тися:")
          
        columns = left.split()
          
        if len(columns) < 2:
            return line
          
        dual = False
        if "advp" in comment:
            suff = "сь"
        elif re.match(".*те$", columns[1]) and ":s:3" in comment:
            suff = "ться"
        elif self.RE_GENERIC.match(columns[1]):
            dual = True
            suff = "ся"
        elif re.match(".*[еє]$", columns[1]):
            suff = "ться"
        else:
            suff = "ся"
          
        columns = self.convert_left(columns, suff)
        
        comment = self.convert_comment(comment, suff)
        

        out_line = "\t".join(columns) + "\t\t#\t\t" + comment
        
        if dual:
            columns[1] = re.sub("ся$", "сь", columns[1])

            out_line += "\n"
            comment = comment.replace("ся\t", "сь\t")
            out_line += "\t".join(columns) + "\t\t#\t\t" + comment
        
        return out_line
        
        
    def convert_comment(self, comment, suff):
        comment = comment.replace("verb:", "verb:rev:")
        comment = comment.replace("advp:", "advp:rev:")
        
        columns = comment.split("@")
        left_cols = columns[0].split()
        
        left_cols[0] += suff
        left_cols[1] += suff
        
        comment = "\t".join(left_cols) + "\t\t@" + columns[1]
        
        return comment
        
        
    def convert_left(self, columns, suff):
        if columns[0] == "0":
            columns[0] = "ся"
        else:
            columns[0] += "ся"

        if columns[1] == "0":
            columns[1] = "ся"
        else:
            columns[1] += suff
        
        if len(columns) > 2:
            columns[2] += "ся"
        
        return columns
          


if __name__ == "__main__":

    cnv = VerbReverse()

    convert = True
    for line in sys.stdin:
        
        if convert:
            line = cnv.generate_rev(line)
            print(line)
        else:
            if "ся " in line:
                print(line)

        # v5 is special - don't generate reverse affixes for it
        if "group " in line:
            convert = not " v5" in line

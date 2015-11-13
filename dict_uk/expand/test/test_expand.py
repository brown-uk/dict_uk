#!/usr/bin/python3

import os
import sys
import unittest
import difflib


class ExpandTest(unittest.TestCase):
    in_file = "test.lst"
    out_file = "test.lst.tag"

    def test_expand(self):
        # TODO: use python module directly
        res = os.system("../expand.py -aff ../../../data/affix -corp -indent -mfl < " + self.in_file + " > " + self.out_file)
        self.assertEqual(0, res)

        with open("prev/" + self.out_file, "r", encoding="utf-8") as ff:
            fromlines = ff.readlines()
        with open(self.out_file, "r", encoding="utf-8") as tf:
            tolines = tf.readlines()
        diff = difflib.unified_diff(fromlines, tolines, fromfile="prev/"+self.out_file, tofile=self.out_file)
        self.assertEqual("", "".join(diff))
        
        os.remove(self.out_file)


if __name__ == '__main__':
    unittest.main()

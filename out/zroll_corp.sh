#!/bin/sh

# Rotates all newely generated files into prev/ directory for the following regression checking

FILE1="dict_corp_lt.txt"
FILE2="dict_corp_vis.txt"

#mv -f *.dups *.uniq prev/
rm -f prev/$FILE1 ; cp -f $FILE1 prev/
rm -f prev/$FILE2 ; cp -f $FILE2 prev/
rm -f prev/words_spell.txt; cp -f words_spell.txt prev/
mv words.txt lemmas.txt prev/
mv dict_stats.txt stats/

#!/bin/sh

# Rotates all newely generated files into prev/ directory for the following regression checking

FILE1="dict_corp_lt.txt"
FILE2="dict_corp_vis.txt"

#mv -f *.dups *.uniq prev/
cp -f $FILE1 prev/
cp -f $FILE2 prev/
cp -f words_spell.txt prev/
mv words.txt lemmas.txt tags.txt prev/
mv dict_stats.txt stats/

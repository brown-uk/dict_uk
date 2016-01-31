#!/bin/sh

# Rotates all newely generated files into prev/ directory for the following regression checking

FILES="dict_corp_vis.txt dict_corp_lt.txt"

#mv -f *.dups *.uniq prev/
cp -f $FILES prev/
mv words.txt lemmas.txt tags.txt words.txt tags.txt prev/
mv dict_stats.txt stats/

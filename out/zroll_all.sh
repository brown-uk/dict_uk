#!/bin/sh

# Rotates all newely generated files into prev/ directory for the following regression checking

FILE="dict_corp_vis.txt dict_corp_lt.txt"

mv -f $FILE prev/$FILE
mv dict_stats.txt stats/

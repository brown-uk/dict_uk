#!/bin/sh

BASE="../.."
time groovy -cp $BASE/src/main/resources $BASE/src/main/groovy/org/dict_uk/tools/Stemmer.groovy $BASE && \
diff -U 1 dict_corp_vis.txt.roots.prev dict_corp_vis.txt.roots > dict_corp_vis.txt.roots.diff
grep -v " - .* " dict_corp_vis.txt.roots > dict_corp_vis.txt.roots1

diff -U 1 dict_corp_vis.txt.roots1.prev dict_corp_vis.txt.roots1 > dict_corp_vis.txt.roots1.diff

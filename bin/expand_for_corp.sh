#!/bin/sh
#
# Generates tagged dictionary (both LT and visual versions) for the corpus
#


function diff_u() {
    FIL="$1"
    diff -u prev/$FIL $FIL > $FIL.diff
    return $?
}


BASE=$(dirname $0)/..
CODE_BASE=$BASE/dict_uk
OUT_DIR=$BASE/out

FLAGS="-corp -indent -mfl -wordlist"
FLAGS="$FLAGS -stats -time --log-usage"

DICT_CORP_VIS=dict_corp_vis.txt
DICT_CORP_LT=dict_corp_lt.txt

cd $OUT_DIR

$CODE_BASE/expand/expand_all.py -aff $BASE/data/affix -dict $BASE/data/dict $FLAGS && \
(echo "Diffing..."; diff_u $DICT_CORP_VIS; diff_u $DICT_CORP_LT; diff_u words.txt; diff_u lemmas.txt; diff_u tags.txt; diff_u word_list.txt; /bin/true) && \
diff stats/dict_stats.txt dict_stats.txt > dict_stats.txt.diff

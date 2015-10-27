#!/bin/sh

BASE=..
CODE_BASE=$BASE/dict_uk
OUT_DIR=$BASE/out

FLAGS="-corp -indent --log-usage"
FLAGS="$FLAGS -stats -mfl"

DICT_CORP_VIS=dict_corp_vis.txt
DICT_CORP_LT=dict_corp_lt.txt

cd $OUT_DIR

$CODE_BASE/expand/expand.py -aff $BASE/data/affix $FLAGS $@ < uk_words.lst > $DICT_CORP_VIS && \
(echo "Diffing..."; diff prev/$DICT_CORP_VIS $DICT_CORP_VIS > $DICT_CORP_VIS.diff; diff -u prev/$DICT_CORP_LT $DICT_CORP_LT > $DICT_CORP_LT.diff)

diff stats/dict_stats.txt dict_stats.txt > dict_stats.txt.diff


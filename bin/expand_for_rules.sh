#!/bin/sh

BASE=..
CODE_BASE=$BASE/dict_uk
OUT_DIR=$BASE/out

FLAGS=""

cd $OUT_DIR

DICT_RULES_LT=dict_rules_lt.txt

#FLAGS="-mfl"

$CODE_BASE/expand.py -aff $BASE/data/affix $FLAGS $@ < uk_words.lst > $DICT_RULES_LT && \
(echo "Diffing..."; diff prev/$DICT_RULES_LT $DICT_RULES_LT > $DICT_RULES_LT.diff)

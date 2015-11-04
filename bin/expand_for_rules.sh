#!/bin/sh
#
# Generates tagged dictionary (both LT version) for the grammar/style rule check
#

BASE=$(dirname $0)/..
CODE_BASE=$BASE/dict_uk
OUT_DIR=$BASE/out

FLAGS="-time"
#FLAGS="-mfl"

cd $OUT_DIR

DICT_RULES_LT=dict_rules_lt.txt


$CODE_BASE/expand/expand_all.py -aff $BASE/data/affix -dict $BASE/data/dict $FLAGS > $DICT_RULES_LT && \
(echo "Diffing..."; diff prev/$DICT_RULES_LT $DICT_RULES_LT > $DICT_RULES_LT.diff)

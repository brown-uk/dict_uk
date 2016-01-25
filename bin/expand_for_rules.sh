#!/bin/sh

#
# Generates tagged dictionary (both LT version) for the grammar/style rule check
#

BASE=$(dirname $0)/..
CODE_BASE=$BASE/dict_uk
OUT_DIR=$BASE/out

FLAGS="-time -wordlist"
#FLAGS="-mfl"

cd $OUT_DIR

DICT_RULES_LT=dict_rules_lt.txt


time $CODE_BASE/expand/expand_all.py -aff $BASE/data/affix -dict $BASE/data/dict $FLAGS && \
(echo "Diffing..."; diff prev/$DICT_RULES_LT $DICT_RULES_LT > $DICT_RULES_LT.diff)

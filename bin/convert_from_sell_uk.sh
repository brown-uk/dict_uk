#!/bin/sh

#
# Converts source dictionary files from spell-uk project to dict_uk
#

SPELL_UK_DIR=../../spell-uk
DICT_SRC_DIR=$SPELL_UK_DIR/src/Dictionary

TAGS_ONLY=`grep "^TAGS_ONLY" $DICT_SRC_DIR/dictionaries.mk.inc | sed -r "s/TAGS_ONLY=//"`
INDICT=`grep -E "^INDICT" $DICT_SRC_DIR/dictionaries.mk.inc | sed -r "s/INDICT=//"`

CODE_BASE=$(dirname $0)/../dict_uk/tools/convert
OUT_DIR=$(dirname $0)/../data/dict

echo "Files: $INDICT $TAGS_ONLY"

for file in $INDICT $TAGS_ONLY; do
    FLAGS=""
    [ $file == "exceptions.lst" ] && FLAGS="-nosort"
    $CODE_BASE/convert.py $FLAGS < $DICT_SRC_DIR/$file > $OUT_DIR/$file || exit 1
done

$CODE_BASE/convert.py -comp < $DICT_SRC_DIR/composite.lst > $OUT_DIR/composite.lst

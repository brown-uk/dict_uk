#!/bin/sh

SPELL_UK_DIR=../../spell-uk
DICT_SRC_DIR=$SPELL_UK_DIR/data/Dictionary

TAGS_ONLY=`grep "^TAGS_ONLY" $DICT_SRC_DIR/dictionaries.mk.inc | sed -r "s/TAGS_ONLY=//"`
INDICT=`grep -E "^INDICT" $DICT_SRC_DIR/dictionaries.mk.inc | sed -r "s/INDICT=//"`

CODE_BASE=../dict_uk/tools/convert
OUT_DIR=../data/dict

echo "Files: $INDICT $TAGS_ONLY"

for file in $INDICT $TAGS_ONLY; do
    FLAGS=""
    [ $file == "exceptions.lst" ] && FLAGS="-nosort"
    $CODE_BASE/convert.py $FLAGS < $DICT_SRC_DIR/$file > $OUT_DIR/$file || exit 1
done

$CODE_BASE/convert.py -comp < $DICT_SRC_DIR/composite.lst > $OUT_DIR/composite.lst

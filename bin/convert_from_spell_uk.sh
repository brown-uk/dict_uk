#!/bin/sh

#
# Converts source dictionary files from spell-uk project (https://github.com/arysin/spell-uk) to dict_uk
#

SPELL_UK_DIR=../../spell-uk
DICT_SRC_DIR=$SPELL_UK_DIR/src/Dictionary

if [[ `uname` == "Darwin" ]]; then  # Mac
    type gsed >/dev/null 2>&1 || { echo >&2 "Please install GNU version of sed (gsed) e.g. \"brew install gnu-sed\""; exit 1; }
    SED="gsed"
else
    SED="sed"
fi
    

TAGS_ONLY=`grep "^TAGS_ONLY" $DICT_SRC_DIR/dictionaries.mk.inc | $SED -r "s/TAGS_ONLY=//"`
INDICT=`grep -E "^INDICT" $DICT_SRC_DIR/dictionaries.mk.inc | $SED -r "s/INDICT=//"`

CODE_BASE=$(dirname $0)/../dict_uk/tools/convert
OUT_DIR=$(dirname $0)/../data/dict

to_check=`ls -1 $DICT_SRC_DIR/*.lst | grep -v composite.lst`

grep -Ei "^[^#]*[а-яіїєґ] *$" $to_check && {
    exit 1
}

grep -Ei "[a-z]'?[а-яіїєґ]|[а-яіїєґ]['-]?[a-z]" $DICT_SRC_DIR/*.lst && {
    exit 1
}



echo "Files: $INDICT $TAGS_ONLY"

for file in $INDICT $TAGS_ONLY; do
    FLAGS=""
    ([ $file == "exceptions.lst" ] || [ $file == "pronouns.lst" ] || [ $file == "lang.lst" ]) && FLAGS="-nosort"
    $CODE_BASE/convert.py $FLAGS < $DICT_SRC_DIR/$file > $OUT_DIR/$file || exit 1
done

$CODE_BASE/convert.py -comp < $DICT_SRC_DIR/composite.lst > $OUT_DIR/composite.lst

#!/bin/sh

# TODO gen_verb_rev

BASE=..
CODE_BASE=$BASE/dict_uk

OUT_DIR=$BASE/out
OUT_FILE=$OUT_DIR/uk_words.lst

echo -n > $OUT_FILE
for file in `ls $BASE/data/dict/*.lst`; do
  echo "Processing $file"
  if echo $file | grep -q "composite.lst"; then
      $CODE_BASE/expand/expand_comps.py -aff $BASE/data/affix < $file >> $OUT_FILE
  else
      $CODE_BASE/expand/tagged_wordlist.py $file >> $OUT_FILE
  fi
  [ "$?" == 0 ] || exit 1
done

diff $OUT_DIR/prev/uk_words.lst $OUT_DIR/uk_words.lst > $OUT_DIR/uk_words.lst.diff

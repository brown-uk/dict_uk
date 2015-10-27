#!/bin/sh

BASE=../../..

FILE=test.lst
CORP_LT_FILE=dict_corp_lt.txt

rm -f $FILE.tag.diff
../expand.py -aff $BASE/data/affix -corp -indent -mfl $@ < $FILE > $FILE.tag &&\
diff -u prev/$FILE.tag $FILE.tag > $FILE.tag.diff
diff -u prev/$CORP_LT_FILE $CORP_LT_FILE > $CORP_LT_FILE.diff

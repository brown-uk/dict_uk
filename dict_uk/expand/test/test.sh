#!/bin/sh

BASE=../../..

FILE=test.lst
CORP_LT_FILE=dict_corp_lt.txt
CORP_VIS_FILE=dict_corp_vis.txt
RULES_LT_FILE=dict_rules_lt.txt

function diff_u() {
    FIL="$1"
    diff -u prev/$FIL $FIL > $FIL.diff
    return $?
}


rm -f $FILE.tag.diff
../expand.py -aff $BASE/data/affix -corp -indent -mfl --uncontr -stats -wordlist < $FILE > $CORP_VIS_FILE &&\
diff -u prev/$CORP_VIS_FILE $CORP_VIS_FILE > $CORP_VIS_FILE.diff && \
diff -u prev/$CORP_LT_FILE $CORP_LT_FILE > $CORP_LT_FILE.diff && \
diff_u lemmas.txt && \
diff_u words.txt && \
diff_u tags.txt && \
echo "No regressions" && rm $CORP_VIS_FILE $CORP_LT_FILE *.diff

echo
echo

../expand.py -aff $BASE/data/affix -mfl < $FILE > $RULES_LT_FILE && \
diff -u prev/$RULES_LT_FILE $RULES_LT_FILE > $RULES_LT_FILE.diff && \
echo "No regressions" && rm $RULES_LT_FILE*

#!/bin/sh

BASE=../../..

FILE=test.lst
CORP_LT_FILE=dict_corp_lt.txt

function diff_u() {
    FIL="$1"
    diff -u prev/$FIL $FIL > $FIL.diff
    return $?
}


rm -f $FILE.tag.diff
../expand.py -aff $BASE/data/affix -corp -indent -mfl -stats -wordlist < $FILE > $FILE.tag &&\
diff -u prev/$FILE.tag $FILE.tag > $FILE.tag.diff && \
diff -u prev/$CORP_LT_FILE $CORP_LT_FILE > $CORP_LT_FILE.diff && \
diff_u lemmas.txt && \
diff_u words.txt && \
diff_u tags.txt && \
echo "No regressions" && rm $FILE.tag $CORP_LT_FILE *.diff

../expand.py -aff $BASE/data/affix -mfl < $FILE > ${FILE}_rules.tag && \
diff -u prev/${FILE}_rules.tag ${FILE}_rules.tag > ${FILE}_rules.tag.diff && \
echo "No regressions" && rm $FILE.tag $CORP_LT_FILE *.diff

#!/bin/sh

BASE=../..

FILE=test.lst
CORP_LT_FILE=dict_corp_lt.txt
CORP_VIS_FILE=dict_corp_vis.txt
RULES_LT_FILE=dict_rules_lt.txt

function diff_u() {
    FIL="$1"
    diff -u prev/$FIL $FIL > $FIL.diff
    return $?
}

function run_expand() {
  echo "Running $@"
  JAVA_HOME=/usr groovy -cp $BASE/build:$BASE/src/main/groovy $BASE/src/main/groovy/org/dict_uk/expand/ExpandAll.groovy $@
}

OPTS="-corp -indent -mfl --uncontr -stats -wordlist"

echo "Diffing..."

rm -f *.diff
#run_expand -aff $BASE/data/affix -dict . $OPTS && \
diff -u prev/dict_corp_vis.txt dict_corp_vis.txt > dict_corp_vis.txt.diff
diff -u prev/dict_corp_lt.txt dict_corp_lt.txt > dict_corp_lt.txt.diff
#diff_u lemmas.txt
diff_u words.txt
#diff_u tags.txt

#echo
#echo

#run_expand -aff $BASE/data/affix -dict . -mfl < $FILE > $RULES_LT_FILE && \
#diff -u prev/$RULES_LT_FILE $RULES_LT_FILE > $RULES_LT_FILE.diff && \
#echo "No regressions for rules" && rm $RULES_LT_FILE*

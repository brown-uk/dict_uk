#!/bin/sh

BASE=..

FILE=test.lst
CORP_LT_FILE=dict_corp_lt.txt
CORP_VIS_FILE=dict_corp_vis.txt

function diff_u() {
    FIL="$1"
    diff -u prev/$FIL $FIL > $FIL.diff
    return $?
}

PWD=`pwd`

rm -f *.diff
cd $BASE && \
gradle testExpand
cd -
./test_diff.sh

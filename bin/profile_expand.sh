#!/bin/sh

BASE=$(dirname $0)/..
CODE_BASE=$BASE/dict_uk
CODE_BASE_PROF=$BASE/dict_uk.prof
OUT_DIR=$BASE/out

FLAGS="-corp -indent -aff ../data/affix"

cd $OUT_DIR

/bin/true && {
    echo "Profiling..."
    rm -rf $CODE_BASE_PROF
    cp -r $CODE_BASE $CODE_BASE_PROF
    sed -ir "s/^#@profile/@profile/" $CODE_BASE_PROF/expand/expand.py && \
    sed -ir "s/^#@profile/@profile/" $CODE_BASE_PROF/expand/util.py && \
    head -n 5000 $OUT_DIR/uk_words.lst > $CODE_BASE_PROF/uk_words.lst
    python3 /usr/lib64/python3.4/site-packages/kernprof.py -l -v $CODE_BASE_PROF/expand/expand.py $FLAGS < $CODE_BASE_PROF/uk_words.lst > /dev/null && \
    python3 /usr/lib64/python3.4/site-packages/line_profiler.py expand.py.lprof > $OUT_DIR/prof/expand.py.lprof.txt
    rm expand.py.lprof
    rm -rf $CODE_BASE_PROF
    exit
}


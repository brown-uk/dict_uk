#!/bin/sh

SRC=../../src/main/
PKG=groovy/org/dict_uk
groovy -cp $SRC/groovy $SRC/$PKG/tools/adjp_impers_check.groovy < ../dict_corp_lt.txt > mismatch.txt

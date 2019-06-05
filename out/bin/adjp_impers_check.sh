#!/bin/sh

SRC=../../src/tools/
CP=../../src/main/
PKG=groovy
groovy -cp $CP/groovy $SRC/$PKG/dict/adjp_impers_check.groovy < ../dict_corp_lt.txt > mismatch.txt

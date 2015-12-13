#!/bin/sh

groovy -cp ../scripts/common ../scripts/tools/adjp_impers_check.groovy < dict_corp_lt.txt > mismatch.txt

#!/bin/sh

#
# Generate short description summary for affix groups
#


BASE=$(dirname $0)/..
OUT_FILE=$BASE/doc/affix_groups.txt

AFF_FILES="a.aff n1.aff n2.aff n2n.aff n3.aff n4.aff np.aff n_patr.aff numr.aff v_advp.aff v.aff v_impers.aff vr.aff vr_advp.aff"
AFF_DIR=$BASE/data/affix

echo -n "" > $OUT_FILE
for f in $AFF_FILES; do
    echo -e "Файл: $f:\n" >> $OUT_FILE
    grep group -B 2 $AFF_DIR/$f | grep -vE "^$" | sed -r "s/--//" >> $OUT_FILE
    echo -e "\n\n" >> $OUT_FILE
done



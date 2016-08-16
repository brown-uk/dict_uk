#!/bin/sh

FILE="uk_wordlist.xml"
#INFILE="counts10.txt"
INFILE="counts2.txt"

echo '<wordlist locale="uk_UA" description="Ukrainian" date="1419214833" version="1">' > $FILE

max=`awk '{ print log($2/10)/log(10.); exit }' $INFILE`
echo "max: $max"

#awk -v max=$max '{ printf("<w f=\"%.0f\" flags=\"\">%s</w>\n", 255 * $2 / max, $1) }' $INFILE  >> $FILE
grep -E " [1-9][0-9]+" $INFILE | awk -v max=$max '{ printf("<w f=\"%.0f\" flags=\"\">%s</w>\n", log($2/10)/log(10.) * 255 / max, $1) }'  >> $FILE

echo '</wordlist>' >> $FILE

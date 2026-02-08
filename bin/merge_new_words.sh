#!/bin/sh
DIR="../.."

for file in geo-other; do
  grep "#=> $file" $DIR/new_words.lst | grep -vE '^$' | sed -r "s/ *#=> $file *//" >> ${file}.lst && \
done
grep -v "#=> " $DIR/new_words.lst | grep -vE '^$' >> base.lst && \
mv $DIR/new_words.lst $DIR/new_words.lst.old

#!/bin/sh
DIR="."

[ -f "$DIR/new_words.lst" ] || {
  echo "No file"
  exit 1
}
[ -f "data/dict/base.lst" ] || {
  echo "No data/dict/base.lst"
  exit 1
}

for file in geo-other; do
  grep "#=> $file" $DIR/new_words.lst | grep -vE '^$' | sed -r "s/ *#=> $file *//" >> data/dict/${file}.lst
done

grep -v "#=> " $DIR/new_words.lst | grep -vE '^$' >> data/dict/base.lst && \
mv $DIR/new_words.lst $DIR/new_words.lst.old

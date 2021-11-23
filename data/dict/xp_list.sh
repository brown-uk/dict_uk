#/bin/sh
grep -E "^[^#]*xp" *.lst | grep -v "base-abbr" |sort -t ':' -k 2 > xp_lst

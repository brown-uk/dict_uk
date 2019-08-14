## Створення словника для postgresql

### Копіювання файлів для словника 
```sh
cp build/hunspell/uk_UA.aff /usr/share/postgresql/9.6/tsearch_data/uk_UA.affix
cp build/hunspell/uk_UA.dic /usr/share/postgresql/9.6/tsearch_data/uk_UA.dict
cp ukrainian.stop /usr/share/postgresql/9.6/tsearch_data/ukrainian.stop
```

### Створення словника
```sql
CREATE TEXT SEARCH DICTIONARY ukrainian_huns (
    TEMPLATE = ispell,
    DictFile = uk_UA,
    AffFile = uk_UA,
    StopWords = ukrainian
);
```
### Створення словника стоп слів
```sql
CREATE TEXT SEARCH DICTIONARY ukrainian_stem (
    template = simple,
    stopwords = ukrainian
);
```
### Створення конфігурації
```sql
CREATE TEXT SEARCH CONFIGURATION ukrainian (PARSER=default);
```
### Налаштування конфігурації
```sql
ALTER TEXT SEARCH CONFIGURATION ukrainian ALTER MAPPING FOR  hword, hword_part, word WITH ukrainian_huns, ukrainian_stem;

ALTER TEXT SEARCH CONFIGURATION ukrainian ALTER MAPPING FOR  int, uint, numhword, numword, hword_numpart, email, float, file, url, url_path, version, host, sfloat WITH simple;

ALTER TEXT SEARCH CONFIGURATION ukrainian ALTER MAPPING FOR asciihword, asciiword, hword_asciipart WITH english_stem;
```
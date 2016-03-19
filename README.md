This is a project to generate POS tag dictionary for Ukrainian language.

Це — проект генерування словника з тегами частин мови для української мови.


Description:

    For all files in data/dict the project generates all possible word forms with POS tags
    by using affix rules from files in data/affix.


Required software:
    * java (>=1.8)
    * gradle


How to run:

    # gradle expandForCorp
    Output:

        * out/dict_corp_vis.txt - Dictionary in visual (indented) format for review, analysis or conversion
        * out/dict_corp_lt.txt - Dictionary in flat format (is used for preparing morfologik dictionary that can be used by LanguageTool)
        * out/words.txt - list of all unique known words
        * out/words_spell.txt - words valid for spelling
        * out/lemmas.txt - list of unique lemmas
        * out/tags.txt - list of unique tags

This is a project to generate POS tag dictionary for Ukrainian language.

Це — проект генерування словника з тегами частин мови для української мови.


Description:

    dict_uk/expand/expand_all.py -aff data/affix -dict data/dict

    For all files in data/dict the project genereates all possible word forms with POS tags
    by using affix rules from files in data/affix.


How to run:

    # dict_uk/expand/expand_all.py -aff data/affix -dict data/dict -corp -indent -mfl -wordlist
    Output:

        * dict_corp_vis.txt - Dictionary in visual (indented) format for review, analysis or conversion
        * dict_corp_lt.txt - Dictionary for LT for annotating the corpus
        * words.txt, lemmas.txt, tags.txt - list of all uniq words, lemmas and tags

    # dict_uk/expand/expand_all.py -aff data/affix -dict data/dict
    Output:

        * dict_rules_lt.txt - Dictionary file for LT (LanguageTool) used for grammar rules checking


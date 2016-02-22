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
        * out/dict_corp_lt.txt - Dictionary for LT for annotating the corpus
        * out/words.txt, out/lemmas.txt, out/tags.txt - list of all unique words, lemmas and tags

    # gradle expandForRules
    Output:

        * out/dict_rules_lt.txt - Dictionary file for LT (LanguageTool) used for grammar rules checking


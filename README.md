## Це — проект генерування словника з тегами частин мови для української мови. ##

## This is a project to generate POS tag dictionary for Ukrainian language. ##



### Опис ###
    Для всіх файлів в data/dict цей проект генерує всі можливі словоформи з тегами частин мови
    за допомогою правил афіксів у каталозі data/affix.


### Потрібні програмні засоби ###
* java (>=1.8)
* gradle


### Застосування ###
    зі словником можна робити дві речі:
1. згенерувати всі можливі словоформи для слів, що вже є в словнику (див. параграф «Як запускати» нижче)
2. генерувати форми для довільних слів в інтерактивному режимі: [докладніше](blob/master/doc/interactive_mode.md)


### Як запускати ###

    `gradle expand`

    На виході:

* out/dict_corp_vis.txt - словник у візуальному форматі (з відступами, згрупований за лемами) для перегляду, аналізу і опрацьовування
* out/dict_corp_lt.txt - словник у табличному форматі для використання в ПЗ, зокрема з цього файлу генеруємо словник morfologik, що використовується в LanguageTool
* out/words.txt - список всіх відомих словоформ
* out/words_spell.txt - список всіх відомих словоформ, правильних з погляду правопису
* out/lemmas.txt - список лем
* out/tags.txt - список тегів




### Description ###
    For all files in data/dict the project generates all possible word forms with POS tags
    by using affix rules from files in data/affix.


### Required software ###
* java (>=1.8)
* gradle


### How to run ###
    `gradle expand`

    Output:

* out/dict_corp_vis.txt - Dictionary in visual (indented) format for review, analysis or conversion
* out/dict_corp_lt.txt - Dictionary in flat format (is used for preparing morfologik dictionary that can be used by LanguageTool)
* out/words.txt - list of all unique known words
* out/words_spell.txt - words valid for spelling
* out/lemmas.txt - list of unique lemmas
* out/tags.txt - list of unique tags

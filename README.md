## Це — великий електронний словник української мови (ВЕСУМ). ##

## This is a project to generate POS tag dictionary for Ukrainian language. ##



### Опис ###
    Словник містить слова та їхні парадигми з відповідними тегами, а також іншу інформацію,
    зокрема:
    * додаткові теги: slang, rare, bad...
    * пропоновані заміни для покручів
    * зв’язок між базовими та порівняльними формами прикметників
    * керування відмінками для прикметників

    Для всіх файлів в data/dict цей проект генерує всі можливі словоформи з тегами частин мови
    за допомогою правил афіксів у каталозі data/affix.


### Вимоги до програмних засобів ###
* java (JDK >= 8)


### Застосування ###
    зі словником можна робити дві речі:
1. згенерувати всі можливі словоформи для слів, що вже є в словнику (див. параграф «Як запускати» нижче)
2. генерувати форми для довільних слів в інтерактивному режимі: [докладніше](doc/interactive_mode.md)


### Як встановити ###
* Встановити java (JDK 8 або новішу)
* (Лише для Windows) встановити і запустити git bash
* Клонувати проект: `git clone https://github.com/brown-uk/dict_uk.git`
* Зайти в теку проекту: cd dict_uk

### Як запускати ###

    `./gradlew expand`
    
    або для Windows:
    
    `bin/expand_win.sh`

    На виході:

* out/dict_corp_vis.txt - словник у візуальному форматі (з відступами, згрупований за лемами) для перегляду, аналізу і опрацьовування
* out/dict_corp_lt.txt - словник у табличному форматі для використання в ПЗ, зокрема з цього файлу генеруємо словник morfologik, що використовується в LanguageTool
* out/words.txt - список всіх відомих словоформ
* out/words_spell.txt - список всіх відомих словоформ, правильних з погляду правопису
* out/lemmas.txt - список лем

### Ліцензія ###

Дані словника доступні для використання згідно з умовами ліцензії "Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License" (https://creativecommons.org/licenses/by-nc-sa/4.0/)
Програмні засоби вільно розповсюджується за умов ліцензії GPL версії 3.

Copyright (c) 2018 Андрій Рисін (arysin@gmail.com), Василь Старко, команда БрУК

### Похідні проекти ###

* [Словники morfologik](distr/morfologik-ukrainian/README.md)
* [Словники hunspell](distr/hunspell/README.md)
* [Словники Firefox](distr/mozilla/README.md)
* [Словники LibreOffice.org](distr/openoffice.org/README.md)


### Description ###
    For all files in data/dict the project generates all possible word forms with POS tags
    by using affix rules from files in data/affix.


### Required software ###
* java (JDK >= 8)


### How to run ###
    `./gradlew expand`

    or on Windows:

    `bin/expand_win.sh`

    Output:

* out/dict_corp_vis.txt - Dictionary in visual (indented) format for review, analysis or conversion
* out/dict_corp_lt.txt - Dictionary in flat format (is used for preparing morfologik dictionary that can be used by LanguageTool)
* out/words.txt - list of all unique known words
* out/words_spell.txt - words valid for spelling
* out/lemmas.txt - list of unique lemmas

### Building under docker ###

```
sudo docker build -t brown-uk/dict_uk .
sudo docker run -d --name dict_uk brown-uk/dict_uk /bin/bash
sudo docker cp dict_uk:/src/out/ ./out
sudo chown -R $USER: ./out
sudo docker stop dict_uk
```

### License ###

Dictionary data are distributed under "Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License" (https://creativecommons.org/licenses/by-nc-sa/4.0/)
Software is distributed under GPLv3.

Copyright (c) 2018 Andriy Rysin (arysin@gmail.com), Vasyl Starko, BrUK team

### Derivative Projects ###

* [morfologik dictionaries](distr/morfologik-ukrainian/README.md)
* [hunspell dictionaries](distr/hunspell/README.md)
* [Firefox dictionaries](distr/mozilla/README.md)
* [LibreOffice.org dictionaries](distr/openoffice.org/README.md)

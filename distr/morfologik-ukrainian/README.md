Цей проект генерує артефакти для репозитрію Maven (група ua.net.nlp).

morfologik-ukrainian-lt-{версія}.jar - словники у форматі Morfologik для модуля української в LanguageTool

Поширюється за умов LGPL

build.gradle генерує всі словники для LanguageTool і створює артефакт morfologik-ukrainian-lt

Щоб геренувати словники у форматі morfologik потрібно встановити модуль languagetool-tools з https://github.com/languagetool-org/languagetool
1. `git clone https://github.com/languagetool-org/languagetool`
2. `cd languagetool`
3. `./build.sh languagetool-tools install`

Щоб встановити словник(и) в LanguageTool потрібно додати змінну ltDir, що вказує на кореневий каталог сирців LanguageTool (через gradle.properties або командний рядок: `-PltDir=../../../langaugetool`)

Цей проект також геренує допоміжні словники для LanguageTool (ці словники є частиною сирців українсього модуля LanguageTool):
1. Словник замін для покручів (replace.txt)
2. Словник пропозицій замін для небажаних слів (replace_soft.txt)
3. Словник керування відмінками (case_government.txt)


morfologik-ukrainian-search-{версія}.jar - словник тегів у форматі Morfologik для повнотекстового пошуку (зокрема Apache Lucene)

Поширюється за умов Apache License 2.0.

build.nlp.gradle створює артефакт morfologik-ukrainian-search лише зі словником POS tag для повнотекстового пошуку

ЗАУВАГИ:
1. словоформи надані у нижньому регістрі
2. більшість додаткових тегів (coll, rare, slang тощо) перед генеруванням вилучаються
3. наразі словоформи, що мають лему власної назви, отримуть лему з великої летери,
наприклад: європи - Європа (це створює омонімічні позиції, прибл. 2 тис.)
4. наразі словоформи, що мають декілька лем створюють декілька позицій, що в свою чергу дає їм більший пошуковий рейтинг,
наприклад: `абазинці` має позицію з лемами `абазинка` і `абазинець` (це наразі близько 80 тис. позицій)

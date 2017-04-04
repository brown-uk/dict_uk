Цей проект генерує артефакти  для репозитрію Maven (група ua.net.nlp).

morfologik-ukrainian-lt-{версія}.jar - словники у форматі Morfologik для модуля української в LanguageTool
morfologik-ukrainian-search-{версія}.jar - словник тегів у форматі Morfologik для повнотекстового пошуку (зокрема Apache Lucene)

build.gradle генерує всі словники для LanguageTool і створює артефакт morfologik-ukrainian-lt
build.nlp.gradle створює артефакт morfologik-ukrainian-search лише зі словником POS tag для повнотекстового пошуку

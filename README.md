This is a project to generate POS tag dictionary for Ukrainian language.

Це — проект генерування словника з тегами частин мови для української мови.


Опис:

    dict_uk/expand/expand_all.py -aff data/affix -dict data/dict

    Для всіх файлів слів в data/dict за допомогою правил генерування афіксів в data/affix 
    згенерує всі можливі словоформи з тегами частин мови.


Виходові файли програми (каталог out/):

    * dict_rules_lt.txt - Файл для LT (LanguageTool) для перевірки правил
    * dict_corp_lt.txt - файл для LT для генерування тегів корпусу
    * dict_corp_vis.txt - файл для перегляду та опрацювання лем (корпусна версія словника)


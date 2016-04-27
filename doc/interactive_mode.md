Щоб швидко протестувати парпорці для нових слів можна запустити expand в інтерактивному режимі:

  gradle -q expandInteractive

потім на вхід подавати рядки, напр.:

річка /n10

Типовий вивід буде у форматі з відступом. Для табличного формату потрібно додати "-Pflat=true":

  gradle -q -Pflat=true expandInteractive


Для виходу набрати exit або Ctrl+C


Опис прапорців ось тут https://github.com/arysin/dict_uk/blob/master/doc/affix_groups.txt
Приклад вхідних рядків можна глянути тут https://github.com/arysin/dict_uk/blob/master/data/dict/slang.lst

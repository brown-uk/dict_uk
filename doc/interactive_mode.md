Щоб швидко протестувати парпорці для нових слів можна запустити expand в інтерактивному режимі:

  ./gradle -q expandInteractive
  
  або
  
  ./bin/afx

потім на вхід подавати рядки, напр.:

річка /n10

Типовий вивід буде у форматі з відступом. Для табличного формату потрібно додати "-Pflat=true":

  gradlew -q -Pflat=true expandInteractive
  
  або
  
  ./bin/afx -f


Для виходу набрати exit або Ctrl+C


Для групи слів згенерувати всі форми найкраще через канал:

  cat word_list.txt | gradle -q -Pflat=true expandInteractive > word_forms.txt


[Опис прапорців](../doc/affix_groups.txt)

[Приклад вхідних рядків](../data/dict/slang.lst)

# Оновлення словника для браузера Chrome


## Оновлення з останньої офіційної версії

* Стягнути архів https://github.com/brown-uk/dict_uk/releases/download/v4.9.0/chrome_dict_uk-UA-4.9.0.zip
* Розпакувати архів в каталог "Chrome/User Data":
** на Windows, c:\Users\<Ім'я користувача>\AppData\Local\Google\Chrome\User Data\)
** на Linux - $HOME/.config/google-chrome/Dictionaries



## Розробницька версія

### Стягнути сирці chromium

https://chromium.googlesource.com/chromium/src/+/master/docs/linux/build_instructions.md

### Побудувати convert_tool

Нам потрібні лише convert_tool, тому крок побудови має команду
```autoninja -C out/Default/ chrome/tools/convert_dict```

### Побудувати словник hunspell в dict_uk
```sh
../../gradlew -b ../hunspell/build.gradle hunspell
```

### Копіювати файли для словника в chromium
```sh
cp ../hunspell/build/hunspell/uk_UA.dic $CHROMIUM_DIR/src/third_party/hunspell_dictionaries/
grep -v "IGNORE" ../hunspell/build/hunspell/uk_UA.aff > $CHROMIUM_DIR/src/third_party/hunspell_dictionaries/uk_UA.aff
```

### Конвертувати словник hunspell у формат bdic
```sh
$CHROMIUM_DIR/src/chrome/tools/convert_dict/out/Default/convert_dict $CHROMIUM_DIR/src/third_party/hunspell_dictionaries/uk_UA
mv $CHROMIUM_DIR/src/third_party/hunspell_dictionaries/uk_UA.bdic $CHROMIUM_DIR/src/third_party/hunspell_dictionaries/uk-UA-3-0.bdic
```

### Скопіювати словник в каталог браузера
* Скопіювати файл uk-UA-3-0.bdic в каталог "Chrome/User Data":
** на Windows, c:\Users\<Ім'я користувача>\AppData\Local\Google\Chrome\User Data\)
** на Linux - $HOME/.config/google-chrome/Dictionaries
```sh
cp $CHROMIUM_DIR/src/third_party/hunspell_dictionaries/uk-UA-3-0.bdic $HOME/.config/google-chrome/Dictionaries
```
* перезапустити Chrome

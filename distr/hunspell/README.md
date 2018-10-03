## Проект, що генерує український словник для hunspell ##

### Як запускати ###

```sh
../../gradlew hunspell
```

Для Windows:

```sh
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
../../gradlew hunspell
```

Файли `uk_UA.aff` та `uk_UA.dic` будуть в каталозі `build/hunspell`.

Поширюється за умов ліцензії MPL (Mozilla Public License) 1.1

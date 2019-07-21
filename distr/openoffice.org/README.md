## Проект, що генерує український словник та словник синонімів для libreoffice ##

### Як запускати ###

```sh
../../gradlew oxt
```

Для Windows:

```sh
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
../../gradlew oxt
```

Файл `dict-uk_UA-<version>.oxt` будуть в каталозі `build/`.

Поширюється за умов ліцензії MPL (Mozilla Public License) 1.1

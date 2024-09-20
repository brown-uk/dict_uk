#!/bin/sh


function z_dep {
    ./gradlew $OPTS deployLtDict || exit 1

    V=`cat VERSION | head -n 1`
    echo "Dict version: \"$V\""
    cd  ../grammar/languagetool-test
    grep ">$V<" ../languagetool/languagetool-language-modules/uk/pom.xml || {
      echo "No \"$V\" in pom.xml"
      exit 1
    }
    ./zdep.sh
}


OPTS="--parallel"

[ -f "new_words.lst" ] && {
    echo "new_words.lst not merged!"
    exit 1
}

./gradlew $OPTS sortDict expand diff || exit 1

./gradlew checkDups checkReplacements

if [ "$1" == "-d" ]; then
    exec ./gradlew $OTPS deployLtDict
fi

if [ "$1" == "-z" ]; then
    sleep 1
    z_dep
fi

if [ "$1" == "-f" ]; then
    sleep 1
    z_dep && \
    ./test_all_bg.sh
fi

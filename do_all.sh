#!/bin/sh

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
    ./gradlew $OPTS deployLtDict || exit 1
    sleep 1
    cd  ../grammar/languagetool-test
    ./zdep.sh
fi

if [ "$1" == "-f" ]; then
    ./gradlew $OPTS deployLtDict || exit 1
    sleep 1
    cd  ../grammar/languagetool-test && \
    ./zdep.sh && \
    ./test_all_bg.sh
fi

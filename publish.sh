#!/bin/sh

grep SNAPSHOT VERSION && {
    echo "SNAPSHOT version"
    exit 1
}

./gradlew -p distr/morfologik-ukrainian publish

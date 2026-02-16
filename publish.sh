#!/bin/sh

grep SNAPSHOT VERSION && {
    echo "SNAPSHOT version"
    exit 1
}

./gradlew -p distr/morfologik-ukrainian publishToMavenLocal
#./gradlew -p distr/morfologik-ukrainian preparePublishZip

exit 0

version=$(cat VERSION)
cd "$HOME/.m2/repository"
# zip -b "$HOME/.m2/repository" "morfologik-ukrainian-lt-${version}.zip" "ua/net/nlp/morfologik-ukrainian-lt/$version/*"
zip -r "morfologik-ukrainian-lt-${version}.zip" "ua/net/nlp/morfologik-ukrainian-lt/$version/*"
cd ~

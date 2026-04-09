#!/bin/sh

grep SNAPSHOT VERSION && {
    echo "SNAPSHOT version"
    exit 1
}

#./gradlew --info -p distr/morfologik-ukrainian publishToMavenLocal
./gradlew --info -p distr/morfologik-ukrainian publish
#./gradlew -p distr/morfologik-ukrainian preparePublishZip

version=$(cat VERSION)
cd "$HOME/.m2/repository"
# zip -b "$HOME/.m2/repository" "morfologik-ukrainian-lt-${version}.zip" "ua/net/nlp/morfologik-ukrainian-lt/$version/*"
zip -r "morfologik-ukrainian-lt-${version}.zip" "ua/net/nlp/morfologik-ukrainian-lt/$version"
mv "morfologik-ukrainian-lt-${version}.zip" ~/
cd ~

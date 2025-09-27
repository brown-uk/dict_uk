FROM ubuntu:24.04

RUN apt update
RUN apt install -y openjdk-21-jdk-headless locales

ADD . /src
WORKDIR /src

RUN locale-gen uk_UA.UTF-8 && update-locale LANG=uk_UA.UTF-8
# RUN locale -a
# RUN LC_ALL=uk_UA.UTF-8 ls -al
RUN mkdir -p out/stats
RUN LC_ALL=uk_UA.UTF-8 ./gradlew expand

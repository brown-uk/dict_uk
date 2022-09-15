FROM ubuntu:20.04

RUN apt update
RUN apt install -y openjdk-11-jdk-headless
ADD . /src
WORKDIR /src
RUN ./gradlew expand

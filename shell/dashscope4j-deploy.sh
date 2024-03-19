#!/usr/bin/env bash

## define the GPG_TTY for input password
GPG_TTY="$(tty)"
export GPG_TTY

## maven clean & deploy to maven repo
mvn -f ../pom.xml -DskipTests clean deploy -P sonatype
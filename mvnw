#!/usr/bin/env sh
DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/c/Program Files/Java/jdk-25" ]; then
        export JAVA_HOME="/c/Program Files/Java/jdk-25"
    elif [ -d "C:/Program Files/Java/jdk-25" ]; then
        export JAVA_HOME="C:/Program Files/Java/jdk-25"
    fi
fi
exec "$DIR/.tools/apache-maven-3.9.9/bin/mvn" "$@"

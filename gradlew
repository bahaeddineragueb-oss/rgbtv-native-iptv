#!/bin/sh
#
# Gradle start-up script for POSIX shells.
# It is a thin launcher around gradle/wrapper/gradle-wrapper.jar.
#
set -e

PRG="$0"
# Resolve symlinks so the script can be linked into PATH.
while [ -h "$PRG" ] ; do
    ls=$(ls -ld "$PRG")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    case $link in
        /*) PRG="$link" ;;
        *)  PRG=$(dirname "$PRG")/"$link" ;;
    esac
done

APP_HOME=$(cd "$(dirname "$PRG")" > /dev/null && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD=java
fi

if ! command -v "$JAVACMD" > /dev/null 2>&1 ; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
    exit 1
fi

if [ ! -f "$CLASSPATH" ] ; then
    echo "ERROR: $CLASSPATH is missing." >&2
    echo "Generate it once with: gradle wrapper --gradle-version 8.10.2" >&2
    echo "(or simply open the project in Android Studio, which creates it for you)." >&2
    exit 1
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    "-Dorg.gradle.appname=gradlew" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"

#!/bin/sh
# Minimal wrapper launcher. gradle-wrapper.jar is a binary that could not be bundled
# (project was generated offline). Generate it once: gradle wrapper --gradle-version 8.11.1
APP_HOME=$(cd "$(dirname "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$JAR" ]; then
  echo "gradle-wrapper.jar missing. Run: gradle wrapper --gradle-version 8.11.1  (see BUILD_TERMUX.md)"; exit 1
fi
exec java -Dorg.gradle.appname=gradlew -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"

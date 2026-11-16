@echo off
set JAR=%~dp0gradle\wrapper\gradle-wrapper.jar
if not exist "%JAR%" (echo gradle-wrapper.jar missing. Run: gradle wrapper --gradle-version 8.11.1 & exit /b 1)
java -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*

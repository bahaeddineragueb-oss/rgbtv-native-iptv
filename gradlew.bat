@rem Gradle startup script for Windows.
@rem Thin launcher around gradle\wrapper\gradle-wrapper.jar.
@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem  Set local scope for the variables with windows NT shell
@rem ##########################################################################
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if defined JAVA_HOME (
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA_EXE=java.exe
)

if not exist "%CLASSPATH%" (
    echo ERROR: %CLASSPATH% is missing. 1>&2
    echo Generate it once with: gradle wrapper --gradle-version 8.10.2 1>&2
    exit /b 1
)

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=gradlew" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

endlocal

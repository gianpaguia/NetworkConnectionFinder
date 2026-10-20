@echo off
if "%JAVA_HOME%" == "" (
    if exist "C:\Program Files\Java\jdk-25" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-25"
    )
)
"%~dp0.tools\apache-maven-3.9.9\bin\mvn.cmd" %*

@echo off
setlocal

if "%JAVA_HOME%" == "" (
    if exist "C:\Program Files\Java\jdk-25" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-25"
    )
)

set "TOOL_DIR=%~dp0.tools"
set "MVN_CMD=%TOOL_DIR%\apache-maven-3.9.9\bin\mvn.cmd"

if not exist "%MVN_CMD%" (
    echo Bootstrapping Apache Maven 3.9.9 into .tools ^(one-time download^)...
    if not exist "%TOOL_DIR%" mkdir "%TOOL_DIR%"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; $zip=Join-Path $env:TOOL_DIR 'maven-bootstrap.zip'; $u1='https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip'; $u2='https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip'; try { Invoke-WebRequest -Uri $u1 -OutFile $zip -UseBasicParsing } catch { Invoke-WebRequest -Uri $u2 -OutFile $zip -UseBasicParsing }; Expand-Archive -Path $zip -DestinationPath $env:TOOL_DIR -Force; Remove-Item $zip"
    if errorlevel 1 (
        echo Maven bootstrap failed. Download apache-maven-3.9.9 manually into .tools\apache-maven-3.9.9 1>&2
        exit /b 1
    )
)

"%MVN_CMD%" %*

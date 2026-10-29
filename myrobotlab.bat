@ECHO ON

rem TODO move vars to top
rem TODO have --id as an optional var

set APPDIR=%CD%
echo APPDIR=%APPDIR%

set CLASSPATH="%CLASSPATH%;%APPDIR%\target\classes\*;%APPDIR%\libraries\jar\*;%APPDIR%\myrobotlab.jar"

echo CLASSPATH=%CLASSPATH%

rem TODO - option to package jdk for now use bin in path
set JAVA=java

rem Java 17.0.19 and older ship a Visual C++ runtime that cannot load the
rem OpenCV natives on Windows (UnsatisfiedLinkError on jniopencv_core.dll).
"%JAVA%" -version >nul 2>&1
if errorlevel 1 (
    echo Java was not found. MyRobotLab requires Java 17.0.20 or newer.
    pause
    exit /b 1
)
set JAVA_VERSION=
for /f "tokens=3" %%g in ('"%JAVA%" -version 2^>^&1') do (
    set JAVA_VERSION=%%g
    goto :java_version_parsed
)
:java_version_parsed
set JAVA_VERSION=%JAVA_VERSION:"=%
for /f "tokens=1,2,3 delims=.-_+" %%a in ("%JAVA_VERSION%") do (
    set JAVA_MAJOR=%%a
    set JAVA_MINOR=%%b
    set JAVA_UPDATE=%%c
)
if "%JAVA_MAJOR%"=="1" set JAVA_MAJOR=%JAVA_MINOR%
if "%JAVA_UPDATE%"=="" set JAVA_UPDATE=0
set JAVA_TOO_OLD=0
if %JAVA_MAJOR% LSS 17 set JAVA_TOO_OLD=1
if %JAVA_MAJOR% EQU 17 if %JAVA_UPDATE% LSS 20 set JAVA_TOO_OLD=1
if %JAVA_TOO_OLD% EQU 1 (
    echo Your Java version ^(%JAVA_VERSION%^) is too old.
    echo MyRobotLab requires Java 17.0.20 or newer.
    echo Older Java 17 builds include a Visual C++ runtime that is too old to load the OpenCV native libraries on Windows.
    echo Starting OpenCV then fails with: UnsatisfiedLinkError: jniopencv_core.dll: Can't find dependent libraries.
    echo Install Java 17.0.20 or later, or a newer release such as Java 21 or 25, and run MyRobotLab with that java.
    pause
    exit /b 1
)

rem Processing/Arduino handle this in an array - no need for now
set JAVA_OPTIONS=-Djava.library.path=libraries/native -Djna.library.path=libraries/native -Dfile.encoding=UTF-8

IF NOT "%*"=="" (
    echo "USER SUPPLIED ARGS"
    "%JAVA%" %JAVA_OPTIONS% -cp %CLASSPATH% org.myrobotlab.service.Runtime %*
) ELSE (

    IF EXIST "libraries/repo.json" (
        echo "libraries/repo.json exists."
    ) ELSE (
        echo "libraries/repo.json does not exist."
        "%JAVA%" %JAVA_OPTIONS% -cp %CLASSPATH% org.myrobotlab.service.Runtime --install
    )

    "%JAVA%" %JAVA_OPTIONS% -cp %CLASSPATH% org.myrobotlab.service.Runtime --log-level info -s log Log security Security webgui WebGui intro Intro python Python

)
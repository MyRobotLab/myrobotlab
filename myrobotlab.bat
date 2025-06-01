@ECHO ON

rem TODO move vars to top
rem TODO have --id as an optional var

set APPDIR=%CD%
echo APPDIR=%APPDIR%

set CLASSPATH="%CLASSPATH%;%APPDIR%\target\classes\*;%APPDIR%\libraries\jar\*;%APPDIR%\myrobotlab.jar"

echo CLASSPATH=%CLASSPATH%

rem TODO - option to package jdk for now use bin in path
set JAVA=java

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
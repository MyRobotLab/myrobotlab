rem TODO move vars to top
rem TODO have --id as an optional var
rem @ECHO OFF

set APPDIR=%CD%
echo APPDIR=%APPDIR%

set CLASSPATH="%CLASSPATH%;%APPDIR%\target\classes\*;%APPDIR%\libraries\jars\*;%APPDIR%\myrobotlab.jar"

echo CLASSPATH=%CLASSPATH%

rem TODO - option to package jdk for now use bin in path
set JAVA=java

rem Processing/Arduino handle this in an array - no need for now
set JAVA_OPTIONS="-Djava.library.path=libraries/native -Djna.library.path=libraries/native -Dfile.encoding=UTF-8"

"%JAVA%" %JAVA_OPTIONS% org.myrobotlab.service.Runtime --from-launcher --log-level info -s webgui WebGui intro Intro python Python


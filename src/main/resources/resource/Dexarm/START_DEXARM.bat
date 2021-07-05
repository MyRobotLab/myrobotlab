REG ADD HKCU\Console /v CodePage /t REG_DWORD /d 0xfde9 /f
REG ADD HKCU\Console /v FaceName /t REG_SZ /d "Lucida Console" /f
@chcp 65001>nul
@echo off
echo ------------------------------------------------------
echo 			DEXARM BATCH LAUNCHER 0.1 Nixie - 1.1.190+
echo ------------------------------------------------------
echo KILL JAVA to clean reborn

taskkill.exe /F /IM java.exe
taskkill.exe /F /IM javaW.exe
echo taskkill.exe /F /IM chrome.exe

echo ------------------------------------------------------
echo Rotate log files for clean no worky

del myrobotlab.log.1 > NUL
move /y myrobotlab.log myrobotlab.log.1

echo "Done."
echo ------------------------------------------------------
COLOR 0F
cls
echo ------------------------------------------------------
echo START MRL AND DEXARM
echo ------------------------------------------------------
REM start chrome --new-tab "http://localhost:8888/#/service"
REM This is the command to start up the agent jar, specify the memory and run the default InMoov script

SET script=%cd%\resource\Dexarm\scripts\launchdexarm.py
timeout 2 > NUL
echo Executing file %script%
java -Dfile.encoding=UTF-8 -jar myrobotlab.jar -m 1024m --service python Python webgui WebGui --invoke python execFile %script%

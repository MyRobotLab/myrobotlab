rem wait a couple seconds so
rem mrl is completely shutdown & we have access to myrobotlab.jar
rem xp - does not have sleep probably :p
sleep 3

rem - move the update
move /y update\myrobotlab.jar libraries\jar\myrobotlab.jar

rem - start again
start myrobotlab.bat
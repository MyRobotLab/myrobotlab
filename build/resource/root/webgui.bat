REM Proxy configuration
REM the following will allow mrl to update and check the repo if its behind a firewall
REM -Dhttp.proxyHost=webproxy -Dhttp.proxyPort=8080 -Dhttp.proxyUserName="myusername" -Dhttp.proxyPassword="mypassword" -Dhttps.proxyHost=webproxy -Dhttps.proxyPort=8080 

REM start javaw starts java without another shell window on windows
REM to display system out messages enable logging or run simply as java ..<parameters>..
set PATH=%PATH%;%CD%\libraries\native\x86.32.windows;%CD%\libraries\native\x86.64.windows
start javaw -Djava.library.path="libraries/native/x86.32.windows;libraries/native/x86.64.windows"  -cp "libraries/jar/*;libraries/jar/x86.64.windows/*;libraries/jar/x86.32.windows/*;" org.myrobotlab.service.Runtime -service webgui WebGUI python Python 

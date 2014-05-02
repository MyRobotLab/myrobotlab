rem Proxy configuration
rem the following will allow mrl to update and check the repo if its behind a firewall
rem -Dhttp.proxyHost=webproxy -Dhttp.proxyPort=8080 -Dhttp.proxyUserName="myusername" -Dhttp.proxyPassword="mypassword" -Dhttps.proxyHost=webproxy -Dhttps.proxyPort=8080 

rem start javaw starts java without another shell window on windows
rem to display system out messages enable logging or run simply as java ..<parameters>..
java -Djava.library.path="libraries/native/x86.32.windows;libraries/native/x86.64.windows"  -cp "libraries/jar/*" org.myrobotlab.service.Runtime -update -logLevel INFO -logToConsole

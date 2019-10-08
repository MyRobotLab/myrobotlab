WebGui = Runtime.create("WebGui","WebGui")

WebGui.hide('cli')
sleep(1)
WebGui.show('cli')
sleep(1)
WebGui.set('cli', 400, 400, 999)

# if you don't want the browser to 
# autostart to homepage
#
# WebGui.autoStartBrowser(false)

# set a different port number to listen to
# default is 8888
# WebGui.setPort(7777)

# on startup the WebGui will look for a "resources"
# directory (may change in the future)
# static html files can be placed here and accessed through
# the WebGui service

# starts the websocket server
# and attempts to autostart browser
WebGui.startService();


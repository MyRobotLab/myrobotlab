webgui = Runtime.create("webgui","WebGUI")

# if you don't want the browser to 
# autostart to homepage
#
# webgui.autoStartBrowser(false)

# set a different port number to listen to
# default is 7777
# webgui.setPort(8080)

# on startup the webgui will look for a "resources"
# directory (may change in the future)
# static html files can be placed here and accessed through
# the webgui service

# starts the websocket server
# and attempts to autostart browser
webgui.startService();


################################################
# HttpClient service is a service wrapper of the Apache HttpClient
# So, you can download webpages, images, and a all sorts of
# goodies from the internet

http = runtime.start("http","HttpClient")

# blocking methods
# GETs
print(http.get("https://www.google.com"))
print(http.get("https://www.cs.tut.fi/~jkorpela/forms/testing.html"))

# POSTs
http.addFormField("Comments", "This is a different comment")
http.addFormField("Box", "yes")
http.addFormField("Unexpected", "this is an unexpected field")
http.addFormField("hidden field", "something else")

print(http.post("http://www.cs.tut.fi/cgi-bin/run/~jkorpela/echo.cgi"))

http.clearForm()
http.addFormField("NewField", "Value")
http.addFormField("name", "value")

# call-back methods
# step one add a listener
# you could also 'subscribe' to the appropriate methods
# e.g. python.subscribe('http','publishHttpData') &
# python subscript('http','publishHttpResponse') - the addListeners
# do the same thing

http.addHttpDataListener(python)
http.addHttpResponseListener(python)

# define the callback endpoints
def onHttpData(httpData):
  print(httpData.uri)
  print(httpData.contentType)
  print(httpData.data)
  print(httpData.responseCode)

def onHttpResponse(response):
  print(response)

# make the request and the callbacks will be called when
# the method completes
http.post("http://www.cs.tut.fi/cgi-bin/run/~jkorpela/echo.cgi")

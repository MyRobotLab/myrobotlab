# a short script by raver1975 to send an OpenCV picture to Twitter
twitter = Runtime.createAndStart("twitter","Twitter")
opencv = Runtime.createAndStart("opencv","OpenCV")

# add a filter to display
# opencv must be a running service, with a camera capturing!!!!
opencv.addFilter("PyramidDown")
opencv.capture()

# replace security information with your own keys : 
# register your application at https://dev.twitter.com/apps/new and obtain your own keys
twitter.setSecurity("yourConsumerKey","yourConsumerSecret", "yourAccessToken", "yourAccessTokenSecret")
twitter.configure()
twitter.uploadImageFile(opencv.recordSingleFrame() , "text to upload");
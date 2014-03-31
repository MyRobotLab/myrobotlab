twitter = Runtime.createAndStart("twitter","Twitter")
# replace security information with your own keys : 
#register your application at https://dev.twitter.com/ and obtain your own keys
twitter.setSecurity("yourConsumerKey","yourConsumerSecret", "yourAccessToken", "yourAccessTokenSecret")
twitter.configure()
twitter.tweet("Ciao from MyRobotLab")
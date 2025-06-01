# start the service
twitter = runtime.start("twitter","Twitter")

# credentials for your twitter account and api key info goes here
consumerKey = "XXX"
consumerSecret = "XXX" 
accessToken = "XXX"
accessTokenSecret = "XXX"

# set the credentials on the twitter service.
twitter.setSecurity(consumerKey, consumerSecret, accessToken, accessTokenSecret)
twitter.configure()

# tweet all of your beep bop boops..
twitter.tweet("Ciao from MyRobotLab")
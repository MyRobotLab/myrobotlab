#########################################
# GoogleCloud.py
# description: google api client service
# categories: [google, vision, cloud]
# possibly more info @: http://myrobotlab.org/service/GoogleCloud
#########################################
# start the service
googlecloud = runtime.start("googlecloud","GoogleCloud")

# connect to the google cloud back end with the vision api
# this authorization api json file needs to be created 
if googlecloud.connect("../API Project-c90c3d12e7d3.json"):

  faces = googlecloud.detectFaces("faces.jpg")
  print("Found ", faces.size(), " faces")
  print("Writing to file ", "facesOutput.jpg")
  googlecloud.writeWithFaces("faces.jpg", "facesOutput.jpg", faces)

  print(googlecloud.getLabels("kitchen.jpg"))
  print(googlecloud.getLabels("plumbing.jpg"))
  print(googlecloud.getLabels("ship.jpg"))
  print(googlecloud.getLabels("greenball.jpg"))
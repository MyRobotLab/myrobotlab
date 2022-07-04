#########################################
# Security.py
# description: used as a general template
# categories: simulator
# more info @: http://myrobotlab.org/service/Security
#########################################


# start the service
security = runtime.start("security","Security")

# store & crypt secret infomations
# these lines are added only one time the program is run.. then they are deleted
# so that the "actual" keys are no longer in the script !  - and the encrypted keys
# exist on the file system (don't forget to delete these lines after you add your secrets)
security.addSecret("amazon.polly.user.key", "FIE38238733459852");
security.addSecret("amazon.polly.user.secret", "Ujffkds838234jf/kDKJkdlskjlfkj");
security.saveStore()

# get & decrypt secret infomations
security.loadStore()
print security.getSecret("amazon.polly.user.key")
print security.getSecret("amazon.polly.user.secret")

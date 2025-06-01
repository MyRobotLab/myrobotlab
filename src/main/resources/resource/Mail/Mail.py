#########################################
# Mail.py
# more info @: http://myrobotlab.org/service/Mail
#########################################

# start the service
mail = runtime.start("mail","Mail")

mail.username="who@domain.com"
mail.password="yourpassword"
mail.from=mail.username
mail.to="who@domain.com"
mail.smtpServer="smtp.gmail.com"
mail.smtpServerPort=465

mail.subjet="test"
mail.body="body test"

mail.sendMailSSL()
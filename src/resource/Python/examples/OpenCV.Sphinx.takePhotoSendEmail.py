from java.lang import String
from org.myrobotlab.service import Speech
from org.myrobotlab.service import Sphinx
from org.myrobotlab.service import Runtime
import smtplib
from email.MIMEMultipart import MIMEMultipart
from email.MIMEBase import MIMEBase
from email.MIMEText import MIMEText
from email import Encoders
import os

gmail_user = "youremailadress@gmail.com"
gmail_pwd = "YOURPASSWORD"



# create ear and mouth
ear = Runtime.createAndStart("ear","Sphinx")
mouth = Runtime.createAndStart("mouth","Speech")
opencv = Runtime.createAndStart("opencv","OpenCV")
opencv.addFilter("pdown","PyramidDown")
opencv.setDisplayFilter("pdown")
opencv.capture()
# start listening for the words we are interested in
ear.startListening("hello robot|take photo|send email")


# set up a message route from the ear --to--> python method "heard"
ear.addListener("recognized", python.name, "heard", String().getClass()); 

def mail(to, subject, text, attach):
            msg = MIMEMultipart()

            msg['From'] = gmail_user
            msg['To'] = to
            msg['Subject'] = subject
 
            msg.attach(MIMEText(text))

            part = MIMEBase('application', 'octet-stream')
            part.set_payload(open(attach, 'rb').read())
            Encoders.encode_base64(part)
            part.add_header('Content-Disposition',
                        'attachment; filename="%s"' % os.path.basename(attach))
            msg.attach(part)

            mailServer = smtplib.SMTP("smtp.gmail.com", 587)
            mailServer.ehlo()
            mailServer.starttls()
            mailServer.ehlo()
            mailServer.login(gmail_user, gmail_pwd)
            mailServer.sendmail(gmail_user, to, msg.as_string())
            # Should be mailServer.quit(), but that crashes...
            mailServer.close()

def heard():
      data = msg_ear_recognized.data[0]
      print "heard ", data
      if (data == "hello robot"):
         mouth.speak("Hi Alessandro.") 
      elif (data == "take photo"):           
           global photoFileName
           photoFileName = opencv.recordSingleFrame(True)
           print "name file is" , photoFileName
           mouth.speak("photo taken")
           mouth.speak("if you want i could send the photo by email")
      elif (data == "send email"):
           mouth.speak ("Alessandro I am sending you an email")
           mouth.speak ("I will inform you when I am done")
           mail("ReceiverEmailAdress@gmail.com",
             "Hello from python!",
             "This is an email sent with python",
             photoFileName)
           mouth.speak("I am done")
           mouth.speak("photo sent by email")

ear.attach("mouth")
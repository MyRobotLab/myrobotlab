#########################################
# wolframalpha.py
# description: used as a general template
# more info @: http://myrobotlab.org/service/WolframAlpha
#########################################

#Start the Service
wolframalpha = runtime.start("wolframalpha","WolframAlpha")

#Beside using the GUI of the engine which works exactly like a usual search engine, one can use the engine with these methods.
keyword = "ape"
#Searches a keyword
print(wolframalpha.wolframAlpha(keyword))
print ("-----------------------------------------") #delimiter to see which output came from what method

#Does the same as print(wolframalpha.wolframAlpha(keyword))
print(wolframalpha.wolframAlpha(keyword,0))
print ("-----------------------------------------")

#Prints an html code, can be usefull for extracting the image links as example
print(wolframalpha.wolframAlpha(keyword,1))
print ("-----------------------------------------")

#Seaches a keyword and only prints the Category(pod), in the GUI the categories are the same as the bold titles.
print(wolframalpha.wolframAlpha("mass of the moon", "result"))
print ("-----------------------------------------")

#Searches the solution of a problem, if the solution consists of complex numbers or arrays, this may not get a proper result.
for e in wolframalpha.wolframAlphaSolution("3x + 5 = 7"):
	print (e)
print ("-----------------------------------------")

#This is another way of getting the result of a problem by using the pod = Solutions
for e in wolframalpha.wolframAlphaSolution("4x^2 - 3x + 5 = 7", "Solutions"):
	print (e)
print ("-----------------------------------------")

#This is yet another way of getting the result of a problem by using the pod = Solutions
print (wolframalpha.wolframAlpha("2x^2 - 3x + 5 = 7", "Solutions"))
print ("-----------------------------------------")

#With the pod one can get more than results of problems, alternative forms for example (Array which needs to be processed)
print (wolframalpha.wolframAlphaSolution("3x^2 - 3x + 5 = 7", "Alternate forms"))
print ("-----------------------------------------")

#Prints an html code, can be usefull for extracting the image links as example
string = wolframalpha.wolframAlpha(keyword,1)
print (string)
print ("-----------------------------------------")

#This is an example of ho to extract the image urls out of the html output from
#string = wolframalpha.wolframAlpha(keyword,1)
#The import statement is best done at the beginning of the script
import re
string = wolframalpha.wolframAlpha(keyword,1)
urls = re.findall('http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\(\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+', string)
for url in urls:
	print (url)
print ("-----------------------------------------")

#Same example as above bnut instead ov extracting all images it only gets the image of the searchobject itself
#Can be combined with the ImageDisplay service
#import re
#string = wolframalpha.wolframAlpha(keyword,1)
#url = str(re.findall('Image</b><br><img src="http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\(\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+', string))[26:-2]
#print (url)

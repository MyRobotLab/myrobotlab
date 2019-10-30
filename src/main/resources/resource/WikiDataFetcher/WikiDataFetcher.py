#########################################
# WikiDataFetcher.py
# description: used as a general template
# more info @: http://myrobotlab.org/service/WikiDataFetcher
#########################################
# start the service
wikidatafetcher = Runtime.start("wikidatafetcher","WikiDataFetcher")

# english crawl

query = "eiffel tower"
wikidatafetcher.setWebSite("enwiki") 
print "Url : " + wikidatafetcher.getData(query,"P856")
# Display a property ( high of the eiffel tower )
# print "high : " + wikidatafetcher.getData(query,"P2048")
# this is actually broken

# Display a monolingual value
print "Birthname of Adam Sandler : " + wikidatafetcher.getData("Adam Sandler","P1477")

# Display Date or time  (day, month, year, hour, minute, second, after, before
query = "adam sandler"
ID = "P569"
print "BirthDate : " + wikidatafetcher.getTime(query,ID,"day") +"/" + wikidatafetcher.getTime(query,ID,"month") + "/" + wikidatafetcher.getTime(query,ID,"year")

# Display Date by default
query = "statue of liberty"
ID = "P571"
print "Label : " + wikidatafetcher.getLabel(query)
print "Built in : " + wikidatafetcher.getData(query,ID)
# Display Date (year)
print "Built in : " + wikidatafetcher.getTime(query,ID,"year")
print "Coordinates : " + wikidatafetcher.getData(query,"P625")

# french crawl

wikidatafetcher.setWebSite("frwiki")
wikidatafetcher.setLanguage("fr") 
print u"c est quoi un éléphant  : " + wikidatafetcher.getDescription(u"éléphant")
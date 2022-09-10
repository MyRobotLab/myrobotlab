#########################################
# WikiDataFetcher.py
# description: used as a general template
# more info @: http://myrobotlab.org/service/WikiDataFetcher
#########################################
# start the service
wiki = Runtime.start("wikiDataFetcher", "WikiDataFetcher")
wiki.setWebSite("enwiki")
wiki.setLanguage("en")

print(wiki.getDescription("halloween"))
print(wiki.getDescription("empire state building"))
print(wiki.getDescription("the pyramids"))
print(wiki.getDescription("dog"))

# french crawl
wiki.setWebSite("frwiki")
wiki.setLanguage("fr") 
print (u"c est quoi un éléphant  : " + wiki.getDescription(u"éléphant"))
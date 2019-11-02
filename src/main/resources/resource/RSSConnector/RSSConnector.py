# start the service
rssconnector = Runtime.start("rssconnector","RSSConnector")

rssconnector.setRssUrl("http://www.myrobotlab.org/rss.xml")

rssconnector.addListener("publishDocument", "python", "onDocument");

def onDocument(doc):
    # The articles are published as documents.
    print doc

rssconnector.startCrawling()

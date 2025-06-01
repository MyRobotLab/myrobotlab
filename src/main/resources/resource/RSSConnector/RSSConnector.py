##################################################################################
# RSSConnector.py
# description: A connector that can attach to an RSS news feed and publish those documents
# categories: 
# more info @: http://myrobotlab.org/service/RSSConnector
##################################################################################

# Start the connector
rssconnector = runtime.start("rssconnector","RSSConnector")

# set the url for the rss feed
rssconnector.setRssUrl("http://www.myrobotlab.org/rss.xml")

# Define a method that will be called when the rss connector crawls a document.
def onDocument(doc):
    # The articles are published as documents.
    print doc

# register the python service as a listener for documents.
rssconnector.addListener("publishDocument", "python", "onDocument");

# tell the rss connecto to crawl the feed.
rssconnector.startCrawling()

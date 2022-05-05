# FileConnector crawler
# Crawls a directory and publishes a docment for each files found
def onDocument(doc):
    print(doc)
fc = runtime.start("fc","FileConnector")
fc.addListener("publishDocument","python","onDocument")
# start crawling
fc.setDirectory(".myrobotlab")
fc.startCrawling()
sleep(5)
fc.stopCrawling()

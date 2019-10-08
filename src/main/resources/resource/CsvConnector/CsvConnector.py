#########################################
# CsvConnector.py
# categories: ingest
# more info @: http://myrobotlab.org/service/CsvConnector
#########################################
# uncomment for virtual hardware
# virtual = True

# crawlers publish documents
def onDocument(doc):
    print(doc)

# start the service
csvconnector = Runtime.start("csvconnector","CsvConnector")

csvconnector.setFilename("crazybigdata.csv")

csvconnector.setColumns("FirstName", "LastName", "Sex", "Occupation", "Address")
csvconnector.setSeparator(";")
csvconnector.addListener("publishDocument","python","onDocument")

# start crawling
csvconnector.startCrawling()

sleep(5)

csvconnector.stopCrawling()

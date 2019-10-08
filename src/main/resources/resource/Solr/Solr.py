from org.apache.solr.common import SolrInputDocument
from org.apache.solr.common import SolrDocument
 
solr = Runtime.createAndStart("solr", "Solr");
solr.setSolrUrl("http://localhost:8983/solr")
 
doc = SolrInputDocument()
doc.setField("id", "doc1")
doc.setField("title", "This is the title of the document.")
doc.setField("content", "This is the body or main content of the document. myrobotlab rocks.")
 
solr.addDocument(doc)
solr.commit()
 
# A word to search for
q = "myrobotlab"
response = solr.search(q)
# iterate the results
for i in range(0 , response.getResults().size()):
  # grab the doc and print out it's fields and values.
  doc = response.getResults().get(i);
  for fieldname in doc.getFieldNames():
    print(fieldname + ":" + str(doc.getFieldValue(fieldname)))



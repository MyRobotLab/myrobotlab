package org.myrobotlab.document.connector;

import org.junit.Ignore;
import org.myrobotlab.document.transformer.StageConfiguration;
import org.myrobotlab.document.transformer.WorkflowConfiguration;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.DocumentPipeline;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.XMLConnector;

@Ignore
public class WikipediaIndexer {

  public static void main(String[] args) throws ClassNotFoundException {
    String solrUrl = "http://phobos:8983/solr/wikipedia";
    String wikipediaFilename = "Z:\\freeagent\\Wikipedia\\wikipedia\\enwiki-20160113-pages-articles-multistream.xml";
    int NUM_THREADS = 8;
    
    //TODO: why do I need this to index wikipedia from the xml dump?!  I know , it's like a 50GB xml file.. gah..
    System.setProperty("jdk.xml.totalEntitySizeLimit", String.valueOf(Integer.MAX_VALUE));
    org.apache.log4j.BasicConfigurator.configure();
    LoggingFactory.getInstance().setLevel(Level.INFO);
    // wikipedia xml file.  ( freely available via wiki-dumps )
    
    XMLConnector wikipediaConnector = (XMLConnector) Runtime.start("wikipediaConnector", "XMLConnector");
    wikipediaConnector.setFilename(wikipediaFilename);
    wikipediaConnector.setXmlRootPath("/mediawiki/page");
    wikipediaConnector.setXmlIDPath("/mediawiki/page/id");
    wikipediaConnector.setDocIDPrefix("wikipedia_");
    // set the table field
    StageConfiguration staticFieldStageConfig = new StageConfiguration();
    staticFieldStageConfig.setStageClass("org.myrobotlab.document.transformer.SetStaticFieldValue");
    staticFieldStageConfig.setStageName("SetTableField");
    staticFieldStageConfig.setStringParam("fieldName", "table");
    staticFieldStageConfig.setStringParam("value", "wikipedia");
    // an xpath extractor to parse the xml ..
    StageConfiguration xpathStageConfig = new StageConfiguration("extractXPaths", "org.myrobotlab.document.transformer.XPathExtractor");
    xpathStageConfig.setStringParam("configFile", "src/test/resources/xpaths.txt");
    // TODO: remove the xml field.. ?!?! argh!
    StageConfiguration deleteXMLFieldConfig = new StageConfiguration("deleteXMLField", "org.myrobotlab.document.transformer.DeleteField");
    deleteXMLFieldConfig.setStringParam("fieldName", "xml");
    // TODO: remove the xml field.. ?!?! argh!
    StageConfiguration parseWikiTextConfig = new StageConfiguration("parseWikiText", "org.myrobotlab.document.transformer.ParseWikiText");
    // parseWikiTextConfig.setStringParam("fieldName", "text");
    // TODO: followed by a wiki markup parser
    // followed by a solr output stage.
    StageConfiguration solrStageConfig = new StageConfiguration("sendToSolr", "org.myrobotlab.document.transformer.SendToSolr");
    solrStageConfig.setStringParam("solrUrl", solrUrl);
    solrStageConfig.setIntegerParam("batchSize", 200);
    solrStageConfig.setBoolParam("issueCommit", false);
    DocumentPipeline docproc = new DocumentPipeline("docproc");
    // build the pipeline.. assemble the stages.
    // create our document processing pipeline workflow.
    WorkflowConfiguration workflowConfig = new WorkflowConfiguration("default");
    workflowConfig.setNumWorkerThreads(NUM_THREADS);
    // workflowConfig.setName("default");zx`
    workflowConfig.addStage(staticFieldStageConfig);
    workflowConfig.addStage(xpathStageConfig);
    // remove the original xml.. it's icky
    workflowConfig.addStage(deleteXMLFieldConfig);
    workflowConfig.addStage(parseWikiTextConfig);
    workflowConfig.addStage(solrStageConfig);
    docproc.setConfig(workflowConfig);
    docproc.initalize();
    docproc.startService();
    // attach the doc proc to the connector
    wikipediaConnector.addDocumentListener(docproc);
    wikipediaConnector.setBatchSize(200);
    // start crawling...
    wikipediaConnector.startCrawling();

  }

}

package org.myrobotlab.document.connector;

import org.junit.Ignore;
import org.myrobotlab.document.transformer.StageConfiguration;
import org.myrobotlab.document.transformer.WorkflowConfiguration;
import org.myrobotlab.service.DocumentPipeline;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.XMLConnector;

@Ignore
public class WikipediaIndexer {

  public static void main(String[] args) throws ClassNotFoundException {
    // wikipedia xml file.
    String wikipediaFilename = "D:\\data\\wikipedia\\enwiki-20160113-pages-articles-multistream.xml";
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
    xpathStageConfig.setStringParam("configFile", "test/resources/xpaths.txt");

    // TODO: remove the xml field.. ?!?! argh!
    StageConfiguration deleteXMLFieldConfig = new StageConfiguration("deleteXMLField", "org.myrobotlab.document.transformer.DeleteField");
    deleteXMLFieldConfig.setStringParam("fieldName", "xml");

    // TODO: followed by a wiki markup parser
    // followed by a solr output stage.
    StageConfiguration solrStageConfig = new StageConfiguration("sendToSolr", "org.myrobotlab.document.transformer.SendToSolr");
    solrStageConfig.setStringParam("solrUrl", "http://phobos:8983/solr/graph");

    DocumentPipeline docproc = new DocumentPipeline("docproc");
    // build the pipeline.. assemble the stages.
    // create our document processing pipeline workflow.
    WorkflowConfiguration workflowConfig = new WorkflowConfiguration("default");
    workflowConfig.setNumWorkerThreads(5);
    // workflowConfig.setName("default");
    workflowConfig.addStage(staticFieldStageConfig);
    workflowConfig.addStage(xpathStageConfig);
    // remove the original xml.. it's icky
    workflowConfig.addStage(deleteXMLFieldConfig);
    workflowConfig.addStage(solrStageConfig);

    docproc.setConfig(workflowConfig);
    docproc.initalize();
    docproc.startService();

    // attach the doc proc to the connector
    wikipediaConnector.addDocumentListener(docproc);

    wikipediaConnector.setBatchSize(20);
    // start crawling...
    wikipediaConnector.startCrawling();

  }

}

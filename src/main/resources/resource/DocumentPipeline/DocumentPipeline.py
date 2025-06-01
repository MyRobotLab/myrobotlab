######################################
# Example Document Processing pipeline
######################################
from org.myrobotlab.service import Runtime
from org.myrobotlab.document.transformer import WorkflowConfiguration
from org.myrobotlab.document.transformer import StageConfiguration
# create the pipeline service
pipeline = runtime.start("docproc", "DocumentPipeline")
# create a pipeline
# pipeline.workflowName = "default";
# create a workflow to load into that pipeline service
workflowConfig = WorkflowConfiguration();
workflowConfig.setName("default");
staticFieldStageConfig = StageConfiguration();
staticFieldStageConfig.setStageClass("org.myrobotlab.document.transformer.SetStaticFieldValue");
staticFieldStageConfig.setStageName("SetTableField");
# statically assign the value of "MRL" to the field "table" on the document
staticFieldStageConfig.setStringParam("table", "MRL");
workflowConfig.addStage(staticFieldStageConfig);
# a stage that sends a document to solr

openNLPConfig = StageConfiguration()
openNLPConfig.setStageClass("org.myrobotlab.document.transformer.OpenNLP")
openNLPConfig.setStageName("OpenNLP")
openNLPConfig.setStringParam("textField","description")
workflowConfig.addStage(openNLPConfig)

sendToSolrConfig = StageConfiguration();
sendToSolrConfig.setStageClass("org.myrobotlab.document.transformer.SendToSolr")
sendToSolrConfig.setStageName("SendToSolr")
sendToSolrConfig.setStringParam("solrUrl", "http://www.skizatch.org:8983/solr/graph")
workflowConfig.addStage(sendToSolrConfig)
# set the config on the pipeline service
pipeline.setConfig(workflowConfig)
# initialize the pipeline (load the config)
pipeline.initalize()
# create a connector that crawls MyRobotLab RSS url
rss = runtime.start("rss", "RSSConnector")
# Attach the output of the rss connector to the pipeline
rss.addDocumentListener(pipeline)
# tell the RSS connector to start crawling the site
rss.startCrawling()
# connector issues a flush when it's done crawling.
######################################
# Example Document Processing pipeline
######################################
from org.myrobotlab.service import Runtime
from org.myrobotlab.document.transformer import WorkflowConfiguration
from org.myrobotlab.document.transformer import StageConfiguration

# create the pipeline service
pipeline = Runtime.createAndStart("docproc", "DocumentPipeline")

        
# create a pipeline
# pipeline.workflowName = "default";
# create a workflow to load into that pipeline service
workflowConfig = WorkflowConfiguration();
workflowConfig.setName("default");
stage1Config = StageConfiguration();
stage1Config.setStageClass("org.myrobotlab.document.transformer.SetStaticFieldValue");
stage1Config.setStageName("SetTableField");
stage1Config.setStringParam("table", "MRL");
workflowConfig.addStage(stage1Config);

# a stage that sends a document to solr
stage2Config = StageConfiguration();
stage2Config.setStageClass("org.myrobotlab.document.transformer.SendToSolr");
stage2Config.setStageName("SendToSolr");
stage2Config.setStringParam("solrUrl", "http://phobos:8983/solr/graph");
workflowConfig.addStage(stage2Config);

# set the config on the pipeline service
pipeline.setConfig(workflowConfig);
# initialize the pipeline (load the config)
pipeline.initalize();
# create a connector that crawls MyRobotLab RSS url
rss = Runtime.createAndStart("rss", "RSSConnector")
# Attach the output of the rss connector to the pipeline
rss.addDocumentListener(pipeline);
# tell the RSS connector to start crawling the site
rss.startCrawling();
# connector issues a flush when it's done crawling.






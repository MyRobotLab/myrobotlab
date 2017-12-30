package org.myrobotlab.document.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.ProcessingStatus;
import org.myrobotlab.document.transformer.AbstractStage;
import org.myrobotlab.document.transformer.StageConfiguration;
import org.myrobotlab.document.transformer.WorkflowConfiguration;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * 
 * WorkflowWorker : this is a list of stages that will poll the workflow queue
 * and process documents through that list of stages.
 */
public class WorkflowWorker extends Thread {
  public final static Logger log = LoggerFactory.getLogger(WorkflowWorker.class);
  boolean processing = false;
  private ArrayList<AbstractStage> stages;

  private final LinkedBlockingQueue<Document> queue;

  WorkflowWorker(WorkflowConfiguration workflowConfig, LinkedBlockingQueue<Document> queue) throws ClassNotFoundException {
    // set the thread name
    this.setName("WorkflowWorker-" + workflowConfig.getName());
    this.queue = queue;
    stages = new ArrayList<AbstractStage>();
    for (StageConfiguration stageConf : workflowConfig.getStages()) {
      String stageClass = stageConf.getStageClass().trim();
      String stageName = stageConf.getStageName();
      log.info("Starting stage: {} class: {}", stageName, stageClass);
      Class<?> sc = Workflow.class.getClassLoader().loadClass(stageClass);
      try {
        AbstractStage stageInst = (AbstractStage) sc.newInstance();
        stageInst.startStage(stageConf);
        addStage(stageInst);
      } catch (InstantiationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  public void run() {
    Document doc;
    boolean running = true;
    while (running) {
      try {
        doc = queue.take();
        // when can this case happen
        if (doc == null) {
          log.info("Doc was null from workflow queue. setting running to false.");
          running = false;
        } else {
          processing = true;
          // process from the start of the workflow
          processDocumentInternal(doc, 0);
          processing = false;
        }
      } catch (InterruptedException e) {
        // TODO: handle these properly
        log.warn("Workflow Worker Died! {}", e.getMessage());
        e.printStackTrace();
      }
    }
  }

  public boolean isProcessing() {
    return processing;
  }

  public void processDocumentInternal(Document doc, int stageOffset) {
    // TODO:
    int i = 0;
    for (AbstractStage s : stages.subList(i, stages.size())) {
      // create a pool of stages, so that when you call processDocument
      // or each thread should have it's own pool?
      List<Document> childDocs = s.processDocument(doc);
      i++;
      if (childDocs != null) {
        // process each of the children docs down the rest of the pipeline
        for (Document childDoc : childDocs) {
          processDocumentInternal(childDoc, i);
        }
      }
      // TODO:should I create a completely new concept for
      // callbacks?
      if (doc.getStatus().equals(ProcessingStatus.DROP)) {
        // if it's a drop, break here.
        break;
      }
    }
  }

  public void addStage(AbstractStage stage) {
    stages.add(stage);
  }

  public void flush() {
    for (AbstractStage s : stages) {
      s.flush();
    }
  }

}

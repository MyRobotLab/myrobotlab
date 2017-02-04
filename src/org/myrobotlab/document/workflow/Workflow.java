package org.myrobotlab.document.workflow;

import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.transformer.WorkflowConfiguration;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * 
 * Workflow : top level workflow class that controls the thread that do the work
 * of processing documents on each stage.
 *
 */
public class Workflow {

  private final int numWorkerThreads;
  private final int queueLength;
  private final LinkedBlockingQueue<Document> queue;
  private String name = "defaultWorkflow";
  // The workflow has it's own copy of each stage. to avoid thread safety issues
  // when running
  // with more than 1 thread. (todo:review this design pattern for something
  // more thread poolesque?)
  private WorkflowWorker[] workers;
  private WorkflowConfiguration workflowConfig;
  public final static Logger log = LoggerFactory.getLogger(Workflow.class);

  // constructor
  public Workflow(WorkflowConfiguration workflowConfig) throws ClassNotFoundException {
    // create each of the worker threads. each with their own copy of the stages
    numWorkerThreads = workflowConfig.getNumWorkerThreads();
    queueLength = workflowConfig.getQueueLength();
    queue = new LinkedBlockingQueue<Document>(queueLength);
    this.workflowConfig = workflowConfig;
    // We need to load a config
    // then we need to create each of the stages for the config
    // and add those to our stage list.
    this.name = workflowConfig.getName();
  }

  // initialize the workflow
  public void initialize() {
    workers = new WorkflowWorker[numWorkerThreads];
    for (int i = 0; i < numWorkerThreads; i++) {
      initializeWorkerThread(i);
    }
  }

  // init the worker threads
  private void initializeWorkerThread(int threadNum) {
    WorkflowWorker worker = null;
    try {
      worker = new WorkflowWorker(workflowConfig, queue);
    } catch (ClassNotFoundException e) {
      // TODO: better handling?
      log.warn("Error starting the worker thread. {}", e.getLocalizedMessage());
      e.printStackTrace();
      return;
    }
    worker.start();
    workers[threadNum] = worker;
  }

  public void processDocument(Document doc) throws InterruptedException {
    // put the document on the processing queue.
    if (doc != null) {
      queue.put(doc);
    } else {
      queue.put(doc);
    }
  }

  public Document getDocToProcess() throws InterruptedException {
    Document doc = queue.take();
    return doc;
    // TODO: cleaner way to do this is have a queue of a new class, which
    // has the Item in it and meta information to tell us when to stop / etc
    // if (doc.getId() == stopDoc.getId()) {
    // // For now, we push it back on, so that in multi-worker environment
    // // all of them get the stop notification.
    // queue.put(stopDoc);
    // return null;
    // } else {
    // // System.out.println("Pulled doc to process : " + doc.getId());
    // return doc;
    // }
  }

  // flush all the stages on each worker thread.
  public void flush() {
    // TODO: Or make it block here.
    while (!queue.isEmpty()) {
      try {
        // TODO: give a logger object to this class.
        // TODO: review this for threading issues and concurrency
        log.info("Waiting for workflow flush.");
        Thread.sleep(500);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        log.info("Interrupted while waiting for queue to drain. {}", e.getMessage());
        e.printStackTrace();
      }
    }

    // now wait for the threads to no longer be running
    while (true) {
      boolean oneIsRunning = false;
      for (int i = 0; i < numWorkerThreads; i++) {
        oneIsRunning |= workers[i].isProcessing();
      }
      if (!oneIsRunning) {
        break;
      }
      try {
        System.out.println("Workers are still running...");
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }

    // Each worker will get flushed.
    // (each worker flushes its stage)
    for (WorkflowWorker worker : workers) {
      worker.flush();
    }
    log.info("Workflow {} flushed.", name);

  }

  public String getName() {
    return name;
  }

}

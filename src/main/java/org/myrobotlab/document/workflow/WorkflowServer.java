package org.myrobotlab.document.workflow;

import java.util.HashMap;

import org.myrobotlab.document.transformer.WorkflowConfiguration;
import org.myrobotlab.service.DocumentPipeline;

public class WorkflowServer {

  private static WorkflowServer instance = null;

  private HashMap<String, Workflow> workflowMap;
  private final DocumentPipeline pipeline;

  // singleton, the constructor is private.
  private WorkflowServer(DocumentPipeline pipeline) {
    workflowMap = new HashMap<String, Workflow>();
    this.pipeline = pipeline;
  }

  // This is a singleton also
  public static WorkflowServer getInstance(DocumentPipeline pipeline) {
    if (instance == null) {
      instance = new WorkflowServer(pipeline);
      return instance;
    } else {
      return instance;
    }
  }

  // public void addWorkflow(String name, Workflow workflow) {
  // workflowMap.put(name, workflow);
  // }

  public void addWorkflow(WorkflowConfiguration config) throws ClassNotFoundException {
    Workflow w = new Workflow(config);
    w.initialize(pipeline);
    workflowMap.put(w.getName(), w);
  }

  public void processMessage(WorkflowMessage msg) throws InterruptedException {
    // Handle the message here!
    // Multi thread this here we should be putting the message on a queue
    // so that it can be picked up by the workflow that is a worker on that
    // queue.

    Workflow w = workflowMap.get(msg.getWorkflow());
    // w.addDocumentToQueue(msg.getDoc());
    w.processDocument(msg.getDoc());
  }

  public void flush(String workflow) {
    // flush the workflow/pipeline
    Workflow w = workflowMap.get(workflow);
    w.flush();
    // publish that we have flushed (a workflow, pass the flush down the line?) 
    pipeline.invoke("publishFlush");
  }

  public String[] listWorkflows() {
    // TODO Auto-generated method stub
    String[] ws = new String[workflowMap.keySet().size()];
    workflowMap.keySet().toArray(ws);
    return ws;
  }
}

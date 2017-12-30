package org.myrobotlab.document.workflow;

import java.util.HashMap;

import org.myrobotlab.document.transformer.WorkflowConfiguration;

public class WorkflowServer {

  private static WorkflowServer instance = null;

  private HashMap<String, Workflow> workflowMap;

  // singleton, the constructor is private.
  private WorkflowServer() {
    workflowMap = new HashMap<String, Workflow>();
  }

  // This is a singleton also
  public static WorkflowServer getInstance() {
    if (instance == null) {
      instance = new WorkflowServer();
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
    w.initialize();
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
    // TODO Auto-generated method stub
    Workflow w = workflowMap.get(workflow);
    w.flush();

  }

  public String[] listWorkflows() {
    // TODO Auto-generated method stub
    String[] ws = new String[workflowMap.keySet().size()];
    workflowMap.keySet().toArray(ws);
    return ws;
  }

}

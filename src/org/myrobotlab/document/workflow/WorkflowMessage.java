package org.myrobotlab.document.workflow;

import org.myrobotlab.document.Document;

public class WorkflowMessage {

  private String type;
  private Document doc;
  private String workflow;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Document getDoc() {
    return doc;
  }

  public void setDoc(Document doc) {
    this.doc = doc;
  }

  public String getWorkflow() {
    return workflow;
  }

  public void setWorkflow(String workflow) {
    this.workflow = workflow;
  }

}

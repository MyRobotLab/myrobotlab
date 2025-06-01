package org.myrobotlab.service.interfaces;

import org.myrobotlab.document.Document;

public interface DocumentPublisher {

  public static String[] publishMethods = new String[] { "publishDocument" , "publishFlush"};
  
  public String getName();
  
  public Document publishDocument(Document doc);
  
  public void publishFlush();
  
  default public void attachDocumentListener(String name) {
    for (String publishMethod : DocumentPublisher.publishMethods) {
      addListener(publishMethod, name);
    }
  }
  
  // Add the addListener method to the interface all services implement this.
  public void addListener(String topicMethod, String callbackName);

}

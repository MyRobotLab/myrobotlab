package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface TextListener extends NameProvider {
  
  public void onText(String text) throws Exception;
  
  public void attachTextPublisher(TextPublisher service) throws Exception;
  
}

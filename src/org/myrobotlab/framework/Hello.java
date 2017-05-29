package org.myrobotlab.framework;

public class Hello {
    Platform platform;
    
    public Hello(String id){
      platform = Platform.getLocalInstance(id);
    }
}

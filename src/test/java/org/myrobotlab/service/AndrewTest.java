package org.myrobotlab.service;

import org.junit.Ignore;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.nd4j.linalg.io.Assert;


@Ignore
public class AndrewTest extends AbstractServiceTest {

  /*
  public void startVirtualPort(String port) {
    
    VirtualArduino virtual = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
    SerialDevice uart = virtual.getSerial();
    uart.setTimeout(100); // don't want to hang when decoding results...
    try {
      virtual.connect(port);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    
  }
  */
  
  @Override
  public Service createService() {
    Platform.setVirtual(true);
    // TODO Auto-generated method stub
    WebGui webgui = (WebGui)Runtime.create("webgui", "WebGui");
    webgui.autoStartBrowser = false;

    
    Andrew andrew = (Andrew)Runtime.start("andrew", "Andrew");
    return andrew;
  }
  

  @Override
  public void testService() throws Exception {
    // TODO Auto-generated method stub
    String port = "VIRTUAL_COM_PORT";
    // startVirtualPort(port);
    
    
    
    Andrew andrew = (Andrew)service;
    
    andrew.startService();
  

    andrew.startServos();
    
    
    andrew.connect(port);
    
  //  sweety.startUltraSonic(port);
    
    andrew.attach();
    
    Assert.notNull(andrew.captureGesture());
    
    andrew.detach();
    
    // for some reason this isn't released. 
    // Runtime.release("webgui");
    
  }

}

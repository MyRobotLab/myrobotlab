package org.myrobotlab.service;

import org.junit.Ignore;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.nd4j.linalg.io.Assert;

@Ignore
public class SweetyTest extends AbstractServiceTest {

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

    
    Sweety sweety = (Sweety)Runtime.start("sweety", "Sweety");
    return sweety;
  }
  

  @Override
  public void testService() throws Exception {
    // TODO Auto-generated method stub
    String port = "VIRTUAL_COM_PORT";
    // startVirtualPort(port);
    
    
    
    Sweety sweety = (Sweety)service;
    
    sweety.startService();
  

    sweety.startServos();
    
    
    sweety.connect(port);
    
  //  sweety.startUltraSonic(port);
    
    sweety.attach();
    
    Assert.notNull(sweety.captureGesture());
    
    sweety.detach();
    
    // for some reason this isn't released. 
    // Runtime.release("webgui");
    
  }

}

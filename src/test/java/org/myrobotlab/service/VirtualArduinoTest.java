package org.myrobotlab.service;

import org.junit.Ignore;
import org.myrobotlab.arduino.Msg;
import org.myrobotlab.framework.Service;

@Ignore
public class VirtualArduinoTest extends AbstractServiceTest {

  
  @Override
  public Service createService() {
    
    Runtime.setLogLevel("info");

    
    // TODO Auto-generated method stub
    VirtualArduino service = (VirtualArduino)Runtime.start("virtualArduino", "VirtualArduino");
    return service;
  }

  @Override
  public void testService() throws Exception {
   
    String testPort = "testPort";
    
    VirtualArduino va = (VirtualArduino)service;

    // Ok.. now what ?  i mean.. what the heck can a virtual arduino do?  
    // It should be able to read and write bytes to a uart.  (com port)
    
    log.info("About to connect");
    
    // the virtual arduino service should respond to some bytes being written to it's uart.
    // but first thing is to test.. if we "connect" to the virtual arduino.. does the uart respond with hello.
    va.connect(testPort);
    
  //   va.start();
    
    // now
    
    // Now that the virtual arduino connected.. what does the uart have to say.
    while (true) {
      // int i = va.uart.read();
      //System.out.println("READ: " + i);
      // Just so text doesn't spew too fast.
      
      //  let's try writing some data to the serial port.
      Thread.sleep(5000);
      log.info("Writing a messge to attach a servo!");
      Msg msg = new Msg(null, null);
      byte[] data = msg.servoAttach(0, 1, 0, 0, "s1");
      va.uart.write(data);
      // i'd like to see an ack come back!
      va.uart.write(msg.servoMoveToMicroseconds(0, 2000));
      
      // va.getMsg();
      
      System.out.println("Waiting... for what I have no idea.");
      Thread.sleep(5000);
    }
    
    
    
  }

}

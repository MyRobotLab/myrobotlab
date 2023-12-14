package org.myrobotlab.arduino;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Python;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;

/**
 * Not a real unit test (yet)
 * 
 * Use this to generate a configuration Use this to load a configurtion.
 * Uncomment the @Test annotation to run the desired test method.
 * 
 * SimpleConfig is defined as an arduino connected to COM5, with a servo
 * attached to pin 7 with an initial position of 90 degrees.
 * 
 * InMoov2Config is a genereated config based on loading the current InMoov2.py
 * scripts then having runtime save the configs.
 * 
 * @author kwatters
 *
 */

@Ignore
public class ArduinoServoConfigTest {

  @Test
  public void generateSimpleConfigs() throws Exception {

    // create and connect the arduino to com5
    Arduino ard1 = (Arduino) Runtime.start("ard1", "Arduino");
    ard1.setVirtual(true);
    ard1.connect("COM5");
    // create a servo
    Servo servo1 = (Servo) Runtime.start("servo1", "Servo");
    servo1.setPin(7);
    // TODO: set speed
    // Attach the servo to the arudino
    ard1.attach(servo1);
    // move the servo to 90.
    servo1.moveTo(90.0);
    ard1.disconnect();

    // save this configuration as the "simple" config/
    Runtime.getInstance().setConfigName("simple");
    Runtime.getInstance().save();
  }

  // @Test
  public void loadSimpleConfig() throws IOException {

    //
    //
    // Runtime.createAndStart("webgui", "WebGui");
    // Runtime.createAndStart("gui", "SwingGui");
    // load it up.
    Runtime.setConfig("simple");
    Runtime.getInstance().load(); // "data/config/simple/runtime.yml"
    System.out.println("Loaded...");

    Runtime.createAndStart("gui", "SwingGui");
    waitOnAnyKey();

  }

  // @Test
  public void generateInMoov2Configs() throws Exception {

    // work around for java11 and maryspeech
    // if you start mary with jdk11 it will fail because of some lame
    // string/float parsing logic in marytts
    // TODO: see if we need to move this workaround to the MarySpeech service.
    System.setProperty("java.version", "11.0");

    // Runtime.createAndStart("gui", "SwingGui");
    Python python = (Python) Runtime.createAndStart("python", "Python");
    python.execFile("resource/InMoov2/InMoov2.py");
    System.out.println("Done loading script.");
    // waitOnAnyKey();
    // Save this config set as the default config set.
    Runtime.getInstance().save();
    System.out.println("Saved...");
    // exit

  }

  // @Test
  public void loadInMoov2Configs() throws IOException {

    // work around for java11 and maryspeech
    System.setProperty("java.version", "11.0");

    Runtime.createAndStart("webgui", "WebGui");
    Runtime.createAndStart("gui", "SwingGui");
    // load it up.
    Runtime.getInstance().load(); // "data/config/runtime.yml"
    System.out.println("Loaded...");

    waitOnAnyKey();
  }

  // @Test
  public static void installAll() throws ParseException, IOException {
    // Runtime.install("MarySpeech");
    Runtime.install(null, true);
  }

  public static void waitOnAnyKey() {
    System.out.println("Press any key...");
    System.out.flush();
    try {
      System.in.read();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}

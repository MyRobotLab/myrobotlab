/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.NeoPixelControl;
import org.myrobotlab.service.interfaces.NeoPixelController;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class NeoPixel extends Service implements NeoPixelControl {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(NeoPixel.class);

  transient NeoPixelController controller;

  public static class PixelColor {
    public int address;
    public int red;
    public int blue;
    public int green;
    public boolean changed;

    PixelColor(int address, int red, int green, int blue) {
      this.address = address;
      this.red = red;
      this.blue = blue;
      this.green = green;
      changed=true;
    }

    PixelColor() {
      address = 0;
      red = 0;
      blue = 0;
      green = 0;
      changed=true;
    }
    public boolean isEqual(PixelColor pix){
      if(pix.red==red && pix.green==green && pix.blue==blue){
        return true;
      }
      return false;
    }
  }

  public HashMap<Integer, PixelColor> pixelMatrix = new HashMap<Integer, PixelColor>();
  public ArrayList<PixelColor> savedPixelMatrix = new ArrayList<PixelColor>();

  public Integer numPixel;

  /**
   * list of names of possible controllers
   */
  public ArrayList<String> controllers;
  public String controllerName;
  
  boolean isAttached=false;

  public Integer pin;
  public boolean off = false;

  public NeoPixel(String n) {
    super(n);
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
  }

  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();
  }

  public ArrayList<String> refreshControllers() {
    controllers = Runtime.getServiceNamesFromInterface(NeoPixelController.class);
    return controllers;
  }

  @Override
  public NeoPixelController getController() {
    return controller;
  }

  public String getControllerName() {
    String controlerName = null;
    if (controller != null) {
      controlerName = controller.getName();
    }
    return controlerName;
  }

  public boolean isAttached() {
    if(controller != null){
      if(((Arduino) controller).getDeviceId((DeviceControl)this)!=null) {
        isAttached=true;
        return true;
      }
      controller = null;
    }
    isAttached = false;
    return false;
  }

  public void setPixel(int address, int red, int green, int blue) {
    PixelColor pixel = new PixelColor(address, red, green, blue);
    setPixel(pixel);
  }

  public void setPixel(String address, String red, String green, String blue) {
    PixelColor pixel = new PixelColor(Integer.parseInt(address), Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue));
    setPixel(pixel);
  }

  public void setPixel(PixelColor pixel) {
    if (off)
      return;
    if (pixel.address <= getNumPixel()) {
      PixelColor pix=pixelMatrix.get(pixel.address);
      if(pix!=null && !pix.isEqual(pixel)){
        pixel.changed = true;
      }
      else{
        pixel.changed = false;
      }
      pixelMatrix.put(pixel.address, pixel);
    } else {
      log.info("Pixel address over the number of pixel");
    }
  }

  public void sendPixel(PixelColor pixel) {
    if (off)
      return;
    List<Integer> msg = new ArrayList<Integer>();
    msg.add(pixel.address);
    msg.add(pixel.red);
    msg.add(pixel.green);
    msg.add(pixel.blue);
    setPixel(pixel);
    controller.neoPixelWriteMatrix(this, msg);
    //savedPixelMatrix.clear();
    //savedPixelMatrix.add(pixel);
  }

  public void sendPixel(int address, int red, int green, int blue) {
    PixelColor pixel = new PixelColor(address, red, green, blue);
    sendPixel(pixel);
  }

  public void sendPixel(String address, String red, String green, String blue) {
    PixelColor pixel = new PixelColor(Integer.parseInt(address), Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue));
    sendPixel(pixel);
  }

  public void writeMatrix() {
    savedPixelMatrix.clear();
    Set<Entry<Integer, PixelColor>> set = pixelMatrix.entrySet();
    Iterator<Entry<Integer, PixelColor>> i = set.iterator();
    List<Integer> msg = new ArrayList<Integer>();
    while (i.hasNext()) {
      Map.Entry<Integer, PixelColor> me = (Map.Entry<Integer, PixelColor>) i.next();
      PixelColor pix=me.getValue();
      //will only send if the pixel value have changed
      if(pix.changed){
        msg.add(pix.address);
        msg.add(pix.red);
        msg.add(pix.green);
        msg.add(pix.blue);
        pix.changed=false;
        me.setValue(pix);
      }
      savedPixelMatrix.add(me.getValue());
      if (msg.size() > 32) {
        if (!off && isAttached())
          controller.neoPixelWriteMatrix(this, msg);
        msg.clear();
      }
    }
    if (!off && isAttached())
      controller.neoPixelWriteMatrix(this, msg);
    broadcastState();
  }

  public Integer getPin() {
    return pin;
  }

  public int getNumPixel() {
    return numPixel;
  }

  public void turnOff() {
    for (int i = 1; i <= numPixel; i++) {
      PixelColor pixel = new PixelColor(i, 0, 0, 0);
      setPixel(pixel);
    }
    writeMatrix();
    off = true;
  }

  public void turnOn() {
    off = false;
    broadcastState();
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(NeoPixel.class.getCanonicalName());
    meta.addDescription("Control a Neopixel hardware");
    meta.setAvailable(true); // false if you do not want it viewable in a
                  // gui
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("Neopixel, Control");
    return meta;
  }
  public void attach(String controllerName, int pin, int numPixel) throws Exception {
    attach((NeoPixelController) Runtime.getService(controllerName),pin,numPixel);
  }

  public void attach(String controllerName, String pin, String numPixel) throws Exception {
    attach((NeoPixelController) Runtime.getService(controllerName),Integer.parseInt(pin),Integer.parseInt(numPixel));
  }
  
  @Override
  public void attach(NeoPixelController controller, int pin, int numPixel) throws Exception {
    this.pin = pin;
    this.numPixel = numPixel;

    // clear the old matrix
    pixelMatrix.clear();

    // create a new matrix
    for (int i = 1; i < numPixel + 1; i++) {
      setPixel(new PixelColor(i, 0, 0, 0));
    }

    //setController(controller);

    controller.deviceAttach(this, pin, numPixel);
    isAttached = true;
    broadcastState();
  }

  @Override
  public void setController(DeviceController controller) {
    if (controller == null) {
      error("setting null as controller");
      return;
    }
    log.info(String.format("%s setController %s", getName(), controller.getName()));
    this.controller = (NeoPixelController) controller;
    controllerName = this.controller.getName();
  }
  
  public void detach(){
    detach(controller);
  }

  @Override
  public void detach(NeoPixelController controller) {
    // let the controller you want to detach this device
    controller.deviceDetach(this);
    // setting controller reference to null
    this.controller = null;
    isAttached = false;
    refreshControllers();
    broadcastState();
  }
  
  public void refresh(){
    broadcastState();
  }

  public static void main(String[] args) throws InterruptedException {
    LoggingFactory.getInstance().configure();
    LoggingFactory.getInstance().setLevel(Level.INFO);

    try {
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      Runtime.start("gui", "GUIService");
      Runtime.start("python", "Python");
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.arduinoPath="C:\\Program Files (x86)\\Arduino";
      arduino.setBoardMega();
      arduino.connect("COM15");
      arduino.setDebug(true);
      NeoPixel neopixel = (NeoPixel) Runtime.start("neopixel", "NeoPixel");
      webgui.startBrowser("http://localhost:8888/#/service/neopixel");
      neopixel.attach(arduino, 5, 16);
      PixelColor pix = new NeoPixel.PixelColor(1, 255, 0, 0);
      neopixel.setPixel(pix);
      neopixel.writeMatrix();
//      //arduino.setLoadTimingEnabled(true);
//      Servo servo=(Servo)Runtime.start("servo","Servo");
//      servo.attach(arduino, 5);
//      servo.moveTo(180);
    } catch (Exception e) {
      Logging.logError(e);
    }

  }

}

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

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.NeopixelControl;
import org.myrobotlab.service.interfaces.NeopixelController;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

import java.util.*;
import java.util.Map.Entry;


public class Neopixel extends Service implements NeopixelControl{

  
  
  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Neopixel.class);
  
  transient NeopixelController controller;
  
  public static class PixelColor{
    public int address;
    public int red;
    public int blue;
    public int green;
    PixelColor(int address, int red, int green, int blue){
      this.address=address;
      this.red=red;
      this.blue=blue;
      this.green=green;
    }
    PixelColor(){
      address=0;
      red=0;
      blue=0;
      green=0;
    }
  }
  
  HashMap<Integer, PixelColor> pixelMatrix = new HashMap<Integer, PixelColor>();
  ArrayList<PixelColor> savedPixelMatrix = new ArrayList<PixelColor>();
  
 // PixelColor[] pixelMatrix;
  
  int numPixel=0;
  
  public boolean isAttached = false;
  /**
   * list of names of possible controllers
   */
  ArrayList<String> controllers;
  public String controllerName;
  
  public int pin=0; 
  public boolean off=false;
  
  public Neopixel(String n) {
    super(n);
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
  }

  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();
  }

  public ArrayList<String> refreshControllers() {
    controllers = Runtime.getServiceNamesFromInterface(NeopixelController.class);
    return controllers;
  }

  @Override
  public NeopixelController getController() {
    return controller;
  }
  
  public String getControllerName() {
    String controlerName = null;
    if (controller != null) {
      controlerName = controller.getName();
    }
    return controlerName;
  }

  public void unsetController() {
    controller.detach(this);
    controller = null;
    //controllerName = null;
    isAttached = false;
    broadcastState();
  }

  @Override
  public Integer getDeviceType() {
    return DeviceControl.DEVICE_TYPE_NEOPIXEL;
  }

  public void setController(String controllerName, int pin, int numPixel){
    if(controllerName!=""){
      ((NeopixelController) Runtime.getService(controllerName)).attach(this, pin, numPixel);
    }
  }
  
  @Override
  public void setController(DeviceController controller) {
    if (controller == null) {
      error("setting null as controller");
      return;
    }
    log.info(String.format("%s setController %s", getName(), controller.getName()));
    this.controller = (NeopixelController) controller;
    controllerName = this.controller.getName();
    isAttached = true;
    numPixel=0;
    pixelMatrix.clear();
    broadcastState();
  }
  
  public boolean isAttached() {
    return isAttached;
  }

  public void setPixel(int address, int red, int green, int blue){
    PixelColor pixel = new PixelColor(address, red, green, blue);
    setPixel(pixel);
  }
  
  public void setPixel(PixelColor pixel){
    if(off) return;
    if (pixel.address<=getNumPixel()){
      pixelMatrix.put(pixel.address, pixel);
    }
    else {
      log.info("Pixel address over the number of pixel");
    }
  }
  
  public void sendPixel(PixelColor pixel){
    if (off) return;
    List<Integer> msg = new ArrayList<Integer>();
    msg.add(pixel.address);
    msg.add(pixel.red);
    msg.add(pixel.green);
    msg.add(pixel.blue);
    controller.neopixelWriteMatrix(this, msg);
    savedPixelMatrix.clear();
    savedPixelMatrix.add(pixel);
  }
  
  public void sendPixel(int address, int red, int green, int blue){
    PixelColor pixel=new PixelColor(address,red,green,blue);
    sendPixel(pixel);
  }
  
  public void sendPixelMatrix(){
    savedPixelMatrix.clear();
    Set<Entry<Integer, PixelColor>> set = pixelMatrix.entrySet();
    Iterator<Entry<Integer, PixelColor>> i= set.iterator();
    List<Integer> msg = new ArrayList<Integer>();
    while (i.hasNext()) {
      Map.Entry<Integer,PixelColor> me= (Map.Entry<Integer,PixelColor>)i.next();
      msg.add(me.getValue().address);
      msg.add(me.getValue().red);
      msg.add(me.getValue().green);
      msg.add(me.getValue().blue);
      savedPixelMatrix.add(me.getValue());
      if(msg.size()>32){
        if (!off) controller.neopixelWriteMatrix(this, msg);
        msg.clear();
      }
    }
    if (!off) controller.neopixelWriteMatrix(this, msg);
    broadcastState();
  }
  
  public int getPin(){
    if(!isAttached) return 0;
    pin = (int) controller.getConfig(this)[0];
    return pin;
  }

  public int getNumPixel(){
    if (!isAttached) return 0;
    if(numPixel!=0) return numPixel;
    numPixel = (int) controller.getConfig(this)[1];
    for (int i=1; i < numPixel+1; i++){
      setPixel(new PixelColor(i,0,0,0));
    }
    return numPixel;
  }
  
  public void turnOff(){
    for (int i=1; i<=numPixel; i++) {
      PixelColor pixel=new PixelColor(i,0,0,0);
      setPixel(pixel);
    }
    sendPixelMatrix();
    off=true;
  }
  
  public void turnOn(){
    off=false;
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

    ServiceType meta = new ServiceType(Neopixel.class.getCanonicalName());
    meta.addDescription("Control a Neopixel hardware");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("Neopixel, Control");
    return meta;
  }

  public static void main(String[] args) throws InterruptedException {
    LoggingFactory.getInstance().configure();
    LoggingFactory.getInstance().setLevel(Level.INFO);
  
    try {
      WebGui webgui=(WebGui)Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      Runtime.start("gui", "GUIService");
      Runtime.start("python", "Python");
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.connect("COM15");
     // arduino.setDebug(true);
      Neopixel neopixel = (Neopixel) Runtime.start("neopixel", "Neopixel");
      webgui.startBrowser("http://localhost:8888/#/service/neopixel");
      //arduino.setLoadTimingEnabled(true);
      arduino.attachDevice(neopixel, 31,16 );
      PixelColor pix=new Neopixel.PixelColor(1,255,0,0);
      neopixel.setPixel(pix);
      neopixel.sendPixelMatrix();
    } catch (Exception e) {
      Logging.logError(e);
    }

  }

}

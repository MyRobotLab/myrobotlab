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
import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.NeopixelControl;
import org.myrobotlab.service.interfaces.NeopixelController;
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
  }
  
  HashMap<Integer, PixelColor> pixelMatrix = new HashMap<Integer, PixelColor>();
  
  int numPixel=0;

  public Neopixel(String n) {
    super(n);
  }
  
  @Override
  public NeopixelController getController() {
    return controller;
  }

  @Override
  public Integer getDeviceType() {
    return DeviceControl.DEVICE_TYPE_NEOPIXEL;
  }

  @Override
  public void setController(DeviceController controller) {
    if (controller == null) {
      error("setting null as controller");
      return;
    }
    log.info(String.format("%s setController %s", getName(), controller.getName()));
    this.controller = (NeopixelController) controller;
    pixelMatrix.clear();
    for (int i=1; i<=numPixel; i++){
      PixelColor pixel=new PixelColor(i,0,0,0);
      pixel.address = i;
      setPixel(pixel);
    }
    broadcastState();
  }
  
  public void setPixel(int address, int red, int green, int blue){
    PixelColor pixel = new PixelColor(address, red, green, blue);
    setPixel(pixel);
  }
  
  public void setPixel(PixelColor pixel){
    if (pixel.address<=numPixel){
      pixelMatrix.put(pixel.address, pixel);
    }
    else {
      log.info("Pixel address over the number of pixel");
    }
  }
  
  public void sendPixelMatrix(){
    Set<Entry<Integer, PixelColor>> set = pixelMatrix.entrySet();
    Iterator<Entry<Integer, PixelColor>> i= set.iterator();
    List<Integer> msg = new ArrayList<Integer>();
    while (i.hasNext()) {
      Map.Entry<Integer,PixelColor> me= (Map.Entry<Integer,PixelColor>)i.next();
      msg.add(me.getValue().address);
      msg.add(me.getValue().red);
      msg.add(me.getValue().green);
      msg.add(me.getValue().blue);
      if(msg.size()>32){
        controller.neopixelWriteMatrix(this, msg);
        msg.clear();
      }
    }
    controller.neopixelWriteMatrix(this, msg);
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
     // Runtime.start("webgui", "WebGui");
      Runtime.start("gui", "GUIService");
      Runtime.start("python", "Python");
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.connect("COM15");
     // arduino.setDebug(true);
      Neopixel neopixel = (Neopixel) Runtime.start("neopixel", "Neopixel");
      neopixel.numPixel=16;
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

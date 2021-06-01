/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.NeoPixel2Control;
import org.myrobotlab.service.interfaces.NeoPixel2Controller;
import org.slf4j.Logger;

public class NeoPixel2 extends Service implements NeoPixel2Control {

  public static class Pixel {
    public int address;
    public int blue;
    public int green;
    public int red;
    public int white;

    public Pixel(int address, int red, int green, int blue, int white) {
      this.address = address;
      this.red = red;
      this.blue = blue;
      this.green = green;
      this.white = white;
    }

    public int[] flatten() {
      return new int[] { this.address, this.red, this.green, this.blue, this.white };
    }
  }

  public static class PixelSet {
    public List<Pixel> pixels = new ArrayList<>();
    public long delayMs = 0;

    public int[] flatten() {
      // List<Integer> ret = new ArrayList<>();

      // initial imp of RGB and RGBW
      // was done with RGBW buckets ...
      int[] ret = new int[pixels.size() * 5];

      for (int i = 0; i < pixels.size(); ++i) {
        Pixel p = pixels.get(i);
        ret[i] = p.address;
        ret[i + 1] = p.red;
        ret[i + 2] = p.green;
        ret[i + 3] = p.blue;
        // lame .. using the same strategy as original neopix
        // bucket of 4 bytes...
        ret[i + 4] = p.white;
      }
      return ret;
    }
  }

  public final static Logger log = LoggerFactory.getLogger(NeoPixel2.class);

  private static final long serialVersionUID = 1L;

  /**
   * name of controller currently attached to
   */
  protected String controller = null;
  

  /**
   * list of possible controllers
   */
  protected Set<String> controllers = new HashSet<>();

  /**
   * name of current matrix
   */
  protected String currentMatrix = "default";

  /**
   * currentSequence in a matrix
   */
  protected int currentSequence = 0;

  /**
   * A named set of sequences of pixels initially you start with "default" but
   * if you can choose to name and save sequences
   */
  Map<String, List<PixelSet>> matrices = new HashMap<>();

  /**
   * the number of pixels in a strand
   */
  protected int pixelCount = 8;

  /**
   * pin NeoPixel is attached to on controller
   */
  protected Integer pin = null;

  /**
   * RGB or RGBW supported
   */
  protected String type = "RGB";

  public NeoPixel2(String n, String id) {
    super(n, id);
    // search through current for onStarted (in startService
  }

  @Override
  public void attach(String controller, int pin, int numPixel, int depth) throws Exception {
    if (controller == null) {
      error("controller cannot be null");
      return;
    }
    this.controller = controller;
    setCount(numPixel);

    // NOT GOOD - FRAGILE - if type is changed after attachment - problems will
    // occur :(
    if (type.equals("RGBW")) {
      // controller.neoPixel2Attach(getName(), pin, numPixel, 4);
      send(controller, "neoPixel2Attach", getName(), pin, numPixel, 4);
    } else {
      // controller.neoPixel2Attach(getName(), pin, numPixel, 3);
      send(controller, "neoPixel2Attach", getName(), pin, numPixel, 3);
    }
  }

  @Override
  public void detach(String controller) {
    if (this.controller == null) {
      log.info("already detached");
      return;
    }
    // i detach from controller
    this.controller = null;
    if (controller != null) {
      // have controller detach from me
      // controller.detach(this);
      send(controller, "detach", getName());
    }
  }

  public int setCount(int pixelCount) {
    this.pixelCount = pixelCount;
    broadcastState();
    return pixelCount;
  }

  public int getCount() {
    return pixelCount;
  }

  @Override
  public int getNumPixel() {
    return pixelCount;
  }

  @Override
  public Integer getPin() {
    return pin;
  }

  // TODO - onStarted
  public void onStarted(String name) {
    refreshControllers();
  }

  public Set<String> refreshControllers() {
    try {
      Set<String> ret = new HashSet<>();
      List<String> c = Runtime.getServiceNamesFromInterface("NeoPixelController");
      ret.addAll(c);
      controllers = ret;
      broadcastState();
    } catch (Exception e) {
      error(e);
    }
    return controllers;
  }

  @Override
  public void setAnimation(int animation, int red, int green, int blue, int speed) {
    if (speed < 1)
      speed = 1;
    // controller.neoPixelSetAnimation(this, animation, red, green, blue,
    // speed);
    send(controller, "neoPixel2SetAnimation", getName(), animation, red, green, blue, speed);
    // NeoPixel2Controller nc2 =
    // (NeoPixel2Controller)Runtime.getService(controller);
    // nc2.neoPixel2SetAnimation(controller, animation, red, green, blue,
    // speed);
  }

  @Override
  public void setAnimation(String animation, int red, int green, int blue, int speed) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setAnimation(String animation, String red, String green, String blue, String speed) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setAnimationSetting(String animation) {
    // TODO Auto-generated method stub

  }

  /**
   * basic setting of a pixel
   */
  public void setPixel(int address, int red, int green, int blue) {
    setPixel(currentMatrix, currentSequence, address, red, green, blue, 0, 0);
  }

  public void setPixel(int address, int red, int green, int blue, int white) {
    setPixel(currentMatrix, currentSequence, address, red, green, blue, white, 0);
  }

  /**
   * setPixel of maximum complexity
   * 
   * @param matrixName
   * @param sequence
   * @param address
   * @param red
   * @param green
   * @param blue
   * @param white
   * @param delayMs
   */
  public void setPixel(String matrixName, Integer sequence, int address, int red, int green, int blue, int white, Integer delayMs) {
    if (matrixName == null) {
      matrixName = currentMatrix;
    }

    if (sequence == null) {
      sequence = currentSequence;
    }

    List<PixelSet> matrix = matrices.get(matrixName);

    if (matrix == null) {
      // make new matrix
      matrix = new ArrayList<>();
      matrices.put(matrixName, matrix);
    }

    // add new pixel set if we dont have the one requested
    if (sequence > matrix.size()) {
      error("sequence %d out of bounds", sequence);
      return;
    }

    // sequence address == size we need a sequence created
    if (sequence == matrix.size()) {
      PixelSet ps = new PixelSet();
      for (int i = 0; i < pixelCount; ++i) {
        if (type.equals("RGBW")) {
          ps.pixels.add(new Pixel(i, 0, 0, 0, 0));
        } else {
          ps.pixels.add(new Pixel(i, 0, 0, 0, 0));
        }
      }
      matrix.add(ps);
    }

    PixelSet ps = matrix.get(sequence);

    ps.delayMs = delayMs;

    // NeoPixel2Controller c = (NeoPixel2Controller)
    // Runtime.getService(controller);
    ServiceInterface sc = Runtime.getService(controller);
    if (sc == null) {
      error("controler %s not valid", controller);
      return;
    }

    // update pixel in sequence in matrix
    Pixel pixel = new Pixel(address, red, green, blue, white);

    // update memory
    ps.pixels.set(address, pixel);

    log.info("{}", ps.flatten());

    // write immediately
    send(controller, "neoPixel2WriteMatrix", getName(), pixel.flatten());
    
  }

  /**
   * Set type of strand only RGB and RGBW are currently supported
   * 
   * @param type
   * @return
   */
  public String setType(String type) {
    if (type == null || (!type.equals("RGB") && !type.equals("RGBW"))) {
      error("type of RGB or RGBW only supported");
      return null;
    }
    this.type = type;
    broadcastState();
    return type;
  }

  public void startService() {
    super.startService();
    refreshControllers();
  }

  public void stopAnimation() {
    setAnimation(0, 0, 0, 0, 0);
  }

  @Deprecated /* use stop() */
  public void animationStop() {
    stopAnimation();
  }

  public void clear() {
    stopAnimation();
    for (int i = 0; i < pixelCount; i++) {
      setPixel(i, 0, 0, 0, 0);
    }
    writeMatrix();
  }

  @Deprecated /* use clear() */
  @Override
  public void turnOff() {
    clear();
  }

  @Deprecated
  @Override
  public void turnOn() {
    // NOOP
  }

  public void setMatrix(int address, int red, int green, int blue) {
    setMatrix(address, red, green, blue, 0);
  }

  public void setMatrix(int address, int red, int green, int blue, int white) {

  }

  @Override
  public void writeMatrix() {
    // TODO Auto-generated method stub

  }

  public void attach(NeoPixel2Controller arduino, int pin2, int numPixel, int depth) throws Exception {
    attach(arduino.getName(), pin2, numPixel, depth);
  }

  public static void main(String[] args) throws InterruptedException {

    try {

      Runtime.main(new String[] { "--id", "admin", "--from-launcher" });
      LoggingFactory.init(Level.INFO);

      // Runtime.start("gui", "SwingGui");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      Runtime.start("neopixel", "NeoPixel2");

      // Runtime.start("gui", "SwingGui");
      Runtime.start("python", "Python");
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.connect("/dev/ttyACM0");
      // Arduino arduino1 = (Arduino) Runtime.start("arduino1", "Arduino");
      // arduino1.setBoardUno();
      // arduino1.connect(arduino, "Serial2");
      // //arduino.setDebug(true);
      NeoPixel2 neopixel = (NeoPixel2) Runtime.start("neopixel", "NeoPixel2");
      // webgui.startBrowser("http://localhost:8888/#/service/neopixel");
      // neopixel.attach(arduino, 6, 120);
      neopixel.attach(arduino, 3, 8, 3);
      neopixel.setAnimation(2, 110, 110, 0, 1);
      // neopixel.clear();

      neopixel.stopAnimation();
      
      neopixel.setAnimation(0, 0, 0, 0, 0);
      
      neopixel.clear();
      
      neopixel.setPixel(3, 0, 110, 0);
      neopixel.setPixel(2, 0, 110, 0);
      // neopixel.setPixel(1, 0, 110, 0);

      
      // sleep(50);
      // PixelColor pix = new NeoPixel.PixelColor(1, 255, 255, 0, 0);
      // neopixel.setPixel(pix);
      // neopixel.sendPixel(pix);
      neopixel.setPixel(3, 110, 110, 0);
      neopixel.setPixel(2, 110, 110, 0);
      // neopixel.setPixel(1, 110, 110, 0);
      
      /*
      for (int i = 0; i < 8; ++i) {
        neopixel.setPixel(i, 110, 110, 0);
      }
      */

      boolean done = true;
      if (done) {
        return;
      }

      // neopixel.setAnimation(NEOPIXEL_ANIMATION_LARSON_SCANNER, 255, 0, 0, 1);
      // arduino.enableBoardStatus(true);
      // neopixel.setAnimation(NEOPIXEL_ANIMATION_LARSON_SCANNER, 0, 255, 0, 1);
      Servo servo = (Servo) Runtime.start("servo", "Servo");
      servo.attach(arduino, 5);
      servo.moveTo(180.0);
      sleep(2000);
      // neopixel.setAnimation(NEOPIXEL_ANIMATION_LARSON_SCANNER, 200, 0, 0, 1);
    } catch (Exception e) {
      log.error("main threw", e);
    }

  }


}
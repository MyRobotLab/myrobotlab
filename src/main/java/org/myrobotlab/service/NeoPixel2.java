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
import java.util.Random;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechRecognizer.ListeningEvent;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.interfaces.NeoPixel2Control;
import org.myrobotlab.service.interfaces.NeoPixel2Controller;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.slf4j.Logger;

public class NeoPixel2 extends Service implements NeoPixel2Control {

  protected final AnimationRunner animationRunner;
  
  public static class Pixel {
    public int address;
    public int blue;
    public int green;
    public int red;
    public int white;

    public Pixel(int address, int red, int green, int blue, int white) {
      this.address = address;
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.white = white;
    }

    public void clear() {
      red = 0;
      green = 0;
      blue = 0;
      white = 0;
    }

    public int[] flatten() {
      return new int[] { this.address, this.red, this.green, this.blue, this.white };
    }

    public String toString() {
      return String.format("%d:%d,%d,%d,%d", address, red, green, blue, white);
    }
  }

  public static class PixelSet {
    public long delayMs = 0;
    public List<Pixel> pixels = new ArrayList<>();

    public int[] flatten() {
      // List<Integer> ret = new ArrayList<>();

      // initial imp of RGB and RGBW
      // was done with RGBW buckets ...
      int[] ret = new int[pixels.size() * 5];

      for (int i = 0; i < pixels.size(); i++) {
        Pixel p = pixels.get(i);
        int j = i * 5;
        ret[j] = p.address;
        ret[j + 1] = p.red;
        ret[j + 2] = p.green;
        ret[j + 3] = p.blue;
        // lame .. using the same strategy as original neopix
        // bucket of 4 bytes...
        ret[j + 4] = p.white;
      }
      return ret;
    }
  }

  public final static Logger log = LoggerFactory.getLogger(NeoPixel2.class);

  private static final long serialVersionUID = 1L;

  final protected Map<String, Integer> animationToIndex = new HashMap<>();

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
   * pin NeoPixel is attached to on controller
   */
  protected Integer pin = null;

  /**
   * the number of pixels in a strand
   */
  protected int pixelCount = 8;

  /**
   * RGB or RGBW supported
   */
  protected String type = "RGB";

  public NeoPixel2(String n, String id) {
    super(n, id);
    animationRunner = new AnimationRunner();
    animationToIndex.put("blah", 3);
    // search through current for onStarted (in startService
  }

  @Deprecated /* use stop() */
  public void animationStop() {
    stopAnimation();
  }

  public void attach(NeoPixel2Controller arduino, int pin2, int numPixel, int depth) throws Exception {
    attach(arduino.getName(), pin2, numPixel, depth);
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

    NeoPixel2Controller np2 = (NeoPixel2Controller) Runtime.getService(controller);

    // FIXME WRONG !!!
    if (type.equals("RGBW")) {
      np2.neoPixel2Attach(getName(), pin, numPixel, 4);
    } else {
      np2.neoPixel2Attach(getName(), pin, numPixel, 3);
    }
  }

  public void clear() {
    // stopAnimation();
    clearPixelSet();
    log.info("clear getPixelSet {}", getPixelSet().flatten());
    writeMatrix();
  }

  public void clearPixelSet() {
    clearPixelSet(null, null);
  }

  public void clearPixelSet(String matrixName, Integer sequenceId) {
    PixelSet ps = getPixelSet(matrixName, sequenceId);
    if (ps == null) {
      return;
    }
    for (Pixel p : ps.pixels) {
      p.clear();
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
      NeoPixel2Controller np2 = (NeoPixel2Controller) Runtime.getService(controller);
      np2.detach(getName());
    }
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

  public PixelSet getPixelSet() {
    return getPixelSet(null, null);
  }

  /**
   * Get the pixel set requested - if it does not exist and the pixel set index
   * is off by one it will create a new pixel set of 0,0,0,0 value add it to the
   * matrix and return it
   * 
   * @param matrixName
   * @param pixelSetIndex
   * @return
   */
  public PixelSet getPixelSet(String matrixName, Integer pixelSetIndex) {
    if (matrixName == null) {
      matrixName = currentMatrix;
    }

    if (pixelSetIndex == null) {
      pixelSetIndex = currentSequence;
    }

    List<PixelSet> pixelSets = matrices.get(matrixName);

    if (pixelSets == null) {
      // make new matrix
      pixelSets = new ArrayList<>();
      matrices.put(matrixName, pixelSets);
    }

    // add new pixel set if we dont have the one requested
    if (pixelSetIndex > pixelSets.size()) {
      error("sequence %d out of bounds", pixelSetIndex);
      return null;
    }

    // sequence address == size we need a pixel set created
    if (pixelSetIndex == pixelSets.size()) {
      PixelSet ps = new PixelSet();
      for (int i = 0; i < pixelCount; ++i) {
        ps.pixels.add(new Pixel(i, 0, 0, 0, 0));
      }
      pixelSets.add(ps);
    }
    return pixelSets.get(pixelSetIndex);
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
  public void setAnimation(int animation, int red, int green, int blue, int wait_ms) {
    if (wait_ms < 1)
      wait_ms = 1;
    log.info("setAnimation {} {} {} {} {}", animation, red, green, blue, wait_ms);
    NeoPixel2Controller nc2 = (NeoPixel2Controller) Runtime.getService(controller);
    nc2.neoPixel2SetAnimation(getName(), animation, red, green, blue, 0, wait_ms);
  }

  @Override
  public void setAnimation(String animation, int red, int green, int blue, int wait_ms) {
    // TODO convert to int

  }

  @Override
  public void setAnimationSetting(String animation) {
    // TODO Auto-generated method stub

  }

  public int setCount(int pixelCount) {
    this.pixelCount = pixelCount;
    broadcastState();
    return pixelCount;
  }

  public void setMatrix(int address, int red, int green, int blue) {
    setMatrix(address, red, green, blue, 0);
  }

  public void setMatrix(int address, int red, int green, int blue, int white) {
    Pixel p = getPixelSet().pixels.get(address);
    p.red = red;
    p.green = green;
    p.blue = blue;
    p.white = white;
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
   * @param pixelSetIndex
   * @param address
   * @param red
   * @param green
   * @param blue
   * @param white
   * @param delayMs
   */
  public void setPixel(String matrixName, Integer pixelSetIndex, int address, int red, int green, int blue, int white, Integer delayMs) {

    PixelSet ps = getPixelSet(matrixName, pixelSetIndex);

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

    // write immediately
    // FIXME optimize send array of only changed pixels
    writeMatrix();
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

  @Override
  public void writeMatrix() {
    NeoPixel2Controller np2 = (NeoPixel2Controller) Runtime.getService(controller);
    if (np2 != null) {
      np2.neoPixel2WriteMatrix(getName(), getPixelSet().flatten());
    }
  }
  
  private class AnimationRunner implements Runnable {

    boolean running = false;
    Thread thread = null;
        
    @Override
    public void run() {
      try {
      running = true;

      while(running) {
        equalizer();
        sleep(30);
      }
      } catch(Exception e) {
        error(e);
        stop();
      }
      
    }
    
    public synchronized void stop() {
      running = false;
      thread = null;
    }

    public synchronized void start() {
      running = false;
      thread = new Thread(this, String.format("%s-animation-runner", getName()));
      thread.start();
    }
    
  }

  public void equalizer() {
    equalizer(null, null, null, null, null, null);
  }

  public void equalizer(int r, int g, int b) {
    equalizer(null, null, r, g, b, null);
  }

  public void equalizer(Long wait_ms, Integer range, Integer r, Integer g, Integer b, Integer w) {

    if (wait_ms == null) {
      wait_ms = 25L;
    }

    if (range == null) {
      range = 25;
    }

    if (r == null) {
      r = 110;
    }

    if (g == null) {
      g = 110;
    }

    if (b == null) {
      b = 0;
    }

    if (w == null) {
      w = 0;
    }

    Random rand = new Random();

      int c = rand.nextInt(range);

      fillMatrix(c, c, 0);
      if (c < 18) {
        setMatrix(0, 0, 0, 0);
        setMatrix(7, 0, 0, 0);
      }

      fillMatrix(c, c, 0);
      if (c < 16) {
        setMatrix(0, 0, 0, 0);
        setMatrix(7, 0, 0, 0);
      }
      if (c < 12) {
        setMatrix(1, 0, 0, 0);
        setMatrix(6, 0, 0, 0);
      }

      if (c < 8) {
        setMatrix(2, 0, 0, 0);
        setMatrix(5, 0, 0, 0);
      }

      writeMatrix();

  }
  
  /**
   * handle both user defined, java defined, and controller on board animations
   * FIXME - make "settings" separate call
   * @param name
   */
  public void startAnimation(String name) {
    animationRunner.start();    
  }

  public void stopAnimation() {
    // stop java animations
    animationRunner.stop(); 
    // stop on board controller animations
    setAnimation(0, 0, 0, 0, 0);
    clear();
  }
  
  public void test() {
    clear();
    for (int i = 0; i < 1000; ++i) {
      // setAnimation(6, 10, 110, 0, 10); // rainbow 
      setAnimation(4, 10, 110, 50, 130); // rainbow 
    }
  }

  public static void main(String[] args) throws InterruptedException {

    try {

      Runtime.main(new String[] { "--id", "admin", "--from-launcher" });
      LoggingFactory.init(Level.INFO);

      // Runtime.start("gui", "SwingGui");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      // Runtime.start("neopixel", "NeoPixel2");
      // Runtime.start("gui", "SwingGui");

      Runtime.start("python", "Python");
      Polly polly = (Polly)Runtime.start("polly", "Polly");
      
      
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.connect("/dev/ttyACM0");
      // Arduino arduino1 = (Arduino) Runtime.start("arduino1", "Arduino");
      // arduino1.setBoardUno();
      // arduino1.connect(arduino, "Serial2");
      // //arduino.setDebug(true);
      
      NeoPixel2 neopixel = (NeoPixel2) Runtime.start("neopixel", "NeoPixel2");
      neopixel.attach(arduino, 3, 8, 3);
      neopixel.test();
      
      neopixel.clear();
      for (int i = 0; i < 1000; ++i) {
        neopixel.setAnimation(2, 0, 110, 0, 100); 
      }
      
      neopixel.startAnimation();
      neopixel.stopAnimation();
      
      neopixel.subscribe("polly", "publishStartSpeaking");
      neopixel.subscribe("polly", "publishEndSpeaking");
      
      
      polly.speak("hi there");
      polly.speak("hi there");

//      neopixel.subscribe("polly.audioFile", "publishAudioStart", "neopixel", "startAnimation");
//      neopixel.subscribe("polly.audioFile", "publishAudioStop", "neopixel", "stopAnimation");
      
      neopixel.equalizer();

      for (int i = 0; i < 1000; ++i) {
        neopixel.fillMatrix(110, 110, 0);
        neopixel.writeMatrix();
        sleep(100);

        neopixel.fillMatrix(110, 110, 0);
        neopixel.setMatrix(0, 0, 0, 0);
        neopixel.setMatrix(7, 0, 0, 0);
        neopixel.writeMatrix();
        sleep(100);

        neopixel.setMatrix(1, 70, 70, 0);
        neopixel.setMatrix(6, 70, 70, 0);
        neopixel.writeMatrix();
        sleep(100);

        neopixel.setMatrix(1, 30, 30, 0);
        neopixel.setMatrix(6, 30, 30, 0);
        neopixel.setMatrix(2, 70, 70, 0);
        neopixel.setMatrix(5, 70, 70, 0);
        neopixel.writeMatrix();
        sleep(100);

        neopixel.setMatrix(1, 0, 0, 0);
        neopixel.setMatrix(6, 0, 0, 0);
        neopixel.setMatrix(2, 30, 30, 0);
        neopixel.setMatrix(5, 30, 30, 0);
        neopixel.setMatrix(3, 70, 70, 0);
        neopixel.setMatrix(4, 70, 70, 0);
        neopixel.writeMatrix();
        sleep(100);

        neopixel.setMatrix(2, 0, 0, 0);
        neopixel.setMatrix(5, 0, 0, 0);
        neopixel.setMatrix(3, 30, 30, 0);
        neopixel.setMatrix(4, 30, 30, 0);
        neopixel.writeMatrix();
        sleep(100);

      }

      for (int i = 0; i < 1000; ++i) {
        neopixel.setPixel(i % 8, 20, 0, 0);
        sleep(10);
        neopixel.setPixel(i % 8, i % 255, 255 - i % 255, 0);
      }

      // webgui.startBrowser("http://localhost:8888/#/service/neopixel");
      // neopixel.attach(arduino, 6, 120);
      for (int i = 0; i < 1000; ++i) {

        neopixel.setAnimation(2, 0, 110, 0, 100);

        neopixel.setAnimation(2, 110, 110, 0, 100);
        // neopixel.clear();

        neopixel.setAnimation(2, 0, 0, 110, 100);
        // FIXME - 0 pixel set :(
        neopixel.stopAnimation();

        neopixel.setAnimation(0, 0, 0, 0, 0);

        neopixel.setPixel(5, 200, 10, 10);

        neopixel.clear();

        neopixel.setPixel(3, 0, 110, 10);
        neopixel.setPixel(2, 0, 110, 10);
        // neopixel.setPixel(1, 0, 110, 0);

        // sleep(50);
        // PixelColor pix = new NeoPixel.PixelColor(1, 255, 255, 0, 0);
        // neopixel.setPixel(pix);
        // neopixel.sendPixel(pix);
        neopixel.setPixel(3, 110, 110, 10);
        neopixel.setPixel(2, 110, 110, 10);
        // neopixel.setPixel(1, 110, 110, 0);

        /*
         * for (int i = 0; i < 8; ++i) { neopixel.setPixel(i, 110, 110, 0); }
         */

      }
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

  public void startAnimation() {
    startAnimation(currentMatrix);
  }

  public void fillMatrix(int r, int g, int b) {
    fillMatrix(r, g, b, 0);
  }

  public void fillMatrix(int r, int g, int b, int w) {
    PixelSet ps = getPixelSet();
    for (Pixel p : ps.pixels) {
      p.red = r;
      p.green = g;
      p.blue = b;
      p.white = w;
    }

  }

  // @Override
  public void onEndSpeaking(String utterance) {
    stopAnimation();
  }

  // @Override
  public String onStartSpeaking(String utterance) {
    startAnimation();
    return utterance;
  }


}
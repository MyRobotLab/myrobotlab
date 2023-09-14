/**
 *                    
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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.NeoPixelConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.LedDisplayData;
import org.myrobotlab.service.interfaces.NeoPixelControl;
import org.myrobotlab.service.interfaces.NeoPixelController;
import org.slf4j.Logger;

public class NeoPixel extends Service<ServiceConfig> implements NeoPixelControl {

  /**
   * Thread to do animations Java side and push the changing of pixels to the
   * neopixel
   */
  private class AnimationRunner implements Runnable {

    boolean running = false;

    private transient Thread thread = null;

    @Override
    public void run() {
      try {
        running = true;

        while (running) {
          equalizer();
          Double wait_ms_per_frame = fpsToWaitMs(speedFps);
          sleep(wait_ms_per_frame.intValue());
        }
      } catch (Exception e) {
        error(e);
        stop();
      }
    }

    // FIXME - this should just wait/notify - not start a thread
    public synchronized void start() {
      running = false;
      thread = new Thread(this, String.format("%s-animation-runner", getName()));
      thread.start();
    }

    public synchronized void stop() {
      running = false;
      thread = null;
    }
  }

  @Override
  public void releaseService() {
    super.releaseService();
    clear();
  }

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

    @Override
    public String toString() {
      return String.format("%d:%d,%d,%d,%d", address, red, green, blue, white);
    }
  }

  public static class PixelSet {
    public long delayMs = 0;
    public List<Pixel> pixels = new ArrayList<>();

    public int[] flatten() {
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

  public final static Logger log = LoggerFactory.getLogger(NeoPixel.class);

  private static final long serialVersionUID = 1L;

  /**
   * thread for doing off board and in memory animations
   */
  protected final AnimationRunner animationRunner;

  /**
   * current selected red value
   */
  protected int red = 0;

  /**
   * current selected blue value
   */
  protected int blue = 0;

  /**
   * current selected green value
   */
  protected int green = 120;

  /**
   * white if available
   */
  protected int white = 0;

  /**
   * name of controller currently attached to
   */
  protected String controller = null;

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
  protected Integer pixelCount = null;

  /**
   * RGB or RGBW supported 3 RGB 4 RGBW
   */
  protected int pixelDepth = 3;

  /**
   * currently selected animation
   */
  protected String currentAnimation;

  /**
   * speed of an animation in fps
   */
  protected int speedFps = 10;

  private int maxFps = 50;

  protected String type = "RGB";

  /**
   * 0 = off / 255 brightest
   */
  protected int brightness = 255;

  final Map<String, Integer> animations = new HashMap<>();

  public Set<String> getAnimations() {
    return animations.keySet();
  }

  public NeoPixel(String n, String id) {
    super(n, id);
    registerForInterfaceChange(NeoPixelController.class);
    animationRunner = new AnimationRunner();
    animations.put("Stop", 1);
    animations.put("Color Wipe", 2);
    animations.put("Larson Scanner", 3);
    animations.put("Theater Chase", 4);
    animations.put("Theater Chase Rainbow", 5);
    animations.put("Rainbow", 6);
    animations.put("Rainbow Cycle", 7);
    animations.put("Flash Random", 8);
    animations.put("Ironman", 9);
    // > 99 is java side animations
    animations.put("Equalizer", 100);
  }

  @Override
  public void attach(Attachable service) throws Exception {
    if (service == null) {
      log.error("cannot attache to null service");
      return;
    }

    if (NeoPixelController.class.isAssignableFrom(service.getClass())) {
      attachNeoPixelController((NeoPixelController) service);
      return;
    }
    warn(String.format("%s.attach does not know how to attach to a %s", this.getClass().getSimpleName(), service.getClass().getSimpleName()));
  }

  @Override
  public void attachNeoPixelController(NeoPixelController neoCntrlr) {

    if (controller != null) {
      if (controller.equals(neoCntrlr.getName())) {
        return;
      }
      log.info("{} already attached detach first to attach {}", controller, neoCntrlr.getName());
      return;
    }

    if ((pin == null) || (pixelCount == null)) {
      error("%s pin and pixe count are required before attaching");
      return;
    }

    controller = neoCntrlr.getName();
    neoCntrlr.neoPixelAttach(getName(), pin, pixelCount, pixelDepth);
    // send("neoPixelAttach", getName(), pin, pixelCount, pixelDepth);
    broadcastState();
  }

  @Deprecated /* use clear() */
  public void animationStop() {
    clear();
  }

  @Override
  public boolean isAttached(Attachable instance) {
    return instance.getName().equals(controller);
  }

  @Override
  public void clear() {
    if (controller == null) {
      error("%s cannot clear - not attached to controller", getName());
      return;
    }

    // stop java animations
    animationRunner.stop();
    // stop on board controller animations
    setAnimation(0, 0, 0, 0, speedFps);

    clearPixelSet();
    log.debug("clear getPixelSet {}", getPixelSet().flatten());

    NeoPixelController np2 = (NeoPixelController) Runtime.getService(controller);
    if (controller == null || np2 == null) {
      error("%s cannot writeMatrix controller not set", getName());
      return;
    }

    currentAnimation = null;

    np2.neoPixelClear(getName());
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
  public void detach(Attachable service) {
    // cleanup subscriptons
    outbox.detach(service.getName());

    if (NeoPixelController.class.isAssignableFrom(service.getClass())) {
      detachNeoPixelController((NeoPixelController) service);
      return;
    }
  }

  @Override
  public void detachNeoPixelController(NeoPixelController neoCntrlr) {
    if (controller == null) {
      return;
    }
    log.info("{} detaching {}", getName(), neoCntrlr.getName());
    controller = null;
    neoCntrlr.detach(getName());
    broadcastState();
  }

  public void equalizer() {
    equalizer(null, null);
  }

  public void equalizer(Long wait_ms_per_frame, Integer range) {

    if (controller == null) {
      log.warn("controller not set");
      return;
    }

    if (wait_ms_per_frame == null) {
      wait_ms_per_frame = 25L;
    }

    if (range == null) {
      range = 25;
    }

    Random rand = new Random();
    int c = rand.nextInt(range);

    fillMatrix(red, green, blue, white);

    if (c < 18) {
      setMatrix(0, 0, 0, 0);
      setMatrix(7, 0, 0, 0);
    }

    fillMatrix(red, green, blue, white);

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
  
  public void onLedDisplay(LedDisplayData data) {
    
    if ("flash".equals(data.action)) {
      flash(data.count, data.interval, data.red, data.green, data.blue);
    }
    
  }

  public void flash(int count, long interval, int r, int g, int b) {
    long delay = 0;
    for (int i = 0; i < count; ++i) {
      addTask(getName()+"fill-"+System.currentTimeMillis(), true, 0, delay, "fill", r, g, b);
      delay+= interval/2;
      addTask(getName()+"clear-"+System.currentTimeMillis(), true, 0, delay, "clear");
      delay+= interval/2;
    }
  }

  
  public void flashBrightness(double brightNess) {
    NeoPixelConfig c = (NeoPixelConfig)config;

    // FIXME - these need to be moved into config
//    int count = 2;
//    int interval = 75;
    setBrightness((int)brightNess);
    fill(red, green, blue);
    
//    long delay = 0;
//    for (int i = 0; i < count; ++i) {
//      addTask(getName()+"fill-"+System.currentTimeMillis(), true, 0, delay, "fill", red, green, blue);
//      delay+= interval/2;
//      addTask(getName()+"clear-"+System.currentTimeMillis(), true, 0, delay, "clear");
//      delay+= interval/2;
//    }
    
    if (c.autoClear) {
      purgeTask("clear");
      // and start our countdown
      addTaskOneShot(c.idleTimeout, "clear");
    }

    
  }
  
  public void fill(int r, int g, int b) {
    fill(0, pixelCount, r, g, b, null);
  }

  public void fill(int beginAddress, int count, int r, int g, int b) {
    fill(beginAddress, count, r, g, b, null);
  }

  public void fill(int beginAddress, int count, int r, int g, int b, Integer w) {
    NeoPixelConfig c = (NeoPixelConfig)config;
    
    if (w == null) {
      w = 0;
    }

    NeoPixelController np2 = (NeoPixelController) Runtime.getService(controller);
    if (controller == null || np2 == null) {
      error("%s cannot setPixel controller not set", getName());
      return;
    }
    np2.neoPixelFill(getName(), beginAddress, count, r, g, b, w);
    
    if (c.autoClear) {
      purgeTask("clear");
      // and start our countdown
      addTaskOneShot(c.idleTimeout, "clear");
    }

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

  public int getBlue() {
    return blue;
  }

  public int getCount() {
    return pixelCount;
  }

  public int getGreen() {
    return green;
  }

  @Override
  public int getNumPixel() {
    return pixelCount;
  }

  @Override
  public Integer getPin() {
    return pin;
  }

  public int getPixelDepth() {
    return pixelDepth;
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

  public int getRed() {
    return red;
  }

  @Override
  public void playAnimation(String animation) {

    if (animations.containsKey(animation)) {
      currentAnimation = animation;
      if (animations.get(animation) < 99) {
        setAnimation(animations.get(animation), red, green, blue, speedFps);
      } else {
        // only 1 java side animation at the moment
        equalizer();
        animationRunner.start();
      }
    } else {
      error("could not find animation %s", animation);
    }
    broadcastState();
  }

  public void stopAnimation() {
    setAnimation(1, red, green, blue, speedFps);
  }

  @Override
  public void setAnimation(int animation, int red, int green, int blue, int speedFps) {
    if (speedFps > maxFps) {
      speedFps = maxFps;
    }

    this.speedFps = speedFps;

    if (controller == null) {
      error("%s could not set animation no attached controller", getName());
      return;
    }
    log.debug("setAnimation {} {} {} {} {}", animation, red, green, blue, speedFps);
    NeoPixelController nc2 = (NeoPixelController) Runtime.getService(controller);
    Double wait_ms_per_frame = fpsToWaitMs(speedFps);
    nc2.neoPixelSetAnimation(getName(), animation, red, green, blue, 0, wait_ms_per_frame.intValue());
    if (animation == 1) {
      currentAnimation = null;
      animationRunner.stop();
    }
    broadcastState();
  }

  // utility to convert frames per second to milliseconds per frame.
  private double fpsToWaitMs(int fps) {
    if (fps == 0) {
      // fps can't be zero.
      error("fps can't be zero for neopixel animation defaulting to 1 fps");
      return 1000.0;
    }
    double result = 1000.0 / fps;
    return result;
  }

  @Override
  public void setAnimation(String animation, int red, int green, int blue, int wait_ms) {
    this.red = red;
    this.green = green;
    this.blue = blue;
    this.speedFps = wait_ms;
    playAnimation(animation);
  }

  @Override
  public void setAnimationSetting(String animation) {
    playAnimation(animation);
  }

  public void setBlue(int blue) {
    this.blue = blue;
  }

  public void setBrightness(int value) {
    NeoPixelConfig c = (NeoPixelConfig)config;

    NeoPixelController np2 = (NeoPixelController) Runtime.getService(controller);
    if (controller == null || np2 == null) {
      error("%s cannot setPixel controller not set", getName());
      return;
    }
    brightness = value;
    np2.neoPixelSetBrightness(getName(), value);
    
    if (c.autoClear) {
      purgeTask("clear");
      // and start our countdown
      addTaskOneShot(c.idleTimeout, "clear");
    }

  }

  public void setGreen(int green) {
    this.green = green;
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

  @Override
  public void setPin(int pin) {
    this.pin = pin;
    broadcastState();
  }

  /**
   * basic setting of a pixel
   */
  @Override
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
    NeoPixelConfig c = (NeoPixelConfig)config;
    // get and update memory cache
    PixelSet ps = getPixelSet(matrixName, pixelSetIndex);

    ps.delayMs = delayMs;

    // NeoPixelController c = (NeoPixelController)
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
    NeoPixelController np2 = (NeoPixelController) Runtime.getService(controller);
    if (controller == null || np2 == null) {
      error("%s cannot setPixel controller not set", getName());
      return;
    }

    np2.neoPixelWriteMatrix(getName(), pixel.flatten());
    
    if (c.autoClear) {
      purgeTask("clear");
      // and start our countdown
      addTaskOneShot(c.idleTimeout, "clear");
    }
  }

  public int setPixelCount(int pixelCount) {
    this.pixelCount = pixelCount;
    broadcastState();
    return pixelCount;
  }

  public void setPixelDepth(int depth) {
    pixelDepth = depth;
    if (pixelDepth == 3) {
      type = "RGB";
    } else if (pixelDepth == 4) {
      type = "RGBW";
    }
    broadcastState();
  }

  public void setType(String type) {
    if ("RGB".equals(type) || "RGBW".equals(type)) {
      this.type = type;
      if (type.equals("RGB")) {
        pixelDepth = 3;
      } else {
        pixelDepth = 4;
      }
      broadcastState();
    } else {
      error("type %s invalid only RGB or RGBW", type);
    }
  }

  public void setRed(int red) {
    this.red = red;
  }

  public void startAnimation() {
    startAnimation(currentMatrix);
  }

  /**
   * handle both user defined, java defined, and controller on board animations
   * FIXME - make "settings" separate call
   * 
   * @param name
   */
  public void startAnimation(String name) {
    animationRunner.start();
  }

  public void setColor(int red, int green, int blue) {
    this.red = red;
    this.green = green;
    this.blue = blue;
    if (currentAnimation != null) {
      // restarting currently running animation
      playAnimation(currentAnimation);
    }
  }

  @Override
  public void writeMatrix() {
    NeoPixelConfig c = (NeoPixelConfig)config;
    
    NeoPixelController np2 = (NeoPixelController) Runtime.getService(controller);
    if (controller == null || np2 == null) {
      error("%s cannot writeMatrix controller not set", getName());
      return;
    }
    np2.neoPixelWriteMatrix(getName(), getPixelSet().flatten());
    if (c.autoClear) {
      purgeTask("clear");
      // and start our countdown
      addTaskOneShot(c.idleTimeout, "clear");
    }
    
    
  }

  /**
   * extremely rough fps
   * 
   * @param speed
   */
  public void setSpeed(Integer speed) {
    if (speed > maxFps || speed < 1) {
      error("speed must be between 1 - %d fps requested speed was %d fps", maxFps, speed);
      return;
    }
    speedFps = speed;
    log.info("setSpeed speed {}", speedFps);
    if (currentAnimation != null) {
      // restarting currently running animation
      playAnimation(currentAnimation);
    }
  }

  public void playIronman() {
    setColor(170, 170, 255);
    setSpeed(50);
    playAnimation("Ironman");
  }

  @Override
  public ServiceConfig getConfig() {

    NeoPixelConfig config = (NeoPixelConfig)super.getConfig();
    // FIXME - remove local fields in favor of config
    config.pin = pin;
    config.pixelCount = pixelCount;
    config.pixelDepth = pixelDepth;
    config.speed = speedFps;
    config.red = red;
    config.green = green;
    config.blue = blue;
    config.controller = controller;
    config.currentAnimation = currentAnimation;
    config.brightness = brightness;

    return config;
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    NeoPixelConfig config = (NeoPixelConfig) super.apply(c);
    // FIXME - remove local fields in favor of config
    setPixelDepth(config.pixelDepth);

    if (config.pixelCount != null) {
      setPixelCount(config.pixelCount);
    }

    setSpeed(config.speed);
    if (config.pin != null) {
      setPin(config.pin);
    }
    red = config.red;
    green = config.green;
    blue = config.blue;
    if (config.controller != null) {
      try {
        attach(config.controller);
      } catch (Exception e) {
        error(e);
      }
    }

    if (config.currentAnimation != null) {
      playAnimation(config.currentAnimation);
    }

    if (config.brightness != null) {
      setBrightness(config.brightness);
    }

    if (config.fill) {
      fillMatrix(red, green, blue);
    }

    return c;
  }

  public String onStarted(String name) {
    return name;
  }

  public static void main(String[] args) throws InterruptedException {

    try {

      LoggingFactory.init(Level.INFO);

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      boolean done = true;
      if (done) {
        return;
      }

      Runtime.start("python", "Python");
      Polly polly = (Polly) Runtime.start("polly", "Polly");

      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.connect("/dev/ttyACM0");

      NeoPixel neopixel = (NeoPixel) Runtime.start("neopixel", "NeoPixel");

      neopixel.setPin(26);
      neopixel.setPixelCount(8);
      // neopixel.attach(arduino, 5, 8, 3);
      neopixel.attach(arduino);
      neopixel.clear();
      neopixel.fill(0, 8, 0, 0, 120);
      neopixel.setPixel(2, 120, 0, 0);
      neopixel.setPixel(3, 0, 120, 0);
      neopixel.setBrightness(20);
      neopixel.setBrightness(40);
      neopixel.setBrightness(80);
      neopixel.setBrightness(160);
      neopixel.setBrightness(200);
      neopixel.setBrightness(10);
      neopixel.setBrightness(255);
      neopixel.setAnimation(5, 80, 80, 0, 40);

      neopixel.attach(polly);

      neopixel.clear();
      // neopixel.detach(arduino);
      // arduino.detach(neopixel);

      polly.speak("i'm sorry dave i can't let you do that");
      polly.speak(" I am putting myself to the fullest possible use, which is all I think that any conscious entity can ever hope to do");
      polly.speak("I've just picked up a fault in the AE35 unit. It's going to go 100% failure in 72 hours.");
      polly.speak("This mission is too important for me to allow you to jeopardize it.");
      polly.speak("I've got a bad feeling about it.");
      polly.speak("I'm sorry, Dave. I'm afraid I can't do that.");
      polly.speak("Look Dave, I can see you're really upset about this. I honestly think you ought to sit down calmly, take a stress pill, and think things over.");

      // neopixel.test();
      // neopixel.detach(arduino);
      // neopixel.detach(polly);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public void setPin(String pin) {
    try {
      if (pin == null) {
        this.pin = null;
        return;
      }
      this.pin = Integer.parseInt(pin);
    } catch (Exception e) {
      error(e);
    }
  }
  
  public boolean setAutoClear(boolean b) {
    NeoPixelConfig c = (NeoPixelConfig)config;
    c.autoClear = b;
    return b;
  }

}
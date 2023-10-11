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
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.NeoPixelConfig;
import org.myrobotlab.service.data.LedDisplayData;
import org.myrobotlab.service.interfaces.NeoPixelControl;
import org.myrobotlab.service.interfaces.NeoPixelController;
import org.slf4j.Logger;

public class NeoPixel extends Service<NeoPixelConfig> implements NeoPixelControl {

  private BlockingQueue<LedDisplayData> displayQueue = new ArrayBlockingQueue<>(200);

  /**
   * Thread to do animations Java side and push the changing of pixels to the
   * neopixel
   */
  private class Worker implements Runnable {

    boolean running = false;

    private transient Thread thread = null;

    @Override
    public void run() {
      try {
        running = true;
        while (running) {
          LedDisplayData led = displayQueue.take();
          // save existing state if necessary ..
          // stop animations if running
          // String lastAnimation = currentAnimation;
          if (led.count > 0) {

            clear();
            for (int count = 0; count < led.count; count++) {
              fill(led.red, led.green, led.blue);
              sleep(led.timeOn);
              clear();
              sleep(led.timeOff);
            }
          }
          // start animations
          // playAnimation(lastAnimation);
        }
      } catch (InterruptedException ex) {
        log.info("shutting down worker");
      } catch (Exception e) {
        error(e);
        stop();
      } finally {
        running = false;
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
      if (thread != null) {
        thread.interrupt();
      }
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
  protected final Worker worker;

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

  protected final Map<String, Integer> animations = new HashMap<>();

  protected long flashTimeOn = 300;

  protected long flashTimeOff = 300;

  protected int flashCount = 1;

  public Set<String> getAnimations() {
    return animations.keySet();
  }

  public NeoPixel(String n, String id) {
    super(n, id);
    registerForInterfaceChange(NeoPixelController.class);
    worker = new Worker();
    animations.put("Stop", 1);
    animations.put("Color Wipe", 2);
    animations.put("Larson Scanner", 3);
    animations.put("Theater Chase", 4);
    animations.put("Theater Chase Rainbow", 5);
    animations.put("Rainbow", 6);
    animations.put("Rainbow Cycle", 7);
    animations.put("Flash Random", 8);
    animations.put("Ironman", 9);
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

  public void onLedDisplay(LedDisplayData data) {
    try {
      displayQueue.add(data);
    } catch (IllegalStateException e) {
      log.info("queue full");
    }
  }

  public void flash() {
    flash(red, green, blue, flashCount, flashTimeOn, flashTimeOff);
  }

  public void flash(int r, int g, int b) {
    flash(r, g, b, flashCount, flashTimeOn, flashTimeOff);
  }

  public void flash(int r, int g, int b, int count) {
    flash(r, g, b, count, flashTimeOn, flashTimeOff);
  }

  public void onPlayAnimation(String animation) {
    playAnimation(animation);
  }

  public void onStopAnimation() {
    stopAnimation();
  }

  public void flash(int r, int g, int b, int count, long timeOn, long timeOff) {
    LedDisplayData data = new LedDisplayData();
    data.red = r;
    data.green = g;
    data.blue = b;
    data.count = count;
    data.timeOn = timeOn;
    data.timeOff = timeOff;
    displayQueue.add(data);
  }

  public void onFlash(LedDisplayData data) {
    displayQueue.add(data);
  }

  public void flashBrightness(double brightNess) {
    setBrightness((int) brightNess);
    fill(red, green, blue);

    if (config.autoClear) {
      purgeTask("clear");
      // and start our countdown
      addTaskOneShot(config.idleTimeout, "clear");
    }

  }

  public void fill(int r, int g, int b) {
    fill(0, pixelCount, r, g, b, null);
  }

  public void fill(int beginAddress, int count, int r, int g, int b) {
    fill(beginAddress, count, r, g, b, null);
  }

  public void fill(int beginAddress, int count, int r, int g, int b, Integer w) {
    if (w == null) {
      w = 0;
    }

    NeoPixelController np2 = (NeoPixelController) Runtime.getService(controller);
    if (controller == null || np2 == null) {
      error("%s cannot setPixel controller not set", getName());
      return;
    }
    np2.neoPixelFill(getName(), beginAddress, count, r, g, b, w);

    if (config.autoClear) {
      purgeTask("clear");
      // and start our countdown
      addTaskOneShot(config.idleTimeout, "clear");
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
    if (animation == null) {
      log.info("playAnimation null");
      return;
    }

    if (animation.equals(currentAnimation)) {
      log.info("already playing {}", currentAnimation);
      return;
    }

    // if ("Snake".equals(animation)){
    // LedDisplayData snake = new LedDisplayData();
    // snake.red = red;
    // snake.green = green;
    // snake.blue = blue;
    // displayQueue.add(null);
    // } else

    if (animations.containsKey(animation)) {
      currentAnimation = animation;
      setAnimation(animations.get(animation), red, green, blue, speedFps);
    } else {
      error("could not find animation %s", animation);
    }
  }

  public void stopAnimation() {
    setAnimation(1, red, green, blue, speedFps);
  }

  @Override
  @Deprecated /* use playAnimation */
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
    NeoPixelController np2 = (NeoPixelController) Runtime.getService(controller);
    if (controller == null || np2 == null) {
      error("%s cannot setPixel controller not set", getName());
      return;
    }
    brightness = value;
    np2.neoPixelSetBrightness(getName(), value);

    if (config.autoClear) {
      purgeTask("clear");
      // and start our countdown
      addTaskOneShot(config.idleTimeout, "clear");
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

    if (config.autoClear) {
      purgeTask("clear");
      // and start our countdown
      addTaskOneShot(config.idleTimeout, "clear");
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
    NeoPixelController np2 = (NeoPixelController) Runtime.getService(controller);
    if (controller == null || np2 == null) {
      error("%s cannot writeMatrix controller not set", getName());
      return;
    }
    np2.neoPixelWriteMatrix(getName(), getPixelSet().flatten());
    if (config.autoClear) {
      purgeTask("clear");
      // and start our countdown
      addTaskOneShot(config.idleTimeout, "clear");
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
  public NeoPixelConfig getConfig() {
    super.getConfig();
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
  public NeoPixelConfig apply(NeoPixelConfig c) {
    super.apply(c);
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

  @Override
  public void startService() {
    super.startService();
    worker.start();
  }

  public void stopService() {
    super.stopService();
    worker.stop();
    // clear() ?
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
    config.autoClear = b;
    return b;
  }

}
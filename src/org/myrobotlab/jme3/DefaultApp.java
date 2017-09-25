package org.myrobotlab.jme3;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.jme3.controller.Jme3Servo;
import org.myrobotlab.jme3.interfaces.Jme3App;
import org.myrobotlab.jme3.interfaces.Jme3Object;
import org.myrobotlab.virtual.VirtualMotor;
import org.myrobotlab.virtual.VirtualServo;

/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

/**
 * Sample 4 - how to trigger repeating actions from the main event loop. In this
 * example, you use the loop to make the player character rotate continuously.
 */
public class DefaultApp extends SimpleApplication implements Jme3App {

  /**
   * Not sure if Shape, Box, or some other element or interface should be
   * used.... But at the moment going with boxes - these items would typically
   * correspond to Mrl Services which could display themselves graphically -
   * like Servos...
   */
  transient Map<String, Jme3Object> objects = new HashMap<String, Jme3Object>();

  @Override
  public void simpleInitApp() {
    this.setPauseOnLostFocus(false);
  }

  // Generalized create - is this a good thing ?
  public Geometry create(String name, String type) {
    /** this blue box is our player character */
    Box b = new Box(1, 1, 1);
    /*
     * player = new Geometry("blue cube", b); Material mat = new
     * Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
     * mat.setColor("Color", ColorRGBA.Red); player.setMaterial(mat);
     */
    Geometry cube2Geo = new Geometry("window frame", b);
    Material cube2Mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    cube2Mat.setTexture("ColorMap", assetManager.loadTexture("Textures/ColoredTex/Monkey.png"));
    cube2Mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha); // activate
                                                                       // transparency
    cube2Geo.setQueueBucket(Bucket.Transparent);
    cube2Geo.setMaterial(cube2Mat);
    rootNode.attachChild(cube2Geo);

    Geometry teaGeo = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.j3o");

    return teaGeo;
    // rootNode.attachChild(player);
  }

  /*
   * public Jme3Object create(ServoControl servo){ Jme3Servo jme3Servo = new
   * Jme3Servo(servo, this); objects.put(servo.getName(), jme3Servo); return
   * jme3Servo; }
   */

  /* Use the main event loop to trigger repeating actions. */
  @Override
  public void simpleUpdate(float tpf) {
    // make the player rotate:
    // cube2Geo.rotate(0, 2 * tpf, 0);
    for (String name : objects.keySet()) {
      Jme3Object object = objects.get(name);
      object.simpleUpdate(tpf);
    }
  }

  @Override
  public Jme3Object get(String name) {
    if (objects.containsKey(name)) {
      return objects.get(name);
    }
    return null;
  }

  @Override
  public SimpleApplication getApp() {
    return this;
  }

  public static void main(String[] args) {
    DefaultApp app = new DefaultApp();
    app.start();
  }

  @Override
  public VirtualServo createVirtualServo(String name) {
    Jme3Servo servo = new Jme3Servo(name, this);
    objects.put(name, servo);
    return servo;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public VirtualMotor createVirtualMotor(String name) {
    // TODO Auto-generated method stub
    return null;
  }

}
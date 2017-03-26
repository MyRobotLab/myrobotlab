package org.myrobotlab.jme3.interfaces;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

public abstract class Jme3Object {
  // String name;
  // String type; // not sure if it will be used
  // transient ServiceInterface service; // not sure if it will be used
  String name;
  
  // uber parent of all - moved up
  transient Jme3App app;
  transient protected AssetManager assetManager;
  transient protected Node rootNode;
  transient protected Camera cam;
  
  public Jme3Object(String name, Jme3App app){
    this.name = name;
    this.app = app;
    this.assetManager = app.getApp().getAssetManager();
    this.rootNode = app.getApp().getRootNode();
    this.cam = app.getApp().getCamera();
  }
  
  abstract public void simpleUpdate(float tpf);
  
  public String getName(){
    return name;
  }
  
  abstract public Node getNode();
}

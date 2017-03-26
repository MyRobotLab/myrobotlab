package org.myrobotlab.jme3.controller;

import org.myrobotlab.jme3.interfaces.Jme3App;
import org.myrobotlab.jme3.interfaces.Jme3Object;
import org.myrobotlab.virtual.VirtualServo;
import org.python.jline.internal.Log;

import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

/**
 * Does not Bind !!! (that is a job for VirtualServer) This is a controller,
 * actuator
 *
 */
public class Jme3Servo extends Jme3Object implements VirtualServo {

  // jme3 side
  transient Spatial node;

  int posUs;

  private int lastPosUs;

  // uber parent of all - moved up
  // transient SimpleApplication app;

  public Jme3Servo(String name, Jme3App app) {
    super(name, app);

    // placement - orientation ????

    // graphics
    /** this blue box is our player character */
    Box b = new Box(1, 1, 1);
    /*
     * player = new Geometry("blue cube", b); Material mat = new
     * Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
     * mat.setColor("Color", ColorRGBA.Red); player.setMaterial(mat);
     */
    // Geometry cube2Geo = new Geometry("window frame", b);

    /*
     * This WORKY ! geometry = new Geometry("window frame", b); Material
     * cube2Mat = new Material(assetManager,
     * "Common/MatDefs/Misc/Unshaded.j3md"); cube2Mat.setTexture("ColorMap",
     * assetManager.loadTexture("Textures/ColoredTex/Monkey.png"));
     * cube2Mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha); //
     * activate transparency geometry.setQueueBucket(Bucket.Transparent);
     * geometry.setMaterial(cube2Mat); rootNode.attachChild(geometry);
     * 
     */
    // geometry = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.j3o");
    // geometry = (Geometry)
    // assetManager.loadModel("C:/mrlDevelop/myrobotlab/assets/Models/Teapot/Teapot.j3o");
    // rootNode.attachChild(geometry);

    // Add some objects to the scene: a tea pot
    // Geometry teaGeo = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.j3o");
    /* NO WORKY
    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    mat.setBoolean("UseMaterialColors", true);
    mat.setColor("Diffuse", ColorRGBA.Pink);
    geometry.setMaterial(mat);

    geometry.scale(3);
    geometry.setLocalTranslation(32, 3, -24);
    rootNode.attachChild(geometry);
    cam.lookAt(geometry.getLocalTranslation(), Vector3f.UNIT_Y);
    */
    
    /** Load a teapot model (OBJ file from test-data) */
    //     geometry = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj");
    // node = (Node)assetManager.loadModel("Models/InMoovHead/InMoovHead.j3o");
    // node = (Node)assetManager.loadModel("Models/InMoovHead/InMoovHead.blend");
    node = assetManager.loadModel("Models/Teapot/Teapot.obj");
    /*
    node = assetManager.loadModel("Models/InMoovHead/InMoovHead.j3o");
    node = assetManager.loadModel("Models/VirtualInMoov236/VirtualInMoov236.j3odata");
    node = assetManager.loadModel("Models/VirtualInMoov236/VirtualInMoov236.j3o");
    */
    // node.scale(4.0f, 4.0f, 4.0f);
    // node.setLocalTranslation(0.0f,-2.5f,-4.0f);
    
    /*
    Material mat_default = new Material( assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
    node.setMaterial(mat_default);
    */
    // node.scale(4.0f, 4.0f, 4.0f);
    rootNode.attachChild(node);
    
    
    // sunset light
    DirectionalLight dl = new DirectionalLight();
    dl.setDirection(new Vector3f(-0.1f,-0.7f,1).normalizeLocal());
    dl.setColor(new ColorRGBA(0.44f, 0.30f, 0.20f, 1.0f));
    rootNode.addLight(dl);

    // skylight
    dl = new DirectionalLight();
    dl.setDirection(new Vector3f(-0.6f,-1,-0.6f).normalizeLocal());
    dl.setColor(new ColorRGBA(0.10f, 0.22f, 0.44f, 1.0f));
    rootNode.addLight(dl);

    // white ambient light
    dl = new DirectionalLight();
    dl.setDirection(new Vector3f(1, -0.5f,-0.1f).normalizeLocal());
    dl.setColor(new ColorRGBA(0.80f, 0.70f, 0.80f, 1.0f));
    rootNode.addLight(dl);

  }

  @Override
  public Node getNode() {
    return null;
  }

  @Override
  public void simpleUpdate(float tpf) {
    node.rotate(0, lastPosUs - posUs, 0);
    lastPosUs = posUs;
    // Log.info("simpleUpdate " + tpf);
  }

  @Override
  public void writeMicroseconds(int posUs) {
    // TODO - convert to Radians relative to the initial
    // position config
    // geometry.rotate(0, 2 * posUs, 0);
    this.posUs = posUs;
  }

  @Override
  public void attach(int pin) {
    Log.info("attach {}", pin);
  }

  @Override
  public void detach() {
    // TODO Auto-generated method stub

  }

}

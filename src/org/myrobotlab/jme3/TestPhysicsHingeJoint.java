package org.myrobotlab.jme3;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.joints.HingeJoint;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

public class TestPhysicsHingeJoint extends SimpleApplication implements AnalogListener  {
  
  private BulletAppState bulletAppState;
  private HingeJoint joint;

  public static void main(String[] args) {
      TestPhysicsHingeJoint app = new TestPhysicsHingeJoint();
      app.start();
  }

  private void setupKeys() {
      inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_H));
      inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_K));
      inputManager.addMapping("SwingGui", new KeyTrigger(KeyInput.KEY_SPACE));
      inputManager.addListener(this, "Left", "Right", "SwingGui");
  }

  public void onAnalog(String binding, float value, float tpf) {
      if(binding.equals("Left")){
          joint.enableMotor(true, 1, .1f);
      }
      else if(binding.equals("Right")){
          joint.enableMotor(true, -1, .1f);
      }
      else if(binding.equals("SwingGui")){
          joint.enableMotor(false, 0, 0);
      }
  }

  @Override
  public void simpleInitApp() {
      bulletAppState = new BulletAppState();
      stateManager.attach(bulletAppState);
      bulletAppState.getPhysicsSpace().enableDebug(assetManager);
      setupKeys();
      setupJoint();
  }

  private PhysicsSpace getPhysicsSpace(){
      return bulletAppState.getPhysicsSpace();
  }

  public void setupJoint() {
      Node holderNode=PhysicsTestHelper.createPhysicsTestNode(assetManager, new BoxCollisionShape(new Vector3f( .1f, .1f, .1f)),0);
      holderNode.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(0f,0,0f));
      rootNode.attachChild(holderNode);
      getPhysicsSpace().add(holderNode);

      Node hammerNode=PhysicsTestHelper.createPhysicsTestNode(assetManager, new BoxCollisionShape(new Vector3f( .3f, .3f, .3f)),1);
      hammerNode.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(0f,-1,0f));
      rootNode.attachChild(hammerNode);
      getPhysicsSpace().add(hammerNode);

      joint=new HingeJoint(holderNode.getControl(RigidBodyControl.class), hammerNode.getControl(RigidBodyControl.class), Vector3f.ZERO, new Vector3f(0f,-1,0f), Vector3f.UNIT_Z, Vector3f.UNIT_Z);
      getPhysicsSpace().add(joint);
  }

  @Override
  public void simpleUpdate(float tpf) {
      
  }


}

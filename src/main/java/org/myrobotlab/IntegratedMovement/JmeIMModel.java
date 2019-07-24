package org.myrobotlab.IntegratedMovement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.myrobotlab.framework.Service;
import org.myrobotlab.jme3.IntegratedMovementInterface;
import org.myrobotlab.jme3.Jme3App;
import org.myrobotlab.jme3.MainMenuState;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.JMonkeyEngine;
import org.myrobotlab.service.interfaces.ServoData;
import org.slf4j.Logger;

import com.jme3.app.BasicProfilerState;
import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;

/**
 * @author Christian
 *
 */
public class JmeIMModel extends SimpleApplication {
	public final static Logger log = LoggerFactory.getLogger(Jme3App.class);
	protected transient JmeManager jme = null;
    
	JmeIMModel(JmeManager jme){
	    super(new StatsAppState(), new DebugKeysAppState(), new BasicProfilerState(false),
	            // new OptionPanelState(), // from Lemur
	            // menu = new MainMenuState(jme),
	            new ScreenshotAppState("", System.currentTimeMillis()));
	    this.jme = jme;
	        // setShowSettings(true);
		
	}
	
  @Override
  public void simpleInitApp() {
	    jme.simpleInitApp();
	    Node n =null;
  }



  public void simpleUpdate(float tpf) {
	    jme.simpleUpdate(tpf);
 }


}

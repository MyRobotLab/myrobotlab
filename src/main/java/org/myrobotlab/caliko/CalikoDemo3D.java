package org.myrobotlab.caliko;

import au.edu.federation.caliko.FabrikBone3D;
import au.edu.federation.caliko.FabrikChain3D;
import au.edu.federation.caliko.FabrikStructure3D;
import au.edu.federation.caliko.demo.CalikoDemo;
import au.edu.federation.caliko.demo3d.CalikoDemoStructure3D;
import au.edu.federation.caliko.visualisation.Axis;
import au.edu.federation.caliko.visualisation.Camera;
import au.edu.federation.caliko.visualisation.FabrikConstraint3D;
import au.edu.federation.caliko.visualisation.FabrikLine3D;
// import au.edu.federation.caliko.visualisation.FabrikModel3D;
import au.edu.federation.caliko.visualisation.Grid;
import au.edu.federation.caliko.visualisation.MovingTarget3D;
import au.edu.federation.utils.Mat4f;
import au.edu.federation.utils.Utils;
import au.edu.federation.utils.Vec3f;

/**
 * Class to demonstrate some of the features of the Caliko library in 3D.
 * 
 * @author Al Lansley
 * @version 0.7.1 - 20/07/2016
 */
public class CalikoDemo3D implements CalikoDemo
{
	static float defaultBoneLength      = 10.0f;
	static float boneLineWidth          = 5.0f;
	static float constraintLineWidth    = 2.0f;	
	static float baseRotationAmountDegs = 0.3f;
	
	// Set yo a camera which we'll use to navigate. Params: location, orientation, width and height of window.
	static Camera camera = new Camera(new Vec3f(0.0f, 00.0f, 150.0f), new Vec3f(), 800, 600);
		
	// Setup some grids to aid orientation
	static float extent       = 1000.0f;
	static float gridLevel    = 100.0f;
	static int   subdivisions = 20;
	static Grid  lowerGrid    = new Grid(extent, extent, -gridLevel, subdivisions);
	static Grid  upperGrid    = new Grid(extent, extent,  gridLevel, subdivisions);
	
	// An axis to show the X/Y/Z orientation of each bone. Params: Axis length, axis line width
	static Axis axis = new Axis(3.0f, 1.0f);
		
	// A constraint we can use to draw any joint angle restrictions of ball and hinge joints
	static FabrikConstraint3D constraint = new FabrikConstraint3D();
		
	// A simple Wavefront .OBJ format model of a pyramid to display around each bone (set to draw with a 1.0f line width)
	static FabrikModel3D model = new FabrikModel3D("/pyramid.obj", 1.0f);

	// Setup moving target. Params: location, extents, interpolation frames, grid height for vertical bar
	static MovingTarget3D target = new MovingTarget3D(new Vec3f(0, -30, 0), new Vec3f(60.0f), 200, gridLevel);
	
	private FabrikStructure3D mStructure;
	
	private CalikoDemoStructure3D demoStructure3d;
	
	private transient Application application;
	
	/**
	 * Constructor.
	 * 
	 * @param	demoNumber	The number of the demo to set up.
	 */
	public CalikoDemo3D(Application application) 
	{
	  this.application = application; 
		setup(0);
	}
	
	/**
	 * Set up a demo consisting of an arrangement of 3D IK chains with a given configuration.
	 * 
	 * @param	demoNumber	The number of the demo to set up.
	 */
	public void setup(int demoNumber) 
	{
	  this.mStructure = application.getService().getStructure();
	  
	  this.demoStructure3d = new GuiDemoStructure(application.getService(), null);

		// Set the appropriate window title and make an initial solve pass of the structure
		application.window.setWindowTitle(this.mStructure.getName());
		//structure.updateTarget( target.getCurrentLocation() );
	}
	
	/** Set all chains in the structure to be in fixed-base mode whereby the base locations cannot move. */
	public void setFixedBaseMode(boolean value) { mStructure.setFixedBaseMode(value); }
		
	/** Handle the movement of the camera using the W/S/A/D keys. */
	public void handleCameraMovement(int key, int action) { camera.handleKeypress(key, action); }
	
	public void draw()
	{
		// Move the camera based on keypresses and mouse movement
		camera.move(1.0f / 60.0f);
			
		// Get the ModelViewProjection matrix as we use it multiple times
		Mat4f mvpMatrix = application.window.getMvpMatrix();

		// Draw our grids
    lowerGrid.draw(mvpMatrix);
    upperGrid.draw(mvpMatrix);
    
    // If we're not paused then step the target and solve the structure for the new target location
    if (!application.paused)
    {
    	target.step();
  		this.demoStructure3d.drawTarget(mvpMatrix);
    	
    	// Solve the structure (chains with embedded targets will use those, otherwise the provided target is used)
    	mStructure.solveForTarget( target.getCurrentLocation() );
    	
    	FabrikChain3D chain = application.getService().getChain("default");
    	
      for (FabrikBone3D bone : chain.getChain()) {
        bone.getStartLocation().getGlobalPitchDegs();
        bone.getStartLocation().getGlobalYawDegs();

        System.out.println("Bone X: " + bone.getStartLocation().toString());
      }
    	
    }
    
    // If we're in rotate base mode then rotate the base location(s) of all chains in the structure
    if (application.rotateBasesMode)
    {
    	int numChains = mStructure.getNumChains();
    	for (int loop = 0; loop < numChains; ++loop)
    	{
    		Vec3f base = mStructure.getChain(loop).getBaseLocation();
        	base       = Vec3f.rotateAboutAxisDegs(base, baseRotationAmountDegs, CalikoDemoStructure3D.Y_AXIS);            
        	mStructure.getChain(loop).setBaseLocation(base);
    	}
    }
    
    // Draw the target
    target.draw(Utils.YELLOW, 8.0f, mvpMatrix);
    
    // Draw the structure as required
    // Note: bone lines are drawn in the bone colour, models are drawn in white by default but you can specify a colour to the draw method,
    //       axes are drawn X/Y/Z as Red/Green/Blue and constraints are drawn the colours specified in the FabrikConstraint3D class.
    if (application.drawLines)       { FabrikLine3D.draw(mStructure, boneLineWidth, mvpMatrix);                                       }            
    if (application.drawModels)      { model.drawStructure(mStructure, camera.getViewMatrix(), application.window.mProjectionMatrix); }         
		if (application.drawAxes)        { axis.draw(mStructure, camera.getViewMatrix(), application.window.mProjectionMatrix);           }
		if (application.drawConstraints) { constraint.draw(mStructure, constraintLineWidth, mvpMatrix);                                   }
	}
}

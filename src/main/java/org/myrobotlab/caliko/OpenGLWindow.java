package org.myrobotlab.caliko;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_FOCUSED;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_C;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_L;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_M;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_P;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_X;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_SAMPLES;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glClearDepth;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;
import org.myrobotlab.service.Caliko;
import org.myrobotlab.service.config.CalikoConfig;

import au.edu.federation.caliko.visualisation.Axis;
import au.edu.federation.caliko.visualisation.Camera;
import au.edu.federation.caliko.visualisation.Grid;
//import au.edu.federation.caliko.demo2d.CalikoDemoStructure2DFactory.CalikoDemoStructure2DEnum;
//import au.edu.federation.caliko.demo3d.CalikoDemoStructure3DFactory.CalikoDemoStructure3DEnum;
import au.edu.federation.utils.Mat4f;
import au.edu.federation.utils.Utils;
import au.edu.federation.utils.Vec2f;
import au.edu.federation.utils.Vec3f;

/**
 * Class to set up an OpenGL window.
 * 
 * @author Al Lansley
 * @version 0.3 - 07/12/2015
 */
public class OpenGLWindow
{
  
	// Mouse cursor locations in screen-space and world-space
	public Vec2f screenSpaceMousePos = null;
	public Vec2f worldSpaceMousePos  = new Vec2f();
	
	// Window properties
    long  mWindowId;	
	int   mWindowWidth;
	int   mWindowHeight;	
	float mAspectRatio;
	
	// Matrices
	Mat4f mProjectionMatrix;
	Mat4f mModelMatrix = new Mat4f(1.0f);
	Mat4f mMvpMatrix   = new Mat4f();
	
	// Matrix properties
	boolean mOrthographicProjection; // Use orthographic projection? If false, we use a standard perspective projection
	float mVertFoVDegs;
	float mZNear;
	float mZFar;
	float mOrthoExtent;
	
	// We need to strongly reference callback instances so that they don't get garbage collected.
    private GLFWErrorCallback       errorCallback;
    private GLFWKeyCallback         keyCallback;
    private GLFWWindowSizeCallback  windowSizeCallback;
    private GLFWMouseButtonCallback mouseButtonCallback;
    private GLFWCursorPosCallback   cursorPosCallback;
    
    CalikoConfig config = null;
    
    Caliko service = null;
    public Application application;
    
    // Constructor
    public OpenGLWindow(Application application, Caliko service, int windowWidth, int windowHeight, float vertFoVDegs, float zNear, float zFar, float orthoExtent)
    {
      this.service = service;
      this.application = application;
      
    	// Set properties and create the projection matrix
    	mWindowWidth  = windowWidth <= 0 ? 1 : windowWidth;
    	mWindowHeight = windowHeight <= 0 ? 1 : windowHeight;
    	mAspectRatio  = (float)mWindowWidth / (float)mWindowHeight; 
    	
    	mVertFoVDegs = vertFoVDegs;
    	mZNear       = zNear;
    	mZFar        = zFar;
    	mOrthoExtent = orthoExtent; 
    	
    	config = service.getConfig();
    	
    	screenSpaceMousePos = new Vec2f(config.windowWidth / 2.0f, config.windowHeight / 2.0f);
    	
//    	if (config.use3dDemo)
    	
    		mOrthographicProjection = false;
    		mProjectionMatrix = Mat4f.createPerspectiveProjectionMatrix(mVertFoVDegs, mAspectRatio, mZNear, mZFar);
    	
//    	else
//    	{
//    		mOrthographicProjection = true;
//    		mProjectionMatrix = Mat4f.createOrthographicProjectionMatrix(-mOrthoExtent, mOrthoExtent, mOrthoExtent, -mOrthoExtent, mZNear, mZFar);
//    	}
    	
    	// Setup the error callback to output to System.err
    	glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
 
        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() ) { throw new IllegalStateException("Unable to initialize GLFW"); }
 
        // ----- Specify window hints -----
        // Note: Window hints must be specified after glfwInit() (which resets them) and before glfwCreateWindow where the context is created.
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);                 // Request OpenGL version 3.3 (the minimum we can get away with)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE); // We want a core profile without any deprecated functionality...
        //glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);         // ...however we do NOT want a forward compatible profile as they've removed line widths!
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);                       // We want the window to be resizable
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);                         // We want the window to be visible (false makes it hidden after creation)
        glfwWindowHint(GLFW_FOCUSED, GLFW_TRUE);                         // We want the window to take focus on creation
        glfwWindowHint(GLFW_SAMPLES, 4);                               // Ask for 4x anti-aliasing (this doesn't mean we'll get it, though) 
                
        // Create the window
        mWindowId = glfwCreateWindow(mWindowWidth, mWindowHeight, "LWJGL3 Test", NULL, NULL);        
        if (mWindowId == NULL) { throw new RuntimeException("Failed to create the GLFW window"); }
        
        // Get the resolution of the primary monitor
        GLFWVidMode vidmode = glfwGetVideoMode( glfwGetPrimaryMonitor() );
        int windowHorizOffset = (vidmode.width()  - mWindowWidth)  / 2;
        int windowVertOffset  = (vidmode.height() - mWindowHeight) / 2;
                
        glfwSetWindowPos(mWindowId, windowHorizOffset, windowVertOffset); // Center our window
        glfwMakeContextCurrent(mWindowId);                                // Make the OpenGL context current
        glfwSwapInterval(1);                                              // Swap buffers every frame (i.e. enable vSync)
        
        // This line is critical for LWJGL's interoperation with GLFW's OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread, creates the ContextCapabilities instance and makes
        // the OpenGL bindings available for use.
        glfwMakeContextCurrent(mWindowId);
        
        // Enumerate the capabilities of the current OpenGL context, loading forward compatible capabilities
        GL.createCapabilities(true);
        
        // Setup our keyboard, mouse and window resize callback functions
        setupCallbacks();
        
        // ---------- OpenGL settings -----------

        // Set the clear color
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        
        // Specify the size of the viewport. Params: xOrigin, yOrigin, width, height
     	glViewport(0, 0, mWindowWidth, mWindowHeight);

     	// Enable depth testing
     	glDepthFunc(GL_LEQUAL);
     	glEnable(GL_DEPTH_TEST);

     	// When we clear the depth buffer, we'll clear the entire buffer
     	glClearDepth(1.0f);

     	// Enable blending to use alpha channels
     	// Note: blending must be enabled to use transparency / alpha values in our fragment shaders.
     	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
     	glEnable(GL_BLEND);
     	
     	glfwShowWindow(mWindowId); // Make the window visible
    }
    
    // Constructor with some sensible projection matrix values hard-coded
    public OpenGLWindow(Application application, Caliko service, int width, int height) { this(application, service, width, height, 35.0f, 1.0f, 5000.0f, 120.0f); }
	
    /** Return a calculated ModelViewProjection matrix.
     * <p>
     * This MVP matrix is the result of multiplying the projection matrix by the view matrix obtained from the camera, and
     * as such is really a ProjectionView matrix or 'identity MVP', however you'd like to term it.
     * 
     * If you want a MVP matrix specific to your model, simply multiply this matrix by your desired model matrix to create
     * a MVP matrix specific to your model.
     *  
     * @return	A calculate ModelViewProjection matrix.
     */
	public Mat4f getMvpMatrix() { return mProjectionMatrix.times( service.getCamera().getViewMatrix() ); }
	
	/**
	 * Return the projection matrix.
	 *
	 * @return	The projection matrix.
	 */
	public Mat4f getProjectionMatrix() { return mProjectionMatrix; }
	
	/** Swap the front and back buffers to update the display. */
	public void swapBuffers() { glfwSwapBuffers(mWindowId); }
	
	/**
	 * Set the window title to the specified String argument.
	 * 
	 * @param	title	The String that will be used as the title of the window.
	 */
	public void setWindowTitle(String title)   { glfwSetWindowTitle(mWindowId, title); }
	
	/** Destroy the window, finish up glfw and release all callback methods. */
	public void cleanup()
	{
		// Free the window callbacks and destroy the window
		//glfwFreeCallbacks(mWindowId);
		cursorPosCallback.close();
		mouseButtonCallback.close();
		windowSizeCallback.close();
        keyCallback.close();   
		
		glfwDestroyWindow(mWindowId);
		
		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}
	
	// Setup keyboard, mouse cursor, mouse button and window resize callback methods.
	private void setupCallbacks()
	{	
        // Key callback
		glfwSetKeyCallback(mWindowId, keyCallback = GLFWKeyCallback.create( (long window, int key, int scancode, int action, int mods) ->        
        {        	
           	if (action == GLFW_PRESS)
        	{
        		switch (key)
        		{            	
//        			// Setup demos
//	            	case GLFW_KEY_RIGHT:						
//	            		if (config.use3dDemo)
//	            		{
//	            			if (config.demoNumber < CalikoDemoStructure3DEnum.values().length) { config.demoNumber++; }
//	            		}
//	            		else // 2D Demo mode
//	            		{
//	            			if (config.demoNumber < CalikoDemoStructure2DEnum.values().length) { config.demoNumber++; }
//	            		}
//						config.demo.setup(config.demoNumber);	            		
//						break;	
//	            	case GLFW_KEY_LEFT:						
//	            		if (config.demoNumber > 1) { config.demoNumber--; }
//						config.demo.setup(config.demoNumber);	            		
//						break;				
//						
//					// Toggle fixed base mode
//					case GLFW_KEY_F:
//						config.fixedBaseMode = !config.fixedBaseMode;
//						config.demo.setFixedBaseMode(config.fixedBaseMode);
//						break;
//					// Toggle rotating bases
//					case GLFW_KEY_R:
//						config.rotateBasesMode = !config.rotateBasesMode;
//						break;
						
					// Various drawing options
					case GLFW_KEY_C:
						config.drawConstraints = !config.drawConstraints;
						break;					
					case GLFW_KEY_L:
						config.drawLines = !config.drawLines;
						break;
					case GLFW_KEY_M:
						config.drawModels = !config.drawModels;
						break;
					case GLFW_KEY_P:
						mOrthographicProjection = !mOrthographicProjection;
					 	if (mOrthographicProjection)
					 	{
					 		mProjectionMatrix = Mat4f.createOrthographicProjectionMatrix(-mOrthoExtent, mOrthoExtent, mOrthoExtent, -mOrthoExtent, mZNear, mZFar);
					 	}
					 	else
					 	{
					 		mProjectionMatrix = Mat4f.createPerspectiveProjectionMatrix(mVertFoVDegs, mAspectRatio, mZNear, mZFar);
					 	}
						break;
					case GLFW_KEY_X:
						config.drawAxes = !config.drawAxes;
						break;
						
					// Camera controls
					case GLFW_KEY_W:
            		case GLFW_KEY_S:
            		case GLFW_KEY_A:
            		case GLFW_KEY_D:
            			// if (config.use3dDemo) { config.demo.handleCameraMovement(key, action); }
            			break;
            			
            		// Close the window
            		case GLFW_KEY_ESCAPE:
            			glfwSetWindowShouldClose(window, true);
            			break;
            			
            		// Cycle through / switch between 2D and 3D demos with the up and down cursors
            		case GLFW_KEY_UP:
            		case GLFW_KEY_DOWN:
            			config.use3dDemo = !config.use3dDemo;
            			config.demoNumber = 1;
            			
            			// Viewing 2D demos?
            			if (!config.use3dDemo)
            			{
            				mOrthographicProjection = true;
            				mProjectionMatrix = Mat4f.createOrthographicProjectionMatrix(-mOrthoExtent, mOrthoExtent, mOrthoExtent, -mOrthoExtent, mZNear, mZFar);            				
            				// config.demo = new CalikoDemo2D(config.demoNumber);
            			}
            			else // Viewing 3D demos
            			{            			
            				mOrthographicProjection = false;
            				mProjectionMatrix = Mat4f.createPerspectiveProjectionMatrix(mVertFoVDegs, mAspectRatio, mZNear, mZFar);            				
            				// config.demo = new CalikoDemo3D(config.demoNumber);
            			}
            			break;
            			
            		// Dynamic add/remove bones for first demo
//            		case GLFW_KEY_COMMA:
//            			if (config.demoNumber == 1 && config.structure.getChain(0).getNumBones() > 1)
//            			{
//            				config.structure.getChain(0).removeBone(0);
//            			}
//            			break;
//            		case GLFW_KEY_PERIOD:
//            			if (config.demoNumber == 1)
//            			{
//            				config.structure.getChain(0).addConsecutiveBone(config.X_AXIS, config.defaultBoneLength);
//            			}
//            			break;
            			
            		case GLFW_KEY_SPACE:
            			application.paused = !application.paused;
            			break;
            			
        		} // End of switch
        		
        	}         	
           	else if (action == GLFW_REPEAT || action == GLFW_RELEASE) // Camera must also handle repeat or release actions
        	{
        		switch (key)
        		{
	            	case GLFW_KEY_W:
	        		case GLFW_KEY_S:
	        		case GLFW_KEY_A:
	        		case GLFW_KEY_D:
	        			//if (config.use3dDemo) { config.demo.handleCameraMovement(key, action); }
	        			break;
        		}
        	}
        }));
        
        // Mouse cursor position callback
        glfwSetCursorPosCallback(mWindowId, cursorPosCallback = GLFWCursorPosCallback.create( (long windowId, double mouseX,  double mouseY) ->
        {   
        	// Update the screen space mouse location
        	screenSpaceMousePos.set( (float)mouseX, (float)mouseY );
        	
        	// If we're holding down the LMB, then...
        	if (config.leftMouseButtonDown)
        	{
        		// ...in the 3D demo we update the camera look direction...
        		if (config.use3dDemo)
        		{
    				service.getCamera().handleMouseMove(mouseX, mouseY);
    			}
        		else // ...while in the 2D demo we update the 2D target.
        		{	
        			// Convert the mouse position in screen-space coordinates to our orthographic world-space coordinates
//					worldSpaceMousePos.set(  Utils.convertRange(screenSpaceMousePos.x, 0.0f,  mWindowWidth, -mOrthoExtent, mOrthoExtent),
//                                             -Utils.convertRange(screenSpaceMousePos.y, 0.0f, mWindowHeight, -mOrthoExtent, mOrthoExtent) );
//					
//					CalikoDemo2D.mStructure.solveForTarget(worldSpaceMousePos);
        		}
        	}    		
        }));
        
        // Mouse button callback
        glfwSetMouseButtonCallback(mWindowId, mouseButtonCallback = GLFWMouseButtonCallback.create( (long windowId, int button, int action, int mods) ->
        {
			// If the left mouse button was the button that invoked the callback...
			if (button == GLFW_MOUSE_BUTTON_1)
			{	
				// ...then set the LMB status flag accordingly
				// Note: We cannot simply toggle the flag here as double-clicking the title bar to fullscreen the window confuses it and we
				// then end up mouselook-ing without the LMB being held down!
				if (action == GLFW_PRESS) { config.leftMouseButtonDown = true; } else { config.leftMouseButtonDown = false; }
				
				if (config.use3dDemo)
				{	
					// Immediately set the cursor position to the centre of the screen so our view doesn't "jump" on first cursor position change
					glfwSetCursorPos(windowId, ((double)mWindowWidth / 2), ((double)mWindowHeight / 2) );
					
					switch (action)
					{
						case GLFW_PRESS:
							// Make the mouse cursor hidden and put it into a 'virtual' mode where its values are not limited
					        glfwSetInputMode(mWindowId, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
					        break;

						case GLFW_RELEASE:
							// Restore the mouse cursor to normal and reset the camera last cursor position to be the middle of the window
					        glfwSetInputMode(windowId, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
					        service.getCamera().resetLastCursorPosition();
					        break;
					}						
				}
				else
				{	
        			// Convert the mouse position in screen-space coordinates to our orthographic world-space coordinates
					worldSpaceMousePos.set(  Utils.convertRange(screenSpaceMousePos.x, 0.0f,  mWindowWidth, -mOrthoExtent, mOrthoExtent),
                                             -Utils.convertRange(screenSpaceMousePos.y, 0.0f, mWindowHeight, -mOrthoExtent, mOrthoExtent) );
					
					// CalikoDemo2D.mStructure.solveForTarget(worldSpaceMousePos);
				}
				
				// Nothing needs be done in 2D demo mode - the config.leftMouseButtonDown flag plus the mouse cursor handler take care of it.				
			}
		}));
        
        // Window size callback
        glfwSetWindowSizeCallback(mWindowId, windowSizeCallback = GLFWWindowSizeCallback.create( (long windowId, int windowWidth,  int windowHeight) ->
        {   
    		// Update our window width and height and recalculate the aspect ratio
    		if (windowWidth  <= 0) { windowWidth  = 1; }
    		if (windowHeight <= 0) { windowHeight = 1; }        		
    		mWindowWidth  = windowWidth;
    		mWindowHeight = windowHeight;
    		mAspectRatio  = (float)mWindowWidth / (float)mWindowHeight;
    		
    		// Let our camera know about the new size so it can correctly recentre the mouse cursor
    		service.getCamera().updateWindowSize(windowWidth, windowHeight);
    		
    		// Update our viewport
    		glViewport(0, 0, mWindowWidth, mWindowHeight);

    		// Recalculate our projection matrix
    		if (mOrthographicProjection)
		 	{
		 		mProjectionMatrix = Mat4f.createOrthographicProjectionMatrix(-mOrthoExtent, mOrthoExtent, mOrthoExtent, -mOrthoExtent, mZNear, mZFar);
		 	}
		 	else
		 	{
		 		mProjectionMatrix = Mat4f.createPerspectiveProjectionMatrix(mVertFoVDegs, mAspectRatio, mZNear, mZFar);
		 	}
        }));
        
	} // End of setupCallbacks method
	
} // End of OpenGLWindow class
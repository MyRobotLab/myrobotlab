package org.myrobotlab.headtracking;

import static com.oculusvr.capi.OvrLibrary.OVR_DEFAULT_EYE_HEIGHT;
import static com.oculusvr.capi.OvrLibrary.OVR_DEFAULT_IPD;
import static com.oculusvr.capi.OvrLibrary.ovrProjectionModifier.ovrProjection_ClipRangeOpenGL;
import static com.oculusvr.capi.OvrLibrary.ovrProjectionModifier.ovrProjection_RightHanded;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;

import java.awt.Rectangle;
import java.io.Serializable;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.PixelFormat;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OculusRift;
import org.myrobotlab.service.data.OculusData;
import org.saintandreas.gl.FrameBuffer;
import org.saintandreas.gl.MatrixStack;
import org.saintandreas.gl.SceneHelpers;
import org.saintandreas.gl.app.LwjglApp;
import org.saintandreas.math.Matrix4f;
import org.saintandreas.math.Vector3f;
import org.saintandreas.vr.RiftUtils;
import org.slf4j.Logger;

import com.oculusvr.capi.EyeRenderDesc;
import com.oculusvr.capi.FovPort;
import com.oculusvr.capi.GLTexture;
import com.oculusvr.capi.Hmd;
import com.oculusvr.capi.HmdDesc;
import com.oculusvr.capi.LayerEyeFov;
import com.oculusvr.capi.OvrLibrary;
import com.oculusvr.capi.OvrMatrix4f;
import com.oculusvr.capi.OvrRecti;
import com.oculusvr.capi.OvrSizei;
import com.oculusvr.capi.OvrVector2i;
import com.oculusvr.capi.OvrVector3f;
import com.oculusvr.capi.Posef;
import com.oculusvr.capi.SwapTextureSet;
import com.oculusvr.capi.TrackingState;
import com.oculusvr.capi.ViewScaleDesc;

public class OculusHeadTracking extends LwjglApp implements Runnable, Serializable  {

	public final static Logger log = LoggerFactory.getLogger(OculusHeadTracking.class);
	private static final long serialVersionUID = -4067064437788846187L;
	protected final Hmd hmd;
	protected final HmdDesc hmdDesc;
	boolean running = false;
	transient public OculusRift oculus;
	transient Thread trackerThread = null;
	
	protected int width, height;
	protected float aspect;
	

	private GLContext glContext = new GLContext();
	private int frameCount = -1;


	
	private LayerEyeFov layer = new LayerEyeFov();

	protected final Posef[] poses = Posef.buildPair();
	private final FovPort[] fovPorts = FovPort.buildPair();
	private final Matrix4f[] projections = new Matrix4f[2];
	private final OvrSizei[] textureSizes = new OvrSizei[2];

	private SwapTextureSet swapTexture = null;
	private GLTexture mirrorTexture = null;

	private final ViewScaleDesc viewScaleDesc = new ViewScaleDesc();
	private FrameBuffer frameBuffer = null;
	private final OvrVector3f[] eyeOffsets = OvrVector3f.buildPair();

	private float ipd;
	private float eyeHeight;

	protected ContextAttribs contextAttributes = new ContextAttribs();
	protected PixelFormat pixelFormat = new PixelFormat();
	
	public OculusHeadTracking(Hmd hmd, HmdDesc hmdDesc) {
		// TODO Auto-generated constructor stub
		this.hmd = hmd;
		this.hmdDesc = hmdDesc;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		
		// maybe this is where we update our display loop / gl stuff.
		if (Display.wasResized()) {
			onResize(Display.getWidth(), Display.getHeight());
		}
		//if (Display.isActive()) {
			// update();
			drawFrame();

			// // Display update combines both input processing and
			// // buffer swapping. We want only the input processing
			// // so we have to call processMessages.
			// Display.processMessages();
			Display.update();
		//}
		
		
		running = true;
		while (running) {
			
			TrackingState trackingState = hmd.getTrackingState(0);
			double w = Math.toDegrees(trackingState.HeadPose.Pose.Orientation.w);
	  		// rotations about x axis  (pitch)
	  		double pitch = Math.toDegrees(trackingState.HeadPose.Pose.Orientation.x);
	  		// rotation about y axis (yaw)
	  		double yaw = Math.toDegrees(trackingState.HeadPose.Pose.Orientation.y);
	  		// rotation about z axis (roll)
	  		double roll = Math.toDegrees(trackingState.HeadPose.Pose.Orientation.z);
	  		
			// log.info("Roll: " + z*RAD_TO_DEGREES);
			// log.info("Pitch:"+ x*RAD_TO_DEGREES);
			// log.info("Yaw:"+ y*RAD_TO_DEGREES );
	  		
	  		OculusData headTrackingData = new OculusData(roll, pitch, yaw);
	  		oculus.invoke("publishOculusData", headTrackingData);
	  		
	  		try {
	  			// TODO: should we have a minor pause here?
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				// oops.. bomb out.
				break;
			}
	  		
	  		
	  		
		}
		
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public OculusRift getOculus() {
		return oculus;
	}

	public void setOculus(OculusRift oculus) {
		this.oculus = oculus;
	}

	public void start() {
		log.info("starting head tracking");
		if (trackerThread != null) {
			log.info("video processor already started");
			return;
		}
		trackerThread = new Thread(this, String.format("%s_oculusHeadTracking", oculus.getName()));
		trackerThread.start();
		
		// start our display?
		startDisplay();
		
 	}

	public void stop() {
		// TODO Auto-generated method stub
		log.debug("stopping head tracking");
        running = false;
        trackerThread = null;
	}

	
	// TODO: replace all of this
	// with an OculusRiftDisplay subclass that extends LwjglApp 
	private void startDisplay() {	
		
		for (int eye = 0; eye < 2; ++eye) {
			fovPorts[eye] = hmdDesc.DefaultEyeFov[eye];
			OvrMatrix4f m = Hmd.getPerspectiveProjection(fovPorts[eye], 0.1f, 1000000f, ovrProjection_RightHanded
					| ovrProjection_ClipRangeOpenGL);
			projections[eye] = RiftUtils.toMatrix4f(m);
			textureSizes[eye] = hmd.getFovTextureSize(eye, fovPorts[eye], 1.0f);
		}
		
		try {
			
			ipd = hmd.getFloat(OvrLibrary.OVR_KEY_IPD, OVR_DEFAULT_IPD);
			eyeHeight = hmd.getFloat(OvrLibrary.OVR_KEY_EYE_HEIGHT, OVR_DEFAULT_EYE_HEIGHT);
			
			setupContext();
			setupDisplay();
			Display.create(pixelFormat, contextAttributes);
			// This supresses a strange error where when using 
			// the Oculus Rift in direct mode on Windows, 
			// there is an OpenGL GL_INVALID_FRAMEBUFFER_OPERATION 
			// error present immediately after the context has been created.  
			@SuppressWarnings("unused")
			int err = glGetError();
			GLContext.useContext(glContext, false);

			Mouse.create();
			Keyboard.create();
		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		}

		System.out.println("Ok , we're here..");

		// ?!
		glContext.getCapabilities();

		initGl();


	};
	
	protected void initGl() {

		Display.setVSyncEnabled(false);
		OvrSizei doubleSize = new OvrSizei();
		doubleSize.w = textureSizes[0].w + textureSizes[1].w;
		doubleSize.h = textureSizes[0].h;
		swapTexture = hmd.createSwapTexture(doubleSize, GL_RGBA);
		mirrorTexture = hmd.createMirrorTexture(new OvrSizei(width, height), GL_RGBA);

		layer.Header.Type = OvrLibrary.ovrLayerType.ovrLayerType_EyeFov;
		layer.ColorTexure[0] = swapTexture;
		layer.Fov = fovPorts;
		layer.RenderPose = poses;
		for (int eye = 0; eye < 2; ++eye) {
			layer.Viewport[eye].Size = textureSizes[eye];
			layer.Viewport[eye].Pos  = new OvrVector2i(0, 0);
		}
		layer.Viewport[1].Pos.x = layer.Viewport[1].Size.w;
		frameBuffer = new FrameBuffer(doubleSize.w, doubleSize.h);

		for (int eye = 0; eye < 2; ++eye) {
			EyeRenderDesc eyeRenderDesc = hmd.getRenderDesc(eye, fovPorts[eye]);
			this.eyeOffsets[eye].x = eyeRenderDesc.HmdToEyeViewOffset.x;
			this.eyeOffsets[eye].y = eyeRenderDesc.HmdToEyeViewOffset.y;
			this.eyeOffsets[eye].z = eyeRenderDesc.HmdToEyeViewOffset.z;
		}
		viewScaleDesc.HmdSpaceToWorldScaleInMeters = 1.0f;
	}

	
	protected void setupContext() {
		glContext = new GLContext();
		contextAttributes = new ContextAttribs(4, 1).withProfileCore(true).withDebug(true);
	}

	protected void setupDisplay(Rectangle r) {
		setupDisplay(r.x, r.y, r.width, r.height);
	}


	protected void setupDisplay(int left, int top, int width, int height) {
		try {
			Display.setDisplayMode(new DisplayMode(width, height));
			Display.setTitle("Oculus Rift - MyRobotLab");
		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		}
		Display.setLocation(left, top);
		Display.setVSyncEnabled(true);
		onResize(width, height);
	}

	protected void onResize(int width, int height) {
		this.width = width;
		this.height = height;
		this.aspect = (float) width / (float) height;
	}

	protected final void setupDisplay() {
		width = hmdDesc.Resolution.w / 4;
		height = hmdDesc.Resolution.h / 4;
		setupDisplay(new Rectangle(100, 100, width, height));
	}

	public final void drawFrame() {
		
		// System.out.println("Draw Frame called.");
		// ??
		
		width = hmdDesc.Resolution.w / 4;
		height = hmdDesc.Resolution.h / 4;

		++frameCount;
		Posef eyePoses[] = hmd.getEyePoses(frameCount, eyeOffsets);
		frameBuffer.activate();

		MatrixStack pr = MatrixStack.PROJECTION;
		MatrixStack mv = MatrixStack.MODELVIEW;
		GLTexture texture = swapTexture.getTexture(swapTexture.CurrentIndex);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture.ogl.TexId, 0);
		for (int eye = 0; eye < 2; ++eye) {
			OvrRecti vp = layer.Viewport[eye];
			glScissor(vp.Pos.x, vp.Pos.y, vp.Size.w, vp.Size.h);
			glViewport(vp.Pos.x, vp.Pos.y, vp.Size.w, vp.Size.h);
			pr.set(projections[eye]);
			Posef pose = eyePoses[eye];
			// This doesn't work as it breaks the contiguous nature of the array
			// FIXME there has to be a better way to do this
			poses[eye].Orientation = pose.Orientation;
			poses[eye].Position = pose.Position;
			mv.push().preTranslate(RiftUtils.toVector3f(poses[eye].Position).mult(-1))
			.preRotate(RiftUtils.toQuaternion(poses[eye].Orientation).inverse());
			renderScene();
			mv.pop();
		}
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture.ogl.TexId, 0);
		frameBuffer.deactivate();


		hmd.submitFrame(frameCount, layer);
		swapTexture.CurrentIndex++;
		swapTexture.CurrentIndex %= swapTexture.TextureCount;

		// FIXME Copy the layer to the main window using a mirror texture
		glScissor(0, 0, width, height);
		glViewport(0, 0, width, height);
		glClearColor(0.5f, 0.5f, System.currentTimeMillis() % 1000 / 1000.0f, 1);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		// TODO : put something on the screen!
		SceneHelpers.renderTexturedQuad(mirrorTexture.ogl.TexId);
	}

	public void renderScene() {
		// TODO: pass in the left/right images.
		// display them as a texture on a quad?! hrmmm..
		System.out.println("Render Scene:" + Display.getAdapter());
		glClear(GL_DEPTH_BUFFER_BIT);
		SceneHelpers.renderSkybox();
		SceneHelpers.renderFloor();
		MatrixStack mv = MatrixStack.MODELVIEW;
		mv.push();
		mv.translate(new Vector3f(0, eyeHeight, 0)).scale(ipd);
		SceneHelpers.renderColorCube();
		mv.pop();
		mv.push();
		mv.translate(new Vector3f(0, eyeHeight / 2, 0)).scale(new Vector3f(ipd / 2, eyeHeight, ipd / 2));
		SceneHelpers.renderColorCube();
		mv.pop();
	}
	
	// Oculus Direct Mode update methods below.

	private void recenterView() {
		Vector3f center = Vector3f.UNIT_Y.mult(eyeHeight);
		Vector3f eye = new Vector3f(0, eyeHeight, ipd * 10.0f);
		MatrixStack.MODELVIEW.lookat(eye, center, Vector3f.UNIT_Y);
		hmd.recenterPose();
	}

}

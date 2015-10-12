package org.myrobotlab.oculus;

import static com.oculusvr.capi.OvrLibrary.OVR_DEFAULT_EYE_HEIGHT;
import static com.oculusvr.capi.OvrLibrary.OVR_DEFAULT_IPD;
import static com.oculusvr.capi.OvrLibrary.ovrProjectionModifier.ovrProjection_ClipRangeOpenGL;
import static com.oculusvr.capi.OvrLibrary.ovrProjectionModifier.ovrProjection_RightHanded;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.PixelFormat;
import org.myrobotlab.service.OculusRift.RiftFrame;
import org.saintandreas.gl.FrameBuffer;
import org.saintandreas.gl.MatrixStack;
import org.saintandreas.gl.SceneHelpers;
import org.saintandreas.math.Matrix4f;
import org.saintandreas.math.Vector3f;
import org.saintandreas.vr.RiftUtils;
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
import com.oculusvr.capi.ViewScaleDesc;

/**
 * 
 * OculusDisplay - This call will start up a lwjgl instance
 * that will display the rift image in a side by side fashion
 * in the oculus rift display.
 *
 */
public class OculusDisplay implements Runnable {

	protected int width;
	protected int height;
	private final float ipd;
	private final float eyeHeight;
	protected final Hmd hmd;
	protected final HmdDesc hmdDesc;
	protected float aspect = 1.0f;
	ContextAttribs contextAttributes;
	protected PixelFormat pixelFormat = new PixelFormat();
	private GLContext glContext = new GLContext();
	private final FovPort[] fovPorts = FovPort.buildPair();
	protected final Posef[] poses = Posef.buildPair();
	// TODO: can we skip these
	private final Matrix4f[] projections = new Matrix4f[2];
	private final OvrVector3f[] eyeOffsets = OvrVector3f.buildPair();
	private final OvrSizei[] textureSizes = new OvrSizei[2];

	// TODO: understand what these are!
	private SwapTextureSet swapTexture = null;
	// a texture to mirror what is displayed on the rift.
	private GLTexture mirrorTexture = null;
	// this is what gets submitted to the rift for display.
	private LayerEyeFov layer = new LayerEyeFov();
	// ?
	private FrameBuffer frameBuffer = null;
	// keep track of how many frames we have submitted to the display.
	private int frameCount = -1;
	private final ViewScaleDesc viewScaleDesc = new ViewScaleDesc();
	// The last image captured from the OpenCV services (left&right)
	private RiftFrame currentFrame;

	public OculusDisplay() {
		// constructor
		// start up hmd libs
		Hmd.initialize();
		try {
			Thread.sleep(400);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}

		// create it
		hmd = Hmd.create();
		if (null == hmd) {
			throw new IllegalStateException("Unable to initialize HMD");
		}

		// grab the description of the device.
		hmdDesc = hmd.getDesc();
		hmd.configureTracking();
		for (int eye = 0; eye < 2; ++eye) {
			fovPorts[eye] = hmdDesc.DefaultEyeFov[eye];
			OvrMatrix4f m = Hmd.getPerspectiveProjection(fovPorts[eye], 0.1f, 1000000f, ovrProjection_RightHanded
					| ovrProjection_ClipRangeOpenGL);
			projections[eye] = RiftUtils.toMatrix4f(m);
			textureSizes[eye] = hmd.getFovTextureSize(eye, fovPorts[eye], 1.0f);
		}

		ipd = hmd.getFloat(OvrLibrary.OVR_KEY_IPD, OVR_DEFAULT_IPD);
		eyeHeight = hmd.getFloat(OvrLibrary.OVR_KEY_EYE_HEIGHT, OVR_DEFAULT_EYE_HEIGHT);
		// TODO: do i need to center this?
		recenterView();
	}

	private void recenterView() {
		Vector3f center = Vector3f.UNIT_Y.mult(eyeHeight);
		Vector3f eye = new Vector3f(0, eyeHeight, ipd * 10.0f);
		MatrixStack.MODELVIEW.lookat(eye, center, Vector3f.UNIT_Y);
		hmd.recenterPose();
	}

	protected final void setupDisplay() {

		// our size.
		width = hmdDesc.Resolution.w / 4;
		height = hmdDesc.Resolution.h / 4;
		int left = 100;
		int right = 100;

		try {
			Display.setDisplayMode(new DisplayMode(width, height));
		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		}
		Display.setLocation(left, right);
		Display.setVSyncEnabled(true);
		onResize(width, height);
	}

	protected void onResize(int width, int height) {
		this.width = width;
		this.height = height;
		this.aspect = (float) width / (float) height;
	}


	public void run() {
		try {
			contextAttributes = new ContextAttribs(4, 1).withProfileCore(true).withDebug(true);
			setupDisplay();
			Display.create(pixelFormat, contextAttributes);
			// This supresses a strange error where when using 
			// the Oculus Rift in direct mode on Windows, 
			// there is an OpenGL GL_INVALID_FRAMEBUFFER_OPERATION 
			// error present immediately after the context has been created.  
			@SuppressWarnings("unused")
			int err = glGetError();
			GLContext.useContext(glContext, false);
			// TODO: maybe get rid of these?
			Mouse.create();
			Keyboard.create();
		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		}
		initGl();
		while (!Display.isCloseRequested()) {
			if (Display.wasResized()) {
				onResize(Display.getWidth(), Display.getHeight());
			}
			update();
			drawFrame();
			finishFrame();
		}
		onDestroy();
		Display.destroy();
	}

	public final void drawFrame() {
		// System.out.println("Draw Frame called.");
		// TODO: synchronize the current frame updating..
		if (currentFrame == null) {
			return;
		}
		if (currentFrame.left == null || currentFrame.right == null) {
			return;
		}
		
		width = hmdDesc.Resolution.w / 4;
		height = hmdDesc.Resolution.h / 4;
		++frameCount;

		// here it is folks!
		
		Posef eyePoses[] = hmd.getEyePoses(frameCount, eyeOffsets);

		frameBuffer.activate();

		MatrixStack pr = MatrixStack.PROJECTION;
		MatrixStack mv = MatrixStack.MODELVIEW;
		GLTexture texture = swapTexture.getTexture(swapTexture.CurrentIndex);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture.ogl.TexId, 0);


		// GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.ogl.TexId);
		// render each eye in here!
		//		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		//		// left eye
		//		glScissor(0,0, width/2, height);
		//		glClearColor(1,0,0,1);
		//		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		//		// right eye
		//		glScissor(width/2,0, width/2, height);
		//		glClearColor(0,0,1,1);
		//		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		//		GL11.glDisable(GL11.GL_SCISSOR_TEST);


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

			glClearColor(0,0,1,1);
			// ok.. now we have a loop 2x through that gives us our left/right images.
			if (eye == 0) {
				// left screen
				if (currentFrame.left != null) 
					SceneHelpers.renderScreen(currentFrame.left);
			} else {
				// right screen
				if (currentFrame.right != null)
					SceneHelpers.renderScreen(currentFrame.right);	        	
			}
		}

		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture.ogl.TexId, 0);
		frameBuffer.deactivate();

		// This is the Magic Line!!!  it turns the light blue! weee!
		hmd.submitFrame(frameCount, layer);

		// hmd.

		swapTexture.CurrentIndex++;
		swapTexture.CurrentIndex %= swapTexture.TextureCount;

		// FIXME Copy the layer to the main window using a mirror texture
		glScissor(0, 0, width, height);
		glViewport(0, 0, width, height);
		glClearColor(0.5f, 0.5f, System.currentTimeMillis() % 1000 / 1000.0f, 1);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		SceneHelpers.renderTexturedQuad(mirrorTexture.ogl.TexId);


	}


	protected void finishFrame() {
		// // Display update combines both input processing and
		// // buffer swapping. We want only the input processing
		// // so we have to call processMessages.
		// Display.processMessages();
		Display.update();
	}

	protected void initGl() {
		Display.setVSyncEnabled(false);
		OvrSizei doubleSize = new OvrSizei();
		doubleSize.w = textureSizes[0].w + textureSizes[1].w;
		doubleSize.h = textureSizes[0].h;
		swapTexture = hmd.createSwapTexture(doubleSize, GL_RGBA);
		mirrorTexture = hmd.createMirrorTexture(new OvrSizei(width, height), GL_RGBA);
		// TODO: understand the layer types.
		//layer.Header.Type = OvrLibrary.ovrLayerType.ovrLayerType_EyeFov;
		// Direct seems more like what I want.
		layer.Header.Type = OvrLibrary.ovrLayerType.ovrLayerType_Direct;
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


	protected void onDestroy() {
		hmd.destroy();
		Hmd.shutdown();
	}

	protected void update() {
		// TODO: some sort of update logic for the game?
		//				    while (Keyboard.next()) {
		//				      onKeyboardEvent();
		//				    }
		//		
		//				    while (Mouse.next()) {
		//				      onMouseEvent();
		//				    }
		// TODO : nothing?

	}

	public static void main(String[] args) {
		// TODO : noop
		//OculusDisplay test = new OculusDisplay();
		//test.run();
	}

	public RiftFrame getCurrentFrame() {
		return currentFrame;
	}

	public void setCurrentFrame(RiftFrame currentFrame) {
		this.currentFrame = currentFrame;
	}

}

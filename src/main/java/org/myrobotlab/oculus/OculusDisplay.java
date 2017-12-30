package org.myrobotlab.oculus;

import static com.oculusvr.capi.OvrLibrary.OVR_DEFAULT_EYE_HEIGHT;
import static com.oculusvr.capi.OvrLibrary.OVR_DEFAULT_IPD;
import static com.oculusvr.capi.OvrLibrary.ovrProjectionModifier.ovrProjection_ClipRangeOpenGL;
import static com.oculusvr.capi.OvrLibrary.ovrProjectionModifier.ovrProjection_RightHanded;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;

import java.io.IOException;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.Vector3f;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.oculus.lwjgl.entities.Camera;
import org.myrobotlab.oculus.lwjgl.entities.Entity;
import org.myrobotlab.oculus.lwjgl.models.RawModel;
import org.myrobotlab.oculus.lwjgl.models.TexturedModel;
import org.myrobotlab.oculus.lwjgl.renderengine.Loader;
import org.myrobotlab.oculus.lwjgl.renderengine.Renderer;
import org.myrobotlab.oculus.lwjgl.shaders.StaticShader;
import org.myrobotlab.oculus.lwjgl.textures.ModelTexture;
import org.myrobotlab.service.OculusRift;
import org.myrobotlab.service.OculusRift.RiftFrame;
import org.myrobotlab.service.data.Orientation;
import org.saintandreas.gl.FrameBuffer;
import org.saintandreas.gl.MatrixStack;
import org.saintandreas.gl.buffers.VertexArray;
import org.saintandreas.gl.shaders.Program;
import org.saintandreas.gl.textures.Texture;
import org.saintandreas.math.Matrix4f;
import org.saintandreas.vr.RiftUtils;
import org.slf4j.Logger;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
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
 * OculusDisplay - This call will start up a lwjgl instance that will display
 * the rift image in a side by side fashion in the oculus rift display.
 *
 * @author kwatters
 */
public class OculusDisplay implements Runnable {

  public final static Logger log = LoggerFactory.getLogger(OculusDisplay.class);

  // operate the display on a thread so we don't block
  transient Thread displayThread = null;
  // the oculus service
  transient public OculusRift oculus;

  private int width;
  private int height;
  private float ipd;
  private float eyeHeight;
  protected Hmd hmd;
  protected HmdDesc hmdDesc;
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

  private static Program unitQuadProgram;
  private static VertexArray unitQuadVao;
  private Camera camera = new Camera();
  private boolean trackOrientation = false;

  private static final String UNIT_QUAD_VS;
  private static final String UNIT_QUAD_FS;

  static {
    try {
      UNIT_QUAD_VS = Resources.toString(Resources.getResource("resource/oculus/unitQuad.vs"), Charsets.UTF_8);
      UNIT_QUAD_FS = Resources.toString(Resources.getResource("resource/oculus/unitQuad.fs"), Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static TexturedModel texturedModel = null;
  private static Loader loader;
  private static Renderer renderer;
  private static StaticShader shader;

  public Orientation orientationInfo;

  // the raw model for the screen in VR.
  // private RawModel model = null;
  private Entity texturedEntity = null;

  public OculusDisplay() {

  }

  private void recenterView() {

    org.saintandreas.math.Vector3f center = org.saintandreas.math.Vector3f.UNIT_Y.mult(eyeHeight);
    org.saintandreas.math.Vector3f eye = new org.saintandreas.math.Vector3f(0, eyeHeight, ipd * 10.0f);
    MatrixStack.MODELVIEW.lookat(eye, center, org.saintandreas.math.Vector3f.UNIT_Y);
    hmd.recenterPose();
  }

  public void updateOrientation(Orientation orientation) {
    //
    this.orientationInfo = orientation;
  }

  protected final void setupDisplay() {

    // our size. ??
    width = hmdDesc.Resolution.w / 4;
    height = hmdDesc.Resolution.h / 4;
    int left = 100;
    int right = 100;

    try {
      Display.setDisplayMode(new DisplayMode(width, height));
    } catch (LWJGLException e) {
      throw new RuntimeException(e);
    }
    Display.setTitle("Oculus Rift Mirror");
    Display.setLocation(left, right);
    Display.setVSyncEnabled(true);

    onResize(width, height);

    log.info("Setup Oculus Diplsay with resolution {} x {}", width, height);

  }

  protected void onResize(int width, int height) {
    this.width = width;
    this.height = height;
    this.aspect = (float) width / (float) height;
  }

  private void internalInit() {
    // constructor
    // start up hmd libs
    if (hmd == null) {
      Hmd.initialize();
      try {
        Thread.sleep(400);
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
      // create it  (this should be owned by the Oculus service i think? and passed in with setHmd(hmd)
      hmd = Hmd.create();
    }
    if (null == hmd) {
      throw new IllegalStateException("Unable to initialize HMD");
    }

    // grab the description of the device.
    hmdDesc = hmd.getDesc();
    hmd.configureTracking();
    for (int eye = 0; eye < 2; ++eye) {
      fovPorts[eye] = hmdDesc.DefaultEyeFov[eye];
      OvrMatrix4f m = Hmd.getPerspectiveProjection(fovPorts[eye], 0.1f, 1000000f, ovrProjection_RightHanded | ovrProjection_ClipRangeOpenGL);
      projections[eye] = toMatrix4f(m);
      textureSizes[eye] = hmd.getFovTextureSize(eye, fovPorts[eye], 1.0f);
    }

    ipd = hmd.getFloat(OvrLibrary.OVR_KEY_IPD, OVR_DEFAULT_IPD);
    eyeHeight = hmd.getFloat(OvrLibrary.OVR_KEY_EYE_HEIGHT, OVR_DEFAULT_EYE_HEIGHT);
    // TODO: do i need to center this?

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

    loader = new Loader();
    shader = new StaticShader();
    renderer = new Renderer(shader);

    recenterView();
  }

  public void run() {
    internalInit();
    // Load the screen in the scene i guess first.
    initScene();
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

  private void initScene() {

    float size = 1.0f;
    float depth = -1.0f;
    float[] verticies = {
        // left bottom triangle
        -size, size, depth, // V0
        -size, -size, depth, // V1
        size, -size, depth, // V2
        size, size, depth, // V3
    };
    int[] indicies = { 0, 1, 3, 3, 1, 2 };

    // TODO: calculate the texture scaling (probably depends on the power of 2
    // size thing for a texture.)
    // What's the power of 2 here?
    float xMax = 1.0f;
    float yMax = 1.0f;
    // float yMax = 0.63f;
    // something like this?
    // float xMax = img.getWidth()/getWidth();
    // float yMax = img.getHeight()/getHeight();
    float[] textureCoords = { 0, 0, // V0
        0, yMax, // V1
        xMax, yMax, // V2
        xMax, 0 // V3
    };
    // TODO: maybe I shouldn't do this each time ?
    RawModel model = loader.loadToVAO(verticies, textureCoords, indicies);
    // TODO: load the image first.
    ModelTexture texture = new ModelTexture(loader.loadTexture("OculusRift"));
    // the textured model (of the screen) to render.
    texturedModel = new TexturedModel(model, texture);
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

    log.info("Rendering frame {}", frameCount);

    // here it is folks!
    Posef eyePoses[] = hmd.getEyePoses(frameCount, eyeOffsets);
    frameBuffer.activate();
    MatrixStack pr = MatrixStack.PROJECTION;
    MatrixStack mv = MatrixStack.MODELVIEW;
    GLTexture texture = swapTexture.getTexture(swapTexture.CurrentIndex);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture.ogl.TexId, 0);

    // GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.ogl.TexId);
    // render each eye in here!
    // GL11.glEnable(GL11.GL_SCISSOR_TEST);
    // // left eye
    // glScissor(0,0, width/2, height);
    // glClearColor(1,0,0,1);
    // GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    // // right eye
    // glScissor(width/2,0, width/2, height);
    // glClearColor(0,0,1,1);
    // GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    // GL11.glDisable(GL11.GL_SCISSOR_TEST);

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

      glClearColor(0, 0, 1, 1);

      // ok use the matrix view and pretranslate/stuff?
      mv.push().preTranslate(RiftUtils.toVector3f(poses[eye].Position).mult(-1)).preRotate(RiftUtils.toQuaternion(poses[eye].Orientation).inverse());

      // ok.. now we have a loop 2x through that gives us our left/right images.
      if (eye == 0 && currentFrame.left != null) {
        renderScreen(currentFrame.left, orientationInfo);
      } else if (eye == 1 && currentFrame.right != null) {
        // right screen
        renderScreen(currentFrame.right, orientationInfo);
      } else {
        // TODO log something here?
      }
      mv.pop();
    }

    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture.ogl.TexId, 0);
    frameBuffer.deactivate();

    // This is the Magic Line!!! it turns the light blue! weee!
    hmd.submitFrame(frameCount, layer);

    // hmd.

    swapTexture.CurrentIndex++;
    swapTexture.CurrentIndex %= swapTexture.TextureCount;

    // FIXME Copy the layer to the main window using a mirror texture
    glScissor(0, 0, width, height);
    glViewport(0, 0, width, height);
    glClearColor(0.5f, 0.5f, System.currentTimeMillis() % 1000 / 1000.0f, 1);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    // SceneHelpers.renderTexturedQuad(mirrorTexture.ogl.TexId);
    renderTexturedQuad(mirrorTexture.ogl.TexId);

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
    // layer.Header.Type = OvrLibrary.ovrLayerType.ovrLayerType_EyeFov;
    // Direct seems more like what I want.
    layer.Header.Type = OvrLibrary.ovrLayerType.ovrLayerType_Direct;
    layer.ColorTexure[0] = swapTexture;
    layer.Fov = fovPorts;
    layer.RenderPose = poses;
    for (int eye = 0; eye < 2; ++eye) {
      layer.Viewport[eye].Size = textureSizes[eye];
      layer.Viewport[eye].Pos = new OvrVector2i(0, 0);
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
    // while (Keyboard.next()) {
    // onKeyboardEvent();
    // }
    //
    // while (Mouse.next()) {
    // onMouseEvent();
    // }
    // TODO : nothing?

    // Here we could update our projection matrix based on HMD info

  }

  // TODO: synchronize access to the current frame?
  public synchronized RiftFrame getCurrentFrame() {
    return currentFrame;
  }

  // TODO: do i need to synchronize this?
  public synchronized void setCurrentFrame(RiftFrame currentFrame) {
    this.currentFrame = currentFrame;
  }

  // helper function from saintandreas !
  public static Matrix4f toMatrix4f(OvrMatrix4f m) {
    if (null == m) {
      return new Matrix4f();
    }
    return new Matrix4f(m.M).transpose();
  }

  public static void renderTexturedQuad(int texture) {
    if (null == unitQuadProgram) {
      unitQuadProgram = new Program(UNIT_QUAD_VS, UNIT_QUAD_FS);
      unitQuadProgram.link();
    }
    if (null == unitQuadVao) {
      unitQuadVao = new VertexArray();
    }
    unitQuadProgram.use();
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);
    unitQuadVao.bind();
    glBindTexture(GL_TEXTURE_2D, texture);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    Texture.unbind(GL_TEXTURE_2D);
    Program.clear();
    VertexArray.unbind();
  }

  /*
   * helper function to render an image on the current bound texture.
   */
  public void renderScreen(SerializableImage img, Orientation orientation) {

    if (img != null) {
      log.info("Image height {} width {}", img.getHeight(), img.getWidth());
      // clean up the texture as we're about to replace it?
      GL11.glDeleteTextures(texturedModel.getTexture().getID());
      // GL11.glDeleteTextures(1);
      // load the new image as a texture.
      ModelTexture texture = new ModelTexture(loader.loadTexture(img.getImage()));
      texturedModel.setTexture(texture);
      texturedEntity = new Entity(texturedModel, new Vector3f(0, 0, 0), 0, 0, 0, 1);

    }
    // update the camera position i guess?
    if (trackOrientation) {
      if (orientation != null) {
        float roll = orientation.getRoll().floatValue();
        float pitch = orientation.getPitch().floatValue();
        float yaw = orientation.getYaw().floatValue();
        camera.setYaw((float) Math.toRadians(yaw));
        camera.setPitch((float) Math.toRadians(pitch));
        camera.setRoll((float) Math.toRadians(roll));
        log.info("ORIENTATION:" + orientation);
      } else {
        log.info("Orientation was null.");
      }
    }

    camera.move();
    shader.start();
    shader.loadViewMatrix(camera);
    // depending on our orientation. we want to rotate/translate
    renderer.render(texturedEntity, shader);
    shader.stop();

  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public void start() {
    log.info("starting oculus display thread");
    if (displayThread != null) {
      log.info("Oculus Display thread already started.");
      return;
    }
    // TODO: what is the name
    displayThread = new Thread(this, String.format("%s_oculusDisplayThread", oculus.getName()));
    displayThread.start();
  }

  public static void main(String[] args) {
    // TODO : noop
    OculusDisplay test = new OculusDisplay();
    test.run();
  }

  public Hmd getHmd() {
    return hmd;
  }

  public void setHmd(Hmd hmd) {
    this.hmd = hmd;
  }

}

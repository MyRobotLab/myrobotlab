package org.myrobotlab.oculus;

import static com.oculusvr.capi.OvrLibrary.OVR_DEFAULT_IPD;
import static com.oculusvr.capi.OvrLibrary.ovrProjectionModifier.ovrProjection_ClipRangeOpenGL;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.saintandreas.ExampleResource.IMAGES_SKY_CITY_XNEG_PNG;
import static org.saintandreas.ExampleResource.IMAGES_SKY_CITY_XPOS_PNG;
import static org.saintandreas.ExampleResource.IMAGES_SKY_CITY_YNEG_PNG;
import static org.saintandreas.ExampleResource.IMAGES_SKY_CITY_YPOS_PNG;
import static org.saintandreas.ExampleResource.IMAGES_SKY_CITY_ZNEG_PNG;
import static org.saintandreas.ExampleResource.IMAGES_SKY_CITY_ZPOS_PNG;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.opengl.GL;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OculusRift;
import org.myrobotlab.service.OculusRift.RiftFrame;
import org.myrobotlab.service.data.Orientation;
import org.saintandreas.gl.FrameBuffer;
import org.saintandreas.gl.IndexedGeometry;
import org.saintandreas.gl.MatrixStack;
import org.saintandreas.gl.OpenGL;
import org.saintandreas.gl.buffers.VertexArray;
import org.saintandreas.gl.shaders.Program;
import org.saintandreas.gl.textures.Texture;
import org.saintandreas.math.Matrix4f;
import org.saintandreas.math.Quaternion;
import org.saintandreas.math.Vector2f;
import org.saintandreas.math.Vector3f;
import org.saintandreas.resources.Resource;
import org.slf4j.Logger;

import com.oculusvr.capi.EyeRenderDesc;
import com.oculusvr.capi.FovPort;
import com.oculusvr.capi.Hmd;
import com.oculusvr.capi.HmdDesc;
import com.oculusvr.capi.LayerEyeFov;
import com.oculusvr.capi.MirrorTexture;
import com.oculusvr.capi.MirrorTextureDesc;
import com.oculusvr.capi.OvrLibrary;
import com.oculusvr.capi.OvrMatrix4f;
import com.oculusvr.capi.OvrQuaternionf;
import com.oculusvr.capi.OvrRecti;
import com.oculusvr.capi.OvrSizei;
import com.oculusvr.capi.OvrVector2i;
import com.oculusvr.capi.OvrVector3f;
import com.oculusvr.capi.Posef;
import com.oculusvr.capi.TextureSwapChain;
import com.oculusvr.capi.TextureSwapChainDesc;
import com.oculusvr.capi.ViewScaleDesc;

/**
 * 
 * OculusDisplay - This call will start up a lwjgl instance that will display
 * the rift image in a side by side fashion in the oculus rift display. This is
 * largely adapted from jocular-examples
 * https://github.com/OculusRiftInAction/jocular-examples
 * 
 * @author kwatters
 */
public class OculusDisplay implements Runnable {

  public final static Logger log = LoggerFactory.getLogger(OculusDisplay.class);
  // handle to the glfw window
  private long window = 0;
  // lwjgl3 callback
  private GLFWErrorCallback errorCallback;
  // A reference to the framebuffer size callback.
  private GLFWFramebufferSizeCallback framebufferSizeCallback;
  // operate the display on a thread so we don't block
  transient Thread displayThread = null;
  // the oculus service
  transient public OculusRift oculus;
  private int width;
  private int height;
  private float ipd;
  private float eyeHeight;
  transient protected Hmd hmd;
  transient protected HmdDesc hmdDesc;
  private final FovPort[] fovPorts = FovPort.buildPair();
  protected final Posef[] poses = Posef.buildPair();
  private final Matrix4f[] projections = new Matrix4f[2];
  private final OvrVector3f[] eyeOffsets = OvrVector3f.buildPair();
  private final OvrSizei[] textureSizes = new OvrSizei[2];
  private final ViewScaleDesc viewScaleDesc = new ViewScaleDesc();
  private FrameBuffer frameBuffer = null;
  // keep track of how many frames we have submitted to the display.
  private int frameCount = -1;
  private TextureSwapChain swapChain = null;
  // a texture to mirror what is displayed on the rift.
  private MirrorTexture mirrorTexture = null;
  // this is what gets submitted to the rift for display.
  private LayerEyeFov layer = new LayerEyeFov();
  // The last image captured from the OpenCV services (left&right)
  private RiftFrame currentFrame;

  private static Program unitQuadProgram;
  private static VertexArray unitQuadVao;

  private static final String UNIT_QUAD_VS;
  private static final String UNIT_QUAD_FS;
  private static final String SHADERS_TEXTURED_VS;
  private static final String SHADERS_TEXTURED_FS;
  private static final String SHADERS_CUBEMAP_VS;
  private static final String SHADERS_CUBEMAP_FS;

  private static IndexedGeometry screenGeometry;
  private static Program screenProgram;

  private static IndexedGeometry cubeGeometry;
  private static Program skyboxProgram;
  private static Texture skyboxTexture;

  private volatile boolean newFrame = true;
  private float screenSize = 1.0f;

  public volatile boolean trackHead = true;

  static {
    UNIT_QUAD_VS = FileIO.resourceToString("OculusRift" + File.separator + "unitQuad.vs");
    UNIT_QUAD_FS = FileIO.resourceToString("OculusRift" + File.separator + "unitQuad.fs");
    SHADERS_TEXTURED_FS = FileIO.resourceToString("OculusRift" + File.separator + "Textured.fs");
    SHADERS_TEXTURED_VS = FileIO.resourceToString("OculusRift" + File.separator + "Textured.vs");
    SHADERS_CUBEMAP_VS = FileIO.resourceToString("OculusRift" + File.separator + "CubeMap.vs");
    SHADERS_CUBEMAP_FS = FileIO.resourceToString("OculusRift" + File.separator + "CubeMap.fs");
  }

  private static final Resource SKYBOX[] = { IMAGES_SKY_CITY_XPOS_PNG, IMAGES_SKY_CITY_XNEG_PNG, IMAGES_SKY_CITY_YPOS_PNG, IMAGES_SKY_CITY_YNEG_PNG, IMAGES_SKY_CITY_ZPOS_PNG,
      IMAGES_SKY_CITY_ZNEG_PNG, };

  public Orientation orientationInfo;

  // textures for the screen one for the left eye, one for the right eye.
  private Texture leftTexture;
  private Texture rightTexture;

  public OculusDisplay() {
  }

  private void recenterView() {
    org.saintandreas.math.Vector3f center = org.saintandreas.math.Vector3f.UNIT_Y.mult(eyeHeight);
    org.saintandreas.math.Vector3f eye = new org.saintandreas.math.Vector3f(0, eyeHeight, ipd * 10.0f);
    MatrixStack.MODELVIEW.lookat(eye, center, org.saintandreas.math.Vector3f.UNIT_Y);
    hmd.recenterPose();
  }

  public void updateOrientation(Orientation orientation) {
    // TODO: we can probably remove this , the orientation info is known when
    // we're rendering.
    this.orientationInfo = orientation;
  }

  // sets up the opengl display for rendering the mirror texture.
  protected final long setupDisplay() {
    // our size. / resolution? is this configurable? maybe not?
    width = hmdDesc.Resolution.w / 4;
    height = hmdDesc.Resolution.h / 4;
    // TODO: these were to specify where the glfw window would be placed on the
    // monitor..
    // int left = 100;
    // int right = 100;
    // try {
    // Display.setDisplayMode(new DisplayMode(width, height));
    // } catch (LWJGLException e) {
    // throw new RuntimeException(e);
    // }
    // Display.setTitle("MRL Oculus Rift Viewer");
    // TODO: which one??
    long monitor = 0;
    long window = glfwCreateWindow(width, height, "MRL Oculus Rift Viewer", monitor, 0);
    if (window == 0) {
      throw new RuntimeException("Failed to create window");
    }
    // Make this window's context the current on this thread.
    glfwMakeContextCurrent(window);
    // Let LWJGL know to use this current context.
    GL.createCapabilities();
    // Setup the framebuffer resize callback.
//    glfwSetFramebufferSizeCallback(window, (framebufferSizeCallback = new GLFWFramebufferSizeCallback() {
//      @Override
//      public void invoke(long window, int width, int height) {
//        onResize(width, height);
//      }
//    }));
    // TODO: set location and vsync?! Do we need to update these for lwjgl3?
    // Display.setLocation(left, right);
    // TODO: vsync enabled?
    // Display.setVSyncEnabled(true);
    onResize(width, height);
    log.info("Setup Oculus Diplsay with resolution " + width + "x" + height);
    return window;
  }

  // if the window is resized.
  protected void onResize(int width, int height) {
    this.width = width;
    this.height = height;
  }

  // initialize the oculus hmd
  private void internalInit() {
    // start up hmd libs
    initHmd();
    // initialize the opengl rendering context
    initGl();
    // look ahead
    recenterView();
  }

  // oculus device initialization, assumes that oculus runtime is up and running
  // on the server.
  private void initHmd() {
    if (hmd == null) {
      Hmd.initialize();
      try {
        Thread.sleep(400);
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
      // create it (this should be owned by the Oculus service i think? and
      // passed in with setHmd(hmd)
      hmd = Hmd.create();
    }
    if (null == hmd) {
      throw new IllegalStateException("Unable to initialize HMD");
    }
    // grab the description of the device.
    hmdDesc = hmd.getDesc();
    // hmd.recenterPose();
    for (int eye = 0; eye < 2; ++eye) {
      fovPorts[eye] = hmdDesc.DefaultEyeFov[eye];
      OvrMatrix4f m = Hmd.getPerspectiveProjection(fovPorts[eye], 0.1f, 1000000f, ovrProjection_ClipRangeOpenGL);
      projections[eye] = toMatrix4f(m);
      textureSizes[eye] = hmd.getFovTextureSize(eye, fovPorts[eye], 1.0f);
    }
    // TODO: maybe ipd and eyeHeight go away?
    ipd = hmd.getFloat(OvrLibrary.OVR_KEY_IPD, OVR_DEFAULT_IPD);
    // eyeHeight = hmd.getFloat(OvrLibrary.OVR_KEY_EYE_HEIGHT,
    // OVR_DEFAULT_EYE_HEIGHT);
    eyeHeight = 0;
  }

  // Main rendering loop for running the oculus display.
  @Override
  public void run() {
    internalInit();
    // Load the screen in the scene i guess first.
    while (!glfwWindowShouldClose(window)) {
      // while (!Display.isCloseRequested()) {
      // TODO: resize testing.. make sure it's handle via the other callback? or
      // something
      // if (Display.wasResized()) {
      // onResize(Display.getWidth(), Display.getHeight());
      // }
      update();
      drawFrame();
      finishFrame();
    }
    onDestroy();
    // Display.destroy();
    glfwDestroyWindow(window);
  }

  public final void drawFrame() {
    // System.out.println("Draw Frame called.");
    if (currentFrame == null) {
      return;
    }
    if (currentFrame.left == null || currentFrame.right == null) {
      return;
    }
    // load new textures if a new rift frame has arrived.
    if (newFrame) {
      loadRiftFrameTextures();
      newFrame = false;
    }

    width = hmdDesc.Resolution.w / 4;
    height = hmdDesc.Resolution.h / 4;

    ++frameCount;
    Posef eyePoses[] = hmd.getEyePoses(frameCount, eyeOffsets);
    frameBuffer.activate();

    MatrixStack pr = MatrixStack.PROJECTION;
    MatrixStack mv = MatrixStack.MODELVIEW;
    int textureId = swapChain.getTextureId(swapChain.getCurrentIndex());
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);
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
      if (trackHead)
        mv.push().preTranslate(toVector3f(poses[eye].Position).mult(-1)).preRotate(toQuaternion(poses[eye].Orientation).inverse());
      // TODO: is there a way to render both of these are the same time?
      if (eye == 0 && currentFrame.left != null) {
        renderScreen(leftTexture, orientationInfo);
      } else if (eye == 1 && currentFrame.right != null) {
        renderScreen(rightTexture, orientationInfo);
      }
      if (trackHead) {
        mv.pop();
      }
    }
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, 0, 0);
    frameBuffer.deactivate();
    swapChain.commit();
    hmd.submitFrame(frameCount, layer);
    // FIXME Copy the layer to the main window using a mirror texture
    glScissor(0, 0, width, height);
    glViewport(0, 0, width, height);
    glClearColor(0.5f, 0.5f, System.currentTimeMillis() % 1000 / 1000.0f, 1);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    // render the quad with our images/textures on it, one for the left eye, one
    // for the right eye.
    renderTexturedQuad(mirrorTexture.getTextureId());
  }

  private void loadRiftFrameTextures() {

    // if the left & right texture are already loaded, let's delete them
    if (leftTexture != null)
      glDeleteTextures(leftTexture.id);
    if (rightTexture != null)
      glDeleteTextures(rightTexture.id);

    // here we can just update the textures that we're using
    leftTexture = Texture.loadImage(currentFrame.left.getImage());
    rightTexture = Texture.loadImage(currentFrame.right.getImage());

    leftTexture.bind();
    leftTexture.parameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    leftTexture.parameter(GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
    glGenerateMipmap(GL_TEXTURE_2D);
    leftTexture.unbind();

    rightTexture.bind();
    rightTexture.parameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    rightTexture.parameter(GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
    glGenerateMipmap(GL_TEXTURE_2D);
    rightTexture.unbind();

  }

  protected void finishFrame() {
    // Display update combines both input processing and
    // buffer swapping. We want only the input processing
    // so we have to call processMessages.
    // Display.processMessages();
    // Display.update();
    glfwPollEvents();
    glfwSwapBuffers(window);
  }

  protected void initGl() {
    // Upgrade via the documentation here:
    // https://github.com/LWJGL/lwjgl3-wiki/wiki/2.6.6-LWJGL3-migration

    // ContextAttribs contextAttributes;
    // PixelFormat pixelFormat = new PixelFormat();
    // GLContext glContext = new GLContext();

    // Initialize GLFW.
    glfwInit();
    // Setup an error callback to print GLFW errors to the console.
    // glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));

    // contextAttributes = new ContextAttribs(4,
    // 1).withProfileCore(true).withDebug(true);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
    // TODO: what about withDebug?
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

    // try {
    window = setupDisplay();
    // presumably this is called in setupDisplay now?
    // Display.create(pixelFormat, contextAttributes);

    // This supresses a strange error where when using
    // the Oculus Rift in direct mode on Windows,
    // there is an OpenGL GL_INVALID_FRAMEBUFFER_OPERATION
    // error present immediately after the context has been created.
    @SuppressWarnings("unused")
    int err = glGetError();
    // GLContext.useContext(glContext, false);
    // TODO: maybe get rid of these?
    // Mouse.create();
    // Keyboard.create();
    // } catch (LWJGLException e) {
    // throw new RuntimeException(e);
    // }
    // TODO: vSyncEnabled in lwjgl3
    // Display.setVSyncEnabled(false);

    TextureSwapChainDesc desc = new TextureSwapChainDesc();
    desc.Type = OvrLibrary.ovrTextureType.ovrTexture_2D;
    desc.ArraySize = 1;
    desc.Width = textureSizes[0].w + textureSizes[1].w;
    desc.Height = textureSizes[0].h;
    desc.MipLevels = 1;
    desc.Format = OvrLibrary.ovrTextureFormat.OVR_FORMAT_R8G8B8A8_UNORM_SRGB;
    desc.SampleCount = 1;
    desc.StaticImage = false;
    swapChain = hmd.createSwapTextureChain(desc);
    MirrorTextureDesc mirrorDesc = new MirrorTextureDesc();
    mirrorDesc.Format = OvrLibrary.ovrTextureFormat.OVR_FORMAT_R8G8B8A8_UNORM;
    mirrorDesc.Width = width;
    mirrorDesc.Height = height;
    mirrorTexture = hmd.createMirrorTexture(mirrorDesc);

    layer.Header.Type = OvrLibrary.ovrLayerType.ovrLayerType_EyeFov;
    layer.ColorTexure[0] = swapChain;
    layer.Fov = fovPorts;
    layer.RenderPose = poses;
    for (int eye = 0; eye < 2; ++eye) {
      layer.Viewport[eye].Size = textureSizes[eye];
      layer.Viewport[eye].Pos = new OvrVector2i(0, 0);
    }
    layer.Viewport[1].Pos.x = layer.Viewport[1].Size.w;
    frameBuffer = new FrameBuffer(desc.Width, desc.Height);

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
    newFrame = true;
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

  public static void renderSkybox() {
    if (null == cubeGeometry) {
      cubeGeometry = OpenGL.makeColorCube();
    }
    if (null == skyboxProgram) {
      skyboxProgram = new Program(SHADERS_CUBEMAP_VS, SHADERS_CUBEMAP_FS);
      skyboxProgram.link();
    }
    if (null == skyboxTexture) {
      skyboxTexture = OpenGL.getCubemapTextures(SKYBOX);
    }
    MatrixStack mv = MatrixStack.MODELVIEW;
    cubeGeometry.bindVertexArray();
    mv.push();
    Quaternion q = mv.getRotation();
    mv.identity().rotate(q);
    skyboxProgram.use();
    OpenGL.bindAll(skyboxProgram);
    glCullFace(GL_FRONT);
    skyboxTexture.bind();
    glDisable(GL_DEPTH_TEST);
    cubeGeometry.draw();
    glEnable(GL_DEPTH_TEST);
    skyboxTexture.unbind();
    glCullFace(GL_BACK);
    mv.pop();
  }

  /*
   * helper function to render an image on the current bound texture.
   */
  public void renderScreen(Texture screenTexture, Orientation orientation) {
    // clean up
    glClear(GL_DEPTH_BUFFER_BIT);
    renderSkybox();
    // TODO: don't lazy create this.
    if (null == screenGeometry) {
      screenGeometry = OpenGL.makeTexturedQuad(new Vector2f(-screenSize, -screenSize), new Vector2f(screenSize, screenSize));
    }
    if (null == screenProgram) {
      screenProgram = new Program(SHADERS_TEXTURED_VS, SHADERS_TEXTURED_FS);
      screenProgram.link();
    }
    screenProgram.use();
    OpenGL.bindAll(screenProgram);
    screenTexture.bind();
    screenGeometry.bindVertexArray();
    screenGeometry.draw();
    Texture.unbind(GL_TEXTURE_2D);
    Program.clear();
    VertexArray.unbind();
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public void start() {
    log.info("Starting oculus display thread.");
    if (displayThread != null) {
      log.info("Oculus Display thread already started.");
      return;
    }
    // create a thread to run the main render loop
    displayThread = new Thread(this, String.format("%s_oculusDisplayThread", "oculus"));
    displayThread.start();
  }

  public Hmd getHmd() {
    return hmd;
  }

  public void setHmd(Hmd hmd) {
    this.hmd = hmd;
  }

  // The following methods are taken from the joculur-examples
  public static Vector3f toVector3f(OvrVector3f v) {
    return new Vector3f(v.x, v.y, v.z);
  }

  public static Quaternion toQuaternion(OvrQuaternionf q) {
    return new Quaternion(q.x, q.y, q.z, q.w);
  }

  public static Matrix4f toMatrix4f(Posef p) {
    return new Matrix4f().rotate(toQuaternion(p.Orientation)).mult(new Matrix4f().translate(toVector3f(p.Position)));
  }

  public static Matrix4f toMatrix4f(OvrMatrix4f m) {
    if (null == m) {
      return new Matrix4f();
    }
    return new Matrix4f(m.M).transpose();
  }
  // End methods from saintandreas

  public static void main(String[] args) throws IOException {
    System.out.println("Hello world.");
    OculusDisplay display = new OculusDisplay();
    display.start();
    RiftFrame frame = new RiftFrame();
    File imageFile = new File("src/main/resources/resource/mrl_logo.jpg");
    BufferedImage lbi = ImageIO.read(imageFile);
    BufferedImage rbi = ImageIO.read(imageFile);
    SerializableImage lsi = new SerializableImage(lbi, "left");
    SerializableImage rsi = new SerializableImage(rbi, "right");
    frame.left = lsi;
    frame.right = rsi;
    display.start();
    display.setCurrentFrame(frame);
  }

}

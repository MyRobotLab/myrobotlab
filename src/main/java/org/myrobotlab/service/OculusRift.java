package org.myrobotlab.service;

import static com.oculusvr.capi.OvrLibrary.OVR_DEFAULT_IPD;
import static com.oculusvr.capi.OvrLibrary.ovrProjectionModifier.ovrProjection_ClipRangeOpenGL;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.headtracking.OculusTracking;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.oculus.OculusDisplay;
import org.myrobotlab.service.data.Orientation;
import org.myrobotlab.service.interfaces.PointPublisher;
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
import com.oculusvr.capi.TrackingState;
import com.oculusvr.capi.ViewScaleDesc;

/**
 * The OculusRift service for MyRobotLab.
 * 
 * Currently this service only exposed the head tracking information from the
 * rift. The Yaw, Pitch and Roll are exposed. Yaw - twist around vertical axis
 * (look left/right) Pitch - twist around horizontal axis (look up/down) Roll -
 * twist around axis in front of you (tilt head left/right)
 * 
 * Coming soon, lots of great stuff...
 * 
 * @author kwatters
 *
 */
// TODO: implement publishOculusRiftData ...
public class OculusRift extends Service implements PointPublisher {

  public static final int ABS_TIME_MS = 0;
  public static final boolean LATENCY_MARKER = false;

  public static final String RIGHT_OPEN_CV = "rightOpenCV";
  public static final String LEFT_OPEN_CV = "leftOpenCV";
  private static final long serialVersionUID = 1L;
  private static final float RAD_TO_DEGREES = 57.2957795F;
  public final static Logger log = LoggerFactory.getLogger(OculusRift.class);

  // the Rift stuff.
  transient protected Hmd hmd;

  protected Map<String, Object> hmdProps = new HashMap<>();

  @Deprecated /* not needed send left and right images or mono and mirror it */
  transient private RiftFrame lastRiftFrame = new RiftFrame();

  @Deprecated /* use pub/sub */
  transient private OculusDisplay display;

  private HmdDesc hmdDesc;

  transient public OculusTracking headTracker = null;
  
 
  // for single camera support, mirror the images
  private boolean mirrorImage = false;
  protected String rightImagePublisher;
  protected String leftImagePublisher;

  ////////////////////////// begin display decompose /////////////////////////////////////////
  /**
   * handle to the glfw window - for a regular display window monitor
   */
  private long window = 0;  
  // lwjgl3 callback
  transient private GLFWErrorCallback errorCallback;
  transient private GLFWFramebufferSizeCallback framebufferSizeCallback;

  Long currentThreadId = null;
  
  transient private final ViewScaleDesc viewScaleDesc = new ViewScaleDesc();
  transient private FrameBuffer frameBuffer = null;

  /*
  // oculus dimensions
  private int width = 1080 * 2;
  private int height = 1200;
  
  // monitor window on primary display
  protected int monitorWidth = width / 4;
  protected int monitorHeight = height / 4;
  */
  
  int width;
  int height;

  transient private final FovPort[] fovPorts = FovPort.buildPair();
  transient protected final Posef[] poses = Posef.buildPair();
  transient private final Matrix4f[] projections = new Matrix4f[2];
  transient private final OvrVector3f[] eyeOffsets = OvrVector3f.buildPair();
  transient private final OvrSizei[] textureSizes = new OvrSizei[2];

  // keep track of how many frames we have submitted to the display.
  private int frameCount = -1;
  transient private TextureSwapChain swapChain = null;
  // a texture to mirror what is displayed on the rift.
  transient private MirrorTexture mirrorTexture = null;
  // this is what gets submitted to the rift for display.
  transient private LayerEyeFov layer = new LayerEyeFov();
  // The last image captured from the OpenCV services (left&right)

  transient private static Program unitQuadProgram;
  transient private static VertexArray unitQuadVao;
  
  public volatile boolean trackHead = true;

  
  transient private Texture texture;
  transient public Orientation orientationInfo;
  
  transient private static IndexedGeometry screenGeometry;
  transient private static IndexedGeometry screenGeometry2;
  transient private static Program screenProgram;

  transient private static IndexedGeometry cubeGeometry;
  transient private static Program skyboxProgram;
  transient private static Texture skyboxTexture;

  // FIXME - not interested in doing this form of resource management
  // FIXME - make it filebased...
  private static final String SHADERS_TEXTURED_VS;
  private static final String SHADERS_TEXTURED_FS;
  private static final String SHADERS_CUBEMAP_VS;
  private static final String SHADERS_CUBEMAP_FS;
  private static final String UNIT_QUAD_VS;
  private static final String UNIT_QUAD_FS;

  // panel size
  private float size = 1.0f;
  //private float panelWidth = 2.0f;
  
  class Panel {
    public float panelHeight = 2.0f;
    public float panelWidth = 2.0f;
    public IndexedGeometry screenGeometry;
    // could be static
    public Program screenProgram;
    public String name;
    public String type;
  }
  
  
  transient Map<String, Panel> panels = new HashMap<>();


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


  ////////////////////////// end display decompose /////////////////////////////////////////

  
  public static class RiftFrame {
    public SerializableImage left;
    public SerializableImage right;
  }

  public OculusRift(String n, String id) {
    super(n, id);
    ready = false;
  }

  public void onLeftImage(SerializableImage image) {
    if (display != null) {
      // display.drawImage(image);
     
    }
    drawImage(image);
  }
  

  public void onRightImage(SerializableImage image) {
    if (display != null) {
      display.drawImage(image);
    }
  }

  public void onImage(SerializableImage image) {
    if (display != null) {
      display.drawImage(image);
    }
  }

  // FIXME - implement onDisplay "full image" left & right fused together
  public void onDisplay(SerializableImage frame) {

    // if we're only one camera
    // the left frame is both frames.
    if (mirrorImage) {
      // if we're mirroring the left camera
      // log.info("Oculus Frame Source {}",frame.getSource());
      if ("leftAffine".equals(frame.getSource())) {
        lastRiftFrame.left = frame;
        lastRiftFrame.right = frame;
      }
    } else {
      if ("left".equals(frame.getSource())) {
        lastRiftFrame.left = frame;
      } else if ("leftAffine".equals(frame.getSource())) {
        lastRiftFrame.left = frame;
      } else if ("right".equals(frame.getSource())) {
        lastRiftFrame.right = frame;
      } else if ("rightAffine".equals(frame.getSource())) {
        lastRiftFrame.right = frame;
      } else {
        log.error("unknown source {}", frame.getSource());
      }
    }

    // update the oculus display with the last rift frame
    if (display != null) {
      // display.setCurrentFrame(lastRiftFrame);
    } else {
      // TODO: wait on the display to be initialized ?
      // maybe just log something?
      // log.warn("The Oculus Display was null.");
    }
    invoke("publishRiftFrame", lastRiftFrame);
  }

  @Override
  public void stopService() {
    super.stopService();
    // TODO: validate proper life cycle.
    if (headTracker != null) {
      headTracker.stop();
    }
    if (hmd != null) {
      hmd.destroy();
      Hmd.shutdown();
    }
  }

  /**
   * Re-centers orientation of the head tracking Makes the current orientation
   * the straight ahead orientation. Use this to align your perspective.
   */
  public void recenterPose() {
    if (hmd != null) {
      hmd.recenterPose();
    }
  }

  /**
   * Log the head tracking info to help with debugging.
   */
  public void logOrientation() {
    TrackingState trackingState = hmd.getTrackingState(ABS_TIME_MS, LATENCY_MARKER);
    OvrVector3f position = trackingState.HeadPose.Pose.Position;
    position.x *= 100.0f;
    position.y *= 100.0f;
    position.z *= 100.0f;
    log.info((int) position.x + ", " + (int) position.y + " " + (int) position.z);

    // TODO: see if we care about this value ?
    // float w = trackingState.HeadPose.Pose.Orientation.w;
    float x = trackingState.HeadPose.Pose.Orientation.x;
    float y = trackingState.HeadPose.Pose.Orientation.y;
    float z = trackingState.HeadPose.Pose.Orientation.z;

    log.info("Roll: " + z * RAD_TO_DEGREES);
    log.info("Pitch:" + x * RAD_TO_DEGREES);
    log.info("Yaw:" + y * RAD_TO_DEGREES);
  }

  public float getYaw() {
    TrackingState trackingState = hmd.getTrackingState(ABS_TIME_MS, LATENCY_MARKER);
    float y = trackingState.HeadPose.Pose.Orientation.y * RAD_TO_DEGREES;
    return y;
  }

  public float getRoll() {
    TrackingState trackingState = hmd.getTrackingState(ABS_TIME_MS, LATENCY_MARKER);
    float z = trackingState.HeadPose.Pose.Orientation.z * RAD_TO_DEGREES;
    return z;
  }

  public float getPitch() {
    TrackingState trackingState = hmd.getTrackingState(ABS_TIME_MS, LATENCY_MARKER);
    float x = trackingState.HeadPose.Pose.Orientation.x * RAD_TO_DEGREES;
    return x;
  }

  public void addRiftFrameListener(Service service) {
    addListener("publishRiftFrame", service.getName(), "onRiftFrame");
  }

  public RiftFrame publishRiftFrame(RiftFrame frame) {
    return frame;
  }

  public Orientation publishOrientation(Orientation data) {
    // grab the last published data (if we need it somewhere)
    // if (data != null) {
    // System.out.println("Oculus Data: " + data.toString());
    // }
    // TODO: make this a proper callback / subscribe..

    if (display != null) {
      display.updateOrientation(data);
    }

    // TODO selection of what format(s) to publish based on config ?

    broadcast("publishPitch", data.pitch);
    broadcast("publishYaw", data.yaw);
    broadcast("publishRoll", data.roll);

    // return the data to the mrl framework to be published.
    return data;
  }

  public Double publishYaw(Double yaw) {
    return yaw;
  }

  public Double publishPitch(Double pitch) {
    return pitch;
  }

  public Double publishRoll(Double roll) {
    return roll;
  }

  // Points from what ?
  @Override
  public List<Point> publishPoints(List<Point> points) {
    return points;
  }

  // This publishes the hand position and orientation from the oculus touch
  // controllers.
  public Point publishLeftHandPosition(Point point) {
    return point;
  }

  public Point publishRightHandPosition(Point point) {
    return point;
  }

  // this would be the correct way to attach to a ImagePublisher
  // or some service that can generate a stream of images....
  public void attachLeft(String leftImagePublisher) {
    this.leftImagePublisher = leftImagePublisher;
    subscribe(leftImagePublisher, "publishDisplay", getName(), "onLeftImage");
  }

  public void attachRight(String rightImagePublisher) {
    this.rightImagePublisher = rightImagePublisher;
    subscribe(rightImagePublisher, "publishDisplay", getName(), "onRightImage");
  }

  public static void main(String s[]) {
    try {
      LoggingFactory.init("info");

      /*
       * OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");
       * cv.setCameraIndex(1); cv.capture(); cv.addFilter("Flip");
       */

      OpenCV left = (OpenCV) Runtime.start("left", "OpenCV");
      // left.addFilter("Flip");
      OpenCV right = (OpenCV) Runtime.start("right", "OpenCV");

      boolean web = false;

      if (web) {
        WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
        webgui.autoStartBrowser(false);
        webgui.startService();
      }

      // left.capture(left.getResourcePath("stereo-1-left.jpg"));
      left.setCameraIndex(0); // 1 is usb webcam
      left.capture();
      // right.capture(right.getResourcePath("stereo-1-right.jpg"));

      OculusRift rift = (OculusRift) Runtime.start("rift", "OculusRift");

      boolean usePanTilt = false;
      if (usePanTilt) {

        Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
        arduino.connect("COM4");

        Servo x = (Servo) Runtime.start("x", "Servo");
        Servo y = (Servo) Runtime.start("y", "Servo");
        x.setPin(7);
        y.setPin(6);
        y.map(30, -30, 0, 180);
        x.map(30, -30, 0, 180);

        y.setInverted(false);
        x.setInverted(false);

        // TODO - test - should be string based - does string base work?
        arduino.attach(x);
        arduino.attach(y);

        // x.moveTo(0.0);
        x.moveTo(90.0);
        // x.moveTo(180.0);

        // y.moveTo(0.0);
        y.moveTo(90.0);
        // y.moveTo(180.0);

        // x.disable();
        // y.disable();

        x.subscribe("rift", "publishYaw", x.getName(), "moveTo");
        y.subscribe("rift", "publishPitch", y.getName(), "moveTo");

      }

      // TODO - jme could provide 2 stereoscopic camera projections
      rift.setMirrorImage(true);

      rift.attachLeft("left");
      // rift.attachRight("right");

      // rift.startTracking();
      // rift.startDisplay();
      rift.start();

      // rift.startDisplay();

      rift.logOrientation();

    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

  public void setMirrorImage(boolean b) {
    this.mirrorImage = b;
  }

  public Hmd getHmd() {
    return hmd;
  }

  // is it reentrant
  // startTracking
  // startDisplay ??? not

  public void start() {
    startTracking();
    startDisplay();
  }

  private boolean createHmd() {
    try {

      if (hmd != null) {
        log.info("hmd already created");
        return true;
      }

      log.info("creating hmd");
      // Initalize the JNA library/ head mounted device.
      Hmd.initialize();

      // delay before the create
      Service.sleep(400);

      // get description details
      hmd = Hmd.create();
      if (null == hmd) {
        throw new IllegalStateException("Unable to initialize HMD");
      }

      hmdDesc = hmd.getDesc();
      hmdProps.put("Type", hmdDesc.Type);
      hmdProps.put("VendorId", hmdDesc.VendorId);
      hmdProps.put("ProductId", hmdDesc.ProductId);
      hmdProps.put("FirmwareMajor", hmdDesc.FirmwareMajor);
      hmdProps.put("FirmwareMinor", hmdDesc.FirmwareMinor);
      hmdProps.put("AvailableHmdCaps", hmdDesc.AvailableHmdCaps);
      hmdProps.put("DefaultHmdCaps", hmdDesc.DefaultHmdCaps);
      hmdProps.put("AvailableTrackingCaps", hmdDesc.AvailableTrackingCaps);
      hmdProps.put("DisplayRefreshRate", hmdDesc.DisplayRefreshRate);
      hmdProps.put("Resolution-w", hmdDesc.Resolution.w);
      hmdProps.put("Resolution-h", hmdDesc.Resolution.h);
      hmdProps.put("DefaultTrackingCaps", hmdDesc.DefaultTrackingCaps);
    } catch (Exception e) {
      error(e.getMessage());
      log.error("createHmd threw", e);
      return false;
    }
    ready = true;
    broadcastState();
    log.info("created hmd");
    return true;
  }

  public void startTracking() {
    log.info("starting head tracking thread");

    if (!createHmd()) {
      return;
    }

    // now that we have the hmd. lets start up the polling thread.
    if (headTracker != null) {
      headTracker = new OculusTracking(this);
      headTracker.start();
    }

    hmd.recenterPose();
    log.info("started head tracking thread");
  }

  public void startDisplay() {
    log.info("starting display");

 
    
    if (!createHmd()) {
      return;
    }

//    initGl();
    // display = new OculusDisplay(this);

    log.info("started display");
  }
  
  // sets up the opengl display for rendering the mirror texture.
  protected final long setupMirroredDisplay() {
    // our size. / resolution? is this configurable? maybe not?
    width = hmdDesc.Resolution.w / 4;
    height = hmdDesc.Resolution.h / 4;
    // TODO: these were to specify where the glfw window would be placed on the monitor.. 
    // int left = 100;
    // int right = 100;
    // try {
    //   Display.setDisplayMode(new DisplayMode(width, height));
    // } catch (LWJGLException e) {
    //   throw new RuntimeException(e);
    // }
    // Display.setTitle("MRL Oculus Rift Viewer");
    // TODO: which one?? 
    long monitor = 0;
    long window = glfwCreateWindow(width, height, "MRL Oculus Rift Viewer", monitor, 0);       
    if(window == 0) {
      throw new RuntimeException("Failed to create window");
    }
    // Make this window's context the current on this thread.
    glfwMakeContextCurrent(window);
    // Let LWJGL know to use this current context.
    GL.createCapabilities();
    //Setup the framebuffer resize callback.
    glfwSetFramebufferSizeCallback(window, (framebufferSizeCallback = new GLFWFramebufferSizeCallback() {
        @Override
        public void invoke(long window, int width, int height) {
            onResize(width, height);
        }
    }));
    // TODO: set location and vsync?!  Do we need to update these for lwjgl3?
    // Display.setLocation(left, right);
    // TODO: vsync enabled?
    // Display.setVSyncEnabled(true);
    onResize(width, height);
    log.info("Setup Oculus Display with resolution " + width + "x" + height);
    return window;
  }

  // if the window is resized.
  protected void onResize(int width, int height) {
    this.width = width;
    this.height = height;
  }

  
  protected void initGl() {
    //Initialize GLFW.
    if (!glfwInit()) {
      error("could not initialize glfw");
      return;
    }
 
    //Setup an error callback to print GLFW errors to the console.
    glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));

    // contextAttributes = new ContextAttribs(4, 1).withProfileCore(true).withDebug(true);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
    // TODO: what about withDebug?
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    
   // try {
    window = setupMirroredDisplay();
    
    
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
    //} catch (LWJGLException e) {
    //  throw new RuntimeException(e);
    //}
    // TODO: vSyncEnabled in lwjgl3
    // Display.setVSyncEnabled(false);
    
    // FIXME - currently requires hmdDesc :(
    for (int eye = 0; eye < 2; ++eye) {
      fovPorts[eye] = hmdDesc.DefaultEyeFov[eye];
      OvrMatrix4f m = Hmd.getPerspectiveProjection(fovPorts[eye], 0.1f, 1000000f, ovrProjection_ClipRangeOpenGL);
      projections[eye] = toMatrix4f(m);
      textureSizes[eye] = hmd.getFovTextureSize(eye, fovPorts[eye], 1.0f);
    }
    
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
  

  // must be synchronized - only a single thread can have GL context at one time
  // and provide an update
  public synchronized void drawImage(SerializableImage si) {
    
    if (si == null) {
      log.error("image null");
      return;
    }
        
    // FIXME - only some things need to be initialized "once"
    // the GL context needs to be initialized whenever a different thread is updating
    // if thread not last thread - init context for that thread ...
    
     if (currentThreadId == null || currentThreadId != Thread.currentThread().getId()) {    
      // FIXME something need to be initalized once per thread access
      // others like GL context need to be called each time a thread switches
       
      //  ONLY  glfwMakeContextCurrent(window); NEEDED ?
       
      // internalInit(); MORE CONTEXT NEEDS TO SWITCH ???
      // glfwMakeContextCurrent(window); ????
       initGl();
      currentThreadId = Thread.currentThread().getId();
    }
     
     // check if there was a window command to shutdown
     /*
     if (!glfwWindowShouldClose(window)) {
       onDestroy();
       glfwDestroyWindow(window);
       return;
     }
     */

    // FIXME - remove right !!
    // remove the dualism - just write an image to a location .. begin    
    // if the left & right texture are already loaded, let's delete them

    if (texture != null) {
      glDeleteTextures(texture.id); // delete previous image ...
    }

    // here we can just update the textures that we're using
    texture = Texture.loadImage(si.getImage());


    texture.bind();
    texture.parameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    texture.parameter(GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
    glGenerateMipmap(GL_TEXTURE_2D);
    texture.unbind();
    
    //remove the dualism - end

    width = hmdDesc.Resolution.w / 4;
    height = hmdDesc.Resolution.h / 4;

    ++frameCount;
    Posef eyePoses[] = hmd.getEyePoses(frameCount, eyeOffsets);
    frameBuffer.activate();
    
    // render left and right pov

    MatrixStack pr = MatrixStack.PROJECTION;
    MatrixStack mv = MatrixStack.MODELVIEW;
    int textureId = swapChain.getTextureId(swapChain.getCurrentIndex());
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);
    for (int eye = 0; eye < 2; ++eye) {
      OvrRecti vp = layer.Viewport[eye];
      // scissors the current view port left and right
      glScissor(vp.Pos.x, vp.Pos.y, vp.Size.w, vp.Size.h);
      glViewport(vp.Pos.x, vp.Pos.y, vp.Size.w, vp.Size.h);
      // log.info("{} {}", vp.Pos.x, vp.Pos.y); // 0,0 then 1344, 0
      pr.set(projections[eye]);
      Posef pose = eyePoses[eye];
      // This doesn't work as it breaks the contiguous nature of the array
      // FIXME there has to be a better way to do this
      poses[eye].Orientation = pose.Orientation;
      poses[eye].Position = pose.Position;
      if (trackHead) {
        log.info("{} {} {}", poses[eye].Position.x, poses[eye].Position.y, poses[eye].Position.z);
        poses[eye].Position.z = 3;
        mv.push().preTranslate(toVector3f(poses[eye].Position).mult(-1)).preRotate(toQuaternion(poses[eye].Orientation).inverse());
        // mv.push().preTranslate(toVector3f(poses[eye].Position).mult(-1));//.preRotate(toQuaternion(poses[eye].Orientation).inverse());
        // mv.push().preTranslate(toVector3f(poses[eye].Position));
      }
      // TODO: is there a way to render both of these are the same time?     
      renderScreen(texture, orientationInfo, eye);
      
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
    
    // MAKE NOTE ! : - visually makes no difference if glClearColor and glClear are or are not called .. why?
    glClearColor(0.5f, 0.5f, System.currentTimeMillis() % 1000 / 1000.0f, 1);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    
    // MAKE NOTE ! below is for the "viewer" on the os ...- not for the oculus screen - is this required for kwatters ?
    // render the quad with our images/textures on it, one for the left eye, one for the right eye.
    renderTexturedQuad(mirrorTexture.getTextureId());
    
    glfwPollEvents();
    glfwSwapBuffers(window);
  }
  
  
  public boolean addPanel(String name, int width, int height) {
    if (!panels.containsKey(name)) {
      Panel p = new Panel();
      p.name = name;
      p.type = "simple";
      p.panelWidth = width;
      p.panelHeight = height;
      // FIXME - need position & orientation ?
      p.screenGeometry = OpenGL.makeTexturedQuad(new Vector2f(-size, -size), new Vector2f(size, size));
      p.screenProgram = new Program(SHADERS_TEXTURED_VS, SHADERS_TEXTURED_FS);
      p.screenProgram.link();
      panels.put(name, p);
      return true;
    }
    return false;
  }

  float move = 0f;
  /**
   * helper function to render an image on the current bound texture.
   * 
   * @param screenTexture
   * @param orientation
   */
  public void renderScreen(Texture screenTexture, Orientation orientation, int eye) {
    // clean up
    glClear(GL_DEPTH_BUFFER_BIT);
    renderSkybox();
    // TODO: don't lazy create this.
//    size = size + 0.01f;
   move -= 0.00f;
  //  if (null == screenGeometry) {
      screenGeometry = OpenGL.makeTexturedQuad(new Vector2f(-size + move, -size  + move), new Vector2f(size + move, size + move));
      //screenGeometry = OpenGL.makeTexturedQuad(
      // screenGeometry2 = OpenGL.makeTexturedQuad(new Vector2f(-panelWidth/2, -panelHeight/2), new Vector2f(panelWidth/2, panelHeight/2));
   // }
    
    if (null == screenProgram) {
      screenProgram = new Program(SHADERS_TEXTURED_VS, SHADERS_TEXTURED_FS);
      screenProgram.link();
    }
 
    /*
    MatrixStack mv = MatrixStack.MODELVIEW; // <- very interesting - bound to self adjusting model view?
    cubeGeometry.bindVertexArray();
    mv.push();
    //Quaternion q = mv.getRotation();
    //mv.identity().rotate(q);
    mv.push().preTranslate(toVector3f(poses[eye].Position).mult(-1)).preRotate(toQuaternion(poses[eye].Orientation).inverse());
    */
    
    screenProgram.use();
    OpenGL.bindAll(screenProgram);
    screenTexture.bind();
    screenGeometry.bindVertexArray();
    screenGeometry.draw();
    Texture.unbind(GL_TEXTURE_2D);
    Program.clear();
    VertexArray.unbind();
    screenGeometry.destroy();
    
    //mv.pop();
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
    MatrixStack mv = MatrixStack.MODELVIEW; // <- very interesting - bound to self adjusting model view?
    cubeGeometry.bindVertexArray();
    mv.push();
    Quaternion q = mv.getRotation();
    mv.identity().rotate(q);
    skyboxProgram.use();
    OpenGL.bindAll(skyboxProgram);
    glCullFace(GL_FRONT); // letting "everything" occlude background ?
    skyboxTexture.bind();
    glDisable(GL_DEPTH_TEST); // don't render with a test for occlusion
    cubeGeometry.draw();
    glEnable(GL_DEPTH_TEST);
    skyboxTexture.unbind();
    glCullFace(GL_BACK);
    mv.pop();
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
  

  public static Quaternion toQuaternion(OvrQuaternionf q) {
    return new Quaternion(q.x, q.y, q.z, q.w);
  }


  // The following methods are taken from the joculur-examples
  public static Vector3f toVector3f(OvrVector3f v) {
    return new Vector3f(v.x, v.y, v.z);
  }


  public static Matrix4f toMatrix4f(OvrMatrix4f m) {
    if (null == m) {
      return new Matrix4f();
    }
    return new Matrix4f(m.M).transpose();
  }
}

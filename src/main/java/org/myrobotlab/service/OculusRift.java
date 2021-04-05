package org.myrobotlab.service;

import static com.oculusvr.capi.OvrLibrary.ovrProjectionModifier.ovrProjection_ClipRangeOpenGL;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.opengl.GL;
import org.myrobotlab.framework.Service;
import org.myrobotlab.headtracking.OculusTracking;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Orientation;
import org.myrobotlab.service.interfaces.PointPublisher;
import org.saintandreas.gl.FrameBuffer;
import org.saintandreas.gl.IndexedGeometry;
import org.saintandreas.gl.MatrixStack;
import org.saintandreas.gl.OpenGL;
import org.saintandreas.gl.buffers.VertexArray;
import org.saintandreas.gl.shaders.Attribute;
import org.saintandreas.gl.shaders.Program;
import org.saintandreas.gl.textures.Texture;
import org.saintandreas.math.Matrix4f;
import org.saintandreas.math.Quaternion;
import org.saintandreas.math.Vector3f;
import org.saintandreas.math.Vector4f;
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
import com.oculusvr.capi.OvrLibrary.InputState;
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
// TODO: implement OrientationPublisher PositionPublisher
public class OculusRift extends Service implements PointPublisher {

  public static final int ABS_TIME_MS = 0;
  public static final boolean LATENCY_MARKER = false;

  private static final long serialVersionUID = 1L;
  private static final float RAD_TO_DEGREES = 57.2957795F;
  public final static Logger log = LoggerFactory.getLogger(OculusRift.class);

  final transient OvrLibrary.InputState.ByReference inputStateRef = new OvrLibrary.InputState.ByReference();
  
  // the Rift stuff.
  transient protected Hmd hmd;

  protected Map<String, Object> hmdProps = new HashMap<>();

  private HmdDesc hmdDesc;

  final transient public OculusTracking headTracker;

  // for single camera support, monitor the images
  private boolean useMonitor = false;

  /**
   * handle to the glfw window - for a regular display window monitor
   */
  private long monitorWindow = 0;
  // lwjgl3 callback
  transient private GLFWErrorCallback errorCallback;
  transient private GLFWFramebufferSizeCallback framebufferSizeCallback;

  Long currentThreadId = null;

  transient private final ViewScaleDesc viewScaleDesc = new ViewScaleDesc();
  transient private FrameBuffer frameBuffer = null;

  /*
   * // oculus dimensions private int width = 1080 * 2; private int height =
   * 1200;
   * 
   * // monitor window on primary display protected int monitorWidth = width /
   * 4; protected int monitorHeight = height / 4;
   */

  int width = 1080 * 2;
  int height = 1200;

  int monitorWidth = width / 4;
  int monitorHeight = height / 4;

  transient private final FovPort[] fovPorts = FovPort.buildPair();
  transient protected final Posef[] poses = Posef.buildPair();
  transient private final Matrix4f[] projections = new Matrix4f[2];
  transient private final OvrVector3f[] eyeOffsets = OvrVector3f.buildPair();
  transient private final OvrSizei[] textureSizes = new OvrSizei[2];

  // keep track of how many frames we have submitted to the display.
  private int frameCount = -1;
  transient private TextureSwapChain swapChain = null;
  // a texture to monitor what is displayed on the rift.
  transient private MirrorTexture monitorTexture = null;
  // this is what gets submitted to the rift for display.
  transient private LayerEyeFov layer = new LayerEyeFov();
  // The last image captured from the OpenCV services (left&right)

  transient private static Program unitQuadProgram;
  transient private static VertexArray unitQuadVao;

  public volatile boolean trackHead = true;

  // transient private Texture texture;
  transient public Orientation orientationInfo;

  // transient private static IndexedGeometry screenGeometryxx;
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
  private float size = 1.35f;
  // private float panelWidth = 2.0f;

  class Panel {
    public String name;

    public float x;
    public float y;
    public float z;

    public float width = 5.0f;
    public float height = 4.0f;

    transient public IndexedGeometry geometery;
    // could be static
    transient public Program program;

    transient public Texture textureLeft = null;
    transient public Texture textureRight = null;

    public String type;

    String leftImageSrc;
    String rightImageSrc;

    private SerializableImage imageLeft;
    private SerializableImage imageRight;

    boolean stereo = false;

    public long imageLeftWrittenTs;
    public long imageLeftReadTs;
    public long imageRightWrittenTs;
    public long imageRightReadTs;
    public String eyeChannel;

    // FIXME - optimize loading by checking writte/read timestamps
    // FIXME -- DELETE texture.id if updating !!!
    public Texture getLeftTexture() {
      return textureLeft;
    }

    public Texture getRightTexture() {
      return textureRight;
    }

    // FIXME - loading is one of the most intensive processes - so
    // optimize with read and write timestamps
    public void load() {

      if (geometery == null) {
        geometery = makeTexturedQuad(this);
        program = new Program(SHADERS_TEXTURED_VS, SHADERS_TEXTURED_FS);
        program.link();
      }

      // if the left & right texture are already loaded, let's delete them
      if (textureLeft != null) {
        glDeleteTextures(textureLeft.id);
      }

      if (imageLeft == null) {
        return;
      }

      textureLeft = Texture.loadImage(imageLeft.getImage());
      textureLeft.bind();
      textureLeft.parameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST);
      textureLeft.parameter(GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
      glGenerateMipmap(GL_TEXTURE_2D);
      textureLeft.unbind();

      if (stereo) {

        if (textureRight != null) {
          glDeleteTextures(textureRight.id);
        }

        textureRight = Texture.loadImage(imageRight.getImage());

        textureRight.bind();
        textureRight.parameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        textureRight.parameter(GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
        glGenerateMipmap(GL_TEXTURE_2D);
        textureRight.unbind();

      } else {
        textureRight = textureLeft;
      }
    }

    private IndexedGeometry makeTexturedQuad(Panel panel) {

      List<Vector4f> vertices = new ArrayList<>();

      // local texture coordinates
      float textMaxX = 1;
      float texMaxY = 1;
      float textMinX = 0;
      float textMinY = 0;

      float maxX = panel.x + panel.width;
      float maxY = panel.y + panel.height;

      vertices.add(new Vector4f(maxX, maxY, panel.z, 1));
      vertices.add(new Vector4f(textMaxX, texMaxY, 0, 0));
      vertices.add(new Vector4f(panel.x, maxY, panel.z, 1));
      vertices.add(new Vector4f(textMinX, texMaxY, 0, 0));

      vertices.add(new Vector4f(panel.x, panel.y, panel.z, 1));
      vertices.add(new Vector4f(textMinX, textMinY, 0, 0));
      vertices.add(new Vector4f(maxX, panel.y, panel.z, 1));
      vertices.add(new Vector4f(textMaxX, textMinY, 0, 0));
      vertices.add(new Vector4f(maxX, maxY, panel.z, 1));
      vertices.add(new Vector4f(textMaxX, texMaxY, 0, 0));
      vertices.add(new Vector4f(panel.x, maxY, panel.z, 1));
      vertices.add(new Vector4f(textMinX, texMaxY, 0, 0));

      List<Short> indices = new ArrayList<>();
      indices.add((short) 0); // LL
      indices.add((short) 1); // LR
      indices.add((short) 3); // UL
      indices.add((short) 2); // UR
      IndexedGeometry.Builder builder = new IndexedGeometry.Builder(indices, vertices);
      builder.withDrawType(GL_TRIANGLE_STRIP).withAttribute(Attribute.POSITION).withAttribute(Attribute.TEX);
      return builder.build();
    }

    public void renderLeft() {
      if (textureLeft == null) {
        return;
      }
      // glClear(GL_DEPTH_BUFFER_BIT);
      // renderSkybox();
      program.use();
      OpenGL.bindAll(program);
      textureLeft.bind();
      geometery.bindVertexArray();
      geometery.draw();
      Texture.unbind(GL_TEXTURE_2D);
      Program.clear();
      VertexArray.unbind();
      // screenGeometry.destroy();
    }

    public void renderRight() {
      if (textureRight == null) {
        return;
      }
      // glClear(GL_DEPTH_BUFFER_BIT);
      // renderSkybox();
      program.use();
      OpenGL.bindAll(program);
      textureRight.bind();
      geometery.bindVertexArray();
      geometery.draw();
      Texture.unbind(GL_TEXTURE_2D);
      Program.clear();
      VertexArray.unbind();
      // screenGeometry.destroy();
    }

    public void setImage(SerializableImage image) {
      String src = image.getSource();
      if (src.equals(leftImageSrc)) {
        imageLeft = image;
      }
      if (src.equals(rightImageSrc)) {
        imageRight = image;
      }

    }
  }

  protected Map<String, Panel> panels = new HashMap<>();
  protected Map<String, List<Panel>> imgSrcToPanel = new HashMap<>();

  public static final String EYE_LEFT = "left";
  public static final String EYE_RIGHT = "right";

  private transient DisplayWorker displayWorker;

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

  public OculusRift(String n, String id) {
    super(n, id);
    ready = false;
    headTracker = new OculusTracking(this);
  }

  /**
   * Handles all incoming images to be rendered. Will create a "default" panel
   * to display the image if one has not already been created. Matches a image
   * to a panel with the image's source to the panels name. So if a pre-existing
   * panel exists with the appropriate name the texture of the image will be
   * rendered to it. If eyeChannel is set to left or right, the panel will only
   * be rendered in that channel. If its not set, it will be rendered in both
   * channels.
   * 
   * @param image
   * @param eyeChannel
   */
  public void onImage(SerializableImage image) {
    List<Panel> panelList = null;
    if (!imgSrcToPanel.containsKey(image.getSource())) {
      // add a default mono panel if "nothing" exists
      addPanel(image.getSource(), 5, 4, image.getSource());
    }
    panelList = imgSrcToPanel.get(image.getSource());

    // update the panel references
    for (Panel panel : panelList) {
      panel.setImage(image);
    }
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

  public Orientation publishOrientation(Orientation data) {
    broadcast("publishPitch", data.pitch);
    broadcast("publishYaw", data.yaw);
    broadcast("publishRoll", data.roll);
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
  public void attach(String leftImagePublisher) {
    subscribe(leftImagePublisher, "publishDisplay", getName(), "onImage");
  }

  public static void main(String s[]) {
    try {
      LoggingFactory.init("info");

      /*
       * OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");
       * cv.setCameraIndex(1); cv.capture(); cv.addFilter("Flip");
       */

      // OpenCV left = (OpenCV) Runtime.start("left", "OpenCV");
      // left.addFilter("Flip");
      // OpenCV right = (OpenCV) Runtime.start("right", "OpenCV");

      boolean web = false;

      if (web) {
        WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
        webgui.autoStartBrowser(false);
        webgui.startService();
      }

      // left.capture(left.getResourcePath("stereo-1-left.jpg"));
      /*
       * left.setCameraIndex(0); // 1 is usb webcam left.capture();
       * left.setStreamName("left");
       */
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

      rift.addPanel("left-0", -0.5f, 0f, -1, 3, 2, "left");
      rift.addPanel("left-1", -5, 0, -1, 3, 2, "left");

      rift.attach("left");
      // rift.attachRight("right");

      // rift.startTracking();
      // rift.startDisplay();
      rift.start();

      rift.onImage(new SerializableImage("C:\\home\\grperry\\github\\mrl\\myrobotlab\\src\\main\\resources\\resource\\OpenCV\\stereo-1-left.jpg", "left"));

      // rift.startDisplay();

      // rift.logOrientation();

    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

  public void setMirrorImage(boolean b) {
    this.useMonitor = b;
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

  public class DisplayWorker extends Thread {
    protected boolean isRunning = false;

    public DisplayWorker(String name) {
      super(String.format("%s-display-worker", name));
    }

    @Override
    public void run() {
      isRunning = true;
      initGl();
      long t = 0;
      long deltams = 0;
      double fps = 0;
      try {
        while (isRunning) {
          t = System.currentTimeMillis();
          render();
          deltams = System.currentTimeMillis() - t;
          fps = 1000 / deltams;
          log.info("delta {} ms {} fps", deltams, fps);
          // sleep(10);
        }
      } catch (Exception e) {
        stopDisplay();
        error(e);
      }
    }
  }

  public synchronized void startTracking() {
    log.info("starting head tracking thread");

    if (!createHmd()) {
      return;
    }

    headTracker.start();

    hmd.recenterPose();
    log.info("started head tracking thread");
  }

  public synchronized void stopTracking() {

    headTracker.stop();

  }

  public synchronized void startDisplay() {
    log.info("starting display");

    // FIXME - should be tracking ...
    if (!createHmd()) {
      return;
    }

    if (displayWorker != null) {
      log.info("already running display");
      return;
    }

    displayWorker = new DisplayWorker(getName());
    displayWorker.start();

    log.info("started display");
  }

  public synchronized void stopDisplay() {
    log.info("stopping display");

    if (displayWorker == null) {
      log.info("already stopped display");
      return;
    }
    displayWorker.isRunning = false;
    displayWorker = null;

    log.info("started display");
  }

  // sets up the opengl display for rendering the mirror texture.
  protected long setupMonitorWindow() {

    long monitor = 0;
    long window = glfwCreateWindow(monitorWidth, monitorHeight, "MRL Oculus Rift Viewer", monitor, 0);
    if (window == 0) {
      throw new RuntimeException("Failed to create window");
    }
    // Make this window's context the current on this thread.
    glfwMakeContextCurrent(window);
    // Let LWJGL know to use this current context.
    GL.createCapabilities();
    // Setup the framebuffer resize callback.
    glfwSetFramebufferSizeCallback(window, (framebufferSizeCallback = new GLFWFramebufferSizeCallback() {
      @Override
      public void invoke(long window, int width, int height) {
        onResize(width, height); // whats the point of this ? resizeable window
                                 // can be an auto property of opengl
      }
    }));
    // TODO: set location and vsync?! Do we need to update these for lwjgl3?
    // Display.setLocation(left, right);
    // TODO: vsync enabled?
    // Display.setVSyncEnabled(true);
    onResize(monitorWidth, monitorHeight);
    log.info("Setup Oculus Display with resolution " + monitorWidth + "x" + monitorHeight);
    return window;
  }

  // if the window is resized.
  protected void onResize(int width, int height) {
    this.monitorWidth = width;
    this.monitorHeight = height;
  }

  protected void initGl() {
    // Initialize GLFW.
    if (!glfwInit()) {
      error("could not initialize glfw");
      return;
    }

    // Setup an error callback to print GLFW errors to the console.
    glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));

    // set default hints
    glfwDefaultWindowHints();

    // try {
    monitorWindow = setupMonitorWindow();

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
    mirrorDesc.Width = monitorWidth;
    mirrorDesc.Height = monitorHeight;
    monitorTexture = hmd.createMirrorTexture(mirrorDesc);

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
  public void render() {

    ++frameCount;
    Posef eyePoses[] = hmd.getEyePoses(frameCount, eyeOffsets);
    frameBuffer.activate();

    // render left and right pov

    MatrixStack pr = MatrixStack.PROJECTION;
    MatrixStack mv = MatrixStack.MODELVIEW;
    int textureId = swapChain.getTextureId(swapChain.getCurrentIndex());
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);

    // glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glClear(GL_DEPTH_BUFFER_BIT);

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
        // log.info("{} {} {}", poses[eye].Position.x, poses[eye].Position.y,
        // poses[eye].Position.z);
        poses[eye].Position.z = 3;
        mv.push().preTranslate(toVector3f(poses[eye].Position).mult(-1)).preRotate(toQuaternion(poses[eye].Orientation).inverse());
        // mv.push().preTranslate(toVector3f(poses[eye].Position).mult(-1));//.preRotate(toQuaternion(poses[eye].Orientation).inverse());
        // mv.push().preTranslate(toVector3f(poses[eye].Position));
      }
      renderSkybox();
      for (Panel panel : panels.values()) {

        panel.load();
        // TODO: is there a way to render both of these are the same time?
        if (eye == 0) {
          panel.renderLeft();
        } else {
          panel.renderRight();
        }
      } // for (Panel panel : panels.values())

      if (trackHead) {
        mv.pop();
      }
    }

    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, 0, 0);
    frameBuffer.deactivate();
    swapChain.commit();
    hmd.submitFrame(frameCount, layer);

    // FIXME Copy the layer to the main window using a mirror texture
    glScissor(0, 0, monitorWidth, monitorHeight);
    glViewport(0, 0, monitorWidth, monitorHeight);

    // MAKE NOTE ! : - visually makes no difference if glClearColor and
    // glClear
    // are or are not called .. why?
    glClearColor(0.5f, 0.5f, System.currentTimeMillis() % 1000 / 1000.0f, 1);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    // MAKE NOTE ! below is for the "viewer" on the os ...- not for the oculus
    // screen - is this required for kwatters ?
    // render the quad with our images/textures on it, one for the left eye,
    // one
    // for the right eye.
    renderTexturedQuad(monitorTexture.getTextureId());

    glfwPollEvents();
    glfwSwapBuffers(monitorWindow);

  }

  // deffault mono panel
  public boolean addPanel(String name, float width, float height, String source) {
    float x = -width / 2;
    float y = -height / 2;
    float z = -2;
    return addPanel(name, x, y, z, width, height, source, source);
  }

  public boolean addPanel(String name, float width, float height) {
    float x = -width / 2;
    float y = -height / 2;
    float z = -2;
    return addPanel(name, width, x, y, z, height, null, null);
  }

  // add mono panel
  public boolean addPanel(String name, float x, float y, float z, float width, float height, String source) {
    return addPanel(name, x, y, z, width, height, source, source);
  }

  // add stereo panel
  public boolean addPanel(String name, float x, float y, float z, float width, float height, String leftImageSrc, String rightImageSrc) {
    if (leftImageSrc == null && rightImageSrc == null) {
      leftImageSrc = rightImageSrc = name;
    }

    if (!panels.containsKey(name)) {
      Panel p = new Panel();
      // p.x
      p.leftImageSrc = leftImageSrc;
      p.rightImageSrc = rightImageSrc;
      p.name = name;
      p.type = "simple";
      p.x = x;
      p.y = y;
      p.z = z;
      p.width = width;
      p.height = height;
      // FIXME - need position & orientation ?

      // thread safe replacement of new map
      Map<String, Panel> newPanels = new HashMap<>();
      newPanels.putAll(panels);
      newPanels.put(name, p);
      panels = newPanels;
      List<Panel> pl = null;

      if (!imgSrcToPanel.containsKey(leftImageSrc)) {
        pl = new ArrayList<>();
      } else {
        pl = imgSrcToPanel.get(leftImageSrc);
      }
      pl.add(p);

      imgSrcToPanel.put(leftImageSrc, pl);

      if (!imgSrcToPanel.containsKey(rightImageSrc)) {
        pl = new ArrayList<>();
      } else {
        pl = imgSrcToPanel.get(rightImageSrc);
      }
      pl.add(p);

      imgSrcToPanel.put(rightImageSrc, pl);
      return true;
    }
    return false;
  }

  float move = 0f;

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
    MatrixStack mv = MatrixStack.MODELVIEW; // <- very interesting - bound to
                                            // self adjusting model view?
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

 

  public void getInputState() {
    int success = OvrLibrary.INSTANCE.ovr_GetInputState(hmd, 3, inputStateRef);
    log.info("input state {}", inputStateRef);
  }
}

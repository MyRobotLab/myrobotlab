/* ----------------------------------------------------------------------------
 * SimpleOpenNI
 * ----------------------------------------------------------------------------
 * Copyright (C) 2011 Max Rheiner / Interaction Design Zhdk
 *
 * This file is part of SimpleOpenNI.
 *
 * SimpleOpenNI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * SimpleOpenNI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SimpleOpenNI.  If not, see <http://www.gnu.org/licenses/>.
 * ----------------------------------------------------------------------------
 */

package SimpleOpenNI;

// FIXME - make simple direct pathing 

//import processing.core.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.openni.PImage;
import org.myrobotlab.openni.PMatrix3D;
import org.myrobotlab.openni.PVector;
import org.myrobotlab.service.OpenNi;
import org.slf4j.Logger;

//import SimpleOpenNI.ContextWrapper;

public class SimpleOpenNI extends ContextWrapper implements SimpleOpenNIConstants {
  public final static Logger log = LoggerFactory.getLogger(SimpleOpenNI.class);

  static String nativDepLibPath = "";

  static String nativLibPath = "";

  static { // load the nativ shared lib

    String sysStr = System.getProperty("os.name").toLowerCase();
    String libName = "SimpleOpenNI";
    String archStr = System.getProperty("os.arch").toLowerCase();
    // String depLib;
    try {
      // check which system + architecture
      if (sysStr.indexOf("win") >= 0) { // windows
        if (archStr.indexOf("86") >= 0) { // 32bit
          libName += "32.dll";
          nativLibPath = getLibraryPathWin();// +
          // "/SimpleOpenNI/library/";
          // recent change
          nativDepLibPath = nativLibPath;// + "win32/"; recent change
        } else if (archStr.indexOf("64") >= 0) {
          libName += "64.dll";
          // nativLibPath = getLibraryPathWin() +
          // "/SimpleOpenNI/library/";
          nativLibPath = getLibraryPathWin();
          // nativDepLibPath = nativLibPath + "win64/"; GroG ...
          // recent change
          nativDepLibPath = nativLibPath;
        }
        // load dependencies
        System.load(nativDepLibPath + "OpenNI2.dll");
        System.load(nativDepLibPath + "NiTE2.dll");
      } else if (sysStr.indexOf("nix") >= 0 || sysStr.indexOf("linux") >= 0) { // unix
        nativLibPath = "/SimpleOpenNI/library/linux";

        if (archStr.indexOf("86") >= 0) { // 32bit
          libName += "32";
        } else if (archStr.indexOf("64") >= 0) {
          libName = "lib" + libName + "64.so";
          nativLibPath = getLibraryPathLinux() + "/SimpleOpenNI/library/";
          nativDepLibPath = nativLibPath + "linux64/";
        }

        // log.info("nativDepLibPath = " + nativDepLibPath);
      } else if (sysStr.indexOf("mac") >= 0) { // mac

        libName = "lib" + libName + ".jnilib";
        // nativLibPath = getLibraryPathLinux() +
        // "/SimpleOpenNI/library/";
        nativLibPath = getLibraryPathMac();
        nativDepLibPath = nativLibPath + "osx/";

        // log.info("nativDepLibPath = " + nativDepLibPath);
      }

      // log.info("-- " + System.getProperty("user.dir"));
      System.load(nativLibPath + libName);
    } catch (UnsatisfiedLinkError e) {
      log.error("Can't load SimpleOpenNI library (" + libName + ") : " + e);
      log.error("Verify if you installed SimpleOpenNI correctly.\nhttp://code.google.com/p/simple-openni/wiki/Installation");
    } catch (Exception ex) {
      Logging.logError(ex);
    }

  }

  // /////////////////////////////////////////////////////////////////////////
  // callback vars
  protected Object _userCbObject;

  protected Object _calibrationCbObject;

  protected Object _poseCbObject;

  protected Object _handCbObject;

  protected Object _gestureCbObject;

  protected Object _sessionCbObject;

  protected Method _newUserMethod;
  protected Method _lostUserMethod;
  protected Method _outOfSceneUserMethod;
  protected Method _visibleUserMethod;
  // hands cb
  protected Method _newHandMethod;
  protected Method _trackedHandMethod;

  protected Method _lostHandMethod;
  // gesture cb
  protected Method _newGestureMethod;
  protected Method _inProgressGestureMethod;
  protected Method _abortedGestureMethod;

  protected Method _completedGestureMethod;
  // nite session cb
  protected Method _startSessionMethod;
  protected Method _endSessionMethod;

  protected Method _focusSessionMethod;
  protected String _filename;
  protected OpenNi _parent;
  protected PImage _depthImage;

  protected int[] _depthRaw;
  protected PVector[] _depthMapRealWorld;
  protected float[] _depthMapRealWorldXn;

  // protected XnPoint3DArray _depthMapRealWorldArray;
  protected PImage _rgbImage;
  protected PImage _irImage;

  protected PImage _sceneImage;
  protected int[] _sceneRaw;
  protected PImage _userImage;

  protected int[] _userRaw;

  // update flags
  protected long _depthMapTimeStamp;

  protected long _depthImageTimeStamp;

  protected long _depthRealWorldTimeStamp;
  protected long _rgbTimeStamp;

  protected long _irImageTimeStamp;
  protected long _userMapTimeStamp;

  protected long _userImageTimeStamp;
  protected float[] _tempVec = new float[3];
  static protected boolean _initFlag = false;

  public static int deviceCount() {
    start();
    return ContextWrapper.deviceCount();
  }

  public static int deviceNames(StrVector nodeNames) {
    start();
    return ContextWrapper.deviceNames(nodeNames);
  }

  public static String getLibraryPathLinux() {
    URL url = SimpleOpenNI.class.getResource("SimpleOpenNI.class");
    if (url != null) {
      // Convert URL to string, taking care of spaces represented by the
      // "%20"
      // string.
      String path = url.toString().replace("%20", " ");
      int n0 = path.indexOf('/');

      int n1 = -1;
      n1 = path.indexOf("/SimpleOpenNI/library");
      // exported apps.

      if ((-1 < n0) && (-1 < n1))
        return path.substring(n0, n1);
      else
        return "";
    } else
      return "";
  }

  public static String getLibraryPathMac() {

    File f = new File("libraries/native/");
    return f.getAbsolutePath() + "/";
  }

  public static String getLibraryPathWin() {
    /*
     * FIXME -- need a different way URL url =
     * SimpleOpenNI.class.getResource("SimpleOpenNI.class"); //log.info("url = "
     * + url); if (url != null) { // Convert URL to string, taking care of
     * spaces represented by the "%20" // string. String path =
     * url.toString().replace("%20", " "); int n0 = path.indexOf('/');
     * 
     * int n1 = -1; n1 = path.indexOf("/SimpleOpenNI/library"); // exported
     * apps.
     * 
     * // In Windows, path string starts with "jar file/C:/..." // so the
     * substring up to the first / is removed. n0++;
     * 
     * if ((-1 < n0) && (-1 < n1)) return path.substring(n0, n1); else return
     * ""; } else return "";
     */
    // FIXME - get a Platform.instance - to support 32 bit
    File f = new File("libraries/native/");
    return f.getAbsolutePath().replace("\\", "/") + "/";
  }

  public static Method getMethodRef(Object obj, String methodName, Class[] paraList) {
    Method ret = null;
    try {
      ret = obj.getClass().getMethod(methodName, paraList);
    } catch (Exception e) { // no such method, or an error.. which is fine,
      // just ignore
    }
    return ret;
  }

  public static int raySphereIntersection(PVector p, PVector dir, PVector sphereCenter, float sphereRadius, PVector hit1, PVector hit2) {
    float[] hit1Ret = new float[3];
    float[] hit2Ret = new float[3];

    int ret = raySphereIntersection(p.array(), dir.array(), sphereCenter.array(), sphereRadius, hit1Ret, hit2Ret);

    if (ret > 0) {
      hit1.set(hit1Ret);

      if (ret > 1)
        hit2.set(hit2Ret);
    }
    return ret;
  }

  public static boolean rayTriangleIntersection(PVector p, PVector dir, PVector vec0, PVector vec1, PVector vec2, PVector hit) {
    float[] hitRet = new float[3];
    if (rayTriangleIntersection(p.array(), dir.array(), vec0.array(), vec1.array(), vec2.array(), hitRet)) {
      hit.set(hitRet);
      return true;
    } else
      return false;
  }

  public static void start() {
    if (_initFlag)
      return;

    String curPath = ContextWrapper.getcwd();
    ContextWrapper.chdir(new String(nativDepLibPath));

    _initFlag = true;
    initContext();

    ContextWrapper.chdir(curPath);
  }

  /**
   * Creates the OpenNI context ands inits the modules
   * 
   * @param parent
   *          PApplet
   */
  public SimpleOpenNI(OpenNi parent) {
    initEnv(parent, RUN_MODE_SINGLE_THREADED, -1);
  }

  /**
   * Creates the OpenNI context ands inits the modules
   * 
   * @param parent
   *          OpenNI
   * @param recordPath
   *          String, path to the record file
   */
  public SimpleOpenNI(OpenNi parent, String recordPath) {
    String path = parent.dataPath(recordPath);

    String curPath = ContextWrapper.getcwd();
    ContextWrapper.chdir(new String(nativDepLibPath));

    this._parent = parent;
    parent.registerDispose(this);
    initVars();

    // setup the callbacks
    setupCallbackFunc();

    // start openni
    this.init(path, RUN_MODE_DEFAULT);

    if ((nodes() & NODE_DEPTH) > 0)
      setupDepth();
    if ((nodes() & NODE_IMAGE) > 0)
      setupRGB();
    if ((nodes() & NODE_IR) > 0)
      setupIR();

    ContextWrapper.chdir(curPath);
  }

  /**
   * Creates the OpenNI context ands inits the modules
   * 
   * @param parent
   *          OpenNI
   * @param recordPath
   *          String, path to the record file
   * @param runMode
   *          - RUN_MODE_DEFAULT, RunMode_SingleThreaded = Runs all in a single
   *          thread - RunMode_MultiThreaded = Runs the openNI/NIITE in another
   *          thread than processing
   */
  public SimpleOpenNI(OpenNi parent, String recordPath, int runMode) {
    String path = parent.dataPath(recordPath);

    String curPath = ContextWrapper.getcwd();
    ContextWrapper.chdir(new String(nativDepLibPath));

    this._parent = parent;
    parent.registerDispose(this);
    initVars();

    // setup the callbacks
    setupCallbackFunc();

    // start openni
    this.init(path, runMode);

    // start openni
    this.init(path, RUN_MODE_DEFAULT);

    if ((nodes() & NODE_DEPTH) > 0)
      setupDepth();
    if ((nodes() & NODE_IMAGE) > 0)
      setupRGB();
    if ((nodes() & NODE_IR) > 0)
      setupIR();

    ContextWrapper.chdir(curPath);
  }

  /**
   * Applies only the rotation part of 'mat' to
   * 
   * @param mat
   *          PMatrix3D
   */
  public void calcUserCoordsys(PMatrix3D mat) {
    if (hasUserCoordsys() == false)
      return;

    float[] matRet = new float[9];
    matRet[0] = mat.m00;
    matRet[1] = mat.m01;
    matRet[2] = mat.m02;

    matRet[3] = mat.m10;
    matRet[4] = mat.m11;
    matRet[5] = mat.m12;

    matRet[6] = mat.m20;
    matRet[7] = mat.m21;
    matRet[8] = mat.m22;

    calcUserCoordsys(matRet);

    mat.set(matRet[0], matRet[1], matRet[2], 0, matRet[3], matRet[4], matRet[5], 0, matRet[6], matRet[7], matRet[8], 0, 0, 0, 0, 1);
  }

  /**
   * Calculates a point in the user defined coordinate system back to the 3d
   * system of the 3d camera
   * 
   * @param point
   *          PVector
   */
  public void calcUserCoordsys(PVector point) {
    if (hasUserCoordsys() == false)
      return;

    float[] p = new float[3];
    calcUserCoordsys(p);
    point.set(p[0], p[1], p[2]);
  }

  /**
   * Calculates a point in origninal 3d camera coordinate system to the
   * coordinate system defined by the user
   * 
   * @param mat
   *          PMatrix3D
   */
  public void calcUserCoordsysBack(PMatrix3D mat) {
    if (hasUserCoordsys() == false)
      return;

    float[] matRet = new float[9];
    matRet[0] = mat.m00;
    matRet[1] = mat.m01;
    matRet[2] = mat.m02;

    matRet[3] = mat.m10;
    matRet[4] = mat.m11;
    matRet[5] = mat.m12;

    matRet[6] = mat.m20;
    matRet[7] = mat.m21;
    matRet[8] = mat.m22;

    calcUserCoordsysBack(matRet);

    mat.set(matRet[0], matRet[1], matRet[2], 0, matRet[3], matRet[4], matRet[5], 0, matRet[6], matRet[7], matRet[8], 0, 0, 0, 0, 1);
  }

  /**
   * Calculates a point in origninal 3d camera coordinate system to the
   * coordinate system defined by the user
   * 
   * @param point
   *          PVector
   */
  public void calcUserCoordsysBack(PVector point) {
    if (hasUserCoordsys() == false)
      return;

    float[] p = new float[3];
    calcUserCoordsysBack(p);
    point.set(p[0], p[1], p[2]);
  }

  public void convertProjectiveToRealWorld(PVector proj, PVector world) {
    convertProjectiveToRealWorld(proj.array(), _tempVec);
    world.set(_tempVec);
  }

  public void convertRealWorldToProjective(PVector world, PVector proj) {
    convertRealWorldToProjective(world.array(), _tempVec);
    proj.set(_tempVec);
  }

  /*
   * Enable the depthMap data collection
   * 
   * @param width
   *          int
   * @param height
   *          int
   * @param fps
   *          int
   * @return returns true if depthMap generation was succesfull
   */
  /*
   * public boolean enableDepth(int width,int height,int fps) {
   * if(super.enableDepth(width,height,fps)) { // setup the var for depth calc
   * setupDepth(); return true; } else return false; }
   */

  public PImage depthImage() {
    updateDepthImage();
    return _depthImage;
  }

  public int[] depthMap() {
    updateDepthRaw();
    return _depthRaw;
  }

  public PVector[] depthMapRealWorld() {
    updateDepthRealWorld();
    return _depthMapRealWorld;
  }

  /**
  * 
  */
  public void dispose() {
    close();
  }

  // /////////////////////////////////////////////////////////////////////////
  // helper methods
  /**
   * Helper method that draw the 3d camera and the frustum of the camera
   * 
   */
  public void drawCamFrustum() {
    /*
     * _parent.g.pushStyle(); _parent.g.pushMatrix();
     * 
     * if(hasUserCoordsys()) { // move the camera to the real nullpoint
     * PMatrix3D mat = new PMatrix3D(); getUserCoordsys(mat);
     * _parent.g.applyMatrix(mat); }
     * 
     * // draw cam case _parent.stroke(200,200,0); _parent.noFill();
     * _parent.g.beginShape(); _parent.g.vertex(270 * .5f,40 * .5f,0.0f);
     * _parent.g.vertex(-270 * .5f,40 * .5f,0.0f); _parent.g.vertex(-270 *
     * .5f,-40 * .5f,0.0f); _parent.g.vertex(270 * .5f,-40 * .5f,0.0f);
     * _parent.g.endShape(PConstants.CLOSE);
     * 
     * _parent.g.beginShape(); _parent.g.vertex(220 * .5f,40 * .5f,-50.0f);
     * _parent.g.vertex(-220 * .5f,40 * .5f,-50.0f); _parent.g.vertex(-220 *
     * .5f,-40 * .5f,-50.0f); _parent.g.vertex(220 * .5f,-40 * .5f,-50.0f);
     * _parent.g.endShape(PConstants.CLOSE);
     * 
     * _parent.g.beginShape(PConstants.LINES); _parent.g.vertex(270 * .5f,40 *
     * .5f,0.0f); _parent.g.vertex(220 * .5f,40 * .5f,-50.0f);
     * 
     * _parent.g.vertex(-270 * .5f,40 * .5f,0.0f); _parent.g.vertex(-220 *
     * .5f,40 * .5f,-50.0f);
     * 
     * _parent.g.vertex(-270 * .5f,-40 * .5f,0.0f); _parent.g.vertex(-220 *
     * .5f,-40 * .5f,-50.0f);
     * 
     * _parent.g.vertex(270 * .5f,-40 * .5f,0.0f); _parent.g.vertex(220 *
     * .5f,-40 * .5f,-50.0f); _parent.g.endShape();
     * 
     * // draw cam opening angles _parent.stroke(200,200,0,50);
     * _parent.g.line(0.0f,0.0f,0.0f, 0.0f,0.0f,1000.0f);
     * 
     * // calculate the angles of the cam, values are in radians, radius is 10m
     * float distDepth = 10000;
     * 
     * float valueH = distDepth * _parent.tan(hFieldOfView() * .5f); float
     * valueV = distDepth * _parent.tan(vFieldOfView() * .5f);
     * 
     * _parent.stroke(200,200,0,100); _parent.g.line(0.0f,0.0f,0.0f,
     * valueH,valueV,distDepth); _parent.g.line(0.0f,0.0f,0.0f,
     * -valueH,valueV,distDepth); _parent.g.line(0.0f,0.0f,0.0f,
     * valueH,-valueV,distDepth); _parent.g.line(0.0f,0.0f,0.0f,
     * -valueH,-valueV,distDepth); _parent.g.beginShape();
     * _parent.g.vertex(valueH,valueV,distDepth);
     * _parent.g.vertex(-valueH,valueV,distDepth);
     * _parent.g.vertex(-valueH,-valueV,distDepth);
     * _parent.g.vertex(valueH,-valueV,distDepth);
     * _parent.g.endShape(PConstants.CLOSE);
     * 
     * _parent.g.popMatrix(); _parent.g.popStyle();
     */
  }

  /**
   * Draws a limb from joint1 to joint2
   * 
   * @param userId
   *          int
   * @param joint1
   *          int
   * @param joint2
   *          int
   */
  public void drawLimb(int userId, int joint1, int joint2) {

    PVector joint1Pos = new PVector();
    PVector joint2Pos = new PVector();

    getJointPositionSkeleton(userId, joint1, joint1Pos);
    getJointPositionSkeleton(userId, joint2, joint2Pos);

    PVector joint1Pos2d = new PVector();
    PVector joint2Pos2d = new PVector();

    convertRealWorldToProjective(joint1Pos, joint1Pos2d);
    convertRealWorldToProjective(joint2Pos, joint2Pos2d);

    _parent.line(joint1Pos2d.x, joint1Pos2d.y, joint2Pos2d.x, joint2Pos2d.y, userId, joint1, joint2);
  }

  /**
   * Enable the depthMap data collection
   */
  @Override
  public boolean enableDepth() {
    if (super.enableDepth()) { // setup the var for depth calc
      setupDepth();
      return true;
    } else
      return false;
  }

  /**
   * Enable hands
   */
  @Override
  public boolean enableHand() {
    return enableHand(_parent);
  }

  /**
   * Enable hands
   * @param cbObject c
   * @return true/false
   */
  public boolean enableHand(Object cbObject) {
    _handCbObject = cbObject;

    if (super.enableHand()) {
      setupHandCB(_handCbObject);
      return true;
    } else
      return false;
  }

  /**
   * Enable the irMap data collection ir is only available if there is no
   * rgbImage activated at the same time
   */
  @Override
  public boolean enableIR() {
    if (super.enableIR()) { // setup the var for depth calc
      setupIR();
      return true;
    } else
      return false;
  }

  /**
   * Enable the irMap data collection ir is only available if there is no
   * irImage activated at the same time
   * 
   * @param width
   *          int
   * @param height
   *          int
   * @param fps
   *          int
   * @return returns true if irMap generation was succesfull
   */
  @Override
  public boolean enableIR(int width, int height, int fps) {
    if (super.enableIR(width, height, fps)) { // setup the var for depth
      // calc
      setupIR();
      return true;
    } else
      return false;
  }

  // private void setupScene()
  // {
  // _sceneImage = new PImage(sceneWidth(), sceneHeight(),PConstants.RGB);
  // _sceneRaw = new int[sceneWidth() * sceneHeight()];
  // }

  // /**
  // * Enable the scene data collection
  // */
  // public boolean enableScene()
  // {
  // if(super.enableScene())
  // { // setup the var for depth calc
  // setupScene();
  // return true;
  // }
  // else
  // return false;
  // }

  // /**
  // * Enable the scene data collection
  // *
  // * @param width
  // * int
  // * @param height
  // * int
  // * @param fps
  // * int
  // * @return returns true if sceneMap generation was succesfull
  // */
  // public boolean enableScene(int width,int height,int fps)
  // {
  // if(super.enableScene(width,height,fps))
  // { // setup the var for depth calc
  // setupScene();
  // return true;
  // }
  // else
  // return false;
  // }

  // public PImage sceneImage()
  // {
  // updateSceneImage();
  // return _sceneImage;
  // }

  // public int[] sceneMap()
  // {
  // updateSceneRaw();
  // return _sceneRaw;
  // }

  // public void getSceneFloor(PVector point,PVector normal)
  // {
  // XnVector3D p = new XnVector3D();
  // XnVector3D n = new XnVector3D();

  // super.getSceneFloor(p, n);
  // point.set(p.getX(),p.getY(),p.getZ());
  // normal.set(n.getX(),n.getY(),n.getZ());
  // }

  /**
   * Enable recorder
   */
  @Override
  public boolean enableRecorder(String filePath) {
    String path = _parent.dataPath(filePath);
    _parent.createPath(path);

    if (super.enableRecorder(path))
      return true;
    else
      return false;
  }

  /**
   * Enable the camera image collection
   */
  @Override
  public boolean enableRGB() {
    if (super.enableRGB()) { // setup the var for depth calc
      setupRGB();
      return true;
    } else
      return false;
  }

  /**
   * Enable the camera image collection
   * 
   * @param width
   *          int
   * @param height
   *          int
   * @param fps
   *          int
   * @return returns true if rgbMap generation was succesfull
   */
  @Override
  public boolean enableRGB(int width, int height, int fps) {
    if (super.enableRGB(width, height, fps)) { // setup the var for depth
      // calc
      setupRGB();
      return true;
    } else
      return false;
  }

  /**
   * Enable user
   */
  @Override
  public boolean enableUser() {
    return enableUser(_parent);
  }

  /**
   * Enable user
   * @param cbObject c
   * @return true/false
   */
  public boolean enableUser(Object cbObject) {
    String curPath = ContextWrapper.getcwd();
    ContextWrapper.chdir(new String(nativDepLibPath));

    boolean ret = super.enableUser();

    ContextWrapper.chdir(curPath);

    if (ret) {
      setupUserCB(cbObject);
      setupUser();
      return true;
    } else
      return false;
  }

  @Override
  public void finalize() {
    close();
  }

  public boolean getBoundingBox(int user, PVector bbMin, PVector bbMax) {
    boolean ret;
    float[] vec = new float[6];

    ret = super.getBoundingBox(user, vec);
    bbMin.set(vec[0], vec[1], vec[2]);
    bbMax.set(vec[3], vec[4], vec[5]);

    return ret;
  }

  public boolean getCoM(int user, PVector com) {
    boolean ret;
    float[] vec = new float[3];

    ret = super.getCoM(user, vec);
    com.set(vec);

    return ret;
  }

  /**
   * gets the orientation of a joint
   * 
   * @param userId
   *          int
   * @param joint
   *          int
   * @param jointOrientation
   *          PMatrix3D
   * @return The confidence of this joint float
   */
  public float getJointOrientationSkeleton(int userId, int joint, PMatrix3D jointOrientation) {
    float[] mat = new float[9];

    float ret = getJointOrientationSkeleton(userId, joint, mat);

    jointOrientation.set(mat[0], mat[1], mat[2], 0, mat[3], mat[4], mat[5], 0, mat[6], mat[7], mat[8], 0, 0, 0, 0, 1);

    return ret;
  }

  /**
   * gets the coordinates of a joint
   * 
   * @param userId
   *          int
   * @param joint
   *          int
   * @param jointPos
   *          PVector
   * @return The confidence of this joint float
   */
  public float getJointPositionSkeleton(int userId, int joint, PVector jointPos) {
    float ret = getJointPositionSkeleton(userId, joint, _tempVec);
    jointPos.set(_tempVec);
    jointPos.quality = ret;
    return ret;
  }

  protected Method getMethodRef(String methodName, Class<Object>[] paraList) {
    Method ret = null;
    try {
      ret = _parent.getClass().getMethod(methodName, paraList);
    } catch (Exception e) { // no such method, or an error.. which is fine,
      // just ignore
    }
    return ret;
  }

  public void getUserCoordsys(PMatrix3D mat) {
    if (hasUserCoordsys() == false)
      return;

    float matRet[] = new float[16];
    getUserCoordsys(matRet);

    mat.set(matRet[0], matRet[1], matRet[2], matRet[3], matRet[4], matRet[5], matRet[6], matRet[7], matRet[8], matRet[9], matRet[10], matRet[11], matRet[12], matRet[13],
        matRet[14], matRet[15]);

  }

  public void getUserCoordsysBack(PMatrix3D mat) {
    if (hasUserCoordsys() == false)
      return;

    float matRet[] = new float[16];
    getUserCoordsysBack(matRet);

    mat.set(matRet[0], matRet[1], matRet[2], matRet[3], matRet[4], matRet[5], matRet[6], matRet[7], matRet[8], matRet[9], matRet[10], matRet[11], matRet[12], matRet[13],
        matRet[14], matRet[15]);
  }

  // private void setupGesture()
  // {
  // // gesture
  // _recognizeGestureMethod =
  // getMethodRef(_gestureCbObject,"onRecognizeGesture",new Class[] {
  // String.class,PVector.class,PVector.class });
  // _progressGestureMethod =
  // getMethodRef(_gestureCbObject,"onProgressGesture",new Class[] {
  // String.class,PVector.class,float.class });
  // }

  // /**
  // * Enable hands
  // */
  // public boolean enableGesture()
  // {
  // return enableGesture(_parent);
  // }

  // /**
  // * Enable gesture
  // */
  // public boolean enableGesture(Object cbObject)
  // {
  // _gestureCbObject = cbObject;
  // if(super.enableGesture())
  // {
  // setupGesture();
  // return true;
  // }
  // else
  // return false;
  // }

  /**
   * gets the transformation matrix of a user defined coordinatesystem
   * 
   * @param xformMat
   *          PMatrix3D
   */
  public void getUserCoordsysTransMat(PMatrix3D xformMat) {
    // xformMat.identity();
    if (hasUserCoordsys() == false)
      return;

    float[] mat = new float[16];
    getUserCoordsysTransMat(mat);

    xformMat.set(mat[0], mat[1], mat[2], mat[3], mat[4], mat[5], mat[6], mat[7], mat[8], mat[9], mat[10], mat[11], mat[12], mat[13], mat[14], mat[15]);

  }

  // /**
  // * Enable the player
  // */
  // public boolean openFileRecording(String filePath)
  // {
  // String path = _parent.dataPath(filePath);

  // if(super.openFileRecording(path))
  // { // get all the nodes that are in use and init them

  // if((nodes() & NODE_DEPTH) > 0)
  // setupDepth();
  // if((nodes() & NODE_IMAGE) > 0)
  // setupRGB();
  // if((nodes() & NODE_IR) > 0)
  // setupIR();
  // if((nodes() & NODE_SCENE) > 0)
  // setupScene();
  // if((nodes() & NODE_USER) > 0)
  // setupUser();
  // if((nodes() & NODE_GESTURE) > 0)
  // setupGesture();
  // if((nodes() & NODE_HANDS) > 0)
  // setupHands();

  // return true;
  // }
  // else
  // return false;
  // }

  public int[] getUsers() {
    IntVector intVec = new IntVector();
    getUsers(intVec);

    int[] userList = new int[(int) intVec.size()];
    for (int i = 0; i < intVec.size(); i++)
      userList[i] = intVec.get(i);

    return userList;
  }

  protected void initEnv(OpenNi parent, int runMode, int deviceIndex) {
    String curPath = ContextWrapper.getcwd();
    ContextWrapper.chdir(new String(nativDepLibPath));

    this._parent = parent;
    parent.registerDispose(this);
    initVars();

    // setup the callbacks
    setupCallbackFunc();

    //
    if (deviceIndex < 0)
      this.init(runMode);
    else
      this.init(deviceIndex, runMode);

    ContextWrapper.chdir(curPath);
  }

  protected void initVars() {
    _depthMapTimeStamp = -1;
    _depthImageTimeStamp = -1;
    _depthRealWorldTimeStamp = -1;

    _rgbTimeStamp = -1;

    _irImageTimeStamp = -1;

    _userMapTimeStamp = -1;
    _userImageTimeStamp = -1;

  }

  public PImage irImage() {
    updateIrImage();
    return _irImage;
  }

  @Override
  protected void onAbortedGestureCb(int gestureId) {
    log.info("onAbortedGestureCb");
    try {
      _abortedGestureMethod.invoke(_handCbObject, new Object[] { this, (int) gestureId });
    } catch (Exception e) {
    }
  }

  @Override
  protected void onCompletedGestureCb(int gestureId, Vec3f pos) {
    try {
      _completedGestureMethod.invoke(_handCbObject, new Object[] { this, (int) gestureId, new PVector(pos.x(), pos.y(), pos.z()) });
    } catch (Exception e) {
    }
  }

  @Override
  protected void onInProgressGestureCb(int gestureId) {
    try {
      _inProgressGestureMethod.invoke(_handCbObject, new Object[] { this, (int) gestureId });
    } catch (Exception e) {
    }
  }

  @Override
  protected void onLostHandCb(int handId) {
    try {
      _lostHandMethod.invoke(_handCbObject, new Object[] { this, (int) handId });
    } catch (Exception e) {
    }
  }

  @Override
  protected void onLostUserCb(int userId) {
    try {
      _lostUserMethod.invoke(_userCbObject, new Object[] { this, (int) userId });
    } catch (Exception e) {
    }
  }

  // gesture
  @Override
  protected void onNewGestureCb(int gestureId) {
    try {
      _newGestureMethod.invoke(_handCbObject, new Object[] { this, (int) gestureId });
    } catch (Exception e) {
    }
  }

  // hand
  @Override
  protected void onNewHandCb(int handId, Vec3f pos) {
    try {
      _newHandMethod.invoke(_handCbObject, new Object[] { this, (int) handId, new PVector(pos.x(), pos.y(), pos.z()) });
    } catch (Exception e) {
    }
  }

  // user
  @Override
  protected void onNewUserCb(int userId) {
    try {
      _newUserMethod.invoke(_userCbObject, new Object[] { this, (int) userId });
    } catch (Exception e) {
    }
  }

  // /*
  // public void convertRealWorldToProjective(Vector3D worldArray, Vector3D
  // projArray)
  // {
  // }
  // */

  @Override
  protected void onOutOfSceneUserCb(int userId) {
    try {
      _outOfSceneUserMethod.invoke(_userCbObject, new Object[] { this, (int) userId });
    } catch (Exception e) {
    }
  }

  // /*
  // public void convertProjectiveToRealWorld(Vector3D projArray, Vector3D
  // worldArray)
  // {
  // }
  // */

  @Override
  protected void onTrackedHandCb(int handId, Vec3f pos) {
    try {
      _trackedHandMethod.invoke(_handCbObject, new Object[] { this, (int) handId, new PVector(pos.x(), pos.y(), pos.z()) });
    } catch (Exception e) {
    }
  }

  @Override
  protected void onVisibleUserCb(int userId) {
    try {
      _visibleUserMethod.invoke(_userCbObject, new Object[] { this, (int) userId });
    } catch (Exception e) {
    }
  }

  public PImage rgbImage() {
    updateImage();
    return _rgbImage;
  }

  protected void setupCallbackFunc() {
    _userCbObject = _parent;
    _handCbObject = _parent;
    _gestureCbObject = _parent;

    _calibrationCbObject = _parent;
    _poseCbObject = _parent;
    _sessionCbObject = _parent;

    _newUserMethod = null;
    _lostUserMethod = null;
    _outOfSceneUserMethod = null;
    _visibleUserMethod = null;

    _newHandMethod = null;
    _trackedHandMethod = null;
    _lostHandMethod = null;

    _newGestureMethod = null;
    _inProgressGestureMethod = null;
    _abortedGestureMethod = null;
    _completedGestureMethod = null;

    // user callbacks
    setupUserCB(_parent);

    // hands
    setupHandCB(_parent);

  }

  private void setupDepth() {
    _depthImage = new PImage(depthWidth(), depthHeight());
    _depthRaw = new int[depthMapSize()];
    _depthMapRealWorld = new PVector[depthMapSize()];
    _depthMapRealWorldXn = new float[depthMapSize() * 3];

    for (int i = 0; i < depthMapSize(); i++)
      _depthMapRealWorld[i] = new PVector();
  }

  private void setupHandCB(Object obj) {
    _newHandMethod = getMethodRef(obj, "onNewHand", new Class[] { SimpleOpenNI.class, int.class, PVector.class });
    _trackedHandMethod = getMethodRef(obj, "onTrackedHand", new Class[] { SimpleOpenNI.class, int.class, PVector.class });
    _lostHandMethod = getMethodRef(obj, "onLostHand", new Class[] { SimpleOpenNI.class, int.class });

    // gesture
    _newGestureMethod = getMethodRef(obj, "onNewGesture", new Class[] { SimpleOpenNI.class, int.class });
    _inProgressGestureMethod = getMethodRef(obj, "onProgressGesture", new Class[] { SimpleOpenNI.class, int.class });
    _abortedGestureMethod = getMethodRef(obj, "onAbortedGesture", new Class[] { SimpleOpenNI.class, int.class });
    _completedGestureMethod = getMethodRef(obj, "onCompletedGesture", new Class[] { SimpleOpenNI.class, int.class, PVector.class });
  }

  private void setupIR() {
    _irImage = new PImage(irWidth(), irHeight());
  }

  private void setupRGB() {
    _rgbImage = new PImage(rgbWidth(), rgbHeight());
  }

  // /////////////////////////////////////////////////////////////////////////
  // geometry helper functions

  private void setupUser() {
    _userRaw = new int[userWidth() * userHeight()];
    _userImage = new PImage(userWidth(), userHeight());
  }

  private void setupUserCB(Object obj) {
    _newUserMethod = getMethodRef(obj, "onNewUser", new Class[] { SimpleOpenNI.class, int.class });
    _lostUserMethod = getMethodRef(obj, "onLostUser", new Class[] { SimpleOpenNI.class, int.class });
    _outOfSceneUserMethod = getMethodRef(obj, "onOutOfSceneUser", new Class[] { SimpleOpenNI.class, int.class });
    _visibleUserMethod = getMethodRef(obj, "onVisibleUser", new Class[] { SimpleOpenNI.class, int.class });
  }

  // /////////////////////////////////////////////////////////////////////////
  // callbacks

  public int startTrackingHand(PVector pos) {
    return super.startTrackingHand(pos.array());
  }

  /**
   * Enable the user data collection
   */
  @Override
  public void update() {
    super.update();
  }

  protected void updateDepthImage() {
    if ((nodes() & NODE_DEPTH) == 0)
      return;
    if (_depthImageTimeStamp == updateTimeStamp())
      return;

    _depthImage.loadPixels();
    depthImage(_depthImage.pixels);
    _depthImage.updatePixels();
    _depthImageTimeStamp = updateTimeStamp();
  }

  protected void updateDepthRaw() {
    if ((nodes() & NODE_DEPTH) == 0)
      return;
    if (_depthMapTimeStamp == updateTimeStamp())
      return;

    depthMap(_depthRaw);
    _depthMapTimeStamp = updateTimeStamp();
  }

  protected void updateDepthRealWorld() {
    if ((nodes() & NODE_DEPTH) == 0)
      return;
    if (_depthRealWorldTimeStamp == updateTimeStamp())
      return;

    /*
     * float[] depth3dArray = depthMapRealWorldA(); int index=0; for(int i=0;i <
     * _depthMapRealWorld.length;i++) {
     * _depthMapRealWorld[i].set(depth3dArray[index++], depth3dArray[index++],
     * depth3dArray[index++]); }
     */

    depthMapRealWorld(_depthMapRealWorldXn);
    int index = 0;
    for (int i = 0; i < _depthMapRealWorld.length; i++) {
      _depthMapRealWorld[i].set(_depthMapRealWorldXn[index++], _depthMapRealWorldXn[index++], _depthMapRealWorldXn[index++]);
    }

    _depthRealWorldTimeStamp = updateTimeStamp();
  }

  protected void updateImage() {
    if ((nodes() & NODE_IMAGE) == 0)
      return;
    if (_rgbTimeStamp == updateTimeStamp())
      return;

    // copy the rgb map
    _rgbImage.loadPixels();
    rgbImage(_rgbImage.pixels);
    _rgbImage.updatePixels();

    _rgbTimeStamp = updateTimeStamp();
  }

  protected void updateIrImage() {
    if ((nodes() & NODE_IR) == 0)
      return;
    if (_irImageTimeStamp == updateTimeStamp())
      return;

    _irImage.loadPixels();
    irImage(_irImage.pixels);
    _irImage.updatePixels();

    _irImageTimeStamp = updateTimeStamp();
  }

  protected void updateUserImage() {
    if ((nodes() & NODE_USER) == 0)
      return;
    if (_userImageTimeStamp == updateTimeStamp())
      return;

    // copy the scene map
    _userImage.loadPixels();
    userImage(_userImage.pixels);
    _userImage.updatePixels();
    _userImageTimeStamp = updateTimeStamp();
  }

  protected void updateUserRaw() {
    if ((nodes() & NODE_USER) == 0)
      return;
    if (_userImageTimeStamp == updateTimeStamp())
      return;

    userMap(_userRaw);

    _userImageTimeStamp = updateTimeStamp();
  }

  /*
   * public int[] getUsersPixels(int user) { int size = userWidth() *
   * userHeight(); if(size == 0) return _userRaw;
   * 
   * if(_userRaw.length != userWidth() * userHeight()) { // resize the array
   * _userRaw = new int[userWidth() * userHeight()]; }
   * 
   * super.getUserPixels(user,_userRaw); return _userRaw; }
   */
  public PImage userImage() {
    updateUserImage();
    return _userImage;
  }

  public int[] userMap() {
    updateUserRaw();
    return _userRaw;
  }

  /*
   * protected void onStartCalibrationCb(long userId) { try {
   * _startCalibrationMethod.invoke(_calibrationCbObject, new Object[] {
   * (int)userId }); } catch (Exception e) { } }
   * 
   * protected void onEndCalibrationCb(long userId, boolean successFlag) { try {
   * _endCalibrationMethod.invoke(_calibrationCbObject, new Object[] {
   * (int)userId, successFlag}); } catch (Exception e) { } }
   * 
   * protected void onStartPoseCb(String strPose, long userId) { try {
   * _startPoseMethod.invoke(_poseCbObject, new Object[] { strPose,(int)userId
   * }); } catch (Exception e) { } }
   * 
   * protected void onEndPoseCb(String strPose, long userId) { try {
   * _endPoseMethod.invoke(_poseCbObject, new Object[] { strPose,(int)userId });
   * } catch (Exception e) { } }
   * 
   * // hands protected void onCreateHandsCb(long nId, XnPoint3D pPosition,
   * float fTime) { try { _createHandsMethod.invoke(_handCbObject, new Object[]
   * { (int)nId,new
   * PVector(pPosition.getX(),pPosition.getY(),pPosition.getZ()),fTime}); }
   * catch (Exception e) {} }
   * 
   * protected void onUpdateHandsCb(long nId, XnPoint3D pPosition, float fTime)
   * { try { _updateHandsMethod.invoke(_handCbObject, new Object[] {
   * (int)nId,new
   * PVector(pPosition.getX(),pPosition.getY(),pPosition.getZ()),fTime}); }
   * catch (Exception e) {} }
   * 
   * protected void onDestroyHandsCb(long nId, float fTime) { try {
   * _destroyHandsMethod.invoke(_handCbObject, new Object[] { (int)nId,fTime});
   * } catch (Exception e) {} }
   * 
   * protected void onRecognizeGestureCb(String strGesture, XnPoint3D
   * pIdPosition, XnPoint3D pEndPosition) { try {
   * _recognizeGestureMethod.invoke(_gestureCbObject, new Object[] { strGesture,
   * new PVector(pIdPosition.getX(),pIdPosition.getY(),pIdPosition.getZ()), new
   * PVector(pEndPosition.getX(),pEndPosition.getY(),pEndPosition.getZ()) }); }
   * catch (Exception e) {} }
   * 
   * protected void onProgressGestureCb(String strGesture, XnPoint3D pPosition,
   * float fProgress) { try { _progressGestureMethod.invoke(_gestureCbObject,
   * new Object[] { strGesture, new
   * PVector(pPosition.getX(),pPosition.getY(),pPosition.getZ()), fProgress });
   * } catch (Exception e) {} }
   * 
   * // nite callbacks protected void onStartSessionCb(XnPoint3D ptPosition) {
   * try { _startSessionMethod.invoke(_sessionCbObject, new Object[] { new
   * PVector(ptPosition.getX(),ptPosition.getY(),ptPosition.getZ()) }); } catch
   * (Exception e) {} }
   * 
   * protected void onEndSessionCb() { try {
   * _endSessionMethod.invoke(_sessionCbObject, new Object[] { }); } catch
   * (Exception e) {} }
   * 
   * protected void onFocusSessionCb(String strFocus, XnPoint3D ptPosition,
   * float fProgress) { try { _focusSessionMethod.invoke(_sessionCbObject, new
   * Object[] {strFocus, new
   * PVector(ptPosition.getX(),ptPosition.getY(),ptPosition.getZ()), fProgress
   * }); } catch (Exception e) {}
   * 
   * }
   */
}

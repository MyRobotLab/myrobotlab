package org.myrobotlab.openni;

/**

 Andrew Davison, September 2011, ad@fivedots.coe.psu.ac.th

 PointsShape is a Java 3D shape which is drawn as a collection
 of colored points stored in a PointsArray. These points are
 calculated from the Kinect's current depth buffer.

 The points' coordinates and colors are represented
 by two arrays: coords[] and colors[]

 The points are stored in the PointArray as a BY_REFERENCE geometry,
 which means that only the coords[] and colors[] arrays need
 to be changed in order to affect the PointArray. Once changed, Java 3D
 automatically redraws the PointArray in the 3D scene.

 When a new depth buffer is passed to updateDepthCoords(), a request
 is made to Java 3D to update the PointArray, which is does by calling
 updateData() which updates the coords[] and colors[] arrays.

 PointsShape implements GeometryUpdater so it can update
 the PointArray by having the system call it's updateData() method.

 The mapping from 8-bits to colour is done
 using the ColorUtils library methods 
 (http://code.google.com/p/colorutils/)

 Other references :
 http://ex.osaka-kyoiku.ac.jp/~fujii/JREC6/onlinebook_selman/Htmls/3DJava_Ch04.htm - killer point array demo
 http://pesona.mmu.edu.my/~ypwong/virtualreality/java3d_tutorial_dave/slides/mt0084.htm - more great demos - LineArray MeshArray

 There should be data and then - a translation set of coordinates - base on other criteria - x & y step count, mesh size etc.
 */

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.concurrent.Semaphore;

import javax.media.j3d.Appearance;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.PointArray;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.TriangleArray;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.data.KinectSensorData;
import org.slf4j.Logger;

public class PointsShape extends Shape3D implements GeometryUpdater {

  public final static Logger log = LoggerFactory.getLogger(PointsShape.class.getCanonicalName());

  /**
   * resolution of depth image; change to match setting in DepthReader the
   * resolution of a kinect IR camera is always 640x480
   */
  private static final int IM_WIDTH = 640;
  private static final int IM_HEIGHT = 480;

  /**
   * display volume for particles inside the 3D scene; arrived at by
   * trial-and-error testing to see what looked 'good' in the scene
   */
  // private static final int X_WIDTH = 12;
  // private static final int Y_WIDTH = 12;
  // private static final int Z_WIDTH = 50;

  /**
   * the gap between depth positions being sampled this is an optimization of
   * only sampling modulus SAMPLE_FREQ on the X axis data
   */
  // private static final int SAMPLE_FREQ = 1;

  private static final int MAX_POINTS = IM_WIDTH * IM_HEIGHT; // wuh? FIXME -
  // what is this
  // - shouldn't a
  // frame from a
  // kinect always
  // be 640X480X2
  // for the 11bit
  // res?
  /*
   * make sure that MAX_POINTS*SAMPLE_FREQ >= IM_WIDTH*IM_HEIGHT otherwise the
   * coords[] array will not be big enough for all the sampled points
   */

  private final static int POINT_SIZE = 1;

  // private float xScale, yScale, zScale;

  /**
   * Java 3D geometry holding the points
   */
  private PointArray cloud;
  private float[] coords, colors; // holds (x,y,z) and (R,G,B) of the points
  private TriangleArray mesh;

  /**
   * used to make updateDepthCoords() wait until GeometryUpdater.updateData()
   * has finished an update
   */
  private Semaphore sem;
  private KinectSensorData kinectData;

  /**
   * This method is called by the system some (short) time after
   * pointParts.updateData(this) is called in updateDepthCoords(). An update of
   * the geometry is carried out: the z-coord is changed in coords[], and the
   * point's corresponding colour is updated
   * 
   * Understand there is a distinction between depth data - and the display
   * array the pure depth data is always 640 X 480 resolution - but display area
   * expands and the resolution per fixed area decreases as an inverse to the
   * distance from the sensor
   * 
   * References : http://openkinect.org/wiki/Imaging_Information depthInMeters =
   * 1.0 / (rawDepth * -0.0030711016 + 3.3309495161);
   */
  int w = 640;

  int h = 480;

  int minDistance = -10;

  float scaleFactor = 0.0021f;
  final double fx_d = 1.0 / 5.9421434211923247e+02;
  final double fy_d = 1.0 / 5.9104053696870778e+02;
  final double cx_d = 3.3930780975300314e+02;

  final double cy_d = 2.4273913761751615e+02;
  int min = 100000;
  int max = 0;
  float displayScale = 0.001f;

  public PointsShape() {
    // BY_REFERENCE PointArray storing coordinates and colors
    cloud = new PointArray(MAX_POINTS, GeometryArray.COORDINATES | GeometryArray.COLOR_3 | GeometryArray.BY_REFERENCE);
    mesh = new TriangleArray(MAX_POINTS, GeometryArray.COORDINATES | GeometryArray.COLOR_3 | GeometryArray.BY_REFERENCE);

    TransparencyAttributes ta = new TransparencyAttributes();
    ta.setTransparencyMode(TransparencyAttributes.NICEST);
    ta.setTransparency(0.0f);

    PointAttributes pointAttributes = new PointAttributes();
    pointAttributes.setPointSize(2.83f);
    pointAttributes.setCapability(PointAttributes.ALLOW_SIZE_WRITE);

    Appearance a = new Appearance();
    a.setPointAttributes(pointAttributes);
    a.setTransparencyAttributes(ta);

    // the data structure can be read and written at run time
    cloud.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
    cloud.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);

    mesh.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
    mesh.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);

    sem = new Semaphore(0);

    // create PointsShape geometry and appearance
    createGeometry();
    createAppearance();
    // setAppearance(a);
  }

  private void createAppearance() {
    Appearance app = new Appearance();

    PointAttributes pa = new PointAttributes();
    pa.setPointSize(POINT_SIZE); // fix point size
    app.setPointAttributes(pa);

    setAppearance(app);
  } // end of createAppearance()

  /**
   * Create and initialize coords and colors arrays for the depth points. Only
   * sample every SAMPLE_FREQ point to reduce the arrays size. Each point
   * requires 3 floats in the coords array (x, y, z) and 3 floats in the colours
   * array (R, G, B) + Alpha
   * 
   * The z-coordinates will change as the depths change, which will cause the
   * points colors to change as well.
   */
  private void createGeometry() {
    // TODO - make a XModulus X YModulus - to limit the number of array
    // points

    coords = new float[IM_WIDTH * IM_HEIGHT * 3]; // for (x,y,z) coords of a
    // point
    colors = new float[IM_WIDTH * IM_HEIGHT * 3]; // to store each a point's
    // color
    /*
     * int pointsCount = IM_WIDTH * IM_HEIGHT;
     * 
     * for (int index = 0; index < pointsCount*3; index+=3) { // if (dpIdx %
     * SAMPLE_FREQ == 0) { // only look at depth index that is // to be sampled
     * // int ptIdx = (dpIdx / SAMPLE_FREQ) * 3; // calc point index // if
     * (ptIdx < MAX_POINTS * 3) { // is there enough space? coords[index] =
     * index%IM_WIDTH * 0.01f;// * xScale; // x coord coords[index + 1] =
     * (index/3)/IM_WIDTH * 0.01f;// * yScale; // y coord coords[index + 2] =
     * 1f; // z coord (will change later)
     * 
     * // initial point colour is white (will change later) colors[index] =
     * 1.0f; colors[index + 1] = 1.0f; colors[index + 2] = 1.0f; // colors[index
     * + 3] = 1.0f;
     * 
     * // } // } } System.out.println("Initialized " + pointsCount + " points");
     * System.out.println("min  " + min + " max " + max);
     */
    // store the coordinates and colours in the PointArray
    cloud.setCoordRefFloat(coords); // use BY_REFERENCE
    cloud.setColorRefFloat(colors);

    // mesh.setCoordRefFloat(coords); // use BY_REFERENCE
    // mesh.setColorRefFloat(colors);
    /*
     * PointsShape is drawn as the collection of colored points stored in the
     * PointsArray.
     */
    setGeometry(cloud);
    // setGeometry(mesh);
  } // end of createGeometry()

  private void printCoord(float[] coords, int xIdx) {
    System.out.println("" + xIdx + ". depth coord (x,y,z): (" + coords[xIdx] + ", " + coords[xIdx + 1] + ", " + coords[xIdx + 2] + ")");
  }

  /**
   * map z-coord to colormap key between 0 and 255, and store its color as the
   * point's new color; similar to the depth coloring in version 5 of the
   * ViewerPanel example: red in forground (and for no depth), changing to
   * violet in the background
   */
  private void updateColour(int xCoordIdx, float zCoord) {

    Color col = new Color(Color.HSBtoRGB((zCoord * (0.5f)), 0.9f, 0.7f)); // TODO
    // -
    // calculate
    // color
    // range
    // based
    // on
    // max
    // depth

    // assign colormap color to the point as a float between 0-1.0f
    colors[xCoordIdx] = col.getRed() / 255.0f;
    colors[xCoordIdx + 1] = col.getGreen() / 255.0f;
    colors[xCoordIdx + 2] = col.getBlue() / 255.0f;
    colors[xCoordIdx + 3] = 1.0f;
  } // end of updateColour()

  @Override
  public void updateData(Geometry geo) {

    PrintWriter out = null;
    try {
      out = new PrintWriter("meshlab.xyz");
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    short[] data = kinectData.data; // 640 X 480 11 bit depth data

    int coordIndex = 0;
    for (int depthDataIndex = 0; depthDataIndex < data.length; ++depthDataIndex) {
      coordIndex = depthDataIndex * 3;// +2 z data offset ???

      // we have (i,j,r) -> data x - data y - raw depth in a single array
      // we want (xyz) -> display x y z in Cartesian coordinates
      // start z
      // float distance = 0.1236 * Math.tan(data[depthDataIndex + 2] /
      // 2842.5 + 1.1863) in meters.
      int i = depthDataIndex % w;
      int j = (depthDataIndex / w);
      int r = data[depthDataIndex];
      // float z = -4;
      // float z = displayScale * (float)(1/(-0.00307 *
      // data[depthDataIndex] + 3.33)); // in centimeters
      // float z = (float)(10.0 / ((double)(data[depthDataIndex]) *
      // 0.30711016 + 3.3309495161));
      // float x = displayScale * (depthDataIndex + 2 - w / 2) * (z +
      // minDistance) * scaleFactor * (w/h);
      // float y = displayScale * (j - h / 2) * (z + minDistance) *
      // scaleFactor;
      float z = -displayScale * (data[depthDataIndex]);
      float x = (float) ((i - cx_d) * -0.001 * r * fx_d);
      float y = (float) ((j - cy_d) * -0.001 * r * fy_d);

      coords[coordIndex] = 10 * x;
      coords[coordIndex + 1] = 10 * y;
      coords[coordIndex + 2] = 10 * z;

      out.print(String.format("%f %f %f\n", x * 10, y * 10, z * 10));

      if (r < min && r != 0)
        min = r;
      if (r > max)
        max = r;

      // with observed min 451 max 9757 there about 10000 depth values

      Color color = new Color(Color.HSBtoRGB((r / (float) 1000), 0.9f, 0.7f));
      colors[coordIndex] = color.getRed() / 255.0f;
      colors[coordIndex + 1] = color.getGreen() / 255.0f;
      colors[coordIndex + 2] = color.getBlue() / 255.0f;
      // colors[xCoordIdx + 3] = 1.0f; transparency

      // if (depthDataIndex == 0)
      // {
      // log.warn(String.format("top left ijr (%d,%d,%d) => xyz (%f,%f,%f)
      // (%f,%f,%f) ",
      // i,j,r, x,y,z, 39.3701 * x, 39.3701 * y, 39.3701 * z));
      // } else
      if (depthDataIndex == 153920) // 640 * 240 + 320 == midpoint index
      {
        log.warn(String.format("midpoint ijr (%d,%d,%d) => xyz (%f,%f,%f)  (%f,%f,%f) ", i, j, r, x, y, z, 39.3701 * x, 39.3701 * y, 39.3701 * z));
      }
      /*
       * if (depthDataIndex == 306081) { log.warn(String.format(
       * "br       ijr (%d,%d,%d) => xyz (%f,%f,%f)  (%f,%f,%f) ", i,j,r, x,y,z,
       * 39.3701 * x, 39.3701 * y, 39.3701 * z)); }
       */

    }

    out.close();
    log.warn(String.format("min %d max %d", min, max));
    /*
     * for (int i = 0; i < data.length; ++i) { float zCoord = ((float) data[i])
     * * zScale; // convert to 3D scene //float zCoord = zScale * (float) (1.0 /
     * ((float) data[i] * -0.0030711016 + 3.3309495161));; // coord if (i %
     * SAMPLE_FREQ == 0) { // save this z-coord int zCoordIdx = (i /
     * SAMPLE_FREQ) * 3 + 2; if (zCoordIdx < coords.length) { coords[zCoordIdx]
     * = -zCoord; // negate so depths are spread out along -z axis, away from //
     * camera // printCoord(coords, zCoordIdx-2); updateColour(zCoordIdx - 2,
     * zCoord); } } }
     */

    sem.release();
    // signal that update is finished; now updateDepthCoords() can return
  } // end of updateData()

  /*
   * Use new depth buffer data to update the PointsArray inside the Java 3D
   * scene. This method is repeatedly called by DepthReader as the depth buffer
   * changes. This method will not return until the 3D scene has been updated.
   */
  public void updateDepthCoords(KinectSensorData kd) {

    this.kinectData = kd;

    cloud.updateData(this); // request an update of the geometry
    // mesh.updateData(this);
    try {
      sem.acquire(); // wait for update to finish in updateData()
    } catch (InterruptedException e) {
      Logging.logError(e);
    }
  } // end of updateDepthCoords()

} // end of PointsShape class

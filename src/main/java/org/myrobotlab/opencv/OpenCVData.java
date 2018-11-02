package org.myrobotlab.opencv;

import static org.myrobotlab.service.OpenCV.INPUT_KEY;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.geometry.Point2Df;
import org.myrobotlab.math.geometry.Rectangle;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

/**
 * This is the data returned from a single pass of an OpenCV pipeline of
 * filters. The amount of data can be changed depending on individual
 * configuration of the filters. The filters have the ability to add a copy of
 * the image and add other data structures such as arrays of point, bounding
 * boxes, masks, classifications and other information.
 * 
 * The default behavior is to return the data from the LAST FILTER ON THE
 * PIPELINE
 * 
 * Some optimizations are done by saving the results of type conversions. For
 * example if a JPG is asked for it is saved back into the data map, so that if
 * its asked again, the cached copy will be returned
 * 
 * All data is put in with keys with the following format
 * [ServiceName].[FilterName].[Data Type] - e.g.
 * <b><i>opencv.pyramidDown.Frame</i></b>
 * 
 * input and output are key "FilterName"s which represent the beginning and end
 * of pipeline data. The input Frame for example will have the key
 * <b><i>opencv.input.Frame</i></b>
 * 
 * Data from the filters is captured typically in non-graphic form, then for a
 * display it is added to the Java Graphics2D of the display BufferedImage.
 * 
 * 
 * <pre>
 * 
 * cv.input.Frame
 * cv.input.IplImage        => 
 * cv.canny.IplImage        => result of canny filter
 * cv.canny.BufferedImage   => conversion of IplImage -> BufferedImage
 * cv.display.BufferedImage <= result of conversion
 * cv.output.IplImage
 * 
 * 
 * </pre>
 * 
 * processDisplay vs display vs selected display
 * 
 * data.getGraphics should be used by filters to manage their displays
 * 
 * @author GroG
 * 
 */
public class OpenCVData implements Serializable {
  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVData.class);

  /**
   * serializable objects - these can be transported TODO - implement later...
   */
  // HashMap<String, Object> serializable = null;

  /**
   * the type converters from JavaCV
   */
  transient static OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
  transient static OpenCVFrameConverter.ToIplImage converterToIplImage = new OpenCVFrameConverter.ToIplImage();
  transient static Java2DFrameConverter converterToJava = new Java2DFrameConverter();

  /**
   * all non-serializable data including frames an IplImages It will also
   * contain a global source set of keys
   */
  transient final Map<String, Object> sources = new TreeMap<>();

  /**
   * name of the service which produced this data
   */
  private String name;

  /**
   * graphics object for display
   */
  transient Map<String, Graphics2D> g2ds = new HashMap<>();

  /**
   * list of filters which have processed this pipeline
   */
  List<String> filters = new ArrayList<String>();

  /**
   * the filter's name - used as a key to get or put data associated with a
   * specific filter
   */
  private String outputFilter = INPUT_KEY;
  private String selectedFilter = INPUT_KEY;

  private long timestamp;
  private int frameIndex;
  private int eyesDifference;

  public OpenCVData(String name, long frameStartTs, int frameIndex, Frame frame) {
    this.name = name;
    this.timestamp = frameStartTs;
    this.frameIndex = frameIndex;
    selectedFilter = INPUT_KEY;

    // before the first filter is added input & output point to the
    // same things
    sources.put(String.format("%s.input.Frame", name), frame);
    sources.put(String.format("%s.output.Frame", name), frame);

    IplImage firstImage = converterToIplImage.convertToIplImage(frame);
    sources.put(String.format("%s.input.IplImage", name), firstImage);
    sources.put(String.format("%s.output.IplImage", name), firstImage);

  }

  /**
   * Order of fetching a display try => displayFilterName try => output try =>
   * input
   * 
   * @return
   */
  public BufferedImage getBufferedImage() {
    return getBufferedImage(null);
  }

  public Graphics2D getGraphics(String filterKey) {
    if (g2ds.containsKey(filterKey)) {
      return g2ds.get(filterKey);
    } else {
      Graphics2D graphics = getBufferedImage(filterKey).createGraphics();
      g2ds.put(filterKey, graphics);
      return graphics;
    }
  }

  public Frame getInputFrame() {
    return (Frame) sources.get(String.format("%s.input.Frame", name));
  }

  public IplImage getInputImage() {
    return getImage("input");
  }

  public Mat getMat(String filterKey) {
    String key = String.format("%s.%s.Mat", filterKey, name);
    Mat image = null;
    if (!sources.containsKey(key)) {
      image = converterToMat.convert(getFrame(filterKey));
      sources.put(key, image);
    }
    return (Mat) sources.get(key);
  }

  public Frame getFrame() {
    return getFrame(null);
  }

  public Frame getFrame(String filterKey) {
    String key = String.format("%s.Frame", getKeyPrefix(filterKey));
    return (Frame) sources.get(key);
  }

  public IplImage getImage() {
    return getImage(null);
  }

  /**
   * the generalized getImage returns the 'latest' output - if that does not
   * exist it return the original input - most other type converters should use
   * this method
   * 
   * @return
   */
  public IplImage getImage(String filterKey) {

    // try cumulative output
    String key = String.format("%s.IplImage", getKeyPrefix(filterKey));
    if (sources.containsKey(key)) {
      return (IplImage) sources.get(key);
    }

    IplImage image = null;
    if (!sources.containsKey(key)) {
      image = converterToMat.convertToIplImage(getFrame(filterKey));
      sources.put(key, image);
    }
    return (IplImage) sources.get(key);
  }

  public String getName() {
    return name;
  }
  
  public IplImage getOutputImage() {
    return getImage("output");
  }

  /**
   * resource cleanup
   */
  public void dispose() {
    for (Graphics2D g : g2ds.values()) {
      g.dispose();
    }
  }

  public String writeDisplay() throws IOException {
    return writeDisplay("OpenCV", "png");
  }

  public String writeDisplay(String dir, String format) throws IOException {
    String filename = null;
    if (dir == null) {
      filename = String.format("%s-%05d.%s", name, frameIndex, format);
    } else {
      File parent = new File(dir);
      parent.mkdirs();
      filename = String.format("%s/%s-%05d.%s", dir, name, frameIndex, format);
    }

    FileOutputStream fos = new FileOutputStream(filename);
    ImageIO.write(getDisplay(), format, fos);
    fos.close();
    return filename;
  }

  /**
   * getDisplay will attempt to get whatever was put into the {name}.display If
   * that doesn't exist - the assumption is that no filter exported nor created
   * any display - if that's the case, we get the original input and create the
   * display
   * 
   * This is an "optimization" because real robots don't need displays, and if a
   * display is really really needed for a human delay it until the very end...
   * 
   * FIXME - logic to convert from selected or convert from input should
   * probably NOT be here, but be controlled by the filters more directly
   * 
   * @return
   */
  public BufferedImage getDisplay() {
    BufferedImage bi = null;
    String key = String.format("%s.BufferedImage", getKeyPrefix("output"));
    if (!sources.containsKey(key)) {
      
      IplImage image = getImage(); // <- should be output or "selected Filter .. i guess"
      if (image != null) {
      // bi = converterToJava.convert(getInputFrame());
        bi = converterToJava.convert(converterToMat.convert(image));
      } else {
        bi = converterToJava.convert(getInputFrame()); // logic should probably not be buried down
      }
      // cache result
      sources.put(key, bi);
      // put(String.format("%s.display", name), bi);
    }
    return (BufferedImage) sources.get(key);
  }

  /**
   * method called when a filter has finished processing
   * 
   * @param processedImage
   * 
   *          TODO - end of display cache display too
   */
  public void postProcess(IplImage processedImage) {
    put(processedImage);
    filters.add(selectedFilter);
  }

  public void put(Graphics2D object) {
    sources.put(String.format("%s.output.Graphics2D", name), object);
    sources.put(String.format("%s.%s.Graphics2D", name, selectedFilter), object);
  }

  public void put(IplImage object) {
    sources.put(String.format("%s.output.IplImage", name), object);
    sources.put(String.format("%s.%s.IplImage", name, selectedFilter), object);
  }

  public void put(Mat object) {
    sources.put(String.format("%s.output.Mat", name), object);
    sources.put(String.format("%s.%s.Mat", name, selectedFilter), object);
  }

  public void put(BufferedImage object) {
    sources.put(String.format("%s.output.BufferedImage", name), object);
    sources.put(String.format("%s.%s.BufferedImage", name, selectedFilter), object);
  }

  /**
   * This is the typical method filters will use to store their output, it has a key
   * with their filter's name and an "output" reference.
   * @param keyPart
   * @param object
   */
  public void put(String keyPart, Object object) {
    sources.put(String.format("%s.output.%s", name, keyPart), object);
    sources.put(String.format("%s.%s.%s", name, selectedFilter, keyPart), object);
  }

  public long getTs() {
    return timestamp;
  }

  public void setSelectedFilter(String selectedFilter) {
    this.selectedFilter = selectedFilter;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%s ts %d fi %d\n", name, timestamp, frameIndex));
    sb.append(String.format("selectedFilter: %s\n", selectedFilter));
    // sb.append(String.format("displayFilter: %s\n", displayFilter));
    sb.append("data:\n");
    for (String key : sources.keySet()) {
      sb.append(key);
      Object o = sources.get(key);
      sb.append("= ");
      sb.append(System.identityHashCode(o) % 1000);
      sb.append(" ");
      sb.append(o.getClass().getSimpleName());
      if (o.getClass().equals(IplImage.class)) {
        IplImage i = (IplImage) o;
        sb.append(" width=").append(i.width());
        sb.append(" height=").append(i.height());
        sb.append(" depth=").append(i.depth());
        sb.append(" channels=").append(i.nChannels());
      } else if (o.getClass().equals(IplImage.class)) {
        Frame f = (Frame) o;
        sb.append(" width=").append(f.imageWidth);
        sb.append(" height=").append(f.imageHeight);
        sb.append(" depth=").append(f.imageDepth);
        sb.append(" channels=").append(f.imageChannels);
        sb.append(" stride=").append(f.imageStride);
      } else if (o.getClass().equals(BufferedImage.class)) {
        BufferedImage f = (BufferedImage) o;
        sb.append(" width=").append(f.getWidth());
        sb.append(" height=").append(f.getHeight());
        sb.append(" type=").append(f.getType());
      } else {
        sb.append(o);
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  public String getKeyPrefix(String filterKey) {
    if (filterKey == null && selectedFilter != null) {
      filterKey = selectedFilter;
    } else if (filterKey == null && selectedFilter == null) {
      filterKey = "output";
    }

    return String.format("%s.%s", name, filterKey);
  }

  public BufferedImage getBufferedImage(String filterKey) {
    // search through current sources
    String key = String.format("%s.BufferedImage", getKeyPrefix(filterKey));
    BufferedImage image = (BufferedImage) sources.get(key);

    if (image != null) {
      // 1st selected ? 2nd output ?
      image = converterToJava.convert(converterToMat.convert(getImage(filterKey)));
      sources.put(key, image);
    }
    return (BufferedImage) sources.get(key);
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init();
      File f = new File("OpenCV/prpol-rerender2.avi");
      log.info("{}", f.exists());
      // TODO - test
      FrameGrabber grabber = new FFmpegFrameGrabber(f);// OpenCVFrameGrabber.createDefault(f);

      grabber.start();
      Frame frame = grabber.grab();
      OpenCVData data = new OpenCVData("cv", 1540660275000L, 0, frame);
      BufferedImage img = data.getDisplay();

      CanvasFrame cframe = new CanvasFrame("test");
      cframe.showImage(frame);
      // cframe.showImage(frame, true);

      JFrame jframe = new JFrame();
      ImageIcon icon = new ImageIcon(img);
      JLabel label = new JLabel(icon, JLabel.CENTER);
      JPanel jpanel = new JPanel();
      jpanel.add(label);
      jframe.setContentPane(jpanel);
      jframe.pack();
      jframe.setVisible(true);

      grabber.close();

      data.writeDisplay();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public void putDisplay(BufferedImage bi) {
    sources.put(String.format("%s.BufferedImage", getKeyPrefix("output")), bi);
  }

  public String getSelectedFilter() {
    return selectedFilter;
  }
  
  public void putKinectDepth(IplImage kinect) {    
    sources.put(OpenCV.INPUT_KEY, kinect);
  }

  public IplImage getKinectDepth() {    
    return (IplImage)sources.get(OpenCV.INPUT_KEY);
  }

  public Object getFrameIndex() {
    return frameIndex;
  }

  public ArrayList<Rectangle> getBoundingBoxArray() {    
    return (ArrayList)sources.get(String.format("%s.output.BoundingBoxArray", name));
  }

  public void putBoundingBoxArray(ArrayList<Rectangle> bb) {    
    sources.put(String.format("%s.output.BoundingBoxArray", name), bb);
  }
  public IplImage get(String fullKey) {    
    return (IplImage)sources.get(fullKey);
  }

  public List<Point2Df> getPointArray() {
    return (ArrayList)sources.get(String.format("%s.output.PointArray", name));
  }

}

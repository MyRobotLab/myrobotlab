package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

/**
 * GPS - Global Positioning System for MyRobotLab. It will read data from a GPS
 * sensor through a serial connection and parse it into its appropriate fields
 * The service is able to parse NMEA sentences coming in over Serial (including
 * Bluetooth Serial). One important note is that the Lat and Lon in NMEA are in
 * the format ddmm.mmmm which means that they have to be converted to Degrees
 * (d) from Degrees(d) Minutes(m) to be used with some of the other functions in
 * the service. The service automatically does the conversion when it parses the
 * sentences into GPSData objects. If you capture the raw GPS data coming out of
 * the device to a file, it won't be the converted version. It will be the raw
 * NMEA value. some Geo Fence capabilities have been added. The most basic of
 * these is the Point based radius. You define a Lat/Lon point and a radius
 * around it in meters and then you can test to see if other points are inside
 * or outside the fence. A more complicated/flexible version is created by
 * sending an array of GPS points to form a polygon. The last point will be
 * connected back to the first point automatically to close the fence. So if
 * your robot is sending you it's current GPS coordinates, you can see if has
 * wandered into our out of a fenced area.
 *
 */
public class Gps extends Service implements SerialDataListener {

  /***********************************************************************************
   * This block of methods will be used to GeoFencing This code is based on the
   * examples on the following blog http://stefanbangels.blogspot.be/2012/12
   * /for-several-years-now-i-have-been.html
   * http://stefanbangels.blogspot.be/2014
   * /03/point-geo-fencing-sample-code.html
   * http://stefanbangels.blogspot.nl/2013/10/geo-fencing-sample-code.html
   *********************************************************************************/
  // We need a circle object to build a point/radius geofence
  class Circle {

    private double lat;
    private double lon;
    private int radius;

    public Circle(double lat, double lon, int radius) {
      this.lat = lat;
      this.lon = lon;
      this.radius = radius;
    }

    public double getLat() {
      return lat;
    }

    public double getLon() {
      return lon;
    }

    public int getRadius() {
      return radius;
    }

    public int setRadius(int m) {
      radius = m;
      return radius;
    }

  }

  // publish gps data begin ---
  public static class GpsData {
    public String type; // msg type
    public Double latitude;
    public Double longitude;
    String time;

    HashMap<String, String> addInfo = new HashMap<String, String>();
  }

  // We need a line to break a polygon down
  class Line {

    private Point from;
    private Point to;

    public Line(Point from, Point to) {
      this.from = from;
      this.to = to;
    }

    public Point getFrom() {
      return from;
    }

    public Point getTo() {
      return to;
    }

  }

  // We need a point object to build a line or polygon
  // FIXME - make common geometric POJOs !
  public class Point {

    private double lat;
    private double lon;

    public Point(double lat, double lon) {
      this.lat = lat;
      this.lon = lon;
    }

    public double getLat() {
      return lat;
    }

    public double getLon() {
      return lon;
    }

  }

  public class Polygon {

    private Point[] points;

    public Polygon(Point[] points) {
      this.points = points;
    }

    public Point[] getPoints() {
      return points;
    }

  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Gps.class.getCanonicalName());

  public static final String MODEL = "FV_M8";

  public static final String GEOID_SEPARATION_KEY = "GEOID_SEPARATION_KEY";

  transient public ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  String model;

  String messageString;

  // peers
  transient public Serial serial;

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Gps template = new Gps("gps1");
      template.startService();

      Python python = new Python("python");
      python.startService();

      Runtime.createAndStart("gui", "SwingGui");
      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Gps(String n) {
    super(n);
  }

  public void addGPSListener(Service service) {
    addListener("publishGPS", service.getName(), "onGPS");
  }

  @Override
  public Integer onByte(Integer b) throws IOException {

    try {
      // log.info("byteReceived Index = " + index + " actual data byte = "
      // + String.format("%02x", b));
      buffer.write(b);
      // so a byte was appended
      // now depending on what model it was and
      // what stage of initialization we do that funky stuff
      if (b == 0x0a) { // GPS strings end with /CR /LF = 0x0d 0x0a

        // log.info("Buffer size = " + buffer.size() + " Buffer = " +
        // buffer.toString());
        buffer.flush(); // flush entire buffer so I can convert it to a
        // byte array
        // message = buffer.toByteArray();
        messageString = new String(buffer.toByteArray(), ("UTF-8"));
        // log.info("size of message = " + message.length);

        if (messageString.contains("GGA")) {
          log.info("GGA string detected");
          invoke("publishGGAData");
        } else if (messageString.contains("RMC")) {
          log.info("RMC string detected");
          invoke("publishRMCData");
        } else if (messageString.contains("VTG")) {
          log.info("VTG string detected");
          invoke("publishVTGData");
        } else if (messageString.contains("GSA")) {
          log.info("GSA string detected");
          invoke("publishGSAData");
        } else if (messageString.contains("GSV")) {
          log.info("GSV string detected");
          invoke("publishGSVData");
        } else if (messageString.contains("GLL")) {
          log.info("GLL string detected");
          invoke("publishGLLData");
        } else if (messageString.contains("ZDA")) {
          log.info("ZDA string detected");
          invoke("publishZDAData");
        } else if (messageString.contains("MSS")) {
          log.info("MSS string detected");
          invoke("publishMSSData");
        } else if (messageString.contains("POLYN")) // San Jose
        // navigation FV-M8
        // specific?
        {
          log.info("POLYN string detected");
          // invoke("publishPOLYNData");
        } else if (messageString.contains("PMTK101")) {
          log.info("Hot Restart string detected");
          // invoke("publishMTKData");
        } else if (messageString.contains("PMTK010, 001")) {
          log.info("Startup string detected");
          // invoke("publishMTKData");
        } else {
          log.info("unknown string detected");
        }
        buffer.reset();
      }

    } catch (Exception e) {
      error(e.getMessage());
    }

    return b;

  }

  public double calculateDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
    double c = Math.sin(Math.toRadians(latitude1)) * Math.sin(Math.toRadians(latitude2))
        + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2)) * Math.cos(Math.toRadians(longitude2) - Math.toRadians(longitude1));
    c = c > 0 ? Math.min(1, c) : Math.max(-1, c);
    return 3959 * 1.609 * 1000 * Math.acos(c);
  }

  // If they want to calculate distance between Point objects
  public double calculateDistance(Point point1, Point point2) {
    return calculateDistance(point1.getLat(), point1.getLon(), point2.getLat(), point2.getLon());
  }

  boolean calculateInside(List<Point> sortedPoints, double lat) {
    boolean inside = false;
    for (Point point : sortedPoints) {
      if (lat < point.getLat()) {
        break;
      }
      inside = !inside;
    }
    return inside;
  }

  List<Point> calculateIntersectionPoints(List<Line> lines, double lon) {
    List<Point> results = new LinkedList<Point>();
    for (Line line : lines) {
      double lat = calculateLineLatAtLon(line, lon);
      results.add(new Point(lat, lon));
    }
    return results;
  }

  double calculateLineLatAtLon(Line line, double lon) {
    Point from = line.getFrom();
    double slope = calculateSlope(line);
    return from.getLat() + (lon - from.getLon()) / slope;
  }

  List<Line> calculateLines(Polygon polygon) {
    List<Line> results = new LinkedList<Line>();

    // get the polygon points
    Point[] points = polygon.getPoints();

    // form lines by connecting the points
    Point lastPoint = null;
    for (Point point : points) {
      if (lastPoint != null) {
        results.add(new Line(lastPoint, point));
      }
      lastPoint = point;
    }

    // close the polygon by connecting the last point
    // to the first point
    results.add(new Line(lastPoint, points[0]));

    return results;
  }

  double calculateSlope(Line line) {
    Point from = line.getFrom();
    Point to = line.getTo();
    return (to.getLon() - from.getLon()) / (to.getLat() - from.getLat());
  }

  // Test if Lat and Long are inside your circular GeoFence.
  public boolean checkInside(Circle circle, double lat, double lon) {
    return calculateDistance(circle.getLat(), circle.getLon(), lat, lon) < circle.getRadius();
  }

  // If they want to use a Point object
  public boolean checkInside(Circle circle, Point point) {
    return checkInside(circle, point.getLat(), point.getLon());
  }

  // Test if Lat and Lon are inside your polygon GeoFence.
  public boolean checkInside(Polygon polygon, double lat, double lon) {
    List<Line> lines = calculateLines(polygon);
    List<Line> intersectionLines = filterIntersectingLines(lines, lon);
    List<Point> intersectionPoints = calculateIntersectionPoints(intersectionLines, lon);
    sortPointsByLat(intersectionPoints);
    return calculateInside(intersectionPoints, lat);
  }

  // If they want to use a Point object
  public boolean checkInside(Polygon polygon, Point point) {
    return checkInside(polygon, point.getLat(), point.getLon());
  }

  public void connect(String port) throws IOException {
	  serial.open(port, 38400, 8, 1, 0);
    // serial.publishType(PUBLISH_STRING); // GPS units publish strings
  }

  public void connect(String port, int baud) throws IOException {
	  serial.open(port, baud, 8, 1, 0);
  }

  /***********************************************************************************
   * This ends the GeoFence block
   *********************************************************************************/

  // NMEA Lat/Lon values are ddmm.mmmm or dddmm.mmmm respectively and need to
  // be converted
  /**
   * 
   * @param nmea huh?
   * @return no idea
   */
  public double convertNMEAToDegrees(String nmea) {
    String degrees;
    String minutes;
    // If we have 5 leading digits it's a Longitude
    if (nmea.matches("\\d\\d\\d\\d\\d\\.\\d\\d\\d\\d")) {
      degrees = nmea.substring(0, 3);
      minutes = nmea.substring(3);
    } else { // It's a Latitude
      degrees = nmea.substring(0, 2);
      minutes = nmea.substring(2);
    }
    double result = Double.parseDouble(degrees) + Double.parseDouble(minutes) / 60;
    return result;
  }

  public boolean disconnect() throws Exception {
    serial.disconnect();
    return serial.isConnected();
  }

  List<Line> filterIntersectingLines(List<Line> lines, double lon) {
    List<Line> results = new LinkedList<Line>();
    for (Line line : lines) {
      if (isLineIntersectingAtLon(line, lon)) {
        results.add(line);
      }
    }
    return results;
  }

  public SerialDevice getSerial() throws Exception {
    return serial;
  }

  boolean isLineIntersectingAtLon(Line line, double lon) {
    double minLon = Math.min(line.getFrom().getLon(), line.getTo().getLon());
    double maxLon = Math.max(line.getFrom().getLon(), line.getTo().getLon());
    return lon > minLon && lon <= maxLon;
  }

  public void onGPS(GpsData gps) {
    log.info(String.format("lat: %f", gps.latitude));
    log.info(String.format("long: %f", gps.longitude));
  }

  /**
   * The FV-M8 module skips one of the latter elements in the string, leaving
   * only 0-14 elements. GGA Global Positioning System Fixed Data GLL Geographic
   * Position - Latitude/Longitude GSV GNSS Satellites in View RMC Recommended
   * Minimum Specific GNSS Data VTG Course Over Ground and Ground Speed GSA GNSS
   * DOP and Active Satellites MSS MSK Receiver Signal kmc - so the data you
   * have doesn't have two (GLL and MSK)
   * @return string array of data
   */
  public String[] publishGGAData() {

    GpsData gps = new GpsData();

    log.info("publishGGAData has been called");
    log.info("Full data String = " + messageString);

    String[] tokens = messageString.split("[,*]", -1);
    try {
      log.info("String type: " + tokens[0]);
      gps.type = tokens[0];

      log.info("Time hhmmss.ss: " + tokens[1]);
      gps.time = tokens[1];

      log.info("Latitude: " + tokens[2]);
      log.info("North or South: " + tokens[3]);
      if (tokens[2].length() > 0) {
        gps.latitude = convertNMEAToDegrees(tokens[2]);
        tokens[2] = String.valueOf(gps.latitude);

      }
      if (tokens[3].contains("S")) { // if South then negative latitude
        gps.latitude = gps.latitude * -1;
        tokens[2] = String.valueOf(gps.latitude);
      }

      if (tokens[4].length() > 0) {
        gps.longitude = convertNMEAToDegrees(tokens[4]);
        tokens[4] = String.valueOf(gps.longitude);
      }
      if (tokens[5].contains("W")) {// if West then negative longitude
        gps.longitude = gps.longitude * -1;
        tokens[4] = "-" + String.valueOf(gps.longitude);
      }
      log.info("Longitude: " + String.valueOf(gps.longitude));
      log.info("East or West: " + tokens[5]);

      log.info("GPS quality ('0' = no fix, '1' = GPS SPS fix valid, '2' = DGPS, SPS fix valid, '6' = Dead Reckoning fix valid, '8' = simulated): " + tokens[6]);

      log.info("# of Satellites: " + tokens[7]);

      log.info("Horiz Dilution: " + tokens[8]);

      log.info("Altitude (meters above mean sealevel): " + tokens[9]);
      log.info("meters?: " + tokens[10]);

      log.info("Geoid Separation: (Geoid-to-ellipsoid separation. Ellipsoid altitude = MSL Altitude + Geoid Separation.) " + tokens[11]);
      log.info("meters?: " + tokens[12]);

      gps.addInfo.put(GEOID_SEPARATION_KEY, tokens[12]);

      log.info("Seconds since last update (likely blank): " + tokens[13]);

      if (tokens.length == 16) {
        log.info("DGPS reference station ID (likely blank): " + tokens[14]);
        log.info("Checksum: " + tokens[15]);
      } else {
        log.info("Checksum: " + tokens[14]);
      }
      invoke("publishGPS", gps);
    } catch (Exception e) {
      Logging.logError(e);
    }
    return tokens; // This should return data to the python code if the user
    // has subscribed to it
  }// end dataToString

  public String[] publishGLLData() {

    GpsData gps = new GpsData();

    log.info("publishGLLData has been called");
    log.info("Full data String = " + messageString);

    String[] tokens = messageString.split("[,*]", -1);

    log.info("String type: " + tokens[0]);
    gps.type = tokens[0];

    if (tokens[1].length() > 0) {
      gps.latitude = convertNMEAToDegrees(tokens[1]);
      tokens[1] = String.valueOf(gps.latitude);

    }
    if (tokens[2].contains("S")) { // if South then negative latitude
      gps.latitude = gps.latitude * -1;
      tokens[1] = String.valueOf(gps.latitude);
    }

    if (tokens[3].length() > 0) {
      gps.longitude = convertNMEAToDegrees(tokens[3]);
      tokens[3] = String.valueOf(gps.longitude);
    }
    if (tokens[4].contains("W")) {// if West then negative longitude
      gps.longitude = gps.longitude * -1;
      tokens[3] = "-" + String.valueOf(gps.longitude);
    }
    log.info("Longitude: " + String.valueOf(gps.longitude));

    log.info("Time hhmmss.ss: " + tokens[5]);
    gps.time = tokens[5];

    log.info("Status: ('A' = valid, 'V' = not valid): " + tokens[6]);

    if (tokens.length == 9) {
      log.info("Mode: ('A'=Autonomous, 'D'=DGPS, 'E'=DR (Only present in NMEA v3.00)) " + tokens[7]);
      log.info("Checksum: " + tokens[8]);
    } else {
      log.info("Checksum: " + tokens[7]);
    }
    return tokens; // This should return data to the python code if the user
    // has subscribed to it
  }// end dataToString

  public final GpsData publishGPS(final GpsData gps) {
    return gps;
  }

  // publish gps data end ---

  // When your radius is defined in meters, you will need the Haversine
  // formula.
  // This formula will calculate the distance between two points (in meters)
  // while taking into account the earth curvation:

  public String[] publishGSAData() {

    GpsData gps = new GpsData();

    log.info("publishGSAData has been called");
    log.info("Full data String = " + messageString);

    String[] tokens = messageString.split("[,*]", -1);

    log.info("String type: " + tokens[0]);
    gps.type = tokens[0];

    log.info("Mode 1: ('M' = Manually forced into 2D or 3D, 'A' = Automatically allowed to switch between 2D/3D) " + tokens[1]);

    log.info("Mode 2: ('1' = no fix, '2' = 2D, '3' = 3D) " + tokens[2]);

    for (int x = 1; x < 13; x++) {
      log.info("Channel " + x + " (Satellite #): " + tokens[x + 2]);
    }

    log.info("PDOP (positional dilution): " + tokens[15]);

    log.info("HDOP (horizontal dilution): " + tokens[16]);

    log.info("VDOP (vertical dilution): " + tokens[17]);

    log.info("Checksum: " + tokens[18]);
    return tokens; // This should return data to the python code if the user
    // has subscribed to it
  }// end dataToString

  public String[] publishGSVData() {

    GpsData gps = new GpsData();

    log.info("publishGSVData has been called");
    log.info("Full data String = " + messageString);

    String[] tokens = messageString.split("[,*]", -1);
    int last = tokens.length - 1;

    log.info("String type: " + tokens[0]);
    gps.type = tokens[0];

    log.info("Num. GSV messages: " + tokens[1]);

    log.info("Message number: " + tokens[2]);

    log.info("Satellites in view: " + tokens[3]);

    int svBlocks = (tokens.length - 5) / 4; // each GSV string can have 1-4
    // SV blocks and each has 4
    // tokens
    for (int x = 0; x < svBlocks; x++) {
      log.info("Satellite ID: " + tokens[4 + x * 4]);
      log.info("Elevation (0-90 degrees): " + tokens[5 + x * 4]);
      log.info("Azimuth (0-359 degrees): " + tokens[6 + x * 4]);
      log.info("Signal Strength (dBHz): " + tokens[7 + x * 4]);
    }

    log.info("Checksum: " + tokens[last]);
    return tokens; // This should return data to the python code if the user
    // has subscribed to it
  }// end dataToString

  public String[] publishMSSData() {

    log.info("publishMSSData has been called");
    log.info("Full data String = " + messageString);

    String[] tokens = messageString.split("[,*]", -1);

    log.info("String type: " + tokens[0]);

    log.info("Signal Strength (dB): " + tokens[1]);

    log.info("Signal to Noise Ratio (dB): " + tokens[2]);

    log.info("Beacon Freq (kHz): " + tokens[3]);

    log.info("Beacon bitrate (bps): " + tokens[4]);

    if (tokens.length == 7) {
      log.info("Channel Num: " + tokens[5]);
      log.info("Checksum: " + tokens[6]);
    } else {
      log.info("Checksum: " + tokens[5]);
    }
    return tokens; // This should return data to the python code if the user
    // has subscribed to it
  }// end dataToString

  public String[] publishRMCData() {

    GpsData gps = new GpsData();

    log.info("publishRMCData has been called");
    log.info("Full data String = " + messageString);

    String[] tokens = messageString.split("[,*]", -1);

    log.info("String type: " + tokens[0]);
    gps.type = tokens[0];

    log.info("Time (hhmmss.ss): " + tokens[1]);
    gps.time = tokens[1];

    log.info("Status ('V' = warning, 'A' = Valid): " + tokens[2]);

    log.info("Latitude: " + tokens[3]);
    log.info("North or South: " + tokens[4]);
    if (tokens[3].length() > 0) {
      gps.latitude = convertNMEAToDegrees(tokens[3]);
      tokens[3] = String.valueOf(gps.latitude);

    }
    if (tokens[4].contains("S")) { // if South then negative latitude
      gps.latitude = gps.latitude * -1;
      tokens[3] = String.valueOf(gps.latitude);
    }

    if (tokens[5].length() > 0) {
      gps.longitude = convertNMEAToDegrees(tokens[5]);
      tokens[5] = String.valueOf(gps.longitude);
    }
    if (tokens[6].contains("W")) {// if West then negative longitude
      gps.longitude = gps.longitude * -1;
      tokens[5] = "-" + String.valueOf(gps.longitude);
    }
    log.info("Longitude: " + String.valueOf(gps.longitude));
    log.info("East or West: " + tokens[6]);

    log.info("Speed (knots): " + tokens[7]);

    log.info("Course (deg): " + tokens[8]);

    log.info("Date (ddmmyy): " + tokens[9]);

    log.info("Magnetic Variation (deg): " + tokens[10]);

    log.info("Magnetic Variation Direction (E/W): " + tokens[11]);

    if (tokens.length == 14) {
      log.info("Position Mode Indicator: " + tokens[12]);
      log.info("Checksum: " + tokens[13]);
    } else {
      log.info("Checksum: " + tokens[12]);
    }
    return tokens; // This should return data to the python code if the user
    // has subscribed to it
  }// end dataToString

  public String[] publishVTGData() {

    GpsData gps = new GpsData();

    log.info("publishVTGData has been called");
    log.info("Full data String = " + messageString);

    String[] tokens = messageString.split("[,*]", -1);

    log.info("String type: " + tokens[0]);
    gps.type = tokens[0];

    log.info("Course (deg): " + tokens[1]);

    log.info("Reference (True): " + tokens[2]);

    log.info("Course (deg): " + tokens[3]);

    log.info("Reference (Magnetic): " + tokens[4]);

    log.info("Speed (knots): " + tokens[5]);

    log.info("Units (knots): " + tokens[6]);

    log.info("Speed (km/hr): " + tokens[7]);

    log.info("Units (km/hr): " + tokens[8]);

    if (tokens.length == 11) {
      log.info("Mode ('A' = Autonomous, 'D' = DGPS, 'E' = DR): " + tokens[9]);
      log.info("Checksum: " + tokens[10]);
    } else {
      log.info("Checksum: " + tokens[9]);
    }
    return tokens; // This should return data to the python code if the user
    // has subscribed to it
  }// end dataToString

  public String[] publishZDAData() {

    GpsData gps = new GpsData();

    log.info("publishZDAData has been called");
    log.info("Full data String = " + messageString);

    String[] tokens = messageString.split("[,*]", -1);

    log.info("String type: " + tokens[0]);
    gps.type = tokens[0];

    log.info("Time UTC (hhmmss.ss): " + tokens[1]);
    gps.time = tokens[1];

    log.info("Day: " + tokens[2]);

    log.info("Month: " + tokens[3]);

    log.info("Year: " + tokens[4]);

    log.info("Local TZ hours: " + tokens[5]);

    log.info("Local TZ minutes: " + tokens[6]);

    log.info("Checksum: " + tokens[7]);

    return tokens; // This should return data to the python code if the user
    // has subscribed to it
  }// end dataToString

  public void setBaud(int baudRate) throws IOException {
    buffer.reset();
    if (baudRate == 9600) {
    } else if (baudRate == 19200) {
    } else if (baudRate == 38400) {
    } else {
      log.error("You've specified an unsupported baud rate");
    }
  }

  public void setMode() {
    log.error("SetMode is Not Yet Implemented");

  }// end of setMode

  public void setModel(String m) {
    model = m;
  }

  /*********************************************************
   * Here's all the GeoFence methods you might want to call from outside.
   *********************************************************/
  // This is how you create a Point
  /**
   * 
   * @param lat latitude
   * @param lon longitude
   * @return the point
   */
  public Point setPoint(double lat, double lon) {
    Point point = new Point(lat, lon);
    return point;
  }

  // This is how you set your circular GeoFence around a point
  public Circle setPointGeoFence(double lat, double lon, int radius) {
    Circle pointFence = new Circle(lat, lon, radius);
    return pointFence;
  }

  // This is in case they want to use a Point object
  public Circle setPointGeoFence(Point point, int radius) {
    return setPointGeoFence(point.getLat(), point.getLon(), radius);
  }

  // This is how you create your polygon shaped GeoFence
  public Polygon setPolygonGeoFence(Point[] points) {
    Polygon polygon = new Polygon(points);
    return polygon;
  }

  void sortPointsByLat(List<Point> points) {
    Collections.sort(points, new Comparator<Point>() {
      @Override
      public int compare(Point p1, Point p2) {
        return Double.compare(p1.getLat(), p2.getLat());
      }
    });
  }

  @Override
  public void startService() {
    super.startService();
    try {
      serial = (Serial) startPeer("serial", "Serial");
      serial.addByteListener(this);

      if (model == null) {
        model = MODEL;
      }

    } catch (Exception e) {
      error(e.getMessage());
    }
  }

  public void write(byte[] command) {
    // iterate through the byte array sending each one to the serial port.
    for (int i = 0; i < command.length; i++) {
      try {
        serial.write(command[i]);
      } catch (Exception e) {
        Logging.logError(e);
      }

    }
  }

  @Override
  public void onConnect(String portName) {
    info("%s connected to %s", getName(), portName);
  }

  @Override
  public void onDisconnect(String portName) {
    info("%s disconnected from %s", getName(), portName);
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Gps.class.getCanonicalName());
    meta.addDescription("parses NMEA sentences coming in over a Serial service");
    meta.addCategory("location", "sensor");
    meta.addPeer("serial", "Serial", "serial port for GPS");
    meta.setLicenseApache();

    return meta;
  }

}

package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class GPS extends Service {

    private static final long serialVersionUID = 1L;
    public final static Logger log = LoggerFactory.getLogger(GPS.class.getCanonicalName());
    public static final String MODEL = "FV_M8";
    
    public static final String GEOID_SEPARATION_KEY = "GEOID_SEPARATION_KEY";
    
    public ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    String model;
    
    // peers
    public transient Serial serial;

    // publish gps data begin ---
    public static class GPSData {
    	public String type; // msg type
    	public Double latitude;
    	public Double longitude;
    	String time;
    	
    	HashMap<String, String> addInfo = new HashMap<String,String>();
    }
    
    public void addGPSListener(Service service) {
		addListener("publishGPS", service.getName(), "onGPS", Long.class);
	}
	
	public void onGPS(GPSData gps) {
		log.info(String.format("lat: %f", gps.latitude));
		log.info(String.format("long: %f", gps.longitude));
	}
	
    public final GPSData publishGPS(final GPSData gps){
    	return gps;
    }
    // publish gps data end ---
    

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("serial", "Serial", "serial port for GPS");
		return peers;
	}

    public GPS(String n) {
		super(n);
    }

    @Override
    public String getDescription() {
        return "The GPS service";
    }

    @Override
    public void startService() {
        super.startService();

        try {
            serial = getSerial();
            // setting callback / message route
            serial.addListener("publishByte", getName(), "byteReceived");
            
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
            } catch (IOException e) {
                Logging.logException(e);
            }

        }
    }
    String messageString;

    public void byteReceived(Integer b) {

        try {
//            log.info("byteReceived Index = " + index + " actual data byte = " + String.format("%02x", b));
            buffer.write(b);
            // so a byte was appended
            // now depending on what model it was and
            // what stage of initialization we do that funky stuff
            if (b == 0x0a) { // GPS strings end with /CR /LF  = 0x0d 0x0a

//                log.info("Buffer size = " + buffer.size() + " Buffer = " + buffer.toString());
                buffer.flush();   //flush entire buffer so I can convert it to a byte array
                //message = buffer.toByteArray();
                messageString = new String(buffer.toByteArray(), ("UTF-8"));
//                log.info("size of message = " + message.length);

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
                } else if (messageString.contains("POLYN")) //San Jose navigation FV-M8 specific?
                {
                   log.info("POLYN string detected");
//                invoke("publishPOLYNData");
                }else if (messageString.contains("PMTK101")) 
                {
                   log.info("Hot Restart string detected");
//                invoke("publishMTKData");
                } else if (messageString.contains("PMTK010, 001")) 
                {
                   log.info("Startup string detected");
//                invoke("publishMTKData");
                } 
                else {
                   log.info("unknown string detected");
                }
                buffer.reset();
            }

        } catch (Exception e) {
            error(e.getMessage());
        }

    }
    
/**
 * The FV-M8 module skips one of the latter elements in the string, 
 * leaving only 0-14 elements.
 * GGA Global Positioning System Fixed Data
 * GLL Geographic Position - Latitude/Longitude
 * GSV GNSS Satellites in View
 * RMC Recommended Minimum Specific GNSS Data
 * VTG Course Over Ground and Ground Speed
 * GSA GNSS DOP and Active Satellites
 * MSS MSK Receiver Signal
 * kmc - so the data you have doesn't have two (GLL and MSK)
 * @return 
 */
    public String[] publishGGAData() {

    	GPSData gps = new GPSData();

       log.info("publishGGAData has been called");
       log.info("Full data String = " + messageString);
       
       String[] tokens = messageString.split("[,*]",-1);
       try {
       log.info("String type: "+tokens[0]);
       gps.type = tokens[0];
       
       log.info("Time hhmmss.ss: "+tokens[1]);
	       gps.time = tokens[1];
              
       log.info("Latitude: "+tokens[2]);
       log.info("North or South: "+tokens[3]);
       if (tokens[2].length() > 0){
    	   gps.latitude = convertNMEAToDegrees(tokens[2]);
           tokens[2]= String.valueOf(gps.latitude);

       }
       if(tokens[3].contains("S")){ //if South then negative latitude
           gps.latitude = gps.latitude * -1;
           tokens[2]= String.valueOf(gps.latitude);
       }
       
       if(tokens[4].length() > 0){
    	   gps.longitude =convertNMEAToDegrees(tokens[4]);
           tokens[4]= String.valueOf(gps.longitude);
       }
       if(tokens[5].contains("W")) {//if West then negative longitude
           gps.longitude = gps.longitude * -1;
           tokens[4]= "-"+String.valueOf(gps.longitude);
       }
       log.info("Longitude: "+String.valueOf(gps.longitude));
       log.info("East or West: "+tokens[5]);

       log.info("GPS quality ('0' = no fix, '1' = GPS SPS fix valid, '2' = DGPS, SPS fix valid, '6' = Dead Reckoning fix valid, '8' = simulated): "+tokens[6]);
       
       log.info("# of Satellites: "+tokens[7]);
       
       log.info("Horiz Dilution: "+tokens[8]);
       
       log.info("Altitude (meters above mean sealevel): "+tokens[9]);
       log.info("meters?: "+tokens[10]);
       
       log.info("Geoid Separation: (Geoid-to-ellipsoid separation. Ellipsoid altitude = MSL Altitude + Geoid Separation.) "+tokens[11]);
       log.info("meters?: "+tokens[12]); 
       
	       gps.addInfo.put(GEOID_SEPARATION_KEY, tokens[12]);
	       
       log.info("Seconds since last update (likely blank): "+tokens[13]);
       
       if (tokens.length == 16) {
            log.info("DGPS reference station ID (likely blank): "+tokens[14]);
            log.info("Checksum: "+tokens[15]);
       }else{
            log.info("Checksum: "+tokens[14]);
       }
	       invoke("publishGPS", gps);
    	} catch(Exception e){
    		Logging.logException(e);
    	}
       return tokens;  //This should return data to the python code if the user has subscribed to it
    }//end dataToString
    
    public String[] publishGLLData() {

    	GPSData gps = new GPSData();

       log.info("publishGLLData has been called");
       log.info("Full data String = " + messageString);


       String[] tokens = messageString.split("[,*]",-1);

       log.info("String type: "+tokens[0]);
       gps.type = tokens[0];
       
       if (tokens[1].length() > 0){
    	   gps.latitude = convertNMEAToDegrees(tokens[1]);
           tokens[1]= String.valueOf(gps.latitude);

       }
       if(tokens[2].contains("S")){ //if South then negative latitude
           gps.latitude = gps.latitude * -1;
           tokens[1]= String.valueOf(gps.latitude);
       }
       
       if(tokens[3].length() > 0){
    	   gps.longitude =convertNMEAToDegrees(tokens[3]);
           tokens[3]= String.valueOf(gps.longitude);
       }
       if(tokens[4].contains("W")) {//if West then negative longitude
           gps.longitude = gps.longitude * -1;
           tokens[3]= "-"+String.valueOf(gps.longitude);
       }
       log.info("Longitude: "+String.valueOf(gps.longitude));

       log.info("Time hhmmss.ss: "+tokens[5]);
       gps.time = tokens[5];
       
       log.info("Status: ('A' = valid, 'V' = not valid): "+tokens[6]);

       if (tokens.length == 9) {
            log.info("Mode: ('A'=Autonomous, 'D'=DGPS, 'E'=DR (Only present in NMEA v3.00)) "+tokens[7]);
            log.info("Checksum: "+tokens[8]);
       }else{
            log.info("Checksum: "+tokens[7]);
       }
       return tokens;  //This should return data to the python code if the user has subscribed to it
    }//end dataToString

    public String[] publishGSAData() {
    
    	GPSData gps = new GPSData();

       log.info("publishGSAData has been called");
       log.info("Full data String = " + messageString);

       String[] tokens = messageString.split("[,*]",-1);

       log.info("String type: "+tokens[0]);
       gps.type = tokens[0];
       
       log.info("Mode 1: ('M' = Manually forced into 2D or 3D, 'A' = Automatically allowed to switch between 2D/3D) "+tokens[1]);

       log.info("Mode 2: ('1' = no fix, '2' = 2D, '3' = 3D) "+tokens[2]);

       for (int x = 1; x <13; x++) {
           log.info("Channel "+x+" (Satellite #): "+tokens[x+2]);
       }

       log.info("PDOP (positional dilution): "+tokens[15]);

       log.info("HDOP (horizontal dilution): "+tokens[16]);

       log.info("VDOP (vertical dilution): "+tokens[17]);

       log.info("Checksum: "+tokens[18]);
       return tokens;  //This should return data to the python code if the user has subscribed to it
    }//end dataToString

    public String[] publishGSVData() {

    	GPSData gps = new GPSData();

       log.info("publishGSVData has been called");
       log.info("Full data String = " + messageString);

       String[] tokens = messageString.split("[,*]",-1);
       int last = tokens.length - 1;

       log.info("String type: "+tokens[0]);
       gps.type = tokens[0];
       
       log.info("Num. GSV messages: "+tokens[1]);

       log.info("Message number: "+tokens[2]);

       log.info("Satellites in view: "+tokens[3]);

       int svBlocks = (tokens.length - 5) / 4; // each GSV string can have 1-4 SV blocks and each has 4 tokens
       for (int x = 0; x < svBlocks; x++) {
           log.info("Satellite ID: "+tokens[4+x*4]);
           log.info("Elevation (0-90 degrees): "+tokens[5+x*4]);
           log.info("Azimuth (0-359 degrees): "+tokens[6+x*4]);
           log.info("Signal Strength (dBHz): "+tokens[7+x*4]);
       }
                
       log.info("Checksum: "+tokens[last]);
       return tokens;  //This should return data to the python code if the user has subscribed to it
    }//end dataToString

    public String[] publishRMCData() {

    	GPSData gps = new GPSData();

       log.info("publishRMCData has been called");
       log.info("Full data String = " + messageString);

       String[] tokens = messageString.split("[,*]",-1);

       log.info("String type: "+tokens[0]);
       gps.type = tokens[0];
       
       log.info("Time (hhmmss.ss): "+tokens[1]);
       gps.time = tokens[1];
       
       log.info("Status ('V' = warning, 'A' = Valid): "+tokens[2]);

       log.info("Latitude: "+tokens[3]);
       log.info("North or South: "+tokens[4]);
       if (tokens[3].length() > 0){
    	   gps.latitude = convertNMEAToDegrees(tokens[3]);
           tokens[3]= String.valueOf(gps.latitude);

       }
       if(tokens[4].contains("S")){ //if South then negative latitude
           gps.latitude = gps.latitude * -1;
           tokens[3]= String.valueOf(gps.latitude);
       }
       
       if(tokens[5].length() > 0){
    	   gps.longitude =convertNMEAToDegrees(tokens[5]);
           tokens[5]= String.valueOf(gps.longitude);
       }
       if(tokens[6].contains("W")) {//if West then negative longitude
           gps.longitude = gps.longitude * -1;
           tokens[5]= "-"+String.valueOf(gps.longitude);
       }
       log.info("Longitude: "+String.valueOf(gps.longitude));
       log.info("East or West: "+tokens[6]);

       log.info("Speed (knots): "+tokens[7]);

       log.info("Course (deg): "+tokens[8]);

       log.info("Date (ddmmyy): "+tokens[9]);

       log.info("Magnetic Variation (deg): "+tokens[10]);

       log.info("Magnetic Variation Direction (E/W): "+tokens[11]);

       if (tokens.length == 14) { 
          log.info("Position Mode Indicator: "+tokens[12]);
          log.info("Checksum: "+tokens[13]);
       }else{
          log.info("Checksum: "+tokens[12]);
       }
       return tokens;  //This should return data to the python code if the user has subscribed to it
    }//end dataToString

    public String[] publishVTGData() {

    	GPSData gps = new GPSData();
    	
       log.info("publishVTGData has been called");
       log.info("Full data String = " + messageString);

       String[] tokens = messageString.split("[,*]",-1);

       log.info("String type: "+tokens[0]);
       gps.type = tokens[0];
       
       log.info("Course (deg): "+tokens[1]);

       log.info("Reference (True): "+tokens[2]);

       log.info("Course (deg): "+tokens[3]);

       log.info("Reference (Magnetic): "+tokens[4]);

       log.info("Speed (knots): "+tokens[5]);

       log.info("Units (knots): "+tokens[6]);

       log.info("Speed (km/hr): "+tokens[7]);

       log.info("Units (km/hr): "+tokens[8]);

       if (tokens.length == 11) {
          log.info("Mode ('A' = Autonomous, 'D' = DGPS, 'E' = DR): "+tokens[9]);
          log.info("Checksum: "+tokens[10]);
       }else{
          log.info("Checksum: "+tokens[9]);
       }
       return tokens;  //This should return data to the python code if the user has subscribed to it
    }//end dataToString

    public String[] publishZDAData() {

    	GPSData gps = new GPSData();
    	
       log.info("publishZDAData has been called");
       log.info("Full data String = " + messageString);

       String[] tokens = messageString.split("[,*]",-1);

       log.info("String type: "+tokens[0]);
       gps.type = tokens[0];
       
       log.info("Time UTC (hhmmss.ss): "+tokens[1]);
       gps.time = tokens[1];
       
       log.info("Day: "+tokens[2]);

       log.info("Month: "+tokens[3]);

       log.info("Year: "+tokens[4]);

       log.info("Local TZ hours: "+tokens[5]);

       log.info("Local TZ minutes: "+tokens[6]);

       log.info("Checksum: "+tokens[7]);

       return tokens;  //This should return data to the python code if the user has subscribed to it
    }//end dataToString


    public String[] publishMSSData() {

       log.info("publishMSSData has been called");
       log.info("Full data String = " + messageString);

       String[] tokens = messageString.split("[,*]",-1);

       log.info("String type: "+tokens[0]);

       log.info("Signal Strength (dB): "+tokens[1]);

       log.info("Signal to Noise Ratio (dB): "+tokens[2]);

       log.info("Beacon Freq (kHz): "+tokens[3]);

       log.info("Beacon bitrate (bps): "+tokens[4]);
       
       if (tokens.length == 7) {
          log.info("Channel Num: "+tokens[5]);
          log.info("Checksum: "+tokens[6]);
       }else{
           log.info("Checksum: "+tokens[5]);
       }
       return tokens;  //This should return data to the python code if the user has subscribed to it
    }//end dataToString

    /***********************************************************************************
     * This block of methods will be used to GeoFencing
     * This code is based on the examples on the following blog
     * http://stefanbangels.blogspot.be/2012/12/for-several-years-now-i-have-been.html
     * *********************************************************************************/
    // We need a circle object to build a point/radius geofence
    class Circle {

        private double x;
        private double y;
        private int radius;

        public Circle(double x, double y, int radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }

        public double getLat() {
            return x;
        }

        public double getLon() {
            return y;
        }

        public int getRadius() {
            return radius;
        }
        
        public int setRadius(int m) {
        	radius = m;
        	return radius;
        }
            
    }
    
    //When your radius is defined in meters, you will need the Haversine formula.  
    //This formula will calculate the distance between two points (in meters) 
    //while taking into account the earth curvation:

    public double calculateDistance(double longitude1, double latitude1, double longitude2, double latitude2) {
    	double c = 
    	Math.sin(Math.toRadians(latitude1)) *
    	Math.sin(Math.toRadians(latitude2)) +
    	Math.cos(Math.toRadians(latitude1)) *
    	Math.cos(Math.toRadians(latitude2)) *
    	Math.cos(Math.toRadians(longitude2) - 
    		Math.toRadians(longitude1));
    	c = c > 0 ? Math.min(1, c) : Math.max(-1, c);
        return 3959 * 1.609 * 1000 * Math.acos(c);
    }
    	
    // Test if Lat(x) and Long(y) are inside your geofence.
    public boolean checkInside(Circle circle, double x, double y) {
        return calculateDistance(
            circle.getLat(), circle.getLon(), x, y
        ) < circle.getRadius();
   	}
    
    public Circle setPointGeoFence (double lat, double lon, int radius) {
    	Circle pointFence = new Circle(lat, lon, radius);
    	return pointFence;
    }

    /***********************************************************************************
     * This ends the GeoFence block
     * *********************************************************************************/
    // NMEA Lat/Lon values are ddmm.mmmm or dddmm.mmmm respectively and need to be converted
    public double convertNMEAToDegrees(String nmea) {
    	String degrees;
    	String minutes;
    	// If we have 5 leading digits it's a Longitude
    	if (nmea.matches("\\d\\d\\d\\d\\d\\.\\d\\d\\d\\d")) {
    		degrees = nmea.substring(0, 3);
    		minutes = nmea.substring(3);
    	}else{ // It's a Latitude
    		degrees = nmea.substring(0, 2);
    		minutes = nmea.substring(2);
    	}
    	double result = Double.parseDouble(degrees) + Double.parseDouble(minutes)/60;
    	return result;
    }
    
    public boolean connect(String port, int baud) {
        serial = getSerial();
        return serial.connect(port, baud, 8, 1, 0);
    }


    public boolean connect(String port) {
        serial = getSerial();
        return serial.connect(port, 38400, 8, 1, 0);
//        serial.publishType(PUBLISH_STRING); // GPS units publish strings
    }

    public boolean disconnect() {
        serial = getSerial();
        serial.disconnect();
        return serial.isConnected();
    }

    public void setBaud(int baudRate) throws IOException {
        buffer.reset();
        if (baudRate == 9600) {
        } else if (baudRate == 19200) {
        } else if (baudRate == 38400) {
        } else {
            log.error("You've specified an unsupported baud rate");
        }
    }

    public Serial getSerial() {
        serial = (Serial)startPeer("serial");
        return serial;
    }

    public void setModel(String m) {
        model = m;
    }

    public void setMode() {
        log.error("SetMode is Not Yet Implemented");

    }// end of setMode

    public static void main(String[] args) {
        LoggingFactory.getInstance().configure();
        LoggingFactory.getInstance().setLevel(Level.INFO);

        try {

            GPS template = new GPS("gps1");
            template.startService();

            Python python = new Python("python");
            python.startService();

            Runtime.createAndStart("gui", "GUIService");
            /*
             * GUIService gui = new GUIService("gui"); gui.startService();
             * 
             */

        } catch (Exception e) {
            Logging.logException(e);
        }
    }
}


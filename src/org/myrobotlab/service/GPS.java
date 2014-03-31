package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

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
    public String serialName;
    public transient Serial serial;
    public ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    String model;
    // states
//    public static final String STATE_PRE_INITIALIZATION = "state pre initialization";
//    public static final String STATE_INITIALIZATION_STAGE_1 = "state initialization stage 1";
//    public static final String STATE_INITIALIZATION_STAGE_2 = "state initialization stage 2";
//    public static final String STATE_INITIALIZATION_STAGE_3 = "state initialization stage 3";
//    public static final String STATE_INITIALIZATION_STAGE_4 = "state initialization stage 4";
//    public static final String STATE_SINGLE_SCAN = "taking a single scan";
//    public static final String STATE_MODE_CHANGE = "changing mode";
//    public static final String STATE_NOMINAL = "waiting on user to tell me what to do";
    // public int dataMessageSize = 213; 
//    String state = STATE_SINGLE_SCAN;//STATE_PRE_INITIALIZATION;
    int index = 0;
    private int GPSbaudRate = 38400; //by default
    private String serialPort;
    private byte[] message;
    private boolean dataAvailable = false;
    public static final int PUBLISH_STRING = 4;

    public GPS(String n) {
		super(n);
        reserve(String.format("%s_serial", n), "Serial", "serial port for GPS");
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
            serial.startService();
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

    public void byteReceived(Byte b) {

        try {
            index++;
//            log.info("byteReceived Index = " + index + " actual data byte = " + String.format("%02x", b));
            buffer.write(b);
            // so a byte was appended
            // now depending on what model it was and
            // what stage of initialization we do that funky stuff
            if (b == 0x0a) { // GPS strings end with /CR /LF  = 0x0d 0x0a

//                log.info("Buffer size = " + buffer.size() + " Buffer = " + buffer.toString());
                buffer.flush();   //flush entire buffer so I can convert it to a byte array
                message = buffer.toByteArray();
                messageString = new String(buffer.toByteArray(), ("UTF-8"));
//                log.info("size of message = " + message.length);
                dataAvailable = true;

                if (messageString.contains("GGA")) {
                   log.info("GGA string detected");
                    invoke("publishGGAData");
                } else if (messageString.contains("RMC")) {
                   log.info("RMC string detected");
//                invoke("publishRMCData");
                } else if (messageString.contains("VTG")) {
                   log.info("VTG string detected");
//                invoke("publishVTGData");
                } else if (messageString.contains("GSA")) {
                   log.info("GSA string detected");
//                invoke("publishGSAData");
                } else if (messageString.contains("GSV")) {
                   log.info("GSV string detected");
//                invoke("publishGSVData");
                } else if (messageString.contains("GLL")) {
                   log.info("GLL string detected");
//                invoke("publishGLLData");
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
                index = 0;
            }

        } catch (Exception e) {
            error(e.getMessage());
        }

    }
/**
 * The FV-M8 module skips one of the latter elements in the string, 
 * leaving only 0-14 elements.
 * @return 
 */
    public String[] publishGGAData() {


       log.info("publishGGAData has been called");
       log.info("Full data String = " + messageString);
       
       
       String[] tokens = messageString.split(",");
       
       log.info("String type: "+tokens[0]);
       
       log.info("Time hhmmss.ss: "+tokens[1]);
       
      
       if(tokens[3].contains("S")) //if negative latitude, prepend a - sign
           tokens[2]= "-"+tokens[2];
       
       log.info("Latitude llll.llll "+tokens[2]);
       log.info("North or South: "+tokens[3]);
       
       
       if(tokens[5].contains("W")) //if negative longitude, prepend a - sign
           tokens[4]= "-"+tokens[4];
       log.info("Longitude llll.llll "+tokens[4]);
       log.info("East or West: "+tokens[5]);

       log.info("GPS quality ('8' = simulated): "+tokens[6]);

       
       log.info("# of Satellites: "+tokens[7]);
       
       log.info("Horiz Dilution: "+tokens[8]);
       
       log.info("Altitude (meters above mean sealevel): "+tokens[9]);
       log.info("meters?: "+tokens[10]);
       
       
       log.info("Height of geoid: "+tokens[11]);
       log.info("meters?: "+tokens[12]); 
       
       log.info("Seconds since last update (likely blank): "+tokens[13]);
       
//       log.info("DGPS reference (likely blank): "+tokens[14]);

       log.info("Checksum: "+tokens[14]);
       invoke ("setValues");
       return tokens;  //This should return data to the python code if the user has subscribed to it
    }//end dataToString

    

    
    
    public boolean connect(String port, int baud) {
        serial = getSerial();
        serialPort = port;
        GPSbaudRate = baud;
        boolean connect = serial.connect(port, baud, 8, 1, 0);

        return serial.isConnected();
    }

    public boolean connect(String port) {
        serial = getSerial();
        serialPort = port;
        boolean connect = serial.connect(port, GPSbaudRate, 8, 1, 0);
//        serial.publishType(PUBLISH_STRING); // GPS units publish strings
        return serial.isConnected();
    }

    public boolean disconnect() {
        serial = getSerial();
        serial.disconnect();
        return serial.isConnected();
    }

    public void setBaud(int baudRate) throws IOException {

        GPSbaudRate = baudRate;

        index = 0;
        buffer.reset();

        if (baudRate == 9600) {
        } else if (baudRate == 19200) {
        } else if (baudRate == 38400) {
        } else {
            log.error("You've specified an unsupported baud rate");
        }
    }

    public Serial getSerial() {
        if (serialName == null) {
            serialName = String.format("%s_serial", getName());
        }
        serial = (Serial) Runtime.create(serialName, "Serial");
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

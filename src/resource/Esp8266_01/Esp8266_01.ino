/*
 *  This sketch demonstrates how to get the parameters from a http POST request
    https://www.youtube.com/watch?v=Edbxyl2BhyU
    http://www.instructables.com/id/How-to-use-the-ESP8266-01-pins/step3/Best-Trick-Use-I2C/
    It also contains code to write and read data from an i2c device
    To be able to compile this sketch you need to install the esp8266 board.
    First add the path to where the esp8266 board can be downloaded
      In Arduin IDE select File => Preferences => Additional Boards Manager URL:s 
      Add http://arduino.esp8266.com/stable/package_esp8266com_index.json
      If you have other paths there, separate them with a ,  
      In Ardino IDE select Tools => Board: => Boards Manager ... >    
      Then search for esp8266 and install it
    You also need to install the libraries used
      In Arduino IDE select Sketch => Include Library => Manage Libraries =>  
      Install ESP8266WiFi, ESP8266WebServer, ESP8266mDSN and ArduinoJson libraries
 
      You also need to connect the ESP8266 to your USB port, using either a development board or a FTDI cable.
      To set the ESP8266 in flash mode, so that you can upload the program, GPIO0 needs to be connected to GND 

      It's possible that this sketch can be used with other ESP8266 variants, but that needs to be tested

      Change ssid and password to match the network you want to connect to.
      After upload is complete you can open the serial monitor to see what ip-address that has been assigned.
      If you don't see it, try to unplug the USB and the reopen the serial monitor
      The you can connect using http://<esp8266-ip-address>
      
      This sketch also supports multicast DNS: 
      https://github.com/esp8266/Arduino/tree/master/libraries/ESP8266mDNS
      It already supported on Mac
      On Windows you need to install "Bonjour". https://support.apple.com/kb/DL999?viewlocale=en_US&locale=en_US
      On Linux install "Avahli" http://avahi.org/ 
      
      Then you can connect using this link:
      http://esp8266-01.local
      
      It will respond with a welcome message and show what ip-address that is being used.
      If you want to change the name it's set in variable  
 */

#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <ESP8266mDNS.h>
#include <ArduinoJson.h>
#include<Wire.h>
#include<stdlib.h>

ESP8266WebServer server;
MDNSResponder mdns;

const char* ssid = "<your-wifi-ssid>";
const char* password = "<your-wifi-password>";
const char* hostName = "esp8266-01";
IPAddress ipaddress;

const String BusLabel = "bus:";
const String DeviceLabel = "device:";
const String SizeLabel = "size:";
const String BufferLabel = "buffer:"; 

bool i2cInitiated = false;
  
int16_t bus;
int16_t device;
int16_t size; 
String i2cBuffer;  
String readbuffer;

void setup() {
  Serial.begin(115200);
  delay(10);
  
  // Connect to WiFi network
  Serial.println();
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);
  
  WiFi.begin(ssid, password);
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.println("WiFi connected");
  
  // Print the IP address
  ipaddress = WiFi.localIP();
  Serial.println(ipaddress);
  
  // 
  if (mdns.begin(hostName,ipaddress)) {
    Serial.println("MDNS Responder Started.");
  }
  
  // Setup the different routes to response methods
  server.on("/",rootResponse);
  server.on("/i2cWrite",i2cWrite);
  server.on("/i2cRead",i2cRead);
  
  // Start the server
  server.begin();
  Serial.println("Server started");

  MDNS.addService("http","tcp",80);
}

void loop() {

  server.handleClient();
}

/*
  Response when accessing the esp8266-01 root
*/ 
void rootResponse(){
    
  String msg = "Welcome to <b>ESP8266</b> and the <b>i2c interface</b><p><b>Hostname: </b>";
  msg = msg + String(hostName);
  msg = msg + "<p><b>Ip-address: </b>";
  msg = msg +  DisplayAddress(ipaddress);
  server.send(200, "text/html", msg); 
} 

String DisplayAddress(IPAddress address)
{
 return String(address[0]) + "." + 
        String(address[1]) + "." + 
        String(address[2]) + "." + 
        String(address[3]);
}
/*
 * Read from an i2c device
 */
void i2cRead(){

  /*
  Serial.println("");
  Serial.println("i2cRead");
  Serial.println("");
  
  String readbuffer = "";
  */
  
  parseParameters();
  if (!i2cInitiated) i2cBegin(bus);

  // Serial.println("Reading start");
   
  int answer = Wire.requestFrom(device,size);  
  for (int i=0; i<answer ; i++){
    int byte = Wire.read();
    if (byte < 15) {
      readbuffer = readbuffer  + "0" + String(byte, HEX);
    }
    else {
      readbuffer = readbuffer  + String(byte,HEX);
    }
    /* 
    Serial.println("--");
    Serial.println(String(i));
    Serial.println(readbuffer); 
    */  
  }
  

  // Serial.println("Reading end");


  String message = "{" +
                   BusLabel + String(bus) + "," +
                   DeviceLabel +String(device) + "," +
                   SizeLabel + String(answer) + "," +
                   BufferLabel + readbuffer + 
                   "}";
                                 
  server.send(200, "application/json", message);
  // server.send(200, "text/plain", "i2cRead Bus = " + String(bus) + " Device = " + String(device) + " Answer = " + String(answer) + " Buffer = " + readbuffer + " End");  
}

/*
 * Write to the i2c device
 */
void i2cWrite(){

  Serial.println("");
  Serial.println("i2cWrite");
  Serial.println("");
  
  parseParameters();
  if (!i2cInitiated) i2cBegin(bus);

  /*
  Wire.beginTransmission(device); 
  for (int i=0; i<size; i++){
    int Pos1 = i*2;
    int Pos2 = Pos1 + 1;
    int Pos3 = Pos1 + 2;
    String byte1 = i2cBuffer.substring(Pos1,Pos2);
    String byte2 = i2cBuffer.substring(Pos2,Pos3);
    String byteString  = byte1 + byte2;
    char xx[3];
    byteString.toCharArray(xx,3);
    char high_nibble = h2d(xx[0]);
    char low_nibble  = h2d(xx[1]);
    int w = (high_nibble << 4) | low_nibble;
    Wire.write(w);
    Serial.println("write " + byte1 + byte2 + " " + String(w));
  }
  Wire.endTransmission();  
  */
  
  char writeBuffer[200];
  i2cBuffer.toCharArray(writeBuffer,(size*2)+1);
      
  Wire.beginTransmission(device); 
  for (int i=0; i<size; i++){
    char high_nibble = h2d(writeBuffer[i*2]);
    char low_nibble  = h2d(writeBuffer[(i*2)+1]);
    int w = (high_nibble << 4) | low_nibble;
    Wire.write(w);
    // Serial.println("write " + String(writeBuffer[i*2]) + String(writeBuffer[(i*2)+1]) + " " + String(w));
  }
  Wire.endTransmission();  
  
  server.send(204, "");   
}

/*
 * Convert a byte from hex to decimal
 */
unsigned char h2d(unsigned char hex)
{
        if(hex > 0x39) hex -= 7; // adjust for hex letters upper or lower case
        return(hex & 0x0f);      // and mask to get a value 0-15
}

/*
 * Parse the incoming json data into Strings
 */
void parseParameters()
{
   bus = 0;
   device = 0;
   size = 0;
   
   String data = server.arg("plain");
   StaticJsonBuffer<200> jBuffer;    
   JsonObject& jObject = jBuffer.parseObject(data);

   String busString  = jObject["bus"];  
   String deviceString  = jObject["device"];  
   String sizeString   = jObject["size"];  
   String i2cBufferString = jObject["buffer"];  

   /*
   Serial.println("Parameters ");
   Serial.println(busString);
   Serial.println(deviceString);
   Serial.println(sizeString);
   Serial.println(i2cBufferString);
   */ 
   bus = busString.toInt();
   device = deviceString.toInt();
   size = sizeString.toInt();
   i2cBuffer = i2cBufferString;
}

/*
 * Select what pins to use for the i2c communication
 * bus = 0   selects GPIO0 (SDA) , GPIO2 ( SCL) 
 * any other selects    TX (SDA ), RX    ( SCL ) 
 * http://www.instructables.com/id/How-to-use-the-ESP8266-01-pins/step3/Best-Trick-Use-I2C/
 */
void i2cBegin(int16_t bus){
  
  if (bus == 0) {
    Wire.begin(0,2);
  }
  else {
    Wire.begin(1,3);
  }
  i2cInitiated = true;
}


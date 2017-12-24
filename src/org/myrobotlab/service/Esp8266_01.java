package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.slf4j.Logger;

import com.google.gson.Gson;

/**
 * 
 * Esp8266_01 - This is the MyRobotLab Service for the ESP8266-01. The
 * ESP8266-01 is a small WiFi enabled device with limited number of i/o pins
 * This service makes it possible to use the ESP8266-01 and i2c devices
 * 
 */
// TODO Ensure that only one instance of RasPi can execute on each RaspBerry PI
public class Esp8266_01 extends Service implements I2CController {

  public static class I2CDeviceMap {
    public transient I2CControl control;
    public String serviceName;
    public String busAddress;
    public String deviceAddress;
  }

  class i2cParms {
    public String getBus() {
      return bus;
    }

    public void setBus(String bus) {
      this.bus = bus;
    }

    public String getDevice() {
      return device;
    }

    public void setDevice(String device) {
      this.device = device;
    }

    public String getWriteSize() {
      return writeSize;
    }

    public void setWriteSize(String writeSize) {
      this.writeSize = writeSize;
    }

    public String getReadSize() {
      return readSize;
    }

    public void setReadSize(String readSize) {
      this.readSize = readSize;
    }
    
    public String getBuffer() {
      return buffer;
    }

    public void setBuffer(String buffer) {
      this.buffer = buffer;
    }

    String bus;
    String device;
    String writeSize;
    String readSize;
    String buffer;
  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Esp8266_01.class);

  transient HttpClient httpclient;

  transient HashMap<String, I2CDeviceMap> i2cDevices = new HashMap<String, I2CDeviceMap>();

  transient HashMap<String, String> i2cWriteData = new HashMap<String, String>();

  String host = "192.168.1.99";

  public static void main(String[] args) {    
    LoggingFactory.init(Level.DEBUG);

  }

  public Esp8266_01(String n) {
    super(n);
    httpclient = HttpClientBuilder.create().build();

  }

  @Override
  public void i2cWrite(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {

    Gson gson = new Gson();

    String stringBuffer = javax.xml.bind.DatatypeConverter.printHexBinary(buffer);

    i2cParms senddata = new i2cParms();
    senddata.setBus(Integer.toString(busAddress));
    senddata.setDevice(Integer.toString(deviceAddress));
    senddata.setWriteSize(Integer.toString(size));
    senddata.setReadSize("0");
    senddata.setBuffer(stringBuffer);

    String method = "i2cWrite";
    String url = "http://" + host + "/" + method;

    // log.info(url);

    HttpPost post = new HttpPost(url);
    StringEntity postingString = null;
    try {
      postingString = new StringEntity(gson.toJson(senddata));
    } catch (UnsupportedEncodingException e) {
      Logging.logError(e);
    }

    // log.info(String.format("postingString: %s", postingString));
    post.setEntity(postingString);
    post.setHeader("Content-type", "application/json");
    HttpResponse response = null;

    try {
      response = httpclient.execute(post);
    } catch (ClientProtocolException e) {
      Logging.logError(e);
    } catch (IOException e) {
      Logging.logError(e);
    }

    int code = response.getStatusLine().getStatusCode();
    // log.info(response.toString());

    if (code == 200) {
      BufferedReader rd = null;
      try {
        rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
      } catch (UnsupportedOperationException e) {
        Logging.logError(e);
      } catch (IOException e) {
        Logging.logError(e);
      }

      StringBuffer result = new StringBuffer();
      String line = "";
      try {
        while ((line = rd.readLine()) != null) {
          result.append(line);
        }
      } catch (IOException e) {
        Logging.logError(e);
      }

      // log.info(result.toString());
      // JSONObject o = new JSONObject(result.toString());
    }

  }

  @Override
  public int i2cRead(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {

    Gson gson = new Gson();

    i2cParms senddata = new i2cParms();
    senddata.setBus(Integer.toString(busAddress));
    senddata.setDevice(Integer.toString(deviceAddress));
    senddata.setWriteSize("0");
    senddata.setReadSize(Integer.toString(size));

    String method = "i2cRead";
    String url = "http://" + host + "/" + method;

    HttpPost post = new HttpPost(url);
    StringEntity postingString = null;

    try {
      postingString = new StringEntity(gson.toJson(senddata));
    } catch (UnsupportedEncodingException e) {
      Logging.logError(e);
    }

    post.setEntity(postingString);
    post.setHeader("Content-type", "application/json");
    HttpResponse response = null;

    try {
      response = httpclient.execute(post);
    } catch (ClientProtocolException e) {
      Logging.logError(e);
    } catch (IOException e) {
      Logging.logError(e);
    }

    int code = response.getStatusLine().getStatusCode();
    // log.info(response.toString());

    i2cParms returndata = null;
    if (code == 200) {
      BufferedReader rd = null;
      try {
        rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
      } catch (UnsupportedOperationException e) {
        Logging.logError(e);
      } catch (IOException e) {
        Logging.logError(e);
      }

      StringBuffer result = new StringBuffer();
      String line = "";
      try {
        while ((line = rd.readLine()) != null) {
          result.append(line);
        }
      } catch (IOException e) {
        Logging.logError(e);
      }

      // log.info(result.toString());

      Gson resultGson = new Gson();

      returndata = resultGson.fromJson(result.toString(), i2cParms.class);
      // log.info(resultGson.fromJson(result.toString(),
      // i2cParms.class).toString());

      log.info(String.format("bus %s, device %s, size %s, buffer %s",returndata.bus, returndata.device, returndata.readSize, returndata.buffer));
    }

    hexStringToArray(returndata.buffer, buffer);

    return size;
  }

  void hexStringToArray(String inBuffer, byte[] outArray) {

    // log.info(String.format("inBuffer %s",inBuffer));
    for (int i = 0; i < outArray.length; i++) {
      String hex = "0x" + inBuffer.substring((i * 2), (i * 2) + 2);
      outArray[i] = (byte) (int) Integer.decode(hex);
      // log.info(String.format("in %s, outArray %d",hex,outArray[i]));
    }

  }

  /*
  @Override
  public int i2cWriteRead(I2CControl control, int busAddress, int deviceAddress, byte[] writeBuffer, int writeSize, byte[] readBuffer, int readSize) {

    i2cWrite(control, busAddress, deviceAddress, writeBuffer, writeSize);
    return i2cRead(control, busAddress, deviceAddress, readBuffer, readSize);
  }
  */
  
  @Override
  public int i2cWriteRead(I2CControl control, int busAddress, int deviceAddress, byte[] writeBuffer, int writeSize, byte[] readBuffer, int readSize) {

    Gson gson = new Gson();

    String stringBuffer = javax.xml.bind.DatatypeConverter.printHexBinary(writeBuffer);

    i2cParms senddata = new i2cParms();
    senddata.setBus(Integer.toString(busAddress));
    senddata.setDevice(Integer.toString(deviceAddress));
    senddata.setWriteSize(Integer.toString(writeSize));
    senddata.setReadSize(Integer.toString(readSize));
    senddata.setBuffer(stringBuffer);

    String method = "i2cWriteRead";
    String url = "http://" + host + "/" + method;

    // log.info(url);

    HttpPost post = new HttpPost(url);
    StringEntity postingString = null;
    try {
      postingString = new StringEntity(gson.toJson(senddata));
    } catch (UnsupportedEncodingException e) {
      Logging.logError(e);
    }

    // log.info(String.format("postingString: %s", postingString));
    post.setEntity(postingString);
    post.setHeader("Content-type", "application/json");
    HttpResponse response = null;

    try {
      response = httpclient.execute(post);
    } catch (ClientProtocolException e) {
      Logging.logError(e);
    } catch (IOException e) {
      Logging.logError(e);
    }

    int code = response.getStatusLine().getStatusCode();
    // log.info(response.toString());

    i2cParms returndata = null;
    if (code == 200) {
      BufferedReader rd = null;
      try {
        rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
      } catch (UnsupportedOperationException e) {
        Logging.logError(e);
      } catch (IOException e) {
        Logging.logError(e);
      }

      StringBuffer result = new StringBuffer();
      String line = "";
      try {
        while ((line = rd.readLine()) != null) {
          result.append(line);
        }
      } catch (IOException e) {
        Logging.logError(e);
      }

      // log.info(result.toString());

      Gson resultGson = new Gson();

      returndata = resultGson.fromJson(result.toString(), i2cParms.class);
      // log.info(resultGson.fromJson(result.toString(),
      // i2cParms.class).toString());

      log.info(String.format("bus %s, device %s, readSize %s, buffer %s",returndata.bus, returndata.device, returndata.readSize, returndata.buffer));
    }
    
    hexStringToArray(returndata.buffer, readBuffer);

    return  Integer.decode(returndata.readSize);
  }

  public void setHost(String host) {
    this.host = host;
  }

  @Override
  public void attachI2CControl(I2CControl control) {
    // This part adds the service to the mapping between
    // busAddress||DeviceAddress
    // and the service name to be able to send data back to the invoker
    String key = String.format("%s.%s", control.getDeviceBus(), control.getDeviceAddress());
    I2CDeviceMap devicedata = new I2CDeviceMap();
    if (i2cDevices.containsKey(key)) {
      devicedata = i2cDevices.get(key);
      if (control.getName() != devicedata.serviceName){
        log.error(String.format("Attach of %s failed: %s already exists on bus %s address %s", control.getName(), devicedata.serviceName, control.getDeviceBus(), control.getDeviceAddress()));
      }
    } else {
      devicedata.serviceName = control.getName();
      devicedata.busAddress = control.getDeviceBus();
      devicedata.deviceAddress = control.getDeviceAddress();
      devicedata.control = control;
      i2cDevices.put(key, devicedata);
      control.attachI2CController(this);
    }
  }

  @Override
  public void detachI2CControl(I2CControl control) {
    // This method should delete the i2c device entry from the list of
    // I2CDevices
    // The order of the detach is important because the higher level service may
    // want to execute something that
    // needs this service to still be availabe
    String key = String.format("%s.%s", control.getDeviceBus(), control.getDeviceAddress());
    if (i2cDevices.containsKey(key)) {
      log.info("detach : "+control.getName());
      i2cDevices.remove(key);
      control.detachI2CController(this);
    }
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

    ServiceType meta = new ServiceType(Esp8266_01.class.getCanonicalName());
    meta.addDescription("ESP8266-01 service to communicate using WiFi and i2c");
    meta.addCategory("i2c", "control");
    meta.setSponsor("Mats");
    meta.addDependency("org.apache.commons.httpclient", "4.5.2");

    return meta;
  }

  @Override
  public Set<String> getAttached() {
    return i2cDevices.keySet();
  }

}

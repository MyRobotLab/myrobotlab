package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.Esp8266Config;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.slf4j.Logger;

/**
 * 
 * Esp8266_01 - This is the MyRobotLab Service for the ESP8266-01. The
 * ESP8266-01 is a small WiFi enabled device with limited number of i/o pins
 * This service makes it possible to use the ESP8266-01 and i2c devices
 * 
 */
// TODO Ensure that only one instance of RasPi can execute on each RaspBerry PI
public class Esp8266 extends Service<Esp8266Config> implements I2CController {

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

  public final static Logger log = LoggerFactory.getLogger(Esp8266.class);

  transient HttpClient httpclient;

  transient HashMap<String, I2CDeviceMap> i2cDevices = new HashMap<String, I2CDeviceMap>();

  transient HashMap<String, String> i2cWriteData = new HashMap<String, String>();

  public static void main(String[] args) {
    LoggingFactory.init(Level.DEBUG);
    
    try {

      Runtime.main(new String[] { "--log-level", "info", "-s", "webgui", "WebGui", "intro", "Intro", "python", "Python", "esp", "Esp8266" });

    } catch(Exception e) {
      e.printStackTrace();
    }

  }

  public Esp8266(String n, String id) {
    super(n, id);
    httpclient = HttpClientBuilder.create().build();

  }

  @Override
  public void i2cWrite(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {

    String stringBuffer = Hex.encodeHexString(buffer);
    i2cParms senddata = new i2cParms();
    senddata.setBus(Integer.toString(busAddress));
    senddata.setDevice(Integer.toString(deviceAddress));
    senddata.setWriteSize(Integer.toString(size));
    senddata.setReadSize("0");
    senddata.setBuffer(stringBuffer);

    String method = "i2cWrite";
    String url = "http://" + config.host + "/" + method;

    // log.info(url);

    HttpPost post = new HttpPost(url);
    StringEntity postingString = null;
    try {
      postingString = new StringEntity(CodecUtils.toJson(senddata));
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

    i2cParms senddata = new i2cParms();
    senddata.setBus(Integer.toString(busAddress));
    senddata.setDevice(Integer.toString(deviceAddress));
    senddata.setWriteSize("0");
    senddata.setReadSize(Integer.toString(size));

    String method = "i2cRead";
    String url = "http://" + config.host + "/" + method;

    HttpPost post = new HttpPost(url);
    StringEntity postingString = null;

    try {
      postingString = new StringEntity(CodecUtils.toJson(senddata));
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
      returndata = CodecUtils.fromJson(result.toString(), i2cParms.class);
      // log.info(CodecUtils.fromJson(result.toString(),
      // i2cParms.class).toString());
      log.info("bus {}, device {}, size {}, buffer {}", returndata.bus, returndata.device, returndata.readSize, returndata.buffer);
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
   * @Override public int i2cWriteRead(I2CControl control, int busAddress, int
   * deviceAddress, byte[] writeBuffer, int writeSize, byte[] readBuffer, int
   * readSize) {
   * 
   * i2cWrite(control, busAddress, deviceAddress, writeBuffer, writeSize);
   * return i2cRead(control, busAddress, deviceAddress, readBuffer, readSize); }
   */

  @Override
  public int i2cWriteRead(I2CControl control, int busAddress, int deviceAddress, byte[] writeBuffer, int writeSize, byte[] readBuffer, int readSize) {

    String stringBuffer = Hex.encodeHexString(writeBuffer);

    i2cParms senddata = new i2cParms();
    senddata.setBus(Integer.toString(busAddress));
    senddata.setDevice(Integer.toString(deviceAddress));
    senddata.setWriteSize(Integer.toString(writeSize));
    senddata.setReadSize(Integer.toString(readSize));
    senddata.setBuffer(stringBuffer);

    String method = "i2cWriteRead";
    String url = "http://" + config.host + "/" + method;

    // log.info(url);

    HttpPost post = new HttpPost(url);
    StringEntity postingString = null;
    try {
      postingString = new StringEntity(CodecUtils.toJson(senddata));
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

      returndata = CodecUtils.fromJson(result.toString(), i2cParms.class);
      // log.info(CodecUtils.fromJson(result.toString(),
      // i2cParms.class).toString());

      log.info("bus {}, device {}, readSize {}, buffer {}", returndata.bus, returndata.device, returndata.readSize, returndata.buffer);
    }

    hexStringToArray(returndata.buffer, readBuffer);

    return Integer.decode(returndata.readSize);
  }

  public void setHost(String host) {
    config.host = host;
  }

  @Override
  public void attachI2CControl(I2CControl control) {
    // This part adds the service to the mapping between
    // busAddress||DeviceAddress
    // and the service name to be able to send data back to the invoker
    String key = String.format("%s.%s", control.getBus(), control.getAddress());
    I2CDeviceMap devicedata = new I2CDeviceMap();
    if (i2cDevices.containsKey(key)) {
      devicedata = i2cDevices.get(key);
      if (control.getName() != devicedata.serviceName) {
        log.error("Attach of {} failed: {} already exists on bus %s address {}", control.getName(), devicedata.serviceName, control.getBus(), control.getAddress());
      }
    } else {
      devicedata.serviceName = control.getName();
      devicedata.busAddress = control.getBus();
      devicedata.deviceAddress = control.getAddress();
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
    String key = String.format("%s.%s", control.getBus(), control.getAddress());
    if (i2cDevices.containsKey(key)) {
      log.info("detach : " + control.getName());
      i2cDevices.remove(key);
      control.detachI2CController(this);
    }
  }

  @Override
  public Set<String> getAttached() {
    return i2cDevices.keySet();
  }

}

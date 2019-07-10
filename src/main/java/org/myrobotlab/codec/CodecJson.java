package org.myrobotlab.codec;

import java.io.IOException;
import java.io.OutputStream;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The base json serialization utility for MRL.  This uses GSON to serialize object to and from JSON.
 *
 */
public class CodecJson extends Codec {

  public final static Logger log = LoggerFactory.getLogger(CodecJson.class);

  private transient static Gson mapper = new GsonBuilder().create();// .setDateFormat("yyyy-MM-dd
  // HH:mm:ss.SSS").disableHtmlEscaping().create();
  private transient static Gson prettyMapper = new GsonBuilder().setPrettyPrinting().create();


  static public String encode(Object obj) {
    String json = null;
    // synchronized(obj) {
      json = mapper.toJson(obj);
    // }
    return json;
  }
  
  static public String encodePretty(Object obj) {    
    return prettyMapper.toJson(obj);
  }


  @Override
  public void encode(OutputStream out, Object obj) throws IOException {
    String json = null;
    // synchronized(obj) {
      json = mapper.toJson(obj);
   //  }
    if (json != null)
      out.write(json.getBytes());
  }

  @Override
  public Object[] decodeArray(Object data) throws Exception {
    // ITS GOT TO BE STRING - it just has to be !!! :)
    String instr = (String) data;
    // array of Strings ? - don't want to double encode !
    Object[] ret = null;
    synchronized (data) {
      ret = mapper.fromJson(instr, Object[].class);
    }
    return ret;
  }

  @Override
  public Object decode(Object data, Class<?> type) throws Exception {
    // data has to be a String !! .. just has to be
    if (data == null) {
      log.error("trying to decode null data");
      return null;
    }
    Object o = null;
    synchronized (data) {
      o = mapper.fromJson(data.toString(), type);
    }
    return o;
  }

  @Override
  public String getMimeType() {
    return "application/json";
  }

  @Override
  public String getKey() {
    return "messages";
  }

}

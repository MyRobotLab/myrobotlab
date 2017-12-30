package org.myrobotlab.codec;

import java.io.IOException;
import java.io.OutputStream;

import org.myrobotlab.codec.ApiFactory.ApiDescription;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CodecJson extends Codec {

  public final static Logger log = LoggerFactory.getLogger(CodecJson.class);

  private transient static Gson mapper = new GsonBuilder().create();// .setDateFormat("yyyy-MM-dd
                                                                    // HH:mm:ss.SSS").disableHtmlEscaping().create();
  static public String encode(Object obj) {
    return mapper.toJson(obj);
  }

  @Override
  public void encode(OutputStream out, Object obj) throws IOException {

    String json = mapper.toJson(obj);

    // if (log.isDebugEnabled()){
    //  log.warn("<< {}", json); - great for debugging !
    // }

    // if (recorder != null){
    // recorder.write(String.format("message << %s\n", json).getBytes());
    // }

    out.write(json.getBytes());
  }

  @Override
  public Object[] decodeArray(Object data) throws Exception {
    // ITS GOT TO BE STRING - it just has to be !!! :)
    String instr = (String) data;
    // array of Strings ? - don't want to double encode !
    Object[] ret = mapper.fromJson(instr, Object[].class);
    return ret;
  }

  @Override
  public Object decode(Object data, Class<?> type) throws Exception {
    // data has to be a String !! .. just has to be
    if (data == null) {
      log.error("trying to decode null data");
      return null;
    }
    return mapper.fromJson(data.toString(), type);
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

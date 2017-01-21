package org.myrobotlab.codec;

import java.io.IOException;
import java.io.OutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CodecJson implements Codec {
  /**
   * Result set to JSON - this is a fluid definition, except for the family
   * qualifier will always be 'd'
   * 
   * 
   * @param name
   * @param result
   * @return
   */

  private transient static Gson mapper = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").disableHtmlEscaping().create();

  public static final byte[] FQ_D = "d".getBytes();

  @Override
  public void encode(OutputStream out, Object obj) throws IOException {
    out.write(mapper.toJson(obj).getBytes());
    // jackson stream way !
    // mapper.writeValue(out, obj);
  }

  // probably should be Object too instead of byte[] :)
  @Override // FIXME - this is stoopid
  public Object[] decodeArray(Object data) throws Exception {
    // ITS GOT TO BE STRING - it just has to be !!! :)
    String instr = (String) data;
    // array of Strings ? - don't want to double encode !
    Object[] ret = mapper.fromJson(instr, Object[].class);
    // TODO Auto-generated method stub
    return ret;
  }

  @Override
  public Object decode(Object data, Class<?> type) throws Exception {
    // data has to be a String !! .. just has to be
    // String x = "\"" + (String)data + "\"";

    // FIXME JACKSON is not like GSON - it will not decode twice !!!

    String instr = String.format("\"%s\"", data.toString());
    // String instr = String.format("%s", data.toString());
    return mapper.fromJson(instr, type);
  }

  @Override
  public String getMimeType() {
    return "application/json";
  }

  @Override
  public String getKey() {
    return "json";
  }

}

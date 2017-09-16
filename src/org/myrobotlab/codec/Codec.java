package org.myrobotlab.codec;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.interfaces.NameProvider;

/**
 * Allows a common encoding/decoding interface for dynamic switching between
 * formats. Supports stream and non-stream systems although the codec behind it
 * may not be designed for one or the other
 * 
 * @author GroG
 *
 */
public abstract class Codec {

  /*
  OutputStream recorder = null;
  
  public void setRecorder(OutputStream recorder){
    this.recorder = recorder;
  }
  */
  
  public byte[] encode(NameProvider sender, String name, String method, Object...data) throws Exception{
    Message msg = Message.createMessage(sender, name, method, data);
    ByteArrayOutputStream encoded = new ByteArrayOutputStream();
    encode(encoded, msg);
    return encoded.toByteArray();
  }

  abstract public void encode(OutputStream out, Object obj) throws Exception;

  /**
   * Very important for message decoding where a parameters need decoding. Our
   * system needs to be able to decode an "Array" of parameters - not just one
   * so this method exists to handle that detail.
   * 
   * e.g. URI POST --to--&gt; Message
   * @param data the data to decode 
   * 
   * @return decoded object
   * @throws Exception e
   */
  abstract public Object[] decodeArray(Object data) throws Exception;

  /**
   * basic decoding method - probably too basic - I currently have only an
   * "Object" input and this certainly won't be the preferred object for many
   * codecs. But you got to start with something :)
   * 
   * @param data
   *          - simple or low level input class - String, byte[]
   * @param type
   *          - the expected type we want to decode into
   * @return decoded object
   * @throws Exception e
   */
  abstract public Object decode(Object data, Class<?> type) throws Exception;

  /**
   * get mime type of codec if it exists
   * 
   * @return decoded object
   */
  abstract public String getMimeType();

  /**
   * @return different strategies of encoding may in the end have the same mime type so
   * a key is needed to identify the exact type
   */

  abstract public String getKey();

}

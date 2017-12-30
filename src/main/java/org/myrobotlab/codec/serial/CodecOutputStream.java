package org.myrobotlab.codec.serial;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import org.myrobotlab.framework.interfaces.LoggingSink;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

/**
 * a class which decodes bytes as an output stream, puts the decoded strings on
 * a queue and relays the stream of bytes to another output stream if available
 * 
 * another thread can wait on the blocking decode method for new decoded
 * messages
 * 
 * @author GroG
 *
 */
public class CodecOutputStream extends OutputStream implements Serializable {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(CodecOutputStream.class);

  Codec codec;
  LoggingSink sink;
  OutputStream out;
  String prefix;

  public CodecOutputStream(String prefix, LoggingSink sink) {
    this.sink = sink;
    codec = new DecimalCodec(sink);
    this.prefix = prefix;
  }

  public Codec getCodec() {
    return codec;
  }

  public String getCodecExt() {
    if (codec == null) {
      return null;
    }
    return codec.getCodecExt();
  }

  public String getKey() {
    if (codec == null) {
      return null;
    } else {
      return codec.getKey();
    }
  }

  public OutputStream getOut() {
    return out;
  }

  public boolean isRecording() {
    return out != null;
  }

  public void setCodec(Codec codec) {
    this.codec = codec;
  }

  public void setCodec(String key) {
    try {
      codec = Codec.getDecoder(key, sink);
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public void setOut(OutputStream out) {
    this.out = out;
  }

  @Override
  public void write(int b) throws IOException {
    if (codec != null) {
      String decoded = codec.decode(b);
      if (decoded != null && out != null) {
        out.write(decoded.getBytes());
      }
    } else {
      if (out != null) {
        out.write(b);
      }
    }
  }

  public void record(String filename) throws FileNotFoundException {
    log.info(String.format("record RX %s", filename));

    if (isRecording()) {
      log.info("already recording");
      return;
    }

    if (filename == null) {
      filename = String.format("%s.%s.%d.data", prefix, sink.getName(), System.currentTimeMillis());
    }

    // FIXME - allow setting of output stream ...
    out = new FileOutputStream(filename);
  }

  public void clear() {
    if (codec != null) {
      codec.clear();
    }
  }

  public void close() {
    try {
      if (out != null) {
        out.close();
        out = null;
      }
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}

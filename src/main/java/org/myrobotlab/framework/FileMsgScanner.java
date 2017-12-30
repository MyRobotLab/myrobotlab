package org.myrobotlab.framework;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.interfaces.MessageSender;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

public class FileMsgScanner extends Thread {

  public final static Logger log = LoggerFactory.getLogger(FileMsgScanner.class);

  static String MSGS_DIR = "msgs";
  transient static MessageSender sender = null;

  public boolean scanning = false;

  static String id;
  static FileMsgScanner fileMsgScanner = null;

  public FileMsgScanner(String id) {
    super(String.format("%s.%s", FileMsgScanner.class.getSimpleName().toLowerCase(), id));
    FileMsgScanner.id = id;
  }

  public void run() {
    File folder = new File(MSGS_DIR);
    folder.mkdirs();
    log.info("enabling file msgs in {}", MSGS_DIR);
    
    scanning = true;
    try {
      while (scanning) {
        if (!scanForMsgs()) {
          sleep(500);
        }
      }
    } catch (Exception e) {
    }
  }

  public boolean scanForMsgs() {
    File folder = new File(MSGS_DIR);
    File[] listOfFiles = folder.listFiles();
    boolean filesExist = false;
    for (int i = 0; i < listOfFiles.length; i++) {
      File json = listOfFiles[i];
      if (json.isFile() && json.getName().endsWith(".json") && json.getName().startsWith(id)) {
        // FIXME - more accurate is to split file from .json and compare for exact match
        try {
          String data = new String(toByteArray(json));
          log.info("%s - %s", json, data);
          Message msg = CodecUtils.fromJson(data, Message.class);
          json.delete();
          sender.send(msg);
          filesExist = true;
        } catch (Exception e) {
          log.error("msgs/{} threw", json);
        }
      }
    }
    return filesExist;
  }

  /**
   * @param is
   *          IntputStream to byte array
   * @return byte array
   */
  static public final byte[] toByteArray(File file) {
    FileInputStream is = null;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      is = new FileInputStream(file.getAbsolutePath());
      int nRead;
      byte[] data = new byte[16384];

      while ((nRead = is.read(data, 0, data.length)) != -1) {
        baos.write(data, 0, nRead);
      }

      baos.flush();
      baos.close();
      is.close();
      return baos.toByteArray();
    } catch (Exception e) {
      Logging.logError(e);
    }

    return null;
  }

  static public void enableFileMsgs(Boolean b, String id) {
    if (b) {
      fileMsgScanner = new FileMsgScanner(id);
      fileMsgScanner.start();
    } else {
      fileMsgScanner.scanning = false;
      fileMsgScanner.interrupt();
      fileMsgScanner = null;
    }
  }
}

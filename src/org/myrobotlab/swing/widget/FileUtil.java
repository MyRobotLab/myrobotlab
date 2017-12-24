package org.myrobotlab.swing.widget;

import java.awt.FileDialog;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFrame;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class FileUtil {

  public final static Logger log = LoggerFactory.getLogger(FileUtil.class);

  static private String lastFileOpened;

  static private String lastFileSaved;

  static private String lastStatus;

  public static String getLastFileOpened() {
    return lastFileOpened;
  }

  public static String getLastFileSaved() {
    return lastFileSaved;
  }

  public static String getLastStatus() {
    return lastStatus;
  }

  static public String open(JFrame frame, String filter) {
    FileDialog file = new FileDialog(frame, "Open File", FileDialog.LOAD);
    file.setFile(filter); // Set initial filename filter
    file.setVisible(true); // Blocks
    String curFile;
    if ((curFile = file.getFile()) != null) {
      String newfilename = file.getDirectory() + curFile;
      char[] data;
      // setCursor (Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      File f = new File(newfilename);
      try {
        FileReader fin = new FileReader(f);
        int filesize = (int) f.length();
        data = new char[filesize];
        fin.read(data, 0, filesize);
        log.info("Loaded: " + newfilename);
        setLastFileOpened(newfilename);
        // avoid leaky file handles.
        fin.close();
        return new String(data);

      } catch (FileNotFoundException exc) {
        lastStatus = "File Not Found: " + newfilename;
        log.error(lastStatus);
      } catch (IOException exc) {
        lastStatus = "IOException: " + newfilename;
        log.error(lastStatus);
      }
      // setCursor (Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    return null;
  }

  static public boolean save(JFrame frame, String data, String filename) {

    if (filename == null || !(new File(filename).exists())) {
      return saveAs(frame, data, filename);
    } else {
      return writeFile(data, filename);
    }
  }

  static public boolean saveAs(JFrame frame, String data, String filename) {
    FileDialog fd = new FileDialog(frame, "Save File", FileDialog.SAVE);
    fd.setFile(filename);
    fd.setVisible(true);
    String selectedFilename = fd.getFile();
    if (selectedFilename != null) {
      filename = fd.getDirectory() + selectedFilename; // new selected
      // file
    } else {
      setLastStatus("canceled file save");
      return false;
    }
    return writeFile(data, filename);
  }

  public static void setLastFileOpened(String lastFileOpened) {
    FileUtil.lastFileOpened = lastFileOpened;
  }

  public static void setLastFileSaved(String lastFileSaved) {
    FileUtil.lastFileSaved = lastFileSaved;
  }

  public static void setLastStatus(String lastStatus) {
    FileUtil.lastStatus = lastStatus;
  }

  static public boolean writeFile(String data, String filename) {
    File f = new File(filename);
    try {
      FileWriter fw = new FileWriter(f);
      fw.write(data, 0, data.length());
      fw.close();
      setLastStatus("saved: " + filename);
      lastFileSaved = filename;
    } catch (IOException exc) {
      setLastStatus("IOException: " + filename);
      return false;
    }

    return true;
  }
}

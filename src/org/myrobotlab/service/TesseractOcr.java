package org.myrobotlab.service;

import java.io.File;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.TesseractOcr.Environment.POSIX;
import org.slf4j.Logger;

import com.sun.jna.Library;
import com.sun.jna.Native;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 * 
 * TesseractOCR - This service will use the open source project tesseract.
 * Tesseract will take an Image and extract any recognizable text from that
 * image as a string.
 *
 */
public class TesseractOcr extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(TesseractOcr.class.getCanonicalName());

  public static void main(String[] args) {
    LoggingFactory.init(Level.WARN);

    try {

      TesseractOcr tesseract = new TesseractOcr("tesseract");
      tesseract.startService();

      Runtime.createAndStart("gui", "SwingGui");
      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  /**
   * Static list of third party dependencies for this service. The list will be
   * consumed by Ivy to download and manage the appropriate resources
   * 
   * @return
   */

  public TesseractOcr(String n) {
    super(n);
    File file = new File(".");
    String filed = file.getAbsolutePath();
    POSIX e = new Environment.POSIX();
    e.setenv("TESSDATA_PREFIX", filed.substring(0, filed.length() - 1), 1);
    // for (Entry<String, String> g : System.getenv().entrySet()) {
    // System.out.println(g.getKey() + "=" + g.getValue());
    //
    // }
  }

  public String OCR(SerializableImage image) {
    try {
      String hh = Tesseract.getInstance().doOCR(image.getImage());
      // System.out.println(hh);
      log.info("Read: " + hh);
      return hh;
    } catch (TesseractException e) {
      e.printStackTrace();
    }
    return null;

  }

  @Override
  public void releaseService() {

    super.releaseService();
  }

  @Override
  public void stopService() {
    super.stopService();
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

    ServiceType meta = new ServiceType(TesseractOcr.class.getCanonicalName());
    meta.addDescription("Optical character recognition - the ability to read");
    meta.addCategory("intelligence");
    meta.addDependency("net.sourceforge.tess4j", "1.1");
    meta.addDependency("com.sun.jna", "3.2.2");
    return meta;
  }

  static class Environment {
    public interface LinuxLibC extends Library {
      public int setenv(String name, String value, int overwrite);

      public int unsetenv(String name);
    }

    static public class POSIX {
      static Object libc;

      static {
        if (System.getProperty("os.name").equals("Linux")) {
          libc = Native.loadLibrary("c", LinuxLibC.class);
        } else {
          libc = Native.loadLibrary("msvcrt", WinLibC.class);
        }
      }

      public int setenv(String name, String value, int overwrite) {
        if (libc instanceof LinuxLibC) {
          return ((LinuxLibC) libc).setenv(name, value, overwrite);
        } else {
          return ((WinLibC) libc)._putenv(name + "=" + value);
        }
      }

      public int unsetenv(String name) {
        if (libc instanceof LinuxLibC) {
          return ((LinuxLibC) libc).unsetenv(name);
        } else {
          return ((WinLibC) libc)._putenv(name + "=");
        }
      }
    }

    public interface WinLibC extends Library {
      public int _putenv(String name);
    }

    static POSIX libc = new POSIX();

  }

}

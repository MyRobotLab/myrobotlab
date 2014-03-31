package org.myrobotlab.service;

import java.io.File;
import java.util.Map.Entry;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Environment.POSIX;
import org.slf4j.Logger;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class TesseractOCR extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(TesseractOCR.class
			.getCanonicalName());

	public TesseractOCR(String n) {
		super(n);
		File file = new File(".");
		String filed = file.getAbsolutePath();
		POSIX e = new Environment.POSIX();
		e.setenv("TESSDATA_PREFIX", filed.substring(0, filed.length() - 1), 1);
//		for (Entry<String, String> g : System.getenv().entrySet()) {
//			System.out.println(g.getKey() + "=" + g.getValue());
//
//		}
	}

	@Override
	public String getDescription() {
		return "Tesseract OCR Engine";
	}

	public String OCR(SerializableImage image) {
		try {
			String hh = Tesseract.getInstance().doOCR(image.getImage());
//			System.out.println(hh);
			log.info("Read: "+hh);
			return hh;
		} catch (TesseractException e) {
			e.printStackTrace();
		}
		return null;

	};

	@Override
	public void stopService() {
		super.stopService();
	}

	@Override
	public void releaseService() {

		super.releaseService();
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		TesseractOCR tesseract = new TesseractOCR("tesseract");
		tesseract.startService();

		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * 
		 * 
		 */
	}

}

class Environment {
	public interface WinLibC extends Library {
		public int _putenv(String name);
	}

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

	static POSIX libc = new POSIX();
}

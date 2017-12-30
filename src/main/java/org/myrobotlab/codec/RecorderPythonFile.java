package org.myrobotlab.codec;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.Runtime;

public class RecorderPythonFile implements Recorder {

  boolean isRecording = false;

  private transient OutputStream recordingPython;

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.codec.Recorder#write(org.myrobotlab.framework.Message)
   */
  @Override
  public void write(Message msg) throws IOException {
    // python
    Object[] data = msg.data;
    String msgName = (msg.name.equals(Runtime.getInstance().getName())) ? "runtime" : msg.name;
    recordingPython.write(String.format("%s.%s(", msgName, msg.method).getBytes());
    if (data != null) {
      for (int i = 0; i < data.length; ++i) {
        Object d = data[i];
        if (d.getClass() == Integer.class || d.getClass() == Float.class || d.getClass() == Boolean.class || d.getClass() == Double.class || d.getClass() == Short.class
            || d.getClass() == Short.class) {
          recordingPython.write(d.toString().getBytes());

          // FIXME Character probably blows up
        } else if (d.getClass() == String.class || d.getClass() == Character.class) {
          recordingPython.write(String.format("\"%s\"", d).getBytes());
        } else {
          recordingPython.write("object".getBytes());
        }
        if (i < data.length - 1) {
          recordingPython.write(",".getBytes());
        }
      }
    }
    recordingPython.write(")\n".getBytes());
    recordingPython.flush();
  }

  @Override
  public void start(NameProvider service) throws FileNotFoundException {
    SimpleDateFormat TSFormatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    String cfgDir = String.format("%s%s.myrobotlab", System.getProperty("user.dir"), File.separator);
    String filenamePython = String.format("%s/%s_%s.py", cfgDir, service.getName(), TSFormatter.format(new Date()));

    // log.info(String.format("started recording %s to file %s", getName(),
    // filename));

    recordingPython = new BufferedOutputStream(new FileOutputStream(filenamePython), 8 * 1024);

  }

  public static void main(String[] args) {

  }

  @Override
  public void stop() throws IOException {
    // TODO Auto-generated method stub

  }

}

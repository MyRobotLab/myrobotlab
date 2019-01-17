package org.myrobotlab.document.transformer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class ConfigUtils {

  public static String toXML(Configuration c) {
    String xml = null;
    xml = initXStream().toXML(c);
    return xml;
  }

  public static XStream initXStream() {
    XStream xstream = new XStream(new StaxDriver());
    xstream.alias("stage", StageConfiguration.class);
    xstream.alias("workflow", WorkflowConfiguration.class);
    return xstream;
  }

}

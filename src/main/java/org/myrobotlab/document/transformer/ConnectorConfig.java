package org.myrobotlab.document.transformer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class ConnectorConfig extends Configuration {

  private final String connectorName;
  private final String connectorClass;

  public ConnectorConfig(String connectorName, String connectorClass) {
    this.connectorName = connectorName;
    this.connectorClass = connectorClass;
  }

  public String getConnectorName() {
    return connectorName;
  }

  public String getConnectorClass() {
    return connectorClass;
  }

  public static ConnectorConfig fromXML(String xml) {
    // TODO: move this to a utility to serialize/deserialize the config objects.
    // TODO: should override on the impl classes so they return a properly
    // cast config.
    Object o = (new XStream(new StaxDriver())).fromXML(xml);
    return (ConnectorConfig) o;
  }

}

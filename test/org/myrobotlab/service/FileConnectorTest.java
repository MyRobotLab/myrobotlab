package org.myrobotlab.service;

import org.myrobotlab.document.connector.AbstractConnector;
import org.myrobotlab.service.interfaces.AbstractConnectorTest;
import org.myrobotlab.service.interfaces.MockDocumentListener;

public class FileConnectorTest extends AbstractConnectorTest {

  @Override
  public AbstractConnector createConnector() {
    FileConnector connector = new FileConnector("testconnector");
    connector.setDirectory(".");
    return connector;
  }

  @Override
  public MockDocumentListener createListener() {
    return new MockDocumentListener("mocklistener");
  }

  @Override
  public void validate(MockDocumentListener listener) {
    // TODO: actually validate something here.
    log.info("Final Count: {}", listener.getCount());
  }

}

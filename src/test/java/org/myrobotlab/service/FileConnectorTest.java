package org.myrobotlab.service;

import org.junit.Ignore;
import org.myrobotlab.document.connector.AbstractConnector;
import org.myrobotlab.service.interfaces.AbstractConnectorTest;
import org.myrobotlab.service.interfaces.MockDocumentListener;

@Ignore
public class FileConnectorTest extends AbstractConnectorTest {

  @Override
  public AbstractConnector createConnector() {
    FileConnector connector = (FileConnector)Runtime.start("testconnector", "FileConnector");
    connector.setDirectory(".");
    return connector;
  }

  @Override
  public MockDocumentListener createListener() {
    return (MockDocumentListener)Runtime.start("mocklistener", "MockDocumentListener");
  }

  @Override
  public void validate(MockDocumentListener listener) {
    // TODO: actually validate something here.
    log.info("Final Count: {}", listener.getCount());
  }

}

package org.myrobotlab.service.interfaces;

import org.myrobotlab.document.connector.ConnectorState;

public interface DocumentConnector {

  public void startCrawling();

  public ConnectorState getConnectorState();

  public void stopCrawling();

}

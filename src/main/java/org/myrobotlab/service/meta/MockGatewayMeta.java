package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class MockGatewayMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MockGatewayMeta.class);

  public MockGatewayMeta() {
    addDescription("Service for testing.");
    addCategory("testing");
  }

}

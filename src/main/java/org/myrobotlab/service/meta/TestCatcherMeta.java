package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TestCatcherMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(TestCatcherMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public TestCatcherMeta() {

    addDescription("This service is used to test messaging");
    setAvailable(false);
    addCategory("testing", "framework");

    // addPeer("globalPeer", "thrower01", "TestThrower", "comment"); // Don't do
                                                                  // recursive
                                                                  // infinite
                                                                  // loop

  }

}

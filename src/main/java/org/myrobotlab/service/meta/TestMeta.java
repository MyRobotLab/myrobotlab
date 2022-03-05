package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TestMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(TestMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public TestMeta() {
    addDescription("Testing service");
    addCategory("testing");
    addPeer("http", "HttpClient", "to interface with Service pages");
    setAvailable(false);

    addDependency("junit", "junit", "4.13.1");
    // addPeer("python", "Python", "python to excercise python scripts");

  }

}

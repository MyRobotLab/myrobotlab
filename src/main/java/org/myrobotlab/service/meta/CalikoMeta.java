package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class CalikoMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(CalikoMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public CalikoMeta() {

    // add a cool description
    addDescription("used as a general template");

    // false will prevent it being seen in the ui
    setAvailable(true);

    // add dependencies if necessary
    // for the solver
    addDependency("au.edu.federation.caliko", "caliko", "1.3.8");

    // for the ui
    addDependency("au.edu.federation.caliko.visualisation", "caliko-visualisation", "1.3.8");
    addDependency("au.edu.federation.caliko.visualisation", "caliko-demo", "1.3.8");
      
    // add it to one or many categories
    addCategory("ik", "inverse kinematics");

    // add a sponsor to this service
    // the person who will do maintenance
    // setSponsor("GroG");

  }

}

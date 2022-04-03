package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class XMLConnectorMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(XMLConnectorMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public XMLConnectorMeta() {

    addDescription("This is an XML Connector that will parse a large xml file into many small xml documents");
    addCategory("filter");
    // FIXME - make a service page, and /python/service example
    setAvailable(false);

  }

}

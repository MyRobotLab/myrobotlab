package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class YahooFinanceStockQuoteMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(YahooFinanceStockQuoteMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * @param name n
   * 
   */
  public YahooFinanceStockQuoteMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("This service will query Yahoo Finance to get the current stock price.  more info @ https://developer.yahoo.com/yql/");
    addCategory("filter", "finance");
    setAvailable(false);

  }

}

package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class YahooFinanceStockQuoteMeta {
  public final static Logger log = LoggerFactory.getLogger(YahooFinanceStockQuoteMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.YahooFinanceStockQuote");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("This service will query Yahoo Finance to get the current stock price.  more info @ https://developer.yahoo.com/yql/");
    meta.addCategory("filter", "finance");
    meta.setAvailable(false);
    return meta;
  }

  
  
}


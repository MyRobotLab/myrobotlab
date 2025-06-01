package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.HtmlParserConfig;
import org.slf4j.Logger;

public class HtmlParser extends Service<HtmlParserConfig>
{
  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(HtmlParser.class);

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Runtime.start("jsoup", "Jsoup");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public HtmlParser(String n, String id) {
    super(n, id);
  }

}

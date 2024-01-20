package org.myrobotlab.programab.handlers.sraix;

import java.util.Locale;

import org.alicebot.ab.Chat;
import org.alicebot.ab.SraixHandler;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.programab.XmlParser;
import org.myrobotlab.programab.handlers.oob.OobProcessor;
import org.myrobotlab.programab.models.Sraix;
import org.myrobotlab.service.ProgramAB;
import org.myrobotlab.service.data.SearchResults;
import org.myrobotlab.service.interfaces.SearchPublisher;
import org.slf4j.Logger;

public class MrlSraixHandler implements SraixHandler {
  transient public final static Logger log = LoggerFactory.getLogger(MrlSraixHandler.class);

  final transient private ProgramAB programab;

  public MrlSraixHandler(ProgramAB programab) {
    this.programab = programab;
  }

  @Override
  public String sraix(Chat chatSession, String input, String defaultResponse, String hint, String host, String botid, String apiKey, String limit, Locale locale) {
    try {
      log.debug("MRL Sraix handler! Input {}", input);
      String xml = String.format("<sraix>%s</sraix>", input);
      // Template template = XmlParser.parseTemplate(xml);
      Sraix sraix = XmlParser.parseSraix(xml);
      
      if (sraix.oob != null) {      
        OobProcessor handler = programab.getOobProcessor();
        String ret = handler.process(sraix.oob, true); // block by default
        return ret;
      } else if (sraix.search != null) {
        log.info("search now");
        SearchPublisher search = (SearchPublisher)programab.startPeer("search");
        // if my default "search" peer key has a name .. use it ?
        if (search != null) {
          SearchResults results = search.search(sraix.search);
        // return results.getTextAndImages();
          return results.getHtml();
        } else {
          log.warn("no default search configured");
        }
      }
    } catch (Exception e) {
      programab.error(e);
    }
    if (defaultResponse != null) {
      return defaultResponse;
    }
    return "";
  }

}

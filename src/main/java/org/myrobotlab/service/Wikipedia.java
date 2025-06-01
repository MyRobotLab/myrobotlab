package org.myrobotlab.service;

import java.awt.event.TextListener;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.service.config.WikipediaConfig;
import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.data.SearchResults;
import org.myrobotlab.service.interfaces.ImageListener;
import org.myrobotlab.service.interfaces.ImagePublisher;
import org.myrobotlab.service.interfaces.SearchPublisher;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;


/**
 * Wikipedia via the official rest api docs here:
 * <p>
 * <a href=
 * "https://en.wikipedia.org/api/rest_v1/#/Page_content/get_page_summary_title">Wikimedia
 * REST API</a>
 * <p>
 * 
 * @see <a href="https://github.com/mudroljub/wikipedia-api-docs">wikipedia api
 *      docs</a>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Special:ApiSandbox">sandbox</a>
 *
 * @see <a href=
 *      "https://www.mediawiki.org/wiki/API:Main_page/lv#Quick_Start">quick
 *      start</a>
 *
 * @see <a href=
 *      "http://en.wikipedia.org/w/api.php?action=query&prop=info&format=json&titles=Stanford%20University">Standford
 *      University example</a>
 *
 *
 *      TODO - control the number of sentences to return
 * @see <a href=
 *      "https://en.wikipedia.org/w/api.php?action=help&modules=query%2Bextracts">Mediawiki
 *      API help</a> exsentences How many sentences to return. The value must be
 *      between 1 and 10. (and lots of other goodies !)
 *
 *
 * @author GroG
 *
 */
public class Wikipedia extends Service<WikipediaConfig>  implements SearchPublisher, ImagePublisher, TextPublisher {

  public final static Logger log = LoggerFactory.getLogger(Wikipedia.class);

  private static final long serialVersionUID = 1L;

  String acceptLanguage = null;

  /**
   * language will be prefixed to baseURL e.g. https://en.wikipedia.org
   */
  String baseUrl = ".wikipedia.org/api/rest_v1/page/summary";

  public Wikipedia(String n, String id) {
    super(n, id);
  }

  @Override
  public Map<String, Locale> getLocales() {
    // FIXME - return all standard wiki support languages
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ImageData> imageSearch(String searchText) {
    List<ImageData> ret = new ArrayList<>();
    SearchResults sr = searchWikipedia(searchText, false, true);
    ret.addAll(sr.images);
    return ret;
  }

  @Override
  public ImageData publishImage(ImageData image) {
    return image;
  }

  @Override
  public List<String> publishImages(List<String> images) {
    return images;
  }

  @Override
  public SearchResults publishResults(SearchResults results) {
    return results;
  }

  @Override
  public String publishText(String text) {
    return text;
  }

  @Override
  public SearchResults search(String searchText) throws IOException {
    return searchWikipedia(searchText, null, null);
  }

  /**
   * Private multi param implementation
   * 
   * FIXME - add image and text cache
   * 
   * @param searchText
   * @param publishText
   * @param publishImages
   * @return
   */
  private SearchResults searchWikipedia(String searchText, Boolean publishText, Boolean publishImages) {

    if (searchText == null || searchText.equals("")) {
      log.warn("searchText is null or empty");
      return null;
    }

    searchText = searchText.trim();

    SearchResults results = new SearchResults(searchText);

    WikipediaConfig c = (WikipediaConfig) config;
    try {

      if (publishText == null) {
        publishText = true;
      }

      if (publishImages == null) {
        publishImages = true;
      }

      // filtering and pre - processing

      // technically not correct - but its what wikipedia wants
      String encoded = URLEncoder.encode(searchText, StandardCharsets.UTF_8.toString()).replace("+", "%20");

      // use locale to set wikipedia endpoint
      String language = getLanguage() == null ? "en" : getLanguage();
      String url = "https://" + language + baseUrl + "/" + encoded;

      // search
      // FIXME - have exception throwing get
      byte[] bytes = Http.get(url);
      if (bytes != null) {
        String response = new String(bytes, StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, Object> json = CodecUtils.fromJson(response, Map.class);
        String extract = (String) json.get("extract");
        if (extract != null) {
          
          if (c.maxSentencesReturned != null) {
            String[] sentences = extract.split("\\.");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < c.maxSentencesReturned && i < sentences.length; ++i) {
              sb.append(sentences[i] + ". ");
            }
            extract = sb.toString().trim();
          }
          
          results.text.add(extract);
          invoke("publishText", extract);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> originalimage = (Map<String, Object>) json.get("originalimage");
        if (originalimage != null) {
          String source = (String) originalimage.get("source");
          if (source != null) {
            ImageData img = new ImageData();
            img.name = searchText;
            img.src = source;
            img.source = getName();
            results.images.add(img);
            invoke("publishImage", img);
          }
        }

      } else {
        log.info("no response for {}", searchText);
      }
      invoke("publishResults", results);

    } catch (Exception e) {
      results.errorText = e.getMessage();
      error(e);
    }

    return results;
  }

  @Override
  public int setMaxImages(int cnt) {
    return cnt;
  }

  @Override
  public void attach(String serviceName) throws Exception {
    ServiceInterface si = Runtime.getService(serviceName);

    if (si instanceof TextListener) {
      attachTextListener(serviceName);
    } else if (si instanceof ImageListener) {
      attachImageListener(serviceName);
    } else {
      error("%s doesn't know how to attach to %s", getName(), serviceName);
    }
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      // webgui.setSsl(true);
      webgui.autoStartBrowser(false);
      webgui.setPort(8888);
      webgui.startService();

      // Runtime.start("python", "Python");
      Wikipedia wiki = (Wikipedia) Runtime.start("wiki", "Wikipedia");
      ImageDisplay display = (ImageDisplay) Runtime.start("display", "ImageDisplay");

      boolean done = true;
      if (done) {
        return;
      }

      Map<String, Locale> locales = wiki.getLocales();
      Locale locale = wiki.getLocale();
      String language = wiki.getLanguage();

      Runtime runtime = Runtime.getInstance();
      // runtime.setLocales("ga"); - this is selection
      Runtime.setAllLocales("ga"); // this is all
      runtime.getLocale();

      language = wiki.getLanguage();

      // wiki.attachImageListener(display);
      wiki.attach("display");

      SearchResults sr = wiki.search("James_Joyce");

      // wiki.search("elon musk");
      //
      // wiki.search("gorilla");
      // wiki.search("monkey");
      // wiki.search("zebra");
      //
      // wiki.search("Claude Shannon");

      log.info("hello");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}

package org.myrobotlab.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.data.SearchResults;
import org.myrobotlab.service.interfaces.ImageListener;
import org.myrobotlab.service.interfaces.ImagePublisher;
import org.myrobotlab.service.interfaces.SearchPublisher;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.myrobotlab.service.interfaces.UtteranceListener;
import org.slf4j.Logger;

import com.google.gson.internal.LinkedHashTreeMap;

/**
 * Wikipedia via the official rest api docs here:
 * 
 * https://en.wikipedia.org/api/rest_v1/#/Page_content/get_page_summary_title
 * https://en.wikipedia.org/api/rest_v1/#/Page%20content/get_page_summary__title_
 * 
 * vs
 * 
 * https://github.com/mudroljub/wikipedia-api-docs
 * 
 * https://en.wikipedia.org/wiki/Special:ApiSandbox
 * 
 * https://www.mediawiki.org/wiki/API:Main_page/lv#Quick_Start
 * 
 * http://en.wikipedia.org/w/api.php?action=query&prop=info&format=json&titles=Stanford%20University
 * 
 * @author GroG
 * 
 * FIXME - make it multi-lingual - there must be a way
 *
 */
public class Wikipedia extends Service implements SearchPublisher, ImagePublisher, TextPublisher {

  public final static Logger log = LoggerFactory.getLogger(Wikipedia.class);

  private static final long serialVersionUID = 1L;

  String acceptLanguage = null;

  String baseUrl = "https://en.wikipedia.org/api/rest_v1/page/summary";
  // String baseUrl = "https://de.wikipedia.org/api/rest_v1/page/summary";

  public Wikipedia(String n, String id) {
    super(n, id);
  }
  
  public void attach(Attachable attachable) {
    if (attachable instanceof ImageListener) {
      attachImageListener(attachable.getName());
    } else {
      error("don't know how to attach a %s", attachable.getName());
    }
  }


  @Override
  public Map<String, Locale> getLocales() {
    // FIXME - return all standard wiki support languages
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> imageSearch(String searchText) {
    List<String> ret = new ArrayList<>();
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
    SearchResults results = new SearchResults(searchText);

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
      String url = baseUrl + "/" + encoded;

      // search
      // FIXME - have exception throwing get
      byte[] bytes = Http.get(url);
      if (bytes != null) {
        String response = new String(bytes, StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        LinkedHashTreeMap<String, Object> json = CodecUtils.fromJson(response, LinkedHashTreeMap.class);
        String extract = (String) json.get("extract");
        if (extract != null) {
          results.text.add(extract);
          invoke("publishText", extract);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> originalimage = (Map<String, Object>) json.get("originalimage");
        if (originalimage != null) {
          String source = (String) originalimage.get("source");
          if (source != null) {
            results.images.add(source);
            ImageData img = new ImageData();
            img.name = searchText;
            img.src = source;
            img.source = getName();
            invoke("publishImage", img);
          }
        }

      } else {
        error("no response for %s", searchText);
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
    attachImageListener(serviceName);
  }


  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("webgui", "WebGui");
      Runtime.start("python", "Python");
      Wikipedia wiki = (Wikipedia) Runtime.start("wiki", "Wikipedia");
      ImageDisplay display = (ImageDisplay) Runtime.start("display", "ImageDisplay");
      wiki.attachImageListener(display);
      wiki.search("elon musk");

      wiki.search("gorilla");
      wiki.search("monkey");
      wiki.search("zebra");

      log.info("hello");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
  
}

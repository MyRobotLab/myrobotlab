package org.myrobotlab.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.data.SearchResults;
import org.myrobotlab.service.interfaces.LocaleProvider;
import org.myrobotlab.service.interfaces.SearchPublisher;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

public class GoogleSearch extends Service implements TextPublisher, SearchPublisher, LocaleProvider {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(GoogleSearch.class);

  // We need a real browser user agent or Google will block our request with a
  // 403 - Forbidden
  public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.75 Safari/537.36";

  boolean saveSearchToFile = true;

  transient private static Pattern patternDomainName;

  transient private Matcher matcher;

  Integer maxImageWidth = null;

  int maxImages = 3;

  private static final String DOMAIN_NAME_PATTERN = "([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}";

  static {
    patternDomainName = Pattern.compile(DOMAIN_NAME_PATTERN);
  }

  /**
   * language of search and results
   */
  Locale locale = null;

  public GoogleSearch(String n, String id) {
    super(n, id);
    Runtime runtime = Runtime.getInstance();
    runtime.getLanguage();
    locale = runtime.getLocale();
  }

  @Override
  public String setLocale(String code) {
    locale = new Locale(code);
    log.info("language is {}", code);
    return code;
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(GoogleSearch.class);
    meta.addDescription("used as a general google search");
    meta.addDependency("org.jsoup", "jsoup", "1.8.3");
    meta.addCategory("search");
    return meta;
  }

  @Override
  public SearchResults search(String searchText) throws IOException {

    SearchResults results = new SearchResults(searchText);

    try {
      StringBuilder sb = new StringBuilder();

      String encodedSearch = URLEncoder.encode(searchText, "UTF-8");

      // not sure if locale is supported tag probably is ....
      String request = "https://google.com/search?lr=lang_" + locale.getTag() + "&q=" + encodedSearch + "&aqs=chrome..69i57.5547j0j7&sourceid=chrome&ie=UTF-8";

      // Fetch the page
      Document doc = Jsoup.connect(request).userAgent(USER_AGENT).get();
      /*
       * String safe = Jsoup.clean(unsafe, Whitelist.basic() .addTags("img")
       * .addAttributes("img", "height", "src", "width") .addProtocols("img",
       * "src", "http", "https", "data"));
       */

      if (saveSearchToFile) {
        FileOutputStream fos = new FileOutputStream(getDataDir() + fs + encodedSearch + ".html");
        fos.write(doc.toString().getBytes());
        fos.close();
      }

      /**
       * get description
       */
      for (Element header : doc.select("h2")) {
        String title = header.text();
        if (title.equals("Description")) {
          Element parent = header.parent();
          Elements spans = parent.select("span");

          for (Element span : spans) {
            log.info("description - {} ", span.text());
            // String url = header.attr("href");
            sb.append(span.text());
            results.text.add(span.text());
          }

        }
      }

      invoke("publishText", sb.toString());

      results.images = imageSearch(searchText);

      invoke("publishResults", results);
    } catch (Exception e) {
      results.errorText = e.getMessage();
      error(e);
    }

    return results;
  }

  // FIXME - use gson not simpl json
  @Override
  public List<String> imageSearch(String searchText) {
    // can only grab first 100 results
    String userAgent = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36";
    String url = "https://www.google.com/search?lr=lang_\" + langCode +\"&site=imghp&tbm=isch&source=hp&q=" + searchText + "&gws_rd=cr";

    List<String> resultUrls = new ArrayList<String>();

    try {
      Document doc = Jsoup.connect(url).userAgent(userAgent).referrer("https://www.google.com/").get();

      Elements elements = doc.select("div.rg_meta");

      int imgCnt = 0;

      JSONObject jsonObject;
      for (Element element : elements) {
        if (element.childNodeSize() > 0) {
          jsonObject = (JSONObject) new JSONParser().parse(element.childNode(0).toString());
          if (imgCnt > maxImages) {
            break;
          }
          resultUrls.add((String) jsonObject.get("ou"));
          ++imgCnt;
        }
      }

      System.out.println("number of results: " + resultUrls.size());

      for (String imageUrl : resultUrls) {

        invoke("publishImage", imageUrl);
        // System.out.println(imageUrl);
      }

      invoke("publishImages", resultUrls);

    } catch (Exception e) {
      error("parsing image threw", e);
    }

    return resultUrls;
  }

  public String getDomainName(String url) {

    String domainName = "";
    matcher = patternDomainName.matcher(url);
    if (matcher.find()) {
      domainName = matcher.group(0).toLowerCase().trim();
    }
    return domainName;

  }

  @Override
  public String publishText(String text) {
    return text;
  }

  @Override
  public SearchResults publishResults(SearchResults results) {
    return results;
  }

  @Override
  public String publishImage(String image) {
    return image;
  }

  @Override
  public List<String> publishImages(List<String> images) {
    return images;
  }

  @Override
  @Deprecated /* use standard attachTextListener */
  public void addTextListener(TextListener service) {
    addListener("publishText", service.getName());

  }

  @Override
  public int setMaxImages(int cnt) {
    maxImages = cnt;
    return cnt;
  }

  @Override
  public void attachTextListener(TextListener service) {
    addListener("publishText", service.getName());
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.setPort(8887);
      webgui.autoStartBrowser(false);
      SearchPublisher google = (SearchPublisher) Runtime.start("google", "GoogleSearch");
      webgui.startService();

      boolean isDone = true;
      if (isDone) {
        return;
      }

      SearchResults results = google.search("gorilla");

      log.info("response - \n{}", results);
      results = google.search("what is a giraffe");
      log.info("response - \n{}", results);
      results = google.search("what is a cat");
      log.info("response - \n{}", results);
      results = google.search("how tall is the empire state building");
      log.info("response - \n{}", results);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public String getLanguage() {
    return locale.getLanguage();
  }

  @Override
  public Locale getLocale() {
    return locale;
  }

  @Override
  public Map<String, Locale> getLocales() {
    return Locale.getAvailableLanguages();
  }


}

package org.myrobotlab.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

public class GoogleSearch extends Service implements TextPublisher {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(GoogleSearch.class);

  // We need a real browser user agent or Google will block our request with a
  // 403 - Forbidden
  public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.75 Safari/537.36";

  boolean saveSearchToFile = true;

  private static Pattern patternDomainName;
  private Matcher matcher;
  private static final String DOMAIN_NAME_PATTERN = "([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}";
  static {
    patternDomainName = Pattern.compile(DOMAIN_NAME_PATTERN);
  }

  public GoogleSearch(String n, String id) {
    super(n, id);
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

  public String search(String searchText) throws IOException {

    StringBuilder sb = new StringBuilder();

    String encodedSearch = URLEncoder.encode(searchText, "UTF-8");

    String request = "https://google.com/search?q=" + encodedSearch + "&aqs=chrome..69i57.5547j0j7&sourceid=chrome&ie=UTF-8";

    // Fetch the page
    Document doc = Jsoup.connect(request).userAgent(USER_AGENT).get();

    if (saveSearchToFile) {
      FileOutputStream fos = new FileOutputStream(getDataDir() + fs + encodedSearch + ".html");
      fos.write(doc.toString().getBytes());
      fos.close();
    }

    // Traverse the results
    // for (Element result : doc.select("h2.r > span")) {
    for (Element header : doc.select("h2")) {
      String title = header.text();
      if (title.equals("Description")) {
        Element parent = header.parent();
        Elements spans = parent.select("span");

        for (Element span : spans) {
          log.info("description - {} ", span.text());
          // String url = header.attr("href");
          sb.append(span.text());
        }

      }
    }
    
    invoke("publishText", sb.toString());

    return sb.toString();
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
  @Deprecated /* use standard attachTextListener */
  public void addTextListener(TextListener service) {
    addListener("publishText", service.getName());
    
  }
  
  @Override
  public void attachTextListener(TextListener service) {
    addListener("publishText", service.getName());
  }
  
  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
      Runtime.start("webgui", "WebGui");
      GoogleSearch google = (GoogleSearch) Runtime.start("google", "GoogleSearch");
      
      boolean isDone = true;
      if (isDone) {
        return;
      }
      
      String response = google.search("what is new caledonia");
      log.info("response - \n{}", response);
      response = google.search("what is a giraffe");
      log.info("response - \n{}", response);
      response = google.search("what is a cat");
      log.info("response - \n{}", response);
      response = google.search("how tall is the empire state building");
      log.info("response - \n{}", response);
      
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}

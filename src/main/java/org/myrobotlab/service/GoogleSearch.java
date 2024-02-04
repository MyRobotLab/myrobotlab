package org.myrobotlab.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.GoogleSearchConfig;
import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.data.SearchResults;
import org.myrobotlab.service.interfaces.ImagePublisher;
import org.myrobotlab.service.interfaces.LocaleProvider;
import org.myrobotlab.service.interfaces.SearchPublisher;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

public class GoogleSearch extends Service<GoogleSearchConfig> implements ImagePublisher, TextPublisher, SearchPublisher, LocaleProvider {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(GoogleSearch.class);

  // We need a real browser user agent or Google will block our request with a
  // 403 - Forbidden
  public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.75 Safari/537.36";

  transient private static Pattern patternDomainName;

  transient private Matcher matcher;

  GoogleSearchConfig c;

  private static final String DOMAIN_NAME_PATTERN = "([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}";

  static {
    patternDomainName = Pattern.compile(DOMAIN_NAME_PATTERN);
  }

  /**
   * language of search and results
   */

  protected List<String> excludeTextFilter = new ArrayList<String>();

  public GoogleSearch(String n, String id) {
    super(n, id);
    Runtime runtime = Runtime.getInstance();
    runtime.getLanguage();
    excludeTextFilter.add("Wikipedia");
    // setLowerCase();
  }

  public void setLowerCase() {
    c.lowerCase = true;
  }

  public void setUpperCase() {
    c.lowerCase = false;
  }

  public void addFilter(String filter) {
    excludeTextFilter.add(filter);
  }

  public void clearFilters() {
    excludeTextFilter.clear();
  }

  @Override
  public SearchResults search(String searchText) throws IOException {

    SearchResults results = new SearchResults(searchText);

    try {
      StringBuilder sb = new StringBuilder();

      String encodedSearch = URLEncoder.encode(searchText, "UTF-8");

      // https://moz.com/blog/the-ultimate-guide-to-the-google-search-parameters
      // not sure if locale is supported tag probably is ....
      String request = "https://google.com/search?lr=lang_" + locale.getLanguage() + "&q=" + encodedSearch + "&aqs=chrome..69i57.5547j0j7&sourceid=chrome&ie=UTF-8";
      log.info(String.format("request to google: %s", request));

      // Fetch the page
      Document doc = Jsoup.connect(request).userAgent(USER_AGENT).get();
      /*
       * String safe = Jsoup.clean(unsafe, Whitelist.basic() .addTags("img")
       * .addAttributes("img", "height", "src", "width") .addProtocols("img",
       * "src", "http", "https", "data"));
       */

      if (c.saveSearchToFile) {
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

            String text = null;
            if (c.lowerCase != null && c.lowerCase) {
              text = span.text().toLowerCase();
            } else if (c.lowerCase != null && !c.lowerCase) {
              text = span.text().toUpperCase();
            }
            for (String filter : excludeTextFilter) {
              text = text.replace(filter.toLowerCase(), "");
            }
            sb.append(text);
            results.text.add(text);
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

  public byte[] cacheFile(String filename, String url) throws IOException {
    Document doc = Jsoup.connect(url).userAgent(USER_AGENT).referrer("https://www.google.com/").get();
    String html = doc.toString();
    return saveFile("cachedFiles" + fs + filename + ".html", html);
  }

  public byte[] saveFile(String filename, String data) throws IOException {
    if (data == null) {
      return saveFile(filename, (byte[]) null);
    } else {
      return saveFile(filename, data.getBytes());
    }
  }

  public byte[] saveFile(String filename, byte[] data) throws IOException {
    if (filename == null) {
      log.error("saveFile cannot have null filename");
      return null;
    }
    filename = getDataDir() + fs + filename;
    // normalize request with os file seperator
    filename = filename.replace("\\", fs).replace("/", fs);
    File f = new File(filename);
    String parent = f.getParent();
    if (parent != null && parent.length() != 0) {
      File p = new File(parent);
      p.mkdirs();
    }

    FileOutputStream fos = new FileOutputStream(f.getAbsolutePath());
    fos.write(data);
    fos.close();

    return data;
  }

  @Override
  public List<ImageData> imageSearch(String searchText) {

    List<ImageData> resultUrls = new ArrayList<>();

    try {
      // can only grab first 100 results

      String url = "https://www.google.com/search?lr=lang_" + locale.getLanguage() + "&site=imghp&tbm=isch&source=hp&q=" + searchText + "&gws_rd=cr";
      String filename = URLEncoder.encode(searchText, StandardCharsets.UTF_8.toString());

      // FIXME - check for cache ??? or useCache boolean config ?
      // byte[] response = cacheFile(filename, url);

      Document doc = Jsoup.connect(url).userAgent(USER_AGENT).referrer("https://www.google.com/").get();
      String html = doc.toString();

      resultUrls = extractImageRefs(html);

      System.out.println("number of results: " + resultUrls.size());

      for (ImageData imageUrl : resultUrls) {

        ImageData img = new ImageData();
        img.name = searchText;
        img.src = imageUrl.src;
        img.source = getName();

        invoke("publishImage", img);
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
  public ImageData publishImage(ImageData image) {
    return image;
  }

  @Override
  public List<String> publishImages(List<String> images) {
    return images;
  }

  @Deprecated /* use standard attachTextListener */
  public void addTextListener(TextListener service) {
    attachTextListener(service.getName());
  }

  @Override
  public int setMaxImages(int cnt) {
    c.maxImages = cnt;
    return cnt;
  }

  @Override
  public void attachTextListener(TextListener service) {
    attachTextListener(service.getName());
  }

  public static void main(String[] args) {
    try {

      Runtime.main(new String[] { "--id", "admin"});
      LoggingFactory.init(Level.INFO);

      GoogleSearch google = (GoogleSearch) Runtime.start("google", "GoogleSearch");
      // ImageDisplay display = (ImageDisplay) Runtime.start("display",
      // "ImageDisplay");
      // display.attachSearchPublisher(google);
      // display.setAlwaysOnTop(true);

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      boolean isDone = true;
      if (isDone) {
        return;
      }

      // List<String> base64Images =
      // google.extractImageRefs("/lhome/grperry/github/mrl.develop/myrobotlab/data/GoogleSearch/cachedFiles/gorilla.html");

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

  public List<ImageData> extractImageRefs(String data) throws IOException {
    List<ImageData> ret = new ArrayList<>();

    // String data = FileIO.toString(filename);

    int pos0 = 0;
    int pos1 = 0;

    // this is where the start of the "real" image references begin
    pos0 = data.indexOf("b-GRID_STATE0");

    if (pos0 > 0) {
      pos0 = data.indexOf("jpg", pos0);
      while (pos0 != -1) {
        pos1 = data.lastIndexOf("\"", pos0);

        if (pos1 > 0) {
          String ref = data.substring(pos1 + 1, pos0 + 3);
          ret.add(new ImageData(ref));
          if (ret.size() == c.maxImages) {
            return ret;
          }
        }
        pos0 = data.indexOf("jpg", pos0 + 3);
      }
    }
    return ret;

  }

  @Override
  public Map<String, Locale> getLocales() {
    return Locale.getAvailableLanguages();
  }

  @Override
  public void attachTextListener(String name) {
    addListener("publishText", name);
  }

}

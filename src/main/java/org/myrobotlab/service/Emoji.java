package org.myrobotlab.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.image.DisplayedImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;
import org.slf4j.Logger;

// emotionListener
public class Emoji extends Service implements TextListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Emoji.class);

  transient ImageDisplay display = null;
  transient HttpClient http = null;

  transient DisplayedImage view = null;
  
  Map<String, EmojiData> emojis = new TreeMap<String, EmojiData>();
  Map<String, EmojiData> descriptionIndex = new TreeMap<String, EmojiData>();
  Map<String, EmojiData> emoticonIndex = new TreeMap<String, EmojiData>();
  Map<String, EmojiData> unicodeIndex = new TreeMap<String, EmojiData>();
  
  static public class EmojiData {
    String name;
    String src;
    String unicode;
    String[] description;
    String emoticon;    
  }

  public Emoji(String n) {
    super(n);
  }

  public void startService() {
    super.startService();
    display = (ImageDisplay) startPeer("display");
    http = (HttpClient) startPeer("http");
    // display.setBackground(Color.BLACK);
  }

  // 1f609
  public void display(String value) {
    // :shortcut: | unicode | emoticon | url
    // FIXME - android, applet .. eg.. cache file
    // https://images.emojiterra.com/google/android-oreo/512px/1f613.png
    // https://images.emojiterra.com/google/android-oreo/512px/1f613.png
    // display.displayFullScreen(String.format("https://images.emojiterra.com/google/android-oreo/512px/%s.png",
    // value));
    // display.displayFullScreen("https://upload.wikimedia.org/wikipedia/commons/thumb/4/45/Noto_Emoji_KitKat_1f613.svg/480px-Noto_Emoji_KitKat_1f613.svg.png");
    // display.displayFullScreen("C:\\github\\twemoji\\72x72\\1f681.png");
    // display.displayFullScreen("https://www.charbase.com/images/glyph/128565");

    // display.displayFullScreen("data:image/png;base64,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");
    display.display(
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEgAAABICAMAAABiM0N1AAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAACBUExURUdwTO2RAO2RABkZGO2RAO2RAO2RAO2RAO2RAO2RAP7jK/alF+2QAP/lK////vzcJ++WAvnEHPGfBPjTJPi0GzQvH+54cfOvCN1FBFVHH3FhHd/EKMqvJJSBIufk3uJmTvXDcrKdI0hHR7u5topLRfriuvKvQ4+Qj/jRkm1tbbyFE20z2/wAAAAKdFJOUwCI0/+8aaAe7UyCylCnAAAEeUlEQVRYw+WY63ajIBCAVxNjNFBQZA3eUnNt8/4PuMNFRGOi2dM/e3bSnqY6fM4NBvz16/+UMIwCz9uAeF4QheHfQSJv42+3yMp262+8KHyXAhA5mqZpmoPAH6po/jusMFAUICSMESOMJcCTrHWwDBV6vqIkLMY4xrH+0V8BJln+ElQAGJonRA59ELhGEunkOpozZ7PVmOeCAQUOei+NisCcVFoTx7OoV0YF0hwSLxCWQ6SiV5wkXiRgFEXb4CknXciRAu5NkySHzUTngTThXaQ4MsN4FqZ1MJAe4xT62i/MynLGLhyXVcUUiaL1uAo2Os64LPhKtAy/sKas+WolSknKEPIeHNN5L1ZSiuqZUZi0QqvI/0g+ClO41gGKY6214nWJp70Ci5UI9VyWovUgYwiZxNcrI6IlDyjMat7dr/WlbGCSNMgUtKM69g+TStjH1CaMJHXjDRGylUjKulPntRN0xytetKV9RuKatLEGqRGstYGoOv8wa2344CLuvU3RxnoGKXOLLWZVbR+ugw5lYc2sSuYWbY780IaaMnu9bOtC2DiteKVBfXjgmijq3lSoymDCs7ZnqJiWxD6gcG/x2gbV+gazw3pWOsqiqPrJAs44aZAkW2h5l7ewzxmurJ6bmT6yVW9W5eRNg6I+RLgUOgiDzAxL27gobGkwiiIT6z5EGB4JlBfzX7lYCGcKQZB0tD0XhKETktkFibhPApBnkqYnPiGmdS1Zs7EdIJcAnba1ThqTbT6RTXoWAw08kQ08zYhJmwMiqd506G1Dlkikbfym+0P/l4Dc7CiQaTpDEMT+et11GqCjJJVcJfCVUuTc3l2vVPsyAGF2R58fTXP5+vxUQGrHIOcbVYDr59fl0nxc6F0V0wgEKW0+jADwcvn6Aqgj8D9cbRqrdSr4I0itjcebVZqV5vTNYZ64run040oWLP8+LWKdbke1iBI3/aYgiVlxjnOsBijcmW+2IAPVQUjc9jP7eDs1zRPI99HOW6HGsW6KwKRNVJ0U7lLEj9+32wl4Rk4nyTgOlqs2luOSbtLKZURVXDXQMjx+lML5xL1CF6xdRqAZ5bp269U7wis9yi5scqnV7FK8A6r1IKeNwOIvJxZMreoNUsG0JAhF/ZYmN1enwjQtojRDnHYkfUuY/rQjEhfiDCLGwRaVGQA52ww2NXLdgN+kdTvF+Xz4beRwFgOO1oejgNuy5SYi6cTGifcULT2qKK36cF8D25rc3sp0vxYjjEJx3Rwzq7wb7rTAJJr1cocaP+/3hxHqcNjvYcYXVZZYVTdC3Z7WIe3u9V7LwYq5UNwdvexxh+yhIWmH0H5CEN1l8mO00ofNqHQOpbuhjFkIjRTk0SacOBjRMWnnNoPHm/AkP5w+0kwoS6GTV+Eh2+jZ4egJaZKDnnAUCaU/wNEkuoxDXx38IOJrtMg92d7X4dzheBYlUzlzODbn45coVRGzx3X9HkJ1+XSaojBL30UE663eMaQjCH3nlYZ5VeNvB/ub7pWN/xcvbH7itc9Pvoj69+UP1r62tqOuGjIAAAAASUVORK5CYII=");
    display.displayFullScreen("https://emojipedia-us.s3.dualstack.us-west-1.amazonaws.com/thumbs/120/apple/129/rolling-on-the-floor-laughing_1f923.png");

    // display.displayFullScreen("https://www.charbase.com/images/glyph/128565");

    // display.displayFullScreen(String.format("https://emojipedia-us.s3.dualstack.us-west-1.amazonaws.com/thumbs/120/google/146/winking-face_%s.png",
    // value));
  }

  public void cacheEmojis() throws IOException {
    File dir = new File("emoji");
    dir.mkdirs();
    // String url = "file:///etc/fstab";
    Document doc = Jsoup.parse(new File("wikipediaEmoji.html"), "UTF-8");
    // Document doc = Jsoup.connect(url).get();
    Elements links = doc.select("a[href]");
    Elements media = doc.select("[src]");
    Elements imports = doc.select("link[href]");

    print("\nMedia: (%d)", media.size());
    for (Element srcLink : media) {
      String tag = srcLink.tagName();
      if (tag.equals("img")) {
        String src = srcLink.attr("abs:src");
        if (src.contains("Noto_Emoji_Oreo")) {
          src = src.replace("48px", "1024px");
          byte[] image = http.getBytes(src);
          String filename = "emoji" + src.substring(src.lastIndexOf("/"));

          FileOutputStream fos = new FileOutputStream(filename);
          fos.write(image);
          fos.close();
        }
        print(" * %s: <%s> %sx%s (%s)", tag, srcLink.attr("abs:src"), srcLink.attr("width"), srcLink.attr("height"), trim(srcLink.attr("alt"), 20));
      } else {
        print(" * %s: <%s>", srcLink.tagName(), srcLink.attr("abs:src"));
      }
    }

    print("\nImports: (%d)", imports.size());
    for (Element link : imports) {
      print(" * %s <%s> (%s)", link.tagName(), link.attr("abs:href"), link.attr("rel"));
    }

    print("\nLinks: (%d)", links.size());
    for (Element link : links) {
      print(" * a: <%s>  (%s)", link.attr("abs:href"), trim(link.text(), 35));
    }
  }

  private static void print(String msg, Object... args) {
    System.out.println(String.format(msg, args));
  }

  private static String trim(String s, int width) {
    if (s.length() > width)
      return s.substring(0, width - 1) + ".";
    else
      return s;
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

    ServiceType meta = new ServiceType(Emoji.class);
    meta.addDescription("used as a general template");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary

    // TEMPORARY CORE DEPENDENCIES !!! (for uber-jar)
    // meta.addDependency("orgId", "artifactId", "2.4.0");
    // meta.addDependency("org.bytedeco.javacpp-presets", "artoolkitplus",
    // "2.3.1-1.4");
    // meta.addDependency("org.bytedeco.javacpp-presets",
    // "artoolkitplus-platform", "2.3.1-1.4");

    // meta.addDependency("com.twelvemonkeys.common", "common-lang", "3.1.1");

    meta.addPeer("display", "ImageDisplay", "image display");
    meta.addPeer("http", "HttpClient", "downloader");

    meta.setAvailable(false);
    meta.addCategory("general");
    return meta;
  }

  public void renameFiles() {
    File dir = new File("emoji-gerty");
    // File dir = new File("emoji-gerty-rename");
    File[] files = dir.listFiles();
    for (File file : files) {
      int len = "1024px-Noto_Emoji_Oreo_".length();
      String unicode = file.getName().substring(len, file.getName().indexOf("."));
      File rename = new File(String.format("emoji-gerty/%s.png", unicode));
      file.renameTo(rename);
    }
  }

  public static void main(String[] args) {
    try {

      // excellent resource
      // https://emojipedia.org/shocked-face-with-exploding-head/ (descriptions,
      // unicode)
      // https://emojipedia.org/freezing-face/
      // https://unicode.org/emoji/charts/full-emoji-list.html#1f600
      // https://apps.timwhitlock.info/emoji/tables/unicode#block-1-emoticons
      // https://unicode.org/emoji/charts/full-emoji-list.html

      // https://commons.wikimedia.org/wiki/Emoji

      // scan text for emotional words - addEmotionWordPair(happy 1f609) ...
      LoggingFactory.init(Level.INFO);

      Emoji emoji = (Emoji) Runtime.start("emoji", "Emoji");
      // emoji.cacheEmojis();
      emoji.renameFiles();
      // Runtime.start("gui", "SwingGui");

      // FIXME cache file
      for (int i = 609; i < 999; ++i) {
        // emoji.display("1f609");

        emoji.display(String.format("1f%03d", i));
      }

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public void onText(String text) {
    // TODO Auto-generated method stub

  }
}

// yum yum ...
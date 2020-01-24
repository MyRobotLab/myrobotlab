package org.myrobotlab.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.fsm.api.Event;
import org.myrobotlab.fsm.api.EventHandler;
import org.myrobotlab.fsm.api.SimpleEvent;
import org.myrobotlab.fsm.api.State;
import org.myrobotlab.fsm.core.SimpleTransition;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.StateListener;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

// emotionListener
// Links
// - http://googleemotionalindex.com/
public class Emoji extends Service
    implements TextListener, EventHandler, StateListener /* , StateHandler */ {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Emoji.class);

  transient ImageDisplay display = null;
  transient HttpClient http = null;
  transient FiniteStateMachine fsm = null;
  
  int defaultSize = 32;//px

  Map<String, String> descriptionIndex = new TreeMap<String, String>();

  State lastState = null;

  String mode =  "small";

  static public class EmojiData {
    String name;
    String src;
    String unicode;
    String[] description;
    String emoticon;
  }

  // FIXME ! - emoji are multi-unicode !!! - create unicode key
  public Emoji(String n, String id) {
    super(n, id);
    
    startPeer("fsm");
    
  }

  public void addEmojis(String... states) {
    // check for %2
    for (int i = 0; i < states.length; i += 2) {
      addEmoji(states[i], states[i + 1]);
    }
  }

  public void addEmoji(String keyword, String unicode) {
    descriptionIndex.put(keyword, unicode);
    fsm.addState(keyword);
  }

  public void startService() {
    super.startService();
    if (display == null) {
      display = (ImageDisplay) startPeer("display");
    }
    if (http == null) {
      http = (HttpClient) startPeer("http");
    }
    if (fsm == null) {
      fsm = (FiniteStateMachine) startPeer("fsm");
    } else {
      fsm.startService();
    }
    
    // subscribing to errors
    subscribe("*", "publishStatus");
  }
  

  public void onStatus(org.myrobotlab.framework.Status status) {
    if (status.isError()) {
      fire("ill-event");
    }
  }

  public String fire(String event) {
    try {
      fsm.fire(event);
      return event;
    } catch (Exception e) {
      log.error("onStatus threw", e);
    }
    return event;
  }

  public void display(String source) {
    display(new State(source)); 
  }

  public void display(State state) {
    String source = state.getId();
    log.info("display source {} fullscreen {}", source);
    String filename = null;

    // check if file (absolute file exists)

    // check for unicode name
    // if unicode file does not exist - populate cache
    File unicodeDir = new File(getEmojiCacheDir());
    if (!unicodeDir.exists()) {
      try {
        cacheEmojis();
      } catch (IOException e) {
        log.error("caching problem", e);
      }
    }

    // check for keyword
    if (descriptionIndex.containsKey(source)) {
      String unicodeFileName = getEmojiCacheDir() + File.separator + descriptionIndex.get(source) + ".png";
      if (new File(unicodeFileName).exists()) {
        filename = unicodeFileName;
      }
    }

    if (filename == null) {
      String unicodeFileName = getEmojiCacheDir() + File.separator + source + ".png";
      if (new File(unicodeFileName).exists()) {
        filename = unicodeFileName;
      }
    }

    // hail mary - just assign what its been told to display
    if (filename == null) {
      filename = source;
    }
    
    // defaults 
        // worke = fullscreen, black bg, single frame
        // small = 32px, no window, always on top (work on moving image)
    
    // common display attributes
    display.setMultiFrame(false);

    if (mode.equals("small")) {
      defaultSize = 32;
      display.setAlwaysOnTop(true);
      display.setFullScreen(false);      
    } else {
      defaultSize = 1024;
      display.setAlwaysOnTop(false);
      display.setFullScreen(true);
      display.setColor("#000");
      display.setColor("#000000");
    }

    // FIXME implement
    display.setColor("#000");
    try {
      display.display(filename);
    } catch (Exception e) {
      log.error("displayFullScreen threw", e);
    }
  }

  public String getEmojiCacheDir() {
    return getDataDir() + File.separator + defaultSize + "px";
  }

  public void cacheEmojis() throws IOException {
    info("downloading and caching emojis into [%s]", getEmojiCacheDir());
    String wikimediaEmojiList = getEmojiCacheDir() + File.separator + "wikimedia-emoji.html";
    String emojiKeywordIndex = getEmojiCacheDir() + File.separator + "wikimedia-emoji.properties";

    File dir = new File(getEmojiCacheDir());
    dir.mkdirs();

    String emojiList = null;

    try {
      emojiList = FileIO.toString(wikimediaEmojiList);
    } catch (Exception e) {
      log.info("could not find file {} will attempt to download it", wikimediaEmojiList);
    }

    if (emojiList == null) {
      Connection.Response html = Jsoup.connect("https://commons.wikimedia.org/wiki/Emoji").execute();
      Files.write(Paths.get(wikimediaEmojiList), html.body().getBytes());
    }

    emojiList = FileIO.toString(new File(wikimediaEmojiList));

    if (emojiList == null) {
      log.error("tried locally, and tried remotely .. giving up . no https://commons.wikimedia.org/wiki/Emoji available");
      return;
    }

    Document doc = Jsoup.parse(new File(wikimediaEmojiList), "UTF-8");

    Element table = doc.select("table").get(0); // select the first table.
    Elements rows = table.select("tr");

    int emojiFound = 0;

    // FIXME - handle multi-unicode streams !!

    for (int i = 1; i < rows.size(); i++) {
      Element row = rows.get(i);
      ++emojiFound;
      // 3 == Noto Color Emoji, Oreo
      // src means we have an image
      if (row.select("td").get(3).select("[src]").size() != 0) {
        // String index = row.select("th").get(0).ownText();
        String unicode = row.select("code").get(0).ownText();
        String[] keywords = row.select("small").get(0).ownText().replace("(", "").replace(")", "").toLowerCase().trim().split(",");

        Element notoOreo = row.select("td").get(3);
        Element imgSrc = notoOreo.select("img").first();

        String size = defaultSize + "px";
        // change size - we want the big one ..
        String src = imgSrc.absUrl("src").replace("48px", size);
        byte[] image = http.getBytes(src);

        String filename = getEmojiCacheDir() + File.separator + unicode + ".png";
        FileOutputStream fos = new FileOutputStream(filename);
        fos.write(image);
        fos.close();

        fos = new FileOutputStream(emojiKeywordIndex, true);
        for (String keyword : keywords) {
          keyword = keyword.toLowerCase().trim();
          fos.write(String.format("%s=%s\n", keyword, unicode).getBytes());
          if (!descriptionIndex.containsKey(keyword)) {
            descriptionIndex.put(keyword.toLowerCase().trim(), unicode);
          }
        }
        fos.close();
      } else {
        log.info("skipping row {}", i);
      }
    }

    log.info("emojis found {}", emojiFound);

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

    meta.addPeer("display", "ImageDisplay", "image display");
    meta.addPeer("http", "HttpClient", "downloader");
    meta.addPeer("fsm", "FiniteStateMachine", "emotional state machine");

    // meta.setAvailable(false);
    meta.addCategory("general");
    return meta;
  }

  @Override
  public void onText(String text) {
    // FIXME - look for emotional words

  }

  // FIXME - publish events if desired...
  @Override
  public void handleEvent(Event event) throws Exception {
    log.info("handleEvent {}", event);
    SimpleEvent se = (SimpleEvent) event;
    SimpleTransition transition = (SimpleTransition) se.getTransition();
    EmojiData emoji = new EmojiData();
    emoji.name = transition.getTargetState().getId();
    emoji.unicode = descriptionIndex.get(emoji.name);
    invoke("publishEmoji", emoji);
    display(transition.getTargetState());
    // emotionalState.getCurrentState().getName();
  }

  public State getCurrentState() {
    return fsm.getCurrentState();
  }

  public void publishEmoji(EmojiData emoji) {
    log.info("publishEmoji {}", emoji);
  }

  @Override
  public void onState(State state) {
    // check display properties fullscreen / popfront / icon / vs toolbar icon ?
    display(state); // it
  }
  
  public void initEmojiState() {

    // build the emotional finite state machine
    addEmojis("neutral", "1f610", "ecstasy", "1f923", "joy", "1f602", "serenity", "1f60c", "admiration", "1f929", "trust", "1f642", "acceptance", "1f642", "terror", "1f631",
        "fear", "1f628", "apprehension", "1f627", "amazement", "1f92f", "surprise", "1f92d", "distraction", "1f928", "grief", "1f62d", "sadness", "1f622", "pensiveness", "1f614",
        "loathing", "1f61d", "disgust", "1f623", "boredem", "1f644", "rage", "1f620", "anger", "1f621", "annoyance", "2639", "vigilance", "1f914", "anticipation", "1f914",
        "interest", "1f914 ", "vomiting", "1f92e", "sick", "1f922", "ill", "1f912");

    // serenity-ecstasy axis
    fsm.addTransition("neutral", "serenity-event", "serenity");
    fsm.addTransition("serenity", "serenity-event", "joy");
    fsm.addTransition("joy", "serenity-event", "ecstasy");

    fsm.addTransition("serenity", "clear-event", "neutral");
    fsm.addTransition("joy", "clear-event", "serenity");
    fsm.addTransition("ecstasy", "clear-event", "joy");

    // acceptance-admiration axis
    fsm.addTransition("neutral", "acceptance-event", "acceptance");
    fsm.addTransition("acceptance", "acceptance-event", "trust");
    fsm.addTransition("trust", "acceptance-event", "admiration");

    fsm.addTransition("acceptance", "clear-event", "neutral");
    fsm.addTransition("trust", "clear-event", "acceptance");
    fsm.addTransition("admiration", "clear-event", "trust");

    // apprehension-terror axis
    fsm.addTransition("neutral", "apprehension-event", "apprehension");
    fsm.addTransition("apprehension", "apprehension-event", "fear");
    fsm.addTransition("fear", "apprehension-event", "terror");

    fsm.addTransition("apprehension", "clear-event", "neutral");
    fsm.addTransition("fear", "clear-event", "apprehension");
    fsm.addTransition("terror", "clear-event", "fear");

    // distraction-amazement axis
    fsm.addTransition("neutral", "distraction-event", "distraction");
    fsm.addTransition("distraction", "distraction-event", "surprise");
    fsm.addTransition("surprise", "distraction-event", "amazement");

    fsm.addTransition("distraction", "clear-event", "neutral");
    fsm.addTransition("surprise", "clear-event", "distraction");
    fsm.addTransition("amazement", "clear-event", "surprise");

    // pensiveness-grief axis
    fsm.addTransition("neutral", "pensiveness-event", "pensiveness");
    fsm.addTransition("pensiveness", "pensiveness-event", "sadness");
    fsm.addTransition("sadness", "pensiveness-event", "grief");

    fsm.addTransition("pensiveness", "clear-event", "neutral");
    fsm.addTransition("sadness", "clear-event", "pensiveness");
    fsm.addTransition("grief", "clear-event", "sadness");

    // boredom-loathing axis
    fsm.addTransition("neutral", "boredom-event", "boredom");
    fsm.addTransition("boredom", "boredom-event", "disgust");
    fsm.addTransition("disgust", "boredom-event", "loathing");

    fsm.addTransition("boredom", "clear-event", "neutral");
    fsm.addTransition("disgust", "clear-event", "boredom");
    fsm.addTransition("loathing", "clear-event", "disgust");

    // annoyance-rage axis
    fsm.addTransition("neutral", "annoyance-event", "annoyance");
    fsm.addTransition("annoyance", "annoyance-event", "anger");
    fsm.addTransition("anger", "annoyance-event", "rage");

    fsm.addTransition("annoyance", "clear-event", "neutral");
    fsm.addTransition("anger", "clear-event", "annoyance");
    fsm.addTransition("rage", "clear-event", "anger");

    // interest-vigilance axis
    fsm.addTransition("neutral", "interest-event", "interest");
    fsm.addTransition("interest", "interest-event", "anticipation");
    fsm.addTransition("anticipation", "interest-event", "vigilance");

    fsm.addTransition("interest", "clear-event", "neutral");
    fsm.addTransition("anticipation", "clear-event", "interest");
    fsm.addTransition("vigilance", "clear-event", "anticipation");

    // ill-vomiting axis
    fsm.addTransition("neutral", "ill-event", "ill");
    fsm.addTransition("ill", "ill-event", "sick");
    fsm.addTransition("sick", "ill-event", "vomiting");

    fsm.addTransition("ill", "clear-event", "neutral");
    fsm.addTransition("sick", "clear-event", "ill");
    fsm.addTransition("vomiting", "clear-event", "sick");

    fsm.addScheduledEvent("clear-event", 1000 * 30);
    // emotionalState.addScheduledEvent("clear-event", 1000);
    
    // FIXME - DUMP INFO sorted tree map based on source state !
    log.info(fsm.getFsmMap());

    // loopback on state publishing...
    // subscribe(getName(),"publishState");
    try {
      fsm.attach(this);
    } catch (Exception e) {
      error(e);
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
      emoji.fire("ill-event");
      
      emoji.fire("clear-event");
      Service.sleep(2000);
      emoji.fire("ill-event");
      Service.sleep(2000);
      emoji.fire("ill-event");
      Service.sleep(2000);
      emoji.fire("ill-event");
      
      Runtime.release("emoji");
     
      log.info("here");

      /**
       * <pre>
       * State state = emoji.getCurrentState();
       * emoji.display(state.getName());
       * log.info("state {}", state);
       * 
       * emoji.fire("ill-event");
       * log.info("state {}", emoji.getCurrentState());
       * 
       * emoji.fire("ill-event");
       * log.info("state {}", emoji.getCurrentState());
       * 
       * emoji.fire("ill-event");
       * log.info("state {}", emoji.getCurrentState());
       * 
       * emoji.fire("ill-event");
       * log.info("state {}", emoji.getCurrentState());
       * 
       * emoji.fire("ill-event");
       * log.info("state {}", emoji.getCurrentState());
       * 
       * // emoji.cacheEmojis(); - FIXME - fix caching
       * for (int i = 0; i < 100; ++i) {
       *   emoji.display("1f610");
       *   Service.sleep(1000);
       *   emoji.display("1f627");
       *   Service.sleep(1000);
       *   emoji.display("1f628");
       *   Service.sleep(1000);
       *   emoji.display("1f631");
       *   Service.sleep(1000);
       *   emoji.display("1f92e");
       *   Service.sleep(1000);
       *   emoji.display("1f610");
       *   Service.sleep(1000);
       *   emoji.display("2639");
       *   Service.sleep(1000);
       *   emoji.display("2639");
       *   Service.sleep(1000);
       *   emoji.display("1f621");
       *   Service.sleep(1000);
       *   emoji.display("1f620");
       *   Service.sleep(1000);
       *   // FIXME - underlying does not use httpclient - so redirect FAILS !!!
       *   // -
       *   // should use httpclient & cache !!!
       *   // FIXME - url or file vs unicode addEmoji("happy",
       *   // "https://hips.hearstapps.com/hmg-prod.s3.amazonaws.com/images/vollmer1hr-1534611173.jpg")
       *   emoji.display("https://hips.hearstapps.com/hmg-prod.s3.amazonaws.com/images/vollmer1hr-1534611173.jpg");
       *   Service.sleep(1000);
       * 
       * }
       * </pre>
       */

      // FIXME cache file
      /*
       * for (int i = 609; i < 999; ++i) { // emoji.display("1f609");
       * 
       * emoji.display(String.format("1f%03d", i)); }
       */

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public ImageDisplay getDisplay() {
    return display;
  }

  public FiniteStateMachine getFsm() {
    return fsm;
  }
  
  @Override
  public void attachTextPublisher(TextPublisher service) {
    if (service == null) {
      log.warn("{}.attachTextPublisher(null)");
      return;
    }
    subscribe(service.getName(), "publishText");
  }

}

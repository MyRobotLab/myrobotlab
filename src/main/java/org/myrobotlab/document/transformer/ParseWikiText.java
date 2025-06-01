package org.myrobotlab.document.transformer;

import java.util.List;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.transformer.wiki.TextConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.WtEngineImpl;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.nodes.EngProcessedPage;
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;

public class ParseWikiText extends AbstractStage {

  public final static Logger log = LoggerFactory.getLogger(ParseWikiText.class.getCanonicalName());
  String input = "text";
  String output = "text";
  WikiConfig wikiConfig = null;
  WtEngineImpl engine = null;

  @Override
  public void startStage(StageConfiguration config) {
    // TODO Auto-generated method stub
    input = config.getStringParam("input", input);
    output = config.getStringParam("output", output);
    // perhaps this needs to be created once up front?
    wikiConfig = DefaultConfigEnWp.generate();
    // Instantiate a compiler for wiki pages
    engine = new WtEngineImpl(wikiConfig);

  }

  @Override
  public List<Document> processDocument(Document doc) {
    // TODO: what's the wiki id to use for this? does it even really matter?
    String title = doc.getId();
    if (!doc.hasField(input)) {
      log.info("no input field for wiki {}", doc.getId());
      return null;
    }
    String wikiText = doc.getField(input).get(0).toString();
    try {

      // Retrieve a page
      PageTitle pageTitle = PageTitle.make(wikiConfig, title);
      PageId pageId = new PageId(pageTitle, -1);
      // Compile the retrieved page
      EngProcessedPage cp = engine.postprocess(pageId, wikiText, null);
      // This compiled page i think has all the mojo i seek!
      TextConverter p = new TextConverter(wikiConfig, 132, doc);
      String result = (String) p.go(cp.getPage());
      doc.setField(output, result);
      // how many children docs are there.
      List<Document> children = p.getChildrenDocs();
      // System.gc();
      int i = 0;
      for (Document d : children) {
        String childId = doc.getId() + "_infobox_" + i;
        d.setId(childId);
        i++;
        // let's fix the doc ids so they're deterministic
      }
      // log.info("######### CHILDREN : {}", children.size());
      p = null;
      // emit the children docs from this method.
      return children;
    } catch (Exception e) {
      log.warn("Error parsing wiki text on document {}\n{}", doc.getId(), wikiText);
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void stopStage() {
    // TODO Auto-generated method stub

  }

  @Override
  public void flush() {
    // TODO Auto-generated method stub

  }

}

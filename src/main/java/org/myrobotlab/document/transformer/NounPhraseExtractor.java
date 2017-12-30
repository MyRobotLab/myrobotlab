package org.myrobotlab.document.transformer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.myrobotlab.document.Document;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

public class NounPhraseExtractor extends AbstractStage {

  public final static Logger log = LoggerFactory.getLogger(NounPhraseExtractor.class.getCanonicalName());

  private String personModelFile = "./opennlp/en-ner-person.bin";
  private String sentenceModelFile = "./opennlp/en-sent.bin";
  private String tokenModelFile = "./opennlp/en-token.bin";
  private String posModelFile = "./opennlp/en-pos-maxent.bin";

  // TODO: These are NOT thread safe!!!! WorkflowServer must be single threaded
  // until we make these thread safe... :-/
  private SentenceDetectorME sentenceDetector;
  private Tokenizer tokenizer;
  private NameFinderME nameFinder;
  private POSTaggerME posTagger;

  private String textField = "text";
  private String peopleField = "people";
  private String posTextField = "pos_text";
  private String sep = " ";

  @Override
  public void startStage(StageConfiguration config) {

    // parse the config to map the params properly
    textField = config.getProperty("textField", textField);
    peopleField = config.getProperty("peopleField", peopleField);
    posTextField = config.getProperty("posTextField", posTextField);

    try {
      // Sentence finder
      SentenceModel sentModel = new SentenceModel(new FileInputStream(sentenceModelFile));
      sentenceDetector = new SentenceDetectorME(sentModel);
      // tokenizer
      TokenizerModel tokenModel = new TokenizerModel(new FileInputStream(tokenModelFile));
      tokenizer = new TokenizerME(tokenModel);
      // person name finder
      TokenNameFinderModel nameModel = new TokenNameFinderModel(new FileInputStream(personModelFile));
      nameFinder = new NameFinderME(nameModel);
      // load the part of speech tagger.
      posTagger = new POSTaggerME(new POSModel(new FileInputStream(posModelFile)));
    } catch (IOException e) {
      log.info("Error loading up OpenNLP Models. {}", e.getLocalizedMessage());
      e.printStackTrace();
    }
  }

  @Override
  public List<Document> processDocument(Document doc) {
    log.info("Processing Doc: {}", doc.getId());
    ArrayList<Document> children = new ArrayList<Document>();
    if (!doc.hasField(textField)) {
      log.info("No Text Field On Document {}", doc.getId());
      return null;
    }

    for (Object o : doc.getField(textField)) {
      if (o == null) {
        log.info("Null field value! Field : {} Doc: {}", textField, doc.getId());
        continue;
      }
      if (o instanceof String) {
        String text = o.toString();
        if (StringUtils.isEmpty(text)) {
          // skip empty/null strings
          continue;
        }
        String sentences[] = sentenceDetector.sentDetect(text);
        for (String sentence : sentences) {
          if (StringUtils.isEmpty(sentence)) {
            log.info("Empty sentence...");
            continue;
          }
          String tokens[] = tokenizer.tokenize(sentence);
          Span[] spans = nameFinder.find(tokens);
          // part of speech tagging
          String posText = posTagger.tag(sentence);
          // extract a triple from the sentence.
          children.addAll(createTripleDocuments(doc.getId(), posText));
          doc.addToField(posTextField, posText);
          for (Span span : spans) {
            String[] terms = Arrays.copyOfRange(tokens, span.getStart(), span.getEnd());
            String entity = StringUtils.join(terms, sep);
            doc.addToField(peopleField, entity);
          }
        }
      } else {
        log.info("Only Strings will be processed not {}", o.getClass());
      }
    }
    // TODO: move this into it's own stage. but for now, this is just to poc it.
    children.addAll(createEntityMentionDocs(doc));
    log.info("Extracted {} children records from that document.", children.size());
    for (Document d : children) {
      log.info(d.toString());
    }
    return children;
  }

  private List<Document> createTripleDocuments(String parentId, String posText) {
    // TODO : implement a much better tuned grammar for parsing
    // subject/object/verb
    // this is very likely language dependent.
    ArrayList<Document> childrenDocs = new ArrayList<Document>();

    // we'll look for the nouns, then the verbs, then the nouns again. (add an
    // end element to the sentence)
    String[] parts = (posText + " END/END").split(" ");
    // System.out.println("#######################################");

    // we want to find the runs of n* and v* ...
    ArrayList<String> subjects = new ArrayList<String>();
    ArrayList<String> verbs = new ArrayList<String>();
    ArrayList<String> objects = new ArrayList<String>();

    StringBuilder currentSubject = new StringBuilder();
    StringBuilder currentVerb = new StringBuilder();
    StringBuilder currentObject = new StringBuilder();

    // state info for the iteration
    String prevPOS = "";
    boolean seenFirstVerb = false;
    for (String part : parts) {
      part = part.trim();
      if (StringUtils.isEmpty(part) || !part.contains("/")) {
        continue;
      }
      String[] subpart = part.split("/");
      String word = subpart[0];
      String pos = subpart[1];
      // System.out.println("WORD: " + word + " POS: " + pos + " PREV: " +
      // prevPOS);
      // NN to not NN ends nouns
      // not NN to NN starts nouns.
      if (pos.startsWith("N")) {
        if (seenFirstVerb) {
          currentObject.append(word + " ");
        } else {
          currentSubject.append(word + " ");
        }
      }
      if (prevPOS.startsWith("N") && !pos.startsWith("N")) {
        if (!seenFirstVerb) {
          String subjectName = currentSubject.toString().trim();
          if (!StringUtils.isEmpty(subjectName)) {
            subjects.add(subjectName);
          }
          currentSubject = new StringBuilder();
        } else {
          String objName = currentObject.toString().trim();
          if (!StringUtils.isEmpty(objName)) {
            objects.add(objName);
          }
          currentObject = new StringBuilder();
        }

      }

      // now for verb phrases.
      if (pos.startsWith("V") || "JJ".equals(pos)) {
        seenFirstVerb = true;
        currentVerb.append(word + " ");
      }
      if (prevPOS.startsWith("V") && !pos.startsWith("V")) {
        String verbName = currentVerb.toString().trim();
        if (!StringUtils.isEmpty(verbName)) {
          verbs.add(verbName);
        }
        currentVerb = new StringBuilder();
      }

      prevPOS = pos;
    }
    // now we want to see what all the verbs/nouns we found are.
    // carteasean expansion.. just for fun!
    for (String subject : subjects) {
      for (String verb : verbs) {
        for (String object : objects) {
          if (!subject.equals(object)) {
            // System.out.println("Subject:" + subject + " VERB:" + verb + "
            // OBJECT:" + object);
            // lets create a child document for each of these combindations.
            // TODO: something better than this..
            String childId = "triple_" + parentId + "_" + subject + " " + verb + " " + object;
            Document child = new Document(childId);
            child.setField("table", "triple");
            child.setField("subject", subject);
            child.setField("verb", verb);
            child.setField("object", object);
            child.setField("parent_id", parentId);
            // TODO: sanitized this fieldname
            String normVerb = normalizeFieldName(verb);
            child.setField(normVerb + "_verb", object);
            // add it to the list of docs that we've created.
            childrenDocs.add(child);
          }
        }
      }
    }
    return childrenDocs;
  }

  private String normalizeFieldName(String verb) {
    // TODO Auto-generated method stub
    String cleanVerb = verb.replaceAll(" ", "_").toLowerCase();
    return cleanVerb;
  }

  private List<Document> createEntityMentionDocs(Document doc) {
    // TODO Auto-generated method stub
    ArrayList<Document> docs = new ArrayList<Document>();
    // we have the fact that certain people are actually people.
    if (!doc.hasField(peopleField)) {
      log.info("No people found...");
      return docs;
    }
    for (Object o : doc.getField(peopleField)) {
      // the unique id for this, is the doc id and the person
      // TODO: handle person name collisions.
      // TODO: something better, but for now this is good enough.
      String docId = "person_" + doc.getId() + "_" + o.toString();
      Document personDoc = new Document(docId);
      personDoc.setField("person", o.toString());
      // TODO: consider some better ideas for how to set these for each person.
      personDoc.setField("parent_id", doc.getId());
      personDoc.setField("node_id", o.toString());
      // maybe copy some other field from the parent doc?
      personDoc.setField("is_verb", "person");
    }
    return docs;
  }

  @Override
  public void stopStage() {
    // TODO: close/shutdown the models!
  }

  @Override
  public void flush() {
    // no op , i believe.
    return;

  }

  public String getPersonModelFile() {
    return personModelFile;
  }

  public void setPersonModelFile(String personModelFile) {
    this.personModelFile = personModelFile;
  }

  public String getSentenceModelFile() {
    return sentenceModelFile;
  }

  public void setSentenceModelFile(String sentenceModelFile) {
    this.sentenceModelFile = sentenceModelFile;
  }

  public String getTokenModelFile() {
    return tokenModelFile;
  }

  public void setTokenModelFile(String tokenModelFile) {
    this.tokenModelFile = tokenModelFile;
  }

  public String getTextField() {
    return textField;
  }

  public void setTextField(String textField) {
    this.textField = textField;
  }

  public String getPeopleField() {
    return peopleField;
  }

  public void setPeopleField(String peopleField) {
    this.peopleField = peopleField;
  }

  public String getPosTextField() {
    return posTextField;
  }

  public void setPostTextField(String posTextField) {
    this.posTextField = posTextField;
  }

}

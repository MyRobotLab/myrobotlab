package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;
import org.wikidata.wdtk.datamodel.json.jackson.JacksonValueSnak;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

public class WikiDataFetcher extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(WikiDataFetcher.class);

  static String language = "en";
  static String website = "enwiki";

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {
      WikiDataFetcher wiki = (WikiDataFetcher) Runtime.start("wikiDataFetcher", "WikiDataFetcher");
      wiki.setWebSite("enwiki");
      wiki.setLanguage("en");

      EntityDocument doc = WikiDataFetcher.getWiki("Halloween");

      log.info(getData("united states", "P36"));

      log.info(doc.toString());

      log.info(getData("eiffel tower", "P2048"));
      
      log.info(getData("nothing to test", "P2048"));

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public WikiDataFetcher(String n) {
    super(n);
  }

  public String[] getCategories() {
    return new String[] { "general" };
  }

  @Override
  public String getDescription() {
    return "Used to collect datas from wikidata";
  }

  public void setLanguage(String lang) {
    language = lang;
  }

  public void setWebSite(String site) {
    website = site;
  }

  private static EntityDocument getWiki(String query) throws MediaWikiApiErrorException {
    WikibaseDataFetcher wbdf = WikibaseDataFetcher.getWikidataDataFetcher();
    query = upperCaseAllFirst(query);
    EntityDocument wiki = wbdf.getEntityDocumentByTitle(website, query);
    if (wiki == null) {
      System.out.println("ERROR ! Can't get the document : " + query);
    }
    return wiki;
  }

  private static EntityDocument getWikiById(String query) throws MediaWikiApiErrorException {
    WikibaseDataFetcher wbdf = WikibaseDataFetcher.getWikidataDataFetcher();
    EntityDocument wiki = wbdf.getEntityDocument(upperCaseAllFirst(query));
    // System.out.println( (String) wiki.getEntityId().getId());
    if (wiki == null) {
      System.out.println("ERROR ! Can't get the document : " + query);
    }
    return wiki;
  }

  // TODO Add comments to build the javadoc
  public String getDescription(String query) throws MediaWikiApiErrorException {
    EntityDocument document = getWiki(query);
    if (document instanceof ValueSnak) {
      System.out.println("MainSnak Value : ");
    }

    try {
      String answer = ((ItemDocument) document).getDescriptions().get(language).getText();
      return answer;
    } catch (Exception e) {
      return "Not Found !";
    }

  }

  public String getLabel(String query) throws MediaWikiApiErrorException {
    EntityDocument document = getWiki(query);
    try {
      String answer = ((ItemDocument) document).getLabels().get(language).getText();
      return answer;
    } catch (Exception e) {
      return "Not Found !";
    }
  }

  public String getId(String query) throws MediaWikiApiErrorException {
    EntityDocument document = getWiki(query);
    try {
      String answer = document.getEntityId().getId();
      return answer;
    } catch (Exception e) {
      return "Not Found !";
    }
  }

  public String getDescriptionById(String query) throws MediaWikiApiErrorException {
    EntityDocument document = getWikiById(query);
    try {
      String answer = ((ItemDocument) document).getDescriptions().get(language).getText();
      return answer;
    } catch (Exception e) {
      return "Not Found !";
    }
  }

  public static String getLabelById(String query) throws MediaWikiApiErrorException {
    EntityDocument document = getWikiById(query);
    try {
      String answer = ((ItemDocument) document).getLabels().get(language).getText();
      return answer;
    } catch (Exception e) {
      return "Not Found !";
    }
  }

  public String cutStart(String sentence) throws MediaWikiApiErrorException {// Remove
    // the
    // first
    // word
    // (The
    // cat
    // ->
    // The)
    try {
      if (sentence.indexOf(" ") != -1) {
        String answer = sentence.substring(sentence.indexOf(" ") + 1);
        return answer;
      } else {
        return sentence;
      }

    } catch (Exception e) {
      return sentence;
    }
  }

  public String grabStart(String sentence) throws MediaWikiApiErrorException {// keep
    // only
    // the
    // first
    // word
    // (The
    // cat
    // ->
    // cat)
    try {
      if (sentence.indexOf(" ") != -1) {
        String answer = sentence.substring(0, sentence.indexOf(" "));
        return answer;
      } else {
        return sentence;
      }
    } catch (Exception e) {
      return sentence;
    }
  }

  public static String upperCaseAllFirst(String value) {

    char[] array = value.toCharArray();
    // Uppercase first letter.
    array[0] = Character.toUpperCase(array[0]);
    int count = 0; // Count number of char since last space
    int charToChange = 0;
    // Uppercase all letters that follow a whitespace character.
    for (int i = 1; i < array.length; i++) {
      count++;
      if (Character.isWhitespace(array[i - 1]) || i == array.length - 1) {
        if (i == array.length - 1) {
          count += 2;
        }
        if (count > 4) {
          array[charToChange] = Character.toUpperCase(array[charToChange]);
        }
        charToChange = i;
        count = 0;
      }

    }
    // Result.
    return new String(array);
  }

  private static List<StatementGroup> getStatementGroup(String query) throws MediaWikiApiErrorException {
    EntityDocument document = getWiki(query);
    return ((ItemDocument) document).getStatementGroups();
  }

  private static ArrayList<Object> getSnak(String query, String ID) throws MediaWikiApiErrorException {
    // TODO: parameterize these data / al objects and parameterize the
    // return
    // value of this function.
    List<StatementGroup> document = getStatementGroup(query);
    String dataType = "error";
    // Value data = document.get(0).getProperty();
    ArrayList<Object> al = new ArrayList<Object>();
    for (StatementGroup sg : document) {
      ID = ID.replaceAll("[\r\n]+", "");
      String testedID = sg.getProperty().getId();
      if (ID.equals(testedID)) { // Check if this ID exist for this
        // document
        System.out.println("Found !");
        for (Statement s : sg.getStatements()) {
          if (s.getClaim().getMainSnak() instanceof ValueSnak) {
            dataType = ((JacksonValueSnak) s.getClaim().getMainSnak()).getDatatype().toString();
            // TODO Add all snaks instead of only the main snak
            al.add(dataType);
            al.add((JacksonValueSnak) s.getClaim().getMainSnak());

          }

        }
      }

    }
    return al;
  }

  public static String getData(String query, String ID) throws MediaWikiApiErrorException {
    // remove the try for dubug :
    // https://github.com/MyRobotLab/myrobotlab/issues/94
    try {
    ArrayList<Object> al = getSnak(query, ID);
    // TODO manage all snaks and qualifiers
    Value data = ((JacksonValueSnak) al.get(1)).getDatavalue();
    String dataType = (String) al.get(0);
    String answer = "";
    System.out.print("Datatype : " + dataType);
    // TODO put switch in a function out of getData()
    switch (dataType) {
      case "wikibase-item"://
        String info = (String) data.toString();
        int beginIndex = info.indexOf('Q');
        int endIndex = info.indexOf("(");
        info = info.substring(beginIndex, endIndex - 1);
        answer = getLabelById(info);
        break;
      case "time"://
        data = (TimeValue) data;
        answer = String.valueOf(((TimeValue) data).getDay()) + "/" + String.valueOf(((TimeValue) data).getMonth()) + "/" + String.valueOf(((TimeValue) data).getYear());
        break;
      case "globe-coordinate":
        answer = ((GlobeCoordinatesValue) data).toString();
        break;
      case "monolingualtext"://
        data = (MonolingualTextValue) data;
        answer = data.toString();
        break;
      case "quantity"://
        data = (QuantityValue) data;
        String quantity = String.valueOf(((QuantityValue) data).getNumericValue());
        String unit = String.valueOf(((QuantityValue) data).getUnit());
        // String unit = data.toString();
        int beginIndex2 = unit.indexOf('Q');
        if (beginIndex2 != -1) {
          unit = unit.substring(beginIndex2);
          if (Long.parseLong(quantity, 16) < 2) {
            quantity += " " + getLabelById(unit);
          } else {
            quantity += " " + getLabelById(unit) + "s";
          }
        }
        answer = quantity;
        break;
      case "propertyId":
        answer = ((PropertyIdValue) data).toString();
        break;
      case "url"://
        answer = data.toString();
        break;
      case "commonsMedia":
        answer = data.toString();
        break;
      default:
        answer = "Not Found !";
        break;
    }
    return answer;
    } catch (Exception e) {
    return "Not Found !";
    }
  }

  public String getProperty(String query, String ID) throws MediaWikiApiErrorException {
    try {
      ArrayList<Object> al = getSnak(query, ID);
      String info = (((JacksonValueSnak) al.get(1)).getDatavalue()).toString();
      int beginIndex = info.indexOf('Q');
      int endIndex = info.indexOf("(");
      info = info.substring(beginIndex, endIndex - 1);
      return getLabelById(info);
    } catch (Exception e) {
      return "Not Found !";
    }
  }

  public String getTime(String query, String ID, String what) throws MediaWikiApiErrorException {
    try {
      ArrayList<Object> al = getSnak(query, ID);
      TimeValue date = (TimeValue) ((JacksonValueSnak) al.get(1)).getDatavalue();
      String data = "";
      switch (what) {
        case "year":
          data = String.valueOf(date.getYear());
          break;
        case "month":
          data = String.valueOf(date.getMonth());
          break;
        case "day":
          data = String.valueOf(date.getDay());
          break;
        case "hour":
          data = String.valueOf(date.getHour());
          break;
        case "minute":
          data = String.valueOf(date.getMinute());
          break;
        case "second":
          data = String.valueOf(date.getSecond());
          break;
        case "before":
          data = String.valueOf(date.getBeforeTolerance());
          break;
        case "after":
          data = String.valueOf(date.getAfterTolerance());
          break;
        default:
          data = "ERROR";

      }
      return data;
    }

    catch (Exception e) {
      return "Not a TimeValue !";
    }
  }

  public String getUrl(String query, String ID) throws MediaWikiApiErrorException {
    try {
      return (((JacksonValueSnak) getSnak(query, ID).get(1)).getDatavalue()).toString();
    } catch (Exception e) {
      return "Not Found !";
    }
  }

  public String getQuantity(String query, String ID) throws MediaWikiApiErrorException {
    try {
      ArrayList<Object> al = getSnak(query, ID);
      QuantityValue data = (QuantityValue) (((JacksonValueSnak) al.get(1)).getDatavalue());
      String info = String.valueOf(data.getNumericValue());
      String unit = data.toString();
      int beginIndex = unit.indexOf('Q');
      if (beginIndex != -1) {
        unit = unit.substring(beginIndex);
        info += " " + getLabelById(unit);
      }
      return info;
    } catch (Exception e) {
      return "Not Found !";
    }
  }

  public String getMonolingualValue(String query, String ID) throws MediaWikiApiErrorException {
    try {
      return (((JacksonValueSnak) getSnak(query, ID).get(1)).getDatavalue()).toString();
    } catch (Exception e) {
      return "Not Found !";
    }
  }

  // TODO Add a function to compare (is there the word "fruit" in the banana's
  // document ? )

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(WikiDataFetcher.class.getCanonicalName());
    meta.addDescription("This service grab data from wikidata website");
    meta.addCategory("intelligence");
    meta.setSponsor("beetlejuice");
    meta.addDependency("org.wikidata.wdtk", "0.8.0-SNAPSHOT");
    meta.addDependency("org.apache.commons.httpclient", "4.5.2");
    meta.addDependency("org.apache.commons.commons-lang3", "3.3.2");
    meta.addDependency("com.fasterxml.jackson.core", "2.5.0");
    meta.setCloudService(true);
    return meta;
  }

}

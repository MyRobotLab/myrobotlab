/**
 * Azure Translator by Microsoft - Service
 * 
 * @author Giovanni Mirulla (Papaouitai), thanks GroG and kwatters
 * 
 *         References : https://github.com/boatmeme/microsoft-translator-java-api 
 */
package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

import com.memetix.mst.detect.Detect;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;

public class AzureTranslator extends Service implements TextListener, TextPublisher {

  private static final long serialVersionUID = 1L;

  String toLanguage = "it";
  String fromLanguage = null;
  public final static Logger log = LoggerFactory.getLogger(AzureTranslator.class);

  public static void main(String[] args) throws Exception {
    LoggingFactory.init(Level.INFO);
    try {

      AzureTranslator translator = (AzureTranslator) Runtime.start("translator", "AzureTranslator");
      Runtime.start("gui", "GUIService");
      log.info("Translator service instance: {}", translator);

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public AzureTranslator(String n) {
    super(n);
  }

  public String translate(String toTranslate) throws Exception {
    String translatedText = null;
    if (fromLanguage == null) {
      translatedText = Translate.execute(toTranslate, Language.AUTO_DETECT, Language.fromString(toLanguage));
    } else {
      translatedText = Translate.execute(toTranslate, Language.fromString(fromLanguage), Language.fromString(toLanguage));
    }
    return translatedText;
  }

  public Language detectLanguage(String toDetect) throws Exception {
    Language detectedLanguage = Detect.execute(toDetect);
    return detectedLanguage;
  }

  public void setCredentials(String clientID, String clientSecret) {
    Translate.setClientId(clientID);
    Translate.setClientSecret(clientSecret);
    Detect.setClientId(clientID);
    Detect.setClientSecret(clientSecret);
  }

  public void fromLanguage(String from) {
    fromLanguage = from;
  }

  public void toLanguage(String to) {
    toLanguage = to;
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

    ServiceType meta = new ServiceType(AzureTranslator.class.getCanonicalName());
    meta.addDescription("interface to Azure translation services");
    meta.addCategory("translation", "cloud", "ai");
    meta.addDependency("com.azure.translator", "0.6.2");
    return meta;
  }

  @Override
  public String publishText(String text) {
    return text;
  }

  @Override
  public void addTextListener(TextListener service) {
    addListener("publishText", service.getName(), "onText");
  }

  @Override
  public void onText(String text) {
      String cleanText;
      try {
        cleanText = translate(text);
        invoke("publishText", cleanText);
      } catch (Exception e) {
        log.error("Unable to translate text! {} {}", text, e);
      }
  }

}

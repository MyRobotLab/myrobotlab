package org.myrobotlab.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.GoogleTranslateConfig;
import org.slf4j.Logger;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

public class GoogleTranslate extends Service<GoogleTranslateConfig>
{

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(GoogleTranslate.class);

  public GoogleTranslate(String n, String id) {
    super(n, id);
  }

  public String translate(String text) throws FileNotFoundException, IOException {
    // Set the path to the Google Cloud service account key file
    String credentialsFilePath = "/path/to/key.json";

    // Create a new instance of GoogleCredentials using the key file
    GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsFilePath));

    // Create a new instance of TranslateOptions using the credentials
    TranslateOptions translateOptions = TranslateOptions.newBuilder().setCredentials(credentials).build();

    // Create a new instance of Translate using the translateOptions
    Translate translate = translateOptions.getService();

    // Define the text to be translated
    // String text = "Bonjour tout le monde";

    // Define the target language
    String targetLanguage = "en";

    // Translate the text
    Translation translation = translate.translate(text, Translate.TranslateOption.targetLanguage(targetLanguage));

    String translated = translation.getTranslatedText();

    // Print the translated text
    System.out.printf("Input Text: %s%nTranslated Text: %s%n", text, translated);

    return translated;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("template", "_TemplateService");
      Runtime.start("servo", "Servo");
      Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}

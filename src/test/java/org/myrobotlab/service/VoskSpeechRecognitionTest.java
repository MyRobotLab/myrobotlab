package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Test;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.Zip;
import org.myrobotlab.test.AbstractTest;

/**
 * Unit tests that do not require a microphone or native Vosk libraries.
 */
public class VoskSpeechRecognitionTest extends AbstractTest {

  @Test
  public void testCatalogAndPaths() throws Exception {
    VoskSpeechRecognition ear = (VoskSpeechRecognition) Runtime.start("voskTest", "VoskSpeechRecognition");
    assertNotNull(ear);

    Map<String, String> catalog = ear.getModelCatalog();
    assertTrue(catalog.containsKey("vosk-model-small-en-us-0.15"));
    assertTrue(catalog.get("vosk-model-small-en-us-0.15").contains("English (US)"));

    List<VoskSpeechRecognition.ModelInfo> available = ear.getAvailableModels();
    assertNotNull(available);
    assertTrue(available.size() >= 70);
    VoskSpeechRecognition.ModelInfo first = available.get(0);
    assertEquals("vosk-model-small-en-us-0.15", first.name);
    assertEquals("English (US)", first.language);
    assertEquals("40M", first.size);
    assertNotNull(first.description);
    assertTrue(first.label.contains("English (US)"));
    assertTrue(first.label.contains(first.name));

    // larger French / German / etc. models from alphacephei.com/vosk/models
    assertTrue(catalog.containsKey("vosk-model-fr-0.22"));
    assertTrue(catalog.containsKey("vosk-model-fr-0.6-linto-2.2.0"));
    assertTrue(catalog.containsKey("vosk-model-de-0.21"));
    assertTrue(catalog.containsKey("vosk-model-es-0.42"));
    assertTrue(catalog.containsKey("vosk-model-it-0.22"));
    assertTrue(catalog.containsKey("vosk-model-hi-0.22"));
    assertTrue(catalog.containsKey("vosk-model-pt-fb-v0.1.1-20220516_2113"));

    assertEquals("vosk-model-small-en-us-0.15", ear.getDefaultModelForLocale("en-US"));
    assertEquals("vosk-model-small-de-0.15", ear.getDefaultModelForLocale("de-DE"));
    assertEquals("vosk-model-small-fr-0.22", ear.getDefaultModelForLocale("fr"));

    String path = ear.getModelPath("vosk-model-small-en-us-0.15");
    assertNotNull(path);
    assertTrue(path.contains("vosk-model-small-en-us-0.15"));
    assertTrue(path.contains("VoskSpeechRecognition"));

    assertFalse(ear.isModelInstalled("definitely-not-a-real-model-xyz"));
    assertNotNull(ear.getLocales());
    assertTrue(ear.getLocales().containsKey("en-US"));

    Runtime.release("voskTest");
  }

  @Test
  public void testValidModelDirDetectionAndInstalledList() throws Exception {
    VoskSpeechRecognition ear = (VoskSpeechRecognition) Runtime.start("voskTest2", "VoskSpeechRecognition");

    File fake = new File(ear.getModelsRoot(), "fake-model-unit-test");
    fake.mkdirs();
    new File(fake, "am").mkdirs();
    new File(fake, "am/final.mdl").createNewFile();

    assertTrue(VoskSpeechRecognition.isValidModelDir(fake));
    assertTrue(ear.isModelInstalled("fake-model-unit-test"));

    List<String> installed = ear.getInstalledModels();
    assertTrue(installed.contains("fake-model-unit-test"));

    // cleanup
    new File(fake, "am/final.mdl").delete();
    new File(fake, "am").delete();
    fake.delete();

    Runtime.release("voskTest2");
  }

  @Test
  public void testZipExtractLayout() throws Exception {
    VoskSpeechRecognition ear = (VoskSpeechRecognition) Runtime.start("voskTest3", "VoskSpeechRecognition");
    String modelName = "vosk-fake-zip-model";
    File downloads = new File(FileIO.gluePaths(ear.getDataDir(), VoskSpeechRecognition.DOWNLOADS_DIR));
    downloads.mkdirs();
    File zip = new File(downloads, modelName + ".zip");

    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
      zos.putNextEntry(new ZipEntry(modelName + "/am/"));
      zos.closeEntry();
      zos.putNextEntry(new ZipEntry(modelName + "/am/final.mdl"));
      zos.write("fake".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
      zos.putNextEntry(new ZipEntry(modelName + "/conf/"));
      zos.closeEntry();
      zos.putNextEntry(new ZipEntry(modelName + "/conf/model.conf"));
      zos.write("beam=10".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }

    File modelsRoot = new File(ear.getModelsRoot());
    modelsRoot.mkdirs();
    Zip.unzip(zip.getAbsolutePath(), modelsRoot.getAbsolutePath());

    assertTrue(ear.isModelInstalled(modelName));

    // cleanup tree
    File modelDir = new File(modelsRoot, modelName);
    deleteTree(modelDir);
    zip.delete();

    Runtime.release("voskTest3");
  }

  private static void deleteTree(File f) {
    if (f == null || !f.exists()) {
      return;
    }
    if (f.isDirectory()) {
      File[] kids = f.listFiles();
      if (kids != null) {
        for (File k : kids) {
          deleteTree(k);
        }
      }
    }
    f.delete();
  }

}

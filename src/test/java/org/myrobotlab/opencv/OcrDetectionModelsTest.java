package org.myrobotlab.opencv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;
import org.myrobotlab.test.AbstractTest;

public class OcrDetectionModelsTest extends AbstractTest {

  @Test
  public void catalogContainsEastAndDbNet() {
    assertNotNull(OcrDetectionModels.get(OcrDetectionModels.EAST));
    assertNotNull(OcrDetectionModels.get(OcrDetectionModels.DB_IC15_R18));
    assertEquals("db", OcrDetectionModels.get(OcrDetectionModels.DB_IC15_R18).detector);
    assertEquals("east", OcrDetectionModels.get(OcrDetectionModels.EAST).detector);
    assertTrue(OcrDetectionModels.get(OcrDetectionModels.DB_IC15_R18).urls.length > 0);
  }

  @Test
  public void normalizeLegacyDetectorNames() {
    assertEquals(OcrDetectionModels.EAST, OcrDetectionModels.normalize(null, "east"));
    assertEquals(OcrDetectionModels.DB_IC15_R18, OcrDetectionModels.normalize(null, "db"));
    assertEquals(OcrDetectionModels.NONE, OcrDetectionModels.normalize(null, "none"));
    assertEquals(OcrDetectionModels.DB_IC15_R50, OcrDetectionModels.normalize("db_ic15_r50", "east"));
    assertEquals(OcrDetectionModels.DB_IC15_R18, OcrDetectionModels.normalize("east", "db"));
  }

  @Test
  public void resolveDoesNotDownloadWhenDisabled() throws Exception {
    OcrDetectionModels.Spec spec = OcrDetectionModels.get(OcrDetectionModels.DB_IC15_R18);
    File resolved = OcrDetectionModels.resolve(spec, null, false);
    if (resolved != null) {
      assertTrue(resolved.isFile());
      assertTrue(resolved.length() >= spec.minBytes);
    }
  }

  @Test
  public void findLocalUsesOverrideFile() throws Exception {
    OcrDetectionModels.Spec spec = OcrDetectionModels.get(OcrDetectionModels.DB_IC15_R18);
    File tmp = File.createTempFile("ocr-db-test-", ".onnx");
    byte[] bytes = new byte[(int) Math.min(spec.minBytes, Integer.MAX_VALUE)];
    Files.write(tmp.toPath(), bytes);
    try {
      File found = OcrDetectionModels.findLocal(spec, tmp.getAbsolutePath());
      assertNotNull(found);
      assertEquals(tmp.getAbsoluteFile(), found.getAbsoluteFile());
    } finally {
      tmp.delete();
    }
  }

  @Test
  public void noneHasNoFile() {
    assertNull(OcrDetectionModels.findLocal(OcrDetectionModels.get(OcrDetectionModels.NONE), null));
    assertTrue(OcrDetectionModels.isInstalled(OcrDetectionModels.NONE));
  }

  @Test
  public void missingDbIsNotInstalledUnlessCached() {
    OcrDetectionModels.Spec spec = OcrDetectionModels.get(OcrDetectionModels.DB_IC15_R18);
    File cached = OcrDetectionModels.cacheFile(spec);
    if (cached == null || !cached.isFile()) {
      assertFalse(OcrDetectionModels.isInstalled(OcrDetectionModels.DB_IC15_R18));
    }
  }
}

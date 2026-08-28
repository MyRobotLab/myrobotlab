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

public class OpenCvZooModelsTest extends AbstractTest {

  @Test
  public void catalogContainsYunetsYoloAndTrackers() {
    assertNotNull(OpenCvZooModels.get(OpenCvZooModels.YUNET));
    assertNotNull(OpenCvZooModels.get(OpenCvZooModels.YOLOX));
    assertNotNull(OpenCvZooModels.get(OpenCvZooModels.NANOTRACK_BACKBONE));
    assertNotNull(OpenCvZooModels.get(OpenCvZooModels.NANOTRACK_HEAD));
    assertNotNull(OpenCvZooModels.get(OpenCvZooModels.VITTRACK));
    assertTrue(OpenCvZooModels.get(OpenCvZooModels.YUNET).urls.length > 0);
    assertTrue(OpenCvZooModels.get(OpenCvZooModels.YOLOX).minBytes > 1_000_000L);
  }

  @Test
  public void resolveDoesNotDownloadWhenDisabled() throws Exception {
    OpenCvZooModels.Spec spec = OpenCvZooModels.get(OpenCvZooModels.YOLOX);
    File resolved = OpenCvZooModels.resolve(spec, null, false);
    if (resolved != null) {
      assertTrue(resolved.isFile());
      assertTrue(resolved.length() >= spec.minBytes);
    }
  }

  @Test
  public void findLocalUsesOverrideFile() throws Exception {
    OpenCvZooModels.Spec spec = OpenCvZooModels.get(OpenCvZooModels.YUNET);
    File tmp = File.createTempFile("yunet-test-", ".onnx");
    byte[] bytes = new byte[(int) Math.min(spec.minBytes, Integer.MAX_VALUE)];
    Files.write(tmp.toPath(), bytes);
    try {
      File found = OpenCvZooModels.findLocal(spec, tmp.getAbsolutePath());
      assertNotNull(found);
      assertEquals(tmp.getAbsoluteFile(), found.getAbsoluteFile());
    } finally {
      tmp.delete();
    }
  }

  @Test
  public void unknownIdIsNull() {
    assertNull(OpenCvZooModels.get("not-a-model"));
  }

  @Test
  public void missingYoloIsNotInstalledUnlessCached() {
    OpenCvZooModels.Spec spec = OpenCvZooModels.get(OpenCvZooModels.YOLOX);
    File cached = OpenCvZooModels.cacheFile(spec);
    if (cached == null || !cached.isFile()) {
      assertFalse(OpenCvZooModels.isInstalled(OpenCvZooModels.YOLOX));
    }
  }
}

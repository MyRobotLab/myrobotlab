package org.myrobotlab.opencv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.myrobotlab.service.OpenCV;

public class OpenCVFilterCatalogTest {

  @Test
  public void everyPossibleFilterHasDescriptionUsageAndDependencies() {
    Map<String, OpenCVFilterInfo> catalog = OpenCVFilterCatalog.getAll();
    List<String> missing = new ArrayList<>();
    List<String> incomplete = new ArrayList<>();
    for (String type : OpenCV.POSSIBLE_FILTERS) {
      OpenCVFilterInfo info = catalog.get(type);
      if (info == null) {
        missing.add(type);
        continue;
      }
      if (isBlank(info.description) || isBlank(info.usage) || isBlank(info.dependencies) || isBlank(info.type)) {
        incomplete.add(type);
      }
    }
    assertTrue("catalog missing filters: " + missing, missing.isEmpty());
    assertTrue("catalog incomplete filters: " + incomplete, incomplete.isEmpty());
    assertEquals("catalog should not have extra types beyond POSSIBLE_FILTERS", OpenCV.POSSIBLE_FILTERS.length, catalog.size());
  }

  @Test
  public void implementedFiltersDeclareStaticCatalogInfoOnTheFilterClass() throws Exception {
    List<String> missingMethod = new ArrayList<>();
    for (String type : OpenCV.POSSIBLE_FILTERS) {
      Class<?> clazz;
      try {
        clazz = Class.forName("org.myrobotlab.opencv.OpenCVFilter" + type);
      } catch (ClassNotFoundException e) {
        continue;
      }
      try {
        Method m = clazz.getDeclaredMethod("catalogInfo");
        assertTrue(type + " catalogInfo must be static", Modifier.isStatic(m.getModifiers()));
        assertEquals(OpenCVFilterInfo.class, m.getReturnType());
        OpenCVFilterInfo info = (OpenCVFilterInfo) m.invoke(null);
        assertNotNull(type, info);
        assertEquals(type, info.type);
      } catch (NoSuchMethodException e) {
        missingMethod.add(type);
      }
    }
    assertTrue("filter classes missing catalogInfo(): " + missingMethod, missingMethod.isEmpty());
  }

  @Test
  public void lookupByType() {
    OpenCVFilterInfo yolo = OpenCVFilterCatalog.get("YoloOnnx");
    assertNotNull(yolo);
    assertEquals("YoloOnnx", yolo.type);
    assertTrue(yolo.dependencies.toLowerCase().contains("onnx"));
    assertTrue(yolo.usage.toLowerCase().contains("download") || yolo.usage.toLowerCase().contains("zoo"));
    assertEquals(yolo.type, OpenCVFilterYoloOnnx.catalogInfo().type);
  }

  @Test
  public void jsonResourceListsEveryFilter() throws Exception {
    String json = OpenCVFilterCatalog.toJson();
    for (String type : OpenCV.POSSIBLE_FILTERS) {
      assertTrue("json missing " + type, json.contains("\"" + type + "\""));
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

}

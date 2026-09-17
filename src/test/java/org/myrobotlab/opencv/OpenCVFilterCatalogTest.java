package org.myrobotlab.opencv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Catalog tests must not require JavaCV natives. CI (agent-tests) has no
 * OpenCV native libraries; {@code Class.forName} of some filters used to run
 * {@code Loader.load} in a static initializer and fail the suite.
 */
public class OpenCVFilterCatalogTest {

  @Test
  public void everyPossibleFilterHasDescriptionUsageAndDependencies() {
    Map<String, OpenCVFilterInfo> catalog = OpenCVFilterCatalog.getAll();
    List<String> missing = new ArrayList<>();
    List<String> incomplete = new ArrayList<>();
    for (String type : OpenCVFilterCatalog.POSSIBLE_FILTERS) {
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
    assertEquals("catalog should not have extra types beyond POSSIBLE_FILTERS", OpenCVFilterCatalog.POSSIBLE_FILTERS.length,
        catalog.size());
  }

  @Test
  public void implementedFiltersDeclareStaticCatalogInfoOnTheFilterClass() throws Exception {
    List<String> missingMethod = new ArrayList<>();
    ClassLoader loader = OpenCVFilterCatalogTest.class.getClassLoader();
    for (String type : OpenCVFilterCatalog.POSSIBLE_FILTERS) {
      Class<?> clazz;
      try {
        clazz = Class.forName("org.myrobotlab.opencv.OpenCVFilter" + type, false, loader);
      } catch (ClassNotFoundException e) {
        continue;
      }
      try {
        Method m = clazz.getDeclaredMethod("catalogInfo");
        assertTrue(type + " catalogInfo must be static", Modifier.isStatic(m.getModifiers()));
        assertEquals(OpenCVFilterInfo.class, m.getReturnType());
        try {
          OpenCVFilterInfo info = (OpenCVFilterInfo) m.invoke(null);
          assertNotNull(type, info);
          assertEquals(type, info.type);
        } catch (InvocationTargetException e) {
          if (!isNativeInitFailure(e.getCause())) {
            throw e;
          }
        }
      } catch (ExceptionInInitializerError | NoClassDefFoundError | UnsatisfiedLinkError e) {
        // Method is present; this environment cannot initialize the filter class.
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
    OpenCVFilterInfo gray = OpenCVFilterCatalog.get("Gray");
    assertNotNull(gray);
    assertEquals("Gray", gray.type);
    assertNotNull(gray.description);
  }

  @Test
  public void jsonResourceListsEveryFilter() throws Exception {
    String json = OpenCVFilterCatalog.toJson();
    for (String type : OpenCVFilterCatalog.POSSIBLE_FILTERS) {
      assertTrue("json missing " + type, json.contains("\"" + type + "\""));
    }
  }

  @Test
  public void trackerNativeFieldsAreTransient() throws Exception {
    Class<?> clazz = Class.forName("org.myrobotlab.opencv.OpenCVFilterTracker", false,
        OpenCVFilterCatalogTest.class.getClassLoader());
    for (String name : new String[] { "mat", "tracker", "boundingBox" }) {
      Field field = clazz.getDeclaredField(name);
      assertTrue(name + " must be transient so WebGui broadcastState() does not serialize JavaCV pointers",
          Modifier.isTransient(field.getModifiers()));
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static boolean isNativeInitFailure(Throwable cause) {
    while (cause != null) {
      if (cause instanceof UnsatisfiedLinkError || cause instanceof NoClassDefFoundError
          || cause instanceof ExceptionInInitializerError) {
        return true;
      }
      String name = cause.getClass().getName();
      if (name.contains("UnsatisfiedLink") || name.contains("javacpp")) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

}

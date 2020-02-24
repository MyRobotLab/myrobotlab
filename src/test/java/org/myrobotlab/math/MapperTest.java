package org.myrobotlab.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.myrobotlab.test.AbstractTest;

public class MapperTest extends AbstractTest {

  @Test
  public void testCalc() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    double testValue = 15;
    double expectedResult = 150;
    MapperLinear myMapper = new MapperLinear(10, 18, 100, 180);
    double actualResult = myMapper.calcOutput(testValue);
    assertEquals("calc(15) should return 150", expectedResult, actualResult, 3);
  }

  @Test
  public void testGetMaxX() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    int testValue = 18;
    MapperLinear myMapper = new MapperLinear(10, testValue, 100, 180);
    assertEquals("getMaxX should return second parameter", myMapper.getMaxX(), testValue, 3);
  }

  @Test
  public void testGetMaxY() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    int testValue = 180;
    MapperLinear myMapper = new MapperLinear(10, 18, 100, testValue);
    assertEquals("getMaxY should return fourth parameter", myMapper.getMaxY(), testValue, 3);
  }


  @Test
  public void testGetMinX() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    int testValue = 10;
    MapperLinear myMapper = new MapperLinear(testValue, 18, 100, 180);
    assertEquals("getMinX should return first parameter", myMapper.getMinX(), testValue, 3);
  }

  @Test
  public void testGetMinY() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    int testValue = 100;
    MapperLinear myMapper = new MapperLinear(10, 18, testValue, 180);
    assertEquals("getMinY should return third parameter", myMapper.getMinY(), testValue, 3);
  }

  @Test
  public void testIsInverted() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    MapperLinear myMapper = new MapperLinear(10, 18, 100, 180);
    boolean inverted = myMapper.isInverted();
    assertFalse("Test should return inverted = false", inverted);
    myMapper.setInverted(true);
    inverted = myMapper.isInverted();
    assertTrue("Test should return inverted = true", inverted);
  }

  @Test
  public void testMapper() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    MapperLinear myMapper = new MapperLinear(10, 18, 100, 180);
    // TODO: validate something here.
    myMapper.getMaxX();
  }

}
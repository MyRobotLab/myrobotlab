package org.myrobotlab.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MapperTest {

  @Test
  public void testMapper() {
    Mapper myMapper = new Mapper(10, 18, 100, 180);
    // TODO: validate something here.
    myMapper.getMaxX();
  }

  @Test
  public void testCalc() {
    double testValue = 15;
    double expectedResult = 150;
    Mapper myMapper = new Mapper(10, 18, 100, 180);
    double actualResult = myMapper.calcOutput(testValue);
    assertEquals("calc(15) should return 150", expectedResult, actualResult, 3);
  }

  @Test
  public void testCalcIntInt() {

  }

  @Test
  public void testCalcIntDouble() {

  }

  @Test
  public void testGetMaxX() {
    double testValue = 18;
    Mapper myMapper = new Mapper(10, testValue, 100, 180);
    assertEquals("getMaxX should return second parameter", myMapper.getMaxX(), testValue, 3);
  }

  @Test
  public void testGetMaxY() {
    double testValue = 180;
    Mapper myMapper = new Mapper(10, 18, 100, testValue);
    assertEquals("getMaxY should return fourth parameter", myMapper.getMaxY(), testValue, 3);
  }

  @Test
  public void testGetMinX() {
    double testValue = 10;
    Mapper myMapper = new Mapper(testValue, 18, 100, 180);
    assertEquals("getMinX should return first parameter", myMapper.getMinX(), testValue, 3);
  }

  @Test
  public void testGetMinY() {
    double testValue = 100;
    Mapper myMapper = new Mapper(10, 18, testValue, 180);
    assertEquals("getMinY should return third parameter", myMapper.getMinY(), testValue, 3);
  }

  @Test
  public void testIsInverted() {
    Mapper myMapper = new Mapper(10, 18, 100, 180);
    boolean inverted = myMapper.isInverted();
    assertFalse("Test should return inverted = false", inverted);
    myMapper.setInverted(true);
    inverted = myMapper.isInverted();
    assertTrue("Test should return inverted = true", inverted);
  }

  @Test
  public void testGetMinOutput() {
    double testValue = 100;
    Mapper myMapper = new Mapper(10, 18, testValue, 180);
    assertEquals("getMinOutput should return third parameter", myMapper.getMinOutput(), testValue, 3);
  }

  @Test
  public void testGetMaxOutput() {
    double testValue = 180;
    Mapper myMapper = new Mapper(10, 18, 100, testValue);
    assertEquals("getMaxOutput should return fourth parameter", myMapper.getMaxOutput(), testValue, 3);
  }

}

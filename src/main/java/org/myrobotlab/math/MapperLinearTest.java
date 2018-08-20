package org.myrobotlab.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.interfaces.Mapper;
import org.slf4j.Logger;

public class MapperLinearTest {

  public final static Logger log = LoggerFactory.getLogger(MapperLinearTest.class);

  @Test
  public void testCalcOutput() {

    Mapper mapper = new MapperLinear();
    mapper.map(-1.0, 1.0, -1.0, 1.0);
    double result = mapper.calcOutput(0.5);
    assertEquals(0.5, result, 0);
    
    mapper.setMap(-1.0, 1.0, 1.0, -1.0);
    assertEquals(-0.5, mapper.calcOutput(0.5), 0);

    mapper.map(-1.0, 1.0, 1.0, -1.0);
    assertEquals(-0.5, mapper.calcOutput(0.5), 0);
    mapper.setMinMaxOutput(null, null);
    assertEquals(-0.5, mapper.calcOutput(0.5), 0);
  }

  @Test
  public void testSetInverted() {
    Double result;
    MapperLinear mapper = new MapperLinear();

    // divide by zero error
    mapper.map(-1.0, -1.0, 3.0, 3.0);
    result = mapper.calcOutput(0.5);
    assertEquals(0.0, result, 0);

    mapper.reset();

    mapper.map(-1.0, 1.0, -1.0, 1.0);
    result = mapper.calcOutput(0.5);
    assertEquals(0.5, result, 0);
    mapper.setInverted(true);
    result = mapper.calcOutput(0.5);
    assertEquals(-0.5, result, 0);
    mapper.setInverted(false);
    result = mapper.calcOutput(0.5);
    assertEquals(0.5, result, 0);

    // checking input min max affect clipping
    mapper.setMinMaxInput(-0.8, 0.8);
    assertEquals(0.8, mapper.calcOutput(1.0), 0);
    assertEquals(-0.8, mapper.calcOutput(-1.0), 0);

    // checking output min max affect clipping
    mapper.setMinMaxOutput(-0.5, 0.5);
    assertEquals(0.5, mapper.calcOutput(1.0), 0);
    assertEquals(-0.5, mapper.calcOutput(-1.0), 0);

    // what is the behavior of min/max output when inverted ?
    // output inverts
    mapper.reset();
    mapper.map(-1.0, 1.0, -1.0, 1.0);
    mapper.setInverted(true);
    mapper.setMinMaxOutput(-0.6, 0.7);
    assertEquals(-0.6, mapper.calcOutput(1.0), 0);
    assertEquals(-0.6, mapper.calcOutput(0.7), 0);
    assertEquals(0.5, mapper.calcOutput(-0.5), 0);
    assertEquals(-0.6, mapper.calcOutput(1.5), 0);
    mapper.setMinMaxInput(-0.5, 0.5);
    assertEquals(-0.5, mapper.calcOutput(0.7), 0);

    // this is what a non-inverted mapper does with
    // the same values
    MapperLinear mapper2 = new MapperLinear();
    mapper2.map(-1.0, 1.0, -1.0, 1.0);
    // mapper2.setInverted(true);
    mapper2.setMinMaxOutput(-0.6, 0.7);
    assertEquals(0.7, mapper2.calcOutput(1.0), 0);
    assertEquals(0.7, mapper2.calcOutput(0.7), 0);
    assertEquals(-0.5, mapper2.calcOutput(-0.5), 0);
    assertEquals(0.7, mapper2.calcOutput(1.5), 0);
    mapper2.setMinMaxInput(-0.5, 0.5);
    assertEquals(0.5, mapper2.calcOutput(0.7), 0);

    // a "double inverted" better do the same as above !
    mapper.setInverted(false);
    mapper.setMinMaxInput(null, null); // clear previous restriction
    // mapper.map(-1.0, 1.0, -1.0, 1.0);
    // mapper.setInverted(true);
    mapper.setMinMaxOutput(-0.6, 0.7);
    assertEquals(0.7, mapper.calcOutput(1.0), 0);
    assertEquals(0.7, mapper.calcOutput(0.7), 0);
    assertEquals(-0.5, mapper.calcOutput(-0.5), 0);
    assertEquals(0.7, mapper.calcOutput(1.5), 0);
    mapper.setMinMaxInput(-0.5, 0.5);
    assertEquals(0.5, mapper.calcOutput(0.7), 0);

  }

  @Test
  public void testSetMinMaxOutput() {

    MapperLinear mapper = new MapperLinear();

    mapper.map(-1.0, 1.0, -1.0, 1.0);
    mapper.setMinMaxOutput(7.0, 13.0);
    assertEquals(7.0, mapper.calcOutput(8.0), 0);
    assertEquals(7.0, mapper.calcOutput(20.0), 0);
    assertEquals(7.0, mapper.calcOutput(-3.0), 0);

    mapper.setMinMaxOutput(7.0, null);
    assertEquals(7.0, mapper.calcOutput(100.0), 0);
    // mapper.reset();
    // remove all input/output restrictions
    mapper.setMinMaxOutput(null, null);
    mapper.setMinMaxInput(null, null);
    assertEquals(-100.0, mapper.calcOutput(-100.0), 0);
  }

  @Test
  public void testSetMinMaxInput() {
    MapperLinear mapper = new MapperLinear();

    mapper.map(-1.0, 1.0, -1.0, 1.0);
    mapper.setMinMaxInput(7.0, 13.0);
    // because output min/max was set in the original map to -1.0 1.0
    assertEquals(1.0, mapper.calcOutput(8.0), 0);
    
    // removing original output min/max
    mapper.setMinMaxOutput(null, null);
    assertEquals(8.0, mapper.calcOutput(8.0), 0);
    assertEquals(13.0, mapper.calcOutput(20.0), 0);
    assertEquals(7.0, mapper.calcOutput(-3.0), 0);

    mapper.setMinMaxInput(7.0, null);
    assertEquals(100.0, mapper.calcOutput(100.0), 0);
    mapper.reset();
    mapper.map(-1.0, 1.0, -100.0, 100.0);
    assertEquals(-100.0, mapper.calcOutput(-100.0), 0);
  }

  @Test
  public void testMerge() {
    
    MapperLinear control = new MapperLinear();
    
    // the "preferred" default of a motor control
    // it has no idea what controller it will interface with - but "wants" to have a standard
    // front end map of -1.0 to 1.0 
    control.map(-1.0, 1.0, null, null);
    
    // sabertooth
    MapperLinear controller = new MapperLinear(-1.0, 1.0, -127.0, 127.0);
    
    control.merge(controller);
    
    assertEquals( 127.0, control.getMaxOutput(), 0);
    assertEquals(-127.0, control.getMinOutput(), 0);
    
    assertEquals(0.0, control.calcOutput(null), 0);
    assertEquals(127.0, control.calcOutput(3.0), 0);
    assertEquals(-127.0, control.calcOutput(-3.0), 0);
    
    // use case user has to limit output - important !!!
    control.setMinMaxOutput(-34.0, 38.0);
    assertEquals(38.0, control.calcOutput(1.0), 0);
    assertEquals(-34.0, control.calcOutput(-1.0), 0);
    log.info("here");
    
    // TODO - get controller map for motor x ... it should == control map
    
    // TODO check for preservation of motor control limits ...
    
    
  }

  @Test
  public void testMap() {
    // default will set limits IF NONE HAVE BEEN SET
    // but preserve them if they have been previously set
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init("INFO");
      boolean quitNow = false;

      if (quitNow) {
        return;
      }

      // run junit as java app
      JUnitCore junit = new JUnitCore();
      Result result = junit.run(MapperLinearTest.class);
      log.info("Result failures: {}", result.getFailureCount());
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}

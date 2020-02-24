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
    
    
    assertEquals( 0.3, mapper.calcInput( mapper.calcOutput(0.3)), 0.01);
    assertEquals( -2.3, mapper.calcInput( mapper.calcOutput(-2.3)), 0.01);
    
    
    mapper.setMinMax(-1.0, 1.0);
    assertEquals( -1.0, mapper.calcInput( mapper.calcOutput(-2.3)), 0.01);
 
    mapper.setInverted(true);
    
    // -1.0 1.0 - 1.0 -1.0 calc output => 1.0
    //                     calc input 1.0 => -1.0
    
    assertEquals(-1.0, mapper.calcInput( mapper.calcOutput(-2.3)), 0.01);
    
    assertEquals(1.0, mapper.calcInput(-7.0), 0.01);
    
    mapper.map(-1.0, 1.0, 10.0, -10.0 );
    log.info("mapper {}", mapper);
    mapper.resetLimits();
    assertEquals( -22.0, mapper.calcOutput(-2.2), 0.0);
    assertEquals( -1.1, mapper.calcInput(-11.0), 0.0);
    mapper.setMinMax(-1.0, 1.0);
    assertEquals( -1.0, mapper.calcInput(-11.0), 0.0);
  }

  @Test
  public void testSetInverted() {
    Double result;
    MapperLinear mapper = new MapperLinear();

    // divide by zero error
    mapper.map(-1.0, -1.0, 3.0, 3.0);
    result = mapper.calcOutput(0.5);
    assertEquals(Double.NaN, result, 0);

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

   
    // what is the behavior of min/max output when inverted ?
    // output inverts
    mapper.reset();
    mapper.map(-1.0, 1.0, -1.0, 1.0);
    mapper.setMinMax(-0.6, 0.6);
    mapper.setInverted(true);
    
    assertEquals(-0.6, mapper.calcOutput(1.0), 0);
    assertEquals(-0.6, mapper.calcOutput(0.7), 0);
    assertEquals(0.5, mapper.calcOutput(-0.5), 0);
    assertEquals(-0.6, mapper.calcOutput(1.5), 0);

    // this is what a non-inverted mapper does with
    // the same values
    MapperLinear mapper2 = new MapperLinear();
    mapper2.map(-1.0, 1.0, -1.0, 1.0);
    // mapper2.setInverted(true);
    mapper2.setMinMax(-0.7, 0.7);
   
    assertEquals(0.7, mapper2.calcOutput(1.0), 0);
    assertEquals(0.7, mapper2.calcOutput(0.7), 0);
    assertEquals(-0.5, mapper2.calcOutput(-0.5), 0);
    assertEquals(0.7, mapper2.calcOutput(1.5), 0);

    // asymmetrical
    MapperLinear mapper3 = new MapperLinear();
    mapper3.map(-1.0, 1.0, 0.0, 90.0);
    // mapper3.setLimits(min, max);
    log.info("{}", mapper3.calcOutput(0.3));
    assertEquals(58.5, mapper3.calcOutput(0.3), 0);
    log.info("{}", mapper3.calcOutput(0.7));
    assertEquals(76.5, mapper3.calcOutput(0.7), 0);
    mapper3.setInverted(true);

    log.info("{}", mapper3.calcOutput(0.3));
    assertEquals(31.499999999999996, mapper3.calcOutput(0.3), 0);
    log.info("{}", mapper3.calcOutput(0.7));
    assertEquals(13.500000000000002, mapper3.calcOutput(0.7), 0);
  }

  @Test
  public void testSetMinMaxOutput() {

    MapperLinear mapper = new MapperLinear();

    mapper.map(-1.0, 1.0, -1.0, 1.0);
    
    assertEquals(8.0, mapper.calcOutput(8.0), 0);
    assertEquals(20.0, mapper.calcOutput(20.0), 0); 
    assertEquals(-3.0, mapper.calcOutput(-3.0), 0);

    
    assertEquals(100.0, mapper.calcOutput(100.0), 0);
    
    mapper.setMinMax(0.0, 0.9);
    
    assertEquals(0, mapper.calcOutput(-0.9), 0);
    assertEquals(0.9, mapper.calcOutput(1.0), 0.01);
       // mapper.reset();
    // remove all input/output restrictions
    
  }

  @Test
  public void testMotorControl() {

    Mapper control = new MapperLinear();

    // the "preferred" default of a motor control
    // it has no idea what controller it will interface with - but "wants" to
    // have a standard
    // front end map of -1.0 to 1.0
    control.map(-1.0, 1.0, null, null);

    // sabertooth
    Mapper controller = new MapperLinear(-1.0, 1.0, -127.0, 127.0);
    controller.setMinMax(-0.9, 0.9);

    // to be done in abstract (Yay!)
    control.merge(controller);

    // verify the results of the merge
    assertEquals(0.9, control.getMax(), 0);
    assertEquals(-0.9, control.getMin(), 0);
    
    assertEquals(127.0, control.getMaxY(), 0);
    assertEquals(-127.0, control.getMinY(), 0);


    assertEquals(null, control.calcOutput(null), null);
    assertEquals(114.29999999999998, control.calcOutput(3.0), 0.01);
    assertEquals(-114.3, control.calcOutput(-3.0), 0.01);

    // invert it
    
    control.setInverted(true);
    control.setMinMax(-1.0, 1.0);
    assertEquals(-127.0, control.calcOutput(1.0), 0.01);
    
    
    assertEquals(127.0, control.calcOutput(-1.0), 0.01);
    assertEquals(-63.5, control.calcOutput(0.5), 0.01);
    assertEquals(63.5, control.calcOutput(-0.5), 0.01);

    // stretch the map
    control.map(-1.0, 1.0, -20.0, 20.0);
    assertEquals(-20.0, control.calcOutput(1.0), 0);
    assertEquals(-10.0, control.calcOutput(0.5), 0);

  
    // reverse-invert it
    control.setInverted(false);

    // limits better not change !!!
    assertEquals(-20.0, control.calcOutput(-1.0), 0);
    assertEquals(20.0, control.calcOutput(1.0), 0);
    
    
    control.map(-1.0, 1.0, -200.0, 200.0);

    // unset limits
    control.resetLimits();
 

    assertEquals(1000.0, control.calcOutput(5.0), 0);
    assertEquals(-1000.0, control.calcOutput(-5.0), 0);

    // reversed stretched map with no limits
    control.map(-1.0, 1.0, 20.0, -20.0);
    assertEquals(100.0, control.calcOutput(-5.0), 0);
    assertEquals(-100.0, control.calcOutput(5.0), 0);

    // reversed stretched map with limits
    control.map(-1.0, 1.0, 20.0, -20.0);
    control.setMinMax(-1.0, 1.0);
    assertEquals(20.0, control.calcOutput(-5.0), 0);
    assertEquals(-20.0, control.calcOutput(5.0), 0);

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

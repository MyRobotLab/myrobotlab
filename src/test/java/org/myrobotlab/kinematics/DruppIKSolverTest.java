package org.myrobotlab.kinematics;

import org.junit.Assert;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class DruppIKSolverTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(DruppIKSolverTest.class);

  @Test
  public void testDrupp() throws Exception {

    // LoggingFactory.init("WARN");

    DruppIKSolver solver = new DruppIKSolver();

    double roll = 0.0;
    double pitch = 0.0;
    double yaw = 0.0;

    double[] result = solver.solve(roll, pitch, yaw);

    log.info("Result : {}", result[0]);
    log.info("Result : {}", result[1]);
    log.info("Result : {}", result[2]);

    Assert.assertEquals(0.0, result[0], 0.01);
    Assert.assertEquals(-2.09, result[1], 0.01);
    Assert.assertEquals(2.09, result[2], 0.01);

  }

}

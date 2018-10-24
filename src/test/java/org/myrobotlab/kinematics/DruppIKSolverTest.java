package org.myrobotlab.kinematics;

import org.junit.Test;
import junit.framework.Assert;

public class DruppIKSolverTest {
  
  @Test
  public void testDrupp() throws Exception {
    DruppIKSolver solver = new DruppIKSolver();

    double roll = 0;
    double pitch = 0; 
    double yaw = 0;
    
    double[] result = solver.solve(roll, pitch, yaw);
    
    System.out.println("Result : " + result[0]);
    System.out.println("Result : " + result[1]);
    System.out.println("Result : " + result[2]);
    
   // Assert.Equals(result[0], 0.0);
    Assert.assertEquals(result[0], 0.0);
    
  }

}

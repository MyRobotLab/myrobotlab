package org.myrobotlab.kinematics;

import org.junit.Test;
import org.junit.Assert;

public class DruppIKSolverTest {
  
  @Test
  public void testDrupp() throws Exception {
    DruppIKSolver solver = new DruppIKSolver();

    double roll = 0.0;
    double pitch = 0.0; 
    double yaw = 0.0;
    
    double[] result = solver.solve(roll, pitch, yaw);
    
    System.out.println("Result : " + result[0]);
    System.out.println("Result : " + result[1]);
    System.out.println("Result : " + result[2]);
    
   
    Assert.assertEquals(0.0, result[0],  0.01);
    Assert.assertEquals(-2.09, result[1],  0.01);
    Assert.assertEquals(2.09, result[2],  0.01);

    
  }

}

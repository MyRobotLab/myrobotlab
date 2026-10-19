package org.myrobotlab.service.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.myrobotlab.math.MapperLinear;

public class InMoov2BindMapperTest {

  @Test
  public void restAngleMapsToZeroAndSlopeIsKept() {
    MapperLinear midStom = InMoov2Config.fromBind(0, 180, 50, 130, 90);
    assertEquals(0, midStom.calcOutput(90), 0.001);
    assertEquals(-40, midStom.calcOutput(0), 0.001);
    assertEquals(40, midStom.calcOutput(180), 0.001);

    MapperLinear rothead = InMoov2Config.fromBind(0, 180, 0, 180, 90);
    assertEquals(0, rothead.calcOutput(90), 0.001);
    assertEquals(-90, rothead.calcOutput(0), 0.001);

    MapperLinear index = InMoov2Config.fromBind(0, 180, -110, -179, 0);
    assertEquals(0, index.calcOutput(0), 0.001);
    assertEquals(-69, index.calcOutput(180), 0.001);
  }
}

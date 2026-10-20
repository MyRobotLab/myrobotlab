package org.myrobotlab.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.myrobotlab.service.data.ServoMove;

/**
 * Servo traffic is subscribed in the JMonkeyEngine constructor (existing InMoov
 * peers). Those callbacks must not throw before GLFW/simpleInitApp assigns the
 * root node.
 */
public class JMonkeyEngineServoGuardTest {

  @Test
  public void servoMoveBeforeSceneReadyDoesNotThrow() throws Exception {
    Runtime.getInstance();
    JMonkeyEngine jme = (JMonkeyEngine) Runtime.create("jmeServoGuard", "JMonkeyEngine");
    try {
      assertFalse(jme.isSceneReady());
      assertNull(jme.find("i01.head.jaw"));
      jme.onServoMove(new ServoMove("i01.head.jaw", 90.0, 90.0));
      jme.onServoMoveTo(null);
      jme.onEncoderData(null);
      jme.simpleInitApp(null);
      assertFalse(jme.isSceneReady());
      jme.loadDelayed(jme.getConfig());
      assertTrue(jme.runOnJmeThread(() -> {
      }, 1));
      assertNull(jme.enqueueGet(() -> null, 1, java.util.concurrent.TimeUnit.SECONDS));
    } finally {
      Runtime.release("jmeServoGuard");
    }
  }

}

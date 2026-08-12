package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * Boots a virtual InMoov (JMonkeyEngine simulator) with InverseKinematics3D and
 * wires {@code publishJointAngles} so solved joint angles drive the arm servos
 * (and therefore the simulated model).
 *
 * <pre>
 * Run: org.myrobotlab.service.VirtualInMoovIkDemo
 * </pre>
 *
 * Expects a single {@code VinMoov5.j3o} under
 * {@code resource/JMonkeyEngine/assets/Models/}.
 *
 * Chain:
 * {@code ik3d.publishJointAngles → i01.onJointAngles → Servo.moveTo →
 * publishServoMoveTo/TimeEncoder → JMonkeyEngine.rotateTo}
 */
public class VirtualInMoovIkDemo {

  public final static Logger log = LoggerFactory.getLogger(VirtualInMoovIkDemo.class);

  private static final String ROBOT = "i01";
  private static final String ARM_KEY = "left";

  public static void main(String[] args) {
    try {
      LoggingFactory.init("info");

      Runtime.setAllVirtual(true);

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      // Start IK early so it shows in WebGui even if simulator init is slow
      log.info("Starting ik3d...");
      InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d", "InverseKinematics3D");
      ik3d.setCurrentArm(ARM_KEY, InMoov2Arm.getDHRobotArm(ROBOT, ARM_KEY));
      log.info("ik3d running");

      InMoov2 i01 = (InMoov2) Runtime.create(ROBOT, "InMoov2");
      i01.getConfig().reportOnBoot = false;
      i01.getConfig().loadGestures = false;
      i01.getConfig().heartbeat = false;
      i01.setMute(true);
      i01.startService();

      InMoov2Arm leftArm = (InMoov2Arm) i01.startPeer("leftArm");
      for (ServoControl servo : armServos(leftArm)) {
        if (servo != null) {
          servo.setAutoDisable(false);
          servo.enable();
        }
      }

      log.info("Starting JMonkeyEngine simulator...");
      JMonkeyEngine simulator = (JMonkeyEngine) i01.startSimulator();
      if (simulator == null || !simulator.isRunning()) {
        log.error("Simulator failed to start — aborting IK demo");
        return;
      }
      log.info("Simulator running: {}", simulator.getName());

      // Do not walk/mutate the live JME scene graph from this thread after the
      // app is running — that can deadlock. loadDelayed already bound VinMoov
      // and applied mappers. Attach only uses message subscriptions.
      attachArmServosToSimulator(simulator, leftArm);

      // Full servo names (i01.leftArm.*) → InMoov2 hub → Servo.moveTo
      ik3d.attach(i01);
      ik3d.addListener("publishJointAngles", i01.getName(), "onJointAngles");
      log.info("IK publishJointAngles → {}.onJointAngles", i01.getName());

      // Sanity: same path as WebGui servo UI (must move VinMoov if wiring OK)
      log.info("Sanity sweep leftArm.bicep 0 → 45 → 0 (watch simulator)");
      leftArm.getBicep().moveTo(0.0);
      sleep(1000);
      leftArm.getBicep().moveTo(45.0);
      sleep(1500);
      leftArm.getBicep().moveTo(0.0);
      sleep(1000);
      log.info("Sanity sweep done — bicep pos={}", leftArm.getBicep().getCurrentInputPos());

      log.info("Centered joints — publishing angles to {}", i01.getName());
      ik3d.centerAllJoints(ARM_KEY);
      sleep(2500);
      logServoSnapshot(leftArm);

      double[][] targets = { { 100.0, 0.0, 50.0 }, { 50.0, -50.0, 100.0 }, { 80.0, 40.0, 80.0 } };
      for (double[] xyz : targets) {
        log.info("IK moveTo ({}, {}, {})", xyz[0], xyz[1], xyz[2]);
        ik3d.centerAllJoints(ARM_KEY);
        sleep(500);
        ik3d.moveTo(ARM_KEY, xyz[0], xyz[1], xyz[2]);
        logServoSnapshot(leftArm);
        log.info("Palm at {}", ik3d.currentPosition(ARM_KEY));
        sleep(3000);
      }

      log.info("Demo moves finished — Runtime/WebGui/simulator still running.");
    } catch (Exception e) {
      log.error("VirtualInMoovIkDemo failed", e);
    }
  }

  private static ServoControl[] armServos(InMoov2Arm arm) {
    return new ServoControl[] { arm.getOmoplate(), arm.getShoulder(), arm.getRotate(), arm.getBicep() };
  }

  private static void attachArmServosToSimulator(JMonkeyEngine simulator, InMoov2Arm arm) throws Exception {
    for (ServoControl servo : armServos(arm)) {
      if (servo == null) {
        continue;
      }
      simulator.attach(servo);
      log.info("Attached {} → {} (encoder + publishServoMoveTo)", servo.getName(), simulator.getName());
    }
  }

  private static void logServoSnapshot(InMoov2Arm arm) {
    Map<String, Double> snap = new HashMap<>();
    snap.put("omoplate", arm.getOmoplate() != null ? arm.getOmoplate().getCurrentInputPos() : Double.NaN);
    snap.put("shoulder", arm.getShoulder() != null ? arm.getShoulder().getCurrentInputPos() : Double.NaN);
    snap.put("rotate", arm.getRotate() != null ? arm.getRotate().getCurrentInputPos() : Double.NaN);
    snap.put("bicep", arm.getBicep() != null ? arm.getBicep().getCurrentInputPos() : Double.NaN);
    log.info("Left arm servo inputs after IK: {}", snap);
  }

  private static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}

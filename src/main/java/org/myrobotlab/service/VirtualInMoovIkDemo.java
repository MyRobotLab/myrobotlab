package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.geometry.Point3df;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * Boots a virtual InMoov (JMonkeyEngine simulator) with InverseKinematics3D
 * (Jacobian) and Fabrik (FABRIK), measures the arm's joint axes off VinMoov,
 * verifies the Jacobian model tracks the rig across every joint range, then
 * leaves WebGui ready for: Center All Joints → MoveTo world X ± a few cm. The
 * Fabrik service panel can be used the same way to compare solvers.
 *
 * <pre>
 * Run: org.myrobotlab.service.VirtualInMoovIkDemo
 * </pre>
 *
 * Green marker = IK Cartesian goal. Blue = simulated left hand. The solver
 * walks a straight line in 1 cm steps toward the goal.
 *
 * Chain:
 * {@code ik3d.publishJointAngles → i01.onJointAngles → Servo.moveTo →
 * publishServoMoveTo/TimeEncoder → JMonkeyEngine.rotateTo}
 */
public class VirtualInMoovIkDemo {

  public final static Logger log = LoggerFactory.getLogger(VirtualInMoovIkDemo.class);

  private static final String ROBOT = "i01";
  private static final String ARM_KEY = "left";

  /** Acceptable IK vs simulator hand error after calibration (meters). */
  private static final double HAND_MATCH_M = 0.02;

  public static void main(String[] args) {
    try {
      LoggingFactory.init("info");

      Runtime.setAllVirtual(true);

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(true);
      webgui.startService();

      // Start IK early so it shows in WebGui even if simulator init is slow
      log.info("Starting ik3d...");
      InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d", "InverseKinematics3D");
      ik3d.setCurrentArm(ARM_KEY, InMoov2Arm.getDHRobotArm(ROBOT, ARM_KEY));
      log.info("ik3d running, DH palm at constructor pose {}", ik3d.currentPosition(ARM_KEY));

      log.info("Starting fabrik...");
      Fabrik fabrik = (Fabrik) Runtime.start("fabrik", "Fabrik");
      fabrik.setCurrentArm(ARM_KEY, ROBOT, ARM_KEY);
      log.info("fabrik running, palm at constructor pose {}", fabrik.currentPosition(ARM_KEY));

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
      ik3d.addListener("publishJointAngles", i01.getName(), "onJointAngles");
      fabrik.addListener("publishJointAngles", i01.getName(), "onJointAngles");
      // Green marker = publishIkGoal; HUD current palm = publishWorldPosition
      ik3d.attach(simulator);
      fabrik.attach(simulator);
      log.info("IK/FABRIK publishJointAngles → {}.onJointAngles; publishIkGoal → {}.onIkGoal", i01.getName(), simulator.getName());

      leftArm.rest();
      sleep(1500);
      ik3d.computePositionFromServos(ARM_KEY);
      if (!calibrateIkToSimulator(ik3d, simulator)) {
        log.error("Could not calibrate IK to simulator — check VinMoov node names");
        return;
      }
      Point fabrikWorld = fabrik.calibrateFromSimulator(simulator);
      if (fabrikWorld == null) {
        log.error("Could not calibrate FABRIK to simulator — check VinMoov node names");
        return;
      }
      log.info("Calibrated FABRIK world {}", fabrikWorld);

      log.info("Rest-pose check (measured chain vs simulator rest)");
      compareHands("rest", ik3d, simulator);
      logArmChain("rest", ik3d, simulator);

      // A rest-pose match proves nothing on its own: a chain with the wrong joint
      // axes or a mirrored direction also matches at rest. Sweep the workspace.
      double worst = ik3d.verifyAgainstSimulator(simulator, 5);
      if (Double.isNaN(worst) || worst > InverseKinematics3D.VERIFY_TOLERANCE_M) {
        log.error("Model disagrees with the simulator by {} m across the joint ranges — IK will diverge as the arm moves", worst);
      } else {
        log.info("Model tracks the simulator within {} m across every joint range", worst);
      }
      leftArm.rest();
      sleep(1500);
      ik3d.computePositionFromServos(ARM_KEY);

      log.info("Centered joints — publishing angles to {}", i01.getName());
      ik3d.centerAllJoints(ARM_KEY);
      sleep(2500);
      logServoSnapshot(leftArm);
      compareHands("centered", ik3d, simulator);

      Point centerWorld = ik3d.currentPositionWorld(ARM_KEY);
      log.info("Centered palm in world frame {}", centerWorld);

      // Validate the WebGui use case: jog world X a few centimeters, stay on Y/Z
      double[] xDeltas = { 0.03, -0.03, 0.05 };
      for (double dx : xDeltas) {
        ik3d.centerAllJoints(ARM_KEY);
        sleep(500);
        double tx = centerWorld.getX() + dx;
        double ty = centerWorld.getY();
        double tz = centerWorld.getZ();
        log.info("IK straight-line world X {} → ({}, {}, {})", dx, tx, ty, tz);
        Point reached = ik3d.moveTo(ARM_KEY, tx, ty, tz);
        logServoSnapshot(leftArm);
        sleep(1500);
        compareHands(String.format("moveTo x%+.3f", dx), ik3d, simulator);
        if (reached != null) {
          log.info("X-line dx={} goal=({}, {}, {}) reached {} errX={} errY={} errZ={}", dx, tx, ty, tz, reached,
              Math.abs(reached.getX() - tx), Math.abs(reached.getY() - ty), Math.abs(reached.getZ() - tz));
        }
      }

      ik3d.centerAllJoints(ARM_KEY);
      sleep(1500);
      fabrik.computePositionFromServos(ARM_KEY);
      log.info("Demo ready for WebGui: use ik3d or fabrik — Center All Joints, then MoveTo world X ± a few cm. Green dot is the IK goal.");
    } catch (Exception e) {
      log.error("VirtualInMoovIkDemo failed", e);
    }
  }

  /**
   * Measure every arm joint's rotation axis and origin off VinMoov and rebuild
   * the solver's chain from them. Both frames are meters, Y-up.
   */
  private static boolean calibrateIkToSimulator(InverseKinematics3D ik3d, JMonkeyEngine simulator) {
    Point world = ik3d.calibrateFromSimulator(simulator);
    log.info("Calibrated IK world {}", world);
    return world != null;
  }

  private static void logArmChain(String label, InverseKinematics3D ik3d, JMonkeyEngine simulator) {
    Map<String, Point3df> chain = simulator.getArmChainWorldTranslations(ROBOT, ARM_KEY);
    log.info("[{}] VinMoov chain {}", label, chain);
    DHRobotArm arm = ik3d.getCurrentArm(ARM_KEY);
    if (arm == null) {
      return;
    }
    for (int i = 0; i < arm.getNumLinks(); i++) {
      log.info("[{}] joint {} {} world {} servo {}", label, i, arm.getLink(i).getName(), arm.getJointPosition(i), arm.getLink(i).toServoDegrees());
    }
    log.info("[{}] origin {} tool offset {} palm world {}", label, ik3d.getWorldOrigin(), arm.getToolOffset(), ik3d.currentPositionWorld(ARM_KEY));
  }

  private static void probeIsolatedServoMoves(InverseKinematics3D ik3d, JMonkeyEngine simulator, InMoov2Arm arm) {
    ServoControl[] servos = armServos(arm);
    double[] rest = new double[servos.length];
    for (int i = 0; i < servos.length; i++) {
      rest[i] = servos[i] != null ? servos[i].getCurrentInputPos() : Double.NaN;
    }
    for (int i = 0; i < servos.length; i++) {
      ServoControl servo = servos[i];
      if (servo == null) {
        continue;
      }
      double orig = rest[i];
      double dest = orig + 30.0;
      log.info("Isolated move {} {} → {}", servo.getName(), orig, dest);
      servo.moveTo(dest);
      sleep(2500);
      ik3d.computePositionFromServos(ARM_KEY);
      compareHands("isolated-" + servo.getName(), ik3d, simulator);
      logArmChain("isolated-" + servo.getName(), ik3d, simulator);
      servo.moveTo(orig);
      sleep(2500);
    }
    arm.rest();
    sleep(2000);
    ik3d.computePositionFromServos(ARM_KEY);
  }

  private static boolean compareHands(String label, InverseKinematics3D ik3d, JMonkeyEngine simulator) {
    Point ikWorld = ik3d.currentPositionWorld(ARM_KEY);
    Point simHand = toPoint(simulator.getHandWorldTranslation(ROBOT, ARM_KEY));
    if (ikWorld != null) {
      simulator.setIkHandWorldPosition(ikWorld.getX(), ikWorld.getY(), ikWorld.getZ());
    }
    if (ikWorld == null || simHand == null) {
      log.warn("[{}] cannot compare — IK world {} sim {}", label, ikWorld, simHand);
      return false;
    }
    double err = ikWorld.distanceTo(simHand);
    boolean match = err <= HAND_MATCH_M;
    if (match) {
      log.info("[{}] HAND MATCH err={}m  IK {}  sim {}", label, String.format("%.3f", err), ikWorld, simHand);
    } else {
      log.warn("[{}] HAND MISMATCH err={}m (limit {}m)  IK {}  sim {}", label, String.format("%.3f", err), HAND_MATCH_M, ikWorld, simHand);
    }
    return match;
  }

  private static Point toPoint(Point3df p) {
    if (p == null) {
      return null;
    }
    return new Point(p.x, p.y, p.z);
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

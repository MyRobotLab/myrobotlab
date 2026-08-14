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
 * Boots a virtual InMoov (JMonkeyEngine simulator) with InverseKinematics3D,
 * calibrates the DH / IK millimeter frame onto the simulator's meter Y-up
 * frame, and checks that IK palm and simulated hand agree.
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
      // Green IK overlay + HUD follow publishWorldPosition (Compute from servos)
      ik3d.attach(simulator);
      log.info("IK publishJointAngles → {}.onJointAngles; publishWorldPosition → {}.onWorldPosition", i01.getName(),
          simulator.getName());

      // Sanity: same path as WebGui servo UI (must move VinMoov if wiring OK)
      log.info("Sanity sweep leftArm.bicep 0 → 45 → 0 (watch simulator)");
      leftArm.getBicep().moveTo(0.0);
      sleep(1000);
      leftArm.getBicep().moveTo(45.0);
      sleep(1500);
      leftArm.getBicep().moveTo(0.0);
      sleep(1500);
      log.info("Sanity sweep done — bicep pos={}", leftArm.getBicep().getCurrentInputPos());

      ik3d.computePositionFromServos(ARM_KEY);
      if (!calibrateIkToSimulator(ik3d, simulator)) {
        log.error("Could not calibrate IK to simulator — check VinMoov node names");
        return;
      }

      log.info("Rest-pose check (DH rest thetas vs simulator rest)");
      compareHands("rest", ik3d, simulator);
      logArmChain("rest", ik3d, simulator);

      probeIsolatedServoMoves(ik3d, simulator, leftArm);

      log.info("Centered joints — publishing angles to {}", i01.getName());
      ik3d.centerAllJoints(ARM_KEY);
      sleep(2500);
      logServoSnapshot(leftArm);
      compareHands("centered", ik3d, simulator);

      Point centerWorld = ik3d.currentPositionWorld(ARM_KEY);
      log.info("Centered palm in world frame {}", centerWorld);

      // Small reachable offsets in simulator meters from the centered pose
      double[][] worldDeltas = { { 0.05, 0.0, 0.05 }, { -0.04, 0.04, 0.0 }, { 0.0, 0.05, 0.04 } };
      for (double[] d : worldDeltas) {
        double tx = centerWorld.getX() + d[0];
        double ty = centerWorld.getY() + d[1];
        double tz = centerWorld.getZ() + d[2];
        log.info("IK moveTo world ({}, {}, {})", tx, ty, tz);
        ik3d.moveTo(ARM_KEY, tx, ty, tz);
        logServoSnapshot(leftArm);
        sleep(3000);
        compareHands(String.format("moveTo(%.3f,%.3f,%.3f)", tx, ty, tz), ik3d, simulator);
      }

      log.info("Demo moves finished — Runtime/WebGui/simulator still running.");
    } catch (Exception e) {
      log.error("VirtualInMoovIkDemo failed", e);
    }
  }

  /**
   * DH is mm at the omoplate, Y-up; JME is meters at the model root, Y-up.
   * Measure VinMoov bone lengths, put the DH origin on the omoplate node, and
   * fit a last-frame wrist offset so hanging Z lives in the chain.
   */
  private static boolean calibrateIkToSimulator(InverseKinematics3D ik3d, JMonkeyEngine simulator) {
    Point omoplate = worldPoint(simulator, ROBOT + ".leftArm.omoplate", ROBOT + ".leftArm.shoulder", "leftArm.omoplate");
    Point shoulder = worldPoint(simulator, ROBOT + ".leftArm.shoulder");
    Point rotate = worldPoint(simulator, ROBOT + ".leftArm.rotate");
    Point bicep = worldPoint(simulator, ROBOT + ".leftArm.bicep");
    Point wrist = toPoint(simulator.getHandWorldTranslation(ROBOT, ARM_KEY));
    if (wrist == null) {
      wrist = worldPoint(simulator, ROBOT + ".leftHand.wrist", ROBOT + ".leftHand", "leftHand.wrist");
    }
    if (omoplate == null || wrist == null) {
      log.error("Calibration samples missing omoplate={} wrist={}", omoplate, wrist);
      return false;
    }
    logVinMoovDistances(omoplate, shoulder, rotate, bicep, wrist);
    ik3d.fitVinMoovLinkLengths(ARM_KEY, omoplate, shoulder, rotate, bicep, wrist);
    Point world = ik3d.calibrateFromWorldSamples(omoplate, wrist);
    log.info("Calibrated IK world {} vs sim wrist {}", world, wrist);
    return world != null;
  }

  private static void logVinMoovDistances(Point omoplate, Point shoulder, Point rotate, Point bicep, Point wrist) {
    log.info("VinMoov omoplate-shoulder {} mm", fmtMm(omoplate, shoulder));
    log.info("VinMoov shoulder-rotate {} mm", fmtMm(shoulder, rotate));
    log.info("VinMoov rotate-bicep {} mm", fmtMm(rotate, bicep));
    log.info("VinMoov bicep-wrist {} mm", fmtMm(bicep, wrist));
  }

  private static String fmtMm(Point a, Point b) {
    if (a == null || b == null) {
      return "n/a";
    }
    return String.format("%.1f", a.distanceTo(b) * InverseKinematics3D.IK_MM_PER_JME_METER);
  }

  private static void logArmChain(String label, InverseKinematics3D ik3d, JMonkeyEngine simulator) {
    Map<String, Point3df> chain = simulator.getArmChainWorldTranslations(ROBOT, ARM_KEY);
    log.info("[{}] VinMoov chain {}", label, chain);
    DHRobotArm dh = ik3d.getCurrentArm(ARM_KEY);
    if (dh == null) {
      return;
    }
    Point origin = ik3d.getWorldOrigin();
    for (int i = 0; i < dh.getNumLinks(); i++) {
      Point jp = dh.getJointPosition(i);
      Point world = origin != null ? ik3d.toWorldFrame(jp) : jp;
      log.info("[{}] DH joint {} {} world {}", label, i, dh.getLink(i).getName(), world);
    }
    log.info("[{}] DH palm world {}", label, ik3d.currentPositionWorld(ARM_KEY));
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

  private static Point worldPoint(JMonkeyEngine simulator, String... names) {
    for (String name : names) {
      Point3df p = simulator.getWorldTranslation(name);
      if (p != null) {
        log.info("Resolved world node {} -> ({}, {}, {})", name, p.x, p.y, p.z);
        return toPoint(p);
      }
      log.info("World node {} not found", name);
    }
    return null;
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

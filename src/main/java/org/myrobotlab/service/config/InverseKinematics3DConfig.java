package org.myrobotlab.service.config;

/**
 * InverseKinematics3D configuration, including an optional world-frame
 * calibration that maps simulator / JMonkeyEngine coordinates onto the DH
 * solver frame.
 *
 * <p>
 * IK / DH units are millimeters with the origin at the first joint (omoplate).
 * The VinMoov / JME simulator is meters, Y-up, with the origin at the model
 * root. When {@link #worldFrame} is true, {@code moveTo} targets are in world
 * coordinates:
 * </p>
 *
 * <pre>
 * p_ik = scale ⊙ (p_world − origin)
 * </pre>
 *
 * Wrist bind-pose offset is a last-frame tool transform on the DH arm, not
 * these origin/scale fields.
 */
public class InverseKinematics3DConfig extends ServiceConfig {

  /**
   * When true, apply {@link #originX origin}/{@link #scaleX scale} so that
   * {@code moveTo} is in simulator/world units (JME meters).
   */
  public boolean worldFrame = false;

  /** World-frame DH-base origin (typically the omoplate node), X. */
  public double originX = 0.0;

  /** World-frame DH-base origin, Y (up in JME). */
  public double originY = 0.0;

  /** World-frame DH-base origin, Z. */
  public double originZ = 0.0;

  /**
   * Scale from world units into IK millimeters (IK = scale * (world − origin)).
   * Default 1000 converts JME meters to mm. Negative values flip an axis.
   */
  public double scaleX = 1000.0;

  public double scaleY = 1000.0;

  public double scaleZ = 1000.0;
}

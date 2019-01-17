package org.myrobotlab.kinematics;

import java.util.ArrayList;

// This is a port from the penguin wrist solver
// https://github.com/parloma/penguinwrist
public class DruppIKSolver {

  double roll = 0;
  double pitch = 0;
  double yaw = 0;

  // solver constants
  double A = 0.0;
  double B = 0.0;
  double C = 0.0;
  double D = 0.0;
  double E = 0.0;
  double F = 0.0;
  double H = 0.0;
  double I = 0.0;
  double L = 0.0;
  double M = 0.0;
  double N = 0.0;
  double O = 0.0;
  double P = 0.0;
  double R = 0.0;

  double l1a, l2a, psi, rl2;
  double[] centerXY = new double[3];
  double[] p4 = new double[3];
  double[] RPY = new double[3];
  double[] sols = new double[3];

  public DruppIKSolver() {
    // initial params.. TODO: maybe these are different for the drupp neck?
    p4[0] = 0;
    p4[1] = 0;
    p4[2] = 36.57;

    l1a = 19.33;
    l2a = 22.01;
    psi = deg2rad(120);
    centerXY[0] = 0;
    centerXY[1] = 0;
    centerXY[2] = 19.06;
    rl2 = 35.03;

    setRpy(0, 0, 0);

  }

  private void setRpy(double r, double p, double y) {
    // TODO Auto-generated method stub
    this.roll = r;
    this.pitch = p;
    this.yaw = y;

  }

  void initConstants() {

    // double[] RPY = new double[] {roll,pitch,yaw};

    A = centerXY[2] - p4[2];
    B = rl2 * Math.sin(RPY[1]);
    C = rl2 * Math.cos(RPY[1]) * Math.cos(RPY[0]);

    D = l1a - l2a * Math.cos(psi);
    E = rl2 * (Math.cos(RPY[2]) * Math.sin(RPY[0]) - Math.cos(RPY[0]) * Math.sin(RPY[1]) * Math.sin(RPY[2]));
    F = rl2 * Math.cos(RPY[1]) * Math.sin(RPY[2]);

    H = rl2 * (Math.sin(RPY[0]) * Math.sin(RPY[2]) + Math.cos(RPY[0]) * Math.cos(RPY[2]) * Math.sin(RPY[1]));
    I = rl2 * Math.cos(RPY[1]) * Math.cos(RPY[2]);
    L = rl2 * (Math.sin(RPY[1]) / 2 - (Math.sqrt(3) * Math.cos(RPY[1]) * Math.sin(RPY[0])) / 2);

    M = rl2 * ((Math.cos(RPY[1]) * Math.sin(RPY[2])) / 2 + (Math.sqrt(3) * (Math.cos(RPY[0]) * Math.cos(RPY[2]) + Math.sin(RPY[1]) * Math.sin(RPY[0]) * Math.sin(RPY[2]))) / 2);
    N = rl2 * ((Math.cos(RPY[1]) * Math.cos(RPY[2])) / 2 - (Math.sqrt(3) * (Math.cos(RPY[0]) * Math.sin(RPY[2]) - Math.cos(RPY[2]) * Math.sin(RPY[1]) * Math.sin(RPY[0]))) / 2);
    O = rl2 * (Math.sin(RPY[1]) / 2 + (Math.sqrt(3) * Math.cos(RPY[1]) * Math.sin(RPY[0])) / 2);
    P = rl2 * ((Math.cos(RPY[1]) * Math.sin(RPY[2])) / 2 - (Math.sqrt(3) * (Math.cos(RPY[0]) * Math.cos(RPY[2]) + Math.sin(RPY[1]) * Math.sin(RPY[0]) * Math.sin(RPY[2]))) / 2);
    R = rl2 * ((Math.cos(RPY[1]) * Math.cos(RPY[2])) / 2 + (Math.sqrt(3) * (Math.cos(RPY[0]) * Math.sin(RPY[2]) - Math.cos(RPY[2]) * Math.sin(RPY[1]) * Math.sin(RPY[0]))) / 2);

  }

  public double[] solve(double roll, double pitch, double yaw) throws Exception {

    // double[] sols = new double[3];
    RPY[0] = roll;
    RPY[1] = pitch;
    RPY[2] = yaw;
    initConstants();
    sols[0] = solveUp();
    sols[1] = solveMiddle();
    sols[2] = solveDown();
    return sols;
  }

  public double deg2rad(double ang) {
    return ang / 180.0 * Math.PI;
  }

  public double solveDown() throws Exception {
    if (-A * A + O * O + C * C < 0.0f) {
      throw new Exception("Failed to solve!");
    }
    ArrayList<Double> st3down = new ArrayList<Double>();
    double yplus = (O + Math.sqrt(-A * A + O * O + C * C));
    double yminus = (O - Math.sqrt(-A * A + O * O + C * C));
    double x = A + C;

    if ((yplus != 0) && (x != 0)) {
      st3down.add(2 * Math.atan2(yplus, x));
    }

    if ((yminus != 0) && (x != 0)) {
      st3down.add(2 * Math.atan2(yminus, x));
    }

    /* Solve for t1 alto: eq 1 and 2: */
    /* eq2alto = E*cos(t3) + D*sin(t1) - F*sin(t3) */
    /* eq1alto = D*cos(t1) - H*cos(t3) - I*sin(t3) */
    /* Combination to avoid asin and acos but using Math.atan2 */

    /* -E*cos(t3) + F*sin(t3) */
    /* tan(t1) = ---------------------- */
    /* H*cos(t3) + I*sin(t3) */

    double y1 = -E * Math.cos(st3down.get(0)) - P * Math.sin(st3down.get(0));
    double x1 = H * Math.cos(st3down.get(0)) - R * Math.sin(st3down.get(0));

    double y2 = -E * Math.cos(st3down.get(1)) - P * Math.sin(st3down.get(1));
    double x2 = H * Math.cos(st3down.get(1)) - R * Math.sin(st3down.get(1));

    ArrayList<Double> st1downsols = new ArrayList<Double>();

    if (st3down.size() == 2) {

      if ((y1 != 0) || (x1 != 0)) {
        st1downsols.add(Math.atan2(y1, x1));
      }

      if ((y2 != 0) || (x2 != 0)) {
        st1downsols.add(Math.atan2(y2, x2));
      }

      if (st1downsols.size() == 0) {
        // TO DO: Generate Exception
        throw new Exception("Failed to solve!");
        // exit(EXITFAILURE);

      }

    } else if (st3down.size() == 1) {
      if ((y1 != 0) || (x1 != 0)) {
        st1downsols.add(Math.atan2(y1, x1));
      } else {
        /* %disp('atan2(0,0) Undefined...NO IK Available...Quit() !!! '); */
        // TO DO: Generate Exception
        throw new Exception("Failed to solve");
        // exit(EXITFAILURE);
      }
    } else {
      /* %disp('found No solutions for t3 alto...quit!!') */
      // TO DO: Generate Exception
      // exit(EXITFAILURE);
      throw new Exception("Failed to solve.");
    }
    return (Math.abs(st1downsols.get(0) - (2.0f / 3) * Math.PI) < Math.abs(st1downsols.get(1) - (2.0f / 3)) ? st1downsols.get(0) : st1downsols.get(1));

  }

  double solveMiddle() throws Exception {
    if (-A * A + L * L + C * C < 0.0f) {
      // TO DO: Generate Exception
      throw new Exception("Failed to solve.");
      // exit(EXITFAILURE);
    }
    ArrayList<Double> st3middle = new ArrayList<Double>();
    double yplus = (L + Math.sqrt(-A * A + L * L + C * C));
    double yminus = (L - Math.sqrt(-A * A + L * L + C * C));
    double x = A + C;

    if ((yplus != 0) && (x != 0)) {
      st3middle.add(-2 * Math.atan2(yplus, x));
    }

    if ((yminus != 0) && (x != 0)) {
      st3middle.add(-2 * Math.atan2(yminus, x));
    }

    double y1 = -E * Math.cos(st3middle.get(0)) + M * Math.sin(st3middle.get(0));
    double x1 = H * Math.cos(st3middle.get(0)) + N * Math.sin(st3middle.get(0));

    double y2 = -E * Math.cos(st3middle.get(1)) + M * Math.sin(st3middle.get(1));
    double x2 = H * Math.cos(st3middle.get(1)) + N * Math.sin(st3middle.get(1));

    ArrayList<Double> st1middlesols = new ArrayList<Double>();

    if (st3middle.size() == 2) {

      if ((y1 != 0) || (x1 != 0)) {
        st1middlesols.add(Math.atan2(y1, x1));
      }

      if ((y2 != 0) || (x2 != 0)) {
        st1middlesols.add(Math.atan2(y2, x2));
      }

      if (st1middlesols.size() == 0) {
        // TO DO: Generate Exception
        throw new Exception("Failed to solve.");

      }

    } else if (st3middle.size() == 1) {
      if ((y1 != 0) || (x1 != 0)) {
        st1middlesols.add(Math.atan2(y1, x1));
      } else {
        /* %disp('atan2(0,0) Undefined...NO IK Available...Quit() !!! '); */
        // TO DO: Generate Exception
        // exit(EXITFAILURE);
        throw new Exception("Failed to solve.");
      }
    } else {
      /* %disp('found No solutions for t3 alto...quit!!') */
      // TO DO: Generate Exception
      throw new Exception("Failed to solve");
      // exit(EXITFAILURE);
    }

    return ((Math.abs(st1middlesols.get(0) - (-2.0f / 3) * Math.PI) < Math.abs(st1middlesols.get(1) - (-2.0f / 3) * Math.PI)) ? st1middlesols.get(0) : st1middlesols.get(1));
  }

  double solveUp() throws Exception {

    if (-A * A + B * B + C * C < 0.0f) {
      // TO DO: Generate Exception
      throw new Exception("Failed to solve");
      // exit(EXITFAILURE);
    }

    ArrayList<Double> st3up = new ArrayList<Double>();
    double yplus = (B + Math.sqrt(-A * A + B * B + C * C));
    double yminus = (B - Math.sqrt(-A * A + B * B + C * C));
    double x = A + C;

    if ((yplus != 0) && (x != 0)) {
      st3up.add(-2 * Math.atan2(yplus, x));
    }

    if ((yminus != 0) && (x != 0)) {
      st3up.add(-2 * Math.atan2(yminus, x));
    }

    double y1 = -E * Math.cos(st3up.get(0)) + F * Math.sin(st3up.get(0));
    double x1 = H * Math.cos(st3up.get(0)) + I * Math.sin(st3up.get(0));

    double y2 = -E * Math.cos(st3up.get(1)) + F * Math.sin(st3up.get(1));
    double x2 = H * Math.cos(st3up.get(1)) + I * Math.sin(st3up.get(1));

    ArrayList<Double> st1upsols = new ArrayList<Double>();

    if (st3up.size() == 2) {

      if ((y1 != 0) || (x1 != 0)) {
        st1upsols.add(Math.atan2(y1, x1));
      }

      if ((y2 != 0) || (x2 != 0)) {
        st1upsols.add(Math.atan2(y2, x2));
      }

      if (st1upsols.size() == 0) {
        // TO DO: Generate Exception
        throw new Exception("Failed to solve");
        // exit(EXITFAILURE);
      }

    } else if (st3up.size() == 1) {
      if ((y1 != 0) || (x1 != 0)) {
        st1upsols.add(Math.atan2(y1, x1));
      } else {
        /* %disp('atan2(0,0) Undefined...NO IK Available...Quit() !!! '); */
        // TO DO: Generate Exception
        throw new Exception("Failed to solve");
        // exit(EXITFAILURE);
      }
    } else {
      /* %disp('found No solutions for t3 alto...quit!!') */
      // TO DO: Generate Exception
      throw new Exception("no solutions...");
      // exit(EXITFAILURE);
    }

    return (Math.abs(st1upsols.get(0) - 0) < Math.abs(st1upsols.get(1) - 0) ? st1upsols.get(0) : st1upsols.get(1));
  }

}

package org.myrobotlab.openni;

// CheckerFloor.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* The floor is a blue and green chessboard, with a small red square
 at the (0,0) position on the (X,Z) plane, and with numbers along
 the X- and Z- axes.

 Explained in the Checkers3D example in Chapter 15,
 "Killer Game Programming in Java"
 (http://fivedots.coe.psu.ac.th/~ad/jg/ch8/)
 */

import java.awt.Font;
import java.util.ArrayList;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.geometry.Text2D;

public class CheckerFloor {
  private final static int FLOOR_LEN = 80; // should be even

  // colours for floor, etc
  // private final static Color3f blue = new Color3f(0.0f, 0.1f, 0.4f);
  // private final static Color3f green = new Color3f(0.0f, 0.5f, 0.1f);
  // private final static Color3f blue = new
  // Color3f(Style.possibleServicesNotInstalled);
  private final static Color3f green = new Color3f(0.17f, 0.43f, 0.20f);
  private final static Color3f blue = new Color3f(0.35f, 0.35f, 0.35f);
  // private final static Color3f green = new
  // Color3f(Style.possibleServicesStable);
  private final static Color3f medRed = new Color3f(0.8f, 0.4f, 0.3f);
  private final static Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

  private BranchGroup floorBG;

  public CheckerFloor()
  // create tiles, add origin marker, then the axes labels
  {
    ArrayList<Point3f> blueCoords = new ArrayList<Point3f>();
    ArrayList<Point3f> greenCoords = new ArrayList<Point3f>();
    floorBG = new BranchGroup();

    boolean isBlue;
    for (int z = -FLOOR_LEN / 2; z <= (FLOOR_LEN / 2) - 1; z++) {
      isBlue = (z % 2 == 0) ? true : false; // set colour for new row
      for (int x = -FLOOR_LEN / 2; x <= (FLOOR_LEN / 2) - 1; x++) {
        if (isBlue)
          createCoords(x, z, blueCoords);
        else
          createCoords(x, z, greenCoords);
        isBlue = !isBlue;
      }
    }
    floorBG.addChild(new ColouredTiles(blueCoords, blue));
    floorBG.addChild(new ColouredTiles(greenCoords, green));

    addOriginMarker();
    labelAxes();
  } // end of CheckerFloor()

  private void addOriginMarker()
  // A red square centered at (0,0,0), of length 0.5
  { // points created counter-clockwise, a bit above the floor
    Point3f p1 = new Point3f(-0.25f, 0.01f, 0.25f);
    Point3f p2 = new Point3f(0.25f, 0.01f, 0.25f);
    Point3f p3 = new Point3f(0.25f, 0.01f, -0.25f);
    Point3f p4 = new Point3f(-0.25f, 0.01f, -0.25f);

    ArrayList<Point3f> oCoords = new ArrayList<Point3f>();
    oCoords.add(p1);
    oCoords.add(p2);
    oCoords.add(p3);
    oCoords.add(p4);

    floorBG.addChild(new ColouredTiles(oCoords, medRed));
  }

  private void createCoords(int x, int z, ArrayList<Point3f> coords) {
    // Coords for a single blue or green square,
    // its left hand corner at (x,0,z)
    // points created in counter-clockwise order
    Point3f p1 = new Point3f(x, 0.0f, z + 1.0f);
    Point3f p2 = new Point3f(x + 1.0f, 0.0f, z + 1.0f);
    Point3f p3 = new Point3f(x + 1.0f, 0.0f, z);
    Point3f p4 = new Point3f(x, 0.0f, z);
    coords.add(p1);
    coords.add(p2);
    coords.add(p3);
    coords.add(p4);
  } // end of createCoords()

  public BranchGroup getBG() {
    return floorBG;
  }

  // Place numbers along the X- and Z-axes at the integer positions
  private void labelAxes() {
    Vector3d pt = new Vector3d();
    for (int i = -FLOOR_LEN / 2; i <= FLOOR_LEN / 2; i++) {
      pt.x = i;
      floorBG.addChild(makeText(pt, "" + i)); // along x-axis
    }

    pt.x = 0;
    for (int i = -FLOOR_LEN / 2; i <= FLOOR_LEN / 2; i++) {
      pt.z = i;
      floorBG.addChild(makeText(pt, "" + i)); // along z-axis
    }
  }

  // Create a Text2D object at the specified vertex
  private TransformGroup makeText(Vector3d vertex, String text) {
    Text2D message = new Text2D(text, white, "SansSerif", 36, Font.BOLD);
    // 36 point bold Sans Serif

    TransformGroup tg = new TransformGroup();
    Transform3D t3d = new Transform3D();
    t3d.setTranslation(vertex);
    tg.setTransform(t3d);
    tg.addChild(message);
    return tg;
  }

}
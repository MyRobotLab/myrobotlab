package org.myrobotlab.openni;

// ColouredTiles.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th
/*
 ColouredTiles creates a coloured quad array of tiles.
 No lighting since no normals or Material used

 Explained in the Checkers3D example in Chapter 15,
 "Killer Game Programming in Java"
 (http://fivedots.coe.psu.ac.th/~ad/jg/ch8/)
 */

import java.util.ArrayList;

import javax.media.j3d.Appearance;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

public class ColouredTiles extends Shape3D {
  private QuadArray plane;

  public ColouredTiles(ArrayList<Point3f> coords, Color3f col) {
    plane = new QuadArray(coords.size(), GeometryArray.COORDINATES | GeometryArray.COLOR_3);
    createGeometry(coords, col);
    createAppearance();
  }

  private void createAppearance() {
    Appearance app = new Appearance();

    PolygonAttributes pa = new PolygonAttributes();
    pa.setCullFace(PolygonAttributes.CULL_NONE);
    // so can see the ColouredTiles from both sides
    app.setPolygonAttributes(pa);

    setAppearance(app);
  }

  private void createGeometry(ArrayList<Point3f> coords, Color3f col) {
    int numPoints = coords.size();

    Point3f[] points = new Point3f[numPoints];
    coords.toArray(points);
    plane.setCoordinates(0, points);

    Color3f cols[] = new Color3f[numPoints];
    for (int i = 0; i < numPoints; i++)
      cols[i] = col;
    plane.setColors(0, cols);

    setGeometry(plane);
  }

}
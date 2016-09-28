/**
 * 
 */
package org.myrobotlab.kinematics;

import java.util.HashMap;
import org.slf4j.Logger;

/**
 * @author Christian
 *
 */
public class CollisionDectection {
  transient HashMap<String, CollisionItem> items= new HashMap<String,CollisionItem>();
  private boolean collision;
  private Point[] collisionPoint = {new Point(0,0,0,0,0,0),new Point(0,0,0,0,0,0)};
  
  
  public CollisionDectection() {
    super();
  }

  public void addItem(CollisionItem item) {
    if (items.containsKey(item.getName())) {
      CollisionItem updateItem = items.get(item.getName());
      updateItem.setOrigin(item.getOrigin());
      updateItem.setEnd(item.getEnd());
      for (int i = 0; i<item.ignore.size(); i++){
        if (!updateItem.ignore.contains(item.ignore.get(i))) {
          updateItem.ignore.add(item.ignore.get(i));
        }
      }
      items.put(item.getName(), updateItem);
      return;
    }
    items.put(item.getName(), item);
  }

  public void runTest() {
    collision = false;
    for (CollisionItem item : items.values()){
      //vect1 = vector of the line between the extremity of the first item
      double[] vect1 = calcVect(item);
      //par1 = parametric formula of vect1
      double[][] par1 = calcPar(item, vect1, 1);//[x,y,z][c,t,k]
      item.clearDone();
      for (CollisionItem citem : items.values()) {
        if (citem.getName().equals(item.getName())) {
          // do not compared two time the same part
          continue;
        }
        if (item.ignore.contains(citem.getName())) {
          // item 2 is in the ignore list of item 1
          continue;
        }
        if (citem.ignore.contains(item.getName())) {
          //item 1 is in the ignore list of item 2
          continue;
        }
        // need to not compare item already done;
        if (citem.isDone(citem.getName())) {
          continue;
        }
        //vect2 = vector of the line between the extremity of the second item
        double[] vect2 = calcVect(citem);
        //par2 = parametric formula of vect1;
        double[][] par2 = calcPar(citem, vect2, 2);
        //vectT  = vector between the origin of the two part
        double[][] vectT = calcVectT(par1, par2);
        // vectT1/vectT2 = imposing perpendicularity with the item vector
        double[] vectT1 = calcPerpendicularity(vectT, vect1);
        double[] vectT2 = calcPerpendicularity(vectT, vect2);
        // resolve the formulas (vectT1[0] + vectT1[1]*t + vectT1[2]*k = 0 and vectT2[0] + vectT2[1]*t + vectT2[2]*k = 0) we need to know value of t and k
        double[] tTemp = new double[3];
        tTemp[0] = vectT1[0]/(vectT1[1]*-1);
        tTemp[2] = vectT1[2]/(vectT1[1]*-1);
        double[] k = new double[3];
        k[0] = vectT2[0] + (tTemp[0] * vectT2[1]);
        k[2] = vectT2[2] + (tTemp[2] * vectT2[1]);
        k[0] = k[0] / (k[2] * -1);
        double[] tk= new double[2];
        tk[0] = (vectT1[0] + (vectT1[2] * k[0])) / (vectT1[1] * -1);
        tk[1] = k[0];
        if(tk[0] < 0) tk[0] = 0;
        if(tk[0] > 1) tk[0] = 1;
        if(tk[1] < 0) tk[1] = 0;
        if(tk[1] > 1) tk[1] = 1;
//        get the equation of the line of the shortest distance between the center line of the items (Vt = (x, y, z))
        double[] vectTFinal = new double[3];
        vectTFinal[0] = vectT[0][0] + (vectT[0][1] * tk[0]) + (vectT[0][2] * tk[1]); 
        vectTFinal[1] = vectT[1][0] + (vectT[1][1] * tk[0]) + (vectT[1][2] * tk[1]); 
        vectTFinal[2] = vectT[2][0] + (vectT[2][1] * tk[0]) + (vectT[2][2] * tk[1]); 
        //get the intersection point between Vt and V1;
        Point point1 = new Point(par1[0][0] + par1[0][1]*tk[0] + par1[0][2] * tk[1], par1[1][0] + par1[1][1]*tk[0] + par1[1][2] * tk[1], par1[2][0] + par1[2][1]*tk[0] + par1[2][2] * tk[1], 0, 0, 0);
        //get the intersection point between Vt and V2;
        Point point2 = new Point(par2[0][0] + par2[0][1]*tk[0] + par2[0][2] * tk[1], par2[1][0] + par2[1][1]*tk[0] + par2[1][2] * tk[1], par2[2][0] + par2[2][1]*tk[0] + par2[2][2] * tk[1], 0, 0, 0);
        //calculate the distance between these two points
        double d = Math.sqrt(((point2.getX() - point1.getX()) * (point2.getX() - point1.getX())) + ((point2.getY() - point1.getY()) * (point2.getY() - point1.getY())) + ((point2.getZ() - point1.getZ()) * (point2.getZ() - point1.getZ())));
        // if d < radius item 1 + radius item 2 then there is a possible collision
        double rad1 = item.getRadius();
        if(tk[0] <= (double)0.0 || tk[0] >= (double)1.0) {
          rad1=0;
        }
        double rad2 = citem.getRadius();
        if(tk[1] == (double)0.0 || tk[1] == (double)1.0) {
          rad2=0;
        }
        item.haveDone(citem.getName());
        if (d <= rad1 + rad2 /*&& ((tk[0] != 0 && tk[0] != 1.0) || (tk[1] != 0 && tk[1] != 1))*/) {
          //we got a potential collision
          collision = true;
          collisionPoint[0] = point1;
          collisionPoint[1] = point2;
          return;
        }
      }
    }
  }
  
  private double[] calcPerpendicularity(double[][] vectT, double[] vect) {
    double[] vectP = new double[3];
    vectP[0] = vectT[0][0] * vect[0] + vectT[1][0] * vect[1] + vectT[2][0] * vect[2];
    vectP[1] = vectT[0][1] * vect[0] + vectT[1][1] * vect[1] + vectT[2][1] * vect[2];
    vectP[2] = vectT[0][2] * vect[0] + vectT[1][2] * vect[1] + vectT[2][2] * vect[2];
    return vectP;
  }

  private double[][] calcVectT(double[][] par1, double[][] par2) {
    double[][] vectT = new double[3][3];
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        vectT[i][j] = par1[i][j] - par2[i][j];
      }
    }
    return vectT;
  }

  double[][] calcPar(CollisionItem item, double[] vect, int pos) {
    double[][] par = new double[3][3];//[x,y,z][c,t,k]
    par[0][0] = item.getOrigin().getX();
    par[0][pos] = vect[0];
    par[1][0] = item.getOrigin().getY();
    par[1][pos] = vect[1];
    par[2][0] = item.getOrigin().getZ();
    par[2][pos] = vect[2];
    return par;
  }
  
  double[] calcVect(CollisionItem item){
    double[] vect = new double[3]; //[x,y,z]
    vect[0] = item.getEnd().getX() - item.getOrigin().getX();
    vect[1] = item.getEnd().getY() - item.getOrigin().getY();
    vect[2] = item.getEnd().getZ() - item.getOrigin().getZ();
    return vect;
    
  }

  public boolean haveCollision() {
    // TODO Auto-generated method stub
    return collision;
  }

  public void clearItem() {
    // TODO Auto-generated method stub
    items.clear();
  }
  
  public Point[] getCollisionPoint() {
    return collisionPoint;
  }
  
  public void addIgnore(String object1, String object2) {
    if (items.containsKey(object1)) {
      items.get(object1).addIgnore(object2);
    }
    
  }
}

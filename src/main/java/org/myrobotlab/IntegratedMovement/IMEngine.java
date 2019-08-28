/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import org.myrobotlab.IntegratedMovement.CollisionDectection.CollisionResults;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.IntegratedMovement;
import org.myrobotlab.service.IntegratedMovement.ObjectPointLocation;
import org.slf4j.Logger;

/**
 * @author Christian
 *
 */
public class IMEngine extends Thread {
  public final static Logger log = LoggerFactory.getLogger(IMEngine.class);

  DHRobotArm arm, computeArm;
  public Point target = null;
  private transient IntegratedMovement service = null;
  private int Ai = IntegratedMovement.Ai.AVOID_COLLISION.value;

  public class MoveInfo {
    Point offset = null;
    CollisionItem targetItem = null;
    ObjectPointLocation objectLocation = null;
    DHLink lastLink = null;
    public String arm;
    public String lastLinkName;
  }

  MoveInfo moveInfo = null;




  public IMEngine(String name, IntegratedMovement IM) {
  }

  public IMEngine(String name, DHRobotArm arm, IntegratedMovement integratedMovement) {
  }


  public void setDHRobotArm(DHRobotArm dhArm) {
    arm = dhArm;
  }

  public void run() {
    while (true) {
      if (AiActive(IntegratedMovement.Ai.AVOID_COLLISION)) {
        Point avoidPoint = checkCollision(arm, service.collisionItems);
        if (avoidPoint != null) {
          Point previousTarget = target;
          target = avoidPoint;
          //move();
          target = previousTarget;
        }
      }
    }
  }

  private boolean AiActive(IntegratedMovement.Ai ai) {
    if ((Ai & ai.value) > 0) {
      return true;
    }
    return false;
  }

  private Point checkCollision(DHRobotArm arm, CollisionDectection cd) {
    DHRobotArm checkArm = new DHRobotArm(arm);
    double time = 0.0;
    double timePerLoop = 0.1;
    CollisionResults collisionResult = null;
    CollisionDectection ccd = new CollisionDectection(cd);
    while (time <= 2.0) {
      // rotate the checkArm by timePerLoop
      for (DHLink link : checkArm.getLinks()) {
        if (link.hasActuator) {
          double delta = link.getSpeed() * timePerLoop;
          double maxDelta = Math.abs(link.getTargetPos() - link.getPositionValueDeg());
          delta = Math.toRadians(Math.min(delta, maxDelta));
          if (link.getTargetPos() < link.getCurrentPos()) {
            delta *= -1;
          }
          link.incrRotate(delta);
        }
      }
      double[][] jp = checkArm.createJointPositionMap();
      // send data to the collision detector class
      for (int i = 0; i < checkArm.getNumLinks(); i++) {
        CollisionItem ci = new CollisionItem(new Point(jp[i][0], jp[i][1], jp[i][2], 0, 0, 0), new Point(jp[i + 1][0], jp[i + 1][1], jp[i + 1][2], 0, 0, 0),
            checkArm.getLink(i).getName());
        if (i != checkArm.getNumLinks() - 1) {
          ci.addIgnore(checkArm.getLink(i + 1).getName());
        }
        ccd.addItem(ci);
        if (time == 0.0) {
          cd.addItem(ci);
        }
      }
      collisionResult = ccd.runTest();
      if (collisionResult.haveCollision) {
        break;
      }
      time += timePerLoop;
    }
    if (collisionResult.haveCollision) {
      // log.info("collision detected");
      CollisionItem ci = null;
      int itemIndex = 0;
      for (DHLink l : checkArm.getLinks()) {
        boolean foundIt = false;
        for (itemIndex = 0; itemIndex < 2; itemIndex++) {
          if (l.getName().equals(collisionResult.collisionItems[itemIndex].getName())) {
            ci = collisionResult.collisionItems[itemIndex];
            foundIt = true;
            break;
          }
        }
        if (foundIt)
          break; // we have the item to watch
      }
      if (ci == null) {
        // log.info("Collision between static item {} and {} detected",
        // collisionResult.collisionItems[0].getName(),
        // collisionResult.collisionItems[1].getName());
        return null;
      }
      Point armPos = checkArm.getPalmPosition();
      Point newPos = checkArm.getPalmPosition();
      Point vCollItem = collisionResult.collisionPoints[itemIndex].subtract(collisionResult.collisionPoints[1 - itemIndex]);
      // if (vCollItem.magnitude() > 100){ // scale vector so the avoiding point
      // is not too far
      vCollItem = vCollItem.unitVector(100);
      // }
      newPos = newPos.add(vCollItem);
      Point ori = collisionResult.collisionItems[1 - itemIndex].getOrigin();
      Point end = collisionResult.collisionItems[1 - itemIndex].getEnd();
      Point colPoint = collisionResult.collisionPoints[1 - itemIndex];
      if (collisionResult.collisionLocation[1 - itemIndex] > 0.0 || collisionResult.collisionLocation[1 - itemIndex] < 1.0) { // collision
        // on
        // the
        // side
        // of
        // item
        Point vToEndOfObject;
        if (collisionResult.collisionLocation[1 - itemIndex] < 0.5) { // collision
          // near
          // the
          // origin
          vToEndOfObject = ori.subtract(colPoint);
          // newPos = newPos.add(ori).subtract(colPoint);
        } else { // collision near the end
          // newPos = newPos.add(end).subtract(colPoint);
          vToEndOfObject = end.subtract(colPoint);
        }
        vToEndOfObject = vToEndOfObject.unitVector(100);
        newPos = newPos.add(vToEndOfObject);
      }
      // move away of the part
      // double length =
      // collisionResult.collisionItems[1-itemIndex].getLength();
      // double ratio = collisionResult.collisionItems[itemIndex].getRadius() /
      // length;
      // double[] vector =
      // collisionResult.collisionItems[1-itemIndex].getVector();
      // for (int i=0; i<3; i++){
      // vector[i] *= ratio;
      // }
      // if (collisionResult.collisionLocation[1-itemIndex] < 0.5) { //collision
      // near the origin
      // newPos.setX(newPos.getX() - vector[0]);
      // newPos.setY(newPos.getY() - vector[1]);
      // newPos.setZ(newPos.getZ() - vector[2]);
      // }
      // else {
      // newPos.setX(newPos.getX() + vector[0]);
      // newPos.setY(newPos.getY() + vector[1]);
      // newPos.setZ(newPos.getZ() + vector[2]);
      // }
      // add a vector end point move toward the collision point
      Point vtocollpoint = armPos.subtract(colPoint);
      vtocollpoint = vtocollpoint.unitVector(100);
      newPos = newPos.add(vtocollpoint);
      // double distance = newPos.distanceTo(arm.getPalmPosition());
      // if (distance > 100) {
      // Point vtonewPos = arm.getPalmPosition().subtract(newPos);
      // vtonewPos = vtonewPos.multiplyXYZ(100/distance);
      // newPos = arm.getPalmPosition().add(vtonewPos);
      // }
      log.info("Avoiding position toward ", newPos.toString());
      return newPos;
    }
    return null;
  }

  public void moveTo(CollisionItem item, ObjectPointLocation location, String lastDHLink) {
    moveInfo = new MoveInfo();
    moveInfo.targetItem = item;
    moveInfo.objectLocation = location;
    moveInfo.lastLinkName = lastDHLink;
    if (moveInfo.targetItem == null) {
      log.info("no items named ", item.getName(), "found");
      moveInfo = null;
      return;
    }
    target = moveToObject();
    //service.getJmeApp().addPoint(target);
  }

  private Point moveToObject() {
    double safety = 10.0;
    Point[] point = new Point[2];
    if (moveInfo.lastLinkName == null) {
      moveInfo.lastLink = arm.getLink(arm.getNumLinks() - 1);
    } else {
      for (DHLink link : arm.getLinks()) {
        if (link.getName() == moveInfo.lastLinkName) {
          moveInfo.lastLink = link;
          break;
        }
      }
    }
    CollisionItem lastLinkItem = service.collisionItems.getItem(moveInfo.lastLink.getName());
    service.collisionItems.addIgnore(moveInfo.targetItem.name, lastLinkItem.name);
    Double[] vector = new Double[3];
    boolean addRadius = false;
    switch (moveInfo.objectLocation) {
      case ORIGIN_CENTER: {
        point[0] = moveInfo.targetItem.getOrigin();
        Point v = moveInfo.targetItem.getOrigin().subtract(moveInfo.targetItem.getEnd());
        v = v.unitVector(safety);
        point[0] = point[0].add(v);
        break;
      }
      case END_CENTER: {
        point[0] = moveInfo.targetItem.getEnd();
        Point v = moveInfo.targetItem.getEnd().subtract(moveInfo.targetItem.getOrigin());
        v = v.unitVector(safety);
        point[0] = point[0].add(v);
        break;
      }
      case CLOSEST_POINT: {
        point = service.collisionItems.getClosestPoint(moveInfo.targetItem, lastLinkItem, new Double[2], vector);
        addRadius = true;
        break;
      }
      case ORIGIN_SIDE: {
        point[0] = moveInfo.targetItem.getOrigin();
        Point v = moveInfo.targetItem.getOrigin().subtract(moveInfo.targetItem.getEnd());
        v = v.unitVector(safety);
        point[0] = point[0].add(v);
        addRadius = true;
        break;
      }
      case END_SIDE: {
        point[0] = moveInfo.targetItem.getEnd();
        Point v = moveInfo.targetItem.getEnd().subtract(moveInfo.targetItem.getOrigin());
        v = v.unitVector(safety);
        point[0] = point[0].add(v);
        addRadius = true;
        break;
      }
      case CENTER_SIDE:
      case LEFT_SIDE:
      case RIGHT_SIDE: {
        point = service.collisionItems.getClosestPoint(moveInfo.targetItem, lastLinkItem, new Double[] { 0.5, 0.5 }, vector);
        addRadius = true;
      }
      case CENTER: {
        point = service.collisionItems.getClosestPoint(moveInfo.targetItem, lastLinkItem, new Double[] { 0.5, 0.5 }, vector);
      }
    }
    if (addRadius) {
      double[] vectori = moveInfo.targetItem.getVector();
      double[] vectorT = moveInfo.targetItem.getVectorT();
      Point side0 = new Point(point[0].getX() + vectorT[0], point[0].getY() + vectorT[1], point[0].getZ() + vectorT[2], 0, 0, 0);
      Point v = new Point(vectorT[0], vectorT[1], vectorT[2], 0, 0, 0);
      v = v.unitVector(safety);
      side0 = side0.add(v);
      Point pointF = side0;
      Point curPos = arm.getPalmPosition(moveInfo.lastLinkName);
      double d = Math.pow((side0.getX() - curPos.getX()), 2) + Math.pow((side0.getY() - curPos.getY()), 2) + Math.pow((side0.getZ() - curPos.getZ()), 2);
      double currentx = side0.getX();
      for (int i = 0; i < 360; i += 10) {
        double L = vectori[0] * vectori[0] + vectori[1] * vectori[1] + vectori[2] * vectori[2];
        double x = ((moveInfo.targetItem.getOrigin().getX() * (Math.pow(vectori[1], 2) + Math.pow(vectori[2], 2))
            - vectori[0] * (moveInfo.targetItem.getOrigin().getY() * vectori[1] + moveInfo.targetItem.getOrigin().getZ() * vectori[2] - vectori[0] * side0.getX()
                - vectori[1] * side0.getY() - vectori[2] * side0.getZ()))
            * (1 - Math.cos(MathUtils.degToRad(i))) + L * side0.getX() * Math.cos(MathUtils.degToRad(i))
            + Math.sqrt(L) * (-moveInfo.targetItem.getOrigin().getZ() * vectori[1] + moveInfo.targetItem.getOrigin().getY() * vectori[2] - vectori[2] * side0.getY()
                + vectori[1] * side0.getZ()) * Math.sin(MathUtils.degToRad(i)))
            / L;
        double y = ((moveInfo.targetItem.getOrigin().getY() * (Math.pow(vectori[0], 2) + Math.pow(vectori[2], 2))
            - vectori[1] * (moveInfo.targetItem.getOrigin().getX() * vectori[0] + moveInfo.targetItem.getOrigin().getZ() * vectori[2] - vectori[0] * side0.getX()
                - vectori[1] * side0.getY() - vectori[2] * side0.getZ()))
            * (1 - Math.cos(MathUtils.degToRad(i))) + L * side0.getY() * Math.cos(MathUtils.degToRad(i))
            + Math.sqrt(L) * (moveInfo.targetItem.getOrigin().getZ() * vectori[0] - moveInfo.targetItem.getOrigin().getX() * vectori[2] + vectori[2] * side0.getX()
                - vectori[0] * side0.getZ()) * Math.sin(MathUtils.degToRad(i)))
            / L;
        double z = ((moveInfo.targetItem.getOrigin().getZ() * (Math.pow(vectori[0], 2) + Math.pow(vectori[1], 2))
            - vectori[2] * (moveInfo.targetItem.getOrigin().getX() * vectori[0] + moveInfo.targetItem.getOrigin().getY() * vectori[1] - vectori[0] * side0.getX()
                - vectori[1] * side0.getY() - vectori[2] * side0.getZ()))
            * (1 - Math.cos(MathUtils.degToRad(i))) + L * side0.getZ() * Math.cos(MathUtils.degToRad(i))
            + Math.sqrt(L) * (-moveInfo.targetItem.getOrigin().getY() * vectori[0] + moveInfo.targetItem.getOrigin().getX() * vectori[1] - vectori[1] * side0.getX()
                + vectori[0] * side0.getY()) * Math.sin(MathUtils.degToRad(i)))
            / L;
        Point check = new Point(x, y, z, 0, 0, 0);
        double dt = Math.pow((check.getX() - curPos.getX()), 2) + Math.pow((check.getY() - curPos.getY()), 2) + Math.pow((check.getZ() - curPos.getZ()), 2);
        if (moveInfo.objectLocation.equals(ObjectPointLocation.RIGHT_SIDE)) {
          if (check.getX() < currentx) {
            pointF = check;
            currentx = check.getX();
          }
        } else if (moveInfo.objectLocation.equals(ObjectPointLocation.LEFT_SIDE)) {
          if (check.getX() > currentx) {
            pointF = check;
            currentx = check.getX();
          }
        } else if (dt < d) {
          pointF = check;
          d = dt;
        }
      }
      point[0] = pointF;
    }

    Point moveToPoint = point[0];
    log.info("Moving to point ", moveToPoint);
    return moveToPoint;
  }




}

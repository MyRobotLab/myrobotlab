package org.myrobotlab.service;

import org.myrobotlab.framework.Service;

import junit.framework.Assert;

public class OculusRiftTest extends AbstractServiceTest {

  @Override
  public Service createService() {
    // TODO Auto-generated method stub
    OculusRift oculus = (OculusRift)Runtime.create("oculus", "OculusRift");
    return oculus;
  }

  @Override
  public void testService() throws Exception {
    // 
    OculusRift oculus = (OculusRift)service;
    
    
    
    oculus.updateAffine();
    
    oculus.setMirrorLeftCamera(false);
    oculus.setLeftCameraDx(0);
    Assert.assertEquals(0.0, oculus.getLeftCameraDx(), 0.001);
    oculus.setLeftCameraDy(0);
    Assert.assertEquals(0.0, oculus.getLeftCameraDy(), 0.001);
    oculus.setLeftCameraAngle(0);
    Assert.assertEquals(0.0, oculus.getLeftCameraAngle(), 0.001);
    oculus.setRightCameraDx(0);
    Assert.assertEquals(0.0, oculus.getRightCameraDx(), 0.001);
    oculus.setRightCameraDy(0);
    Assert.assertEquals(0.0, oculus.getRightCameraDy(), 0.001);
    oculus.setRightCameraAngle(0);
    Assert.assertEquals(0.0, oculus.getRightCameraAngle(), 0.001);
    
    oculus.setMirrorLeftCamera(false);
    oculus.initLeftOpenCV();
    oculus.initRightOpenCV();
    oculus.addUndistortFilter();
    oculus.addTransposeFilter();
    oculus.addAffineFilter();
    
    Assert.assertNotNull(oculus.leftOpenCV.getFilter("leftAffine"));
    Assert.assertNotNull(oculus.rightOpenCV.getFilter("rightAffine"));
    
    oculus.broadcastState();
    
    
  }

}


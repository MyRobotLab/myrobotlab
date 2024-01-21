package org.myrobotlab.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.TextPublisher;

public class ClassUtilTest implements ServoControl, TextPublisher {

  /**
   * because these 2 interfaces inherit multiple interfaces the total is currently 8
   */
  int numIntefaces = 8;

  @Test
  public void testGetInterfacesString() throws ClassNotFoundException {
    Set<String> interfaces = ClassUtil.getInterfaces(this.getClass().getCanonicalName());
    assertEquals("8 itnerfaces", 8, interfaces.size());
  }

  @Test
  public void testGetInterfacesClassOfQSetOfString() throws ClassNotFoundException {
    Set<String> filter = new HashSet<>();
    filter.add("org.myrobotlab.service.interfaces.TextPublisher");
    Set<String> interfaces = ClassUtil.getInterfaces(this.getClass(), filter);
    assertEquals("7 itnerfaces", 7, interfaces.size());
  }

  @Test
  public void testGetInterfacesStringSetOfString() throws ClassNotFoundException {
    Set<String> filter = new HashSet<>();
    filter.add("org.myrobotlab.service.interfaces.TextPublisher");
    Set<String> interfaces = ClassUtil.getInterfaces(this.getClass().getCanonicalName(), filter);
    assertEquals("7 itnerfaces", 7, interfaces.size());
  }

  @Override
  public Double moveTo(Integer newPos) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Double moveTo(Double newPos) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Double moveToBlocking(Integer newPos) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Double moveToBlocking(Integer newPos, Long timeoutMs) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void onEncoderData(EncoderData data) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void attach(Attachable service) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addListener(String localTopic, String otherService, String callback) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addListener(String localTopic, String otherService) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void removeListener(String localTopic, String otherService, String callback) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void removeListener(String localTopic, String otherService) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void attach(String serviceName) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void detach(Attachable service) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void detach(String serviceName) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void detach() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Set<String> getAttached() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<String> getAttached(String publishingPoint) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isAttached(Attachable instance) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isAttached(String name) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isLocal() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean hasInterface(String interfaze) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean hasInterface(Class<?> interfaze) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isType(Class<?> clazz) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isType(String clazz) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ServiceConfig load() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean save() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Service publishState() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void broadcastState() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void attach(ServoController listener) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void detach(ServoController listener) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void disable() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void enable() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean isAutoDisable() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String getController() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public EncoderControl getEncoder() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long getLastActivityTime() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Mapper getMapper() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getMax() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getMin() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getPin() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getCurrentInputPos() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getCurrentOutputPos() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getRest() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Double getSpeed() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getTargetOutput() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getTargetPos() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public boolean isBlocking() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isEnabled() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isInverted() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isMoving() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void map(double minX, double maxX, double minY, double maxY) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Double moveToBlocking(Double pos) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Double moveToBlocking(Double pos, Long timeoutMs) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void rest() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setAutoDisable(boolean autoDisable) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setInverted(boolean invert) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setMapper(Mapper m) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setMinMax(double minXY, double maxXY) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setMinMaxOutput(double minY, double maxY) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setPin(Integer pin) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setPin(String pin) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setPosition(double pos) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setRest(double rest) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setSpeed(Integer degreesPerSecond) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setSpeed(Double degreesPerSecond) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void stop() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void sync(ServoControl sc) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void sync(String name) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void unsync(String name) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void unsync(ServoControl sc) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void waitTargetPos() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeMicroseconds(int uS) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void attachServoController(String sc) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void fullSpeed() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String publishText(String text) {
    // TODO Auto-generated method stub
    return null;
  }

}

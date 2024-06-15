package org.myrobotlab.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.TestCatcher;
import org.myrobotlab.service.TestCatcher.Ball;
import org.myrobotlab.service.interfaces.HttpDataListener;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class MethodCacheTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(MethodCacheTest.class);

  static MethodCache cache;
  static TestCatcher tester;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    cache = MethodCache.getInstance();
    cache.clear();
    assertEquals("all clear should be 0", 0, cache.getObjectSize());
    tester = (TestCatcher) Runtime.start("tester", "TestCatcher");
  }

  /**
   * find missing methods which did not appear through first cache pass
   * 
   * @throws IllegalAccessException
   *           boom
   * @throws IllegalArgumentException
   *           boom
   * @throws InvocationTargetException
   *           boom
   * @throws ClassNotFoundException
   *           boom
   * 
   */
  @Test
  public void findTest() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {

    // FIXME FIXME - CodecUtils.decode("",
    // FIXME FIXME - verify cache entry exist after being resolved through
    // isAssignable !!!
    Object ret = null;
    Method method = null;

    // testing a method defined with an interface parameter and whos ordinal has
    // several other
    // definitions - this effectively should be one of the hardest strongly
    // typed situations to resolve
    method = cache.getMethod(TestCatcher.class, "invokeTest", (HttpDataListener) tester);
    ret = method.invoke(tester, tester);
    assertEquals("sent self as paraemeter", tester, ret);

    method = cache.getMethod(TestCatcher.class, "invokeTest", "echo");
    ret = method.invoke(tester, "echo");
    assertEquals("string param", "echo", ret);
    Registration registration = new Registration(tester);
    // interface test - method with a interface parameter
    method = cache.getMethod(Runtime.class, "register", registration);
    ret = method.invoke(Runtime.getInstance(), registration);
    assertEquals("registering", registration, ret);

    // primitive parameter only test
    method = cache.getMethod(TestCatcher.class, "primitiveOnlyMethod", 3);
    ret = method.invoke(tester, 5);
    assertEquals("getting a primitive param", 5, ret);

    // super overloaded test
    Integer[] testArray = new Integer[] { 3, 5, 10 };
    method = cache.getMethod(TestCatcher.class, "getPin", new Object[] { testArray });
    ret = method.invoke(tester, new Object[] { testArray });
    assertEquals("verifying array as param", testArray, ret);

    // a null value in a mutli-type call
    Object[] jsonParams = new Object[] { "\"hello world\"", "null", "3" };
    Object[] paramTypes = cache.getDecodedJsonParameters(TestCatcher.class, "testMultipleParamTypes", jsonParams);
    assertTrue("string as 1st param", paramTypes[0].getClass().equals(String.class));
    assertTrue("null as second", paramTypes[1] == null);
    assertTrue("Integer as third", paramTypes[2].getClass().equals(Integer.class));

    // method = cache.getMethod(TestCatcher.class, "testMultipleParamTypes", new
    // Object[] {"\"hello world\"", "null", "3" });
    method = cache.getMethod(TestCatcher.class, "testMultipleParamTypes", new Object[] { "\"hello world\"", "null", "3" });
    ret = method.invoke(tester, new Object[] { "hello world", null, 3 });
    assertEquals("multi parameter check on string", ret, "hello world");

  }

  @Test
  public void strongTypeTest() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {

    Object ret = null;
    Method method = null;

    method = cache.getMethod(TestCatcher.class, "getPin", 3);
    ret = method.invoke(tester, 3);
    assertEquals("strong type test pin as int",3, ret);

    method = cache.getMethod(TestCatcher.class, "primitiveOnlyMethod", 3);
    ret = method.invoke(tester, 5);
    assertEquals("primitive test int", 5, ret);

    Integer[] testArray = new Integer[] { 3, 5, 10 };
    method = cache.getMethod(TestCatcher.class, "getPin", new Object[] { testArray });
    ret = method.invoke(tester, new Object[] { testArray });
    assertEquals("array as param", testArray, ret);

  }

  @Test
  public void ancestorTest() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {

    Object ret = null;
    Method method = null;

    // service super type test
    method = cache.getMethod(TestCatcher.class, "stopService");
    ret = method.invoke(tester);
    assertEquals("inherited stopService method test", null, ret);

    method = cache.getMethod(TestCatcher.class, "isRunning");
    ret = method.invoke(tester);
    assertEquals("inherited isRunning method test",false, ret);

    method = cache.getMethod(TestCatcher.class, "startService");
    ret = method.invoke(tester);
    assertEquals("inherited startService method test", null, ret);

    method = cache.getMethod(TestCatcher.class, "isRunning");
    ret = method.invoke(tester);
    assertEquals("inherited startService method test", true, ret);

    Integer[] testArray = new Integer[] { 3, 5, 10 };
    method = cache.getMethod(TestCatcher.class, "getPin", new Object[] { testArray });
    ret = method.invoke(tester, new Object[] { testArray });
    assertEquals("array param in ancestor", testArray, ret);

  }

  /**
   * Testing json
   * 
   * @throws IllegalAccessException
   *           boom
   * @throws IllegalArgumentException
   *           boom
   * @throws InvocationTargetException
   *           boom
   * @throws ClassNotFoundException
   *           boom
   * @throws NoSuchMethodException
   *           boom
   * @throws SecurityException
   *           boom
   * @throws InstantiationException
   *           boom
   * 
   */
  @Test
  public void lossyJsonMethodTest()
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException {

    String[] encodedParams = new String[] { CodecUtils.toJson("blah") };

    Object[] params = cache.getDecodedJsonParameters(TestCatcher.class, "invokeTest", encodedParams);
    Method method = cache.getMethod(TestCatcher.class, "invokeTest", params);
    Object ret = method.invoke(tester, params);
    log.info("ret is {}", ret);
    log.info("ret returned {} of type {}", ret, ret.getClass().getSimpleName());
    assertNotNull("we should find our invokeTest method",ret); // arbitrary that i resolves to string

    encodedParams = new String[] { CodecUtils.toJson(5.0) };
    params = cache.getDecodedJsonParameters(TestCatcher.class, "onDouble", encodedParams);
    method = cache.getMethod(TestCatcher.class, "onDouble", params);
    ret = method.invoke(tester, params);
    log.info("ret returned {} of type {}", ret, ret.getClass().getSimpleName());
    assertEquals("looking for a double", 5.0, ret); // arbitrary float vs int/double

    encodedParams = new String[] { CodecUtils.toJson(5) };
    params = cache.getDecodedJsonParameters(TestCatcher.class, "onInt", encodedParams);
    method = cache.getMethod(TestCatcher.class, "onInt", params);
    ret = method.invoke(tester, params);
    log.info("ret returned {} of type {}", ret, ret.getClass().getSimpleName());
    assertEquals("looking for an int", 5, ret); // arbitrary float vs int/double

    // complex object throw
    Ball ball = new Ball();
    ball.name = "my ball";
    ball.type = "football";
    ball.rating = 5;
    encodedParams = new String[] { CodecUtils.toJson(ball) };
    params = cache.getDecodedJsonParameters(TestCatcher.class, "catchBall", encodedParams);
    method = cache.getMethod(TestCatcher.class, "catchBall", params);
    ret = method.invoke(tester, params);
    log.info("ret returned {} of type {}", ret, ret.getClass().getSimpleName());
    assertEquals("my ball", ball.name);
    assertTrue("verifying an object as parameter", 5 == ball.rating);
  }

  public static class TestClass {
    public int getInt(int i) {
      return i;
    }
  }

  @Test
  public void unknownClassInvokeOnTest() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
    MethodCache cache = MethodCache.getInstance();
    TestClass test = new TestClass();
    Integer r = (Integer) cache.invokeOn(test, "getInt", 7);
    assertTrue("verifying getInt is 7", 7 == r);
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.DEBUG);

      MethodCacheTest test = new MethodCacheTest();
      // test.strongTypeTest();
      test.findTest();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}

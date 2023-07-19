package org.myrobotlab.framework;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.PinListener;
import org.myrobotlab.test.AbstractTest;

import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ProxyFactoryTest extends AbstractTest {

    @BeforeClass
    public static void setup() {
        ProxyInterceptor.timeout = 1000;
    }

    @Test
    public void testProxyFactory() {

        Registration reg = new Registration("testID", "testService", "java:TestProxy");
        reg.interfaces = List.of(ServiceInterface.class.getName());
        ServiceInterface proxy = ProxyFactory.createProxyService(reg);
        // TimeoutException indicates we're getting all the way to
        // Service.waitOn(), so things *probably* work.
        // FIXME use mocks or startup a second instance to rigorously test
        assertThrows(TimeoutException.class, proxy::getName);
    }

    @Test
    public void testMultiInterfaces() {
        Registration reg = new Registration("testID", "testService", "java:TestProxy");
        reg.interfaces = List.of(ServiceInterface.class.getName(), PinListener.class.getName());
        ServiceInterface proxy = ProxyFactory.createProxyService(reg);
        assertTrue(PinListener.class.isAssignableFrom(proxy.getClass()));
        assertThrows(TimeoutException.class, () -> ((PinListener) proxy).getPin());
    }
}

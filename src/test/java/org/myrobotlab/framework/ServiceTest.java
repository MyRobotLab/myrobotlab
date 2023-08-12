package org.myrobotlab.framework;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.test.AbstractTest;

public class ServiceTest extends AbstractTest {

    public static class TestService extends Service<ServiceConfig> {

        private static final long serialVersionUID = 1L;

        /**
         * Constructor of service, reservedkey typically is a services name and inId
         * will be its process id
         *
         * @param reservedKey the service name
         * @param inId        process id
         */
        public TestService(String reservedKey, String inId) {
            super(reservedKey, inId);
        }
    }

    @Test
    public void testConfigListenerFiltering() {
        Platform.getLocalInstance().setId("test-id");
        TestService t = new TestService("test", "test-id");
        List<MRLListener> listeners = List.of(
                new MRLListener("meth", "webgui@webgui-client", "onMeth"),
                new MRLListener("meth", "random@test-id", "onMeth"),
                new MRLListener("meth", "random2@test-2-id", "onMeth")
        );
        t.apply(new ServiceConfig());
        t.outbox.notifyList = Map.of("meth", listeners);
        List<ServiceConfig.Listener> filtered = t.getFilteredConfig().listeners;
        assertEquals("random", filtered.get(0).listener);
        assertEquals("random2@test-2-id", filtered.get(1).listener);
        t.getFilteredConfig();
    }
}

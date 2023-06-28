package org.myrobotlab.framework;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * This class is used internally to intercept
 * all method calls being made to a generated ByteBuddy
 * proxy. The interceptor forwards all calls to a remote
 * instance identified by the id and name via
 * {@link Runtime#sendBlocking(String, Integer, String, Object...)}.
 * Thus, the service identified by id and name must exist and
 * must be reachable, otherwise the interception
 * will result in a {@link TimeoutException}.
 *
 * @author AutonomicPerfectionist
 */
public class ProxyInterceptor {

    protected static Logger log = LoggerFactory.getLogger(ProxyInterceptor.class);

    public static volatile int timeout = 3000;
    private final String name;

    private final String id;

    public ProxyInterceptor(String name, String id) {
        this.name = name;
        this.id = id;
    }


    @RuntimeType
    public Object intercept(@Origin Method method, @AllArguments Object... args) throws InterruptedException, TimeoutException {
        log.debug(
                "Executing proxy method {}@{}.{}({})",
                name,
                id,
                method,
                ((args == null) ? "" : Arrays.toString(args))
        );
        // Timeout should be more sophisticated for long blocking methods
        return Runtime.getInstance().sendBlocking(name + "@" + id, timeout, method.getName(),
                (args != null) ? args : new Object[0]);
    }
}

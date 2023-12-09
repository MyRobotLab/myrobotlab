package org.myrobotlab.framework;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

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

  /**
   * name of remote service
   */
  private final String name;

  /**
   * id of remote instance
   */
  private final String id;

  private final String typeKey;

  public ProxyInterceptor(String name, String id, String typeKey) {
    this.name = name;
    this.id = id;
    this.typeKey = typeKey;
    // this.state ?
  }

  /**
   * Name, Id, FullName, TypeKey and list of interfaces
   * are all available during registration, these methods
   * should not later go out to the client to resolve.
   * @return
   */
  public String getId() {
    return id;
  }

  /**
   * Name is availble at registration, don't need to ask the 
   * remote service again
   * @return
   */
  public String getName() {
    return name;
  }

  /**
   * Convenience method
   * @return
   */
  public String getFullName() {
    return String.format("%s@%s", name, id);
  }

  /**
   * Given on registration, don't need client to be queried for it
   * @return
   */
  public String getTypeKey() {
    return typeKey;
  }

  /**
   * A guess at what might be best
   */
  public String toString() {
    return CodecUtils.toJson(this);
  }

  @RuntimeType
  public Object intercept(@Origin Method method, @AllArguments Object... args)
      throws InterruptedException, TimeoutException {
    log.debug(
        "Executing proxy method {}@{}.{}({})",
        name,
        id,
        method,
        ((args == null) ? "" : Arrays.toString(args)));
    // Timeout should be more sophisticated for long blocking methods
    return Runtime.getInstance().sendBlocking(name + "@" + id, timeout, method.getName(),
        (args != null) ? args : new Object[0]);
  }
}

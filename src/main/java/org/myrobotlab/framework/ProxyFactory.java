package org.myrobotlab.framework;

import static net.bytebuddy.matcher.ElementMatchers.any;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;

/**
 * ProxyFactory takes a service description via {@link Registration} and uses
 * <a href="https://bytebuddy.net/#/">ByteBuddy</a> to generate a new class and
 * instance for it, delegating all method calls to a new instance of
 * {@link ProxyInterceptor}. The registration must contain at least the
 * fully-qualified name of {@link ServiceInterface} in its
 * {@link Registration#interfaces} list. If this name is not present, an
 * {@link IllegalArgumentException} is thrown.
 *
 * @author AutonomicPerfectionist
 */
public class ProxyFactory {

  transient public final static Logger log = LoggerFactory.getLogger(ProxyFactory.class);

  /**
   * Creates a proxy class and instantiates it using the given registration. If
   * the registration's {@link Registration#interfaces} list does not contain
   * the fully-qualified name of {@link ServiceInterface}, an
   * {@link IllegalArgumentException} is thrown. The generated proxy uses the
   * name, id, and interfaces present in the registration to create the new
   * service. The proxy delegates to a new instance of {@link ProxyInterceptor}.
   *
   * @param registration
   *                     The information required to generate a new proxy
   * @return A newly-instantiated proxy
   * @throws IllegalArgumentException
   *                                  if registration's interfaces list does not
   *                                  contain
   *                                  ServiceInterface
   */
  public static ServiceInterface createProxyService(Registration registration) {

    if (registration.interfaces == null) {
      log.info("remote did not provide any interfaces, creating minimal getId and getName from registration data");
    } else {
      // TODO add caching support
      if (!registration.interfaces.contains(ServiceInterface.class.getName())) {
        throw new IllegalArgumentException("Registration must list at least ServiceInterface in the interfaces list.");
      }
    }
    ByteBuddy buddy = new ByteBuddy();
    DynamicType.Builder<?> builder = buddy.subclass(Object.class);
    List<Class<?>> interfaceClasses = registration.interfaces.stream().map(i -> {
      try {
        return Class.forName(i);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Unable to load interface " + i + " for registration " + registration, e);
      }
    }).collect(Collectors.toList());

    builder = builder.implement(interfaceClasses).method(any()).intercept(MethodDelegation.withDefaultConfiguration()
        .to(new ProxyInterceptor(registration.name, registration.id, registration.typeKey)));

    Class<?> proxyClass = builder.make().load(ProxyFactory.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
        .getLoaded();
    try {
      // We never defined any constructors so the default no-args is available
      ServiceInterface si = (ServiceInterface) proxyClass.getConstructors()[0].newInstance();
      MethodCache cache = MethodCache.getInstance();
      cache.cacheMethodEntries(si.getClass());
      return si;

    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      // Really shouldn't happen since we have full control over the
      // newly-generated class
      throw new RuntimeException(e);
    }
  }
}

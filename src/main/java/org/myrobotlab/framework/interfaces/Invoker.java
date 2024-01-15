package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.StaticType;

public interface Invoker {

  default Object invoke(String method) {
    return invoke(method, new StaticType<>(){});
  }

  default <R> R invoke(String method, StaticType<R> returnType) {
    return invoke(method, returnType, new Object[0]);
  }


  default Object invoke(String method, Object... params) {
    return invoke(method, new StaticType<>() {}, params);
  }
  <R> R invoke(String method, StaticType<R> returnType, Object... params);

}

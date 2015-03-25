package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Message;

// TODO - maybe "publisher" is a better Interface name ?  publish vs invoke
public interface Invoker {
	public Object invoke(Message msg);

	public Object invoke(String method);

	public Object invoke(String method, Object... params);

	public Object invokeOn(Object obj, String method, Object... params);
}

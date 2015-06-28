package org.myrobotlab.codec;

import java.lang.reflect.Method;
import java.util.HashMap;

public class MethodCache {

	static final private HashMap<String, Method> cache = new HashMap<String, Method>();

	static final public String getSignature(Class<?> clazz, String methodName, int ordinal) {
		return String.format("%s/%s-%d", clazz.getSimpleName(), methodName, ordinal);
	}

	static final public Class<?>[] getCandidateOnOrdinalSignature(Class<?> clazz, String methodName, int ordinal) throws NoSuchMethodException {
		String signature = getSignature(clazz, methodName, ordinal);
		if (cache.containsKey(signature)) {
			Method m = cache.get(signature);
			return m.getParameterTypes();
		} else {
			// changed to getMethods to support inheritance
			// if failure - overloading funny re-implementing a vTable in c++
			// Method[] methods = clazz.getDeclaredMethods();
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				
				// FIXME - future Many to one Map - if incoming data can "hint" would be
				// an optimization
				if (methodName.equals(method.getName())){
					return method.getParameterTypes();
				}
			}			
			throw new NoSuchMethodException(String.format("could not find %s.%s in declared methods", clazz.getSimpleName(), methodName));
		}
		
	}

	final public static void cache(Class<?> clazz, Method method) {
		cache.put(getSignature(clazz, method.getName(), method.getParameterTypes().length), method);
	}

}

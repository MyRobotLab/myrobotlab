package org.myrobotlab.utils;

import org.myrobotlab.framework.StaticType;

/**
 * A container class that holds an object with its
 * associated type information. This makes
 * constraining objects and types while inside of collections
 * or other containers easier.
 * @param <T> The type of the object contained
 * @author AutonomicPerfectionist
 */
public class ObjectTypePair<T> extends Pair<T, StaticType<T>> {
    public ObjectTypePair(T first, StaticType<T> second) {
        super(first, second);
    }
}

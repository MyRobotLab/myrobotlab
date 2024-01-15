package org.myrobotlab.utils;


/**
 * A simple container for two objects
 * of potentially different types. Allows
 * generics to be leveraged for type constraining
 * or more compiler-friendly collections operations.
 *
 * @param <A> The type of {@link #first}
 * @param <B> The type of {@link #second}
 * @author AutonomicPerfectionist
 */
public class Pair<A, B> {
    public A first;
    public B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

}

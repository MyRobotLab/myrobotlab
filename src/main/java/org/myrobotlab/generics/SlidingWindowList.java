package org.myrobotlab.generics;

import java.util.ArrayList;

public class SlidingWindowList<E> extends ArrayList<E> {
  private static final long serialVersionUID = 1L;
  private final int maxSize;

  public SlidingWindowList(int maxSize) {
      this.maxSize = maxSize;
  }
  
  @Override
  public boolean add(E element) {
      boolean added = super.add(element);
      if (size() > maxSize) {
          removeRange(0, size() - maxSize); // Remove oldest elements if size exceeds maxSize
      }
      return added;
  }

}

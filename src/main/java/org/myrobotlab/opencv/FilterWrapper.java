package org.myrobotlab.opencv;

import java.io.Serializable;

/**
 * @author GroG
 *
 *         Class to wrap a OpenCVFilter, but to hide its type. Otherwise all
 *         OpenCVFilter types would need to be stubbed out in the OpenCV
 *         service. This way the OpenCV is oblivious to the type - making invoke
 *         upcasting unnecessary.
 * 
 *         This does a setFilterState - shallow copy which is a reflective copy
 *         of data variables, very handy, but will be problematic for any JNI or
 *         pointer structures - these MUST be marked transient !
 */
public class FilterWrapper implements Serializable {

  private static final long serialVersionUID = 1L;
  public final String name;
  public final OpenCVFilter filter;

  public FilterWrapper(String name, OpenCVFilter filter) {
    this.name = name;
    this.filter = filter;
  }

}

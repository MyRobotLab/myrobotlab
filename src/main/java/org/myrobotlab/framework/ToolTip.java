/**
 * 
 */
package org.myrobotlab.framework;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author gperry
 * 
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface ToolTip {
  public String value();
}

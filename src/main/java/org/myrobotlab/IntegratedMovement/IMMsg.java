/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

/**
 * @author Calamity
 *
 */
public class IMMsg {
	  String method;
	  Object data[];
	  
	  public IMMsg(String method, Object[] params) {
	    this.method = method;
	    this.data = params;
	  }

}

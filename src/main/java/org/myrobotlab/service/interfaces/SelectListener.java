package org.myrobotlab.service.interfaces;

/**
 * A very generalized listener that listens to a "selected" method, where
 * the selected is identified with a string and is of some interest.
 * 
 * @author grog
 *
 */
public interface SelectListener extends Listener {

  /**
   * event and id of the "selected"
   * @param selected
   */
  public void onSelected(String selected);
}

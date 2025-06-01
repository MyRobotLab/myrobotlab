package org.myrobotlab.service.data;

/**
 * Range with source -
 * 
 * @author grog
 *
 */
public class RangeData extends AbstractData {

  public RangeData(String name, Double range) {
    super(name);
    this.range = range;
  }

  public Double range;

}

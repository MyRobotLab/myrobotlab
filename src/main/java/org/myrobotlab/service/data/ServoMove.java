package org.myrobotlab.service.data;

/**
 * 
 * @author GroG
 *
 */
public class ServoMove {
  /**
   * name of the servo moving
   */
  public String name;
  /**
   * the target input value sent to the servo
   */
  public Double inputPos;
  /**
   * the input value after the servos mapper has been applied
   */
  public Double outputPos;

  public ServoMove(final String name, final Double inputPos, final Double outputPos) {
    this.name = name;
    this.inputPos = inputPos;
    this.outputPos = outputPos;
  }

  @Override
  public String toString() {
    return String.format("%s inputPos %.2f outputPos %.2f", name, inputPos, outputPos);
  }

}

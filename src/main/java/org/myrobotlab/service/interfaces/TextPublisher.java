package org.myrobotlab.service.interfaces;

public interface TextPublisher {

  public String getName();

  public String publishText(String text);

  @Deprecated /* use standard attachTextListener */
  public void addTextListener(TextListener service);

  public void attachTextListener(TextListener service);

  /**
   * the root for any other attachTextListener type
   * @param name - the name of the text listener
   */
  public void attachTextListener(String name);

}

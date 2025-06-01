package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.ImageData;

public interface ImageListener extends NameProvider {

  /**
   * Receives an image
   * 
   * @param img
   *          - standard java.net.URI local files are file:///path
   */
  public void onImage(ImageData img);

  default public void attachImagePublisher(ImagePublisher display) {
    attachImagePublisher(display.getName());
  }

  // Default way to attach an image listener so implementing classes need
  // not worry about these details.
  default public void attachImagePublisher(String name) {
    send(name, "attachImageListener", getName());
  }

  default public void detachImagePublisher(ImagePublisher display) {
    detachImagePublisher(display.getName());
  }

  // Default way to attach an image listener so implementing classes need
  // not worry about these details.
  default public void detachImagePublisher(String name) {
    send(name, "detachImageListener", getName());
  }

  public void send(String name, String method, Object... data);

}

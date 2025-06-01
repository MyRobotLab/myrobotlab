package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.ImageData;

public interface ImagePublisher extends NameProvider {

  // These are all the methods that the image publisher should produce.
  public static String[] publishMethods = new String[] { "publishImage" };

  /**
   * Publishes an image
   * 
   * @param data
   *          - standard java.net.URI local files are file:///path
   */
  public ImageData publishImage(ImageData data);

  default public void attachImageListener(ImageListener display) {
    attachImageListener(display.getName());
  }

  // Default way to attach an image listener so implementing classes need
  // not worry about these details.
  default public void attachImageListener(String name) {
    for (String publishMethod : ImagePublisher.publishMethods) {
      addListener(publishMethod, name);
    }
  }

  default public void detachImageListener(ImageListener display) {
    detachImageListener(display.getName());
  }

  // Default way to attach an image listener so implementing classes need
  // not worry about these details.
  default public void detachImageListener(String name) {
    for (String publishMethod : ImagePublisher.publishMethods) {
      removeListener(publishMethod, name);
    }
  }

  // Add the addListener method to the interface all services implement this.
  public void addListener(String topicMethod, String callbackName);

  public void removeListener(String topicMethod, String callbackName);

}

package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.ImageData;

public interface ImageListener extends NameProvider {
  
  /**
   * Receives an image
   * @param img - standard java.net.URI local files are
   * file:///path 
   */
 public void onImage(ImageData img);

}

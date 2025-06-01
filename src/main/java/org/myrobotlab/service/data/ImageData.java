package org.myrobotlab.service.data;

public class ImageData {

  /**
   * service source this image came from
   */
  public String source;

  /**
   * uri source of image could be a network source http://imageserver/img1.jpg
   * or a local file:///somepath/image1.png
   */
  public String src;

  /**
   * the way the image is encoded
   */
  public String encoding;

  /**
   * name that was given to this image
   */
  public String name;

  public ImageData() {
  }

  public ImageData(String src) {
    this.src = src;
  }

}

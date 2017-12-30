/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.opencv;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import org.bytedeco.javacpp.opencv_core.IplImage;

public class FilterSobel {

  public FilterSobel(String CFGRoot, String name) {
    // super(CFGRoot, name);
    // TODO Auto-generated constructor stub
  }

  public BufferedImage display(IplImage image) {
    return null;
  }

  /*
   * @Override public Object process(BufferedImage image) {
   * 
   * return null; }
   */

  public String getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  public void loadDefaultConfiguration() {

  }

  public Object process(BufferedImage output, BufferedImage image) {

    // output = verifyOutput(output, image);

    // BufferedImage output = new BufferedImage(image.getWidth(),
    // image.getHeight(), image.getType());
    // WritableRaster raster = image.copyData( null );
    // BufferedImage output = new BufferedImage( image.getColorModel(),
    // raster, image.isAlphaPremultiplied(), null );

    Raster in_pixels = image.getRaster();
    WritableRaster out_pixels = output.getRaster();

    short gc;
    int a, b, c, d, e, f, g, h, z;

    float sobscale = 1;
    float offsetval = 0;

    int i = 1, j = 0;
    int final_i = image.getWidth() - 1;
    int final_j = image.getHeight() - 1;

    for (int bnd = 0; bnd < in_pixels.getNumBands(); bnd++) {
      for (j = 1; j < final_j; j++) {
        a = in_pixels.getSample(i - 1, j - 1, bnd);
        b = in_pixels.getSample(i, j - 1, bnd);
        d = in_pixels.getSample(i - 1, j, bnd);
        f = in_pixels.getSample(i - 1, j + 1, bnd);
        g = in_pixels.getSample(i, j + 1, bnd);
        z = in_pixels.getSample(i, j, bnd);

        for (i = 1; i < final_i; i++) {
          c = in_pixels.getSample(i + 1, j - 1, bnd);
          e = in_pixels.getSample(i + 1, j, bnd);
          h = in_pixels.getSample(i + 1, j + 1, bnd);

          int hor = (a + d + f) - (c + e + h); // The Sobel algorithm
          if (hor < 0)
            hor = -hor;
          int vert = (a + b + c) - (f + g + h);
          if (vert < 0)
            vert = -vert;

          gc = (short) (sobscale * (hor + vert));
          gc = (short) (gc + offsetval);

          gc = (gc > 255) ? 255 : gc;

          out_pixels.setSample(i, j, bnd, gc);

          a = b;
          b = c;
          d = z;
          f = g;
          g = h;
          z = e;
        }

        i = 1;
      }
    }

    return output;
    // return null;
  }

  public IplImage process(IplImage image, Object[] data) {
    return image;
  }

}

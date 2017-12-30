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

package org.myrobotlab.image;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

/*
 * References :
 * http://www.generation5.org/jdk/ - org.generation5.vision.SobelEdgeDetectorFilter
 * http://www.generation5.org/articles.asp?Action=List&Topic=Machine%20Vision - lots of info MS sdk? yuk
 */

public class Algorithm {

  public final static int INVERT = 0;

  public final static int OUTLINE = 1;

  public static Color average(BufferedImage image, Rectangle targetArea) {
    return average(image, targetArea, 1);
  }

  public static Color average(BufferedImage image, Rectangle targetArea, int density) {

    // Assuming that all images have the same dimensions
    int w = targetArea.width + targetArea.x;
    int h = targetArea.height + targetArea.y;

    int sumRed = 0;
    int sumGreen = 0;
    int sumBlue = 0;

    int cnt = 0;

    for (int y = targetArea.y; y < h; y += density)
      for (int x = targetArea.x; x < w; x += density) {
        Color c = new Color(image.getRGB(x, y)); // TODO this seems
        // wholly
        // inefficient - fix
        // me
        sumRed += c.getRed();
        sumGreen += c.getGreen();
        sumBlue += c.getBlue();
        ++cnt;
      }

    Color retColor = new Color(sumRed / cnt, sumGreen / cnt, sumBlue / cnt);

    return retColor;
  }

  public static BufferedImage average(BufferedImage[] images) {

    int n = images.length;

    // Assuming that all images have the same dimensions
    int w = images[0].getWidth();
    int h = images[0].getHeight();

    BufferedImage average = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

    WritableRaster raster = average.getRaster().createCompatibleWritableRaster();

    for (int y = 0; y < h; ++y)
      for (int x = 0; x < w; ++x) {

        float sum = 0.0f;

        for (int i = 0; i < n; ++i)
          sum = sum + images[i].getRaster().getSample(x, y, 0);

        raster.setSample(x, y, 0, Math.round(sum / n));
      }

    average.setData(raster);

    return average;
  }

}

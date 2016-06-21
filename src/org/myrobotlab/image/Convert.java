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

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

/* not worth it
 import javax.media.jai.JAI;
 import javax.media.jai.PlanarImage;
 */
public class Convert {

  final static public BufferedImage convertRenderedImage(RenderedImage img) {
    if (img instanceof BufferedImage) {
      return (BufferedImage) img;
    }
    ColorModel cm = img.getColorModel();
    int width = img.getWidth();
    int height = img.getHeight();
    WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    Hashtable<String, Object> properties = new Hashtable<String, Object>();
    String[] keys = img.getPropertyNames();
    if (keys != null) {
      for (int i = 0; i < keys.length; i++) {
        properties.put(keys[i], img.getProperty(keys[i]));
      }
    }
    BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
    img.copyData(raster);
    return result;
  }

  public static BufferedImage toImage(int w, int h, byte[] data) {
    DataBuffer buffer = new DataBufferByte(data, w * h);

    int pixelStride = 3; // assuming r, g, b, r, g, b,...
    int scanlineStride = 3 * w; // no extra padding
    int[] bandOffsets = { 0, 1, 2 }; // r, g, b
    WritableRaster raster = Raster.createInterleavedRaster(buffer, w, h, scanlineStride, pixelStride, bandOffsets, null);

    ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    boolean hasAlpha = false;
    boolean isAlphaPremultiplied = false;
    int transparency = Transparency.OPAQUE;
    int transferType = DataBuffer.TYPE_BYTE;
    ColorModel colorModel = new ComponentColorModel(colorSpace, hasAlpha, isAlphaPremultiplied, transparency, transferType);

    return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
  }

  /*
   * static public PlanarImage bufferedImageToPlanarImage(BufferedImage bi) {
   * ParameterBlock pb = new ParameterBlock(); pb.add(bi);
   * 
   * return (PlanarImage) JAI.create("AWTImage", pb); }
   */

}

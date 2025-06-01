package org.myrobotlab.boofcv;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.gui.image.ShowImages;
import boofcv.gui.tracker.TrackerObjectQuadPanel;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import georegression.struct.shapes.Quadrilateral_F64;

public class BoofCVFilterTrackerObjectQuad extends BoofCVFilter {

  protected transient TrackerObjectQuad tracker = null;

  protected Quadrilateral_F64 location = null;

  protected boolean visible = true;

  protected boolean trackerInitialized = false;

  protected transient TrackerObjectQuadPanel gui = null;

  public BoofCVFilterTrackerObjectQuad(String name) {
    super(name);
  }

  @Override
  public ImageBase<?> process(ImageBase<?> frame) throws InterruptedException {

    if (!trackerInitialized && location != null) {
      tracker = FactoryTrackerObjectQuad.circulant(null, GrayU8.class);
      tracker.initialize(frame, location);
      trackerInitialized = true;
    }

    // FIXME - probably replace BoofCV servie native gui with gui supplied by
    // filter
    //
    if (gui == null) {
      gui = new TrackerObjectQuadPanel(null);
      gui.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));
      gui.setImageUI((BufferedImage) boofcv.getGuiImage());
      gui.setTarget(location, true);
      ShowImages.showWindow(gui, "Tracking Results", true);
    }

    if (location != null) {
      visible = tracker.process(frame, location);

      if (gui != null) {
        gui.setImageUI((BufferedImage)boofcv.getGuiImage());
        gui.setTarget(location, visible);
        gui.repaint();
      }
    }

    return frame;
  }

  public void setLocation(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3) {
    location = new Quadrilateral_F64(x0, y0, x1, y1, x2, y2, x3, y3);
    trackerInitialized = false;
  }

}

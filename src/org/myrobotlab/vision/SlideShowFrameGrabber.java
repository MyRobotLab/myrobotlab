package org.myrobotlab.vision;

import java.io.File;
import java.util.ArrayList;

import org.bytedeco.javacv.Frame;

public class SlideShowFrameGrabber extends ImageFileFrameGrabber {

  // delay in ms between grabs.
  public int delay = 1000;

  public String directory = "training";

  private ArrayList<File> imageFiles = new ArrayList<File>();
  private int grabCount = 0;

  public SlideShowFrameGrabber(String path) {
    super(path);
    // load up the image files in the file directory.
    // TODO: some sort of frame grabber life cycle mgmt.
    loadDirectory();
  }

  public void loadDirectory() {
    File folder = new File(directory);
    File[] listOfFiles = folder.listFiles();
    for (File file : listOfFiles) {
      if (file.isFile()) {
        // TODO: check what formats opencv's cvLoadImage supports and add that
        // here.
        if (file.getName().toLowerCase().endsWith("png") || file.getName().toLowerCase().endsWith("jpg")) {
          // It's an image file! ish...
          imageFiles.add(file);
        }
      }
    }
  }

  @Override
  public Frame grab() {
    try {
      // pause for the specified delay before loading the image.
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // set the file path
    path = imageFiles.get(grabCount).getAbsolutePath();
    log.info("Grabbing file {} - {}", grabCount, path);
    // grab it.
    Frame f = super.grab();
    // increment out count.
    grabCount++;
    grabCount = grabCount % imageFiles.size();

    return f;
  }

  public int getDelay() {
    return delay;
  }

  public void setDelay(int delay) {
    this.delay = delay;
  }

  public String getDirectory() {
    return directory;
  }

  public void setDirectory(String directory) {
    this.directory = directory;
  }

}

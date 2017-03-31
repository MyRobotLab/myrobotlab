package org.myrobotlab.boofcv;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.openkinect.freenect.Context;
import org.openkinect.freenect.DepthFormat;
import org.openkinect.freenect.DepthHandler;
import org.openkinect.freenect.Device;
import org.openkinect.freenect.FrameMode;
import org.openkinect.freenect.Freenect;
import org.openkinect.freenect.VideoFormat;
import org.openkinect.freenect.VideoHandler;

import com.sun.jna.NativeLibrary;

import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;

/**
 * Example demonstrating how to process and display data from the Kinect.
 *
 * @author Peter Abeles
 */
public class KinectStreamer {

  {
    // be sure to set OpenKinectExampleParam.PATH_TO_SHARED_LIBRARY to the
    // location of your shared library!
    NativeLibrary.addSearchPath("freenect", "/home/pja/projects/thirdparty/libfreenect/build/lib");
  }

  Planar<GrayU8> rgb = new Planar<GrayU8>(GrayU8.class, 1, 1, 3);
  GrayU16 depth = new GrayU16(1, 1);

  BufferedImage outRgb;
  ImagePanel guiRgb;

  BufferedImage outDepth;
  ImagePanel guiDepth;

  public void process() {
    Context kinect = Freenect.createContext();

    if (kinect.numDevices() < 0)
      throw new RuntimeException("No kinect found!");

    Device device = kinect.openDevice(0);

    device.setDepthFormat(DepthFormat.REGISTERED);
    device.setVideoFormat(VideoFormat.RGB);

    device.startDepth(new DepthHandler() {
      @Override
      public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {
        processDepth(mode, frame, timestamp);
      }
    });
    device.startVideo(new VideoHandler() {
      @Override
      public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {
        processRgb(mode, frame, timestamp);
      }
    });

    long starTime = System.currentTimeMillis();
    while (starTime + 100000 > System.currentTimeMillis()) {
    }
    System.out.println("100 Seconds elapsed");

    device.stopDepth();
    device.stopVideo();
    device.close();

  }

  protected void processDepth(FrameMode mode, ByteBuffer frame, int timestamp) {
    System.out.println("Got depth! " + timestamp);

    if (outDepth == null) {
      depth.reshape(mode.getWidth(), mode.getHeight());
      outDepth = new BufferedImage(depth.width, depth.height, BufferedImage.TYPE_INT_RGB);
      guiDepth = ShowImages.showWindow(outDepth, "Depth Image");
    }

    UtilOpenKinect.bufferDepthToU16(frame, depth);

    // VisualizeImageData.grayUnsigned(depth,outDepth,UtilOpenKinect.FREENECT_DEPTH_MM_MAX_VALUE);
    VisualizeImageData.disparity(depth, outDepth, 0, UtilOpenKinect.FREENECT_DEPTH_MM_MAX_VALUE, 0);
    guiDepth.repaint();
  }

  protected void processRgb(FrameMode mode, ByteBuffer frame, int timestamp) {
    if (mode.getVideoFormat() != VideoFormat.RGB) {
      System.out.println("Bad rgb format!");
    }

    System.out.println("Got rgb!   " + timestamp);

    if (outRgb == null) {
      rgb.reshape(mode.getWidth(), mode.getHeight());
      outRgb = new BufferedImage(rgb.width, rgb.height, BufferedImage.TYPE_INT_RGB);
      guiRgb = ShowImages.showWindow(outRgb, "RGB Image");
    }

    UtilOpenKinect.bufferRgbToMsU8(frame, rgb);
    ConvertBufferedImage.convertTo_U8(rgb, outRgb, true);

    guiRgb.repaint();
  }

  public static void main(String args[]) {
    KinectStreamer app = new KinectStreamer();

    app.process();
  }
}
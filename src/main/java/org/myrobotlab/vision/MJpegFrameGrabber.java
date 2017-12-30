package org.myrobotlab.vision;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import net.sf.jipcam.axis.MjpegInputStream;

public class MJpegFrameGrabber extends FrameGrabber {

  transient public final static Logger log = LoggerFactory.getLogger(MJpegFrameGrabber.class);

  private URL url;
  private MjpegInputStream mStream;
  transient private Java2DFrameConverter converter = new Java2DFrameConverter();
  
  public MJpegFrameGrabber(String uri) {
    super();
    try {
    	url = new URL(uri);
	} catch (MalformedURLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }

  @Override
  public void start() throws Exception {
    try {
		mStream = new MjpegInputStream(url.openStream());
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return;
	}
    log.info("MJPEG Stream Open {}", url.toString());
  }

  @Override
  public void stop() throws Exception {
    log.info("Framegrabber stop called");
    try {
		mStream.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return;
	}
  }

  @Override
  public void trigger() throws Exception {
    log.info("Framegrabber tigger called");
  }

  @Override
  public Frame grab() throws Exception {
	
    BufferedImage img;
	try {
		img = (BufferedImage)(mStream.readMjpegFrame().getImage());
	    // Frame frame = converter.getFrame(img, 1.0, true);
	    Frame frame = converter.getFrame(img);
	    return frame;
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return null;
	}
  }

  @Override
  public void release() throws Exception {
    // should we close here? or somewhere else?
    log.info("Framegrabber release called");
    try {
		mStream.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }

}

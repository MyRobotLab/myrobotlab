package org.myrobotlab.service.interfaces;

/**
 * Class for managing the thread &amp; callback to do processing of video.
 * Although this is not formally an interface, it 'could' be an abtract class.
 * Since Service's are Runnable, and the run function within them manages the messaging framework,
 * this class is needed to implement 'another' Runnable run, which calls back to the appropriate method
 * named 'processVideo' - The  complexity of really processing the video is to be implemented in the 
 * VideoProcessor. This class's function is only to manage threads.
 * 
 * It could be an abstract class, and you could derive a myriad of other implementations, but I would
 * advise against it, and put all the complexity into the VideoProcessor Service.
 * 
 * @author GroG
 *
 */
public class VideoProcessWorker implements Runnable {
  
  VideoProcessor videoProcessor;
  boolean processing = false;
  transient Thread worker;

  public VideoProcessWorker(VideoProcessor videoProcessor){
    this.videoProcessor = videoProcessor;
  }
  
  // throw from video processing method
  public void run(){
    processing = true;
    while (processing){
      videoProcessor.processVideo();
    }
  }
  
  public void start(){
    if (worker == null){
      worker = new Thread(this, videoProcessor.getName() + ".videoProcessor");
      worker.start();
    }
  }
  
  public void stop(){
    processing = false;
    if (worker != null){
      worker.interrupt();
    }
    worker = null;
  }

  public boolean isProcessing() {
    return worker != null;
  }
}

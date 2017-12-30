package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.ServiceInterface;

public interface VideoProcessor extends ServiceInterface {
  
  default boolean publishCapturing(Boolean b){
    return b;
  }
  
  /**
   * This is where video is processed.
   * In OpenCV a frame is grabbed and sent through a series of filters
   */
  public void processVideo();
  
  default public void startVideoProcessing(){
    VideoProcessWorker vpw = getWorker();
    vpw.start();
    invoke("publishCapturing", true);
  }
  
  default public void stopVideoProcessing(){
    VideoProcessWorker vpw = getWorker();
    vpw.stop();
    invoke("publishCapturing", false);
  }

  default public boolean isProcessing(){
    VideoProcessWorker vpw = getWorker();
    return vpw.isProcessing();
  }
  
  public VideoProcessWorker getWorker();
  
  public boolean isCapturing();
}

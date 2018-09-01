package org.myrobotlab.opencv;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.indexer.Indexer;

import static org.bytedeco.javacpp.opencv_imgproc.undistort;

import javax.swing.WindowConstants;

import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;

public class OpenCVFilterUndistort extends OpenCVFilter {

  /**
   * an undistortion filter that can add/remove a fisheye distortion to the image. 
   * Based on blog post here : https://github.com/opencv/opencv/blob/master/samples/cpp/calibration.cpp
   * and CV docs here: https://docs.opencv.org/3.4.1/db/d58/group__calib3d__fisheye.html#ga167df4b00a6fd55287ba829fbf9913b9
   * 
   * The following code and coeffiecnets are taken directly from this stack overflow post.
   * https://stackoverflow.com/questions/40545992/opencv-how-to-provide-matrix-for-undistort-if-i-know-lens-correction-factor
   * 
   * This one is the bomb!
   * https://medium.com/@kennethjiang/calibrate-fisheye-lens-using-opencv-333b05afa0b0
   * 
   * 
   * This filter does not implement the calibration logic.. it expects the calibration matrix is already known
   * and its hard coded!  oops.. TODO:  make it configurable.
   */
  private static final long serialVersionUID = 1L;

  
  private Mat camMat  = new Mat(3, 3, CV_32FC1);
  // now what's the distVec?
  private Mat distVec = new Mat(1, 5, CV_32FC1);

  
  private transient OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();

  public OpenCVFilterUndistort() {
    super();
    initCameraAndDistortionMatrix();
  }

  public OpenCVFilterUndistort(String name) {
    super(name);
    initCameraAndDistortionMatrix();
  }

  public OpenCVFilterUndistort(String filterName, String sourceKey) {
    super(filterName, sourceKey);
    initCameraAndDistortionMatrix();
  }
  
  // TODO: expose this stuff in a way that people can calibrate it.
  public void initCameraAndDistortionMatrix() {
    // TODO: no idea what good values are here!!!
    double cameraFocal = 1.4656877976320607e+03;
    double cameraCX    = 640.0/2;
    double cameraCY    = 480.0/2;
    //    double cameraCX    = 1920.0/2;
    //    double cameraCY    = 1080.0/2;
    double[] cameraMatrixData = new double[]{
        cameraFocal, 0.0        , cameraCX,
        0.0        , cameraFocal, cameraCY,
        0.0        , 0.0        , 1.0
    };
    // double[] distMatrixData = new double[]{-0.4016824381742f, 0.04368842493074f, 0.0f, 0.0f, 0.1096412142704f};
    double[] distMatrixData = new double[]{-5f, 0.04368842493074f, 0.0f, 0.0f, 0.1096412142704f};
    //double[] distMatrixData = new double[]{-0.4016824381742f, 0.04368842493074f, 1.0f, 1.0f, 0.1096412142704f};
    camMat  = new Mat(3, 3, CV_32FC1);
    // now what's the distVec?
    distVec = new Mat(1, 5, CV_32FC1);
    Indexer camIdx  = camMat.createIndexer();
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        camIdx.putDouble(new long[]{row, col}, cameraMatrixData[row * 3 + col]);
      }
    }

    Indexer distVecIdx = distVec.createIndexer();

    for (int i = 0; i < 5; i++) {
      distVecIdx.putDouble(new long[]{0,i}, distMatrixData[i]);
    }

    
  }

  @Override
  public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {
    // TODO: implement this.  perhaps reference: https://github.com/opencv/opencv/blob/master/samples/cpp/calibration.cpp
    Mat matIn = new Mat(image);
    Mat matOut = new Mat();
    undistort(matIn, matOut, camMat, distVec);
    // show(matOut, "output");
    // mat to image now!
    IplImage unDistImage = converterToIpl.convertToIplImage(converterToIpl.convert(matOut));
    return unDistImage;
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub

  }

  public void show(final Mat imageMat, final String title) {
    IplImage image = converterToIpl.convertToIplImage(converterToIpl.convert(imageMat));
    final IplImage image1 = cvCreateImage(cvGetSize(image), IPL_DEPTH_8U, image.nChannels());
    cvCopy(image, image1);
    CanvasFrame canvas = new CanvasFrame(title, 1);
    canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    canvas.showImage(converterToIpl.convert(image1));
  }


}

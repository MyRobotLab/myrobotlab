package org.myrobotlab.opencv;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.javacpp.indexer.Indexer;
import static org.bytedeco.opencv.global.opencv_calib3d.undistort;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.WindowConstants;

import static org.bytedeco.opencv.global.opencv_core.CV_32FC1;
import static org.bytedeco.opencv.global.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.opencv.global.opencv_core.cvCopy;
import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;

public class OpenCVFilterUndistort extends OpenCVFilter {

  /**
   * an undistortion filter that can add/remove a fisheye distortion to the
   * image. Based on blog post here :
   * https://github.com/opencv/opencv/blob/master/samples/cpp/calibration.cpp
   * and CV docs here:
   * https://docs.opencv.org/3.4.1/db/d58/group__calib3d__fisheye.html#ga167df4b00a6fd55287ba829fbf9913b9
   * 
   * The following code and coeffiecnets are taken directly from this stack
   * overflow post.
   * https://stackoverflow.com/questions/40545992/opencv-how-to-provide-matrix-for-undistort-if-i-know-lens-correction-factor
   * 
   * This one is the bomb!
   * https://medium.com/@kennethjiang/calibrate-fisheye-lens-using-opencv-333b05afa0b0
   * 
   * 
   * This filter does not implement the calibration logic.. it expects the
   * calibration matrix is already known and its hard coded! oops.. TODO: make
   * it configurable.
   */
  private static final long serialVersionUID = 1L;

  private Mat camMat = new Mat(3, 3, CV_32FC1);
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

    // Ok this is what i got from the python calibrate.py script
    // DIM=(640, 480)
    // K=np.array([[234.58254183078603, 0.0, 320.85112106077526], [0.0,
    // 234.4039435813954, 266.33088304372257], [0.0, 0.0, 1.0]])
    // D=np.array([[-0.04837465365620614], [0.0026450354779336965],
    // [-0.00479882203273576], [0.0011053843631062754]])

    // This is a 640x480 matrix.
    double[] cameraMatrixData = new double[] { 234.58254183078603, 0.0, 320.85112106077526, 0.0, 234.4039435813954, 266.33088304372257, 0.0, 0.0, 1.0 };

    // TODO: no idea what good values are here!!!
    // double cameraFocal = 1.4656877976320607e+03;
    // double cameraCX = 640.0/2;
    // double cameraCY = 480.0/2;
    // // double cameraCX = 1920.0/2;
    // // double cameraCY = 1080.0/2;
    // double[] cameraMatrixData = new double[]{
    // cameraFocal, 0.0 , cameraCX,
    // 0.0 , cameraFocal, cameraCY,
    // 0.0 , 0.0 , 1.0
    // };

    double[] distMatrixData = new double[] { -0.04837465365620614, 0.0026450354779336965, -0.00479882203273576, 0.0011053843631062754 };
    // double[] distMatrixData = new double[]{-0.4016824381742f,
    // 0.04368842493074f, 0.0f, 0.0f, 0.1096412142704f};
    // double[] distMatrixData = new double[]{-5f, 0.04368842493074f, 0.0f,
    // 0.0f, 0.1096412142704f};
    // double[] distMatrixData = new double[]{-0.4016824381742f,
    // 0.04368842493074f, 1.0f, 1.0f, 0.1096412142704f};
    camMat = new Mat(3, 3, CV_32FC1);
    // now what's the distVec?
    distVec = new Mat(1, 4, CV_32FC1);
    Indexer camIdx = camMat.createIndexer();
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        camIdx.putDouble(new long[] { row, col }, cameraMatrixData[row * 3 + col]);
      }
    }

    Indexer distVecIdx = distVec.createIndexer();

    for (int i = 0; i < 4; i++) {
      distVecIdx.putDouble(new long[] { 0, i }, distMatrixData[i]);
    }

  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    // TODO: implement this. perhaps reference:
    // https://github.com/opencv/opencv/blob/master/samples/cpp/calibration.cpp
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

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}

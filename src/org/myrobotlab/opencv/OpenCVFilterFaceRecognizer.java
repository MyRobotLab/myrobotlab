package org.myrobotlab.opencv;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import static org.bytedeco.javacpp.opencv_face.createFisherFaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.createEigenFaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.createLBPHFaceRecognizer;
import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_core.cvLoad;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RGB2GRAY;


import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.IntBuffer;
/**
 * This is the OpenCV Face Recognition. It must be trained with a
 * set of images and their labels.  These images should be of people
 * faces and their names are the labels.
 * 
 * It computes the "distance" from the reference new image to existing
 * images that it's been trained on and provides a prediction of what label 
 * applies
 * 
 * Based on: https://github.com/bytedeco/javacv/blob/master/samples/OpenCVFaceRecognizer.java
 * 
 * @author kwatters
 *
 */
public class OpenCVFilterFaceRecognizer extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	private FaceRecognizer faceRecognizer;
	private boolean trained = false;

	public OpenCVFilterFaceRecognizer() {
		super();
	}

	public OpenCVFilterFaceRecognizer(String name) {
		super(name);
	}

	public OpenCVFilterFaceRecognizer(String filterName, String sourceKey) {
		super(filterName, sourceKey);
	}


	// TODO: create some sort of a life cycle for this.
	public boolean train(String trainingDir) {

		File root = new File(trainingDir);

		if (!root.isDirectory()) {
			log.error("Training data directory not found {}", root.getAbsolutePath());
			return false;
		}

		FilenameFilter imgFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				name = name.toLowerCase();
				return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
			}
		};

		File[] imageFiles = root.listFiles(imgFilter);

		MatVector images = new MatVector(imageFiles.length);

		Mat labels = new Mat(imageFiles.length, 1, CV_32SC1);
		IntBuffer labelsBuf = labels.getIntBuffer();

		int counter = 0;

		for (File image : imageFiles) {
			Mat img = imread(image.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);

			int label = Integer.parseInt(image.getName().split("\\-")[0]);

			images.put(counter, img);

			labelsBuf.put(counter, label);

			counter++;
		}

		// TODO: expose the other types of recognizers ?
		faceRecognizer = createFisherFaceRecognizer();
		//faceRecognizer = createEigenFaceRecognizer();
		// faceRecognizer = createLBPHFaceRecognizer()

		faceRecognizer.train(images, labels);

		trained = true;
		return true;
	}

	@Override
	public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {
		if (image == null) {
			log.error("image is null");
		}

		if (!trained) {
			log.error("Face Recognizer filter not trained.");
			return image;
		}

		// this matric needs to be the same resolution and color depth as the training set.
		IplImage imageBW = IplImage.create(image.width(), image.height(),8,1);
		cvCvtColor(image, imageBW, CV_BGR2GRAY);
		
		
		// TODO: this seems super wonky!  isn't there an easy way to go from IplImage to opencv Mat?
		OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
		OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();
		Frame frame = converterToMat.convert(imageBW);
		Mat mat = converterToIpl.convertToMat(frame);
		
		// This line causes the JVM to seg fault on me : EXCEPTION_ACCESS_VIOLATION
		int predictedLabel = faceRecognizer.predict(mat);
		System.out.println("Predicted label: " + predictedLabel);
		// TODO: add a label of some text to the image for
		// the label that was detected.. 
		return imageBW;
	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub

	}

}

package org.myrobotlab.opencv;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_face.FaceRecognizer;

import static org.bytedeco.javacpp.opencv_face.createFisherFaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.createEigenFaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.createLBPHFaceRecognizer;
import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
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
	
	// TODO: create some sort of a life cycle for this.
	public void train(String trainingDir) {
		
        File root = new File(trainingDir);
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
        // FaceRecognizer faceRecognizer = createEigenFaceRecognizer();
        // FaceRecognizer faceRecognizer = createLBPHFaceRecognizer()

        faceRecognizer.train(images, labels);
        
        
	}
	
	@Override
	public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {
		if (image == null) {
			log.error("image is null");
		}


        Mat mat = new Mat(image);
        int predictedLabel = faceRecognizer.predict(mat);
        System.out.println("Predicted label: " + predictedLabel);
		
		// TODO: add a label of some text to the image for
        // the label that was detected.. 
		return image;
	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub

	}

}

package org.myrobotlab.opencv;

import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import static org.bytedeco.javacpp.opencv_face.createFisherFaceRecognizer;
import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawRect;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
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

	private int modelSizeX = 256;
	private int modelSizeY = 256;
	//private OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
	
	//private CvMemStorage storage = null;
	
	private CascadeClassifier faceCascade;
	private CascadeClassifier eyeCascade;
	//private CascadeClassifier noseCascade;
	private CascadeClassifier mouthCascade;
	
	public OpenCVFilterFaceRecognizer() {
		super();
		initHaarCas();
	}

	public OpenCVFilterFaceRecognizer(String name) {
		super(name);
		initHaarCas();
	}

	public OpenCVFilterFaceRecognizer(String filterName, String sourceKey) {
		super(filterName, sourceKey);
		initHaarCas();
	}


	public void initHaarCas() {
		String cascadeDir = "haarcascades";
		//haarcascade = new CvHaarClassifierCascade(cvLoad(String.format("%s/%s", cascadeDir, "haarcascade_frontalface_default.xml")));
		//eyehaarcascade = new CvHaarClassifierCascade(cvLoad(String.format("%s/%s", cascadeDir, "haarcascade_eye.xml")));;
		//nosehaarcascade = new CvHaarClassifierCascade(cvLoad(String.format("%s/%s", cascadeDir, "haarcascade_nose.xml")));;
		//mouthhaarcascade = new CvHaarClassifierCascade(cvLoad(String.format("%s/%s", cascadeDir, "haarcascade_mouth.xml")));;
		// Now we've got all our classifiers i guess?
		
//		if (haarcascade == null) {
//			log.error("Face classifier was null");
//		}
//
//		if (eyehaarcascade == null) {
//			log.error("Eye classifier was null");
//		}
//
//		if (nosehaarcascade == null) {
//			log.error("Nose classifier was null");
//		}
//
//		if (mouthhaarcascade == null) {
//			log.error("Mouth classifier was null");
//		}
		faceCascade = new CascadeClassifier(cascadeDir+"/haarcascade_frontalface_default.xml");
		eyeCascade = new CascadeClassifier(cascadeDir+"/haarcascade_eye.xml");
		// noseCascade = new CascadeClassifier(cascadeDir+"/haarcascade_nose.xml");
		mouthCascade = new CascadeClassifier(cascadeDir+"/haarcascade_mouth.xml");

	}
	
	// TODO: create some sort of a life cycle for this.
	public boolean train(String trainingDir) {
		
		// File filterfile = new File("src/resources/filter.png");
		// Face filter used to mask edges of face pictures
		// Mat facefilter = imread(filterfile.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);
		
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
			// make sure all our test images are resized 
			Mat resized = resizeImage(img);
			// so, now our input for the training set is always 256x256 image.
			// we should probably run face detect and center this resized image.. 
			// no idea what sort of data it's going to be pumping out..
			images.put(counter, resized);
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

	private Mat resizeImage(Mat img) {
		Mat resizedMat = new Mat();
		// IplImage resizedImage = IplImage.create(modelSizeX, modelSizeY, img.depth(), img.channels());
		Size sz = new Size(modelSizeX,modelSizeY);
		resize(img, resizedMat, sz);
		return resizedMat;
	}

	public RectVector detectEyes(Mat mat) {
		RectVector vec = new RectVector();
		eyeCascade.detectMultiScale(mat,vec);
		return vec;
	}
	
	public RectVector detectMouths(Mat mat) {
		RectVector vec = new RectVector();
		mouthCascade.detectMultiScale(mat,vec);
		return vec;
	}
	
	public RectVector detectFaces(Mat mat) {
		RectVector vec = new RectVector();
		// TODO: see about better tuning and passing these parameters in.
		// RectVector faces = faceCascade.detectMultiScale(gray,scaleFactor=1.1,minNeighbors=5,minSize=(50, 50),flags=cv2.cv.CV_HAAR_SCALE_IMAGE)
		faceCascade.detectMultiScale(mat,vec);
		return vec;
	}
	
	public void drawRect(IplImage image, Rect rect, CvScalar color) {
		cvDrawRect(image, cvPoint(rect.x(), rect.y()), cvPoint(rect.x()+rect.width(), rect.y()+rect.height()), color, 1, 1, 0);		
	}
	
	@Override
	public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {
		
		// we want to detect eyes 
		// detect nose ?
		// detect mouth

		// Convert to gray scale Mat object.  this looks so lame.. gotta be better way 
		IplImage imageBW = IplImage.create(image.width(), image.height(),8,1);
		cvCvtColor(image, imageBW, CV_BGR2GRAY);
		// TODO: this seems super wonky!  isn't there an easy way to go from IplImage to opencv Mat?
		OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
		OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();
		Frame frame = converterToMat.convert(imageBW);
		Mat mat = converterToIpl.convertToMat(frame);
		
		// detect the eyes
		RectVector eyes = detectEyes(mat);
		if (eyes.size() == 2) {
			// we have 2 eyes yay!!!
			log.info("We have 2 eyes!");
			Rect leftEye = eyes.get(0);
			Rect rightEye = eyes.get(1);
			// cv2.rectangle(color_image,(x+ex,y+ey),(x+ex+ew,y+ey+eh),(0,255,0),1)
			// leftEye.x()+leftEye.w
			// we have eyes!  draw them
			drawRect(image, leftEye, CvScalar.BLUE);
			drawRect(image, rightEye, CvScalar.RED);
			//cvDrawRect(image, cvPoint(leftEye.x(), leftEye.y()), cvPoint(leftEye.x()+leftEye.width(), leftEye.y()+leftEye.height()), CvScalar.RED, 1, 1, 0);
			//cvDrawRect(image, cvPoint(rightEye.x(), rightEye.y()), cvPoint(rightEye.x()+rightEye.width(), rightEye.y()+rightEye.height()), CvScalar.BLUE, 1, 1, 0);
		}
		
		// ok.. 
		// detect the mouth
//		RectVector mouths = detectMouths(mat);
//		if (mouths.size() == 1) {
//			log.info("We have 1 mouth!");
//			drawRect(image, mouths.get(0), CvScalar.GREEN);
//		}
		
		// TODO: detect the faces
		// RectVector faces = faceCascade.detectMultiScale(gray,scaleFactor=1.1,minNeighbors=5,minSize=(50, 50),flags=cv2.cv.CV_HAAR_SCALE_IMAGE)
		RectVector faces = detectFaces(mat);
		
		for (int i = 0 ; i < faces.size(); i++) {
			// iterate faces detected.
			Rect face = faces.get(i);
			drawRect(image, face, CvScalar.MAGENTA);
		}

		if (!trained) {
			log.error("Face Recognizer filter not trained.");
			return image;
		}

		// This line causes the JVM to seg fault on me : EXCEPTION_ACCESS_VIOLATION
		// resize the mat to fit the model resolution.
		// here we want to crop the detected face .. 
		// affine correct it based on eyes and mouth
		// and resize it for the predictor.
		mat = resizeImage(mat);
		
		int predictedLabel = faceRecognizer.predict(mat);
		System.out.println("Predicted label: " + predictedLabel);
		// TODO: add a label of some text to the image for
		// the label that was detected..
		
		return image;
		// return converterToIpl.convertToIplImage(converterToIpl.convert(mat));
	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub

	}

	public int getModelSizeX() {
		return modelSizeX;
	}

	public void setModelSizeX(int modelSizeX) {
		this.modelSizeX = modelSizeX;
	}

	public int getModelSizeY() {
		return modelSizeY;
	}

	public void setModelSizeY(int modelSizeY) {
		this.modelSizeY = modelSizeY;
	}

}

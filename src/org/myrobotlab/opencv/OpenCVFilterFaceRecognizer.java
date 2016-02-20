package org.myrobotlab.opencv;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core.CvMat;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Point2f;
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
import static org.bytedeco.javacpp.opencv_imgproc.cvWarpAffine;
import static org.bytedeco.javacpp.opencv_imgproc.getAffineTransform;
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

		faceCascade = new CascadeClassifier(cascadeDir+"/haarcascade_frontalface_default.xml");
		eyeCascade = new CascadeClassifier(cascadeDir+"/haarcascade_eye.xml");
		// noseCascade = new CascadeClassifier(cascadeDir+"/haarcascade_nose.xml");
		// TODO: see if we can find the other classifier.. the mcs mouth doesn't see
		// to work very well.. at least on my face.  i have no mouth!
		//		mouthCascade = new CascadeClassifier(cascadeDir+"/haarcascade_mouth.xml");
		mouthCascade = new CascadeClassifier(cascadeDir+"/haarcascade_mcs_mouth.xml");

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

		// first lets pick up on the face. we'll asume the eyes and mouth are inside.
		RectVector faces = detectFaces(mat);
		for (int i = 0 ; i < faces.size(); i++) {
			// iterate faces detected.
			Rect face = faces.get(i);
			drawRect(image, face, CvScalar.MAGENTA);
		}



		// ultimately we want to find the center of the eyes
		// and the mouth so we can rotate and scale the image?
		int centerleftx = -1;
		int centerlefty = -1;
		int centerrightx = -1;
		int centerrighty = -1;
		int centermouthx = -1;
		int centermouthy = -1;


		boolean hasEyes = false;
		boolean hasMouth = false;
		// detect the eyes
		RectVector eyes = detectEyes(mat);
		if (eyes.size() == 2) {
			// log.info("We have 2 eyes!");
			Rect leftEye = eyes.get(0);
			Rect rightEye = eyes.get(1);
			// we have eyes!  draw them
			drawRect(image, leftEye, CvScalar.BLUE);
			drawRect(image, rightEye, CvScalar.RED);
			// here we have 2 eyes
			hasEyes = true;	

			// 		    centerleftx = detected_eyes[0][0]+detected_eyes[0][2]/2  # left side plus half width
			//            centerlefty = detected_eyes[0][1]+detected_eyes[0][3]/2  # top plus half height
			//			centerrightx = detected_eyes[1][0]+detected_eyes[1][2]/2  # left side plus half width
			//			centerrighty = detected_eyes[1][1]+detected_eyes[1][3]/2  # top plus half height



			// left side plus 1/2 width  and 1/2 height
			centerleftx = eyes.get(0).x() + eyes.get(0).width()/2;
			centerlefty = eyes.get(0).y() + eyes.get(0).height()/2;
			// right side
			centerrightx = eyes.get(1).x() + eyes.get(1).width()/2;
			centerrighty = eyes.get(1).y() + eyes.get(1).height()/2;

		}



		// ok.. 
		// detect the mouth
		RectVector mouths = detectMouths(mat);
		// TODO: get a better mouth detector!
		if (mouths.size() >= 1) {

			// log.info("We have at "+mouths.size()+ " mouths!");
			for ( int i = 0 ; i < mouths.size(); i++) {
				if (isInside(faces, mouths.get(i))) {
					if (eyes.size() == 2) {
						if (!isInside(eyes, mouths.get(i))) {
							drawRect(image, mouths.get(i), CvScalar.GREEN);
							hasMouth = true;
						}
					}
				} else { 
					// the mouth is outside of the detected face!
					// log.info("What is this mouth?!?!");
				}
			}
			centermouthx = mouths.get(0).x() + mouths.get(0).width()/2;
			centermouthy = mouths.get(0).y() + mouths.get(0).height()/2;						
		}



		// TODO: detect the faces
		// RectVector faces = faceCascade.detectMultiScale(gray,scaleFactor=1.1,minNeighbors=5,minSize=(50, 50),flags=cv2.cv.CV_HAAR_SCALE_IMAGE)

		// ok. if we have a mouth and we have eyes..
		// it's probably a face.. rotate scale .. mojo
		//int [][] ipts1 = new int[][]{[[centerleftx,centerlefty],[centerrightx,centerrighty],[centermouthx,centermouthy]]};
		int [][] ipts1 = new int[3][2];
		//{[[centerleftx,centerlefty],[centerrightx,centerrighty],[centermouthx,centermouthy]]};
		ipts1[0][0] = centerleftx;
		ipts1[0][1] = centerlefty;
		ipts1[1][0] = centerrightx;
		ipts1[1][1] = centerrighty;
		ipts1[2][0] = centermouthx;
		ipts1[2][1] = centermouthy;

		//		Point2f pts1 = new Point2f(ipts1);
		//		Point2f pts2 = new Point2f();
		//		getAffineTransform(pts1, pts2);
		//		float centerleftx 
		//		Point2f pts1 = np.float32([[centerleftx,centerlefty],[centerrightx,centerrighty],[centermouthx,centermouthy]]);
		//		Point2f pts2 = np.float32([[cols*.3,rows*.45],[cols*.7,rows*.45],[cols*.50,rows*.85]]);
		//		Mat affXform = getAffineTransform(pts1, pts2);
		// 		ok.. maybe we can do the affine warp here
		//		CvMat cvAffXform = new CvMat(affXform);
		//		cvWarpAffine(image, image, cvAffXform);

		if (hasEyes && hasMouth) {
			// WOOHOO! we have eyes and mouth!
			log.info("We found a face!!!");
		}

		if (!trained) {
			// log.error("Face Recognizer filter not trained.");
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

	private boolean isInside(RectVector rects, Rect test) {
		for (int i = 0; i < rects.size(); i++) {
			Rect r = rects.get(i);
			int rx = r.x();
			int rxm = r.x()+r.width();

			int ry = r.y();
			int rym = r.y() + r.height();

			// we want to check that the test rect is inside of the r
			if (rx < test.x() && rxm > (test.x() + test.width())) {
				// the xaxis look good.
				// how about the y axis?
				if (ry < test.y() && rym > (test.y() + test.height())) {
					return true;				
				}

			}
		}
		return false;
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

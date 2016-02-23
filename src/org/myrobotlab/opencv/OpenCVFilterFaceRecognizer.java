package org.myrobotlab.opencv;

import org.apache.commons.lang3.StringUtils;
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
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawRect;
import static org.bytedeco.javacpp.opencv_imgproc.getAffineTransform;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.warpAffine;
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
 * @author scruffy-bob
 *
 */
public class OpenCVFilterFaceRecognizer extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	// training mode stuff
	public Mode mode = Mode.RECOGNIZE;
	// when in training mode, this is the name to associate with the face.
	public String trainName = null;
	
	private FaceRecognizer faceRecognizer;
	private boolean trained = false;
	// the directory to store the training images.
	private String trainingDir = "c:/training";

	private int modelSizeX = 256;
	private int modelSizeY = 256;
	private CascadeClassifier faceCascade;
	private CascadeClassifier eyeCascade;
	private CascadeClassifier mouthCascade;
	
	
	private OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
	private OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();

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

	public enum Mode {
		TRAIN,
		RECOGNIZE
	}
	
	public void initHaarCas() {
		String cascadeDir = "haarcascades";
		//nosehaarcascade = new CvHaarClassifierCascade(cvLoad(String.format("%s/%s", cascadeDir, "haarcascade_nose.xml")));;
		// Now we've got all our classifiers i guess?
		faceCascade = new CascadeClassifier(cascadeDir+"/haarcascade_frontalface_default.xml");
		eyeCascade = new CascadeClassifier(cascadeDir+"/haarcascade_eye.xml");
		mouthCascade = new CascadeClassifier(cascadeDir+"/haarcascade_mcs_mouth.xml");
		// noseCascade = new CascadeClassifier(cascadeDir+"/haarcascade_nose.xml");
		// TODO: see if we can find the other classifier.. the mcs mouth doesn't see
		// to work very well.. at least on my face.  i have no mouth!
		// mouthCascade = new CascadeClassifier(cascadeDir+"/haarcascade_mouth.xml");
	}

	// TODO: create some sort of a life cycle for this.
	public boolean train() {
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
		if (imageFiles.length < 1) {
			log.info("No images found for training.");
			return false;
		}
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

		//log.info("skipping training for now.");
		faceRecognizer.train(images, labels);
		//trained = true;
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
		
		// Ok. need some major refactoring here. 
		
		
		// Convert to a grayscale image.  
		//(TODO: maybe convert to a Mat first, then cut color? not sure what's faster 
		IplImage imageBW = IplImage.create(image.width(), image.height(),8,1);
		cvCvtColor(image, imageBW, CV_BGR2GRAY);
		// TODO: this seems super wonky!  isn't there an easy way to go from IplImage to opencv Mat?
		Frame frame = converterToMat.convert(imageBW);
		int cols = frame.imageWidth;
		int rows = frame.imageHeight;
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
			// left side plus 1/2 width  and 1/2 height
			centerleftx = eyes.get(0).x() + eyes.get(0).width()/2;
			centerlefty = eyes.get(0).y() + eyes.get(0).height()/2;
			// right side
			centerrightx = eyes.get(1).x() + eyes.get(1).width()/2;
			centerrighty = eyes.get(1).y() + eyes.get(1).height()/2;
		}

		// detect the mouth(s)
		RectVector mouths = detectMouths(mat);
		// TODO: get a better mouth detector!
		
		// only the mouth that is in the face and not overlapping with the eyes.
		int mouthIndex = 0;
		if (mouths.size() >= 1) {
			// log.info("We have at "+mouths.size()+ " mouths!");
			for ( int i = 0 ; i < mouths.size(); i++) {

				// mouth must be inside the face and not overlap with the eyes
				if (isInside(faces, mouths.get(i))) {
					
					
					if (eyes.size() == 2) {
						if (!rectOverlap(eyes.get(0), mouths.get(i)) && !rectOverlap(eyes.get(1), mouths.get(i))) {
							drawRect(image, mouths.get(i), CvScalar.GREEN);
							hasMouth = true;
							mouthIndex = i;
						}
					}
				} else {
					// the mouth is outside of the detected face!
					// log.info("What is this mouth?!?!");
				}
			}
			centermouthx = mouths.get(mouthIndex).x() + mouths.get(mouthIndex).width()/2;
			centermouthy = mouths.get(mouthIndex).y() + mouths.get(mouthIndex).height()/2;						
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

		
		Point2f srcTri = new Point2f(3);
		Point2f dstTri = new Point2f(3);

		// ...

		srcTri.position(0).x((float)centerleftx).y((float)centerlefty);
		srcTri.position(1).x((float)centerrightx).y((float)centerrighty);
		srcTri.position(2).x((float)centermouthx).y((float)centermouthy);
		// and do the same for dstTri
		// pts2 = np.float32([[cols*.3,rows*.45],[cols*.7,rows*.45],[cols*.50,rows*.85]])
		dstTri.position(0).x((float)(cols*.3)).y((float)(rows*.45));
		dstTri.position(1).x((float)(cols*.7)).y((float)(rows*.45));
		dstTri.position(2).x((float)(cols*.5)).y((float)(rows*.85));
		
		
		Mat warpMat = getAffineTransform( srcTri.position(0), dstTri.position(0) );
		
		// TODO: make sure these are closed properly?
		try {
			srcTri.close();
			dstTri.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		if (hasEyes && hasMouth) {
			// WOOHOO! we have eyes and mouth!
			log.info("We found a face!!!");
			// rotate and and center the image.
			// TODO: this seems no worky?
			warpAffine(mat, mat, warpMat, new Size(cols,rows));
			
			// OK.. if we're in training mode, we should save the image off.
			if (OpenCVFilterFaceRecognizer.Mode.TRAIN.equals(mode)) {
				// we're in training mode.. so we should save the image
				// TODO: save image with the train name
				if (!StringUtils.isEmpty(trainName)) {
					// ok we know the name for this face. 
					// we have the trimmed down image representing the
					// face. save it off.
					long i = System.currentTimeMillis();
					// TODO: encode a proper/better filename.
					
					String filename = trainingDir + "/" + trainName.hashCode() + "-" + trainName + i + ".png";
					// TODO: what format is this?!!
					imwrite(filename, mat);
				} else {
					log.warn("In Training mode, but the trainName isn't set!");
				}
			}
		}

		
		
		if (!trained) {
			// log.error("Face Recognizer filter not trained.");
			// return converterToIpl.convertToIplImage(converterToIpl.convert(mat));
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

		// return image;
		return converterToIpl.convertToIplImage(converterToIpl.convert(mat));
	}

	private boolean isInside(RectVector rects, Rect test) {
		for (int i = 0; i < rects.size(); i++) {
			boolean res = isInside(rects.get(i), test);
			if (res) {
				return true;
			}
		}
		return false;
	}

	private boolean isInside(Rect r1, Rect r2) {
		// if r2 is inside of r1 return true
		int x1 = r1.x();
		int y1 = r1.y();
		int x2 = x1 + r1.width();
		int y2 = y1 + r1.height();
		
		int x3 = r2.x();
		int y3 = r2.y();
		int x4 = r2.x() + r2.width();
		int y4 = r2.y() + r2.height();
		
		
		// if r2 xmin/xmax is within r1s
		if (x1 < x3 && x2 > x4) {
			if (y1 < y3 && y2 > y4) {
				return true;
			}
		}
		return false;
	}
	
	private boolean rectOverlap(Rect r, Rect test) {
		
		return (((r.x() >= test.x()) && (r.x() < (test.x() + test.width()))) || 
				((test.x() >= r.x()) && (test.x() < (r.x() + r.width())))) &&
				(((r.y() >= test.y()) && (r.y() < (test.y() + test.height()))) || 
						  ((test.y() >= r.y()) && (test.y() < (r.y() + r.height()))));
		
		//		int rx = r.x();
		//		int rxm = r.x()+r.width();
		//		int ry = r.y();
		//		int rym = r.y() + r.height();
		//		// we want to check that the test rect is inside of the r
		//		if (rx < test.x() && rxm > (test.x() + test.width())) {
		//			// the xaxis look good.
		//			// how about the y axis?
		//			if (ry < test.y() && rym > (test.y() + test.height())) {
		//				return true;				
		//			}
		//		}
		//		return false;
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
	
	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public String getTrainName() {
		return trainName;
	}

	public void setTrainName(String trainName) {
		this.trainName = trainName;
	}

	public String getTrainingDir() {
		return trainingDir;
	}

	public void setTrainingDir(String trainingDir) {
		this.trainingDir = trainingDir;
	}



}

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
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import static org.bytedeco.javacpp.opencv_face.createFisherFaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.createEigenFaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.createLBPHFaceRecognizer;
import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawRect;
import static org.bytedeco.javacpp.opencv_imgproc.cvFont;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;
import static org.bytedeco.javacpp.opencv_imgproc.getAffineTransform;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.warpAffine;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import javax.swing.WindowConstants;


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
	public RecognizerType recognizerType = RecognizerType.FISHER;
	// when in training mode, this is the name to associate with the face.
	public String trainName = null;
	private FaceRecognizer faceRecognizer;
	private boolean trained = false;
	// the directory to store the training images.
	private String trainingDir = "training";
	private int modelSizeX = 256;
	private int modelSizeY = 256;
	
	// 
	// We read in the face filter when training the first time, and use it for all subsequent
	// training and for masking images prior to comparison.
	//
	private Mat facemask = null;
	
	private String cascadeDir = "haarcascades";
	private CascadeClassifier faceCascade;
	private CascadeClassifier eyeCascade;
	private CascadeClassifier mouthCascade;
	// TODO: why the heck do we need to convert back and forth, and is this effecient?!?!
	private OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
	private OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();
	
	private HashMap<Integer, String> idToLabelMap = new HashMap<Integer,String>();
	
	private CvFont font = cvFont(CV_FONT_HERSHEY_PLAIN);

	private boolean debug = false;
	// KW: I made up this word, but I think it's fitting.
	private boolean dePicaso = true;
	
	private boolean doAffine = true;
	
	// some padding around the detected face
	private int borderSize = 25;
	
	
	private String lastRecognizedName = null;
	
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

	public enum RecognizerType {
		FISHER,
		EIGEN,
		LBPH		
	}
	
	public void initHaarCas() {
		faceCascade = new CascadeClassifier(cascadeDir+"/haarcascade_frontalface_default.xml");
		eyeCascade = new CascadeClassifier(cascadeDir+"/haarcascade_eye.xml");
		// TODO: find a better mouth classifier! this one kinda sucks.
		mouthCascade = new CascadeClassifier(cascadeDir+"/haarcascade_mcs_mouth.xml");
		// mouthCascade = new CascadeClassifier(cascadeDir+"/haarcascade_mouth.xml");
		// noseCascade = new CascadeClassifier(cascadeDir+"/haarcascade_nose.xml");
	}

	/**
	 * This method will load all of the image files in a directory.  The filename will be parsed 
	 * for the label to apply to the image.  At least 2 different labels must exist in the training
	 * set.  
	 * 
	 * @return
	 */
	public boolean train() {
		//
		// The first time we train, find the image mask, if present, scale it to the current image size, 
		// and save it for later.
		//
		if (facemask == null) {
			File filterfile = new File("src/resource/facerec/Filter.png");
			//
			// Face mask used to mask edges of face pictures to eliminate noise around the edges
			//
			if (!filterfile.exists()) {
				log.warn("No image filter file found.  {}", filterfile.getAbsolutePath());
			} else {
				// Read the filter and rescale it to the current image size
				Mat incomingfacemask = imread(filterfile.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);
				facemask = resizeImage(incomingfacemask);
				if (debug) {
					show(facemask, "Face Mask");
				}
			}
		}
		
		File root = new File(trainingDir);
		if (root.isFile()) {
			log.warn("Training directory was a file, not a directory.  {}", root.getAbsolutePath());
			return false;
		}
		if (!root.exists()) {
			log.info("Creating new training directory {}", root.getAbsolutePath());
			root.mkdirs();
		}
		log.info("Using {} for training data." , root.getAbsolutePath());
		File[] imageFiles = listImageFiles(root);
		if (imageFiles.length < 1) {
			log.info("No images found for training.");
			return false;
		}
		// Storage for the files that we load.
		MatVector images = new MatVector(imageFiles.length);
		// storage for the labels for the images
		Mat labels = new Mat(imageFiles.length, 1, CV_32SC1);
		IntBuffer labelsBuf = labels.getIntBuffer();
		int counter = 0;
		for (File image : imageFiles) {
			// load the image
			Mat img = imread(image.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);
			// Parse the filename label-foo.jpg  everything up to the first - is the label.
			String personName = image.getName().split("\\-")[0];
			// TODO: we need an integer to represent this string .. for now we're using a hashcode here.  
			// this can definitely have a collision!
			// we really need a better metadata store for these images.
			int label = personName.hashCode();
			// make sure all our test images are resized 
			Mat resized = resizeImage(img);
			
			//
			// Mask out unwanted parts of the training image by applying the resized mask
			//
			if (facemask != null) {
				Mat maskedface = facemask.clone();
				resized.copyTo(maskedface,facemask);
				resized = maskedface;	
			}
			
			// so, now our input for the training set is always 256x256 image.
			// we should probably run face detect and center this resized image, so we can see
			// if we detect a full face in the image or not..
			// If these images are generated by this filter, they'll already be cropped so it's ok 
			// TODO: add a debug method to show the image
			if (debug) {
				show(resized, personName);
			}
			// TODO: our training images are indexed by integer,
			images.put(counter, resized);
			labelsBuf.put(counter, label);
			// keep track of what string the hash code maps to.
			idToLabelMap.put(label, personName);
			counter++;
		}
		// Configure which type of recognizer to use
		if (RecognizerType.FISHER.equals(recognizerType)) {
			faceRecognizer = createFisherFaceRecognizer();
		} else if (RecognizerType.EIGEN.equals(recognizerType)) {
			faceRecognizer = createEigenFaceRecognizer();
		} else {
			faceRecognizer = createLBPHFaceRecognizer();
		}
		// must be at least 2 things to classify, is it A or B ?
		if (idToLabelMap.keySet().size() > 1) {
			faceRecognizer.train(images, labels);
			trained = true;
		} else {
			log.info("No labeled images loaded. training skipped.");
			trained = false;
		}
		return true;
	}

	private File[] listImageFiles(File root) {
		FilenameFilter imgFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				name = name.toLowerCase();
				return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
			}
		};
		File[] imageFiles = root.listFiles(imgFilter);
		return imageFiles;
	}

	
	private Mat resizeImage(Mat img, int width, int height) {
		Mat resizedMat = new Mat();
		// IplImage resizedImage = IplImage.create(modelSizeX, modelSizeY, img.depth(), img.channels());
		Size sz = new Size(width,height);
		resize(img, resizedMat, sz);
		return resizedMat;
	}
	
	private Mat resizeImage(Mat img) {
		return resizeImage(img, modelSizeX, modelSizeY);
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

	
	// helper method to show an image.  (todo; convert it to a Mat )
    public void show(final Mat imageMat, final String title) {
    	IplImage image = converterToIpl.convertToIplImage(converterToIpl.convert(imageMat));
    	final IplImage image1 = cvCreateImage(cvGetSize(image), IPL_DEPTH_8U, image.nChannels());
        cvCopy(image, image1);
        CanvasFrame canvas = new CanvasFrame(title, 1);
        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        canvas.showImage(converter.convert(image1));
    }
	
	@Override
	public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {
		// convert to grayscale
		Frame grayFrame = makeGrayScale(image);
		// TODO: this seems super wonky!  isn't there an easy way to go from IplImage to opencv Mat?
		int cols = grayFrame.imageWidth;
		int rows = grayFrame.imageHeight;
		// convert to a Mat
		Mat bwImgMat = converterToIpl.convertToMat(grayFrame);
		
		// 
		// Image detection is done on the grayscale image, so we can modify the original frame once
		// we make a grayscale copy.
		//
		if (Mode.TRAIN.equals(mode)) {
			String status = "Training Mode: " + trainName;
			cvPutText(image, status, cvPoint(20,40), font, CvScalar.GREEN);
		} else if (Mode.RECOGNIZE.equals(mode)) {
			String status = "Recognize Mode:" + lastRecognizedName;
			cvPutText(image, status, cvPoint(20,40), font, CvScalar.YELLOW);
		}
		
		// Find a bunch of faces and their features
		ArrayList<DetectedFace> dFaces = extractDetectedFaces(bwImgMat, cols, rows);
		// Ok, for each of these detected faces we should try to classify them.
		for (DetectedFace dF : dFaces) {
			if (dF.isComplete()) {
				// Ok we have a complete face. lets get the affine points.
				// tell the face to move it's eyes and mouth into the right place?!
				if (dePicaso) {
					dF.dePicaso();
				}
				// and array of 3 x,y points.
				// create the triangle from left->right->mouth center
				Point2f srcTri = dF.resolveCenterTriangle();
				Point2f dstTri = new Point2f(3);
				// populate dest triangle.
				dstTri.position(0).x((float)(dF.getFace().width()*.3)).y((float)(dF.getFace().height()*.45));
				dstTri.position(1).x((float)(dF.getFace().width()*.7)).y((float)(dF.getFace().height()*.45));
				dstTri.position(2).x((float)(dF.getFace().width()*.5)).y((float)(dF.getFace().height()*.85));
				// create the affine rotation/scale matrix
				Mat warpMat = getAffineTransform( srcTri.position(0), dstTri.position(0) );
				//Mat dFaceMat = new Mat(bwImgMat, dF.getFace());
				
				Rect borderRect = dF.faceWithBorder(borderSize, cols, rows);
				Mat dFaceMat = new Mat(bwImgMat, borderRect);
				// TODO: transform the original image , then re-crop from that
				// so we don't loose the borders after the rotation
				if (doAffine) {
					warpAffine(dFaceMat, dFaceMat, warpMat, borderRect.size());
				}
				try {
					// TODO: why do i have to close these?!
					srcTri.close();
					dstTri.close();
				} catch (Exception e) {
					log.warn("Error releasing some OpenCV memory, you shouldn't see this: {}", e);
					// should we continue ?!
				}
				// dFaceMat is the cropped and rotated face 
				if (Mode.TRAIN.equals(mode)) {
					// we're in training mode.. so we should save the image
					log.info("Training Mode for {}.", trainName);
					if (!StringUtils.isEmpty(trainName)) {
						// create some sort of a unique value so the file names don't conflict
						// TODO: use something more random like a
						UUID randValue = UUID.randomUUID();
						String filename = trainingDir + "/" + trainName + "-" + randValue + ".png";
						// TODO: I think this is a png file ? not sure.
						imwrite(filename, dFaceMat);
						cvPutText(image, "Snapshot Saved: " + trainName , cvPoint(20,60), font, CvScalar.CYAN);
					}
				} else if (Mode.RECOGNIZE.equals(mode)) {
					// You bettah recognize!
					if (!trained) {
						// we are a young grasshopper.
						log.info("Classifier not trained yet.");
						return image;
					} else {
						// Resize the face to pass it to the predicter
						Mat dFaceMatSized = resizeImage(dFaceMat);
						Mat copytoMat = dFaceMatSized.clone();
						
						// If we're applying a mask, do it before the prediction
						if (facemask != null) {
							Mat maskedface = facemask.clone();
							dFaceMatSized.copyTo(maskedface,facemask);
							dFaceMatSized = maskedface;	
							if (debug) {
								show(dFaceMatSized, "Masked Face");
							}
						}
						
						int predictedLabel = faceRecognizer.predict(dFaceMatSized);
						String name = Integer.toString(predictedLabel);
						if (idToLabelMap.containsKey(predictedLabel)) {
							name = idToLabelMap.get(predictedLabel);
						} else {
							// you shouldn't ever see this.
							log.warn("Unknown predicted label returned! {}", predictedLabel);
						}
						log.info("Recognized a Face {} - {}", predictedLabel, name);
						cvPutText(image, "Recognized:"+name, dF.resolveGlobalLowerLeftCorner(), font, CvScalar.CYAN);
						lastRecognizedName = name;
					}
				} 
			}
			// highlight each of the faces we find.
			drawFaceRects(image, dF);
		}
		//pass through/return the original image marked up.
		return image;
	}

	private Frame makeGrayScale(IplImage image) {
		IplImage imageBW = IplImage.create(image.width(), image.height(),8,1);
		cvCvtColor(image, imageBW, CV_BGR2GRAY);
		return converterToMat.convert(imageBW);
	}

	private ArrayList<DetectedFace> extractDetectedFaces(Mat bwImgMat, int width , int height) {
		ArrayList<DetectedFace> dFaces = new ArrayList<DetectedFace>();
		// first lets pick up on the face. we'll asume the eyes and mouth are inside.
		RectVector faces = detectFaces(bwImgMat);
		// TODO: take only non overlapping faces.
		// Ok, we have a face, so... we should try to find the eyes and mouths.
		// Now that we've got eyes and mouths.. lets see if they
		// line up on the faces..
		for (int i = 0 ; i < faces.size(); i++) {
			DetectedFace dFace = new DetectedFace();
			Rect face = faces.get(i);
			Mat croppedFace = new Mat(bwImgMat, face);
			// debugging only!
			//String filename = trainingDir + "/" + trainName + "-"+System.currentTimeMillis()+ "-" + i + ".png";
			//imwrite(filename, croppedFace);
			RectVector eyes = detectEyes(croppedFace);
			RectVector mouths = detectMouths(croppedFace);
			// the face!
			dFace.setFace(face);
			// ok find the mouth in the face.
			for (int m = 0 ; m < mouths.size(); m++) {
				// log.info("Mouth...");
				Rect mouth = mouths.get(m);
				// TODO: evaluate what's a better match maybe many mouths
				// Ok, now we need to find the eyes in this face
				for (int e = 0 ; e < eyes.size(); e++) {
					// this one is a bit trickier , we need 2 eyes in the face.
					// but i'm not sure which is left or right?!
					Rect eye = eyes.get(e);
					//log.info("Eye...");
					//if (isInside(face, eye)) {
					// TODO: some better way to know which is left & right.
					// for now, just taking the first and second one we find inside the face.
					// this could be backwards?!
					if (dFace.getLeftEye() == null) {
						dFace.setLeftEye(eye);
					} else {
						dFace.setRightEye(eye);
					}
					//}
				}
				// TODO: reverse the isInside method args.  seems backwards currently.
				//if (isInside(face, mouth)) {
					if (!rectOverlap(dFace.getLeftEye(), mouth) && !rectOverlap(dFace.getRightEye(), mouth)) {
						dFace.setMouth(mouth);
					}
				//}
			}
			// add this to a face that we've found.
			dFaces.add(dFace);
			// debugging show(croppedFace, "Face!");
			if (debug) {
				show(croppedFace, "Face!");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return dFaces;
	}

	private void drawFaceRects(IplImage image, DetectedFace dFace) {
		// helper function to draw rectangles around the detected face(s)
		drawRect(image, dFace.getFace(), CvScalar.MAGENTA);
		if (dFace.getLeftEye() != null) {
			// Ok the eyes are relative to the face
			Rect offset = new Rect(dFace.getFace().x() + dFace.getLeftEye().x(),
					dFace.getFace().y() + dFace.getLeftEye().y(),
					dFace.getLeftEye().width(),
					dFace.getLeftEye().height());
			drawRect(image, offset, CvScalar.BLUE);
		}
		if (dFace.getRightEye() != null) {
			Rect offset = new Rect(dFace.getFace().x() + dFace.getRightEye().x(),
					dFace.getFace().y() + dFace.getRightEye().y(),
					dFace.getRightEye().width(),
					dFace.getRightEye().height());
			drawRect(image, offset, CvScalar.RED);
		}
		if (dFace.getMouth() != null) {
			Rect offset = new Rect(dFace.getFace().x() + dFace.getMouth().x(),
					dFace.getFace().y() + dFace.getMouth().y(),
					dFace.getMouth().width(),
					dFace.getMouth().height());
			drawRect(image, offset, CvScalar.GREEN);
		}

	}

	private void drawRects(IplImage image, RectVector rects, CvScalar color) {
		for (int i = 0 ; i < rects.size(); i++) {
			Rect face = rects.get(i);
			drawRect(image, face, color);
		}
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

	public static boolean isInside(Rect r1, Rect r2) {
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

	public static boolean rectOverlap(Rect r, Rect test) {

		if (test == null || r == null) {
			return false;
		}
		return (((r.x() >= test.x()) && (r.x() < (test.x() + test.width()))) || 
				((test.x() >= r.x()) && (test.x() < (r.x() + r.width())))) &&
				(((r.y() >= test.y()) && (r.y() < (test.y() + test.height()))) || 
						((test.y() >= r.y()) && (test.y() < (r.y() + r.height()))));
	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO: what should we do here?
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

	public String getCascadeDir() {
		return cascadeDir;
	}

	public void setCascadeDir(String cascadeDir) {
		this.cascadeDir = cascadeDir;
	}

}

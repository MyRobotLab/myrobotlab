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
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import static org.bytedeco.javacpp.opencv_face.createFisherFaceRecognizer;
import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
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
	private String cascadeDir = "haarcascades";
	private CascadeClassifier faceCascade;
	private CascadeClassifier eyeCascade;
	private CascadeClassifier mouthCascade;
	// TODO: why the heck do we need to convert back and forth, and is this effecient?!?!
	private OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
	private OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();
	
	private HashMap<Integer, String> idToLabelMap = new HashMap<Integer,String>();
	
	private CvFont font = cvFont(CV_FONT_HERSHEY_PLAIN);

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
		faceCascade = new CascadeClassifier(cascadeDir+"/haarcascade_frontalface_default.xml");
		eyeCascade = new CascadeClassifier(cascadeDir+"/haarcascade_eye.xml");
		// TODO: find a better mouth classifier! this one kinda sucks.
		mouthCascade = new CascadeClassifier(cascadeDir+"/haarcascade_mcs_mouth.xml");
		// mouthCascade = new CascadeClassifier(cascadeDir+"/haarcascade_mouth.xml");
		// noseCascade = new CascadeClassifier(cascadeDir+"/haarcascade_nose.xml");
	}

	// TODO: create some sort of a life cycle for this.
	public boolean train() {
		
		int numLabels = 0;
		// TODO: consider adding the mask as a filter.
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
			String filenamePart = image.getName().split("\\-")[0];
			int label = filenamePart.hashCode();
			idToLabelMap.put(label, filenamePart);
			try {
				label = Integer.parseInt(image.getName().split("\\-")[0]);
			} catch (NumberFormatException e) {
				log.warn("filename not parsed. using hash code.");
				// e.printStackTrace();
			}
			
			// make sure all our test images are resized 
			Mat resized = resizeImage(img);
			// so, now our input for the training set is always 256x256 image.
			// we should probably run face detect and center this resized image.. 
			// no idea what sort of data it's going to be pumping out..

			// TODO: we should detect face in the image, and then crop/scale and insert it into 
			// the array of images.

			// TODO: our training images are indexed by integer,
			// we're really prefer to have a string map, so we have a human readable label
			images.put(counter, resized);
			labelsBuf.put(counter, label);
			counter++;
		}
		
		// TODO: expose the other types of recognizers ?
		faceRecognizer = createFisherFaceRecognizer();
		//faceRecognizer = createEigenFaceRecognizer();
		// faceRecognizer = createLBPHFaceRecognizer()
		//log.info("skipping training for now.");
		// must be at least 2 things to classify
		if (idToLabelMap.keySet().size() > 1) {
			faceRecognizer.train(images, labels);
			trained = true;
		} else {
			log.info("No labeled images loaded. training skipped");
			trained = false;
		}
		
		return true;
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

	@Override
	public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {
		// Convert to a grayscale image.  
		//(TODO: maybe convert to a Mat first, then cut color? not sure what's faster 
		
		if (Mode.TRAIN.equals(mode)) {
			String status = "Training Mode: " + trainName;
			cvPutText(image, status, cvPoint(20,40), font, CvScalar.GREEN);
		} else if (Mode.RECOGNIZE.equals(mode)) {
			String status = "Recognize Mode";
			cvPutText(image, status, cvPoint(20,40), font, CvScalar.YELLOW);
			
		}
		IplImage imageBW = IplImage.create(image.width(), image.height(),8,1);
		cvCvtColor(image, imageBW, CV_BGR2GRAY);
		// TODO: this seems super wonky!  isn't there an easy way to go from IplImage to opencv Mat?
		Frame frame = converterToMat.convert(imageBW);
		int cols = frame.imageWidth;
		int rows = frame.imageHeight;
		// This is the black and white image that we'll work with.
		Mat bwImgMat = converterToIpl.convertToMat(frame);

		ArrayList<DetectedFace> dFaces = extractDetectedFaces(bwImgMat);
		
		// Ok, for each of these detected faces we should try to classify them.
		log.info("We found {} faces!!!", dFaces.size());

		for (DetectedFace dF : dFaces) {
			// highlight! 
			drawFaceRects(image, dF);
			if (dF.isComplete()) {
				// Ok we have a complete face. lets get the affine points.

				// ultimately we want to find the center of the eyes
				// and the mouth so we can rotate and scale the image?

				// left eye center
				int centerleftx = dF.getLeftEye().x() + dF.getLeftEye().width()/2;
				int centerlefty = dF.getLeftEye().y() + dF.getLeftEye().height()/2;
				// right side center
				int centerrightx = dF.getRightEye().x() + dF.getRightEye().width()/2;
				int centerrighty = dF.getRightEye().y() + dF.getRightEye().height()/2;

				// mouth center.
				int centermouthx = dF.getMouth().x() + dF.getMouth().width()/2;
				int centermouthy = dF.getMouth().y() + dF.getMouth().height()/2;						

				// and array of 3 x,y points.
				int[][] ipts1 = new int[3][2];
				// point 1
				ipts1[0][0] = centerleftx;
				ipts1[0][1] = centerlefty;
				// point 2
				ipts1[1][0] = centerrightx;
				ipts1[1][1] = centerrighty;
				// point 3
				ipts1[2][0] = centermouthx;
				ipts1[2][1] = centermouthy;

				// create the points
				Point2f srcTri = new Point2f(3);
				Point2f dstTri = new Point2f(3);

				// populate source triangle
				srcTri.position(0).x((float)centerleftx).y((float)centerlefty);
				srcTri.position(1).x((float)centerrightx).y((float)centerrighty);
				srcTri.position(2).x((float)centermouthx).y((float)centermouthy);

				// populate dest triangle.
				dstTri.position(0).x((float)(cols*.3)).y((float)(rows*.45));
				dstTri.position(1).x((float)(cols*.7)).y((float)(rows*.45));
				dstTri.position(2).x((float)(cols*.5)).y((float)(rows*.85));

				// create the affine rotation/scale matrix
				Mat warpMat = getAffineTransform( srcTri.position(0), dstTri.position(0) );
				
				// Ok, if we do it to the original image..  this will bust 
				// TODO: support multiple face detect properly!
				warpAffine(bwImgMat, bwImgMat, warpMat, new Size(cols,rows));
				// TODO: allow debug display of the warped image.
				try {
					// TODO: why do i have to close these?!
					srcTri.close();
					dstTri.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Ok, now we've got the bwImgMat, that we'll either 
				// save this image as a training image.
				// or we'll try to classify the image with our current model.
				if (Mode.TRAIN.equals(mode)) {
					// we're in training mode.. so we should save the image
					// TODO: save image with the train name
					log.info("Training Mode for {}.", trainName);
					if (!StringUtils.isEmpty(trainName)) {
						// ok we know the name for this face. 
						// we have the trimmed down image representing the
						// face. save it off.
						long i = System.currentTimeMillis();
						// TODO: encode a proper/better filename.
						// TODO: choose some sort of better unique filename.
						String filename = trainingDir + "/" + trainName + "-" + i + ".png";
						// TODO: what format is this?!!
						imwrite(filename, bwImgMat);
						
						String status = "Snapshot Saved.";
						cvPutText(image, status, cvPoint(20,60), font, CvScalar.CYAN);

					} else {
						log.warn("In Training mode, but the trainName isn't set!");
					}
				} else if (Mode.RECOGNIZE.equals(mode)) {
					// You bettah recognize!
					if (!trained) {
						// we are a young grasshopper.
						log.info("Classifier not trained yet.");
						return image;
					} else {
						// Ok... now we've gotta predict something!
						bwImgMat = resizeImage(bwImgMat);
						int predictedLabel = faceRecognizer.predict(bwImgMat);
						log.info("Recognized a Face {}", predictedLabel);
						String name = Integer.toString(predictedLabel);
						if (idToLabelMap.containsKey(predictedLabel)) {
							name = idToLabelMap.get(predictedLabel);
						}
						String labelString = "Recognized: " + name;
						// now we should pick the lower left corner of the rect.
						cvPutText(image, labelString, cvPoint(dF.getFace().x(), dF.getFace().y()+dF.getFace().height()), font, CvScalar.CYAN);
						return image;
					}
				} else {
					return image;
				}
				
			}

		}
		//return converterToIpl.convertToIplImage(converterToIpl.convert(bwImgMat));
		return image;
		
	}

	private ArrayList<DetectedFace> extractDetectedFaces(Mat bwImgMat) {
		ArrayList<DetectedFace> dFaces = new ArrayList<DetectedFace>();
		// first lets pick up on the face. we'll asume the eyes and mouth are inside.
		RectVector faces = detectFaces(bwImgMat);
		// TODO: take only non overlapping faces.
		// Ok, we have a face, so... we should try to find the eyes and mouths.
		RectVector eyes = detectEyes(bwImgMat);
		RectVector mouths = detectMouths(bwImgMat);
		// Now that we've got eyes and mouths.. lets see if they
		// line up on the faces..
		for (int i = 0 ; i < faces.size(); i++) {
			DetectedFace dFace = new DetectedFace();
			Rect face = faces.get(i);
			// the face!
			dFace.setFace(face);
			// ok find the mouth in the face.
			for (int m = 0 ; m < mouths.size(); m++) {
				Rect mouth = mouths.get(m);
				// TODO: evaluate what's a better match
				// maybe many mouths
				// Ok, now we need to find the eyes in this face
				for (int e = 0 ; e < eyes.size(); e++) {
					// this one is a bit trickier , we need 2 eyes in the face.
					// but i'm not sure which is left or right?!
					Rect eye = eyes.get(e);
					if (isInside(face, eye)) {
						// TODO: some better way to know which is left & right.
						// for now, just taking the first and second one we find inside the face.
						// this could be backwards?!
						if (dFace.getLeftEye() == null) {
							dFace.setLeftEye(eye);
						} else {
							dFace.setRightEye(eye);
						}
					}
				}
				// TODO: reverse the isInside method args.  seems backwards currently.
				if (isInside(face, mouth)) {
					if (!rectOverlap(dFace.getLeftEye(), mouth) && !rectOverlap(dFace.getRightEye(), mouth)) {
						dFace.setMouth(mouth);
					}
				}
			}
			// add this to a face that we've found.
			dFaces.add(dFace);
		}
		return dFaces;
	}

	private void drawFaceRects(IplImage image, DetectedFace dFace) {
		// helper function to draw rectangles around the detected face(s)
		drawRect(image, dFace.getFace(), CvScalar.MAGENTA);
		if (dFace.getLeftEye() != null) {
			drawRect(image, dFace.getLeftEye(), CvScalar.BLUE);
		}
		if (dFace.getRightEye() != null) {
			drawRect(image, dFace.getRightEye(), CvScalar.RED);
		}
		if (dFace.getMouth() != null) {
			drawRect(image, dFace.getMouth(), CvScalar.GREEN);
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

		if (test == null || r == null) {
			return false;
		}
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

	public String getCascadeDir() {
		return cascadeDir;
	}

	public void setCascadeDir(String cascadeDir) {
		this.cascadeDir = cascadeDir;
	}



}

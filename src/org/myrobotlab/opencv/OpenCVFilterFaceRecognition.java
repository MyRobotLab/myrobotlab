package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.helper.opencv_legacy.cvCalcEigenObjects;
import static org.bytedeco.javacpp.helper.opencv_legacy.cvEigenDecomposite;
import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_core.CV_L1;
import static org.bytedeco.javacpp.opencv_core.CV_STORAGE_READ;
import static org.bytedeco.javacpp.opencv_core.CV_STORAGE_WRITE;
import static org.bytedeco.javacpp.opencv_core.CV_TERMCRIT_ITER;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_32F;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvConvertScale;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvCreateMat;
import static org.bytedeco.javacpp.opencv_core.cvMinMaxLoc;
import static org.bytedeco.javacpp.opencv_core.cvNormalize;
import static org.bytedeco.javacpp.opencv_core.cvOpenFileStorage;
import static org.bytedeco.javacpp.opencv_core.cvReadByName;
import static org.bytedeco.javacpp.opencv_core.cvReadIntByName;
import static org.bytedeco.javacpp.opencv_core.cvReadStringByName;
import static org.bytedeco.javacpp.opencv_core.cvRect;
import static org.bytedeco.javacpp.opencv_core.cvReleaseFileStorage;
import static org.bytedeco.javacpp.opencv_core.cvReleaseImage;
import static org.bytedeco.javacpp.opencv_core.cvResetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvSetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_core.cvTermCriteria;
import static org.bytedeco.javacpp.opencv_core.cvWrite;
import static org.bytedeco.javacpp.opencv_core.cvWriteInt;
import static org.bytedeco.javacpp.opencv_core.cvWriteString;
import static org.bytedeco.javacpp.opencv_highgui.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_highgui.cvLoadImage;
import static org.bytedeco.javacpp.opencv_highgui.cvSaveImage;
import static org.bytedeco.javacpp.opencv_legacy.CV_EIGOBJ_NO_CALLBACK;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core.CvFileStorage;
import org.bytedeco.javacpp.opencv_core.CvMat;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvSize;
import org.bytedeco.javacpp.opencv_core.CvTermCriteria;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterFaceRecognition extends OpenCVFilter {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4324954034870485036L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFaceRecognition.class.getCanonicalName());


	private int nTrainFaces = 0;
	/** the training face image array */
	IplImage[] trainingFaceImgArr;
	/** the test face image array */
	IplImage[] testFaceImgArr;
	/** the person number array */
	CvMat personNumTruthMat;
	/** the number of persons */
	int nPersons;
	/** the person names */
	final List<String> personNames = new ArrayList<String>();
	/** the number of eigenvalues */
	int nEigens = 0;
	/** eigenvectors */
	IplImage[] eigenVectArr;
	/** eigenvalues */
	CvMat eigenValMat;
	/** the average image */
	IplImage pAvgTrainImg;
	/** the projected training faces */
	CvMat projectedTrainFaceMat;


	public OpenCVFilterFaceRecognition()  {
		super();
	}

	public OpenCVFilterFaceRecognition(String name) {
		super(name);
	}

	public void learn(final String trainingFileName) {
		int i;

		// load training data
		log.info("===========================================");
		log.info("Loading the training images in " + trainingFileName);
		trainingFaceImgArr = loadFaceImgArray(trainingFileName);
		nTrainFaces = trainingFaceImgArr.length;
		log.info("Got " + nTrainFaces + " training images");
		if (nTrainFaces < 3) {
			log.error("Need 3 or more training faces\n"
					+ "Input file contains only " + nTrainFaces);
			return;
		}

		// do Principal Component Analysis on the training faces
		doPCA();

		log.info("projecting the training images onto the PCA subspace");
		// project the training images onto the PCA subspace
		projectedTrainFaceMat = cvCreateMat(
				nTrainFaces, // rows
				nEigens, // cols
				CV_32FC1); // type, 32-bit float, 1 channel

		// initialize the training face matrix - for ease of debugging
		for (int i1 = 0; i1 < nTrainFaces; i1++) {
			for (int j1 = 0; j1 < nEigens; j1++) {
				projectedTrainFaceMat.put(i1, j1, 0.0);
			}
		}

		log.info("created projectedTrainFaceMat with " + nTrainFaces + " (nTrainFaces) rows and " + nEigens + " (nEigens) columns");
		if (nTrainFaces < 5) {
			// TODO: get a larger training set and uncomment this - KW
			// LOGGER.info("projectedTrainFaceMat contents:\n" + oneChannelCvMatToString(projectedTrainFaceMat));
		}

		final FloatPointer floatPointer = new FloatPointer(nEigens);
		for (i = 0; i < nTrainFaces; i++) {
			cvEigenDecomposite(
					trainingFaceImgArr[i], // obj
					nEigens, // nEigObjs
					eigenVectArr, // eigInput (Pointer)
					0, // ioFlags
					null, // userData (Pointer)
					pAvgTrainImg, // avg
					floatPointer); // coeffs (FloatPointer)

			if (nTrainFaces < 5) {
				log.info("floatPointer: " + floatPointerToString(floatPointer));
			}
			for (int j1 = 0; j1 < nEigens; j1++) {
				projectedTrainFaceMat.put(i, j1, floatPointer.get(j1));
			}
		}
		if (nTrainFaces < 5) {
			log.info("projectedTrainFaceMat after cvEigenDecomposite:\n" + projectedTrainFaceMat);
		}

		// store the recognition data as an xml file
		storeTrainingData();

		// Save all the eigenvectors as images, so that they can be checked.
		storeEigenfaceImages();
	}


	/** Returns a string representation of the given float pointer.
	 *
	 * @param floatPointer the given float pointer
	 * @return a string representation of the given float pointer
	 */
	private String floatPointerToString(final FloatPointer floatPointer) {
		final StringBuilder stringBuilder = new StringBuilder();
		boolean isFirst = true;
		stringBuilder.append('[');
		for (int i = 0; i < floatPointer.capacity(); i++) {
			if (isFirst) {
				isFirst = false;
			} else {
				stringBuilder.append(", ");
			}
			stringBuilder.append(floatPointer.get(i));
		}
		stringBuilder.append(']');

		return stringBuilder.toString();
	}

	/** Saves all the eigenvectors as images, so that they can be checked. */
	private void storeEigenfaceImages() {
		// Store the average image to a file
		log.info("Saving the image of the average face as 'out_averageImage.bmp'");
		cvSaveImage("out_averageImage.bmp", pAvgTrainImg);

		// Create a large image made of many eigenface images.
		// Must also convert each eigenface image to a normal 8-bit UCHAR image instead of a 32-bit float image.
		log.info("Saving the " + nEigens + " eigenvector images as 'out_eigenfaces.bmp'");

		if (nEigens > 0) {
			// Put all the eigenfaces next to each other.
			int COLUMNS = 8;        // Put upto 8 images on a row.
			int nCols = Math.min(nEigens, COLUMNS);
			int nRows = 1 + (nEigens / COLUMNS);        // Put the rest on new rows.
			int w = eigenVectArr[0].width();
			int h = eigenVectArr[0].height();
			CvSize size = cvSize(nCols * w, nRows * h);
			final IplImage bigImg = cvCreateImage(
					size,
					IPL_DEPTH_8U, // depth, 8-bit Greyscale UCHAR image
					1);        // channels
			for (int i = 0; i < nEigens; i++) {
				// Get the eigenface image.
				IplImage byteImg = convertFloatImageToUcharImage(eigenVectArr[i]);
				// Paste it into the correct position.
				int x = w * (i % COLUMNS);
				int y = h * (i / COLUMNS);
				CvRect ROI = cvRect(x, y, w, h);
				cvSetImageROI(
						bigImg, // image
						ROI); // rect
				cvCopy(
						byteImg, // src
						bigImg, // dst
						null); // mask
				cvResetImageROI(bigImg);
				cvReleaseImage(byteImg);
			}
			cvSaveImage(
					"out_eigenfaces.bmp", // filename
					bigImg); // image
			cvReleaseImage(bigImg);
		}
	}

	/** Converts the given float image to an unsigned character image.
	 *
	 * @param srcImg the given float image
	 * @return the unsigned character image
	 */
	private IplImage convertFloatImageToUcharImage(IplImage srcImg) {
		IplImage dstImg;
		if ((srcImg != null) && (srcImg.width() > 0 && srcImg.height() > 0)) {
			// Spread the 32bit floating point pixels to fit within 8bit pixel range.
			double[] minVal = new double[1];
			double[] maxVal = new double[1];
			cvMinMaxLoc(srcImg, minVal, maxVal);
			// Deal with NaN and extreme values, since the DFT seems to give some NaN results.
			if (minVal[0] < -1e30) {
				minVal[0] = -1e30;
			}
			if (maxVal[0] > 1e30) {
				maxVal[0] = 1e30;
			}
			if (maxVal[0] - minVal[0] == 0.0f) {
				maxVal[0] = minVal[0] + 0.001;  // remove potential divide by zero errors.
			}                        // Convert the format
			dstImg = cvCreateImage(cvSize(srcImg.width(), srcImg.height()), 8, 1);
			cvConvertScale(srcImg, dstImg, 255.0 / (maxVal[0] - minVal[0]), -minVal[0] * 255.0 / (maxVal[0] - minVal[0]));
			return dstImg;
		}
		return null;
	}

	/** Stores the training data to the file 'facedata.xml'. */
	private void storeTrainingData() {
		CvFileStorage fileStorage;
		int i;

		String faceDataFile =  "facedetect/facedetect.xml";
		log.info("writing "+ faceDataFile);

		// create a file-storage interface
		fileStorage = cvOpenFileStorage(
				faceDataFile, // filename
				null, // memstorage
				CV_STORAGE_WRITE, // flags
				null); // encoding

		// Store the person names. Added by Shervin.
		cvWriteInt(
				fileStorage, // fs
				"nPersons", // name
				nPersons); // value

		for (i = 0; i < nPersons; i++) {
			String varname = "personName_" + (i + 1);
			cvWriteString(
					fileStorage, // fs
					varname, // name
					personNames.get(i), // string
					0); // quote
		}

		// store all the data
		cvWriteInt(
				fileStorage, // fs
				"nEigens", // name
				nEigens); // value

		cvWriteInt(
				fileStorage, // fs
				"nTrainFaces", // name
				nTrainFaces); // value

		cvWrite(
				fileStorage, // fs
				"trainPersonNumMat", // name
				personNumTruthMat); // value

		cvWrite(
				fileStorage, // fs
				"eigenValMat", // name
				eigenValMat); // value

		cvWrite(
				fileStorage, // fs
				"projectedTrainFaceMat", // name
				projectedTrainFaceMat);

		cvWrite(fileStorage, // fs
				"avgTrainImg", // name
				pAvgTrainImg); // value

		for (i = 0; i < nEigens; i++) {
			String varname = "eigenVect_" + i;
			cvWrite(
					fileStorage, // fs
					varname, // name
					eigenVectArr[i]); // value
		}

		// release the file-storage interface
		cvReleaseFileStorage(fileStorage);
	}


	/** Reads the names & image filenames of people from a text file, and loads all those images listed.
	 *
	 * @param filename the training file name
	 * @return the face image array
	 */
	private IplImage[] loadFaceImgArray(final String filename) {
		IplImage[] faceImgArr;
		BufferedReader imgListFile;
		String imgFilename;
		int iFace = 0;
		int nFaces = 0;
		int i;
		try {
			// open the input file
			imgListFile = new BufferedReader(new FileReader(filename));

			// count the number of faces
			while (true) {
				final String line = imgListFile.readLine();
				if (line == null || line.isEmpty()) {
					break;
				}
				nFaces++;
			}
			log.info("nFaces: " + nFaces);
			imgListFile = new BufferedReader(new FileReader(filename));

			// allocate the face-image array and person number matrix
			faceImgArr = new IplImage[nFaces];
			personNumTruthMat = cvCreateMat(
					1, // rows
					nFaces, // cols
					CV_32SC1); // type, 32-bit unsigned, one channel

			// initialize the person number matrix - for ease of debugging
			for (int j1 = 0; j1 < nFaces; j1++) {
				personNumTruthMat.put(0, j1, 0);
			}

			personNames.clear();        // Make sure it starts as empty.
			nPersons = 0;

			// store the face images in an array
			for (iFace = 0; iFace < nFaces; iFace++) {
				String personName;
				String sPersonName;
				int personNumber;

				// read person number (beginning with 1), their name and the image filename.
				final String line = imgListFile.readLine();
				if (line.isEmpty()) {
					break;
				}
				final String[] tokens = line.split(" ");
				personNumber = Integer.parseInt(tokens[0]);
				personName = tokens[1];
				imgFilename = tokens[2];
				sPersonName = personName;
				log.info("Got " + iFace + " " + personNumber + " " + personName + " " + imgFilename);

				// Check if a new person is being loaded.
				if (personNumber > nPersons) {
					// Allocate memory for the extra person (or possibly multiple), using this new person's name.
					personNames.add(sPersonName);
					nPersons = personNumber;
					log.info("Got new person " + sPersonName + " -> nPersons = " + nPersons + " [" + personNames.size() + "]");
				}

				// Keep the data
				personNumTruthMat.put(
						0, // i
						iFace, // j
						personNumber); // v

				// load the face image
				faceImgArr[iFace] = cvLoadImage(
						imgFilename, // filename
						CV_LOAD_IMAGE_GRAYSCALE); // isColor

				if (faceImgArr[iFace] == null) {
					throw new RuntimeException("Can't load image from " + imgFilename);
				}
			}

			imgListFile.close();

		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		log.info("Data loaded from '" + filename + "': (" + nFaces + " images of " + nPersons + " people).");
		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("People: ");
		if (nPersons > 0) {
			stringBuilder.append("<").append(personNames.get(0)).append(">");
		}
		for (i = 1; i < nPersons && i < personNames.size(); i++) {
			stringBuilder.append(", <").append(personNames.get(i)).append(">");
		}
		log.info(stringBuilder.toString());

		return faceImgArr;
	}


	/** Does the Principal Component Analysis, finding the average image and the eigenfaces that represent any image in the given dataset. */
	private void doPCA() {
		int i;
		CvTermCriteria calcLimit;
		CvSize faceImgSize = new CvSize();

		// set the number of eigenvalues to use
		nEigens = nTrainFaces - 1;

		log.info("allocating images for principal component analysis, using " + nEigens + (nEigens == 1 ? " eigenvalue" : " eigenvalues"));

		// allocate the eigenvector images
		faceImgSize.width(trainingFaceImgArr[0].width());
		faceImgSize.height(trainingFaceImgArr[0].height());
		eigenVectArr = new IplImage[nEigens];
		for (i = 0; i < nEigens; i++) {
			eigenVectArr[i] = cvCreateImage(
					faceImgSize, // size
					IPL_DEPTH_32F, // depth
					1); // channels
		}

		// allocate the eigenvalue array
		eigenValMat = cvCreateMat(
				1, // rows
				nEigens, // cols
				CV_32FC1); // type, 32-bit float, 1 channel

		// allocate the averaged image
		pAvgTrainImg = cvCreateImage(
				faceImgSize, // size
				IPL_DEPTH_32F, // depth
				1); // channels

		// set the PCA termination criterion
		calcLimit = cvTermCriteria(
				CV_TERMCRIT_ITER, // type
				nEigens, // max_iter
				1); // epsilon

		log.info("computing average image, eigenvalues and eigenvectors");
		// compute average image, eigenvalues, and eigenvectors
		cvCalcEigenObjects(
				nTrainFaces, // nObjects
				trainingFaceImgArr, // input
				eigenVectArr, // output
				CV_EIGOBJ_NO_CALLBACK, // ioFlags
				0, // ioBufSize
				null, // userData
				calcLimit,
				pAvgTrainImg, // avg
				eigenValMat.data_fl()); // eigVals

		log.info("normalizing the eigenvectors");
		cvNormalize(
				eigenValMat, // src (CvArr)
				eigenValMat, // dst (CvArr)
				1, // a
				0, // b
				CV_L1, // norm_type
				null); // mask
	}
	
	
	 /** Opens the training data from the file 'facedata.xml'.
	   *
	   * @param pTrainPersonNumMat
	   * @return the person numbers during training, or null if not successful
	   */
	  private CvMat loadTrainingData() {
	    log.info("loading training data");
	    CvMat pTrainPersonNumMat = null; // the person numbers during training
	    CvFileStorage fileStorage;
	    int i;
	    String filename = "facedetect/facedetect.xml";
	    // create a file-storage interface
	    fileStorage = cvOpenFileStorage(
	            filename, // filename
	            null, // memstorage
	            CV_STORAGE_READ, // flags
	            null); // encoding
	    if (fileStorage == null) {
	      log.error("Can't open training database file 'facedata.xml'.");
	      return null;
	    }

	    // Load the person names.
	    personNames.clear();        // Make sure it starts as empty.
	    nPersons = cvReadIntByName(
	            fileStorage, // fs
	            null, // map
	            "nPersons", // name
	            0); // default_value
	    if (nPersons == 0) {
	      log.error("No people found in the training database 'facedata.xml'.");
	      return null;
	    } else {
	      log.info(nPersons + " persons read from the training database");
	    }

	    // Load each person's name.
	    for (i = 0; i < nPersons; i++) {
	      String sPersonName;
	      String varname = "personName_" + (i + 1);
	      sPersonName = cvReadStringByName(
	              fileStorage, // fs
	              null, // map
	              varname,
	              "");
	      personNames.add(sPersonName);
	    }
	    log.info("person names: " + personNames);

	    // Load the data
	    nEigens = cvReadIntByName(
	            fileStorage, // fs
	            null, // map
	            "nEigens",
	            0); // default_value
	    nTrainFaces = cvReadIntByName(
	            fileStorage,
	            null, // map
	            "nTrainFaces",
	            0); // default_value
	    Pointer pointer = cvReadByName(
	            fileStorage, // fs
	            null, // map
	            "trainPersonNumMat"); // name
	    pTrainPersonNumMat = new CvMat(pointer);

	    pointer = cvReadByName(
	            fileStorage, // fs
	            null, // map
	            "eigenValMat"); // name
	    eigenValMat = new CvMat(pointer);

	    pointer = cvReadByName(
	            fileStorage, // fs
	            null, // map
	            "projectedTrainFaceMat"); // name
	    projectedTrainFaceMat = new CvMat(pointer);

	    pointer = cvReadByName(
	            fileStorage,
	            null, // map
	            "avgTrainImg");
	    pAvgTrainImg = new IplImage(pointer);

	    eigenVectArr = new IplImage[nTrainFaces];
	    for (i = 0; i <= nEigens; i++) {
	      String varname = "eigenVect_" + i;
	      pointer = cvReadByName(
	              fileStorage,
	              null, // map
	              varname);
	      eigenVectArr[i] = new IplImage(pointer);
	    }

	    // release the file-storage interface
	    cvReleaseFileStorage(fileStorage);

	    log.info("Training data loaded (" + nTrainFaces + " training images of " + nPersons + " people)");
	    final StringBuilder stringBuilder = new StringBuilder();
	    stringBuilder.append("People: ");
	    if (nPersons > 0) {
	      stringBuilder.append("<").append(personNames.get(0)).append(">");
	    }
	    for (i = 1; i < nPersons; i++) {
	      stringBuilder.append(", <").append(personNames.get(i)).append(">");
	    }
	    log.info(stringBuilder.toString());

	    return pTrainPersonNumMat;
	  }


	@Override
	public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {

		float confidence = 0.0f;
		float[] projectedTestFace = new float[nEigens];
		int iNearest;
		int nearest;

		CvMat trainPersonNumMat;  // the person numbers during training
		
		// TODO: do this else where!
		 trainPersonNumMat = loadTrainingData();
		
		// project the test image onto the PCA subspace
		cvEigenDecomposite(
				image, // obj
				nEigens, // nEigObjs
				eigenVectArr, // eigInput (Pointer)
				0, // ioFlags
				null, // userData
				pAvgTrainImg, // avg
				projectedTestFace);  // coeffs

		//LOGGER.info("projectedTestFace\n" + floatArrayToString(projectedTestFace));

		final FloatPointer pConfidence = new FloatPointer(confidence);
		iNearest = findNearestNeighbor(projectedTestFace, new FloatPointer(pConfidence));
		confidence = pConfidence.get();
		nearest = trainPersonNumMat.data_i().get(iNearest);

		log.info("nearest = " + nearest + " . Confidence = " + confidence);



		// TODO Auto-generated method stub
		return null;
	}

	
	
	  /** Find the most likely person based on a detection. Returns the index, and stores the confidence value into pConfidence.
	   *
	   * @param projectedTestFace the projected test face
	   * @param pConfidencePointer a pointer containing the confidence value
	   * @param iTestFace the test face index
	   * @return the index
	   */
	  private int findNearestNeighbor(float projectedTestFace[], FloatPointer pConfidencePointer) {
	    double leastDistSq = Double.MAX_VALUE;
	    int i = 0;
	    int iTrain = 0;
	    int iNearest = 0;

	    log.info("................");
	    log.info("find nearest neighbor from " + nTrainFaces + " training faces");
	    for (iTrain = 0; iTrain < nTrainFaces; iTrain++) {
	      //LOGGER.info("considering training face " + (iTrain + 1));
	      double distSq = 0;

	      for (i = 0; i < nEigens; i++) {
	        //LOGGER.debug("  projected test face distance from eigenface " + (i + 1) + " is " + projectedTestFace[i]);

	        float projectedTrainFaceDistance = (float) projectedTrainFaceMat.get(iTrain, i);
	        float d_i = projectedTestFace[i] - projectedTrainFaceDistance;
	        distSq += d_i * d_i; // / eigenValMat.data_fl().get(i);  // Mahalanobis distance (might give better results than Eucalidean distance)
//	          if (iTrain < 5) {
//	            LOGGER.info("    ** projected training face " + (iTrain + 1) + " distance from eigenface " + (i + 1) + " is " + projectedTrainFaceDistance);
//	            LOGGER.info("    distance between them " + d_i);
//	            LOGGER.info("    distance squared " + distSq);
//	          }
	      }

	      if (distSq < leastDistSq) {
	        leastDistSq = distSq;
	        iNearest = iTrain;
	        log.info("  training face " + (iTrain + 1) + " is the new best match, least squared distance: " + leastDistSq);
	      }
	    }

	    // Return the confidence level based on the Euclidean distance,
	    // so that similar images should give a confidence between 0.5 to 1.0,
	    // and very different images should give a confidence between 0.0 to 0.5.
	    float pConfidence = (float) (1.0f - Math.sqrt(leastDistSq / (float) (nTrainFaces * nEigens)) / 255.0f);
	    pConfidencePointer.put(pConfidence);

	    log.info("training face " + (iNearest + 1) + " is the final best match, confidence " + pConfidence);
	    return iNearest;
	  }
	
	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub

	}

}

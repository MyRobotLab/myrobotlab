package org.myrobotlab.deeplearning4j;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.FacetParams;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.datavec.image.loader.NativeImageLoader;
import org.myrobotlab.image.Util;
import org.myrobotlab.opencv.CloseableFrameConverter;
import org.myrobotlab.service.Solr;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import lombok.Getter;

/**
 * A {@link DataSetIterator} which uses a Solr to search for data that is used
 * as a training dataset
 * 
 * This class will run a search against a solr instance (in myrobotlab) and
 * provide a dataset iterator on the results. It's expected that the solr index
 * has a field called "label" that contains the label for the document in
 * addition, it is also expected that the documents have a field called "bytes"
 * that contains a binary image.
 * 
 */
public class SolrDataSetIterator implements DataSetIterator {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final long serialVersionUID = 1L;
  // the global set of unique labels for the training set.
  final private String[] labelKeys;
  final private int batch;
  private int currentOffset = 0;
  private QueryResponse qresp = null;
  private Solr solr = null;
  private String queryString;
  private String labelField;
  // TODO: we should derive this from the training dataset automagically? maybe
  // not. (it's really more a function of the model...)
  final private int height;
  final private int width;
  final private int channels;
  protected DataSetPreProcessor preProcessor;
  int seed = 42;
  private DataSet[] cache;
  private CloseableFrameConverter converter = new CloseableFrameConverter();

  public SolrDataSetIterator(int batch, String queryString, Solr solr, int height, int width, int channels, String labelField) {
    log.info("Solr Dataset Iterator");
    this.batch = batch;
    this.height = height;
    this.width = width;
    this.channels = channels;
    // Here we should run a search and get some data back that we can iterate.
    // For now, just page it all into memory.. yum!
    this.solr = solr;
    this.queryString = queryString;
    this.labelField = labelField;
    // TODO: remove this, and implement pagination for the query result set.
    int numSamples = 1000;
    // max number of categories in the training data.
    int maxLabels = 100;
    SolrQuery solrQuery = new SolrQuery(queryString);
    solrQuery.setStart(0);
    solrQuery.setRows(numSamples);
    // the label field to use
    solrQuery.addFacetField(labelField);
    solrQuery.setFacetLimit(maxLabels);
    solrQuery.setFacetMinCount(1);
    solrQuery.setFacet(true);
    // add a random sort order.
    solrQuery.setSort("random_" + seed, ORDER.desc);

    // TODO: is this alpabetical on the label?
    solrQuery.setFacetSort(FacetParams.FACET_SORT_INDEX);
    qresp = solr.search(solrQuery);

    // grab the facets labels. (TODO: sort these?
    FacetField ff = qresp.getFacetFields().get(0);
    int length = ff.getValues().size();
    labelKeys = new String[length];
    int i = 0;
    for (Count c : ff.getValues()) {
      labelKeys[i] = c.getName();
      i++;
    }
    // here we should convert all docs to a dataset.
    int cacheSize = qresp.getResults().size();
    cache = new DataSet[cacheSize];
    for (int j = 0; j < cacheSize; j++) {
      SolrDocument doc = qresp.getResults().get(j);
      cache[j] = docToDataSet(doc);
    }

  }

  @Override
  public boolean hasNext() {
    // log.info("Has next?");
    // TODO: make this logic better .. if needed?
    if (currentOffset < qresp.getResults().getNumFound()) {
      // log.info("has next true");
      return true;
    }
    // log.info("has next false");
    return false;
  }

  @Override
  public DataSet next() {
    // log.info("Next");
    return next(batch);
  }

  @Override
  public DataSet next(int num) {
    log.info("Next with a num {} currOffset {}", num, currentOffset);
    try {
      final List<DataSet> rawDataSets = getRawDataSets(num);
      DataSet ds = convertDataSetsToDataSet(rawDataSets);
      return ds;
    } catch (IOException ioe) {
      return null;
    }
  }

  private List<DataSet> getRawDataSets(int numWanted) throws IOException {
    // log.info("Get raw datasets");
    final List<DataSet> rawDataSets = new ArrayList<DataSet>();
    while (hasNext() && 0 < numWanted) {
      // TODO: this is the meat and potatoes i guess?
      SolrDocument trainingDoc = qresp.getResults().get(currentOffset);
      // here we need to figure out where in the cache this is. something like
      int lookupOffset = currentOffset % batch;
      if (lookupOffset > cache.length) {
        log.warn("Uh oh!");
        // this a bad case
      } else {
        // i guess this is ok
      }
      currentOffset++;
      DataSet dataSet = cache[lookupOffset];
      if (dataSet != null) {
        rawDataSets.add(dataSet);
      } else {
        log.warn("Null dataset parsed for training doc. {}", trainingDoc);
      }
      --numWanted;
      // this.tuple = this.tupleStream.read();
    }
    // need to increment by the total number added to this result set.
    return rawDataSets;
  }

  private DataSet docToDataSet(SolrDocument trainingDoc) {
    // which document is being added tot he training result set.
    String label = (String) trainingDoc.getFirstValue(labelField);
    // log.info("Doc ID: {} - {}", trainingDoc.getFirstValue("id"), label);
    // TODO: grab the bytes field.. and convert to an IplImage (or NDArray?)
    byte[] bytes = (byte[]) trainingDoc.getFirstValue("bytes");
    NativeImageLoader loader = new NativeImageLoader(height, width, channels);
    try {
      IplImage iplImage = Util.bytesToImage(bytes);
      // solr.show(iplImage, label + " " + trainingDoc.getFirstValue("id"));
      // TODO: just get the buffered image directly.

      BufferedImage buffImg = converter.toBufferedImage(iplImage);
      // I think i'm supposed to get an array of these images and lump them
      // together.
      INDArray image = loader.asMatrix(buffImg);
      // Frame f = loader.asFrame(buffImg);
      // OpenCVFrameConverter.ToIplImage grabberConverter = new
      // OpenCVFrameConverter.ToIplImage();
      // IplImage resImage = grabberConverter.convert(f);
      // solr.show(resImage, "Converted");

      // now we need the label.
      // can we go indarray back to iplimage?
      // convert to a one hot encoding for the labels.
      final INDArray labels = getOneHot(this.labelKeys, label);
      // ultimately create a dataset out of this example.
      final DataSet rawDataSet = new DataSet(image, labels);
      return rawDataSet;
      // rawDataSets.add(rawDataSet);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  private INDArray getOneHot(String[] keys, String label) {
    // log.info("get values");
    final double[] values = new double[keys.length];
    for (int i = 0; i < keys.length; i++) {
      if (keys[i].equals(label))
        values[i] = 1.0;
      else
        values[i] = 0.0;
    }
    INDArray res = Nd4j.create(values);
    // log.info("Encoding {}", res);
    return res;
  }

  private DataSet convertDataSetsToDataSet(List<DataSet> rawDataSets) throws IOException {
    final int numFound = rawDataSets.size();
    log.info("Convert to data set num found: {}", numFound);
    if (numFound == 0) {
      return null;
    }
    final INDArray inputs = Nd4j.create(numFound, inputColumns());
    final INDArray labels = Nd4j.create(numFound, totalOutcomes());

    Collection<INDArray> inputArr = new ArrayList<INDArray>();
    for (int i = 0; i < numFound; i++) {
      final DataSet dataSet = rawDataSets.get(i);
      if (preProcessor != null) {
        preProcessor.preProcess(dataSet);
      }
      // TODO: I'm not sure this is correct.. but it does get the array into the
      // right "shape"
      INDArray arr = null;
      // arr = dataSet.getFeatures().getRow(0).getRow(0).reshape(channels,
      // height, width);
      // log.info("Dataset feature length: "
      // +dataSet.getFeatures().length());
      arr = dataSet.getFeatures().getRow(0).reshape(channels, height, width);

      // log.info("ARR feature length: " +arr.length());
      // INDArray arr =
      // dataSet.getFeatures().getRow(0).getRow(0).reshape(channels, height,
      // width);
      // INDArray arr = dataSet.getFeatures().getRow(0).getRow(0);
      // inputArr.add(arr);
      inputs.putRow(i, arr);
      labels.putRow(i, dataSet.getLabels());
    }

    // log.info("Total Number of inputs in array :" +inputs.length());
    // log.info(inputs.shapeInfoToString());
    // TODO: something goofy going on here.
    final INDArray inputsShaped = inputs.reshape(numFound, channels, height, width);
    return new DataSet(inputsShaped, labels);
    // return new DataSet(inputs, labels);
    // return mdsToDataSet(mds);
  }

  @Override
  public int inputColumns() {
    // log.info("Input columns called");
    // TODO : what the heck should this be?!
    // I think this really is the size of the input layer..
    // for image data? 640x480 ?! hmm.. o/w just the number of feature columns
    // on each of the examples..
    // TODO: this should be defined based on the image resolution i guess?
    return this.height * this.width * this.channels;
  }

  @Override
  public int totalOutcomes() {
    // log.info("Total Outcomes called");
    return this.labelKeys.length;
  }

  @Override
  public boolean resetSupported() {
    log.info("Reset supported called?");
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean asyncSupported() {
    log.info("Async supported called - false");
    return false;
  }

  @Override
  public void reset() {
    // log.info("Reset called!");
    this.currentOffset = 0;
  }

  @Override
  public int batch() {
    // log.info("Batch called");
    return this.batch;
  }

  @Override
  public void setPreProcessor(DataSetPreProcessor preProcessor) {
    this.preProcessor = preProcessor;
  }

  @Override
  public DataSetPreProcessor getPreProcessor() {
    return preProcessor;
  }

  @Override
  public List<String> getLabels() {
    // log.info("Get labels called..");
    ArrayList<String> labels = new ArrayList<String>();
    for (int i = 0; i < labelKeys.length; i++) {
      labels.add(labelKeys[i]);
    }
    return labels;
  }

}

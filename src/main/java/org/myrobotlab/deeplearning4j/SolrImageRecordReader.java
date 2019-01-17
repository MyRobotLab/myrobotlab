package org.myrobotlab.deeplearning4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.datavec.api.conf.Configuration;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.InputSplit;
import org.datavec.api.split.InputStreamInputSplit;
import org.datavec.api.util.files.FileFromPathIterator;
import org.datavec.api.util.ndarray.RecordConverter;
import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.datavec.api.writable.batch.NDArrayRecordBatch;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.datavec.image.transform.ImageTransform;
import org.nd4j.linalg.api.concurrency.AffinityManager;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * This class will load an image file by using a solr input document and
 * wrapping the bytes field in an input stream. This reads one record for solr ,
 * wraps the value in the bytes field in an input stream and converts it to an
 * NDArray
 * 
 * @author kwatters
 *
 */
public class SolrImageRecordReader extends ImageRecordReader {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final long serialVersionUID = 1L;

  @Override
  public void initialize(InputSplit split) throws IOException {

    // Ok. this seems reasonable create the native image loader.
    if (imageLoader == null) {
      imageLoader = new NativeImageLoader(height, width, channels, imageTransform);
    }

    // TODO: maybe solr is an input stream split?! we should be going down this
    // path?
    if (split instanceof InputStreamInputSplit) {
      this.inputSplit = split;
      this.finishedInputStreamSplit = false;
      return;
    }

    inputSplit = split;
    // TODO: don't call locations!!!
    // This totally sucks and is handled by solr by returning the labels with a
    // facet

    // we really just need a sorted list of labels for this split. right?

    URI[] locations = split.locations();
    if (locations != null && locations.length >= 1) {
      if (appendLabel && labelGenerator != null && labelGenerator.inferLabelClasses()) {
        Set<String> labelsSet = new HashSet<>();
        for (URI location : locations) {
          File imgFile = new File(location);
          String name = labelGenerator.getLabelForPath(location).toString();
          labelsSet.add(name);
          if (pattern != null) {
            String label = name.split(pattern)[patternPosition];
            fileNameMap.put(imgFile.toString(), label);
          }
        }
        labels.clear();
        labels.addAll(labelsSet);
        if (logLabelCountOnInit) {
          log.info("ImageRecordReader: {} label classes inferred using label generator {}", labelsSet.size(), labelGenerator.getClass().getSimpleName());
        }
      }
      iter = new FileFromPathIterator(inputSplit.locationsPathIterator()); // This
      // handles
      // randomization
      // internally
      // if
      // necessary
    } else
      throw new IllegalArgumentException("No path locations found in the split.");

    if (split instanceof FileSplit) {
      // remove the root directory
      FileSplit split1 = (FileSplit) split;
      labels.remove(split1.getRootDir());
    }

    // To ensure consistent order for label assignment (irrespective of file
    // iteration order), we want to sort the list of labels
    Collections.sort(labels);
  }

  public SolrImageRecordReader(int height, int width, int channels, ParentPathLabelGenerator labelMaker) {
    super(height, width, channels, labelMaker);
  }

  @Override
  public List<Writable> next() {
    // TODO maybe we can use the input stream input split?
    if (inputSplit instanceof InputStreamInputSplit) {
      InputStreamInputSplit inputStreamInputSplit = (InputStreamInputSplit) inputSplit;
      try {
        NDArrayWritable ndArrayWritable = new NDArrayWritable(imageLoader.asMatrix(inputStreamInputSplit.getIs()));
        finishedInputStreamSplit = true;
        return Arrays.<Writable> asList(ndArrayWritable);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if (iter != null) {
      List<Writable> ret;
      File image = iter.next();
      InputStream is = null;
      try {
        is = inputSplit.openInputStreamFor(image.getAbsolutePath());
      } catch (Exception e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      currentFile = image;

      if (image.isDirectory())
        return next();
      try {
        invokeListeners(image);
        INDArray row = imageLoader.asMatrix(is);
        Nd4j.getAffinityManager().ensureLocation(row, AffinityManager.Location.DEVICE);
        ret = RecordConverter.toRecord(row);
        if (appendLabel || writeLabel) {
          if (labelMultiGenerator != null) {
            ret.addAll(labelMultiGenerator.getLabels(image.getPath()));
          } else {
            if (labelGenerator.inferLabelClasses()) {
              // Standard classification use case (i.e., handle String ->
              // integer conversion
              ret.add(new IntWritable(labels.indexOf(getLabel(image.getPath()))));
            } else {
              // Regression use cases, and PathLabelGenerator instances that
              // already map to integers
              ret.add(labelGenerator.getLabelForPath(image.getPath()));
            }
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ret;
    } else if (record != null) {
      hitImage = true;
      invokeListeners(record);
      return record;
    }
    throw new IllegalStateException("No more elements");
  }

  @Override
  public List<List<Writable>> next(int num) {
    Preconditions.checkArgument(num > 0, "Number of examples must be > 0: got " + num);

    if (imageLoader == null) {
      imageLoader = new NativeImageLoader(height, width, channels, imageTransform);
    }

    List<File> currBatch = new ArrayList<>();

    int cnt = 0;

    int numCategories = (appendLabel || writeLabel) ? labels.size() : 0;
    List<Integer> currLabels = null;
    List<Writable> currLabelsWritable = null;
    List<List<Writable>> multiGenLabels = null;
    while (cnt < num && iter.hasNext()) {
      currentFile = iter.next();
      currBatch.add(currentFile);
      invokeListeners(currentFile);
      if (appendLabel || writeLabel) {
        // Collect the label Writables from the label generators
        if (labelMultiGenerator != null) {
          if (multiGenLabels == null)
            multiGenLabels = new ArrayList<>();

          multiGenLabels.add(labelMultiGenerator.getLabels(currentFile.getPath()));
        } else {
          if (labelGenerator.inferLabelClasses()) {
            if (currLabels == null)
              currLabels = new ArrayList<>();
            currLabels.add(labels.indexOf(getLabel(currentFile.getPath())));
          } else {
            if (currLabelsWritable == null)
              currLabelsWritable = new ArrayList<>();
            currLabelsWritable.add(labelGenerator.getLabelForPath(currentFile.getPath()));
          }
        }
      }
      cnt++;
    }

    INDArray features = Nd4j.createUninitialized(new long[] { cnt, channels, height, width }, 'c');
    Nd4j.getAffinityManager().tagLocation(features, AffinityManager.Location.HOST);
    for (int i = 0; i < cnt; i++) {
      try {
        InputStream is = inputSplit.openInputStreamFor(currBatch.get(i).getAbsolutePath());
        ((NativeImageLoader) imageLoader).asMatrixView(is, features.tensorAlongDimension(i, 1, 2, 3));
      } catch (Exception e) {
        log.info("Image file failed during load: {}", currBatch.get(i).getAbsolutePath());
        throw new RuntimeException(e);
      }
    }
    Nd4j.getAffinityManager().ensureLocation(features, AffinityManager.Location.DEVICE);

    List<INDArray> ret = new ArrayList<>();
    ret.add(features);
    if (appendLabel || writeLabel) {
      // And convert the previously collected label Writables from the label
      // generators
      if (labelMultiGenerator != null) {
        List<Writable> temp = new ArrayList<>();
        List<Writable> first = multiGenLabels.get(0);
        for (int col = 0; col < first.size(); col++) {
          temp.clear();
          for (List<Writable> multiGenLabel : multiGenLabels) {
            temp.add(multiGenLabel.get(col));
          }
          INDArray currCol = RecordConverter.toMinibatchArray(temp);
          ret.add(currCol);
        }
      } else {
        INDArray labels;
        if (labelGenerator.inferLabelClasses()) {
          // Standard classification use case (i.e., handle String -> integer
          // conversion)
          labels = Nd4j.create(cnt, numCategories, 'c');
          Nd4j.getAffinityManager().tagLocation(labels, AffinityManager.Location.HOST);
          for (int i = 0; i < currLabels.size(); i++) {
            labels.putScalar(i, currLabels.get(i), 1.0f);
          }
        } else {
          // Regression use cases, and PathLabelGenerator instances that already
          // map to integers
          if (currLabelsWritable.get(0) instanceof NDArrayWritable) {
            List<INDArray> arr = new ArrayList<>();
            for (Writable w : currLabelsWritable) {
              arr.add(((NDArrayWritable) w).get());
            }
            labels = Nd4j.concat(0, arr.toArray(new INDArray[arr.size()]));
          } else {
            labels = RecordConverter.toMinibatchArray(currLabelsWritable);
          }
        }

        ret.add(labels);
      }
    }

    return new NDArrayRecordBatch(ret);
  }

}

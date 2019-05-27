/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * http://docs.opencv.org/modules/imgproc/doc/feature_detection.html
 * http://stackoverflow.com/questions/19270458/cvcalcopticalflowpyrlk-not-working-as-expected
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.cvPoint;
import static org.bytedeco.opencv.global.opencv_imgproc.cvFont;
import static org.bytedeco.opencv.global.opencv_imgproc.cvPutText;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.bytedeco.opencv.global.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_FONT_HERSHEY_SIMPLEX;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_imgproc.CvFont;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Deeplearning4j;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Solr;
import org.slf4j.Logger;

/**
 * This implements the ability to overlay data from a solr search result on the
 * opencv display
 * 
 * @author kwatters
 *
 */
public class OpenCVFilterSolr extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  transient public final static Logger log = LoggerFactory.getLogger(OpenCVFilterSolr.class);

  private CvFont font = cvFont(CV_FONT_HERSHEY_PLAIN);
  private CvFont fontWarning = cvFont(CV_FONT_HERSHEY_PLAIN);

  private String formattedSearchResult = "No Result";

  private Solr solr = null;
  private String solrUrl = "http://localhost:8983/solr/wikipedia";

  public OpenCVFilterSolr() {
    super();
  }

  public OpenCVFilterSolr(String name) {
    super(name);
  }

  public void initSolr() {
    solr = (Solr) Runtime.createAndStart("solr", "Solr");
    solr.setSolrUrl(solrUrl);
  }

  public void populateSearch(String queryString, String field) {
    QueryResponse qResp = solr.search(queryString);
    SolrDocumentList results = qResp.getResults();
    if (results.size() > 0) {
      formattedSearchResult = results.get(0).getFirstValue(field).toString();
    } else {
      formattedSearchResult = "No Result";
    }
  }

  @Override
  public IplImage process(IplImage image) {

    if (solr == null)
      initSolr();

    cvPutText(image, formattedSearchResult, cvPoint(20, 40), font, CvScalar.GREEN);
    // TODO: get a handle to the solr instance.
    // TODO: display the solr search result text.
    return image;
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    // TODO Auto-generated method stub
    return null;
  }

}

package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.data.SearchResults;

public interface SearchListener extends NameProvider {

  public void onResults(SearchResults results);

  public void onImage(ImageData image);

  public void onImages(List<String> images);
}

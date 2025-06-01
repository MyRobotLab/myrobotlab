package org.myrobotlab.service.interfaces;

import java.io.IOException;
import java.util.List;

import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.data.SearchResults;

public interface SearchPublisher extends TextPublisher, LocaleProvider {

  // These are all the methods that the image publisher should produce.
  public static String[] publishMethods = new String[] { "publishResults", "publishImage", "publishImages" };

  SearchResults search(String searchText) throws IOException;

  List<ImageData> imageSearch(String searchText);

  SearchResults publishResults(SearchResults results);

  ImageData publishImage(ImageData image);

  List<String> publishImages(List<String> images);

  int setMaxImages(int cnt);

  default public void attachSearchListener(SearchListener display) {
    attachSearchListener(display.getName());
  }

  // Default way to attach an image listener so implementing classes need
  // not worry about these details.
  default public void attachSearchListener(String name) {
    for (String publishMethod : SearchPublisher.publishMethods) {
      addListener(publishMethod, name);
    }
  }

  default public void detachSearchListener(SearchListener display) {
    detachSearchListener(display.getName());
  }

  // Default way to attach an image listener so implementing classes need
  // not worry about these details.
  default public void detachSearchListener(String name) {
    for (String publishMethod : SearchPublisher.publishMethods) {
      removeListener(publishMethod, name);
    }
  }

  // Add the addListener method to the interface all services implement this.
  @Override
  public void addListener(String topicMethod, String callbackName);

  @Override
  public void removeListener(String topicMethod, String callbackName);

}
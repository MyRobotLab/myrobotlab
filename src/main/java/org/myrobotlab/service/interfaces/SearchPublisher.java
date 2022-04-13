package org.myrobotlab.service.interfaces;

import java.io.IOException;
import java.util.List;

import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.data.SearchResults;

public interface SearchPublisher extends TextPublisher, LocaleProvider {

  SearchResults search(String searchText) throws IOException;

  List<String> imageSearch(String searchText);

  SearchResults publishResults(SearchResults results);

  ImageData publishImage(ImageData image);

  List<String> publishImages(List<String> images);

  int setMaxImages(int cnt);

}
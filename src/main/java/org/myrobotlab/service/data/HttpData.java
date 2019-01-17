package org.myrobotlab.service.data;

public class HttpData {
  public byte[] data;
  public String uri;
  public Integer responseCode;
  public String contentType;

  public HttpData(String uri) {
    this.uri = uri;
  }

  public String toString() {
    int length = 0;
    if (data != null) {
      length = data.length;
    }
    return String.format("%d - content type %s, content length  %d", responseCode, contentType, length);
  }
}
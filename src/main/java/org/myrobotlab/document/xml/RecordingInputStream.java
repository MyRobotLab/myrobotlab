package org.myrobotlab.document.xml;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

// extended from example at:
// https://bytes.com/topic/net/answers/818268-java-sax-parser-how-get-raw-xml-code-currently-parsingevent
public class RecordingInputStream extends FilterInputStream {
  protected ByteArrayOutputStream sink;

  public RecordingInputStream(InputStream in) {
    this(in, new ByteArrayOutputStream());
  }

  RecordingInputStream(InputStream in, ByteArrayOutputStream sink) {
    super(in);
    this.sink = sink;
  }

  @Override
  public synchronized int read() throws IOException {
    int i = in.read();
    sink.write(i);
    return i;
  }

  @Override
  public synchronized int read(byte[] buf, int off, int len) throws IOException {
    int l = in.read(buf, off, len);
    if (l == -1)
      return -1;
    sink.write(buf, off, l);
    return l;
  }

  @Override
  public synchronized int read(byte[] buf) throws IOException {
    return read(buf, 0, buf.length);
  }

  @Override
  public synchronized long skip(long len) throws IOException {
    long l = 0;
    int i = 0;
    byte[] buf = new byte[1024];
    while (l < len) {
      i = read(buf, 0, (int) Math.min((long) buf.length, len - l));
      if (i == -1)
        break;
      l += i;
    }
    return l;
  }

  byte[] getBytes() {
    return sink.toByteArray();
  }

  void resetSink() {
    sink.reset();
  }

  public void clearUpTo(String string) throws IOException {

    String tempStr = sink.toString();
    sink.reset();
    int start = tempStr.indexOf(string);
    if (start != -1)
      sink.write(tempStr.substring(start).getBytes());
  }

  public String returnUpTo(String string) throws IOException {
    //
    String tmpStr = sink.toString();
    clearUpTo(string);
    int offset = tmpStr.indexOf(string) + string.length();
    return tmpStr.substring(0, offset);

  }

}

package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.CV_8UC1;
import static org.bytedeco.javacpp.opencv_core.cvMat;
import static org.bytedeco.javacpp.opencv_core.cvReleaseImage;
import static org.bytedeco.javacpp.opencv_highgui.cvDecodeImage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacpp.opencv_core.CvMat;
import org.bytedeco.javacpp.opencv_core.IplImage;

public class IPCameraFrameGrabber extends FrameGrabber {

	/*
	 * excellent reference - http://www.jpegcameras.com/ foscam url
	 * http://host/videostream.cgi?user=username&pwd=password
	 * http://192.168.0.59:60/videostream.cgi?user=admin&pwd=password android ip
	 * cam http://192.168.0.57:8080/videofeed
	 */
	public final static Logger log = LoggerFactory.getLogger(IPCameraFrameGrabber.class);

	private URL url;
	private URLConnection connection;
	private InputStream input;
	private Map<String, List<String>> headerfields;
	private String boundryKey;
	private IplImage template = null;
	private int width, height;

	IplImage decoded = null;

	public IPCameraFrameGrabber(String urlstr) {
		try {
			url = new URL(urlstr);
		} catch (MalformedURLException e) {
			Logging.logError(e);
		}
	}

	@Override
	public IplImage grab() {
		try {

			if (template == null) {
				// create the template for future frames
				Logging.logTime("creating template");
				template = IplImage.createFrom(grabBufferedImage());
				Logging.logTime("created template");
			}

			// // IplImage.create(template.width(), template.height(),
			// template.depth(), template.nChannels());

			byte[] b = readImage();
			CvMat mat = cvMat(1, b.length, CV_8UC1, new BytePointer(b));

			if (decoded != null) {
				// decoded.release();
				cvReleaseImage(decoded);
			}

			decoded = cvDecodeImage(mat);

			Logging.logTime("pre - IplImage.cvDecodeImage");
			return decoded;
		} catch (Exception e) {
			Logging.logError(e);
		} catch (IOException e) {
			Logging.logError(e);
		}
		return null;
	}

	public BufferedImage grabBufferedImage() throws Exception, IOException {
		BufferedImage bi = ImageIO.read(new ByteArrayInputStream(readImage()));
		return bi;
	}

	byte[] readImage() throws Exception, IOException {

		byte[] buffer = new byte[4096];// MTU or JPG Frame Size?
		int n = -1;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		StringBuffer sb = new StringBuffer();
		int total = 0;
		int c;
		// read http subheader
		while ((c = input.read()) != -1) {
			if (c > 0) {
				sb.append((char) c);
				if (c == 13) {
					sb.append((char) input.read());// '10'+
					c = input.read();
					sb.append((char) c);
					if (c == 13) {
						sb.append((char) input.read());// '10'
						break; // done with subheader
					}

				}
			}
		}
		// find embedded jpeg in stream
		String subheader = sb.toString();
		log.debug(subheader);
		int contentLength = -1;
		// if (boundryKey == null)
		// {
		// Yay! - server was nice and sent content length
		int c0 = subheader.indexOf("Content-Length: ");
		int c1 = subheader.indexOf('\r', c0);

		if (c0 < 0) {
			log.info("no content length returning null");
			return null;
		} else {
			log.info("found content length");
		}

		c0 += 16;
		contentLength = Integer.parseInt(subheader.substring(c0, c1).trim());
		log.debug("Content-Length: " + contentLength);

		// adaptive size - careful - don't want a 2G jpeg
		if (contentLength > buffer.length) {
			buffer = new byte[contentLength];
		}

		n = -1;
		total = 0;
		while ((n = input.read(buffer, 0, contentLength - total)) != -1) {
			total += n;
			baos.write(buffer, 0, n);

			if (total == contentLength) {
				break;
			}
		}

		baos.flush();

		input.read();// \r
		input.read();// \n
		input.read();// \r
		input.read();// \n

		return baos.toByteArray();
	}

	@Override
	public void release() throws Exception {
	}

	@Override
	public void start() {

		log.info("connecting to " + url);
		try {
			connection = url.openConnection();
			headerfields = connection.getHeaderFields();
			if (headerfields.containsKey("Content-Type")) {
				List<String> ct = headerfields.get("Content-Type");
				for (int i = 0; i < ct.size(); ++i) {
					String key = ct.get(i);
					int j = key.indexOf("boundary=");
					if (j != -1) {
						boundryKey = key.substring(j + 9); // FIXME << fragile
					}
				}
			}
			input = connection.getInputStream();
		} catch (IOException e) {
			Logging.logError(e);
		}
	}

	@Override
	public void stop() {
		try {
			input.close();
			input = null;
			connection = null;
			url = null;
		} catch (IOException e) {
			Logging.logError(e);
		}
	}

	@Override
	public void trigger() throws Exception {
	}

}

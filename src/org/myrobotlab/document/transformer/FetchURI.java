package org.myrobotlab.document.transformer;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.myrobotlab.document.Document;

public class FetchURI extends AbstractStage {

	private String uriField = "uri";
	private String bytesField = "bytes";

	@Override
	public void startStage(StageConfiguration config) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Document> processDocument(Document doc) {
		// TODO Auto-generated method stub
		// TODO: support https and other protocols

		for (Object o : doc.getField(uriField)) {
			byte[] page;
			try {
				page = fetchUrlAsByteArray(o.toString());
				doc.addToField(bytesField, page);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
		}

		return null;
	}


	private byte[] fetchUrlAsByteArray(String uri) throws IOException {
		URL url = new URL(uri);
		InputStream in = null;
		in = url.openStream();
		DataInputStream dis = new DataInputStream(new BufferedInputStream(in));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.copy(dis, baos);
		return baos.toByteArray();
	}



	@Override
	public void stopStage() {
		// TODO Auto-generated method stub

	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub

	}

	public String getUriField() {
		return uriField;
	}

	public void setUriField(String uriField) {
		this.uriField = uriField;
	}

	public String getBytesField() {
		return bytesField;
	}

	public void setBytesField(String bytesField) {
		this.bytesField = bytesField;
	}

}

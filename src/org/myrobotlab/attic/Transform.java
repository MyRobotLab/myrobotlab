package org.myrobotlab.attic;

import java.io.FileOutputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

public class Transform {

	public static void main(String[] args) {

		try {

			TransformerFactory tFactory = TransformerFactory.newInstance();
			tFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
			Transformer transformer = tFactory.newTransformer(new javax.xml.transform.stream.StreamSource("/home/gperry/apache-ant-1.8.2/src/etc/junit-noframes.xsl"));

			transformer.transform(new javax.xml.transform.stream.StreamSource("report/TESTS-TestSuites.xml"), new javax.xml.transform.stream.StreamResult(new FileOutputStream(
					"TESTS-TestSuites.html")));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

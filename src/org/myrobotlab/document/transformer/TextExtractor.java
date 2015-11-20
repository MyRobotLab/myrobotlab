package org.myrobotlab.document.transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;

import org.myrobotlab.document.transformer.StageConfiguration;
import org.myrobotlab.document.Document;

public class TextExtractor extends AbstractStage {

	private String textField = "text";
	private String filePathField = "filepath";

	@Override
	public void startStage(StageConfiguration config) {
		// TODO Auto-generated method stub


	}

	@Override
	public void processDocument(Document doc) {
		// TODO Auto-generated method stub

		// Create the parser..
		// not sure if the parser is thread safe, so we create a new one here
		// each time.  probably not effecient to do this.
		Parser  parser = new AutoDetectParser();
		ParseContext  parseCtx = new ParseContext();
		parseCtx.set(Parser.class, parser);

		// TODO how does the doc model support this?
		if (!doc.hasField(filePathField)) {
			return;
		}

		// we have the field populated
		for (Object pathObj : doc.getField(filePathField)) {

			// TODO: test the object type here.
			String path = (String)pathObj;
			
			File f = new File(path);
			if (!f.exists()) {
				// TODO: log that the file path was not found
				System.out.println("File path not found " + path);
				continue;
			}
			
			FileInputStream binaryData = null;
			try {
				binaryData = new FileInputStream(f);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				// This should never happen.
				continue;
			}
			//InputStream binaryData = null;

			if (binaryData == null) {
				// This should never happen either.
				continue;
			}
			
			Metadata metadata = new Metadata();
			StringWriter textData = new StringWriter();
			ContentHandler bch = new BodyContentHandler(textData);
			try {
				parser.parse(binaryData, bch, metadata, parseCtx);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TikaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			doc.addToField(textField, textData.toString());
			for (String name : metadata.names()) {
				// clean the field name first.
				String cleanName = cleanFieldName(name);
				for (String value : metadata.getValues(name)) {
					doc.addToField(cleanName , value);
				}
			}
		}

	}
	private static String cleanFieldName(String name) {
		String cleanName = name.trim().toLowerCase();
		cleanName = cleanName.replaceAll(" ", "_");
		cleanName = cleanName.replaceAll("-" , "_");
		cleanName = cleanName.replaceAll(":" , "_");
		return cleanName;
	}

	@Override
	public void stopStage() {
		// TODO Auto-generated method stub

	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub

	}

}

package org.myrobotlab.document.transformer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.myrobotlab.document.Document;

/**
 * This stage will take the values in the inputField and attempt to parse them
 * into a date object based on the formatString.  The successfully parsed values 
 * will be stored in the outputField.  The values will overwrite the outputField values.
 * 
 * @author kwatters
 *
 */
public class ParseDate extends AbstractStage {

	private String inputField = null;
	private String outputField = "date";
	private String formatString = "MM/dd/YYYY";
	private SimpleDateFormat sdf = null;
	
	@Override
	public void startStage(StageConfiguration config) {
		// TODO Auto-generated method stub
		if (config != null) {
			inputField = config.getProperty("inputField");
			outputField = config.getProperty("outputField", "date");
			formatString = config.getProperty("formatString", "MM/dd/YYYY");			
		}
		
		sdf = new SimpleDateFormat(formatString);
		
	}

	@Override
	public List<Document> processDocument(Document doc) {
		
		if (!doc.hasField(inputField)) {
			return null;
		}
		
		ArrayList<Date> dates = new ArrayList<Date>();
		
		for (Object val : doc.getField(inputField)) {
			if (val instanceof String) {
				try {
					Date d = sdf.parse(val.toString());
					dates.add(d);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					//System.out.println("Invalid Date Field: " + val);
				}				
			}
		}		
		
		doc.removeField(outputField);
		for (Date d: dates) {
			doc.addToField(outputField, d);
		}
		
		return null;
	}

	@Override
	public void stopStage() {
		// NOOP
	}

	@Override
	public void flush() {
		// NOOP
	}

}

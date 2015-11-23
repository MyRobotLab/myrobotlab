package org.myrobotlab.document.transformer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.myrobotlab.document.Document;

public class RegexExtractor extends AbstractStage {

	private String inputField = null;
	private String outputField = null;
	private String regex = null;
	
	private Pattern pattern;
	
	@Override
	public void startStage(StageConfiguration config) {
		// TODO Much more stuff like group support and field mapping for the groups
		pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
	}

	@Override
	public void processDocument(Document doc) {
		// TODO Auto-generated method stub
		for (Object o : doc.getField(inputField)) {
			String text = o.toString();
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				// TODO: test me!
				String match = text.substring(matcher.start(), matcher.end());
				doc.setField(outputField, match);
			}
		}
	}

	@Override
	public void stopStage() {
		// TODO Auto-generated method stub

	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
	}

	public String getInputField() {
		return inputField;
	}

	public void setInputField(String inputField) {
		this.inputField = inputField;
	}

	public String getOutputField() {
		return outputField;
	}

	public void setOutputField(String outputField) {
		this.outputField = outputField;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

}

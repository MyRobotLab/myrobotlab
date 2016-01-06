package org.myrobotlab.document.transformer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.apache.commons.lang.StringUtils;
import org.myrobotlab.document.Document;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenNLP extends AbstractStage {

	public final static Logger log = LoggerFactory.getLogger(OpenNLP.class.getCanonicalName());	

	private String personModelFile = "./opennlp/en-ner-person.bin";
	private String sentenceModelFile = "./opennlp/en-sent.bin";
	private String tokenModelFile = "./opennlp/en-token.bin";
	private SentenceDetectorME sentenceDetector;
	private Tokenizer tokenizer;
	private NameFinderME nameFinder;

	private String textField = "text";
	private String peopleField = "people";
	private String sep = " ";
	
	@Override
	public void startStage(StageConfiguration config) {
		try {
			// Sentence finder
			SentenceModel sentModel = new SentenceModel(new FileInputStream(sentenceModelFile));
			sentenceDetector = new SentenceDetectorME(sentModel);
			// tokenizer
			TokenizerModel tokenModel = new TokenizerModel(new FileInputStream(tokenModelFile));
			tokenizer = new TokenizerME(tokenModel);
			// person name finder
			TokenNameFinderModel nameModel = new TokenNameFinderModel(new FileInputStream(personModelFile));
			nameFinder = new NameFinderME(nameModel);
		} catch (IOException e) {
			log.info("Error loading up OpenNLP Models. {}", e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	@Override
	public List<Document> processDocument(Document doc) {
		for (Object o :doc.getField(textField)) {
			if (o instanceof String) {
				String text = o.toString();
				if (StringUtils.isEmpty(text)) {
					// skip empty/null strings
					continue;
				}
				String sentences[] = sentenceDetector.sentDetect(text);
				for (String sentence : sentences) {
					String tokens[] = tokenizer.tokenize(sentence);
					Span[] spans = nameFinder.find(tokens);
					for (Span span : spans) {
						String[] terms = Arrays.copyOfRange(tokens, span.getStart(), span.getEnd());
						String entity = StringUtils.join(terms, sep);
						doc.addToField(peopleField, entity);
					}
				}
			} else {
				log.info("Only Strings will be processed not %s", o.getClass());
			}
		}
		return null;
	}

	@Override
	public void stopStage() {
		// TODO: close/shutdown the models!
	}

	@Override
	public void flush() {
		// no op , i believe.
		return;

	}

	public String getPersonModelFile() {
		return personModelFile;
	}

	public void setPersonModelFile(String personModelFile) {
		this.personModelFile = personModelFile;
	}

	public String getSentenceModelFile() {
		return sentenceModelFile;
	}

	public void setSentenceModelFile(String sentenceModelFile) {
		this.sentenceModelFile = sentenceModelFile;
	}

	public String getTokenModelFile() {
		return tokenModelFile;
	}

	public void setTokenModelFile(String tokenModelFile) {
		this.tokenModelFile = tokenModelFile;
	}

	public String getTextField() {
		return textField;
	}

	public void setTextField(String textField) {
		this.textField = textField;
	}

	public String getPeopleField() {
		return peopleField;
	}

	public void setPeopleField(String peopleField) {
		this.peopleField = peopleField;
	}

}

package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;

/**
 * A service that will either strip out html from input text or
 * wrap the input text in html tags.
 * 
 * @author kwatters
 *
 */
public class HtmlFilter extends Service implements TextListener, TextPublisher  {

	private static final long serialVersionUID = 1L;
	// true will strip html, false will add html
	private boolean stripHtml = true;
	// if stripHtml is false these tags are used to wrap the input text
	private String preHtmlTag = "<pre>";
	private String postHtmlTag = "</pre>";
	
	public HtmlFilter(String reservedKey) {
		super(reservedKey);
	}
	
	@Override
	public String getDescription() {
		return "This service will strip html markup from the input text.";
	}
	
	@Override
	public void onText(String text) {
		// process the text and then publish the new text.
		if (stripHtml) { 
			String cleanText = stripHtml(text);
			invoke("publishText", cleanText);
		} else {
			String htmlText = addHtml(text);
			invoke("publishText", htmlText);
		}
	}
	
	public String publishText(String text) {
		return text;
	}

	public void addTextListener(TextListener service){
		addListener("publishText", service.getName(), "onText", String.class);
	}
	
	// helper function to add html tags
	public String addHtml(String text) {
		return preHtmlTag + text + postHtmlTag;
	}
	
	// helper function to strip html tags.
	public String stripHtml(String text) {
		// TODO: something fancier but this works for now.
		String cleanText = text.replaceAll("\\<.*?\\>", " ");
		cleanText = cleanText.replaceAll("  ", " ");
		return cleanText.trim();
	}
	
	public boolean isStripHtml() {		
		return stripHtml;
	}

	/**
	 * If this is true, the input text will be striped of html.
	 * If this is false, the input text will get the pre and post html tags added to it.
	 * @param stripHtml
	 */
	public void setStripHtml(boolean stripHtml) {
		this.stripHtml = stripHtml;
	}

	public String getPreHtmlTag() {
		return preHtmlTag;
	}

	/**
	 * The string to be prepended to the input text
	 * Defaults to &lt;pre&gt;
	 * 
	 * @param preHtmlTag
	 */
	public void setPreHtmlTag(String preHtmlTag) {
		this.preHtmlTag = preHtmlTag;
	}

	public String getPostHtmlTag() {
		return postHtmlTag;
	}

	/**
	 * The string to be appended to the input text
	 * Defaults to &lt;/pre&gt;
	 * 
	 * @param postHtmlTag
	 */
	public void setPostHtmlTag(String postHtmlTag) {
		this.postHtmlTag = postHtmlTag;
	}
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel("INFO");
		Runtime.createAndStart("gui", "GUIService");
		Runtime.createAndStart("python", "Python");
		HtmlFilter htmlFilter = (HtmlFilter) Runtime.createAndStart("htmlFilter", "HtmlFilter");
		System.out.println(">>>>>>>>>>" + htmlFilter.stripHtml("This is <a>foo</a> bar."));
	}

}

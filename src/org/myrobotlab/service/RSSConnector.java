package org.myrobotlab.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.myrobotlab.document.Document;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.interfaces.DocumentConnector;
import org.myrobotlab.service.interfaces.DocumentListener;
import org.myrobotlab.service.interfaces.DocumentPublisher;

import it.sauronsoftware.feed4j.FeedIOException;
import it.sauronsoftware.feed4j.FeedParser;
import it.sauronsoftware.feed4j.FeedXMLParseException;
import it.sauronsoftware.feed4j.UnsupportedFeedException;
import it.sauronsoftware.feed4j.bean.Feed;
import it.sauronsoftware.feed4j.bean.FeedHeader;
import it.sauronsoftware.feed4j.bean.FeedItem;

public class RSSConnector extends Service implements DocumentPublisher, DocumentConnector {

	private String rssUrl = "http://www.myrobotlab.org/rss.xml";
	
	public RSSConnector(String reservedKey) {
		super(reservedKey);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String[] getCategories() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "This will crawl an rss feed at the given url and break apart the page into Documents";
	}
	
	@Override
	public Document publishDocument(Document doc) {
		// TODO Auto-generated method stub
		return doc;
	}

	@Override
	public void startCrawling() {
		// TODO : make this cooler. :) for now.. fire and forget

		URL url;
		try {
			url = new URL(rssUrl);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		Feed feed;
		try {
			feed = FeedParser.parse(url);
		} catch (FeedIOException | FeedXMLParseException | UnsupportedFeedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		
		
		System.out.println("** HEADER **");
		FeedHeader header = feed.getHeader();
//		System.out.println("Title: " + header.getTitle());
//		System.out.println("Link: " + header.getLink());
//		System.out.println("Description: " + header.getDescription());
//		System.out.println("Language: " + header.getLanguage());
//		System.out.println("PubDate: " + header.getPubDate());
		
		System.out.println("** ITEMS **");
		int items = feed.getItemCount();
		
		for (int i = 0; i < items; i++) {
			FeedItem item = feed.getItem(i);
			// create an id for this as being url # item offset
			Document feedItem = new Document(url + "#"+ i);
			feedItem.setField("rss_title", header.getTitle());
			feedItem.setField("rss_link", header.getLink());
			feedItem.setField("rss_description", header.getDescription());
			feedItem.setField("rss_language", header.getLanguage());
			feedItem.setField("rss_date", header.getPubDate());
			feedItem.setField("title", item.getTitle());
			feedItem.setField("link", item.getLink());
			feedItem.setField("description", item.getDescriptionAsText());
			feedItem.setField("date", item.getPubDate());
			feedItem.setField("html", item.getDescriptionAsHTML());
			//System.out.println("Title: " + item.getTitle());
			//System.out.println("Link: " + item.getLink());
			//System.out.println("Plain text description: " + item.getDescriptionAsText());
			//System.out.println("HTML description: " + item.getDescriptionAsHTML());
			//System.out.println("PubDate: " + item.getPubDate());
			invoke("publishDocument", feedItem);
			
		}
		
	}

	public String getRssUrl() {
		return rssUrl;
	}

	public void setUrl(String rssUrl) {
		this.rssUrl = rssUrl;
	}

	
	public static void main(String[] args) throws Exception {
		
		RSSConnector connector = (RSSConnector)Runtime.start("rss", "RSSConnector");
		connector.startCrawling();
		
	}

	@Override
	public void addDocumentListener(DocumentListener listener) {
		// TODO Auto-generated method stub
		
		this.addListener("publishDocument", listener.getName(), "onDocument");
		
	}
}

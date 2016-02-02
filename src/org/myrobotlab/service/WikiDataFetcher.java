package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;
import org.wikidata.wdtk.datamodel.interfaces.SomeValueSnak;
import org.wikidata.wdtk.datamodel.interfaces.DataObjectFactory;
import org.wikidata.wdtk.datamodel.helpers.ToString;
import org.wikidata.wdtk.datamodel.json.jackson.*;
import org.wikidata.wdtk.datamodel.json.jackson.JacksonItemDocument;
import org.wikidata.wdtk.datamodel.json.jackson.datavalues.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class WikiDataFetcher extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(WikiDataFetcher.class);
	
	String language = "en";
	String website = "enwiki";
	

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			WikiDataFetcher wdf = (WikiDataFetcher) Runtime.start("wikiDataFetcher", "WikiDataFetcher");
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public WikiDataFetcher(String n) {
		super(n);
	}


	@Override
	public String[] getCategories() {
		return new String[] { "general" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}
	
	public void setLanguage(String lang){
		language = lang;
	}
	
	public void setWebSite(String site){
		website = site;
	}
	
	private EntityDocument getWiki(String query) throws MediaWikiApiErrorException{
		WikibaseDataFetcher wbdf =  WikibaseDataFetcher.getWikidataDataFetcher();
		EntityDocument wiki = wbdf.getEntityDocumentByTitle(website,query);
		if (wiki == null) {
			System.out.println("ERROR ! Can't get the document : " + query);
		 	} 	
		return wiki;
	}
	
	private EntityDocument getWikiById(String query) throws MediaWikiApiErrorException{
		WikibaseDataFetcher wbdf =  WikibaseDataFetcher.getWikidataDataFetcher();
		EntityDocument wiki = wbdf.getEntityDocument(query);
		if (wiki == null) {
			System.out.println("ERROR ! Can't get the document : " + query);
		 	} 	
		return wiki;
	}
	
	
	public String getDescription(String query) throws MediaWikiApiErrorException{
		EntityDocument document = getWiki(query);
		if (document instanceof ValueSnak) {
			System.out.println("MainSnak Value : ");
		 	} 	
		
		try {
			String answer = ((ItemDocument) document).getDescriptions().get(language).getText();
			return answer;
		} 
		catch (Exception e){return  " Description not found";}
		
	}
	
	public String getLabel(String query) throws MediaWikiApiErrorException{
		EntityDocument document = getWiki(query);
		try {
			String answer =  ((ItemDocument) document).getLabels().get(language).getText();
			return answer;
		} 
		catch (Exception e){return  "Label not found";}
	}
	
	public String getId(String query) throws MediaWikiApiErrorException{
		EntityDocument document = getWiki(query);
		try {
			String answer =  document.getEntityId().getId();
			return answer;
		}
		catch (Exception e){return  "ID not found";}
	}
	
	public String getDescriptionById(String query) throws MediaWikiApiErrorException{
		EntityDocument document = getWikiById(query);
		try {
			String answer =  ((ItemDocument) document).getDescriptions().get(language).getText();
			return answer;
		}
	 catch (Exception e){return  "Description by ID  not found";}
	}
	
	public String getLabelById(String query) throws MediaWikiApiErrorException{
		EntityDocument document = getWikiById(query);
		try {
			String answer =  ((ItemDocument) document).getLabels().get(language).getText();
			return answer;
		}
		catch (Exception e){return  "Label by ID not found";}
	}
	
	public String cutStart(String sentence) throws MediaWikiApiErrorException{
		try {
			String answer =  sentence.substring(sentence.indexOf(" ")+1);
			return answer;
		}
		catch (Exception e){return  sentence;}
	}
	public String grabStart(String sentence) throws MediaWikiApiErrorException{
		try {
			String answer =  sentence.substring(0,sentence.indexOf(" "));
			return answer;
		}
		catch (Exception e){return  sentence;}
	}
	
	// TODO add other dataType
	public Value getSnak(String query, String ID) throws MediaWikiApiErrorException{
		EntityDocument document = getWiki(query);
		String dataType = "error";
		Value data = (Value)document.getEntityId();// If property is not found, return the value of document ID
		for (StatementGroup sg : ((ItemDocument) document).getStatementGroups()) {
			if (ID.equals(sg.getProperty().getId())) { // Check if this ID exist for this document
				
				for (Statement s : sg.getStatements()) {
				if (s.getClaim().getMainSnak() instanceof ValueSnak) {					
					dataType = ((JacksonValueSnak) s.getClaim().getMainSnak()).getDatatype().toString();
					 switch (dataType) {
			         	case "wikibase-item":
			         		data = ((JacksonValueSnak) s.getClaim().getMainSnak()).getValue();
			         		break;
			         	case "time":
			         		data = (TimeValue)(((JacksonValueSnak) s.getClaim().getMainSnak()).getDatavalue());
			         		break;
			         }
				} 	
				}
			}
		
		}
		return data;
	
		
	}
	
	public String getProperty(String query, String ID)throws MediaWikiApiErrorException{
		String info = (getSnak(query,ID)).toString();
		int beginIndex = info.indexOf('Q');
        int endIndex = info.indexOf("(") ;
        info = info.substring(beginIndex , endIndex-1);
		return getLabelById(info);
	}
	
	public String getTime(String query, String ID, String what)throws MediaWikiApiErrorException{
		try {
		TimeValue date =  (TimeValue)(getSnak(query,ID));	
		String data ="";
		switch (what) {
        	case "year":
        		data = String.valueOf(date.getYear());
        		break;
        	case "month":
        		data = String.valueOf(date.getMonth());
        		break;
        	case "day":
        		data = String.valueOf(date.getDay());
        		break;
        	case "hour":
        		data = String.valueOf(date.getHour());
        		break;
        	case "minute":
        		data = String.valueOf(date.getMinute());
        		break;
        	case "second":
        		data = String.valueOf(date.getSecond());
        		break;
        	case "before":
        		data = String.valueOf(date.getBeforeTolerance());
        		break;
        	case "after":
        		data = String.valueOf(date.getAfterTolerance());
        		break;
        	default:
        		data = "ERROR";
     }
		return data;
		}
		catch (Exception e){return  "Not a TimeValue !";}
	}
		
	
	
}

package org.myrobotlab.framework;

import java.util.ArrayList;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Errors extends Exception {
	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Errors.class);

	private ArrayList<Error> errors = new ArrayList<Error>();
	
	public Errors(){}
	
	public Errors(String msg){
		add(msg);
	}
	
	public void add(String msg){
		add(new Error(msg));
	}
	
	public void add(Error err){
		errors.add(err);
	}
	
	public void clear(){
		errors.clear();
	}
	
	public boolean hasErrors(){
		return (errors != null && errors.size() > 0)?true:false;
	}
	
	public static void throwError(String msg) throws Errors{
		throw new Errors(msg);
	}

	public void add(Exception e) {
		add(new Error(e));
	}

	public void log() {
		for (int i = 0; i < errors.size(); ++i) {
			log.error("error #{} {}", i, errors.get(i));
		}
	}

	public String getFirstError() {
		if (errors.size() > 0){
			return errors.get(0).getMessage();
		}
		
		return null;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < errors.size(); ++i){
			Error e = errors.get(i);
			sb.append(e.toString());
			if (i < errors.size() - 1){
				sb.append(" ");
			}
		}
		
		return sb.toString();
	}

}

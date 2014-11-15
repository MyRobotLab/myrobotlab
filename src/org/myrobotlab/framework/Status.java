package org.myrobotlab.framework;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

/**
 *  WARNING - this class used to extend Exception - but the gson serializer would stack overflow
 *  with self reference issue
 */
public class Status implements Serializable {// extends Exception {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Status.class);

	public final static String DEBUG = "debug";
	public final static String INFO = "info";
	public final static String WARN = "warn";
	public final static String ERROR = "error";

	public String name; // service name ???
	public String level;
	public String key;
	public String detail;
	
	/**
	 * list of sub status
	 */
	
	private ArrayList<Status> statuses = new ArrayList<Status>();

	// FIXME - do others
	private boolean hasError = false;

	// FIXME - do others
	private boolean allowDebug = true;

	public Status(String name, String level, String key, String detail) {
		this.name = name;
		this.level = level;
		this.key = key;
		this.detail = detail;
	}
	
	public Status(Status s){
		this.name = s.name;
		this.level  = s.level;
		this.key = s.key;
		this.detail = s.detail;
	}

	public Status(String detail) {
		this.level = ERROR;
		this.detail = detail;
	}


	public Status(Exception e) {
		this.level = ERROR;
		StringWriter sw;
		try {
		sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			detail = sw.toString();
		} catch (Exception e2) {
		}
		this.key = e.getMessage();
		
	}
	
	public void allowDebug(boolean b){
		allowDebug  = b;
	}

	public boolean isDebug() {
		return DEBUG.equals(level);
	}

	public boolean isInfo() {
		return INFO.equals(level);
	}

	public boolean isWarn() {
		return WARN.equals(level);
	}

	public boolean isError() {
		return ERROR.equals(level);
	}

	/*
	public static void throwError(String msg) throws Status {
		throw new Status(msg);
	}
	*/

	public static Status error(Exception e) {
		Status s = new Status(e);
		s.level = ERROR;
		return s;
	}
	
	
	public static Status error(String msg) {
		Status s = new Status(msg);
		s.level = ERROR;
		return s;
	}
	
	public static Status info(String msg) {
		Status s = new Status(msg);
		s.level = INFO;
		return s;
	}

	public static Status debug(String format, Object... args) {
		Status status = new Status(String.format(format, args));
		status.level = DEBUG;
		return status;
	}
	
	public static Status info(String format, Object... args) {
		Status status = new Status(String.format(format, args));
		status.level = INFO;
		return status;
	}

	public static Status error(String format, Object... args) {
		Status status = new Status(String.format(format, args));
		status.level = ERROR;
		status.hasError  = true;
		return status;
	}

	public Status addDebug(String format, Object... args){
		Status status = debug(format, args);
		add(status);
		return status;
	}
	
	public Status addInfo(String format, Object... args){
		Status status = info(format, args);
		add(status);
		return status;
	}
	
	public Status addError(String format, Object... args){
		Status status = error(format, args);
		add(status);
		return status;
	}
	
	public final static String stackToString(final Throwable e) {
		StringWriter sw;
		try {
			sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
		} catch (Exception e2) {
			return "bad stackToString";
		}
		return "------\r\n" + sw.toString() + "------\r\n";
	}
	
	public Status addError(Exception e){
		Logging.logException(e);
		Status status = error("%s %s", e.getMessage(), stackToString(e));
		add(status);
		return status;
	}
	
	public boolean hasError(){
		boolean b = false;
		for (int i = 0; i < statuses.size(); ++i){
			b |= statuses.get(i).hasError();
		}
		return b;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		if (name != null){
			sb.append(name);
			sb.append(" ");
		}
		if (level != null){
			sb.append(level);
			sb.append(" ");
		}
		if (key != null){
			sb.append(key);
			sb.append(" ");
		}
		if (detail != null){
			sb.append(detail);
		}
		
		sb.append(" ");
		
		for (int i = 0; i < statuses.size(); ++i){
			sb.append(statuses.get(i).toString());
		}
		
		return sb.toString();
	}
	
	public ArrayList<Status> flatten(){		
		ArrayList<Status> ret = new ArrayList<Status>();
		
		for (int i = 0; i < statuses.size(); ++i){
			Status status = statuses.get(i);
			ArrayList<Status> s = status.flatten();
			for (int j = 0; j < s.size(); ++j){
				ret.add(s.get(j));
			}
		}
		return ret;
	}

	public void add(Status status) {
		if (status != null){
			if (status.isDebug() && !allowDebug){
				return;
			}
			// if logging enabled
			/*
			switch(status.level){
			case DEBUG:{
			}
			
			case INFO:{
			}
			
			case WARN:{
			}
			
			case ERROR:{
			}
			
			default:{
				log.error("unkown status level {}", status);
			}
			
			}
			*/
			}
			
			statuses.add(status);
		}
	}


package org.myrobotlab.framework;

public class Error extends Exception {
	private static final long serialVersionUID = 1L;
	
	final public static String LEVEL_ERROR = "error";
	final public static String LEVEL_WARNING = "warning";
	final public static String LEVEL_INFO = "info";
	
	String level = LEVEL_ERROR;
	
	public Error(){
		super();
	}
	
	public Error(String msg){		
		super(msg);
	}
	
	public Error(Throwable throwable){
		super(throwable);
	}

	public Error(String msg, Throwable throwable){
		super(msg, throwable);
	}
	
	public String toString(){
		return String.format("%s %s", level, getMessage());
	}

}

package org.myrobotlab.pickToLight;

public class SOAPResponse {
	
	String error;
	
	public SOAPResponse () {
		
	}
	
	public boolean isError()
	{
		return error != null;
	}

	public void setError(String error) {
		this.error = error;
	}
	
	public String getError(){
		return error;
	}

}

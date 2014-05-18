package org.myrobotlab.framework.repo;

public class Updates {
	
	String repoVersion;
	String currentVersion;
	
	public boolean hasUpdate(){
		if (repoVersion != null && currentVersion != null){
			 return repoVersion.compareTo(currentVersion) > 0;
		}
		return false;
	}
	
	public boolean hasMRLUpdate(){
		
		return false;
	}
	
	public boolean hasNewServiceType(){
		return false;
		
	}
	
	public boolean hasDependencyUpdate(){
		
		return false;
	}
	
	public boolean hasNewDependency(){
		return false;
		
	}

}

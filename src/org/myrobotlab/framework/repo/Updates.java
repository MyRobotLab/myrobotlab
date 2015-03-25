package org.myrobotlab.framework.repo;

import java.util.ArrayList;
import java.util.List;

public class Updates {

	public String repoVersion;
	public String currentVersion;
	public boolean updateJar = false;
	public List<String> serviceTypesToUpdate = new ArrayList<String>();
	public ServiceData remoteServiceData;
	public ServiceData localServiceData;
	// public final String runtimeName;
	public boolean isValid = false;
	public String lastError;

	public Updates() {
	}

	// TODO IMPLEMENT :)
	public boolean hasDependencyUpdate() {

		return false;
	}

	public boolean hasJarUpdate() {
		if (repoVersion != null && currentVersion != null) {
			return repoVersion.compareTo(currentVersion) > 0;
		}
		return false;
	}

	// TODO IMPLEMENT
	public boolean hasNewDependency() {
		return false;

	}

	// TODO return list of specifics
	public boolean hasNewServiceType() {
		return false;

	}

}

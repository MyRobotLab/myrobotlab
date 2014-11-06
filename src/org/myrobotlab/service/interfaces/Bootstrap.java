package org.myrobotlab.service.interfaces;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public interface Bootstrap {

	void createBootstrapJar() throws IOException ;

	List<String> getJVMArgs();

	void spawn(List<String> args) throws IOException, URISyntaxException, InterruptedException;
	

}

package org.myrobotlab.service.interfaces;

public interface DeviceController extends NameProvider {
	
	// GroG is pondering ...
	// FIXME - evaluate String vs complex typed 
	// methods ..
	// typed is always preferred to work in context
	// but string name parameters are accessible through many protocols and interfaces
	// in Python we commonly see this .. and its a "good thing"
	// something.attach(myThing)
	// vs this - which is more likely to break
	// something.attach("nameOfMyThing")
	// however - the idea of creating a url with a device on it is "challenging" but this
	// http://localhost:8888/api/service/arduino/attach/nameOfMyThing   - is quite satisfying..
	
	public void attach(String name) throws Exception;

	public void detach(String name);


	public void attachDevice(Device device) throws Exception;

	public void detachDevice(Device device);

}

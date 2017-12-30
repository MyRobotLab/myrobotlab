package org.myrobotlab.service;

import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

/**
*This class exists as a proxy to control native MRL services connected through the web API. 
*Methods of that native service are called through this class's {@link #exec(String, Object[])} method.
*
* FIXME - although this is a great idea & implementation, something more general needs to exist in order
* to interoperate between processes - it will be coming as a network overlay & addressing system in the
* next release
*
**/
public class PythonProxy extends Service {

  private static final long serialVersionUID = 1L;

  //Subclasses override getLogger() method to supply a new logger
  protected static Logger log = getLogger();

  public static WebGui webgui;

  public boolean startNativeService = true;

  public boolean startTerminal = true;

  public int retTimeout = 2 * 1000; //Two second timeout for return values

  private boolean connected = false;

  private boolean startedNativeService = false;

  private transient LinkedBlockingQueue<Message> inputQueue = new LinkedBlockingQueue<Message>();

  private transient InputQueueThread inputQueueThread;

  private transient volatile Object retObj; //Used for storing the return value of a native method call

  private transient volatile boolean returned = false; //Used for determining whether the data in retObj is from the last method call or a previous one

  public class InputQueueThread extends Thread {
                private PythonProxy proxy;

                public InputQueueThread(PythonProxy proxy) {
                        super(String.format("%s.input", proxy.getName()));
                        this.proxy = proxy;
                }

                @Override
                public void run() {
                        try {
                                while (isRunning()) {

                                        Message msg = inputQueue.take();

                                        try {

                                                // msgHandle is really the data coming from a callback
                                                // it can originate from the same calling function such
                                                // as Sphinx.send - but we want the callback to
                                                // call a different method - this means the data needs
                                                // to go to a data structure which is keyed by only the
                                                // sending method, but must call the appropriate method
                                                // in Sphinx
                                      		String method = msg.method;
						Object[] data;

                                                if (!(msg.data == null || msg.data.length == 0)) {
                                                	data = msg.data;

                                                } else {
							data = (Object[]) null;
                                   		}
                                                proxy.exec(method, data); //We do this instead of exec(msg) because preprocessing of strings is required

                                        } catch (Exception e) {
                                                Logging.logError(e);
                                        }

                                }
                        } catch (Exception e) {
                                if (e instanceof InterruptedException) {
                                        info("shutting down %s", getName());
                                } else {
                                        Logging.logError(e);
                                }
                        }
  	    }
  }


  public PythonProxy(String n) {
    super(n);

  }

 /**
   * preProcessHook is used to intercept messages and process or route them
   * before being processed/invoked in the Service.
   * 
   * Here all messages allowed to go and effect the Python service will be let
   * through. However, all messsages not found in this filter will go "into"
   * the native service, if the proxy is connected. There they can be handled in the native service.
   * 
   * @see org.myrobotlab.framework.Service#preProcessHook(org.myrobotlab.framework.Message)
   */
  @Override
  public boolean preProcessHook(Message msg) {
        // let the messages for this service
        // get processed normally
        if (methodSet.contains(msg.method)) {
        	return true;
        }

	if (connected) {
		// handling call-back input needs to be
    		// done by another thread - in case its doing blocking
        	// or is executing long tasks - the inbox thread needs to
        	// be freed of such tasks - it has to do all the inbound routing
        	inputQueue.add(msg);
        	return false;
	} else {
		//If not connected, log error
		log.error("Proxy is not connected and can't send message to service. Check to see if native service was started.");
		return false;
	}
  }


  public static ServiceType addMetaData(ServiceType meta) {
	meta.addPeer("webgui", "WebGui", "This is used as the API entrance point");
	// meta.addDependency("com.nativeServices", "0.0.1"); what's this ?
	return meta;
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public static ServiceType getMetaData() {

    ServiceType meta = new ServiceType(PythonProxy.class.getCanonicalName());
    meta.addDescription("Provides an API hook point to services written in native Python.");
    // Is a python service script can be written to test ? and some documentation ?
    meta.setAvailable(false); // false if you do not want it viewable in a gui
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("bridge");
    return addMetaData(meta);
  }

  public void startService() {
	super.startService();
	//If webgui hasn't been set by user, try getting service called webgui
	if (webgui == null)
		webgui = (WebGui) Runtime.getService("webgui");

	//If webgui doesn't exist and no service named webgui, create peer
	if (webgui == null) {
		webgui = (WebGui) createPeer("webgui");
		webgui.autoStartBrowser = false;
		webgui.startService();
	}

	if (inputQueueThread == null) {
                inputQueueThread = new InputQueueThread(this);
        	inputQueueThread.start();
        }
	if (startNativeService) {
		//Start the native service
		startNativeService();
	}
  }

  /**
  * Execute method with params dat on the native service this class represents.
  **/
  public Object exec(String method, Object[] dat) {
	//Create message
	Message msg = new Message();
	msg.name = getName();
	msg.method = method;
	if (dat != null) {
		for (int i = 0; i < dat.length; i++) {
			if (dat[i] instanceof String) {
				dat[i] = (Object)("\"" + (String)dat[i] + "\"");
			}
		}
	}
	msg.data = dat;
	return exec(msg);
  }


  /**
  * Execute Message msg on the native service this class represents.
  **/
  public Object exec(Message msg) {
	webgui.broadcast(msg);
	boolean timeout = false;
	long past = System.currentTimeMillis();
	long now = System.currentTimeMillis();
	while (!returned && (now - past) < retTimeout) {
		sleep(20);
		now = System.currentTimeMillis();
	}
	if ((now - past) >= retTimeout)
		return (Object) null;
	else {
		returned = false;
		return retObj;
	}
 }

 public void returnData(Object obj) {
	retObj = obj;
	returned = true;
 }

 public void log(String txt) {
	log.info(txt);
 }

  /***
  *This is used to start the native service. Currently only Linux is supported
  *
  **/
  public void startNativeService() {
	String os = (String) System.getProperty("os.name");
	if (os.toLowerCase().equals("linux")) {
		try {
			if (!startedNativeService) {
				String activateLocation = "native-mrl-services/python/bin/activate";
//				if (new File(activateLocation).exists()) {
					Process p = new ProcessBuilder(
   					"xterm",
					"-e", 
					"./native-mrl-services/utils/bin/virtualenv --relocatable native-mrl-services/python && . native-mrl-services/python/bin/activate && pip install -r native-mrl-services/python/requirements.txt && python native-mrl-services/python/services/" + this.getClass().getSimpleName() + ".py " + getName()).start();
					startedNativeService = true;
   
//				} else {
					//Process p = new ProcessBuilder(
//					"lxterminal", 
//					"-e", 
//					"virtualenv python && source " + activateLocation + " && python native-mrl-services/python/services/" + this.getClass().getSimpleName() + ".py " + getName()).start();
//				}
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	} else {
		log.error("Native services are only supported on linux.");
	}
  }


  /**
  *This is called by the native service to start the handshake sequence.
  *After the handshake is completed, both this class and the native service should remain in sync
  **/
  public void handshake() {
//	if (!connected) {
		connected = true;
		exec("handshake", (Object[]) null);
//	} else {
//		log.error("Another native service is attempting connection, but already connected. Perhaps a name conflict?");
//	}
  }

  public static Logger getLogger() {
	return LoggerFactory.getLogger(PythonProxy.class);
  }

  public static void main(String[] args) {
    try {


      Runtime.start("test", "_TemplateProxy");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}

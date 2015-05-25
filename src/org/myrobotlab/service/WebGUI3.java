package org.myrobotlab.service;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.myrobotlab.framework.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.apache.hadoop.hbase.client.Admin;

public class WebGUI3 extends Service {

	private static final long serialVersionUID = 1L;
	static Logger log = LoggerFactory.getLogger(WebGUI3.class);

	public WebGUI3(String n) {
		super(n);
	}

	// FIXME - change to status 
	public static class Error {

		public String key = "unknown";
		public String msg;

		public Error(String key, String msg) {
			this.key = key;
			this.msg = msg;
		}

		public Error(Throwable e) {
			this.key = e.getMessage();
			this.msg = stackToString(e);
		}

	}
	

	public static void main(String[] args) {

		try {
			org.apache.log4j.BasicConfigurator.configure();
			// Create a basic jetty server object that will listen on port 8080.
			// Note that if you set this to port 0 then a randomly available
			// port
			// will be assigned that you can either look in the logs for the
			// port,
			// or programmatically obtain it for use in test cases.
			Server server = new Server(3333);

			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath("/");
			context.setResourceBase(".");
			server.setHandler(context);

			// Add dump servlet
//			context.addServlet(WebGUI3.class, "/api/*");
			// Add default servlet
//			context.addServlet(DefaultServlet.class, "/");

			/*
			 * // The ServletHandler is a dead simple way to create a context //
			 * handler // that is backed by an instance of a Servlet. // This
			 * handler then needs to be registered with the Server object.
			 * ServletHandler handler = new ServletHandler();
			 * handler.initialize(); server.setHandler(handler);
			 * 
			 * // Passing in the class for the Servlet allows jetty to
			 * instantiate // an // instance of that Servlet and mount it on a
			 * given context path.
			 * 
			 * // IMPORTANT: // This is a raw Servlet, not a Servlet that has
			 * been configured // through a web.xml @WebServlet annotation, or
			 * anything similar.
			 * handler.addServletWithMapping(WebGUI3.class, "/*");
			 */
			// Start things up!
			server.start();

			// The use of server.join() the will make the current thread join
			// and
			// wait until the server is done executing.
			// See
			// http://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html#join()
			server.join();

		} catch (Exception e) {

		}

	}

	@Override
	public String[] getCategories() {
		return new String[] { "display" };
	}

	@Override
	public String getDescription() {
		return "web enabled gui";
	}

}


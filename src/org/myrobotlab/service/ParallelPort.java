/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;

import java.io.IOException;
import java.io.OutputStream;

import org.myrobotlab.framework.Service;

public class ParallelPort extends Service {

	private static OutputStream outputStream;;

	private static gnu.io.ParallelPort parallelPort;
	private static CommPortIdentifier port;

	private static final long serialVersionUID = 1L;

	// CONSTANTS
	public static final String PARALLEL_PORT = "LPT1";

	public static final String[] PORT_TYPE = { "Serial Port", "Parallel Port" };

	public static void main(String[] args) {

		System.out.println("Started test....");

		try {
			// get the parallel port connected to the printer
			port = CommPortIdentifier.getPortIdentifier(PARALLEL_PORT);

			System.out.println("\nport.portType = " + port.getPortType());
			System.out.println("port type = " + PORT_TYPE[port.getPortType() - 1]);
			System.out.println("port.getName() = " + port.getName());

			// open the parallel port -- open(App name, timeout)
			parallelPort = (gnu.io.ParallelPort) port.open("CommTest", 50);
			outputStream = parallelPort.getOutputStream();

			// char[] charArray = printerCodes.toCharArray();
			byte[] byteArray = null; // TODO - fix
										// CharToByteConverter.getConverter is
										// depricated
			/*
			 * byte[] byteArray = CharToByteConverter.getConverter("UTF8")
			 * .convertAll(charArray);
			 */
			System.out.println("Write...");
			outputStream.write(byteArray);
			System.out.println("Flush...");
			outputStream.flush();
			System.out.println("Close...");
			outputStream.close();

		} catch (NoSuchPortException nspe) {
			System.out.println("\nPrinter Port LPT1 not found : " + "NoSuchPortException.\nException:\n" + nspe + "\n");
		} catch (PortInUseException piue) {
			System.out.println("\nPrinter Port LPT1 is in use : " + "PortInUseException.\nException:\n" + piue + "\n");
		}
		/*
		 * catch (UnsupportedCommOperationException usce) {
		 * System.out.println("\nPrinter Port LPT1 fail to write :
		 * UnsupportedCommException.\nException:\n" + usce + "\n"); }
		 */
		catch (IOException ioe) {
			System.out.println("\nPrinter Port LPT1 failed to write : " + "IOException.\nException:\n" + ioe + "\n");
		} catch (Exception e) {
			System.out.println("\nFailed to open Printer Port LPT1 with exeception : " + e + "\n");
		} finally {
			if (port != null && port.isCurrentlyOwned()) {
				parallelPort.close();
			}

			System.out.println("Closed all resources.\n");
		}
	}

	// these commands are specific for my printer around the text
	// private static String printerCodes = "<n>HelloWorld!<p>";

	public ParallelPort(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "sensor", "control" };
	}

	@Override
	public String getDescription() {
		return "<html>(not working yet) used to communicate to and from the parallel port<br>" + "wrapping the great project http://rxtx.qbang.org/ using LGPL v 2.1</html>";
	}

}

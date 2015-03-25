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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class RecorderPlayer extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(RecorderPlayer.class.getCanonicalName());
	public ArrayList<Message> msgs = new ArrayList<Message>();
	public HashMap<String, ArrayList<Message>> msgMap = new HashMap<String, ArrayList<Message>>();

	public final static String FORMAT_MIN = "FORMAT_MIN";
	public final static String FORMAT_TEXT = "FORMAT_TEXT";
	public final static String FORMAT_BINARY = "FORMAT_BINARY";

	String format = FORMAT_MIN;

	String targetServiceName;

	public RecorderPlayer(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "framework" };
	}

	@Override
	public String getDescription() {
		return "<html>service for recording and playing back messages (not fully implemented)</html>";
	}

	public boolean loadFile(String name) {
		return loadFromFile(name + ".msgs");
	}

	public boolean loadFromFile(String filename) {
		boolean ret = false;
		try {

			FileReader infile = new FileReader(filename);
			BufferedReader in = new BufferedReader(infile);

			String s;
			while ((s = in.readLine()) != null) {

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logException(e);
		}

		return ret;
	}

	public void play() {
		long deltaMsgTime = 0;
		targetServiceName = "tilt";

		for (int i = 0; i < msgs.size(); ++i) {
			Message m = msgs.get(i);
			m.name = targetServiceName;
			// m.historyList.clear();
			out(m);

			if (i + 1 < msgs.size()) {
				Message nextMsg = msgs.get(i + 1);
				deltaMsgTime = nextMsg.timeStamp - m.timeStamp;
			}

			try {
				Thread.sleep(deltaMsgTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logException(e);
			}
		}
	}

	@Override
	public boolean preProcessHook(Message m) {
		m.historyList.clear();
		msgs.add(m);
		return true;
	}

	public void record(String serviceName) {
		targetServiceName = serviceName;
	}

	public void saveAs(String filename) {
		try {

			// TODO - FORMAT

			/*
			 * BINARY ObjectOutputStream outputStream = new
			 * ObjectOutputStream(new FileOutputStream(filename));
			 * outputStream.writeObject(msgs);
			 */

			// TODO - condensed

			if (format.compareTo(FORMAT_TEXT) == 0) {
				FileWriter outfile = new FileWriter(filename);
				PrintWriter out = new PrintWriter(outfile);
				for (int i = 0; i < msgs.size(); ++i) {
					msgs.get(i).historyList.clear();
					out.write(msgs.get(i).toString());
					Object[] params = msgs.get(i).data;
					for (int j = 0; j < params.length; ++j) {
						out.write(params[j].toString());
					}
				}
				out.close();
				outfile.close();
			} else if (format.compareTo(FORMAT_MIN) == 0) {
				FileWriter outfile = new FileWriter(filename);
				PrintWriter out = new PrintWriter(outfile);
				for (int i = 0; i < msgs.size(); ++i) {
					msgs.get(i).historyList.clear();
					String d = "";
					Object[] params = msgs.get(i).data;
					for (int j = 0; j < params.length; ++j) {
						// out.write(params[j].toString());
						d += "|" + params[j].toString();
					}
					out.write(msgs.get(i).timeStamp + "|" + Encoder.getParameterSignature(msgs.get(i).data) + d + "\n");
				}
				out.close();
				outfile.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logException(e);
		}

	}

}

package org.myrobotlab.framework;

import java.io.Serializable;
import java.util.ArrayList;

import org.myrobotlab.service.interfaces.Invoker;

/**
 * Simple class representing an operating system process
 * 
 * @author GroG
 *
 */
public class ProcessData implements Serializable {
	private static final long serialVersionUID = 1L;

	public String branch;
	public String name;
	public String version;
	public Long startTs = null;
	public Long stopTs = null;
	public boolean isRunning = false;
	public String state = null; // running | stopped | unknown | non responsive
	public ArrayList<String> cmdLine = null;

	transient public Process process;
	transient Monitor monitor;
	transient public Invoker service;

	public boolean autoUpdate = false;

	public static class Monitor extends Thread {
		ProcessData data;

		public Monitor(ProcessData data) {
			this.data = data;
		}

		@Override
		public void run() {
			try {
				if (data.process != null) {
					data.isRunning = true;
					data.state = "running";
					data.process.waitFor();
				}
			} catch (Exception e) {
			}

			// FIXME - invoke("terminatedProcess(name))
			data.service.invoke("publishTerminated", data.name);
			data.isRunning = false;
			data.state = "stopped";
		}

	}


	public ProcessData(Invoker service, String branch, String version, String name, ArrayList<String> cmdLine, Process process) {
		this.service = service;
		this.name = name;
		this.branch = branch;
		this.version = version;
		this.process = process;
		this.cmdLine = cmdLine;
		this.startTs = System.currentTimeMillis();
		monitor = new Monitor(this);
		monitor.start();
	}

}

package org.myrobotlab.framework;

import java.io.Serializable;

import org.myrobotlab.service.interfaces.Invoker;

/**
 * Simple class representing an operating system process
 * 
 * @author GroG
 *
 */
public class ProcessData implements Serializable {

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
					data.process.waitFor();
				}
			} catch (Exception e) {
			}

			// FIXME - invoke("terminatedProcess(name))
			data.service.invoke("publishTerminated", data.name);
			data.isRunning = false;
		}

	}

	private static final long serialVersionUID = 1L;
	public String name;
	transient public Process process;
	public boolean isRunning = false;
	transient Monitor monitor;
	transient public Invoker service;

	public ProcessData(Invoker service, String name, Process process) {
		this.service = service;
		this.name = name;
		this.process = process;
		monitor = new Monitor(this);
		monitor.start();
	}

}

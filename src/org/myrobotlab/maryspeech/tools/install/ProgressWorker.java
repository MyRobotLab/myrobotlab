/**
 * Copyright 2009 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

/*
 * ProgressWorker.java
 *
 * Created on 21. September 2009, 15:03
 */

package org.myrobotlab.maryspeech.tools.install;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.myrobotlab.maryspeech.tools.install.ComponentDescription.Status;

/**
 *
 * @author marc modified by LunDev
 */
public class ProgressWorker implements Runnable, Observer {
	private List<ComponentDescription> allComponents;
	private ComponentDescription currentComponent = null;
	private boolean install;

	/**
	 * Creates new form ProgressPanel
	 * 
	 * @param componentsToProcess
	 *            componentsToProcess
	 * @param install
	 *            install
	 */
	public ProgressWorker(List<ComponentDescription> componentsToProcess, boolean install) {
		this.allComponents = componentsToProcess;
		this.install = install;
	}

	private void setCurrentComponent(ComponentDescription desc) {
		if (currentComponent != null) {
			currentComponent.deleteObserver(this);
		}
		currentComponent = desc;
		if (currentComponent != null) {
			currentComponent.addObserver(this);
		}
		verifyCurrentComponentDisplay();
	}

	public void run() {
		boolean error = false;
		ComponentDescription problematic = null;
		int i = 0;
		int max = allComponents.size();
		System.out.println("Installing " + max + " components");
		String action = install ? "install" : "uninstall";
		for (ComponentDescription comp : allComponents) {
			System.out.println("Now " + action + "ing " + comp.getName() + "...");
			System.out.println("Overall Progress: " + i + " / " + max);
			setCurrentComponent(comp);
			if (install) {
				ComponentDescription orig = null;
				if (comp.getStatus() == Status.INSTALLED) { // Installing an installed component really means replacing it with
															// its updated version
					assert comp.isUpdateAvailable();
					// 1. uninstall current version; 2. install replacement
					comp.uninstall();
					if (comp.getStatus() == Status.ERROR) {
						error = true;
					} else {
						if (comp.isUpdateAvailable()) {
							comp.replaceWithUpdate();
						}
					}
					// And from here on, treat comp like any other component to install
				}
				if (!error && comp.getStatus() == Status.AVAILABLE || comp.getStatus() == Status.CANCELLED) {
					comp.download(true);
					if (comp.getStatus() == Status.ERROR) {
						error = true;
					}
				}
				if (!error && comp.getStatus() == Status.DOWNLOADED) {
					try {
						comp.install(true);
					} catch (Exception e) {
						e.printStackTrace();
						error = true;
					}
					if (comp.getStatus() == Status.ERROR) {
						error = true;
					}
				}
			} else { // uninstall
				if (comp.getStatus() == Status.INSTALLED) {
					comp.uninstall();
					if (comp.getStatus() == Status.ERROR) {
						error = true;
					} else {
						if (comp.isUpdateAvailable()) {
							comp.replaceWithUpdate();
						}
					}
				}
			}
			if (error) {
				problematic = comp;
				System.err.println("Could not " + action + " " + comp.getName());
				break;
			}
			i++;
		}
		if (error) {
			assert problematic != null;
//			JOptionPane.showMessageDialog(null, "Could not " + action + " " + problematic.getName());
                        System.out.println("ERROR - Could not " + action + " " + problematic.getName());
		} else {
			System.out.println("Overall Progress: " + max + " / " + max);
			// JOptionPane.showMessageDialog(this, max + " components "+action+"ed successfully.");
		}
		this.setCurrentComponent(null);
	}

	public void update(Observable o, Object arg) {
		if (o != currentComponent) {
			throw new IllegalStateException("We are observing " + o + " but the currentComponent is " + currentComponent);
		}
		verifyCurrentComponentDisplay();
	}

	private void verifyCurrentComponentDisplay() {
		if (currentComponent == null)
			return;
                System.out.println("working on component " + currentComponent.getName());
                System.out.println("component status" + currentComponent.getStatus().toString());
		int progress = currentComponent.getProgress();
		if (progress < 0) {
		} else {
                    System.out.println("Component Progress: " + progress);
		}

	}
}

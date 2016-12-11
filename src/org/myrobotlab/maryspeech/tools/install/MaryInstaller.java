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

package org.myrobotlab.maryspeech.tools.install;

import java.awt.HeadlessException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.myrobotlab.maryspeech.Version;
import org.myrobotlab.maryspeech.tools.install.ComponentDescription.Status;

/**
 *
 * @author marc modified by LunDev
 */
public class MaryInstaller {
	private Map<String, LanguageComponentDescription> languages;
	private Map<String, VoiceComponentDescription> voices;
	private String version = Version.specificationVersion();

	private String componentListURL;

	/** Creates new form InstallerGUI */
	public MaryInstaller() {
		this(null);
	}

	/**
	 * Creates new installer gui and fills it with content from the given URL.
	 * 
	 * @param maryComponentURL
	 *            maryComponentURL
	 */
	public MaryInstaller(String maryComponentURL) {
		this.languages = new TreeMap<String, LanguageComponentDescription>();
		this.voices = new TreeMap<String, VoiceComponentDescription>();
		initComponents();
		
		File archiveDir = new File(System.getProperty("mary.downloadDir"));
		File infoDir = new File(System.getProperty("mary.installedDir"));

		File[] componentDescriptionFiles = infoDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}
		});
		for (File cd : componentDescriptionFiles) {
			try {
				addLanguagesAndVoices(new InstallFileParser(cd.toURI().toURL()));
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}
		componentDescriptionFiles = archiveDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}
		});
		for (File cd : componentDescriptionFiles) {
			try {
				addLanguagesAndVoices(new InstallFileParser(cd.toURI().toURL()));
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}

		setAndUpdateFromMaryComponentURL(maryComponentURL);
	}

	public void setAndUpdateFromMaryComponentURL(String maryComponentURL) {
		try {
			URL url = new URL(maryComponentURL);
			// if this doesn't fail then it's OK, we can set it
			componentListURL = maryComponentURL;
			updateFromMaryComponentURL();
		} catch (MalformedURLException e) {
			// ignore, treat as unset value
		}
	}

	public void addLanguagesAndVoices(InstallFileParser p) {
		for (LanguageComponentDescription desc : p.getLanguageDescriptions()) {
			if (languages.containsKey(desc.getName())) {
				LanguageComponentDescription existing = languages.get(desc.getName());
				// Check if one is an update of the other
				if (existing.getStatus() == Status.INSTALLED) {
					if (desc.isUpdateOf(existing)) {
						existing.setAvailableUpdate(desc);
					}
				} else if (desc.getStatus() == Status.INSTALLED) {
					languages.put(desc.getName(), desc);
					if (existing.isUpdateOf(desc)) {
						desc.setAvailableUpdate(existing);
					}
				} else { // both not installed: show only higher version number
					if (ComponentDescription.isVersionNewerThan(desc.getVersion(), existing.getVersion())) {
						languages.put(desc.getName(), desc);
					} // else leave existing as is
				}
			} else { // no such entry yet
				languages.put(desc.getName(), desc);
			}
		}
		for (VoiceComponentDescription desc : p.getVoiceDescriptions()) {
			if (voices.containsKey(desc.getName())) {
				VoiceComponentDescription existing = voices.get(desc.getName());
				// Check if one is an update of the other
				if (existing.getStatus() == Status.INSTALLED) {
					if (desc.isUpdateOf(existing)) {
						existing.setAvailableUpdate(desc);
					}
				} else if (desc.getStatus() == Status.INSTALLED) {
					voices.put(desc.getName(), desc);
					if (existing.isUpdateOf(desc)) {
						desc.setAvailableUpdate(existing);
					}
				} else { // both not installed: show only higher version number
					if (ComponentDescription.isVersionNewerThan(desc.getVersion(), existing.getVersion())) {
						voices.put(desc.getName(), desc);
					} // else leave existing as is
				}
			} else { // no such entry yet
				voices.put(desc.getName(), desc);
			}
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// ">//GEN-BEGIN:initComponents
	private void initComponents() {
		// hack so that SVN checkout from "trunk" will look for "latest"
		// directory on server:
		if (version.equals("trunk")) {
			version = "latest";
		}
	}// </editor-fold>//GEN-END:initComponents

	private void updateFromMaryComponentURL() throws HeadlessException {
		String urlString = componentListURL.trim().replaceAll(" ", "%20");
		try {
			URL url = new URL(urlString);
			InstallFileParser p = new InstallFileParser(url);
			addLanguagesAndVoices(p);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			pw.close();
			String message = sw.toString();
			System.out.println("could not get component list:\n" + message);
			// JOptionPane.showMessageDialog(null, "Problem retrieving component
			// list:\n" + message);
		}
	}

	private HashSet<ComponentDescription> getAllInstalledComponents() {
		HashSet<ComponentDescription> components = new HashSet<ComponentDescription>();
		for (ComponentDescription component : languages.values()) {
			if (component.getStatus().equals(Status.INSTALLED)) {
				components.add(component);
			}
		}
		for (ComponentDescription component : voices.values()) {
			if (component.getStatus().equals(Status.INSTALLED)) {
				components.add(component);
			}
		}
		return components;
	}

	private List<VoiceComponentDescription> getVoicesForLanguage(LanguageComponentDescription language) {
		List<VoiceComponentDescription> lVoices = new ArrayList<VoiceComponentDescription>();
		for (String vName : voices.keySet()) {
			VoiceComponentDescription v = voices.get(vName);
			if (v.getDependsLanguage().equals(language.getName())) {
				lVoices.add(v);
			}
		}
		return lVoices;
	}

	private List<ComponentDescription> getComponentsSelectedForInstallation() {
		List<ComponentDescription> toInstall = new ArrayList<ComponentDescription>();
		for (String langName : languages.keySet()) {
			LanguageComponentDescription lang = languages.get(langName);
			// if (lang.isSelected() && (lang.getStatus() != Status.INSTALLED ||
			// lang.isUpdateAvailable())) {
			// toInstall.add(lang);
			// System.out.println(lang.getName() + " selected for
			// installation");
			// }
			// Show voices with corresponding language:
			List<VoiceComponentDescription> lVoices = getVoicesForLanguage(lang);
			for (VoiceComponentDescription voice : lVoices) {
				// if (voice.isSelected() && (voice.getStatus() !=
				// Status.INSTALLED || voice.isUpdateAvailable())) {
				// toInstall.add(voice);
				// System.out.println(voice.getName() + " selected for
				// installation");
				// }
			}
		}
		return toInstall;
	}

	public void installSelectedLanguagesAndVoicesPre() {
		installSelectedLanguagesAndVoices(getComponentsSelectedForInstallation());
	}

	public void installSelectedLanguagesAndVoices(List<ComponentDescription> toInstall) {
		long downloadSize = 0;
		// List<ComponentDescription> toInstall =
		// getComponentsSelectedForInstallation();
		if (toInstall.size() == 0) {
			// JOptionPane.showMessageDialog(null, "You have not selected any
			// installable components");
			System.out.println("no components selected to install");
			return;
		}
		// Verify if all dependencies are met
		// There are the following ways of meeting a dependency:
		// - the component with the right name and version number is already
		// installed;
		// - the component with the right name and version number is selected
		// for installation;
		// - an update of the component with the right version number is
		// selected for installation.
		Map<String, String> unmetDependencies = new TreeMap<String, String>(); // map
																				// name
																				// to
																				// problem
																				// description
		for (ComponentDescription cd : toInstall) {
			if (cd instanceof VoiceComponentDescription) {
				// Currently have dependencies only for voice components
				VoiceComponentDescription vcd = (VoiceComponentDescription) cd;
				String depLang = vcd.getDependsLanguage();
				String depVersion = vcd.getDependsVersion();
				// Two options for fulfilling the dependency: either it is
				// already installed, or it is in toInstall
				LanguageComponentDescription lcd = languages.get(depLang);
				if (lcd == null) {
					unmetDependencies.put(depLang, "-- no such language component");
				} else if (lcd.getStatus() == Status.INSTALLED) {
					if (ComponentDescription.isVersionNewerThan(depVersion, lcd.getVersion())) {
						ComponentDescription update = lcd.getAvailableUpdate();
						if (update == null) {
							unmetDependencies.put(depLang,
									"version " + depVersion + " is required by " + vcd.getName()
											+ ",\nbut older version " + lcd.getVersion()
											+ " is installed and no update is available");
						} else if (ComponentDescription.isVersionNewerThan(depVersion, update.getVersion())) {
							unmetDependencies.put(depLang, "version " + depVersion + " is required by " + vcd.getName()
									+ ",\nbut only version " + update.getVersion() + " is available as an update");
						} else if (!toInstall.contains(lcd)) {
							unmetDependencies.put(depLang,
									"version " + depVersion + " is required by " + vcd.getName()
											+ ",\nbut older version " + lcd.getVersion()
											+ " is installed\nand update to version " + update.getVersion()
											+ " is not selected for installation");
						}
					}
				} else if (!toInstall.contains(lcd)) {
					if (ComponentDescription.isVersionNewerThan(depVersion, lcd.getVersion())) {
						unmetDependencies.put(depLang, "version " + depVersion + " is required by " + vcd.getName()
								+ ",\nbut only older version " + lcd.getVersion() + " is available");
					} else {
						unmetDependencies.put(depLang,
								"is required  by " + vcd.getName() + "\nbut is not selected for installation");
					}
				}
			}
		}
		// Any unmet dependencies?
		if (unmetDependencies.size() > 0) {
			StringBuilder buf = new StringBuilder();
			for (String compName : unmetDependencies.keySet()) {
				buf.append("Component ").append(compName).append(" ").append(unmetDependencies.get(compName))
						.append("\n");
			}
			System.out.println("WARNING - dependency problem");
			// JOptionPane.showMessageDialog(null, buf.toString(), "Dependency
			// problem", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// for (ComponentDescription cd : toInstall) {
		// if (cd.getStatus() == Status.AVAILABLE) {
		// downloadSize += cd.getPackageSize();
		// } else if (cd.getStatus() == Status.INSTALLED &&
		// cd.isUpdateAvailable()) {
		// if (cd.getAvailableUpdate().getStatus() == Status.AVAILABLE) {
		// downloadSize += cd.getAvailableUpdate().getPackageSize();
		// }
		// }
		// }
		System.out.println("Accepting download size ...");
		// int returnValue = 0;
		// if (returnValue != JOptionPane.YES_OPTION) {
		// System.err.println("Aborting installation.");
		// return;
		// }
		System.out.println("Accepting licenses ...");
		// System.out.println("Check license(s)");
		// boolean accepted = showLicenses(toInstall);
		// if (accepted) {
		System.out.println("Starting installation");
		showProgressPanel(toInstall, true);
		// }
	}

	/**
	 * Show the licenses for the components in toInstall
	 * 
	 * @param toInstall
	 *            the components to install
	 * @return true if all licenses were accepted, false otherwise
	 */
	private boolean showLicenses(List<ComponentDescription> toInstall) {
		Map<URL, SortedSet<ComponentDescription>> licenseGroups = new HashMap<URL, SortedSet<ComponentDescription>>();
		// Group components by their license:
		for (ComponentDescription cd : toInstall) {
			URL licenseURL = cd.getLicenseURL(); // may be null
			// null is an acceptable key for HashMaps, so it's OK.
			SortedSet<ComponentDescription> compsUnderLicense = licenseGroups.get(licenseURL);
			if (compsUnderLicense == null) {
				compsUnderLicense = new TreeSet<ComponentDescription>();
				licenseGroups.put(licenseURL, compsUnderLicense);
			}
			assert compsUnderLicense != null;
			compsUnderLicense.add(cd);
		}
		// Now show license for each group
		for (URL licenseURL : licenseGroups.keySet()) {
			if (licenseURL == null) {
				continue;
			}
			URL localURL = LicenseRegistry.getLicense(licenseURL);
			SortedSet<ComponentDescription> comps = licenseGroups.get(licenseURL);
			System.out.println("Showing license " + licenseURL + " for " + comps.size() + " components");

			String optionPaneValue = "Accept";

			if (!"Accept".equals(optionPaneValue)) {
				System.out.println("License not accepted. Installation of component cannot proceed.");
				return false;
			}
			System.out.println("License accepted.");
		}
		return true;
	}

	private List<ComponentDescription> getComponentsSelectedForUninstall() {
		List<ComponentDescription> toUninstall = new ArrayList<ComponentDescription>();
		for (String langName : languages.keySet()) {
			LanguageComponentDescription lang = languages.get(langName);
			// if (lang.isSelected() && lang.getStatus() == Status.INSTALLED) {
			// toUninstall.add(lang);
			// System.out.println(lang.getName() + " selected for uninstall");
			// }
			// Show voices with corresponding language:
			List<VoiceComponentDescription> lVoices = getVoicesForLanguage(lang);
			for (VoiceComponentDescription voice : lVoices) {
				// if (voice.isSelected() && voice.getStatus() ==
				// Status.INSTALLED) {
				// toUninstall.add(voice);
				// System.out.println(voice.getName() + " selected for
				// uninstall");
				// }
			}
		}
		findAndStoreSharedFiles(toUninstall);
		return toUninstall;
	}

	/**
	 * For all components to be uninstalled, find any shared files required by
	 * components that will <i>not</i> be uninstalled, and store them in the
	 * component (using {@link ComponentDescription#setSharedFiles(List)}).
	 * {@link ComponentDescription#uninstall()} can then check and refrain from
	 * removing those shared files.
	 * 
	 * @param uninstallComponents
	 *            selected for uninstallation
	 */
	private void findAndStoreSharedFiles(List<ComponentDescription> uninstallComponents) {
		// first, find out which components are *not* selected for removal:
		Set<ComponentDescription> retainComponents = getAllInstalledComponents();
		retainComponents.removeAll(uninstallComponents);

		// if all components are selected for removal, there is nothing to do
		// here:
		if (retainComponents.isEmpty()) {
			return;
		}

		// otherwise, list all unique files required by retained components:
		Set<String> retainFiles = new TreeSet<String>();
		for (ComponentDescription retainComponent : retainComponents) {
			retainFiles.addAll(retainComponent.getInstalledFileNames());
		}

		// finally, store shared files in components to be removed (queried
		// later):
		for (ComponentDescription uninstallComponent : uninstallComponents) {
			Set<String> sharedFiles = new HashSet<String>(uninstallComponent.getInstalledFileNames());
			sharedFiles.retainAll(retainFiles);
			if (!sharedFiles.isEmpty()) {
				uninstallComponent.setSharedFiles(sharedFiles);
			}
		}
	}

	public void uninstallSelectedLanguagesAndVoicesPre() {
		installSelectedLanguagesAndVoices(getComponentsSelectedForUninstall());
	}

	public void uninstallSelectedLanguagesAndVoices(List<ComponentDescription> toUninstall) {
		if (toUninstall.size() == 0) {
			// JOptionPane.showMessageDialog(null, "You have not selected any
			// uninstallable components");
			System.out.println("no components selected to unistall");
			return;
		}
		System.out.println("Accepting uninstall ...");
		// int returnValue = JOptionPane.showConfirmDialog(null, "Uninstall " +
		// toUninstall.size() + " components?\n",
		// "Proceed with uninstall?", JOptionPane.YES_NO_OPTION);
		// if (returnValue != JOptionPane.YES_OPTION) {
		// System.err.println("Aborting uninstall.");
		// return;
		// }
		System.out.println("Starting uninstall");
		showProgressPanel(toUninstall, false);

	}

	private void showProgressPanel(List<ComponentDescription> comps, boolean install) {
		final ProgressWorker pp = new ProgressWorker(comps, install);
		Thread t = new Thread(pp);
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			System.err.println("Wait interrupted!");
		}
	}
	
	public Map<String, LanguageComponentDescription> getLanguages() {
		return languages;
	}
	
	public Map<String, VoiceComponentDescription> getVoices() {
		return voices;
	}
}

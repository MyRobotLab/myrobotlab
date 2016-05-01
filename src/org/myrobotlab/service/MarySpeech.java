package org.myrobotlab.service;

import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.sound.sampled.AudioInputStream;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.apache.commons.codec.digest.DigestUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.SynthesisException;
import marytts.tools.install.ComponentDescription;
import marytts.tools.install.ComponentDescription.Status;
import marytts.tools.install.InstallFileParser;
import marytts.tools.install.LanguageComponentDescription;
import marytts.tools.install.LicenseRegistry;
import marytts.tools.install.ProgressPanel;
import marytts.tools.install.VoiceComponentDescription;
import marytts.util.MaryUtils;
import marytts.util.data.audio.AudioPlayer;

public class MarySpeech extends Service implements TextListener, SpeechSynthesis {

    public final static Logger log = LoggerFactory.getLogger(MarySpeech.class);
    private static final long serialVersionUID = 1L;

    transient MaryInterface marytts = null;

    String INSTALLFILEURL = "https://raw.github.com/marytts/marytts/master/download/marytts-components.xml";
    private List<LanguageComponentDescription> possibleLanguages;
    private List<VoiceComponentDescription> possibleVoices;

    String installationstate = "noinstallationstarted";
    Object installationstateparam1;
    Object installationstateparam2;
    transient List<ComponentDescription> installation_toInstall;


    // we need to subclass the audio player class here, so we know when the run method exits and we can invoke
    // publish end speaking from it.
    private class MRLAudioPlayer extends AudioPlayer {

        private final String utterance;

        public MRLAudioPlayer(AudioInputStream ais, String utterance) {
            super(ais);
            this.utterance = utterance;
        }

        @Override
        public void run() {
            invoke("publishStartSpeaking", utterance);
            // give a small pause for sphinx to stop listening?
            try {
                Thread.sleep(100);
                log.info("Ok.. here we go.");
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            super.run();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            invoke("publishEndSpeaking", utterance);
        }
    }

    @Override
    public boolean speakBlocking(String toSpeak) throws SynthesisException, InterruptedException {
        return speakInternal(toSpeak, true);
    }

    public boolean speakInternal(String toSpeak, boolean blocking) throws SynthesisException, InterruptedException {
        AudioInputStream audio;

        log.info("speakInternal Blocking {} Text: {}", blocking, toSpeak);
        if (toSpeak == null || toSpeak.length() == 0) {
            log.info("speech null or empty");
            return false;
        }
        audio = marytts.generateAudio(toSpeak);
        //invoke("publishStartSpeaking", toSpeak);

        MRLAudioPlayer player = new MRLAudioPlayer(audio, toSpeak);
        //player.setAudio(audio);
        player.start();
        // To make this blocking you can join the player thread.
        if (blocking) {
            player.join();
        }
        // TODO: if this isn't blocking, we might just return immediately, rather than
        // saying when the player has finished.
        //invoke("publishEndSpeaking", toSpeak);
        return true;

    }

    public MarySpeech(String reservedKey) {
        super(reservedKey);

        File file = new File("mary");
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File("mary\\download");
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File("mary\\installed");
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File("mary\\lib");
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File("mary\\log");
        if (!file.exists()) {
            file.mkdirs();
        }

        System.setProperty("mary.base", "mary");
        System.setProperty("mary.downloadDir", "mary\\download");
        System.setProperty("mary.installedDir", "mary\\installed");

        try {
//            updateFromComponentUrl();

            marytts = new LocalMaryInterface();
        } catch (Exception e) {
            Logging.logError(e);
        }

        // Grab the first voice that's available.  :-/  
        Set<String> voices = marytts.getAvailableVoices();
        marytts.setVoice(voices.iterator().next());

    }

    @Override
    public void onText(String text) {
        log.info("ON Text Called: {}", text);
        try {
            speak(text);
        } catch (Exception e) {
            Logging.logError(e);
        }
    }

    @Override
    public AudioData speak(String toSpeak) throws SynthesisException, InterruptedException {
    	AudioData ret = new AudioData(toSpeak);
        // TODO: handle the isSpeaking logic/state
        speakInternal(toSpeak, false);
        // FIXME - play cache track
        return ret;
    }


    @Override
    public List<String> getVoices() {
        List<String> list = new ArrayList<>(marytts.getAvailableVoices());
        return list;
    }

    @Override
    public boolean setVoice(String voice) {
        marytts.setVoice(voice);
        return true; //setVoice is void - if voice isn't available it throws an exception
    }

    @Override
    public void setLanguage(String l) {
        marytts.setLocale(Locale.forLanguageTag(l));
    }

    @Override
    public void onRequestConfirmation(String text) {
        try {
            // FIXME - not exactly language independent
            speakBlocking(String.format("did you say. %s", text));
        } catch (Exception e) {
            Logging.logError(e);
        }
    }

    @Override
    public String getLanguage() {
        return marytts.getLocale().getLanguage();
    }

    @Override
    public void setVolume(float volume) {
        // TODO Auto-generated method stub

    }

    @Override
    public float getVolume() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void interrupt() {
        // TODO Auto-generated method stub

    }

    @Override
    public String publishStartSpeaking(String utterance) {
        // TODO Auto-generated method stub
        log.info("Starting to speak: {}", utterance);
        return utterance;
    }

    @Override
    public String publishEndSpeaking(String utterance) {
        // TODO Auto-generated method stub
        log.info("End speaking: {}", utterance);
        return utterance;
    }

    @Override
    public String getVoice() {
        return marytts.getVoice();
    }

    @Override
    public String getLocalFileName(SpeechSynthesis provider, String toSpeak, String audioFileType) throws UnsupportedEncodingException {
        return provider.getClass().getSimpleName()
                + File.separator + URLEncoder.encode(provider.getVoice(), "UTF-8")
                + File.separator + DigestUtils.md5Hex(toSpeak) + "." + audioFileType;
    }

    @Override
    public void addEar(SpeechRecognizer ear) {
        // when we add the ear, we need to listen for request confirmation
        addListener("publishStartSpeaking", ear.getName(), "onStartSpeaking");
        addListener("publishEndSpeaking", ear.getName(), "onEndSpeaking");
    }

    @Override
    public List<String> getLanguages() {
        List<String> ret = new ArrayList<>();
        for (Locale locale : marytts.getAvailableLocales()) {
            ret.add(locale.getLanguage());
        }
        return ret;
    }

    public static void main(String[] args) {
        LoggingFactory.getInstance().configure();
        LoggingFactory.getInstance().setLevel(Level.DEBUG);

        try {
            Runtime.start("webgui", "WebGui");
            MarySpeech mary = (MarySpeech) Runtime.start("mary", "MarySpeech");
//            mary.setVoice("dfki-spike en_GB male unitselection general");
//            mary.speak("hello");
//            mary.speak("world");
//        mary.speakBlocking("Hello world");
//        mary.speakBlocking("I am Mary TTS and I am open source");
//        mary.speakBlocking("and I will evolve quicker than any closed source application if not in a short window of time");
//        mary.speakBlocking("then in the long term evolution of software");
//        mary.speak("Hello world");
        } catch (Exception e) {
            Logging.logError(e);
        }
    }

    public void updateFromComponentUrl(String url) throws IOException, SAXException {
//        InstallFileParser p = new InstallFileParser(new URL(INSTALLFILEURL));
        InstallFileParser p = new InstallFileParser(new URL(url));
        possibleLanguages = p.getLanguageDescriptions();
        possibleVoices = p.getVoiceDescriptions();
        broadcastState();
    }

    public void installSelectedLanguagesAndVoices(String[] toInstall_) {
        //TODO - remove last remaining parts of swing !!! (transform them)
        //a lot of code is copied and modified from marytts.tools.install.InstallerGUI
        System.out.println("toInstall" + Arrays.toString(toInstall_));

        for (VoiceComponentDescription voice : possibleVoices) {
            voice.setSelected(false);
            for (String toInstallVoice : toInstall_) {
                if (toInstallVoice.equals(voice.getName())) {
                    voice.setSelected(true);
                }
            }
        }

        long downloadSize = 0;
        List<ComponentDescription> toInstall = new ArrayList<>();
        for (VoiceComponentDescription voice : possibleVoices) {
            if (voice.isSelected() && (voice.getStatus() != ComponentDescription.Status.INSTALLED || voice.isUpdateAvailable())) {
                toInstall.add(voice);
            }
        }
        if (toInstall.isEmpty()) {
            //move to WebGui
            installationstate = "nothingselected";
            broadcastState();
            return;
        }

        //TODO - would be nice to enable this, but would require more hacking of InstallerGUI
//        // Verify if all dependencies are met
//        // There are the following ways of meeting a dependency:
//        // - the component with the right name and version number is already installed;
//        // - the component with the right name and version number is selected for installation;
//        // - an update of the component with the right version number is selected for installation.
//        Map<String, String> unmetDependencies = new TreeMap<String, String>(); // map name to problem description
//        for (ComponentDescription cd : toInstall) {
//            if (cd instanceof VoiceComponentDescription) {
//                // Currently have dependencies only for voice components
//                VoiceComponentDescription vcd = (VoiceComponentDescription) cd;
//                String depLang = vcd.getDependsLanguage();
//                String depVersion = vcd.getDependsVersion();
//                // Two options for fulfilling the dependency: either it is already installed, or it is in toInstall
//                LanguageComponentDescription lcd = languages.get(depLang);
//                if (lcd == null) {
//                    unmetDependencies.put(depLang, "-- no such language component");
//                } else if (lcd.getStatus() == ComponentDescription.Status.INSTALLED) {
//                    if (ComponentDescription.isVersionNewerThan(depVersion, lcd.getVersion())) {
//                        ComponentDescription update = lcd.getAvailableUpdate();
//                        if (update == null) {
//                            unmetDependencies.put(depLang, "version " + depVersion + " is required by " + vcd.getName()
//                                    + ",\nbut older version " + lcd.getVersion() + " is installed and no update is available");
//                        } else if (ComponentDescription.isVersionNewerThan(depVersion, update.getVersion())) {
//                            unmetDependencies.put(depLang, "version " + depVersion + " is required by " + vcd.getName()
//                                    + ",\nbut only version " + update.getVersion() + " is available as an update");
//                        } else if (!toInstall.contains(lcd)) {
//                            unmetDependencies.put(depLang, "version " + depVersion + " is required by " + vcd.getName()
//                                    + ",\nbut older version " + lcd.getVersion() + " is installed\nand update to version "
//                                    + update.getVersion() + " is not selected for installation");
//                        }
//                    }
//                } else if (!toInstall.contains(lcd)) {
//                    if (ComponentDescription.isVersionNewerThan(depVersion, lcd.getVersion())) {
//                        unmetDependencies.put(depLang, "version " + depVersion + " is required by " + vcd.getName()
//                                + ",\nbut only older version " + lcd.getVersion() + " is available");
//                    } else {
//                        unmetDependencies.put(depLang, "is required  by " + vcd.getName()
//                                + "\nbut is not selected for installation");
//                    }
//                }
//            }
//        }
//        // Any unmet dependencies?
//        if (unmetDependencies.size() > 0) {
//            StringBuilder buf = new StringBuilder();
//            for (String compName : unmetDependencies.keySet()) {
//                buf.append("Component ").append(compName).append(" ").append(unmetDependencies.get(compName)).append("\n");
//            }
//            JOptionPane.showMessageDialog(this, buf.toString(), "Dependency problem", JOptionPane.WARNING_MESSAGE);
//            return;
//        }
//
        for (ComponentDescription cd : toInstall) {
            if (cd.getStatus() == ComponentDescription.Status.AVAILABLE) {
                downloadSize += cd.getPackageSize();
            } else if (cd.getStatus() == ComponentDescription.Status.INSTALLED && cd.isUpdateAvailable()) {
                if (cd.getAvailableUpdate().getStatus() == ComponentDescription.Status.AVAILABLE) {
                    downloadSize += cd.getAvailableUpdate().getPackageSize();
                }
            }
        }

        installation_toInstall = toInstall;

        installationstate = "installcomponents";
        installationstateparam1 = toInstall.size() + "";
        installationstateparam2 = MaryUtils.toHumanReadableSize(downloadSize);
        broadcastState();
    }

    public void installSelectedLanguagesAndVoices2() {
        List<ComponentDescription> toInstall = installation_toInstall;
        System.out.println("Check license(s)");

        Map<URL, SortedSet<ComponentDescription>> licenseGroups = new HashMap<>();
        // Group components by their license:
        for (ComponentDescription cd : toInstall) {
            URL licenseURL = cd.getLicenseURL(); // may be null
            // null is an acceptable key for HashMaps, so it's OK.
            SortedSet<ComponentDescription> compsUnderLicense = licenseGroups.get(licenseURL);
            if (compsUnderLicense == null) {
                compsUnderLicense = new TreeSet<>();
                licenseGroups.put(licenseURL, compsUnderLicense);
            }
            assert compsUnderLicense != null;
            compsUnderLicense.add(cd);
        }

        Map<URL, String> licenseContents = new HashMap<>();
        for (URL licenseURL : licenseGroups.keySet()) {
            if (licenseURL == null) {
                continue;
            }
            URL localURL = LicenseRegistry.getLicense(licenseURL);
            File file;
            try {
                file = new File(localURL.toURI());
            } catch (URISyntaxException e) {
                file = new File(localURL.getPath());
            }
            try {
                byte[] encoded = Files.readAllBytes(Paths.get(file.getPath()));
                String content = new String(encoded, "UTF-8");
                licenseContents.put(licenseURL, content);
            } catch (IOException ex) {
            }
        }

        installationstate = "showlicenses";
        installationstateparam1 = licenseGroups;
        installationstateparam2 = licenseContents;
        broadcastState();
    }

    public void installSelectedLanguagesAndVoices3() {
        List<ComponentDescription> toInstall = installation_toInstall;
        System.out.println("Starting installation");
        InstallationThread installthread = new InstallationThread(toInstall, true);
        new Thread(installthread).start();
        
//        showProgressPanel(toInstall, true);
    }

    private void showProgressPanel(List<ComponentDescription> comps, boolean install) {
        final ProgressPanel pp = new ProgressPanel(comps, install);
        final JOptionPane optionPane = new JOptionPane(pp, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null,
                new String[]{"Abort"}, "Abort");
        // optionPane.setPreferredSize(new Dimension(640,480));
        final JDialog dialog = new JDialog((Frame) null, "Progress", false);
        dialog.setContentPane(optionPane);
        optionPane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String prop = e.getPropertyName();

                if (dialog.isVisible() && (e.getSource() == optionPane) && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                    pp.requestExit();
                    dialog.setVisible(false);
                }
            }
        });
        dialog.pack();
        dialog.setVisible(true);
        new Thread(pp).start();
    }

    private class InstallationThread extends javax.swing.JPanel implements Runnable, Observer {

        private List<ComponentDescription> allComponents;
        private ComponentDescription currentComponent = null;
        private boolean install;
        private boolean exitRequested = false;
        
        private int oldprogress;

        /**
         * Creates new form ProgressPanel
         *
         * @param componentsToProcess componentsToProcess
         * @param install install
         */
        public InstallationThread(List<ComponentDescription> componentsToProcess, boolean install) {
            this.allComponents = componentsToProcess;
            this.install = install;
        }

        public synchronized void requestExit() {
            this.exitRequested = true;
            if (currentComponent != null) {
                currentComponent.cancel();
            }
        }

        private synchronized boolean isExitRequested() {
            return exitRequested;
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

        @Override
        public void run() {
            boolean error = false;
            ComponentDescription problematic = null;
            int i = 0;
            int max = allComponents.size();
            installationstate = "installationprogress";
            installationstateparam1 = new OverallProgress(-1, max);
            installationstateparam2 = null;
            broadcastState();
            String action = install ? "install" : "uninstall";
            for (ComponentDescription comp : allComponents) {
                if (isExitRequested()) {
                    return;
                }
                System.out.println("Now " + action + "ing " + comp.getName() + "...");
                installationstate = "installationprogress";
                installationstateparam1 = new OverallProgress(i, max);
                installationstateparam2 = null;
                broadcastState();
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
                        } else if (comp.isUpdateAvailable()) {
                            comp.replaceWithUpdate();
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
                } else // uninstall
                if (comp.getStatus() == Status.INSTALLED) {
                    comp.uninstall();
                    if (comp.getStatus() == Status.ERROR) {
                        error = true;
                    } else if (comp.isUpdateAvailable()) {
                        comp.replaceWithUpdate();
                    }
                }
                if (error) {
                    problematic = comp;
                    System.err.println("Could not " + action + " " + comp.getName());
                    break;
                }
                comp.setSelected(false);
                i++;
            }
            if (error) {
                assert problematic != null;
                JOptionPane.showMessageDialog(this, "Could not " + action + " " + problematic.getName());
            } else {
                installationstate = "installationprogress";
                installationstateparam1 = new OverallProgress(max, max);
                installationstateparam2 = null;
                broadcastState();
                // JOptionPane.showMessageDialog(this, max + " components "+action+"ed successfully.");
            }
            this.setCurrentComponent(null);
            this.getTopLevelAncestor().setVisible(false);
        }

        @Override
        public void update(Observable o, Object arg) {
            if (o != currentComponent) {
                throw new IllegalStateException("We are observing " + o + " but the currentComponent is " + currentComponent);
            }
            verifyCurrentComponentDisplay();
        }

        private void verifyCurrentComponentDisplay() {
            if (currentComponent == null) {
                return;
            }
            String name = currentComponent.getName();
            String status = currentComponent.getStatus().toString();
            int progress = currentComponent.getProgress();
            if (progress < 0) {
                installationstate = "installationprogress";
                installationstateparam1 = null;
                installationstateparam2 = new CurrentProgress(name, status, 0);
//                broadcastState();
            } else {
                installationstate = "installationprogress";
                installationstateparam1 = null;
                installationstateparam2 = new CurrentProgress(name, status, progress);
//                broadcastState();
                if (oldprogress != progress) {
                    oldprogress = progress;
                    //StackOverflow in webgui somewhere here - dunno why
                    //-seems unrelated yet related
//                    System.out.println("MarySpeech - downloading|update" + progress);
                }
            }

        }
        

        private class OverallProgress {

            int current;
            int max;

            public OverallProgress(int current, int max) {
                this.current = current;
                this.max = max;
            }
        }

        private class CurrentProgress {

            String name;
            String status;
            int progress;

            public CurrentProgress(String name, String status, int progress) {
                this.name = name;
                this.status = status;
                this.progress = progress;
            }
        }
    }
    
	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(MarySpeech.class.getCanonicalName());
		meta.addDescription("Speech synthesis based on MaryTTS");
		meta.addCategory("speech", "sound");
		meta.addDependency("marytts", "5.1.2");
		meta.addDependency("com.sun.speech.freetts", "1.2");
		meta.addDependency("opennlp", "1.6");
		return meta;
	}
}

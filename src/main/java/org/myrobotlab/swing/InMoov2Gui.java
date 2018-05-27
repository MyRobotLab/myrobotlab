/**
 * InMoov V2 White page - The InMoov Service ( refactor WIP ).
 * 
 * The InMoov service allows control of the InMoov robot. This robot was created
 * by Gael Langevin. It's an open source 3D printable robot. All of the parts
 * and instructions to build are on http://www.inmoov.fr/). InMoov is a composite of servos, Arduinos,
 * microphone, camera, kinect and computer. The InMoov service is composed of
 * many other services, and allows easy initialization and control of these
 * sub systems.
 *
 */
package org.myrobotlab.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.InMoov2;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class InMoov2Gui extends ServiceGui implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(InMoov2Gui.class);
	Runtime myRuntime = (Runtime) Runtime.getInstance();

	private final JTabbedPane inmoovPane = new JTabbedPane(JTabbedPane.TOP);
	JComboBox comboLanguage = new JComboBox();
	JCheckBox muteCheckBox = new JCheckBox("");
	JComboBox speechCombo = new JComboBox();
	JButton startSpeech = new JButton("Start Mouth");
	JButton configureSpeech = new JButton("Configure Speech");
	JComboBox earCombo = new JComboBox();
	JButton startEar = new JButton("Start Ear");
	JButton configureEar = new JButton("Configure Ear");

	public InMoov2Gui(final String boundServiceName, final SwingGui myService) {
		super(boundServiceName, myService);

		InMoov2 i02 = (InMoov2) myRuntime.getService(boundServiceName);
		log.info(Runtime.getInstance().getServiceTypeNames().toString());
		Font f = new Font("SansSerif", Font.BOLD, 20);
		// Create TABS and content

		// general tab
		JPanel generalPanel = new JPanel();
		add(inmoovPane);
		generalPanel.setBackground(Color.WHITE);
		ImageIcon generalIcon = Util.getImageIcon("InMoov.png");
		inmoovPane.addTab("General", generalIcon, generalPanel);
		generalPanel.setLayout(new GridLayout(2, 2, 0, 0));

		JLabel lblNewLabel = new JLabel(" Language : ");
		generalPanel.add(lblNewLabel);

		for (Entry<Integer, String[]> e : i02.languages.entrySet()) {
			comboLanguage.addItem(e.getValue()[1]);
		}
		generalPanel.add(comboLanguage);

		JLabel MuteLabel = new JLabel("Mute startup :");
		generalPanel.add(MuteLabel);
		muteCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		generalPanel.add(muteCheckBox);

		// vocal tab
		JPanel vocalRecoPanel = new JPanel();
		vocalRecoPanel.setBackground(Color.WHITE);
		ImageIcon vocalIcon = Util.getImageIcon("Speech.png");
		inmoovPane.addTab("Vocal interraction", vocalIcon, vocalRecoPanel);
		vocalRecoPanel.setLayout(new GridLayout(4, 2, 0, 0));

		JLabel speechLabel = new JLabel("Speech engine :");
		vocalRecoPanel.add(speechLabel);
		for (int i = 0; i < i02.speechEngines.size(); i++)

		{

			speechCombo.addItem(i02.speechEngines.get(i));

		}

		speechCombo.setFont(f);
		speechCombo.setBackground(new Color(227,251,209));
		vocalRecoPanel.add(speechCombo);

		vocalRecoPanel.add(startSpeech);

		vocalRecoPanel.add(configureSpeech);
		JLabel earLabel = new JLabel("Ear engine :");
		vocalRecoPanel.add(earLabel);

		for (int i = 0; i < i02.earEngines.size(); i++)

		{

			earCombo.addItem(i02.earEngines.get(i));

		}
		earCombo.setFont(f);
		earCombo.setBackground(new Color(241,253,231));
		vocalRecoPanel.add(earCombo);

		vocalRecoPanel.add(startEar);

		vocalRecoPanel.add(configureEar);

		// skeleton tab
		JPanel skeletonPanel = new JPanel();
		skeletonPanel.setBackground(Color.WHITE);
		ImageIcon skeletonIcon = Util.getImageIcon("InMoovArm.png");
		inmoovPane.addTab("Skeleton", skeletonIcon, skeletonPanel);

		// life tab
		JPanel lifePanel = new JPanel();
		lifePanel.setBackground(Color.WHITE);
		ImageIcon lifeIcon = Util.getImageIcon("sensor.png");
		inmoovPane.addTab("Life", lifeIcon, lifePanel);

		// listeners
		comboLanguage.addActionListener(this);
		muteCheckBox.addActionListener(this);
		speechCombo.addActionListener(this);
		startSpeech.addActionListener(this);
		configureSpeech.addActionListener(this);
		earCombo.addActionListener(this);
		startEar.addActionListener(this);
		configureEar.addActionListener(this);
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Object o = event.getSource();
				if (o == comboLanguage) {
					send("setLanguage", comboLanguage.getSelectedIndex());
				}
				if (o == muteCheckBox) {
					send("setMute", muteCheckBox.isSelected());
				}
				if (o == speechCombo) {
					send("setSpeechEngine", speechCombo.getSelectedItem());
				}
				if (o == configureSpeech) {
					swingGui.setActiveTab(boundServiceName + ".mouth");
				}
				if (o == earCombo) {
					send("setEarEngine", earCombo.getSelectedItem());
				}
				if (o == configureEar) {
					swingGui.setActiveTab(boundServiceName + ".ear");
				}
				if (o == startEar) {
					send("startEar");
				}
				if (o == startSpeech) {
					send("startMouth");
				}

			}
		});
	}

	public void onState(final InMoov2 i02) {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				removeListeners();
				comboLanguage.setSelectedIndex((i02.getLanguage()));
				speechCombo.setSelectedItem((i02.getSpeechEngine()));
				earCombo.setSelectedItem((i02.getEarEngine()));
				muteCheckBox.setSelected(i02.getMute());
				// TODO : release the service before start an other
				if (i02.mouth == null) {
					startSpeech.setText("Start Speech");
					startSpeech.setEnabled(true);
					configureSpeech.setEnabled(false);
					speechCombo.setEnabled(true);
				} else {
					startSpeech.setText("Speech started...");
					startSpeech.setEnabled(false);
					configureSpeech.setEnabled(true);
					speechCombo.setEnabled(false);
				}
				if (i02.ear == null) {
					startEar.setText("Start Ear");
					startEar.setEnabled(true);
					configureEar.setEnabled(false);
					earCombo.setEnabled(true);
				} else {
					startEar.setText("Ear started...");
					startEar.setEnabled(false);
					configureEar.setEnabled(true);
					earCombo.setEnabled(false);
				}
				restoreListeners();
			}
		});
	}

	public void removeListeners() {
		comboLanguage.removeActionListener(this);
		muteCheckBox.removeActionListener(this);
		speechCombo.removeActionListener(this);
		startSpeech.removeActionListener(this);
		configureSpeech.removeActionListener(this);
		startEar.removeActionListener(this);
		configureEar.removeActionListener(this);
		earCombo.removeActionListener(this);
	}

	public void restoreListeners() {
		comboLanguage.addActionListener(this);
		muteCheckBox.addActionListener(this);
		speechCombo.addActionListener(this);
		startSpeech.addActionListener(this);
		configureSpeech.addActionListener(this);
		startEar.addActionListener(this);
		configureEar.addActionListener(this);
		earCombo.addActionListener(this);
	}

}
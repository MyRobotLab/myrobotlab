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

package org.myrobotlab.control;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.myrobotlab.gp.GPMessageBestFound;
import org.myrobotlab.gp.GPMessageEvaluatingIndividual;
import org.myrobotlab.gp.RealPoint;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.interfaces.VideoGUISource;
import org.slf4j.Logger;

public class GeneticProgrammingGUI extends ServiceGUI implements ListSelectionListener, VideoGUISource {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(GeneticProgrammingGUI.class.toString());

	VideoWidget video = null;
	Graphics g = null;
	BufferedImage img = null;

	int width = 320;
	int height = 240;

	public Random rand = new Random();

	RealPoint[] fitnessCases = new RealPoint[4];

	JTextArea fitnessCasesTextArea = null;
	JTextArea bestProgram = null;
	JTextField populationSize = new JTextField("100");
	JTextField maxDepthNewInd = new JTextField("6.0");
	JTextField crossover = new JTextField("80.0");
	JTextField reproduction = new JTextField("0.0");
	JTextField mutation = new JTextField("20.0");
	JTextField depthForIndAlterCrossover = new JTextField("20.0");
	JTextField depthNewSubtreesInMutant = new JTextField("4.0");

	GPMessageBestFound lastBest = null;

	public GeneticProgrammingGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	public void init() {
		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		g = img.getGraphics();
		video.displayFrame(new SerializableImage(img, boundServiceName));

		// TODO - data needs to come from service and/or Mouse
		fitnessCases[0] = new RealPoint(123, 164);
		fitnessCases[1] = new RealPoint(249, 164);
		fitnessCases[2] = new RealPoint(218, 142);
		fitnessCases[3] = new RealPoint(130, 142);

		String s = "// the data \n";
		for (int i = 0; i < fitnessCases.length; ++i) {
			s += fitnessCases[i].x + " " + fitnessCases[i].y + "\n";
		}

		bestProgram = new JTextArea("new program", 8, 20);

		fitnessCasesTextArea = new JTextArea(s, 8, 20);
		video = new VideoWidget(boundServiceName, myService, tabs);
		video.init();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridheight = 4;
		gc.gridwidth = 2;
		display.add(video.display, gc);

		gc.gridwidth = 1;
		gc.gridheight = 1;

		gc.gridx = 3;
		gc.gridy = 0;
		display.add(bestProgram, gc);

		gc.gridx = 0;
		gc.gridy = 5;
		display.add(fitnessCasesTextArea, gc);

		gc.gridx = 0;
		display.add(new JLabel("population size"), gc);
		gc.gridx = 1;
		++gc.gridy;
		display.add(populationSize, gc);

		gc.gridx = 0;
		display.add(new JLabel("max depth for new ind"), gc);
		gc.gridx = 1;
		++gc.gridy;
		display.add(maxDepthNewInd, gc);

		gc.gridx = 0;
		display.add(new JLabel("crossover"), gc);
		gc.gridx = 1;
		++gc.gridy;
		display.add(crossover, gc);

		gc.gridx = 0;
		display.add(new JLabel("reproduction"), gc);
		gc.gridx = 1;
		++gc.gridy;
		display.add(reproduction, gc);

		gc.gridx = 0;
		display.add(new JLabel("mutation"), gc);
		gc.gridx = 1;
		++gc.gridy;
		display.add(mutation, gc);

		gc.gridx = 0;
		display.add(new JLabel("depth for ind alter crossover"), gc);
		gc.gridx = 1;
		++gc.gridy;
		display.add(depthForIndAlterCrossover, gc);

		gc.gridx = 0;
		display.add(new JLabel("depth new subtrees in mutant"), gc);
		gc.gridx = 1;
		++gc.gridy;
		display.add(depthNewSubtreesInMutant, gc);

		setCurrentFilterMouseListener();

	}

	protected ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	public void displayFrame(SerializableImage img) {
		video.displayFrame(img);
	}

	void drawFitnessGoal() {
		g.setColor(Color.yellow);
		for (int i = 0; i < fitnessCases.length; ++i) {
			if (i != fitnessCases.length - 1) {
				g.drawLine((int) fitnessCases[i].x, (int) fitnessCases[i].y, (int) fitnessCases[i + 1].x, (int) fitnessCases[i + 1].y);
			} else {
				g.drawLine((int) fitnessCases[i].x, (int) fitnessCases[i].y, (int) fitnessCases[0].x, (int) fitnessCases[0].y);
			}
		}
	}

	@Override
	public void attachGUI() {
		video.attachGUI();
		subscribe("publishInd", "publishInd", GPMessageEvaluatingIndividual.class);
		subscribe("publish", "publish", GPMessageBestFound.class);
		drawFitnessGoal();
		video.displayFrame(new SerializableImage(img, boundServiceName));
	}

	void drawPath(RealPoint[] path) {
		for (int i = 0; i < path.length; ++i) {
			if (i != path.length - 1) {
				RealPoint p = path[i];
				RealPoint p1 = path[i + 1];
				g.drawLine((int) p.x, (int) p.y, (int) p1.x, (int) p1.y);
			} else {
				RealPoint p = path[i];
				RealPoint p1 = path[0];
				g.drawLine((int) p.x, (int) p.y, (int) p1.x, (int) p1.y);
			}
		}
	}

	public GPMessageEvaluatingIndividual publishInd(GPMessageEvaluatingIndividual ind) {
		// clear screen
		g.setColor(Color.black);
		g.fillRect(0, 0, width, height);

		drawFitnessGoal();

		if (lastBest != null) {
			g.setColor(Color.red);
			g.drawString("fitness    " + lastBest.fitness, 10, 10);
			g.drawString("rfitness    " + lastBest.standardFitness, 10, 20);
			g.drawString("generation " + lastBest.generation, 10, 30);
			drawPath(lastBest.data);
		}

		g.setColor(Color.gray);

		g.drawString("ind #      " + ind.individualNr, 10, 40);
		g.drawString("generation " + ind.generationNr, 10, 50);
		g.drawString("fitness " + ind.fitness, 10, 60);
		g.drawString("rfitness " + ind.rawFitness, 10, 70);

		drawPath(ind.data);

		video.displayFrame(new SerializableImage(img, boundServiceName));
		return ind;
	}

	public GPMessageBestFound publish(GPMessageBestFound best) {
		lastBest = best;

		// clear screen
		g.setColor(Color.black);
		g.fillRect(0, 0, width, height);

		drawFitnessGoal();

		// write best of info
		g.setColor(Color.red);
		drawPath(best.data);
		g.drawString("fitness    " + best.fitness, 10, 10);
		g.drawString("generation " + best.generation, 10, 20);

		bestProgram.setText(best.program);

		video.displayFrame(new SerializableImage(img, boundServiceName));
		return best;
	}

	@Override
	public void detachGUI() {
		video.detachGUI();
	}

	// TODO - encapsulate this
	// MouseListener mouseListener = new MouseAdapter() {
	public void setCurrentFilterMouseListener() {

		// traces.addMouseListener(mouseListener);
	}

	@Override
	public VideoWidget getLocalDisplay() {
		return video;
	}

	@Override
	public void valueChanged(ListSelectionEvent arg0) {
	}

}

//-------1---------2---------3---------4---------5---------6---------7---------8
package org.myrobotlab.gp;

import java.awt.Rectangle;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

/**
 * This class demonstrates symbolic regression with genetic programming. The
 * algorithms are taken from John R. Koza's book "Genetic programming: on the
 * programming of computers by means of natural selection", Bradford Book, 1993,
 * ISBN 0-262-11170-5. <br>
 * John Koza holds the copyright for the algorithms. My code is a mere
 * translation from Koza's LISP source into Java. I did not change most of the
 * variable and function names, so that you will recognize the parallels
 * immediately. I even left the wording of the original comments untouched
 * (after all, Koza's English is better than mine). <br>
 * You find Koza's original code and license agreement <a
 * href="http://www-cs-faculty.stanford.edu/~koza/littlelisp.html">here.</a> <br>
 * <br>
 * A GP object contains only data and algorithms, but no fancy user interface.
 * In the classic Model-View-Controller architecture, you would call it the
 * "model". Whenever the state of the model changes, it sends a message to all
 * its views so that these may update themselves to reflect the change. In Java
 * parlance, the views are based on "Observer" and the model implements
 * "Observable". <br>
 * The GP evolution algorithm runs in a thread of its own. That's why the class
 * implements interface "Runnable".
 * 
 * @version 1.0
 * @author Original algorithms (c) <a
 *         href="http://www-cs-faculty.stanford.edu/~koza/">John R. Koza</a>,<br>
 *         Java port by <a href="mailto:gerber@ifh.ee.ethz.ch">Hans U.
 *         Gerber</a>
 */
public class GP extends Observable implements Runnable {

	public final static Logger log = LoggerFactory.getLogger(GP.class.getCanonicalName());

	public Number populationSize;
	public Number maxDepthForNewIndividuals;
	public Number maxDepthForIndividualsAfterCrossover;
	public Number maxDepthForNewSubtreesInMutants;
	public Number crossoverFraction;
	public Number fitnessProportionateReproFraction;
	public Number mutationFraction;
	public Choices methodOfGeneration;
	public Choices methodOfSelection;
	public Points fitnessCases;
	public SetData terminalSet;
	public SetData functionSet;

	public Service myService = null;

	Thread thread = null;
	Individual[] population;
	int currentGeneration = 0;
	Individual bestOfRunIndividual = null;
	int generationOfBestOfRunIndividual = 0;
	static final int MAX_GENERATIONS = 1000;

	/**
	 * Each GP has its own random number generator.
	 */
	static final int SEED = 12345;
	static Random random = new Random(SEED);

	public GP(Service s) {
		//
		// All the parameters that the user can change are wrapped up with
		// a user interface component. Let's construct these components here.
		// This will preset their text labels, default values, initial
		// selections and value ranges.
		// To use these components, add them to a dialog.
		//
		myService = s;

		populationSize = new Number("Population size", 100, 10, 1000);
		maxDepthForNewIndividuals = new Number("Max. depth for new individuals", 6, 1, 10);
		maxDepthForIndividualsAfterCrossover = new Number("Max. depth for individuals after crossover", 20, 5, 20);
		maxDepthForNewSubtreesInMutants = new Number("Max. depth for new subtrees in mutants", 4, 1, 7);
		crossoverFraction = new Number("Crossover fraction", 80, 1, 100);
		fitnessProportionateReproFraction = new Number("Reproduction fraction", 0, 0, 100);
		mutationFraction = new Number("Mutation fraction", 20, 0, 100);
		fitnessCases = new Points("Fitness cases", makeDefaultFitnessCases());

		ProgramChoice[] terminalChoices = { new ProgramChoice("Random constant", new RandomConstant().getClass()), new ProgramChoice("Variable x", new Variable().getClass()) };
		int[] terminalSelections = { 0, 1 };
		terminalSet = new SetData("Terminal set", terminalChoices, terminalSelections);

		ProgramChoice[] functionChoices = { new ProgramChoice("+ (add)", new Addition(myService).getClass()), new ProgramChoice("- (sub)", new Subtraction(myService).getClass()),
				new ProgramChoice("* (mul)", new Multiplication(myService).getClass()), new ProgramChoice("/ (div)", new Division(myService).getClass()),
				new ProgramChoice("sin", new Sine(myService).getClass()), new ProgramChoice("cos", new Cosine(myService).getClass()),
				new ProgramChoice("exp", new Exp(myService).getClass()), new ProgramChoice("sleep", new Sleep(myService).getClass()),
				new ProgramChoice("moveHip", new MoveHip(myService).getClass()), new ProgramChoice("moveKnee", new MoveKnee(myService).getClass()) };
		int[] functionSelections = { 0, 1, 2, 3, 8, 9 };
		// int[] functionSelections = {0, 1, 3, 7, 8};
		functionSet = new SetData("Function set", functionChoices, functionSelections);

		MethodOfGeneration[] generationChoices = { new MethodOfGeneration("Grow", MethodOfGeneration.GROW), new MethodOfGeneration("Full", MethodOfGeneration.FULL),
				new MethodOfGeneration("Ramped half and half", MethodOfGeneration.RAMPED_HALF_AND_HALF) };
		methodOfGeneration = new Choices("Method of generation", generationChoices, 2);

		MethodOfSelection[] selectionChoices = { new MethodOfSelection("Fitness proportionate", MethodOfSelection.FITNESS_PROPORTIONATE),
				new MethodOfSelection("Tournament", MethodOfSelection.TOURNAMENT) };
		methodOfSelection = new Choices("Method of selection", selectionChoices, 1);
	}

	/**
	 * Should be called after any of the settings have changed, will addListener
	 * any observers of the changes.
	 */
	public void settingsChanged() {
		notifyObservers(new GPMessageFitnessCasesSet(fitnessCases.get()));
	}

	/**
	 * After the construction of a GP object, you should call the init() method;
	 * this will addListener any observers that the initial settings are now
	 * established. Don't do it in the constructor, because Observer-Observable
	 * relationships are yet not established during construction.
	 */
	public void init() {
		settingsChanged();
	}

	/**
	 * Starts the GP algorithm running in its own thread. The priority is set
	 * lower than normal, so that a user interface thread with normal priority
	 * can surely preempt the GP thread.
	 */
	public synchronized void start() {
		thread = new Thread(this);
		thread.setPriority(Thread.NORM_PRIORITY - 1);
		thread.start();
		state = STARTED;
		log.info("STARTED");
		notifyObservers(new GPMessageStateChanged(state, ""));
	}

	/**
	 * Suspends the GP thread.
	 */
	public synchronized void suspend() {
		if (thread != null && thread.isAlive()) {
			state = SUSPENDED;
			log.info("SUSPENDED");
			notifyObservers(new GPMessageStateChanged(state, ""));
			thread.suspend();
		}
	}

	/**
	 * Resumes a suspended GP thread.
	 */
	public synchronized void resume() {
		if (thread != null && thread.isAlive()) {
			thread.resume();
			state = RESUMED;
			log.info("RESUMED");
			notifyObservers(new GPMessageStateChanged(state, ""));
		}
	}

	/**
	 * Stops a running GP thread and kills it.
	 */
	public synchronized void stop() {
		// Note that the GP thread may call stop() to kill itself.
		// Therefore, any code following thread.stop() below would
		// never be executed.
		if (thread != null && thread.isAlive()) {
			state = STOPPED;
			log.info("STOPPED");
			notifyObservers(new GPMessageStateChanged(state, ""));
			thread.stop();
		}
	}

	/**
	 * Aborts a running GP thread and notifies any observers with a diagnostic
	 * message. This method is tyically called from an exception handler if the
	 * GP algorithm crashes unexpectedly.
	 */
	public synchronized void crash() {
		log.info("CRASH " + currentGeneration);
		if (thread != null && thread.isAlive()) {
			state = STOPPED;
			notifyObservers(new GPMessageStateChanged(state, "The genetic algorithm crashed\n" + "in generation " + currentGeneration + ".\n" + "Sorry."));
			thread.stop();
		}
	}

	public static final int IDLE = 0;
	public static final int STARTED = 1;
	public static final int SUSPENDED = 2;
	public static final int RESUMED = 3;
	public static final int STOPPED = 4;

	private int state = IDLE;
	private int oldState = IDLE;

	/**
	 * @return the execution state of the GP thread
	 */
	public int getState() {
		return state;
	}

	/**
	 * freeze() and thaw() are useful if the GP is running in an applet. A
	 * well-behaved applet should stop running when the user moves to another
	 * page and it should start again when he returns to the (still loaded)
	 * applet. freeze() and thaw() may thus be called from the applet's start()
	 * and stop() methods to pause the GP while its applet is not visible.
	 */
	public synchronized void freeze() {
		log.info("FREEZE " + currentGeneration);
		oldState = getState();
		if (thread != null && thread.isAlive() && (oldState == STARTED || oldState == RESUMED)) { // i.e.
																									// running
			thread.suspend();
		}
	}

	/**
	 * freeze() and thaw() are useful if the GP is running in an applet. A
	 * well-behaved applet should stop running when the user moves to another
	 * page and it should start again when he returns to the (still loaded)
	 * applet. freeze() and thaw() may thus be called from the applet's start()
	 * and stop() methods to pause the GP while its applet is not visible.
	 */
	public synchronized void thaw() {
		log.info("THAW " + currentGeneration);
		if (thread != null && thread.isAlive() && (oldState == STARTED || oldState == RESUMED)) {
			thread.resume();
		}
	}

	/**
	 * @return a terminal, randomly chosen from the terminal set
	 */
	Terminal chooseFromTerminalSet() {
		Terminal choice;
		log.info("chooseFromTerminalSet " + currentGeneration);
		try {
			int index = random.nextInt(terminalSet.countSelections());
			Class cls = ((ProgramChoice) terminalSet.getSelectedItem(index)).value();
			choice = (Terminal) (cls.newInstance());
		} catch (Exception e) {
			choice = null;
		}
		return choice;
	}

	/**
	 * Creates arguments for a function
	 */
	void createArgumentsForFunction(Function function, int allowableDepth, boolean fullP) {
		log.info("createArgumentsForFunction " + currentGeneration);

		for (int i = 0; i < function.arg.length; i++) {
			function.arg[i] = createIndividualProgram(allowableDepth, false, fullP);
		}
	}

	/**
	 * Creates a program recursively using functions and terminals from the
	 * respective sets.
	 * 
	 * @param allowableDepth
	 *            the remaining depth of the tree we can create, when we hit
	 *            zero we will only select terminals
	 * @param topNodeP
	 *            is true only when we are being called as the top node in the
	 *            tree. This allows us to make sure that we always put a
	 *            function at the top of the tree.
	 * @param fullP
	 *            indicates whether this individual is to be maximally bushy or
	 *            not
	 */
	Program createIndividualProgram(int allowableDepth, boolean topNodeP, boolean fullP) {
		Program p;
		int choice;
		Function function;

		// log.warn("createIndividualProgram " + currentGeneration);

		if (allowableDepth <= 0) {
			// We've reached maxdepth, so just pack a terminal.
			p = chooseFromTerminalSet();
		} else {
			if (fullP || topNodeP) {
				// We are the top node or are a full tree, so pick only a
				// function.
				choice = random.nextInt(functionSet.countSelections());
				try {
					Class cls = ((ProgramChoice) functionSet.getSelectedItem(choice)).value();
					Constructor mc = cls.getConstructor(new Class[] { Service.class });
					function = (Function) mc.newInstance(myService);

				} catch (Exception e) {
					Logging.logException(e);
					function = null;
				}
				createArgumentsForFunction(function, allowableDepth - 1, fullP);
				p = function;
			} else {
				// Choose one from the bag of functions and terminals.
				choice = random.nextInt(terminalSet.countSelections() + functionSet.countSelections());
				if (choice < functionSet.countSelections()) {
					// We chose a function, so pick it out and go on creating
					// the tree down from here.
					try {
						// Class cls =
						// ((ProgramChoice)functionSet.getSelectedItem(choice)).value();
						// function = (Function)cls.newInstance();

						Class cls = ((ProgramChoice) functionSet.getSelectedItem(choice)).value();
						Constructor mc = cls.getConstructor(new Class[] { Service.class });
						function = (Function) mc.newInstance(myService);

					} catch (Exception e) {
						function = null;
					}
					createArgumentsForFunction(function, allowableDepth - 1, fullP);
					p = function;
				} else {
					// We chose an atom, so pick it out.
					p = chooseFromTerminalSet();
				}
			}
		}
		return p;
	}

	/**
	 * Creates the initial population. This is an array of individuals. Its size
	 * is sizeOfPopulation. The program slot of each individual is initialized
	 * to a suitable random program.
	 */
	void createPopulation() {

		log.warn("createPopulation " + currentGeneration);

		int allowableDepth;
		boolean fullP;
		Hashtable generation0UniquifierTable = new Hashtable();

		population = new Individual[populationSize.intValue()];
		int minimumDepthOfTrees = 1;
		boolean fullCycleP = false;
		int maxDepthForNewIndivs = maxDepthForNewIndividuals.intValue();
		int attemptsAtThisIndividual = 0;
		int individualIndex = 0;
		while (individualIndex < population.length) {
			switch (((MethodOfGeneration) methodOfGeneration.getSelection()).getValue()) {
			case MethodOfGeneration.FULL:
				allowableDepth = maxDepthForNewIndivs;
				fullP = true;
				break;
			case MethodOfGeneration.GROW:
				allowableDepth = maxDepthForNewIndivs;
				fullP = false;
				break;
			case MethodOfGeneration.RAMPED_HALF_AND_HALF:
				// With each new individual, the allowed depth is ramped up
				// until it reaches the maximum. Then it is reset to the minimum
				// depth again.
				// At this time, we toggle between full trees and grown trees,
				// so that half of the trees are full and the other half grown.
				// Note that the switch occurs only at the first attempt to
				// create
				// a new tree. This is to avoid unecessary toggling when several
				// attempts are necessary for the same individual.
				allowableDepth = minimumDepthOfTrees + (individualIndex % (maxDepthForNewIndivs - minimumDepthOfTrees + 1));
				if (attemptsAtThisIndividual == 0 && individualIndex % (maxDepthForNewIndivs - minimumDepthOfTrees + 1) == 0) {
					fullCycleP = !fullCycleP;
				}
				fullP = fullCycleP;
				break;
			default:
				allowableDepth = maxDepthForNewIndivs;
				fullP = false;
				break;
			}

			Program newProgram = createIndividualProgram(allowableDepth, true, fullP);

			// Check if we have already created this program.
			// If not then store it and move on.
			// If we have then try again.
			// The easiest way to compare two programs is to
			// look at their printed representation:
			//
			String hashKey = newProgram.toString(0);
			if (!generation0UniquifierTable.containsKey(hashKey)) {
				population[individualIndex] = new Individual(newProgram);
				individualIndex++;
				generation0UniquifierTable.put(hashKey, newProgram);
				attemptsAtThisIndividual = 0;
			} else {
				attemptsAtThisIndividual++;
				if (attemptsAtThisIndividual > 20) {
					// This depth has probably filled up, so bump the
					// depth counter and keep the max depth in line with
					// the new minimum:
					//
					minimumDepthOfTrees++;
					maxDepthForNewIndivs = Math.max(maxDepthForNewIndivs, minimumDepthOfTrees);
					attemptsAtThisIndividual = 0;
				}
			}
		}
	}

	static final Condition isProgram = new IsProgram();
	static final Condition isFunction = new IsFunction();

	/**
	 * Produces two new individuals by crossing two parents. First, a crossover
	 * point is selected randomly in each parent. Experience shows that it is
	 * more useful to select inner nodes (functions, not terminals) as crossover
	 * points. The ratio of cuts at inner points vs. cuts at terminals is fixed
	 * in this function. Then the two parents are cut at their crossover points,
	 * giving two tree fragments and two cut-off subtrees. The two subtrees are
	 * swapped and spliced with the fragments again. We have to make sure that
	 * the newly formed trees do not exceed a certain depth. If they do, one of
	 * the parents is used as offspring.
	 */
	Program[] crossover(Program male, Program female) {
		double CrossoverAtFunctionFraction = 0.9;
		boolean crossoverAtFunction;
		// Make copies of the parents first, because they will be destructively
		// modified:
		Program[] offspring = new Program[2];
		offspring[0] = (Program) male.clone();
		offspring[1] = (Program) female.clone();

		int malePoint, femalePoint;
		TreeHook maleHook, femaleHook;
		crossoverAtFunction = (random.nextDouble() < CrossoverAtFunctionFraction);
		if (crossoverAtFunction) {
			malePoint = random.nextInt(male.countNodes(isFunction));
			maleHook = getSubtree(offspring[0], malePoint, isFunction);
		} else {
			malePoint = random.nextInt(male.countNodes());
			maleHook = getSubtree(offspring[0], malePoint, isProgram);
		}
		crossoverAtFunction = (random.nextDouble() < CrossoverAtFunctionFraction);
		if (crossoverAtFunction) {
			femalePoint = random.nextInt(female.countNodes(isFunction));
			femaleHook = getSubtree(offspring[1], femalePoint, isFunction);
		} else {
			femalePoint = random.nextInt(female.countNodes());
			femaleHook = getSubtree(offspring[1], femalePoint, isProgram);
		}

		// Modify the new individuals by smashing in the (copied) subtree from
		// the old individual.
		if (maleHook.parent == null) {
			offspring[0] = femaleHook.subtree;
		} else {
			maleHook.parent.arg[maleHook.childIndex] = femaleHook.subtree;
		}
		if (femaleHook.parent == null) {
			offspring[1] = maleHook.subtree;
		} else {
			femaleHook.parent.arg[femaleHook.childIndex] = maleHook.subtree;
		}

		// Make sure that the new individuals aren't too big.
		validateCrossover(male, female, offspring);

		// log.warn("returning offspring " + currentGeneration);

		return offspring;
	}

	/**
	 * @return the depth of the deepest branch of the tree
	 */
	int maxDepthOfTree(Program tree) {
		int maxDepth = 0;
		if (tree instanceof Function) {
			for (int a = 0; a < ((Function) tree).arg.length; a++) {
				Program s = ((Function) tree).arg[a];
				int depth = maxDepthOfTree(s);
				maxDepth = Math.max(maxDepth, depth);
			}
			return (1 + maxDepth);
		} else {
			return 0;
		}
	}

	/**
	 * Given the parents and two offsprings from a crossover operation check to
	 * see whether we have exceeded the maximum allowed depth. If a new
	 * individual has exceeded the maximum depth then one of the parents is
	 * used.
	 */
	void validateCrossover(Program male, Program female, Program[] offspring) {
		int depth;
		for (int i = 0; i < offspring.length; i++) {
			if (offspring[i] == null) {
				depth = 0;
			} else {
				depth = maxDepthOfTree(offspring[i]);
			}
			if (depth < 1 || depth > maxDepthForIndividualsAfterCrossover.intValue()) {
				int whichParent = random.nextInt(2);
				if (whichParent == 0) {
					offspring[i] = (Program) male.clone();
				} else {
					offspring[i] = (Program) female.clone();
				}
			}
		}
	}

	/**
	 * Mutates the argument program by picking a random point in the tree and
	 * substituting in a brand new subtree created in the same way that we
	 * create the initial random population.
	 * 
	 * @return a mutated copy of the original program
	 */
	Program mutate(Program program) {
		// log.warn("mutate " + currentGeneration);

		// Pick the mutation point.
		int mutationPoint = random.nextInt(program.countNodes());
		// Create a brand new subtree.
		Program newSubtree = createIndividualProgram(maxDepthForNewSubtreesInMutants.intValue(), true, false);
		Program newProgram = (Program) program.clone();
		// Smash in the new subtree.
		TreeHook hook = getSubtree(program, mutationPoint, isProgram);
		if (hook.parent == null) {
			newProgram = hook.subtree;
		} else {
			hook.parent.arg[hook.childIndex] = newSubtree;
		}
		return newProgram;
	}

	/**
	 * Controls the actual breeding of the new population. Loops through the
	 * population executing each operation (e.g., crossover,
	 * fitness-proportionate reproduction, mutation) until it has reached the
	 * specified fraction. The new programs that are created are stashed in
	 * newPrograms until we have exhausted the population, then we copy the new
	 * individuals into the old ones.
	 */
	void breedNewPopulation() {
		log.warn("breedNewPopulation " + currentGeneration);

		Program[] newPrograms;
		double fraction;
		int index;
		Individual individual1, individual2;
		double sumOfFractions = this.crossoverFraction.doubleValue() + this.fitnessProportionateReproFraction.doubleValue() + this.mutationFraction.doubleValue();
		double crossoverFraction = this.crossoverFraction.doubleValue() / sumOfFractions;
		double reproductionFraction = this.fitnessProportionateReproFraction.doubleValue() / sumOfFractions;
		// double mutationFraction = this.mutationFraction.get() /
		// sumOfFractions;

		newPrograms = new Program[population.length];
		fraction = 0.0;
		index = 0;

		// A hint from Jorg Frohlich: keep the best individual all the time:
		newPrograms[index] = (Program) bestOfRunIndividual.program.clone();
		index++;

		while (index < population.length) {
			fraction = (double) index / (double) population.length;
			individual1 = findIndividual();
			if (fraction < crossoverFraction) {
				individual2 = findIndividual();
				Program[] offspring = crossover(individual1.program, individual2.program);
				newPrograms[index] = offspring[0];
				index++;
				if (index < population.length) {
					newPrograms[index] = offspring[1];
					index = index++;
				}
			} else {
				if (fraction < reproductionFraction + crossoverFraction) {
					newPrograms[index] = (Program) individual1.program.clone();
					index++;
				} else {
					newPrograms[index] = mutate(individual1.program);
					index++;
				}
			}
		}
		for (index = 0; index < population.length; index++) {
			population[index].program = newPrograms[index];
		}
	}

	/**
	 * Clean out the statistics in each individual in the population.
	 */
	void zeroizeFitnessMeasuresOfPopulation() {
		log.info("zeroizeFitnessMeasuresOfPopulation " + currentGeneration);

		for (int i = 0; i < population.length; i++) {
			population[i].standardizedFitness = 0.0;
			population[i].adjustedFitness = 0.0;
			population[i].normalizedFitness = 0.0;
			population[i].hits = 0;
		}
	}

	/**
	 * Loops over the individuals in the population evaluating and recording the
	 * fitness and hits.
	 */
	void evaluateFitnessOfPopulation() {
		log.warn("evaluateFitnessOfPopulation " + currentGeneration + " population length " + population.length);

		for (int i = 0; i < population.length; i++) {
			fitnessFunction(population[i], i); // TODO - Over-rideable fitness
												// function
			// log.warn(population[i].program);
		}

	}

	/**
	 * Computes the normalized and adjusted fitness of each individual in the
	 * population.
	 */
	void normalizeFitnessOfPopulation() {
		log.warn("normalizeFitnessOfPopulation " + currentGeneration);

		double sumOfAdjustedFitnesses = 0.0;
		for (int i = 0; i < population.length; i++) {
			// Set the adjusted fitness.
			population[i].adjustedFitness = 1.0 / (population[i].standardizedFitness + 1.0);
			// Add up the adjusted fitnesses so that we can normalize them.
			sumOfAdjustedFitnesses = sumOfAdjustedFitnesses + population[i].adjustedFitness;
			log.warn(i + " " + population[i].adjustedFitness);
		}
		log.warn("normalized ----");
		// Loop through population normalizing the adjusted fitness.
		for (int i = 0; i < population.length; i++) {
			population[i].normalizedFitness = population[i].adjustedFitness / sumOfAdjustedFitnesses;
			log.warn(i + " " + population[i].normalizedFitness);
		}
	}

	/**
	 * Uses a quicksort to sort the population destructively into descending
	 * order of normalized fitness.
	 */
	private void sort(int low, int high) {
		// log.info("sort " + currentGeneration);

		int index1, index2;
		double pivot;
		Individual temp;

		index1 = low;
		index2 = high;
		pivot = population[(low + high) / 2].normalizedFitness;
		do {
			while (population[index1].normalizedFitness > pivot) {
				index1++;
			}
			while (population[index2].normalizedFitness < pivot) {
				index2--;
			}
			if (index1 <= index2) {
				temp = population[index2];
				population[index2] = population[index1];
				population[index1] = temp;
				index1++;
				index2--;
			}
		} while (index1 <= index2);
		if (low < index2) {
			sort(low, index2);
		}
		if (index1 < high) {
			sort(index1, high);
		}
	}

	/**
	 * Sorts the population according to normalized fitness. The population
	 * array is destructively modified.
	 */
	void sortPopulationByFitness() {
		log.info("sortPopulationByFitness " + currentGeneration);

		sort(0, population.length - 1);

		for (int i = 0; i < population.length; i++) {
			log.warn(i + " " + population[i].normalizedFitness);
		}

	}

	/**
	 * This function has to evaluate the fitness for a specific individual. As a
	 * side-effect, it notfies any observers of the GP that an individual has
	 * been evaluated and supplies the test cases.
	 */
	void fitnessFunction(Individual ind, int individualNr) {
		// log.warn("fitnessFunction " + currentGeneration + " individual " +
		// ind);

		double rawFitness = 0.0;
		ind.hits = 0;
		RealPoint[] testCases = new RealPoint[fitnessCases.get().length];
		RealPoint valueFromProgram = null;

		log.warn("ind " + ind.program.toString(0));
		initializeState();
		for (int index = 0; index < fitnessCases.get().length; index++) { // fitness
																			// cases
																			// represents
																			// the
																			// 20
																			// point
																			// originally
																			// wanted
			double x = fitnessCases.get()[index].x;
			double y = fitnessCases.get()[index].y; // get "fitness point" from
													// gate input
			// double valueFromProgram = ind.program.eval(x); // TODO - does
			// eval(x) BUT gets the valueFromProgram from a call back with
			// coordinates
			double t = ind.program.eval(x);
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Rectangle r = waitValidForData();
			// Rectangle r = new Rectangle(30,30,10,10);
			valueFromProgram = new RealPoint(r.x + r.width / 2, r.y + r.height / 2);
			// log.error(valueFromProgram);
			double deltaX = valueFromProgram.x - x;
			double deltaY = valueFromProgram.y - y;
			double difference = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
			rawFitness = rawFitness + difference;
			log.error("input " + x + " " + valueFromProgram.x + "," + valueFromProgram.y + " d " + (int) difference);
			// if (difference < 0.01) {
			if (difference < 10) {
				ind.hits++;
			}
			// testCases[index] = new RealPoint(x, valueFromProgram);
			testCases[index] = valueFromProgram;
		}
		// log.warn("ind " + ind.program.toString(0));
		try {
			outfile.write(individualNr + " " + (int) valueFromProgram.x + "," + (int) valueFromProgram.y + " " + rawFitness + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		myService.invoke("publishInd", new GPMessageEvaluatingIndividual(currentGeneration, individualNr, testCases, ind.standardizedFitness, rawFitness));

		ind.standardizedFitness = rawFitness;
		log.warn("generation " + currentGeneration + " individualNr " + individualNr);

		// notifyObservers(new GPMessageEvaluatingIndividual(
		// currentGeneration, individualNr, testCases));
	}

	boolean interrupted = false;
	BlockingQueue<Rectangle> footData = new LinkedBlockingQueue<Rectangle>();

	public Rectangle waitValidForData() {
		Rectangle r1 = null;
		Rectangle r0 = null;

		try {
			footData.clear(); // clear the data for next sample
			// get two sample points
			// if they are the same - we have stabilized

			while (!interrupted) {
				r0 = footData.take();
				r1 = footData.take();

				if (r1.x == r0.x && r1.y == r0.y && r1.width == r0.width && r1.height == r0.height) {
					return r1;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void evalCallBack(Rectangle r) {
		try {
			footData.put(r);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	void initializeState() {
		myService.send("knee", "moveTo", 62);
		myService.send("hip", "moveTo", 82);
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @returns <code>true</code> if the GP algorithm should terminate
	 */
	boolean terminationPredicate() {
		log.info("terminationPredicate " + currentGeneration);
		return currentGeneration >= MAX_GENERATIONS;
	}

	FileWriter outfile = null;

	/**
	 * Breeds a new population from an old one, thus stepping from one
	 * generation to the next.
	 */
	void evolve() {
		log.warn("EVOLVE ! " + currentGeneration);

		try {
			outfile = new FileWriter("data.txt");
			Individual bestOfGeneration;
			if (currentGeneration > 0) {
				breedNewPopulation();
			}
			zeroizeFitnessMeasuresOfPopulation();
			evaluateFitnessOfPopulation();
			outfile.close();
			// outfile = new FileWriter("data.1.txt");
			// evaluateFitnessOfPopulation();
			// outfile.close();
			normalizeFitnessOfPopulation();
			// Sort the population so that the roulette wheel selection is easy:
			sortPopulationByFitness();

			// Keep track of best-of-run individual:
			bestOfGeneration = population[0];
			if (bestOfRunIndividual == null || // IF THIS IS THE NEW BEST - IT
												// WILL EVAL IT HERE!
					bestOfRunIndividual.standardizedFitness > bestOfGeneration.standardizedFitness) {
				bestOfRunIndividual = bestOfGeneration.copy();
				generationOfBestOfRunIndividual = currentGeneration;
				RealPoint[] testCases = new RealPoint[fitnessCases.get().length];
				initializeState();
				for (int i = 0; i < fitnessCases.get().length; i++) {
					double x = fitnessCases.get()[i].x;
					// double y = bestOfRunIndividual.program.eval(x); // TODO -
					// eval is the evaluation - this needs feedback from other
					// systems
					double t = bestOfRunIndividual.program.eval(x);
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Rectangle r = waitValidForData();
					// Rectangle r = new Rectangle(30,30,10,10);
					RealPoint valueFromProgram = new RealPoint(r.x + r.width / 2, r.y + r.height / 2);

					testCases[i] = valueFromProgram;
				}

				log.warn(bestOfRunIndividual.program.toString(0));

				// TODO deprecate all addListener Observers - change to publish
				// message
				GPMessageBestFound gpm = new GPMessageBestFound(currentGeneration, bestOfRunIndividual.program.toString(0), testCases, bestOfRunIndividual.adjustedFitness,
						bestOfRunIndividual.standardizedFitness);
				myService.invoke("publish", gpm);
				/*
				 * notifyObservers(new GPMessageBestFound(currentGeneration,
				 * bestOfRunIndividual.program.toString(0), testCases,
				 * bestOfRunIndividual.adjustedFitness));
				 */
			}

			double[] fitness = new double[population.length];
			for (int i = 0; i < fitness.length; i++) {
				fitness[i] = population[i].adjustedFitness;
			}

			log.error("fitness " + fitness);
			notifyObservers(new GPMessageEvaluatingPopulation(currentGeneration, fitness));

			currentGeneration++;
			// log.info()
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * @return an individual in the specified population whose normalized
	 *         fitness is greater than the specified value.
	 */
	Individual findFitnessProportionateIndividual(double afterThisFitness) {
		// All we need to do is count along the population from the
		// beginning adding up the fitness until we get past the
		// specified point:
		log.info("findFitnessProportionateIndividual " + currentGeneration);

		int indexOfSelectedIndividual;
		double sumOfFitness = 0.0;
		int index = 0;
		while (index < population.length && sumOfFitness < afterThisFitness) {
			// Sum up the fitness values.
			sumOfFitness = sumOfFitness + population[index].normalizedFitness;
			index++;
		}
		if (index >= population.length) {
			indexOfSelectedIndividual = population.length - 1;
		} else {
			indexOfSelectedIndividual = index - 1;
		}
		return population[indexOfSelectedIndividual];
	}

	/**
	 * @return picks some individuals from the population at random and returns
	 *         the best one.
	 */
	Individual findIndividualUsingTournamentSelection() {

		log.info("findIndividualUsingTournamentSelection " + currentGeneration);

		int TournamentSize = Math.min(population.length, 7);

		Hashtable table = new Hashtable();
		while (table.size() < TournamentSize) {
			int key = random.nextInt(population.length);
			table.put(new Integer(key), population[key]);
		}
		Enumeration e = table.elements();
		Individual best = (Individual) e.nextElement();
		double bestFitness = best.standardizedFitness;
		while (e.hasMoreElements()) {
			Individual individual = (Individual) e.nextElement();
			double thisFitness = individual.standardizedFitness;
			if (thisFitness < bestFitness) {
				best = individual;
				bestFitness = thisFitness;
			}
		}
		return best;
	}

	/**
	 * @return finds an individual in the population according to the defined
	 *         selection method.
	 */
	Individual findIndividual() {
		log.info("findIndividual " + currentGeneration);

		Individual ind = null;
		switch (((MethodOfSelection) methodOfSelection.getSelection()).getValue()) {
		case MethodOfSelection.TOURNAMENT:
			ind = findIndividualUsingTournamentSelection();
			break;
		case MethodOfSelection.FITNESS_PROPORTIONATE:
			ind = findFitnessProportionateIndividual(random.nextDouble());
			break;
		}
		return ind;
	}

	/**
	 * @return some initial fitness cases, in text form, as pairs of x-y values
	 */
	String makeDefaultFitnessCases() {
		log.info("makeDefaultFitnessCases " + currentGeneration);

		// int NrOfFitnessCases = 20;
		// int NrOfFitnessCases = 4;
		String s = "// the data \n";
		s += "123 164\n";
		s += "249 164\n";
		s += "218 142\n";
		s += "130 142\n";
		/*
		 * for (int i = 0; i < NrOfFitnessCases; i++) { int x = i * 4; int y =
		 * x;
		 * 
		 * double x = (double)i / (NrOfFitnessCases - 1); if (x == 0.0) { x =
		 * 1.0e-7; } double y = 1.0 + (-3.0)*x + (3.0)*x*x*x*x; // The stream
		 * tokenizer does not handle numbers with an exponent. // Therefore,
		 * clamp small numbers like 1.2e-6 to 0: if (Math.abs(x) < 1.0e-5) { x =
		 * 0; }; if (Math.abs(y) < 1.0e-5) { y = 0; };
		 * 
		 * s = s + x + "  " + y + "\n"; }
		 */
		return s;
	}

	/**
	 * This is the body of the GP thread. It will be called by the thread's
	 * start() method.
	 */
	public void run() {
		log.info("run " + currentGeneration);

		// Prime the random generator with the same seed for each run,
		// so that we get reproducible results:
		random.setSeed(SEED);

		notifyObservers(new GPMessageFitnessCasesSet(fitnessCases.get()));

		generationOfBestOfRunIndividual = 0;
		bestOfRunIndividual = null;
		createPopulation();
		currentGeneration = 0;

		while (!terminationPredicate()) {
			try {
				evolve();
				thread.sleep(1);
				// thread.sleep() or thread.yield()?
				// I'm not sure about this. The JDK tutorial suggests yielding
				// so that other threads get a chance to run on host systems
				// without built-in thread support. In my experience with
				// Windows 95 and NT, user interface response got much worse
				// with yield()ing. I have to wait for feedback from users on
				// other platforms to get a clue.
			} catch (Exception e) {
				crash();
			}
		}
		// stop() will addListener any Observers that the GP has come to an end:
		stop();
	}

	/**
	 * @return the index'th subtree satisfying a specific condition, for example
	 *         the 3rd terminal of a tree. Subtrees are numbered from left to
	 *         right, depth first. The test function of the condition is applied
	 *         to all nodes of the tree. It must evaluate to true for the
	 *         desired kind of subtrees. getSubtree() returns not only a pointer
	 *         to the specified subtree, but also a pointer to the subtree's
	 *         parent and and index for its position in the list of the parent's
	 *         children. If the subtree is not found, the function returns null.
	 */
	TreeHook getSubtree(Program tree, int index, Condition cond) {
		log.info("getSubtree " + currentGeneration);

		// Hack: I need a global counter across the recursive calls to Walk().
		// In a Wirthian language, a VAR parameter would be fine.
		// In Java, I misuse a single-element array:
		int[] count = { index };
		return Walk(tree, count, cond, null, -1);
	}

	/**
	*
	*/
	private TreeHook Walk(Program tree, int[] count, Condition cond, Function parent, int childIndex) {
		log.info("Walk " + currentGeneration);

		if (cond.test(tree) && count[0] == 0) {
			return new TreeHook(tree, parent, childIndex);
		} else {
			TreeHook hook = null;
			if (tree instanceof Function) {
				Function func = (Function) tree;
				for (int a = 0; a < func.arg.length && count[0] > 0; a++) {
					if (cond.test(func.arg[a])) {
						count[0]--;
					}
					hook = Walk(func.arg[a], count, cond, func, a);
				}
			}
			return hook;
		}
	}

	/**
	 * Notifies any observers of changes in the GP algorithm's state. Observers
	 * must register with this Observable.
	 */
	public void notifyObservers(Object arg) {
		// Why should I call notifyObservers if nothing has changed?
		// I change the semantics of notifyObservers() here and always
		// set the 'changed' flag first:
		setChanged();
		super.notifyObservers(arg);
	}

}

/**
 * An extension of the built-in random generator to supply random integers as
 * well
 */
class Random extends java.util.Random {
	Random(int seed) {
		super(seed);
	}

	/**
	 * @return a random integer in the range 0 to n-1
	 */
	public int nextInt(int n) {
		return (int) (nextDouble() * n);
	}
}

/**
 * Just a data structure to hold the pointer to a subtree as well as to its
 * parent. This is needed to splice trees.
 */
class TreeHook {
	TreeHook(Program subtree, Function parent, int childIndex) {
		this.subtree = subtree;
		this.parent = parent;
		this.childIndex = childIndex;
	}

	Program subtree;
	Function parent;
	int childIndex;
}

/**
 * Just a helper class.
 */
abstract class Condition {
	abstract boolean test(Program p);
}

/**
*
*/
class IsProgram extends Condition {
	boolean test(Program p) {
		return (p instanceof Program);
	}
}

/**
*
*/
class IsFunction extends Condition {
	boolean test(Program p) {
		return (p instanceof Function);
	}
}

/**
 * This is the base class for all kinds of tree nodes.
 */
abstract class Program implements Cloneable {

	/**
	 * @param level
	 *            the current recursion level when descending a subtree
	 * @return the text representation of the subtree
	 */
	public abstract String toString(int level);

	/**
	 * @return a text description of the node. While toString() prints the value
	 *         of a subtree, getName() returns something like the class name of
	 *         this very node. getName() can be used to preset the choice lists
	 *         for the function and terminal sets.
	 */
	abstract String getName();

	/**
	 * @return a series of blanks proportional to the recursion level
	 */
	String indent(int level) {
		String s = new String();
		for (int i = 0; i < level; i++) {
			s = s + "  ";
		}
		return s;
	}

	/**
	 * @return the value of the subtree, evaluated at x, i.e. y = f(x)
	 */
	abstract double eval(double x);

	/**
	 * @return the number of nodes in this subtree
	 */
	abstract int countNodes();

	/**
	 * @return the number of nodes in this subtree satisfying a certain
	 *         condition
	 */
	abstract int countNodes(Condition cond);

	/**
	 * @return a deep copy of the subtree
	 */
	abstract protected Object clone();
}

/**
 * This is the base class for all kinds of terminal nodes.
 */
abstract class Terminal extends Program {

	int countNodes() {
		return 1;
	}

	int countNodes(Condition cond) {
		return (cond.test(this)) ? 1 : 0;
	}

}

/**
*
*/
class RandomConstant extends Terminal {
	// static final double MIN = -4.0;
	// static final double MAX = +4.0;
	static final double MIN = 0.0;
	static final double MAX = 180.0;
	double value;

	RandomConstant() {
		value = GP.random.nextDouble() * (MAX - MIN) + MIN;
	}

	private RandomConstant(double value) {
		this.value = value;
	}

	public String toString(int level) {
		return indent(level) + value;
	}

	protected Object clone() {
		return new RandomConstant(this.value);
	};

	String getName() {
		return "Random Constant";
	}

	double eval(double x) {
		return value;
	}

}

/**
*
*/
class Variable extends Terminal {

	public String toString(int level) {
		return indent(level) + "x";
	}

	protected Object clone() {
		return new Variable();
	}

	String getName() {
		return "x";
	}

	double eval(double x) {
		return x;
	}

}

/**
 * This is the base class for all kinds of function nodes (i.e. non-terminals).
 * Each kind of function can have a different number of arguments (i.e.
 * subtrees).
 */
abstract class Function extends Program {

	protected Program[] arg;
	public Service myService = null;

	public Function(Service s) {
		// TODO - not the most graceful WHAM WHAM WHAM! - I guess that'll work
		// and provide access to all Service fns
		myService = s;
	}

	public String toString(int level) {
		// Just to please the eye: If all arguments of the function
		// are terminals, I print a single line instead of spreading the
		// stuff tree-like.
		boolean allArgsAreTerminals = true;
		for (int a = 0; a < arg.length; a++) {
			allArgsAreTerminals = allArgsAreTerminals && (arg[a] instanceof Terminal);
		}
		String s = new String();
		if (!allArgsAreTerminals) {
			s = indent(level) + getName() + "(\n";
			int i = 0;
			while (i < arg.length - 1) {
				s = s + arg[i].toString(level + 1) + ",\n";
				i++;
			}
			if (i < arg.length) {
				s = s + arg[i].toString(level + 1) + "\n";
			}
			s = s + indent(level) + ")";
		} else {
			s = indent(level) + getName() + "(";
			int i = 0;
			while (i < arg.length - 1) {
				s = s + arg[i].toString(0) + ",";
				i++;
			}
			if (i < arg.length) {
				s = s + arg[i].toString(0);
			}
			s = s + ")";
		}
		return s;
	}

	protected Object clone() {
		Function temp = null;
		try {
			// temp = (Function)getClass().newInstance();

			Class cls = getClass();
			Constructor mc = cls.getConstructor(new Class[] { Service.class });
			temp = (Function) mc.newInstance(myService);

			for (int i = 0; i < arg.length; i++) {
				temp.arg[i] = (Program) arg[i].clone();
			}
		} catch (Exception e) {
		}
		return temp;
	}

	int countNodes() {
		int count = 1;
		for (int a = 0; a < arg.length; a++) {
			count = count + arg[a].countNodes();
		}
		return count;
	}

	int countNodes(Condition cond) {
		int count = (cond.test(this) ? 1 : 0);
		for (int a = 0; a < arg.length; a++) {
			count = count + arg[a].countNodes(cond);
		}
		return count;
	}

}

class Addition extends Function {

	public Addition(Service s) {
		super(s);
		arg = new Program[2];
	}

	public String getName() {
		return "add";
	}

	public double eval(double x) {
		return arg[0].eval(x) + arg[1].eval(x);
	}

}

class Subtraction extends Function {

	public Subtraction(Service s) {
		super(s);
		arg = new Program[2];
	}

	public String getName() {
		return "sub";
	}

	public double eval(double x) {
		return arg[0].eval(x) - arg[1].eval(x);
	}

}

class Multiplication extends Function {

	public Multiplication(Service s) {
		super(s);
		arg = new Program[2];
	}

	public String getName() {
		return "mul";
	}

	public double eval(double x) {
		return arg[0].eval(x) * arg[1].eval(x);
	}

}

class Division extends Function {

	public Division(Service s) {
		super(s);
		arg = new Program[2];
	}

	public String getName() {
		return "div";
	}

	public double eval(double x) {
		double divisor = arg[1].eval(x);
		return (divisor == 0 ? 1 : arg[0].eval(x) / divisor);
	}

}

class Sine extends Function {

	public Sine(Service s) {
		super(s);
		arg = new Program[1];
	}

	public String getName() {
		return "sin";
	}

	public double eval(double x) {
		return Math.sin(arg[0].eval(x));
	}

}

class Cosine extends Function {

	public Cosine(Service s) {
		super(s);
		arg = new Program[1];
	}

	public String getName() {
		return "cos";
	}

	public double eval(double x) {
		return Math.cos(arg[0].eval(x));
	}

}

class Exp extends Function {

	public Exp(Service s) {
		super(s);
		arg = new Program[1];
	}

	public String getName() {
		return "exp";
	}

	public double eval(double x) {
		double v = arg[0].eval(x);
		if (v > 100) {
			v = 100;
		} else if (v < -100) {
			v = -100;
		}
		return Math.exp(v);
	}

}

class MoveHip extends Function {

	public MoveHip(Service s) {
		super(s);
		arg = new Program[1];
	}

	public String getName() {
		return "hip";
	}

	public double eval(double x) {
		// return Math.cos(arg[0].eval(x));

		int X = (int) arg[0].eval(x);
		// Log.error("moveHip " + X);
		myService.send("hip", "moveTo", X);
		return x;
	}

}

class MoveKnee extends Function {

	public MoveKnee(Service s) {
		super(s);
		arg = new Program[1];
	}

	public String getName() {
		return "knee";
	}

	public double eval(double x) {
		// return Math.cos(arg[0].eval(x));
		int X = (int) arg[0].eval(x);
		// Log.error("moveKnee " + X);
		myService.send("knee", "moveTo", X);
		return x;
	}

}

class Sleep extends Function {

	public Sleep(Service s) {
		super(s);
		arg = new Program[1];
	}

	public String getName() {
		return "sleep";
	}

	public double eval(double x) {
		// return Math.cos(arg[0].eval(x));
		int X = (int) arg[0].eval(x);
		try {
			Thread.sleep(X);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return x;
	}

}

class First extends Function {

	public First(Service s) {
		super(s);
		arg = new Program[2];
	}

	public String getName() {
		return "first";
	}

	public double eval(double x) {
		return arg[0].eval(x);
	}

}

class Second extends Function {

	public Second(Service s) {
		super(s);
		arg = new Program[2];
	}

	public String getName() {
		return "second";
	}

	public double eval(double x) {
		return arg[1].eval(x);
	}

}

/**
 * This class combines a program and its computed fitness values.
 */
class Individual {
	public Program program;
	double standardizedFitness;
	double adjustedFitness;
	double normalizedFitness;
	int hits;

	Individual(Program p) {
		program = (Program) p.clone();
		standardizedFitness = 0.0;
		adjustedFitness = 0.0;
		normalizedFitness = 0.0;
		hits = 0;
	}

	Individual copy() {
		Individual newInd = new Individual(this.program);
		newInd.standardizedFitness = this.standardizedFitness;
		newInd.adjustedFitness = this.adjustedFitness;
		newInd.normalizedFitness = this.normalizedFitness;
		newInd.hits = this.hits;
		return newInd;
	}

}

/**
 * Just a helper class: it combines the class template and its text description
 * of a node. It is used to build the choice lists for function and terminal
 * sets.
 */
class ProgramChoice {
	private Class cls;
	private String text;

	ProgramChoice(String text, Class cls) {
		this.cls = cls;
		this.text = text;
	}

	public String toString() {
		return text;
	}

	Class value() {
		return cls;
	}
}

/**
 * Just a helper class: it combines the enumeration value and its text
 * description of a generation method. It is used to build the choice list for
 * the generation method menu.
 */
class MethodOfGeneration {
	static final int GROW = 0;
	static final int FULL = 1;
	static final int RAMPED_HALF_AND_HALF = 2;

	private String text;
	private int value;

	MethodOfGeneration(String text, int value) {
		this.text = text;
		this.value = value;
	}

	int getValue() {
		return this.value;
	}

	public String toString() {
		return this.text;
	}
}

/**
 * Just a helper class: it combines the enumeration value and its text
 * description of a selection method. It is used to build the choice list for
 * the selection method menu.
 */
class MethodOfSelection {
	static final int FITNESS_PROPORTIONATE = 0;
	static final int TOURNAMENT = 1;

	private String text;
	private int value;

	MethodOfSelection(String text, int value) {
		this.text = text;
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	public String toString() {
		return this.text;
	}
}

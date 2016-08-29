package org.myrobotlab.genetic;

import java.util.ArrayList;
import java.util.Random;


public class GeneticAlgorithm {

  double recombinationRate = 0.7;
  double mutationRate = 0.001;
  int populationPoolSize = 100;
  ArrayList<Chromosome> chromosomes = new ArrayList<Chromosome>();
  int geneSize = 8;
  private Genetic geneticClass;
  public GeneticAlgorithm() {
    
  }

  public GeneticAlgorithm(int populationSize, int genomeSize, int geneSize, double recombinationRate, double mutationRate) {
    // TODO Auto-generated constructor stub
    populationPoolSize = populationSize;
    this.recombinationRate = recombinationRate;
    this.mutationRate = mutationRate;
    this.geneSize = geneSize;
    for (int i = 0; i < populationSize; i++) {
      Chromosome chromo = new Chromosome(genomeSize * geneSize);
      chromosomes.add(chromo);
    }
  }
  
  public Chromosome doGeneration(int generation) {
    //decode the genes
    //calculate the fitness of the pool
    Chromosome bestFit=chromosomes.get(0);
    for (int i = 0; i < generation; i++) {
      geneticClass.decode(chromosomes);
      geneticClass.calcFitness(chromosomes);
      Double totalFitness=0.0;
      for (Chromosome chromosome : chromosomes) {
        totalFitness += chromosome.fitness;
        if (bestFit != null && chromosome.fitness > bestFit.fitness) {
          bestFit = chromosome;
        }
      }
      if (i != generation-1){ //last iteration, no need to mutate
        ArrayList<Chromosome> newPool = new ArrayList<Chromosome>();
        newPool.add(bestFit);
        while (newPool.size() < populationPoolSize) {
          Chromosome c1 = new Chromosome(RandomWheel(chromosomes, totalFitness));
          Chromosome c2 = new Chromosome(RandomWheel(chromosomes, totalFitness));
          newPool.add(c1.recombine(c2, recombinationRate).mutate(mutationRate));
          newPool.add(c2.recombine(c1, recombinationRate).mutate(mutationRate));
        }
        chromosomes = newPool;
      }
    }
    return bestFit;
  }
  
  private Chromosome RandomWheel(ArrayList<Chromosome> chromosomes, Double totalFitness) {
    // TODO Auto-generated method stub
    Random rand = new Random();
    double randomNumber = rand.nextInt(totalFitness.intValue());
    Double fitnessCount = 0.0;
    for (Chromosome chromosome : chromosomes) {
      fitnessCount += chromosome.fitness;
      if (randomNumber < fitnessCount) {
        return chromosome;
      }
    }
    return null;
  }

  public void setGeneticClass(Genetic geneticClass) {
    // TODO Auto-generated method stub
    this.geneticClass = geneticClass;
  }

}

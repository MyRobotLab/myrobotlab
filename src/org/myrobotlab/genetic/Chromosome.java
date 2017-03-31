package org.myrobotlab.genetic;

import java.util.ArrayList;
import java.util.Random;

public class Chromosome {
  String genome;
  double fitness;
  ArrayList<Object> decodedGenome;
  Chromosome(int genomeSize) {
    Random rand = new Random();
    genome = new String();
    for (int i=0; i<genomeSize; i++){
      int randomValue = rand.nextInt(2);
      String catValue = ((Integer)randomValue).toString();
      genome += catValue;
    }
  }
  public Chromosome(Chromosome chromosome) {
    // TODO Auto-generated constructor stub
    this.decodedGenome = new ArrayList<Object>(chromosome.decodedGenome);
    this.genome = new String(chromosome.genome);
  }
  public Chromosome() {
    // TODO Auto-generated constructor stub
  }
  /**
   * @return the genome
   */
  public String getGenome() {
    return genome;
  }
  /**
   * @return the fitness
   */
  public double getFitness() {
    return fitness;
  }
  /**
   * @return the decodedGenome
   */
  public ArrayList<Object> getDecodedGenome() {
    return decodedGenome;
  }
  /**
   * @param genome the genome to set
   */
  public void setGenome(String genome) {
    this.genome = genome;
  }
  /**
   * @param d the fitness to set
   */
  public void setFitness(double d) {
    this.fitness = d;
  }
  /**
   * @param decodedGenome the decodedGenome to set
   */
  public void setDecodedGenome(ArrayList<Object> decodedGenome) {
    this.decodedGenome = decodedGenome;
  }
  public Chromosome recombine(Chromosome c, double recombinationRate) {
    // TODO Auto-generated method stub
    Random rand = new Random();
    Chromosome chromosome = new Chromosome();
    if (rand.nextDouble() < recombinationRate){
      int randomNumber = rand.nextInt(genome.length()-1);
      chromosome.genome = genome.substring(0, randomNumber+1) + c.genome.substring(randomNumber+1);
    }
    else {
      chromosome.genome = genome;
    }
    return chromosome;
  }
  public Chromosome mutate(double mutationRate) {
    // TODO Auto-generated method stub
    Random rand = new Random();
    String newGenome = new String();
    for (int i = 0; i < genome.length(); i++){
      if (rand.nextDouble() < mutationRate){
        if(genome.charAt(i) == '1') newGenome += "0";
        else newGenome += "1";
      }
      else newGenome += genome.substring(i,i+1);
    }
    Chromosome ret = new Chromosome();
    ret.setGenome(newGenome);
    return ret;
  }
}
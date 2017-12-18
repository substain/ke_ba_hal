package genAlgo;

import java.util.ArrayList;
import java.util.Random;

public class GenAlgo {
	
	public static final int NUM_ATTS = 8;
	public static final int ATT_MAX = 30;
	public static final int MAX_POPULATION_SIZE = 8;
	public static final int NUM_ITERATIONS = 8;
	public static final int NUM_PARENT_INDV = 4; //should be even
	public static final int NUM_CHILD_MUT = 1; //should not be larger than NUM_PARENT_INDV/2

	public static Random randNum;
	int numAtts;
	int[] atts;
    ArrayList<Individual> population;
    ArrayList<Individual> restPopulation;
    ArrayList<Individual> parents;
    ArrayList<Individual> children;

	public GenAlgo(ArrayList<Individual> pop) {

		population = pop;
	}
	
	public void compute(int genIterations) {
		for(int i = 0; i < genIterations; i++) {
			
			computeGen();
    		System.out.println("Generation "+i+":");
    		printGen(population);
		}
	}
	
	public void computeGen() {
	    ArrayList<Individual> newPopulation = new ArrayList<>();
	    select(); //set individuals with the highest score as parents;
	    recombine(); //create children from the selected parents (and add the parents to the new population)
	    mutate(); //mutate some of the children (and add all childs to new population)
	    finalize(); // add remaining individuals of the old population
	}


	public void select() { //currently: select the individuals with the best score and add them to "parents"
		int parentsChosen = 0;
		restPopulation = population;
		parents = new ArrayList<>();
		while(parentsChosen < NUM_PARENT_INDV) {
			int highestScore = -100;
			int h_id = 0;
			for(Individual ip : restPopulation) {
				int currentScore = getScore8(ip.attributes);
				if(currentScore > highestScore) {
					highestScore = currentScore;
					h_id = ip.getId();
				}
			}
			for(int i = 0; i<restPopulation.size(); i++) {
				Individual ind_i = restPopulation.get(i);
				if(ind_i.getId() == h_id) {
					parents.add(ind_i);
					restPopulation.remove(i);
					break;
				}
			}
			
			parentsChosen++;
		}
	}
	
	
	public void recombine() {
		int numParentsLeft = parents.size();
		population = new ArrayList<>();
		children = new ArrayList<>();
		Individual p1, p2;
		while(numParentsLeft > 1) {
			int ind_p1 = randNum.nextInt(parents.size());
			p1 = parents.get(ind_p1);
			population.add(p1);
			parents.remove(ind_p1);

			int ind_p2 = randNum.nextInt(parents.size());
			p2 = parents.get(ind_p2);
			population.add(p2);
			parents.remove(ind_p2);
			
			children.add(recombineRand(p1, p2));
			
			numParentsLeft -= 2;
		}

	}
	
	public Individual recombineRand(Individual parent1, Individual parent2) {
		int[] p1atts = parent1.getAttributes();
		int[] p2atts = parent2.getAttributes();

		int attsFromP1 = NUM_ATTS/2;
		int attsFromP2 = NUM_ATTS-attsFromP1;
		int[] childAtts = new int[NUM_ATTS];
		for(int i = 0; i < childAtts.length; i++) {
			boolean takeP1atts = randNum.nextBoolean();
			if((takeP1atts && attsFromP1 > 0) || attsFromP2 == 0) {
				childAtts[i] = p1atts[i];
				attsFromP1--;
			} else {
				childAtts[i] = p2atts[i];
				attsFromP2--;
			}
		}
		
		Individual child = new Individual(childAtts);
		return child;
		
	}
	
	public void mutate() {
		int childrenMutated = 0;
		Individual childm;
		while(childrenMutated < NUM_CHILD_MUT && children.size() > 0){
			int ind_chm = randNum.nextInt(children.size());
			childm = children.get(ind_chm);
			children.remove(ind_chm);
			childm.mutate();
			population.add(childm);
		}
		
		for(Individual child_norm : children) {
			population.add(child_norm);
		}
		children.clear();

	}
	
	public void finalize() {
		while(population.size() < MAX_POPULATION_SIZE) {
			int highestScore = -100;
			int h_id = 0;
			for(Individual ip : restPopulation) {
				int currentScore = getScore8(ip.attributes);
				if(currentScore > highestScore) {
					highestScore = currentScore;
					h_id = ip.getId();
				}
			}
			for(int i = 0; i<restPopulation.size(); i++) {
				Individual ind_i = restPopulation.get(i);
				if(ind_i.getId() == h_id) {
					population.add(ind_i);
					restPopulation.remove(i);
					break;
				}
			}
		}
		restPopulation.clear();
	}
	
	
    public static void main(final String[] args) {
    	randNum = new Random(System.currentTimeMillis());

    	//generate a random population
    	ArrayList<Individual> population = new ArrayList<>();
    	for(int i = 0; i < MAX_POPULATION_SIZE; i++) {
        	int[] attrDistr = createRandomIntArray(NUM_ATTS);
        	Individual newInd = new Individual(attrDistr);
        	population.add(newInd);
    	}
    	GenAlgo test = new GenAlgo(population);
    	test.compute(NUM_ITERATIONS);
    }
    
    
    
    public static int[] createRandomIntArray(int attNum) {
    	int[] newData = new int[attNum];
    	for(int i = 0; i < newData.length; i++) {
    		newData[i] = randNum.nextInt(ATT_MAX);
    	}
    	return newData;
    }
    
    //a rather random score function
    public static int getScore8(int[] atts) {
    	int score = 0;
    	if(atts[7] > atts[6]) {
    		score -= (int) 1/3*ATT_MAX;
    	}
    	score += (int)(Math.sqrt((atts[3]*atts[3])+(atts[6]*atts[6])) - atts[1]/2);
    	score += (int) (atts[5]* 1.5);
    	score -= (atts[4]*atts[2] - atts[1]/2);
    	score -= (int) (0.5*atts[6]);
    	score += (int) (0.3*atts[7]);
    	if(atts[1] > atts[5]) {
    		score += (int) 1/4*ATT_MAX;
    	}
    	return score;
    }
    
    public static void printInd(Individual indv) {
    	int[] atts = indv.getAttributes();
    	String printStr = "Individual " + indv.getId() +":";
    	for(int i = 0; i < atts.length; i++) {
    		printStr += atts[i] + " ";
    	}
    	printStr += " - score: " + getScore8(atts);
    	
    	System.out.println(printStr);
    	
    }
    
    public static void printGen(ArrayList<Individual> population) {
    	for(Individual i : population) {
    		printInd(i);
    	}
    }
   
}

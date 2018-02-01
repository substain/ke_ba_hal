package genAlgo;



import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Random;

import hlt.Control;
import hlt.Task;

public class HaliteGenAlgo {
	
	//main arguments
	public static final int NUM_INIT_ARGS = 3;
	public static final int NUM_DEF_ARGS = 0;
	public static final int NUM_CONT_ARGS = 1;

	public static final int NUM_DEF_ITS = 6;
	public static final int NUM_DEF_BATS = 5;
	public static final String STR_DEF_NAME = "DefaultRun";
	
	public static final int NUM_MIN_BATCHES = 1;
	public static final int NUM_MIN_ITERATIONS = 1;
	public static final int MIN_BOT_COUNT = 8;
	
	
	public static final int NUM_INDV = 16;

	//in the current version, attributes shift from the start-distribution to the final distribution (= 2 different settings)
	public static final int NUM_ATT_SETTINGS_PER_GAME = 1; 
	
	public static final int NUM_ATTS = Control.NUM_ATTS;
	
	public static final int ATT_MAX_INT = 50;
	public static final double ATT_MAX = 1.0;

	
	public static final int NUM_CHILDS = 5;
	public static final int MAX_CHILD_MUT = 3; //should not be larger than NUM_CHILDS
	public static final int CHILD_MUTATE_DIV = 2; // probability to mutate a child is 1/CHILD_MUTATE_DIV
	
	public static final int NUM_4PL_MATCHES = 1;
	//io defaults
	
	public static final boolean USES_DISTRIBUTION = true;
	
	public static final String GAIT_FILENAME = "gaitinfo.txt";
	public static final String SAFE_BOTS_FILENAME = "safebots.txt";
	public static final String EXT_BOTS_FILENAME = "extbots.txt";

	//public static final String MATCHUP_INIT = "";
	public static final int MATCHES_PER_EXT_ROUND = 2;
	public static final int MATCHES_PER_TOURN_ROUND = 5;
	//public static final int MATCH_ARGS_TOURN_ROUND = 3;


	public static final String GA_BOT_JAVANAME = "ModifiedBot.java";
	public static final String GA_BOT_CLASSNAME = "ModifiedBot";
	
	
	public static final String BOT_PREFIX = "GABot";
	public static final String SAFE_BOT_PREFIX = "SGABot";


	public static final int NEXT_IT = 0;
	public static final int NEXT_BA = 1;

	
	public static final int GAIT_LINES = 3;
	public static final int GAIT_INIT_ARGS = 5;
	private static final double PARENT_W_FACTOR = 0.3;
	private static final int NUM_MAX_SBS = 2;

	public static Random randNum;
	
	String currentRunID;
	
	//int numIndividuals;
    
    ArrayList<String> bots;
    ArrayList<String> futureBots;
    ArrayList<String> safeBots;
    ArrayList<String> externBotList;
    
    ArrayList<Integer> rankings;
    double[] parentWeightDistr;
	int numIterations;
	int iteration;
	int numBatches;
	int batch;
	GAFileHandler ioHandler;
	//boolean initrun;
	
	int numAtts;
	//int[] atts;
	//ArrayList<LinkedList<Double>> popScores;
    ArrayList<Individual> population;
    ArrayList<Individual> restPopulation;
    ArrayList<Individual> parents;
    ArrayList<Individual> children;
    int avoidParent1ID;
    int avoidParent2ID;

    int bestIndID;
	private int nextIteration;
	private int nextBatch;
    
    /**
     * creation Constructor
     * @param botNames given bot Names
     */
	public HaliteGenAlgo(int numIterations, int numBatches, String currentRunName){
		ioHandler = new GAFileHandler();
		//initrun = true;
		//numIndividuals = numBots;
		this.numIterations = numIterations;
		iteration = 0;
		this.numBatches = numBatches;
		batch = 0;
		currentRunID = currentRunName;
		safeBots = new ArrayList<>();
		externBotList = new ArrayList<>();

		GAFileHandler.clearAllScoresFolder();
    	//generate a random population
    	population = new ArrayList<>(NUM_INDV);
    	
    	//createPopulation();
    	if(Objects.equals(currentRunName, new String("cont"))) {
        	System.out.println("CONTINUE RUN, LOADING OLD POPULATION");

    		readOldPopulationAtts();

    	} else if(Objects.equals(currentRunName, new String("mixed"))){
        	System.out.println("MIXED RUN, CREATING NEW POPULATION");

    		readHalfOldPopulationAtts();
    	} else {
        	System.out.println("STANDARD RUN, CREATING NEW POPULATION");

        	createPopulation3Distr();
    	}

    	
    	//printGen(population);
    	
    	bots = GAFileHandler.getBotNames(0,0);
    	
    	createGAITinfo(); //set safeBots + externBotList
    	
    	ioHandler.createMatchupFile(bots, safeBots, externBotList);
    	//createMatchesSh();
    	
    	saveCurrentBotAtts();
	}

	//extern bots convention: write the name of each Bot(ABcd123.java) in configs/static_configs/extbots.txt like this:
	// ABcd123.java Klvb33.java ExampleBot.java
	// Bots are assumed to be placed in 
	

	public void readPopulationAtts(){
		population = new ArrayList<>(NUM_INDV);
    	for(int i = 0; i < NUM_INDV; i++) {
        	double[] attrDistr = GAFileHandler.readBotAtts(i, iteration, batch);
        	Individual newInd = new Individual(attrDistr, ATT_MAX);
        	population.add(newInd);
    	}

	}
	public void readHalfOldPopulationAtts(){
		population = new ArrayList<>(NUM_INDV);
    	int div_indv = (NUM_INDV/2)/4;

    	for(int i = 0; i < NUM_INDV; i++) {
    		if(i < NUM_INDV/2) {
            	double[] attrDistr = GAFileHandler.readOldBotAtts(i);
            	Individual newInd = new Individual(attrDistr, ATT_MAX);
            	population.add(newInd);
    		} else {
    			double[] attrDistr;
            	if(i < div_indv+NUM_INDV/2) {
                	attrDistr = createQWeightedRandomDA(NUM_ATTS, 0);

            	} else if (i < div_indv*2+NUM_INDV/2) {
                	attrDistr = createQWeightedRandomDA(NUM_ATTS, 1);

            	} else if (i < div_indv*3+NUM_INDV/2) {
                	attrDistr = createQWeightedRandomDA(NUM_ATTS, 2);

            	} else {
                	attrDistr = createQWeightedRandomDA(NUM_ATTS, 3);

            	}
            	attrDistr = Individual.normalizeA(attrDistr);
            	Individual newInd = new Individual(attrDistr, ATT_MAX);
            	population.add(newInd);
    		}

    	}

	}
	

	public void readOldPopulationAtts(){
		population = new ArrayList<>(NUM_INDV);
    	for(int i = 0; i < NUM_INDV; i++) {
        	double[] attrDistr = GAFileHandler.readOldBotAtts(i);
        	Individual newInd = new Individual(attrDistr, ATT_MAX);
        	population.add(newInd);
    	}

	}
    
    private void createPopulation() {
    	for(int i = 0; i < NUM_INDV; i++) {
        	double[] attrDistr = createRandomDoubleArray(NUM_ATTS);
        	attrDistr = Individual.normalizeA(attrDistr);
        	Individual newInd = new Individual(attrDistr, ATT_MAX);
        	population.add(newInd);
    	}
		
	}
    

    private void createPopulationHalfDistr(int restIndNum) {
    	int div_indv = restIndNum/4;

    	for(int i = 0; i < NUM_INDV; i++) {
    		double[] attrDistr;
        	if(i < div_indv) {
            	attrDistr = createQWeightedRandomDA(NUM_ATTS, 0);

        	} else if (i < div_indv*2) {
            	attrDistr = createQWeightedRandomDA(NUM_ATTS, 1);

        	} else if (i < div_indv*3) {
            	attrDistr = createQWeightedRandomDA(NUM_ATTS, 2);

        	} else {
            	attrDistr = createQWeightedRandomDA(NUM_ATTS, 3);

        	}
        	attrDistr = Individual.normalizeA(attrDistr);
        	Individual newInd = new Individual(attrDistr, ATT_MAX);
        	population.add(newInd);
    	}
		
	}
    
    private void createPopulation3Distr() {
    	int div_indv = NUM_INDV/4;

    	for(int i = 0; i < NUM_INDV; i++) {
    		double[] attrDistr;
        	if(i < div_indv) {
            	attrDistr = createQWeightedRandomDA(NUM_ATTS, 0);

        	} else if (i < div_indv*2) {
            	attrDistr = createQWeightedRandomDA(NUM_ATTS, 1);

        	} else if (i < div_indv*3) {
            	attrDistr = createQWeightedRandomDA(NUM_ATTS, 2);

        	} else {
            	attrDistr = createQWeightedRandomDA(NUM_ATTS, 3);

        	}
        	attrDistr = Individual.normalizeA(attrDistr);
        	Individual newInd = new Individual(attrDistr, ATT_MAX);
        	population.add(newInd);
    	}
		
	}

	/**
     * update Constructor
     * @param botNames given bot Names
     * @throws IOException 
     */
	public HaliteGenAlgo(){
		ioHandler = new GAFileHandler();
		//initrun = false;
		boolean cont = getGAITinfo(); //reads gait file data and sets most data for this iteration
		if(!cont) {
			return;
		}
		
		bots = GAFileHandler.getBotNames(iteration, batch);
		futureBots = GAFileHandler.getBotNames(nextIteration, nextBatch);

		readPopulationAtts();

		//readPopulationScores();
		rankings = GAFileHandler.readLastRankings();
		
		compute();
		
		
		if(batch != nextBatch) {
			if(safeBots.size() > NUM_MAX_SBS) {
				safeBots.remove(0);
			}
			safeBots.add(bots.get(0));
		}
		ioHandler.writeBotAtts(iteration, batch, bestIndID, true, population.get(0).getAttributes());
    	ioHandler.createMatchupFile(futureBots, safeBots, externBotList);
		updateGAITinfo();
		saveAsNextPopulationAtts();
	}




	
	private void compute() {
	    select(); //set individuals with the highest score for the new population;
	    recombine(); //create children from the selected parents
	    mutate(); //mutate some of the children (and add all childs to new population)		System.out.println("HaliteGenAlgo:current Generation:");
		System.out.println("population, after");
	    printGen(population);
	}



	private void select() { //currently: select the individuals with the best score and add them to "parents"
		//System.out.println("Popscore size = " + popScores.size() + ", rpopulation.size = " + population.size());
		System.out.println("population, before");
		printGen(population);
		//System.out.println("select: rankings:");
		//printIntArrayList(rankings);
		restPopulation = sortByFitness(population);
		population = new ArrayList<>(NUM_INDV);
		int numSurvivingIndv = NUM_INDV - NUM_CHILDS;

		//System.out.println("restpopulation, after sorting: ");
		//printGen(restPopulation);

		//parents = new ArrayList<>(NUM_PARENT_INDV);
		for(int i = 0; i < numSurvivingIndv; i++) {
			population.add(restPopulation.get(i));
		}
	}
	
	/**
	 * sorts by ranking
	 * @param population to be sorted
	 * @return
	 */
	public ArrayList<Individual> sortByFitness(ArrayList<Individual> population){ // ALSO writes bestIndID and parentWeightDistr
		ArrayList<Individual> sortedPopulation = new ArrayList<>(NUM_INDV);
		int indvIndex = 0;
		bestIndID = rankings.get(0);
		ArrayList<Integer> botIds = new ArrayList<>();
		
		for(int j = 0; j < rankings.size(); j++) {
			indvIndex = rankings.get(j);
			botIds.add(indvIndex);
			sortedPopulation.add(population.get(indvIndex));
		}
		
		parentWeightDistr = new double[sortedPopulation.size()];
		double pwdLength = (double) parentWeightDistr.length;

		//first round rankings
		if(externBotList.isEmpty() && safeBots.isEmpty()) {
			for(int i = 0; i < parentWeightDistr.length; i++) {
				parentWeightDistr[i] = 1/pwdLength;
			}
		} else {
			//System.out.println("HGA: sortByFitness : sortedpopulation:");
			//printGen(sortedPopulation);
			ArrayList<Double> firstScores = GAFileHandler.collectInitScores(botIds, iteration, batch, externBotList.size(),  safeBots.size());
			HashMap<Integer, Integer> frRanking = GAFileHandler.createRankings(botIds, firstScores, 1 , sortedPopulation.size());
			int maxRanking = Collections.max(frRanking.keySet());
			int halfRanking = maxRanking / 2;
			for(int i = 0; i < parentWeightDistr.length; i++) {
				parentWeightDistr[i] = 1/pwdLength;
				parentWeightDistr[i] = parentWeightDistr[i] * ((double)((maxRanking-frRanking.get(i))-halfRanking)/pwdLength) * PARENT_W_FACTOR;
				parentWeightDistr = Individual.normalize(parentWeightDistr);
				//System.out.println("parentweightdistr: " +parentWeightDistr[i]);
			}
		}

		return sortedPopulation;
	}
	
	
	
	private void recombine() {
		children = new  ArrayList<>();
		int firstParent = 0;
		int secondParent = 0;

		for(int i = 0; i < NUM_CHILDS; i++) {
			
				//CHOOSE FIRST PARENT WITH PROBABILITY ACCORDING TO parentWeightDistr
				double firstParentD = randNum.nextDouble();
				double dsum = 0;
				for(int di = 0; di < parentWeightDistr.length; di++) {
					if(firstParentD < dsum+parentWeightDistr[di]) {
						firstParent = di;
						break;
					}
					dsum+=di;
				}
				 
				//SET UP secondParentWeightDistr BY ELIMINATING firstParent FROM THE DISTRIBUTION
			    double[] secondParentWeightDistr = new double[parentWeightDistr.length-1];
			    int spind = 0;
			    for(int sp = 0; sp < secondParentWeightDistr.length; sp++) {
			    	if(sp >= firstParent) {
			    		spind = sp + 1;
			    	} else {
			    		spind = sp;
			    	}
		    		secondParentWeightDistr[sp] = parentWeightDistr[spind];
			    }
			    secondParentWeightDistr = Individual.normalize(secondParentWeightDistr);
			    
				//CHOOSE SECOND PARENT WITH PROBABILITY ACCORDING TO secondParentWeightDistr
				double secondParentD = randNum.nextDouble();
				double d2sum = 0;
				for(int di = 0; di < secondParentWeightDistr.length; di++) {
					if(secondParentD < d2sum+secondParentWeightDistr[di]) {
						secondParent = di;
						break;
					}
					d2sum+=di;
				}


			children.add(Individual.recombineDistr(population.get(firstParent), population.get(secondParent)));

		}
		
		restPopulation.clear();
	}
	
	/*
	public Individual recombineRandDistr(Individual parent1, Individual parent2) {
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
		
		Individual child = new Individual(childAtts, ATT_MAX);
		return child;
		
	} */

	
	private void mutate() {
		int childrenMutated = 0;
		Individual childm;
		while(childrenMutated < MAX_CHILD_MUT && children.size() > 0){

			int ind_chm = randNum.nextInt(children.size());
			childm = children.get(ind_chm);
			int mutateThisChild = randNum.nextInt(CHILD_MUTATE_DIV); // probability of 1/CHILD_MUTATE_DIV to mutate this child
			if(mutateThisChild == 1) {
				childm.mutateDistr();
				childrenMutated++;
			}
			children.remove(ind_chm);
			population.add(childm);
		}
		
		for(Individual child_norm : children) {
			population.add(child_norm);
		}
		children.clear();

	}
	
	/*
	public void finalize() {
		while(population.size() < NUM_INDV && !population.isEmpty()) {
			int highestScore = Integer.MIN_VALUE;
			int h_id = 0;
			for(int i = 0; i < restPopulation.size(); i++) {
				//double currentScore = getAverage(popScores.get(restPopulation.get(i).getId()));
				int currentScore = getScoreByRanking(i, rankings);
				if(currentScore > highestScore) {
					highestScore = currentScore;
					h_id = restPopulation.get(i).getId();
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
		//restPopulation.clear();
	}*/


	public void createGAITinfo(){
		//Line 1
		//String currentRunID
		//int iteration
		//int numIterations;
		//int batch
		//int numBatches;
	 	int finished = 0;
	 	//int numIndividuals //REMOVED

	 	//Line 2
	 	//num_safe_bots = priorSafeBots.size()
	 	safeBots = ioHandler.readPriorSafeBots();

	 	//Line 3
	 	//num_extern_bots = externBots.size()
	 	externBotList = ioHandler.readExternBots();
	 	
	 	ioHandler.writeToGAIT(currentRunID, iteration, numIterations, batch, numBatches, finished, safeBots, externBotList);
	}


	public void updateGAITinfo() {
		 
			//Line 1
			int finished = 0;
			if(nextBatch == -1) {
				finished = 1;
			}
			ioHandler.writeToGAIT(currentRunID, nextIteration, numIterations, nextBatch, numBatches, finished, safeBots, externBotList);
	}
	
	
	/*
	public void setNextIt(){
		nextIteration = iteration + 1;
		nextBatch = batch;
		if(nextIteration >= numIterations) {
			nextIteration = 0;
			nextBatch = batch + 1;
			if(nextBatch >= numBatches) {
				nextIteration = -1;
				nextBatch = -1;
			}
		}
	}*/
	
	

	
	public static int[] getNextItBa(int currentIt, int currentBa, int numIt, int numBa){
		int[] next = new int[2];
		
		next[NEXT_IT] = currentIt + 1;
		next[NEXT_BA] = currentBa;
		if(next[NEXT_IT] >= numIt) {
			next[NEXT_IT] = 0;
			next[NEXT_BA] = currentBa + 1;
			if(next[NEXT_BA] >= numBa) {
				next[NEXT_IT] = -1;
				next[NEXT_BA] = -1;
			}
		}
		
		return next;
	}


	public boolean getGAITinfo() {
		ioHandler.readGAITinfo();
		currentRunID = ioHandler.getGAITID();
		safeBots = ioHandler.getSafeBots();
		externBotList = ioHandler.getExternBots();
		ArrayList<Integer> gaitinit = ioHandler.getGaitInit();


		iteration = gaitinit.get(GAFileHandler.GAIT_I_IT);
		numIterations = gaitinit.get(GAFileHandler.GAIT_I_N_IT);
		batch = gaitinit.get(GAFileHandler.GAIT_I_BA);
		numBatches = gaitinit.get(GAFileHandler.GAIT_I_N_BA);
		int finished = gaitinit.get(GAFileHandler.GAIT_I_FIN);
    	if(finished == 1) {
    		
    		System.out.println("HaliteGenAlgo: The used GAIT file indicates that the algorith has finished.");
    		return false;
    	}		
    	//numIndividuals = gaitinit.get(5);
	    
	    System.out.println("HaliteGenAlgo: readResults: "+ currentRunID +","+ iteration +","+ numIterations +","+ batch +","+ numBatches +","+ NUM_INDV);
	    int[] next = getNextItBa(iteration, batch, numIterations, numBatches);
	    nextBatch = next[NEXT_BA];
	    nextIteration = next[NEXT_IT];
	    return true;
	}
	



	/*
	
	public void readPopulationScores() {
		popScores = new ArrayList<>(NUM_INDV);

		for(int i = 0; i < population.size(); i++) {
			LinkedList<Double> thisScores = GAFileHandler.readBotScores(i, iteration, batch);
			popScores.add(thisScores);
			System.out.println("score of ind " + i + " = " + getAverage(thisScores));

		}
		
	}

	*/
	

	
	public void saveCurrentBotAtts() {

		for(int i = 0; i < population.size(); i++) {
			ioHandler.writeBotAtts(iteration, batch, i, false, population.get(i).getAttributes());
		}
	}
	
	public void saveAsNextPopulationAtts() {
		for(int i = 0; i < population.size(); i++) {
			ioHandler.writeBotAtts(nextIteration, nextBatch, i, false, population.get(i).getAttributes());
		}
	}

	public static int getScoreByRanking(int ind, ArrayList<Integer> rankings) {
		return NUM_INDV - rankings.get(ind);
	}


	public LinkedList<Match> createMatchup(){ //creates 2pl matches, one planned and one random 4pl match per bot
		LinkedList<Match> matches = new LinkedList<>();

		for(int i = 0; i < NUM_INDV; i++) {
			int nid =  (i + 1 + iteration*3)%NUM_INDV;
			int nnid =  (i + 2 + iteration*3)%NUM_INDV;
			int nnnid = (i + 3 + iteration*3)%NUM_INDV;
			for(int gaid = 0; gaid < NUM_INDV; gaid++) {
				if(gaid != i && gaid != nid && gaid != nnid && gaid != nnnid) {
					matches.add(new Match(i, gaid, 0));
				}
			}
			
			for(int sbid = 0; sbid < safeBots.size(); sbid++) {
				matches.add(new Match(i, sbid, 1));
			}
			
			for(int ebid = 0; ebid < externBotList.size(); ebid++) {
				matches.add(new Match(i, ebid, 2));
			}
			
			// creates a random 4pl match
			int[] enemies = new int[3];
			int[] enemyTypes = new int[3];
			/*for(int enm = 0; enm < enm; i++) {
				if(possibleEnemies.isEmpty()) {
					resetPossibleEnemies();
				}
				int index = randNum.nextInt(possibleEnemies.size());
				if(possibleEnemies.get(index) == i) { // decrease chance to have 2 same bots playing against each other
					index = randNum.nextInt(possibleEnemies.size());
				}
				int eid = possibleEnemies.get(index);

				if(eid >= NUM_INDV + safeBots.size()) {
					eid -= NUM_INDV + safeBots.size();
					enemyTypes[enm] = 2;
				} else if (eid >= NUM_INDV) {
					eid -= NUM_INDV;
					enemyTypes[enm] = 1;
				} else {
					enemyTypes[enm] = 0;
				}
				enemies[enm] = eid;

			}*/
			
			matches.add(new Match(i, nid, 0, nnid, 0, nnnid, 0));

			matches.add(new Match(i, enemies[0], enemyTypes[0], enemies[1], enemyTypes[1], enemies[2], enemyTypes[2]));
			
		}
		
		return matches;
	}
	
	/*
	 * this should be started from the ke_ba_hal directory (where the .sh script is found too) due to file reading
	 */
    public static void main(final String[] args){
    	randNum = new Random(System.currentTimeMillis());

    	
    	if(args.length==NUM_INIT_ARGS) {
    		System.out.println("HaliteGenAlgo:Initial run");
        	//int numIndividuals = Integer.valueOf(args[0]);
        	int iterations = Integer.valueOf(args[0]);
        	int batches = Integer.valueOf(args[1]);
        	String runID = args[2];
        	if(iterations<NUM_MIN_ITERATIONS) {
        		System.out.println("HaliteGenAlgo:Error. Minimum iteration amount is " +NUM_MIN_ITERATIONS+", given:" + iterations);
        		return;
        	}
        	if(batches<NUM_MIN_BATCHES) {
        		System.out.println("HaliteGenAlgo:Error. Minimum batch amount is " +NUM_MIN_BATCHES+", given:" + batches);
        		return;
        	}

        	new HaliteGenAlgo(iterations, batches, runID);

    	} else if(args.length == NUM_DEF_ARGS) {

        	new HaliteGenAlgo(NUM_DEF_ITS, NUM_DEF_BATS, STR_DEF_NAME);

    		
    	} else if(args.length == NUM_CONT_ARGS) {
        	int iterations = Integer.valueOf(args[0]);
    		System.out.println("HaliteGenAlgo:Run " + iterations);
        	
        	new HaliteGenAlgo();

    	} else {
    		System.out.println("HaliteGenAlgo:Error. No valid number of arguments (init:"+NUM_INIT_ARGS+", cont:"+NUM_CONT_ARGS+")");
    		return;
    	}
    
    }
 
    
    public static double getAverage(LinkedList<Double> linkedlist) {
    	BigDecimal sum = BigDecimal.ZERO;
    	int count = 0;
    	for(Double sc : linkedlist) {
    		if(sc.isNaN()) {
    			continue;
    		}
    		sum = sum.add(BigDecimal.valueOf(sc));
    		count++;
    	}
    	if(count == 0) {
    		return 0;
    	}
    	double returnvalue = (sum.divide(BigDecimal.valueOf(count), MathContext.DECIMAL128)).doubleValue();
		//System.out.println("c = " + count + " , sum = " + sum + ", avg = " + returnvalue);

    	return returnvalue;
    }
    
	
    
    public static int[] createRandomIntArray(int attNum) {
    	int[] newData = new int[attNum];
    	for(int i = 0; i < newData.length; i++) {
    		newData[i] = randNum.nextInt(ATT_MAX_INT);
    	}
    	return newData;
    }
    
    public static double[] createRandomDoubleArray(int attNum) {
    	double[] newData = new double[attNum];
    	for(int i = 0; i < newData.length; i++) {
    		newData[i] = randNum.nextDouble();
    	}
    	return newData;
    }
    
    public static double[] createQWeightedRandomDA(int attNum, int quarter) {
    	double[] newData = new double[attNum];

    	for(int i = 0; i < newData.length; i++) {
    		if((i+2) % 4 == quarter) {
    			newData[i] = (randNum.nextDouble() * 0.25) + 0.75;
    		} else newData[i] = randNum.nextDouble() *0.75;
    	}
    	return newData;
    }
    
    public static void printInd(Individual indv) {
    	double[] atts = indv.getAttributes();
   
    	String printStr = "Individual " + indv.getId() +": ("+ atts.length +"atts)";
    	for(int i = 0; i < atts.length; i++) {
    		printStr += atts[i] + " ";
    	}

    	
    	System.out.println(printStr);
    	
    }
    
    public static void printGen(ArrayList<Individual> population) {
    	for(Individual i : population) {
    		printInd(i);
    	}
    }
    
    
    public static void printIntArrayList(ArrayList<Integer> ialist) {
    	String output = "List: ";
    	for(Integer i : ialist) {
    		output += i + " ";
    	}
    	System.out.println(output);
    }
    
    
    
}


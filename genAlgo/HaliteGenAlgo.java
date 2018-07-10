package genAlgo;



import java.io.File;
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
	
	//DEBUG OUTPUTS:
	public static final boolean PRINT_GEN = false;
	public static final boolean PRINT_AVG_DIF = true;

	public static final boolean PRINT_INIT_INFO = false;;
	
	//main arguments
	public static final int NUM_INIT_ARGS = 3;
	public static final int NUM_DEF_ARGS = 0;
	public static final int NUM_CONT_ARGS = 1;
	public static final int NUM_ANALYSIS_ARGS = 4;
	public static final int NUM_ANALYSIS_FINARGS = 2;

	public static final int NUM_DEF_ITS = 6;
	public static final int NUM_DEF_BATS = 5;
	public static final String STR_DEF_NAME = "DefaultRun";
	
	public static final int NUM_MIN_BATCHES = 1;
	public static final int NUM_MIN_ITERATIONS = 1;
	public static final int MIN_BOT_COUNT = 8;
	
	
	//public static final int NUM_INDV = 16;

	//in the current version, attributes shift from the start-distribution to the final distribution (= 2 different settings)
	public static final int NUM_ATT_SETTINGS_PER_GAME = 1; 
	
	public static final int NUM_ATTS = Control.NUM_ATTS;
	
	public static final int ATT_MAX_INT = 50;
	public static final double ATT_MAX = 1.0;

	// ############################### HARDCODED INIT VARS #######################################
	public static final int NUM_CHILDS = 12;
	public static final double PRESET_MUTATION_CHANCE = 0.33;
	public static final int PRESET_N_MAX_MUTATIONS = 3;
	public static final double PRESET_CREEPMUT_CHANCE = 0.3;
	public static final double PRESET_CREEPMUT_INTERV= 0.15;

	public static final double PRESET_AVGCROSS_CHANCE = 0.2;
	// ############################### HARDCODED INIT VARS #######################################

	public static final int NUM_4PL_MATCHES = 1;
	//io defaults
	
	public static final boolean USES_DISTRIBUTION = true;
	
	public static final String GAIT_FILENAME = "gaitinfo.txt";
	public static final String SAFE_BOTS_FILENAME = "safebots.txt";
	public static final String EXT_BOTS_FILENAME = "extbots.txt";

	//public static final String MATCHUP_INIT = "";
	//public static final int MATCHES_PER_EXT_ROUND = 1;
	//public static final int MATCHES_PER_TOURN_ROUND = 3;
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
	
	private static final int NUM_MAX_SBS = 5;
	private static final boolean ALLOW_REM_LAST_SB = false;
	public static final boolean REMOVE_FIRST_SB = false;

	public static Random randNum;
	
	public int populationSize;
	
	
	String currentRunID;
	
	//int numIndividuals;
    
    ArrayList<String> bots;
    ArrayList<String> futureBots;
    ArrayList<String> safeBots;
    ArrayList<String> externBotList;
    
    ArrayList<Integer> tRanking;
    
    boolean twoPlayerMode;
    
    double[] parentWeightDistr;
	int numIterations;
	int iteration;
	int numBatches;
	int batch;
	
	GAFileHandler ioHandler;
	//boolean initrun;
	ArrayList<Integer> initAtts;
	long staticSeed;
	double mutationChance;
	int numMaxMutations;
	
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
		ioHandler.readInit(PRINT_INIT_INFO);
		initAtts = ioHandler.getInitData();
		staticSeed = ioHandler.getStaticSeed();
		mutationChance = ioHandler.getMutationChance();

		populationSize = initAtts.get(GAFileHandler.IPAM_POPSIZE)*8;

		//initrun = true;
		//numIndividuals = numBots;
		this.numIterations = numIterations;
		iteration = 0;
		this.numBatches = numBatches;
		batch = 0;
		currentRunID = currentRunName;
		safeBots = new ArrayList<>();
		externBotList = new ArrayList<>();
	    twoPlayerMode = false;
		if(initAtts.get(GAFileHandler.IPAM_PLAYERS) == GAFileHandler.IPAM_PL_2) {
			twoPlayerMode = true;
		}
		numMaxMutations = initAtts.get(GAFileHandler.IPAM_NUM_MUTATIONS);

		GAFileHandler.clearAllScoresFolder();
    	//generate a random population
    	population = new ArrayList<>(populationSize);
    	
    	int attCreationType = initAtts.get(GAFileHandler.IPAM_INIT_ATT_TYPE);
    	String attTypeString;
    	switch(attCreationType) {
    		case GAFileHandler.IPAM_IAT_EQDIST:{
    			attTypeString = "EQUAL DISTRIBUTION("+attCreationType+")";
    			break;
    		}
    		case GAFileHandler.IPAM_IAT_QW:{
    			attTypeString = "Q-WEIGHTED DISTRIBUTION("+attCreationType+")";
    			break;
    		}
    		case GAFileHandler.IPAM_IAT_RANDOM:
    		default:{
    			attTypeString = "RANDOM("+attCreationType+")";
    		}
    	}
    	
    	switch(initAtts.get(GAFileHandler.IPAM_GA_TYPE)) {
    		case GAFileHandler.IPAM_GAT_CONT:{
            	System.out.println(">>> CONTINUE RUN, LOADING OLD POPULATION");

        		readOldPopulationAtts(populationSize);
        		
        		break;
    		}
    		case GAFileHandler.IPAM_GAT_MIXED:{
            	System.out.println(">>> MIXED RUN, LOADING HALF OLD POPULATION, CREATING HALF NEW POPULATION USING " + attTypeString + " ATTRIBUTE GENERATION");

        		readHalfOldPopulationAtts(attCreationType);
        		break;

    		}
    		case GAFileHandler.IPAM_GAT_STANDARD:
    		default:{
    			
            	System.out.println(">>> STANDARD RUN, CREATING NEW POPULATION USING " + attTypeString + " ATTRIBUTE GENERATION");

            	createNewPopulation(populationSize, attCreationType);

    		}
    	}
    	//createPopulation();
    	//if(Objects.equals(currentRunName, new String("cont"))) {


    	if(PRINT_GEN) {
        	printGen(population);
    	}
    	
    	if(PRINT_AVG_DIF) {
    		System.out.println("avg attribute difference diversity of this population: " + getAvgAttDiffOfPop(population));
    	}
    	
    	bots = GAFileHandler.getBotNames(populationSize, 0,0);
    	
    	createGAITinfo(); //set safeBots + externBotList
    	
    	ioHandler.createMatchupFile(populationSize, bots, safeBots, externBotList, twoPlayerMode);
    	//createMatchesSh();
    	
    	saveCurrentBotAtts();
	}

	//extern bots convention: write the name of each Bot(ABcd123.java) in configs/static_configs/extbots.txt like this:
	// ABcd123.java Klvb33.java ExampleBot.java
	// Bots are assumed to be placed in 
	
	public static ArrayList<Individual> getPopulationAt(int popSize, int it, int ba){
		ArrayList<Individual> pop = new ArrayList<>(popSize);
    	for(int i = 0; i < popSize; i++) {
        	double[] attrDistr = GAFileHandler.readBotAtts(popSize, i, it, ba);
        	Individual newInd = new Individual(attrDistr, ATT_MAX);
        	pop.add(newInd);
    	}
    	return pop;
	}

	public void readPopulationAtts(){
		population = getPopulationAt(populationSize, iteration, batch);
	}
	
	public void readOldPopulationAtts(int numIndvs){
		int numIndvsFit = numIndvs;
		if(numIndvsFit > populationSize) {
			numIndvsFit = populationSize;
		}
    	for(int i = 0; i < numIndvsFit; i++) {
        	double[] attrDistr = GAFileHandler.readOldBotAtts(populationSize, i);
        	Individual newInd = new Individual(attrDistr, ATT_MAX);
        	population.add(newInd);
    	}
	}
	
	public void readHalfOldPopulationAtts(int attCreationType){
    	
    	int halfPopulation = populationSize/2;

    	readOldPopulationAtts(halfPopulation);

    	createQWeightDistPopulation(halfPopulation);
    	createNewPopulation(halfPopulation, attCreationType);
    	/*
    	for(int i = 0; i < populationSize; i++) {
    		if(i < populationSize/2) {
            	double[] attrDistr = GAFileHandler.readOldBotAtts(i);
            	Individual newInd = new Individual(attrDistr, ATT_MAX);
            	population.add(newInd);
    		} else {
    			double[] attrDistr;
            	if(i < div_indv+populationSize/2) {
                	attrDistr = createQWeightedRandomDA(NUM_ATTS, 0);

            	} else if (i < div_indv*2+populationSize/2) {
                	attrDistr = createQWeightedRandomDA(NUM_ATTS, 1);

            	} else if (i < div_indv*3+populationSize/2) {
                	attrDistr = createQWeightedRandomDA(NUM_ATTS, 2);

            	} else {
                	attrDistr = createQWeightedRandomDA(NUM_ATTS, 3);

            	}
            	attrDistr = Individual.normalizeA(attrDistr);
            	Individual newInd = new Individual(attrDistr, ATT_MAX);
            	population.add(newInd);
    		}

    	} */

	}
	
    private void createNewPopulation(int numCrIndv, int attCreationType) {
    	switch(attCreationType) {
    		case GAFileHandler.IPAM_IAT_EQDIST:{
    			createEqWDistPopulation(numCrIndv);
    			break;
    		}
    		case GAFileHandler.IPAM_IAT_QW:{
    			createQWeightDistPopulation(numCrIndv);
    			break;
    		}
    		case GAFileHandler.IPAM_IAT_RANDOM:
    		default:{
    			createRandomPopulation(numCrIndv);
    		}
    	}
    }


    private void createRandomPopulation(int numCrIndv) {
    	for(int i = 0; i < numCrIndv; i++) {
        	double[] attrDistr = createRandomDoubleArray(NUM_ATTS);
        	attrDistr = Individual.normalizeA(attrDistr);
        	Individual newInd = new Individual(attrDistr, ATT_MAX);
        	population.add(newInd);
    	}
		
	}


    private void createEqWDistPopulation(int numCrIndv) {
    	//int div_indv = restIndNum/4;

    	//    public static double[] createEqDistDA(int attNum, int individual, int indvSize) {

    	for(int i = 0; i < numCrIndv; i++) {
    		double[] attrDistr;
    		attrDistr = createEqDistDA(NUM_ATTS, i, numCrIndv);
        	attrDistr = Individual.normalizeA(attrDistr);
        	Individual newInd = new Individual(attrDistr, ATT_MAX);
        	population.add(newInd);
    	}
		
	}
    
    private void createQWeightDistPopulation(int numIndvs) {
    	int div_indv = numIndvs/4;

    	for(int i = 0; i < numIndvs; i++) {
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
		ioHandler.readInit();
		initAtts = ioHandler.getInitData();
		staticSeed = ioHandler.getStaticSeed();
		populationSize = initAtts.get(GAFileHandler.IPAM_POPSIZE)*8;
		mutationChance = ioHandler.getMutationChance();

		//initrun = false;
		boolean cont = getGAITinfo(); //reads gait file data and sets most data for this iteration
		if(!cont) {
			System.out.println("HaliteGenAlgo: The used GAIT file indicates that the algorith has finished.");
			return;
		}		
		
		twoPlayerMode = false;
		if(initAtts.get(GAFileHandler.IPAM_PLAYERS) == GAFileHandler.IPAM_PL_2) {
			twoPlayerMode = true;
		}
		numMaxMutations = initAtts.get(GAFileHandler.IPAM_NUM_MUTATIONS);

		bots = GAFileHandler.getBotNames(populationSize, iteration, batch);
		futureBots = GAFileHandler.getBotNames(populationSize, nextIteration, nextBatch);

		readPopulationAtts();

		//readPopulationScores();
		tRanking = GAFileHandler.readLastRankings(populationSize);
		
		compute();
		
		//readBotAttsByName(safeBots.get(0))

		if(batch != nextBatch) { //add safebot in each new batch
			safeBots.add(bots.get(0)); //use first of the winner bots
			
			//create population of safebots to check the average att diffs
			ArrayList<Individual> safeBotPopulation = new ArrayList<>(safeBots.size());
			for(int i = 0; i < safeBots.size(); i++) {
				safeBotPopulation.add(new Individual(GAFileHandler.readBotAttsByName(safeBots.get(i)),ATT_MAX));
			}
			
			if(safeBots.size() >= NUM_MAX_SBS) { //need to remove a safebot
				int sbRemoveIndex;
				if(REMOVE_FIRST_SB) {
					sbRemoveIndex = 0;
				} else {
					sbRemoveIndex = getHighestAvgAttDiffExcludeInd(safeBotPopulation, ALLOW_REM_LAST_SB); //use the one with the lowest avg "diversity"
				}
				System.out.println("safebotsize exceeded limit, removing ("+sbRemoveIndex+").");
				safeBots.remove(sbRemoveIndex);
				safeBotPopulation.remove(sbRemoveIndex);
			}
			if(safeBots.size() > 1) {
				System.out.println("average attribute difference to last safeBot: " + getAvgDiffOfAtts(safeBotPopulation.get(safeBotPopulation.size()-1).getAttributes(), safeBotPopulation.get(safeBotPopulation.size()-2).getAttributes()));
			}
		}
	
		ioHandler.writeBotAtts(iteration, batch, bestIndID, true, population.get(0).getAttributes());
    	ioHandler.createMatchupFile(populationSize, futureBots, safeBots, externBotList, twoPlayerMode);
    	System.out.println("matchupfile created, updating GAITINFO");
		updateGAITinfo();
		saveAsNextPopulationAtts();
    	System.out.println("HGA: safed next Population Attributes");
	}




	
	private void compute() {
	    select(); //select individuals according to their score(s) for the new population;
	    recombine(); //create children from the selected parents
	    mutate(); //mutate some of the children (and add all childs to new population)		System.out.println("HaliteGenAlgo:current Generation:");
		Collections.shuffle(population);
		System.out.println("population, after");
		if(PRINT_GEN) {
		    printGen(population);
		}
    	if(PRINT_AVG_DIF) {
    		System.out.println("population attribute difference diversity = " + getAvgAttDiffOfPop(population));
    	}
	}


	private void select() { //currently: select the individuals with the best score and add them to "parents"
		//System.out.println("Popscore size = " + popScores.size() + ", rpopulation.size = " + population.size());
		System.out.println("population, before");
		if(PRINT_GEN) {
		    printGen(population);
		}
		//System.out.println("select: rankings:");
		//printIntArrayList(rankings);
		restPopulation = sortByFitness(population);
		population = new ArrayList<>(populationSize);
		int numSurvivingIndv = populationSize - NUM_CHILDS;

		//System.out.println("restpopulation, after sorting: ");
		//printGen(restPopulation);

		//parents = new ArrayList<>(NUM_PARENT_INDV);
		for(int i = 0; i < numSurvivingIndv; i++) {
			population.add(restPopulation.get(i));
			System.out.println("adding restpopulation("+i+")");

		}
	}
	
	/**
	 * sorts by ranking
	 * @param population to be sorted
	 * @return
	 */
	public ArrayList<Individual> sortByFitness(ArrayList<Individual> population){ // ALSO writes bestIndID and parentWeightDistr
		ArrayList<Individual> sortedPopulation = new ArrayList<>(populationSize);
		int indvIndex = 0;
		bestIndID = tRanking.get(0);
		ArrayList<Integer> botIds = new ArrayList<>();
		
		for(int j = 0; j < tRanking.size(); j++) {
			indvIndex = tRanking.get(j);
			botIds.add(indvIndex);
			sortedPopulation.add(population.get(indvIndex));
		}
		
		parentWeightDistr = new double[sortedPopulation.size()];
		double pwdLength = (double) parentWeightDistr.length;

		double initRankFactor = 1/pwdLength;
		double rankFactor;
		ArrayList<Double> firstScores = GAFileHandler.collectInitScores(populationSize, botIds, iteration, batch, externBotList.size(),  safeBots.size(), initAtts.get(GAFileHandler.IPAM_MATCHES_EXT));
		HashMap<Integer, Integer> brRanking = GAFileHandler.createRankings(populationSize, botIds, firstScores, 1 , sortedPopulation.size());
		for(int i = 0; i < parentWeightDistr.length; i++) {
			rankFactor= 1.0d-((double)i/(pwdLength+1.0d));
			if(!externBotList.isEmpty() || !safeBots.isEmpty()) {
				initRankFactor= 1.0d-((double)brRanking.get(i)/((double)brRanking.size()+1.0d));
			}
			System.out.println("creating pwd, ind"+i+", rankfac="+rankFactor+",initRankfac="+initRankFactor+"(initrank:"+brRanking.get(i)+")");
			parentWeightDistr[i] = (initRankFactor*0.2)+(0.8*rankFactor*rankFactor*rankFactor);
		}
		parentWeightDistr = Individual.normalize(parentWeightDistr);

		
			
			
			

			//System.out.println("HGA: sortByFitness : sortedpopulation:");
			//printGen(sortedPopulation);

			//int maxRanking = Collections.max(frRanking.keySet());
			//int halfRanking = maxRanking / 2;
			//for(int i = 0; i < parentWeightDistr.length; i++) {
			//	parentWeightDistr[i] = 1/pwdLength;
			//	parentWeightDistr[i] = parentWeightDistr[i] * ((double)((maxRanking-frRanking.get(i))-halfRanking)/pwdLength) * PARENT_W_FACTOR;
				//System.out.println("parentweightdistr: " +parentWeightDistr[i]);
			//}


		return sortedPopulation;
	}
	
	
	
	private void recombine() {
		children = new  ArrayList<>();
		int firstParent = 0;
		int secondParent = 0;
		String pwdout = "parentWeightDistr:";
		for(int i = 0; i < parentWeightDistr.length;i++) {
			pwdout+= parentWeightDistr[i] +" ";
		}
		System.out.println(pwdout);
		for(int i = 0; i < NUM_CHILDS; i++) {
			
				//CHOOSE FIRST PARENT WITH PROBABILITY ACCORDING TO parentWeightDistr
				double firstParentD = randNum.nextDouble();
				System.out.println("firstParentD: "+firstParentD);
				double dsum = 0;
				for(int di = 0; di < parentWeightDistr.length; di++) {
					if(firstParentD < dsum+parentWeightDistr[di]) {
						firstParent = di;
						break;
					}
					dsum+=parentWeightDistr[di];
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
				System.out.println("secondParentD: "+secondParentD);
				double d2sum = 0;
				for(int di = 0; di < secondParentWeightDistr.length; di++) {
					if(secondParentD < d2sum+secondParentWeightDistr[di]) {
						secondParent = di;
						break;
					}
					d2sum+=secondParentWeightDistr[di];
				}

			if(secondParent>= firstParent) {
				secondParent++;
			}
			children.add(Individual.recombineDistr(restPopulation.get(firstParent), restPopulation.get(secondParent)));
			System.out.println("adding child " + (children.size()-1) + " with parents " + firstParent + " and " + secondParent);

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
		

		while(childrenMutated < numMaxMutations && children.size() > 0){
			boolean canMutate = true;
			boolean didMutate = false;
			double currentMutChance = mutationChance;
			int ind_chm = randNum.nextInt(children.size());
			childm = children.get(ind_chm);
			
			while(canMutate && currentMutChance >= 0.02) { //repeat until not possible
				double mutateThisChild = randNum.nextDouble(); // probability of 1/CHILD_MUTATE_DIV to mutate this child

				if(mutateThisChild <= currentMutChance) {
					System.out.println("mutating child " + ind_chm + ", chance was: "+ currentMutChance);
					childm.mutateDistr();
					didMutate = true;
					currentMutChance = currentMutChance/2; //half the chance each time
				} else {
					canMutate = false;
				}
			} 
			if(didMutate) {
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
		while(population.size() < populationSize && !population.isEmpty()) {
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
		safeBots = ioHandler.getBatchBots();
		externBotList = ioHandler.getExternBots();
		ArrayList<Integer> gaitinit = ioHandler.getGaitInit();


		iteration = gaitinit.get(GAFileHandler.GAIT_I_IT);
		numIterations = gaitinit.get(GAFileHandler.GAIT_I_N_IT);
		batch = gaitinit.get(GAFileHandler.GAIT_I_BA);
		numBatches = gaitinit.get(GAFileHandler.GAIT_I_N_BA);
		int finished = gaitinit.get(GAFileHandler.GAIT_I_FIN);
    	if(finished == 1) {
    		
    		return false;
    	}		
    	//numIndividuals = gaitinit.get(5);
	    
	    System.out.println("HaliteGenAlgo: readResults: "+ currentRunID +","+ iteration +","+ numIterations +","+ batch +","+ numBatches +","+ populationSize);
	    int[] next = getNextItBa(iteration, batch, numIterations, numBatches);
	    nextBatch = next[NEXT_BA];
	    nextIteration = next[NEXT_IT];
	    return true;
	}
	



	/*
	
	public void readPopulationScores() {
		popScores = new ArrayList<>(populationSize);

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

	public static int getScoreByRanking(int ind, ArrayList<Integer> rankings, int popSize) {
		return (popSize - rankings.get(ind));
	}


	public LinkedList<Match> createMatchup(){ //creates 2pl matches, one planned and one random 4pl match per bot
		LinkedList<Match> matches = new LinkedList<>();

		for(int i = 0; i < populationSize; i++) {
			int nid =  (i + 1 + iteration*3)%populationSize;
			int nnid =  (i + 2 + iteration*3)%populationSize;
			int nnnid = (i + 3 + iteration*3)%populationSize;
			for(int gaid = 0; gaid < populationSize; gaid++) {
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

				if(eid >= populationSize + safeBots.size()) {
					eid -= populationSize + safeBots.size();
					enemyTypes[enm] = 2;
				} else if (eid >= populationSize) {
					eid -= populationSize;
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
    		String output = "HaliteGenAlgo:Initial run";
        	//int numIndividuals = Integer.valueOf(args[0]);
        	int iterations = Integer.valueOf(args[0]);
        	int batches = Integer.valueOf(args[1]);
        	String runID = args[2];
        	if(iterations<NUM_MIN_ITERATIONS) {
        		System.out.println(output+":Error. Minimum iteration amount is " +NUM_MIN_ITERATIONS+", given:" + iterations);
        		return;
        	}
        	if(batches<NUM_MIN_BATCHES) {
        		System.out.println(output+":Error. Minimum batch amount is " +NUM_MIN_BATCHES+", given:" + batches);
        		return;
        	}
        	System.out.println(output + ", iterations: " + iterations + ", batches: " + batches);
        	new HaliteGenAlgo(iterations, batches, runID);

    	} else if(args.length == NUM_DEF_ARGS) {

        	new HaliteGenAlgo(NUM_DEF_ITS, NUM_DEF_BATS, STR_DEF_NAME);

    		
    	} else if(args.length == NUM_CONT_ARGS) {
        	int iterations = Integer.valueOf(args[0]);
    		System.out.println("HaliteGenAlgo:Run " + iterations);
        	
        	new HaliteGenAlgo();

    	} else if(args.length == NUM_ANALYSIS_ARGS) {
        	String name = args[0];
        	int amatches = Integer.valueOf(args[1]);
        	int bmatches = Integer.valueOf(args[2]);
        	int cmatches = Integer.valueOf(args[3]);
    		System.out.println("HaliteGenAlgo:Analysis Run");
    		new HaliteGenAlgo(name, amatches, bmatches, cmatches);
    		
    	}	else if(args.length == NUM_ANALYSIS_FINARGS) {
    		System.out.println("HaliteGenAlgo:continuing Analysis Run");
    		new HaliteGenAlgo(args[0], Integer.valueOf(args[1]));

    		
    	} else {

    		System.out.println("HaliteGenAlgo:Warning. No valid number of arguments (init:"+NUM_INIT_ARGS+", cont:"+NUM_CONT_ARGS+"). This is used for testing.");
    		//printInd(createSpecialIndividual());
    		
    			
    			
    		int numscores = 20;
    		double addvar = 0.5;
    		LinkedList<Double> scores = new LinkedList<>();
    		for(int i = 0; i < numscores; i++) {
    			scores.add(addvar);
    		}
    		double weightedScore = 0;
    		double count = (double)scores.size();
    			//int halfCount = (int) (0.5*count);
    			for(int i = 0; i < count; i++) {
    				//double factor = (((double)i-halfCount) / (double)count) * 1.5 + 1; //weights: ~0-25 - 1.75
    				double factor = ((double)(i+1))/count;
    				
    				//if(i == ((int)count)-1) { //last value weights more
    				//	factor *= 2;
    				//}
    				weightedScore += scores.get(i) * factor;
    			}
    			
    			weightedScore = weightedScore / (double)count;

    			System.out.println("weighted score of ("+addvar+") x"+numscores+" = "+ weightedScore);
    	}
    
    }
 
    
    public static double getAvgScore(LinkedList<Double> linkedlist) {
    	BigDecimal sum = BigDecimal.ZERO;
    	int count = 0;
    	//System.out.println("getAverage: linkedList of size " + linkedlist.size());
    	for(Double sc : linkedlist) {
    		if(sc.isNaN()) {
    	    	//System.out.println("skipping nan");

    			continue;
    		}
	    	//System.out.println("currentsum = " + sum + ", adding " + BigDecimal.valueOf(sc).toString());

    		sum = sum.add(BigDecimal.valueOf(sc));
    		count++;
    	}
    	if(count == 0) {
    		return 0;
    	}
    	double returnvalue = (sum.divide(BigDecimal.valueOf(count), MathContext.DECIMAL128)).doubleValue();
		//System.out.println("c = " + count + " , sum = " + sum + ", avg = " + returnvalue);
    	//System.out.println("finalsum = " + sum + ", count = " + count + ", result = " + returnvalue);

    	return returnvalue;
    }
    
    public static Individual createSpecialIndividual() {
    	
    	double[] newData = new double[NUM_ATTS];
    	for(int i = 0; i < newData.length; i++) {
    		if(i == 0 || i == 1) {
        		newData[i] = 0.0;
    		} else if(i == 4) {
        		newData[i] = 0.1;
    		} else if(i == 9) {
        		newData[i] = 0.1;
    		} else if(i == 7 || i == 8 || i == 12 || i == 13) {
        		newData[i] = 0.1;
    		} else if(i == 5 || i == 6 || i == 10 || i == 11) {
        		newData[i] = 0.9;
    		} else {
        		newData[i] = 0.5;
    		}
    	}
    	newData = Individual.normalizeA(newData);
    	Individual newIndv = new Individual(newData, NUM_ATTS);
    	
    	return newIndv;
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
    
    
    public static double[] createEqDistDA(int attNum, int individual, int indvSize) {
    	double[] newData = new double[attNum];
    	
    	double stepSize = 1/indvSize;
    	System.out.println(stepSize);
    	
    	for(int i = 0; i < newData.length; i++) {
    		int attrOffs = (i+individual)%indvSize; // 0..n-1
    		double attrRange = attrOffs*stepSize; // attrOffset*1/n
    		double randomVal = randNum.nextDouble() * stepSize; // 0.. 1/n
    		newData[i] = attrRange + randomVal;
    	}
    	return newData;
    }
    
    public static double[] createDistinctEQDDA(int attNum, int individual, int indvSize) {
    	double[] newData = new double[attNum];
    	
    	double stepSize = 1/indvSize;
    	
    	for(int i = 0; i < newData.length; i++) {
    		int attrOffs = (i+individual)%indvSize; // 0..n-1
    		double attrRange = attrOffs*stepSize; // attrOffset*1/n
    		double randomVal = randNum.nextDouble() * stepSize; // 0.. 1/n
    		newData[i] = attrRange + randomVal;
    	}
    	/*
    	 * TODO!
    	 * 
    	 */
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
    
    
    
    /**
     * Shows which Individual to remove from the given population to achieve the highest difference in average attribute distribution
     * @param pop the given population
     * @param allowRemLastSb 
     * @return the index of the individual in the ArrayList
     */
    public static int getHighestAvgAttDiffExcludeInd(ArrayList<Individual> pop, boolean allowRemLastSb) {
    	ArrayList<Individual> testPop = new ArrayList<>(pop.size()-1);;
    	
    	double highestAttDiff = 0;
    	int hadExcludedIndv = 0;
    	
    	for(int i = 0; i < pop.size(); i++) { //for each indv
    		if(allowRemLastSb && i == pop.size()-1) {
    			continue;
    		}
    		for(int j = 0; j < pop.size(); j++) { //for each other indv not used yet
    			if(j == i) {
    				continue;
    			}
    			testPop.add(pop.get(j));
    		}
    		double avgDiff = getAvgAttDiffOfPop(testPop);
    		if(avgDiff>highestAttDiff) {
    			highestAttDiff = avgDiff;
    			hadExcludedIndv = i;
    		}
    		testPop.clear();
		}
    	
    	return hadExcludedIndv;
    }
    
    
    public static double getAvgAttDiffOfPop(ArrayList<Individual> population) {
		BigDecimal indvAttSum = BigDecimal.ZERO;
		int count = 0;
    	//String output = "getAvgAttDiffOfPop: adding";
		for(int i = 0; i < population.size(); i++) { //for each indv
    		for(int j = i+1; j < population.size(); j++) { //for each other indv not used yet
    			indvAttSum = indvAttSum.add(BigDecimal.valueOf(getAvgDiffOfAtts(population.get(i).getAttributes(), population.get(j).getAttributes())));
        		//output += " .. " + indvAttSum.toString();
    			count++;
    		}
    	}
    	double ret = (indvAttSum.divide(BigDecimal.valueOf(count), MathContext.DECIMAL128)).doubleValue(); //div by count and 

    	return ret;
    }
    
    public static double getAvgDiffOfAtts(double[] atts1, double[] atts2) {
    	BigDecimal sum = BigDecimal.ZERO;
    	//String output = "getAvgDiffOfAtts: adding";
    	for(int i = 0; i < atts1.length; i++)     {	//add differences with BigDouble to avoid floating point errors
    		double diff = Math.abs(atts1[i]-atts2[i]);
    		sum = sum.add(BigDecimal.valueOf(diff));
    		//output += " .. " + sum.toString();
    	}
    	double ret = (sum.divide(BigDecimal.valueOf(atts1.length), MathContext.DECIMAL128)).doubleValue(); //div by count and 
		//output += " / " + atts1.length + " =>  " + ret;

		//System.out.println(output);
    	return ret;
    }
    
	public HaliteGenAlgo(String analysisRunName, int numAMatches, int numBMatches, int numCMatches){
		ioHandler = new GAFileHandler();
		ioHandler.readInit();
		initAtts = ioHandler.getInitData();
		staticSeed = ioHandler.getStaticSeed();
		populationSize = initAtts.get(GAFileHandler.IPAM_POPSIZE)*8;

		//initrun = false;
		boolean cont = getGAITinfo(); //reads gait file data and sets most data for this iteration
		if(cont) {
			System.out.println("Warning, this run was not declared as 'finished'");
		}

		twoPlayerMode = false;
		if(initAtts.get(GAFileHandler.IPAM_PLAYERS) == GAFileHandler.IPAM_PL_2) {
			twoPlayerMode = true;
		}
		boolean noReplay = false;
		/*if(initAtts.get(GAFileHandler.IPAM_OUTPUT_MODE) == GAFileHandler.IPAM_OM_ALL) {
			noReplay = false;
		}*/
		tRanking = GAFileHandler.readLastRankings(populationSize);
		
		
		//PRINT RANKINGS
		System.out.println("rankings of the last run:");
		printRankings(tRanking);
		
		//PRINT AVERAGE ATTRIBUTE DIFFERENCE DIVERSITIES
		ArrayList<Individual> firstPopulation = getPopulationAt(populationSize, 0, 0);
		ArrayList<String> firstPopulationNames = GAFileHandler.getBotNames(populationSize, 0, 0);

		ArrayList<Individual> lastPopulation = getPopulationAt(populationSize, iteration, batch);
		ArrayList<String> lastPopulationNames = GAFileHandler.getBotNames(populationSize, iteration, batch);

		ArrayList<String> presetBotNames = GAFileHandler.readPresets();
		double firstPopAADD = getAvgAttDiffOfPop(firstPopulation);
		double lastPopAADD = getAvgAttDiffOfPop(lastPopulation);
		
		

		System.out.println("avg attribute difference diversities:");
		System.out.println("first population: " + firstPopAADD + ", lastPopulation: " + lastPopAADD + ", difference: " + (lastPopAADD-firstPopAADD));
		
		System.out.println("clearing bot scores, creating matchup files: "+numAMatches+"x A-matches, "+numBMatches+"x B-matches, "+numCMatches+"x C-matches.");
		GAFileHandler.clearAllScoresFolder();
		//create Matches between first & final bots // A-MATCHES
		//create Matches between preset & first bots // B-MATCHES
		//create Matches between final & preset bots // C-MATCHES
		long seed = 0;
	
		boolean useOneThreeVersions = false;
		boolean reduced = true;
		ioHandler.createAbcMatchup(populationSize, firstPopulationNames, lastPopulationNames, presetBotNames, numAMatches, numBMatches, numCMatches, noReplay, seed, twoPlayerMode, GAFileHandler.ABC_RED_SIMPLE_A);

	}

	

	public HaliteGenAlgo(String analysisRunName, int analysisPhase){
		ioHandler = new GAFileHandler();
		ioHandler.readInit();
		initAtts = ioHandler.getInitData();
		populationSize = initAtts.get(GAFileHandler.IPAM_POPSIZE)*8;
		getGAITinfo(); //reads gait file data and sets most data for this iteration


		//rankings = GAFileHandler.readLastRankings(populationSize);

		ArrayList<Individual> firstPopulation = getPopulationAt(populationSize, 0, 0);
		ArrayList<String> firstPopulationNames = GAFileHandler.getBotNames(populationSize, 0, 0);

		ArrayList<Individual> lastPopulation = getPopulationAt(populationSize, iteration, batch);
		ArrayList<String> lastPopulationNames = GAFileHandler.getBotNames(populationSize, iteration, batch);

		ArrayList<String> presetBotNames = GAFileHandler.readPresets();
		
		
		LinkedList<String> outputLines = new LinkedList<>();
		String phaseName = "";
		switch(analysisPhase) {
		case 0:{	//first & final bots // A-MATCHES
			phaseName = "A";
			
			outputLines.add("A-Matches (first vs final population) results:");
			
			int matches = 0;
			int firstPopWinCount = 0;
			int firstPopMatchCount = 0;
			BigDecimal firstPopScoreSum = BigDecimal.ZERO;		
			
			int finalPopWinCount = 0;
			int finalPopMatchCount = 0;
			BigDecimal finalPopScoreSum = BigDecimal.ZERO;
			
			outputLines.add(">>> first population:");
			int b1id = 0;
			for(String bname : firstPopulationNames) {
				ArrayList<Boolean> wlist = GAFileHandler.readBotWinListFromName(bname, true);
				String winList = "won matches";
				int thisWins = 0;
				int thisMatches = 0;
				for(int i = 0; i < wlist.size(); i++) {
					if(wlist.get(i) == true) {
					    winList += "- " + i + " - ";
						thisWins++;
					} else {
					    winList += "- x - ";
					}
					thisMatches++;
				}
				outputLines.add(winList);
				double thisScoreSum =  GAFileHandler.summarizeBotScores(GAFileHandler.readWeightedBotScoresFromName(bname));
				firstPopWinCount += thisWins;
				firstPopMatchCount += thisMatches;
				firstPopScoreSum = firstPopScoreSum.add(BigDecimal.valueOf(thisScoreSum));
				
				String botResultString = bname + ": wins = " + thisWins + "(of " + thisMatches + "), score-sum = " + thisScoreSum;
				
				outputLines.add(botResultString);
				
				//COLLECT ACTUAL FITNESS SCORES FOR EACH BOT
				ArrayList<Double> slist = GAFileHandler.getWeightedBotScoreListFromName(bname);
				GAFileHandler.editABCMatchupList(0,0,b1id,slist);

				String scoreList = "botscores:";
				for(int i = 0; i < slist.size(); i++) {
					scoreList += "- " + slist.get(i) + " - ";
				}
				scoreList += "\n";
				outputLines.add(scoreList);
				b1id++;
			}
			
			
			outputLines.add(">>> final population:");
			int b2id = 0;
			for(String bname : lastPopulationNames) {
				ArrayList<Boolean> wlist = GAFileHandler.readBotWinListFromName(bname, true);
				String winList = "won matches";
				int thisWins = 0;
				int thisMatches = 0;
				for(int i = 0; i < wlist.size(); i++) {
					if(wlist.get(i) == true) {
					    winList += "- " + i + " - ";
						thisWins++;
					} else {
					    winList += "- x - ";
					}
					thisMatches++;
				}
				outputLines.add(winList);
				double thisScoreSum =  GAFileHandler.summarizeBotScores(GAFileHandler.readWeightedBotScoresFromName(bname));
				finalPopWinCount += thisWins;
				finalPopMatchCount += thisMatches;
				finalPopScoreSum = finalPopScoreSum.add(BigDecimal.valueOf(thisScoreSum));
				String botResultString = bname + ": wins = " + thisWins + "(of " + thisMatches + "), score-sum = " + thisScoreSum;
				outputLines.add(botResultString);
				
				//COLLECT ACTUAL FITNESS SCORES FOR EACH BOT
				ArrayList<Double> slist = GAFileHandler.getWeightedBotScoreListFromName(bname);
				GAFileHandler.editABCMatchupList(0,1,b2id,slist);

				String scoreList = "botscores:";
				for(int i = 0; i < slist.size(); i++) {
					scoreList += "- " + slist.get(i) + " - ";
				}
				scoreList += "\n";
				outputLines.add(scoreList);
				b2id++;

			}
			
			firstPopMatchCount = firstPopMatchCount/2; //there are 2 indvs per match, so they are counted twice
			finalPopMatchCount = finalPopMatchCount/2;

			outputLines.add("######### matches"+ finalPopMatchCount + "(final) /" + firstPopMatchCount + "(first) ###########");

			String firstPopResults = "first population overall wins = " + firstPopWinCount + "(of " + firstPopMatchCount + "), score-sum = " + firstPopScoreSum;
			outputLines.add(firstPopResults);
			String finalPopResults = "final population overall wins = " + finalPopWinCount + "(of " + finalPopMatchCount + "), score-sum = " + finalPopScoreSum;
			outputLines.add(finalPopResults);
			break;
		}
		case 1:{ //preset & first bots // B-MATCHES
			phaseName = "B";
			outputLines.add("B-Matches (preset vs first population) results:");

			int presetWinCount = 0;
			int presetMatchCount = 0;
			BigDecimal presetScoreSum = BigDecimal.ZERO;
			int firstPopWinCount = 0;
			int firstPopMatchCount = 0;
			BigDecimal firstPopScoreSum = BigDecimal.ZERO;		
			
			outputLines.add(">>> presets:");
			int b3id = 0;
			for(String bname : presetBotNames) {
				ArrayList<Boolean> wlist = GAFileHandler.readBotWinListFromName(bname, true);
				String winList = "won matches";
				int thisWins = 0;
				int thisMatches = 0;
				for(int i = 0; i < wlist.size(); i++) {
					if(wlist.get(i) == true) {
					    winList += "- " + i + " - ";
						thisWins++;
					} else {
					    winList += "- x - ";
					}
					thisMatches++;
				}
				outputLines.add(winList);
				double thisScoreSum =  GAFileHandler.summarizeBotScores(GAFileHandler.readWeightedBotScoresFromName(bname));
				presetWinCount += thisWins;
				presetMatchCount += thisMatches;
				presetScoreSum = presetScoreSum.add(BigDecimal.valueOf(thisScoreSum));
				String botResultString = bname + ": wins = " + thisWins + "(of " + thisMatches + "), score-sum = " + thisScoreSum;
				outputLines.add(botResultString);
				
				
				//COLLECT ACTUAL FITNESS SCORES FOR EACH BOT
				ArrayList<Double> slist = GAFileHandler.getWeightedBotScoreListFromName(bname);
				GAFileHandler.editABCMatchupList(1,0,b3id,slist);

				String scoreList = "botscores:";
				for(int i = 0; i < slist.size(); i++) {
					scoreList += "- " + slist.get(i) + " - ";
				}
				scoreList += "\n";
				outputLines.add(scoreList);
				b3id++;
			}
			outputLines.add(">>> first population:");
			int b4id = 0;
			for(String bname : firstPopulationNames) {
				
				//COLLECT WIN-LIST (COMPARED TO AVG) FOR EACH BOT
				ArrayList<Boolean> wlist = GAFileHandler.readBotWinListFromName(bname, true);
				String winList = "won matches:";
				int thisWins = 0;
				int thisMatches = 0;
				for(int i = 0; i < wlist.size(); i++) {
					if(wlist.get(i) == true) {
					    winList += "- " + i + " - ";
						thisWins++;
					} else {
					    winList += "- x - ";
					}
					thisMatches++;
				}
				outputLines.add(winList);

				double thisScoreSum =  GAFileHandler.summarizeBotScores(GAFileHandler.readWeightedBotScoresFromName(bname));
				firstPopWinCount += thisWins;
				firstPopMatchCount += thisMatches;
				firstPopScoreSum = firstPopScoreSum.add(BigDecimal.valueOf(thisScoreSum));
				
				String botResultString = bname + ": wins = " + thisWins + "(of " + thisMatches + "), score-sum = " + thisScoreSum;
				outputLines.add(botResultString);
				
				//COLLECT ACTUAL FITNESS SCORES FOR EACH BOT
				ArrayList<Double> slist = GAFileHandler.getWeightedBotScoreListFromName(bname);
				GAFileHandler.editABCMatchupList(1,1,b4id,slist);

				String scoreList = "botscores:";
				for(int i = 0; i < slist.size(); i++) {
					scoreList += "- " + slist.get(i) + " - ";
				}
				scoreList += "\n";
				outputLines.add(scoreList);
				b4id++;

			}
			
			presetMatchCount = presetMatchCount/2;
			firstPopMatchCount = firstPopMatchCount/2;
			outputLines.add("######### matches"+ presetMatchCount + "(preset) /" + firstPopMatchCount + "(first) ###########");
			String presetPopResults = "preset population overall wins = " + presetWinCount + "(of " + presetMatchCount + "), score-sum = " + presetScoreSum;
			outputLines.add(presetPopResults);
			String firstPopResults = "first population overall wins = " + firstPopWinCount + "(of " + firstPopMatchCount + "), score-sum = " + firstPopScoreSum;
			outputLines.add(firstPopResults);

			break;
		}
		
		case 2: //final & preset bots // C-MATCHES
		default:{
			phaseName = "C";
			outputLines.add("C-Matches (final vs preset population) results:");

			int finalPopWinCount = 0;
			int finalPopMatchCount = 0;
			BigDecimal finalPopScoreSum = BigDecimal.ZERO;
			int presetWinCount = 0;
			int presetMatchCount = 0;
			BigDecimal presetScoreSum = BigDecimal.ZERO;

			outputLines.add(">>> final population:");
			int b5id = 0;
			for(String bname : lastPopulationNames) {
				ArrayList<Boolean> wlist = GAFileHandler.readBotWinListFromName(bname, true);
				String winList = "won matches";
				int thisWins = 0;
				int thisMatches = 0;
				for(int i = 0; i < wlist.size(); i++) {
					if(wlist.get(i) == true) {
					    winList += "- " + i + " - ";
						thisWins++;
					} else {
					    winList += "- x - ";
					}
					thisMatches++;
				}
				outputLines.add(winList);
				double thisScoreSum =  GAFileHandler.summarizeBotScores(GAFileHandler.readWeightedBotScoresFromName(bname));

				finalPopWinCount += thisWins;
				finalPopMatchCount += thisMatches;
				finalPopScoreSum = finalPopScoreSum.add(BigDecimal.valueOf(thisScoreSum));
				String botResultString = bname + ": wins = " + thisWins + "(of " + thisMatches + "), score-sum = " + thisScoreSum;
				outputLines.add(botResultString);
				
				
				//COLLECT ACTUAL FITNESS SCORES FOR EACH BOT
				ArrayList<Double> slist = GAFileHandler.getWeightedBotScoreListFromName(bname);
				GAFileHandler.editABCMatchupList(2,0,b5id,slist);
				String scoreList = "botscores:";
				for(int i = 0; i < slist.size(); i++) {
					scoreList += "- " + slist.get(i) + " - ";
				}
				scoreList += "\n";
				outputLines.add(scoreList);
				b5id++;
			}
			outputLines.add(">>> presets:");
			int b6id = 0;
			for(String bname : presetBotNames) {
				ArrayList<Boolean> wlist = GAFileHandler.readBotWinListFromName(bname, true);
				String winList = "won matches";
				int thisWins = 0;
				int thisMatches = 0;
				for(int i = 0; i < wlist.size(); i++) {
					if(wlist.get(i) == true) {
					    winList += + i + "-";
						thisWins++;
					} else {
					    winList += "x-";
					}
					thisMatches++;
				}
				outputLines.add(winList);
				double thisScoreSum =  GAFileHandler.summarizeBotScores(GAFileHandler.readWeightedBotScoresFromName(bname));
				presetWinCount += thisWins;
				presetMatchCount += thisMatches;
				presetScoreSum = presetScoreSum.add(BigDecimal.valueOf(thisScoreSum));
				String botResultString = bname + ": wins = " + thisWins + "(of " + thisMatches + "), score-sum = " + thisScoreSum;
				outputLines.add(botResultString);
				
				
				//COLLECT ACTUAL FITNESS SCORES FOR EACH BOT
				ArrayList<Double> slist = GAFileHandler.getWeightedBotScoreListFromName(bname);
				GAFileHandler.editABCMatchupList(2,1,b6id,slist);
				String scoreList = "botscores:";
				for(int i = 0; i < slist.size(); i++) {
					scoreList += "- " + slist.get(i) + " - ";
				}
				scoreList += "\n";
				outputLines.add(scoreList);
				b6id++;
			}
			
			finalPopMatchCount = finalPopMatchCount/2;
			presetMatchCount = presetMatchCount/2;
			
			outputLines.add("######### matches"+ finalPopMatchCount + "(final) /" + presetMatchCount + "(preset) ###########");

			String finalPopResults = "final population overall wins = " + finalPopWinCount + "(of " + finalPopMatchCount + "), score-sum = " + finalPopScoreSum;
			outputLines.add(finalPopResults);
			String presetPopResults = "preset population overall wins = " + presetWinCount + "(of " + presetMatchCount + "), score-sum = " + presetScoreSum;
			outputLines.add(presetPopResults);
			}
		}

		String filename = analysisRunName+"_"+phaseName + ".txt";
		System.out.println("saving output data to file: "+filename);
		GAFileHandler.saveResultDataToFile(filename, outputLines);
		if(analysisPhase != 2) {
			System.out.println("clearing score folders");
			GAFileHandler.clearAllScoresFolder();
		}

	}
	
	
	public static void printRankings(ArrayList<Integer> ranks) {
		String ranksOutStr = "";
		for(int i = 0; i < ranks.size(); i++) {
			ranksOutStr += "["+i+"]: Indv("+ranks.get(i)+")";
			if(i <ranks.size()-1) {
				ranksOutStr += ", ";
			}
		}
	}
    
}


package genAlgo;

import java.io.File;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;

import hlt.Log;
import hlt.Task;
import hlt.Task.TaskType;

public class HaliteGenAlgo {
	
	//main arguments
	public static final int NUM_INIT_ARGS = 4;
	public static final int NUM_DEF_ARGS = 0;
	public static final int NUM_CONT_ARGS = 1;

	public static final int NUM_DEF_INDV = 10;
	public static final int NUM_DEF_ITS = 6;
	public static final int NUM_DEF_BATS = 5;
	public static final String STR_DEF_NAME = "DefaultRun";
	
	public static final int NUM_MIN_BATCHES = 1;
	public static final int NUM_MIN_ITERATIONS = 1;
	public static final int MIN_BOT_COUNT = 8;
	
	
	
	//in the current version, attributes shift from the start-distribution to the final distribution (= 2 different settings)
	public static final int NUM_ATT_SETTINGS_PER_GAME = 2; 
	
	public static final int NUM_ATTS = Task.NUM_ACTIVE_TYPES * NUM_ATT_SETTINGS_PER_GAME + NUM_ATT_SETTINGS_PER_GAME;
	
	public static final int ATT_MAX = 50; //how fine-tuned a distribution can be
	
	public static final int NUM_PARENT_INDV = 4; //should be even
	public static final int NUM_CHILD_MUT = 1; //should not be larger than NUM_PARENT_INDV/2
	
	public static final int NUM_4PL_MATCHES = 1;
	//io defaults
	
	public static final boolean OVERWRITE_GAIT = false;
	public static final String CFG_FOLDERNAME = "configs";
	
	public static final String BOT_SCR_FOLDERNAME = "scores";
	public static final String SAFE_CFG_FOLDERNAME = "static_configs";
	public static final String GA_CFG_FOLDERNAME = "ga_configs";

	
	public static final String GAIT_FILENAME = "gaitinfo.txt";
	public static final String SAFE_BOTS_FILENAME = "safebots.txt";
	public static final String EXT_BOTS_FILENAME = "extbots.txt";
	
	public static final String MATCHUP_FILENAME = "matches.sh";
	public static final String MATCHUP_INIT = "#!/bin/sh";
	public static final String MATCHUP_CALL = "./halite -q "; //TODO: SEE HALITE CLI

	public static final String GA_BOT_JAVANAME = "ModifiedBot.java";
	public static final String GA_BOT_CLASSNAME = "ModifiedBot";
	
	
	public static final String BOT_PREFIX = "GABot";
	public static final String SAFE_BOT_PREFIX = "SGABot";


	
	public static final int GAIT_LINES = 3;
	public static final int GAIT_INIT_ARGS = 6;

	public static Random randNum;
	
	String currentRunID;
	
	int numIndividuals;
    ArrayList<String> bots;
    ArrayList<String> futureBots;

    ArrayList<Integer> possibleEnemies;
    int numMatchesPerBot;
    
    ArrayList<String> safeBots;
    
    ArrayList<String> externBotList;

	int numIterations;
	int iteration;
	int numBatches;
	int batch;
	
	boolean initrun;
	
	int numAtts;
	//int[] atts;
	ArrayList<ArrayList<Double>> popScores;
    ArrayList<Individual> population;
    ArrayList<Individual> restPopulation;
    ArrayList<Individual> parents;
    ArrayList<Individual> children;
    int bestIndID;
	private int nextIteration;
	private int nextBatch;
    
    /**
     * creation Constructor
     * @param botNames given bot Names
     * @throws IOException 
     */
	public HaliteGenAlgo(int numBots, int numIterations, int numBatches, String currentRunName) throws IOException {
		initrun = true;
		numIndividuals = numBots;
		this.numIterations = numIterations;
		iteration = 0;
		this.numBatches = numBatches;
		batch = 0;
		currentRunID = currentRunName;
		safeBots = new ArrayList<>();
		externBotList = new ArrayList<>();

		clearScoresFolder();
    	//generate a random population
    	population = new ArrayList<>();
    	for(int i = 0; i < numIndividuals; i++) {
        	int[] attrDistr = createRandomIntArray(NUM_ATTS);
        	Individual newInd = new Individual(attrDistr, ATT_MAX);
        	population.add(newInd);
    	}
    	bots = getBotNames(0);
    	
    	createGAITinfo();
    	
    	createMatchupScript();
    	
    	saveCurrentBotAtts();
	}

    
    /**
     * update Constructor
     * @param botNames given bot Names
     * @throws IOException 
     */
	public HaliteGenAlgo() throws IOException {
		initrun = false;
		boolean cont = readGAITinfo();
		if(!cont) {
			return;
		}
		setFutureBotNames();

		bots = getBotNames(batch);
		readPopulationAtts();

		readPopulationScores();

		
		compute();
		
		
		
		if(batch != nextBatch) {
			safeBots.add(bots.get(bestIndID));
			saveBotAtts(batch, bestIndID, true);
			createNextMatchupScript();

		}
		updateGAITinfo();
		saveAsNextPopulationAtts();
	}


	//sets the bot names for this iteration
	public ArrayList<String> getBotNames(int ba) {
		ArrayList<String> botNames = new ArrayList<>();
		for(int i = 0; i < numIndividuals; i++) {
			String botName = getBotName(ba, i);
			botNames.add(botName);
		}
		return botNames;
	}
	
	public static String getBotName(int ba, int id) {
		return BOT_PREFIX + "_" + getSuffixByIt(ba) + id;
	}
	
	public void setFutureBotNames() {
		futureBots = new ArrayList<>();
		for(int i = 0; i < numIndividuals; i++) {
			futureBots.add(getBotName(nextBatch, i));
		}
	}
	
	public void compute() {
		computeGen();
		System.out.println("HaliteGenAlgo:current Generation:");
		printGen(population);
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
		System.out.println("Popscore size = " + popScores.size() + ", rpopulation.size = " + population.size());
		restPopulation = population;
		parents = new ArrayList<>();
		boolean bestIndSet = false;
		while(parentsChosen < NUM_PARENT_INDV) {
			double highestScore = Double.NEGATIVE_INFINITY;
			int h_id = 0;
			for(int i = 0; i < restPopulation.size(); i++) {
				int kjb = h_id/2;
				double currentScore = getAverage(popScores.get(restPopulation.get(i).getId()));
				if(currentScore > highestScore) {
					highestScore = currentScore;
					h_id = restPopulation.get(i).getId();
					int uvw = kjb;
				}
			}
			for(int i = 0; i<restPopulation.size(); i++) {
				Individual ind_i = restPopulation.get(i);
				if(ind_i.getId() == h_id) {
					if(!bestIndSet) {
						bestIndID = i;
						bestIndSet = true;
					}
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
		
		Individual child = new Individual(childAtts, ATT_MAX);
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
		while(population.size() < numIndividuals && !population.isEmpty()) {
			double highestScore = Double.NEGATIVE_INFINITY;
			int h_id = 0;
			for(int i = 0; i < restPopulation.size(); i++) {
				double currentScore = getAverage(popScores.get(restPopulation.get(i).getId()));
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
	}
	

	public void createGAITinfo() throws IOException {


		
		//Line 1
		//String currentRunID
		//int iteration
		//int numIterations;
		//int batch
		//int numBatches;
	 	int finished = 0;
	 	//int numIndividuals

	 	//Line 2
	 	//num_safe_bots = priorSafeBots.size()
	 	safeBots = readPriorSafeBots();

	 	//Line 3
	 	//num_extern_bots = externBots.size()
	 	externBotList = readExternBots();
	 	
	 	writeToGAIT(currentRunID, iteration, numIterations, batch, numBatches, finished, numIndividuals, safeBots, externBotList);
	
		
	}


	public void updateGAITinfo() {
		 
			//Line 1
			int finished = 0;
			if(nextBatch == -1) {
				finished = 1;
			}
			writeToGAIT(currentRunID, nextIteration, numIterations, nextBatch, numBatches, finished, numIndividuals, safeBots, externBotList);
	}
	
	
	/* gaitinfo.txt : config file for the HaliteGenAlgo class
	 * 
	 * 
	 * ## LINE 1 ##
	 * currentRunID : string
	 * iteration : 0-n
	 * num_iterations : 1-n
	 * batch: 0-n
	 * num_batches : 1-n
	 * finished : 0-1   (0 is false, 1 is true)
	 * num_bots : 0-n
	 *    ## list_bots not needed, can be computed by iteration + num_bots
	 *    
	 * ## LINE 2 ##
	 * num_safe_bots : 0-iteration
	 * list_safe_bots : BOT_PREFIX + "_" + SUFFIX + ID
	 * 
	 * ## LINE 3 ##
	 * num_extern_bots : 0-n
	 * list_extern_bots : name + ".java"
	 */
	public void writeToGAIT(String currentRun, int iteration, int numIts, int batch, int nBatches, int fin, int numInds, ArrayList<String> safeBots, ArrayList<String> extBots){
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, GAIT_FILENAME);
		//Log.log("HaliteGenAlgo: chosen output path:" + fPath.toString());
		/*
		File file = new File(fPath.toString());
		if(file.exists() && !OVERWRITE_GAIT) { 
			String errormsg = "HaliteGenAlgo:Error. GAIT File already exists, OVERWRITE_GAIT is set to false. Move " + GAIT_FILENAME + "to a different folder to make this work.";	
			throw new IOException(errormsg);
		} */
		
		LinkedList<String> newText = new LinkedList<>();

	 	//int numIndividuals
	 	String l1 = currentRun + " " + iteration + " " + numIts + " " + batch + " " + nBatches + " " + fin + " " + numInds;
		newText.add(l1);

	 	//Line 2
	 	//num_safe_bots = priorSafeBots.size()
	 	String l2 = safeBots.size() + " ";
	 	for(int i = 0; i < safeBots.size(); i++) {
	 		l2 += safeBots.get(i);
	 		if(i < safeBots.size()-1) {
	 			l2 += " ";
	 		}
	 	}
		newText.add(l2);

	 	//Line 3
	 	//num_extern_bots = externBots.size()
	 	String l3 = extBots.size() + " ";
	 	for(int i = 0; i < extBots.size(); i++) {
	 		l3 += extBots.get(i);
	 		if(i < extBots.size()-1) {
	 			l3 += " ";
	 		}
	 	}
		newText.add(l3);
	 	
	    try {
			Files.write(fPath, newText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
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
	}


	public boolean readGAITinfo() throws IOException {
		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, GAIT_FILENAME);
		File file = new File(fPath.toString());

	    try {
			scanner = new Scanner(new FileInputStream(file), "UTF-8");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    // Line 1
	    if(scanner.hasNext()) {
	    	currentRunID = scanner.next();
	    }

	    if(scanner.hasNext()) {
	    	iteration = scanner.nextInt();
	    }

	    if(scanner.hasNext()) {
	    	numIterations = scanner.nextInt();
	    }

	    if(scanner.hasNext()) {
	    	batch = scanner.nextInt();
	    }

	    if(scanner.hasNext()) {
	    	numBatches = scanner.nextInt();
	    }
	    if(scanner.hasNext()) {
	    	int finished = scanner.nextInt();
	    	if(finished == 1) {
	    		
	    		System.out.println("HaliteGenAlgo: The used GAIT file indicates that the algorith has finished.");
	    		return false;
	    	}
	    }
	    if(scanner.hasNext()) {
	    	numIndividuals =scanner.nextInt();
	    }
	    System.out.println("HGA: debug:  line 1 read");

	    //Line 2
	    safeBots = new ArrayList<>();
	    int numSafeBots = 0;
	    if(scanner.hasNext()) {
	    	numSafeBots = scanner.nextInt();
	    }
	    for(int i = 0; i < numSafeBots; i++) {
		    if(scanner.hasNext()) {
		    	safeBots.add(scanner.next());
		    } else {
	    		System.out.println("HaliteGenAlgo: The used GAIT file has an invalid count of safeBots.");
		    	return false;
		    }
	    }
	    //Line 3
	    externBotList = new ArrayList<>();
	    int numExternBots = 0;
	    if(scanner.hasNext()) {
	    	numExternBots = scanner.nextInt();
	    }
	    for(int i = 0; i < numExternBots; i++) {
		    if(scanner.hasNext()) {
		    	externBotList.add(scanner.next());
		    } else {
	    		System.out.println("HaliteGenAlgo: The used GAIT file has an invalid count of externBots.");
		    	return false;
		    }
	    }
	    
	    scanner.close();
	    System.out.println("HaliteGenAlgo: readResults: "+ currentRunID +","+ iteration +","+ numIterations +","+ batch +","+ numBatches +","+ numIndividuals +","+numExternBots);
	    setNextIt();
	    return true;
	}
	
	public ArrayList<String> readPriorSafeBots() {
		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, SAFE_CFG_FOLDERNAME, SAFE_BOTS_FILENAME);
		File file = new File(fPath.toString());
	    ArrayList<String> prSafeBots = new ArrayList<>();

		if(!file.exists()) { 
			System.out.println("HaliteGenAlgo:No prior safe bots specified");	
			return prSafeBots;
		}
	    try {
			scanner = new Scanner(new File(fPath.toString()));
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    
		while(scanner.hasNext()) {
			prSafeBots.add(scanner.next());
	    }
		return prSafeBots;
		
	}

	//extern bots convention: write the name of each Bot(ABcd123.java) in configs/static_configs/extbots.txt like this:
	// ABcd123.java Klvb33.java ExampleBot.java
	// Bots are assumed to be placed in 
	public ArrayList<String> readExternBots() {
		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, SAFE_CFG_FOLDERNAME, EXT_BOTS_FILENAME);
		
		File file = new File(fPath.toString());
	    ArrayList<String> externBots = new ArrayList<>();

		if(!file.exists()) { 
			System.out.println("HaliteGenAlgo:No extern bots specified");	
			return externBots;
		}
		
	    try {
			scanner = new Scanner(new File(fPath.toString()));
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    
		while(scanner.hasNext()) {
			externBots.add(scanner.next());
	    }
		return externBots;
	}

	public void readPopulationAtts() throws IOException {
		population = new ArrayList<>();
    	for(int i = 0; i < numIndividuals; i++) {
        	int[] attrDistr = readBotAtts(i);
        	Individual newInd = new Individual(attrDistr, ATT_MAX);
        	population.add(newInd);
    	}

	}
	
	public int[] readBotAtts(int id) throws IOException {
		int[] res = new int[NUM_ATTS];
		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();

		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, GA_CFG_FOLDERNAME, bots.get(id) + ".txt");
		File file = new File(fPath.toString());

	    try {
			scanner = new Scanner(new FileInputStream(file), "UTF-8");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    
	    String resultstring = "num = ";
	    int numSavedAtts = 0;
	    if(scanner.hasNext()) {
	    	numSavedAtts  = scanner.nextInt();
	    	resultstring += numSavedAtts;
	    }
	    
	    resultstring += ", atts : ";

	    
	    for(int i = 0; i <NUM_ATTS; i++) {
	    	if(scanner.hasNext()) {
	    		res[i] = scanner.nextInt();
	    		resultstring += res[i] + " ";
	    	} else {
				System.out.println("HaliteGenAlgo:Read Error - did not specify enough att values in " + fPath.toString());	
				throw new IOException();
	    	}
	    }
	    
	    System.out.println("scanner read the following values:" + resultstring);
	    
	    return res;

		
	}
	
	public void readPopulationScores() {
		popScores = new ArrayList<>();

		for(int i = 0; i < population.size(); i++) {
			ArrayList<Double> thisScores = readBotScores(i);
			popScores.add(readBotScores(i));
			System.out.println("score of ind " + i + " = " + getAverage(thisScores));

		}
		
	}
	

	public ArrayList<Double> readBotScores(int id) {
		
		ArrayList<Double> botScores = new ArrayList<>();
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, BOT_SCR_FOLDERNAME, bots.get(id));
		File folder = new File(fPath.toString());
		if(folder.exists()) {
			File[] fileList = folder.listFiles();
			if(fileList.length != 0) {
				for (File scoreFile : fileList) {
					if (scoreFile.isFile()) {
						botScores.add(readBotScoreFile(scoreFile));
					}
				}
			} else botScores.add((double) 0);
		} else botScores.add((double) 0);
		
		

		return botScores;

	}
	
	public double readBotScoreFile(File file) {
		Scanner scanner = null;

	    try {
			scanner = new Scanner(new FileInputStream(file), "UTF-8");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    
	    int score = 0;
	    int count = 0;
	    double avg = 0;
		while(scanner.hasNext()) {
			score += scanner.nextInt();
			count ++;
		}
		avg = ((double)score) / (double)count;
		return avg;
	}
	
	public void clearScoresFolder(){
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, BOT_SCR_FOLDERNAME);
		File fPathDir = new File(fPath.toString());
		File[] fileList = fPathDir.listFiles();
		if(fileList.length == 0) {
			return;
		}
		
		for (File scoreFolder : fileList) {
			deleteFolder(scoreFolder);
		}


	}
	
	public void deleteFolder(File folder) {
		File[] files = folder.listFiles();
	    if(files!=null) {
	        for(File f: files) {
	               f.delete();
	        }
	    }
	    folder.delete();
	}
	
	public void saveCurrentBotAtts() {

		for(int i = 0; i < population.size(); i++) {
			saveBotAtts(batch, i, false);
		}
	}
	
	public void saveAsNextPopulationAtts() {
		for(int i = 0; i < population.size(); i++) {
			saveBotAtts(nextBatch, i, false);
		}
	}
	
	public void saveBotAtts(int ba, int id, boolean asSafeBot) {
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, GA_CFG_FOLDERNAME, getBotName(ba, id) + ".txt");
		if(asSafeBot) {
			fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, SAFE_CFG_FOLDERNAME, getBotName(ba, id) + ".txt");
		}
		int[] botAttributes = population.get(id).getAttributes();;
	 	String line = NUM_ATTS + " ";
		LinkedList<String> newText = new LinkedList<>();
		for(int i = 0; i < NUM_ATTS; i++) {
			line += botAttributes[i];
			if(i < NUM_ATTS-1) {
				line += " ";
			}
		}
		System.out.println("saving bot " +  getBotName(ba, id)  + "  data: " + line );
		newText.add(line);

		
	    try {
			Files.write(fPath, newText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void createNextMatchupScript() {

		
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, MATCHUP_FILENAME);
		LinkedList<String> shText = new LinkedList<>();
		
	 	String l1 = MATCHUP_INIT;
		shText.add(l1);
		
		if(iteration == 0 && batch == 0) {
			shText.add(createCompileScript());
		}

	 	String currentMatchLine = "";
	 
		LinkedList<Match> matches = createMatches();
		for(int i= 0; i < matches.size(); i++) {
			currentMatchLine = getMatchCode(matches.get(i));
			shText.add(currentMatchLine);
		}
		
	    try {
			Files.write(fPath, shText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void createMatchupScript() {

		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, MATCHUP_FILENAME);
		LinkedList<String> shText = new LinkedList<>();

	 	String l1 = MATCHUP_INIT;
		shText.add(l1);
		
		if(iteration == 0 && batch == 0) {
			shText.add(createCompileScript());
		}

	 	String currentMatchLine = "";
	 
		LinkedList<Match> matches = createMatches();
		for(int i= 0; i < matches.size(); i++) {
			currentMatchLine = getMatchCode(matches.get(i));
			shText.add(currentMatchLine);
		}
		
	    try {
			Files.write(fPath, shText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void resetPossibleEnemies() {
		possibleEnemies = new ArrayList<>();
		int count = 0;
		for(int i = 0; i < numIndividuals; i++) {
			possibleEnemies.add(count);
			count++;
		}
		for(int i = 0; i < safeBots.size(); i++) {
			possibleEnemies.add(count);
			count++;
		}
		for(int i = 0; i < externBotList.size(); i++) {
			possibleEnemies.add(count);
			count++;
		}
	}
	
	
	public String createCompileScript() {
		String ccode = "";
		ccode += getCompCode(0,0) + "\n";
		for(int i = 0; i< externBotList.size(); i++) {
			ccode += getCompCode(2,i) + "\n";
		}
		return ccode;
	}
	
	public String getMatchCode(Match match) {
		String shcode = "";	
		shcode += MATCHUP_CALL + getMatchTargetHalArg(match, 0) + " " + getMatchTargetHalArg(match, 1);
		if(match.isFourPlayer()) {
			shcode +=  " " + getMatchTargetHalArg(match, 2) + " " + getMatchTargetHalArg(match, 3);
		}
		return shcode;
		
	}
	
	public String getCompCode(int type, int i) {
		String filename = "";
		switch(type) {
		case 2:{
			filename = externBotList.get(i);
			break;
		}

		case 1:
		case 0:
		default: filename = GA_BOT_JAVANAME;
		}
		return "javac " + filename;
	}
	
	public String getMatchTargetHalArg(Match m, int i) {

		String ret = "\"java ";
		int type = m.getType(i);
		switch(type) {
		case 2:{
			String name = externBotList.get(m.getID(i));
			String[] splitName = name.split("\\.");
			ret += splitName[0] + "\"";
			break;
		}

		case 1:{
			String prefix = GA_BOT_CLASSNAME + " " +safeBots.get(m.getID(i));
			ret += prefix + "\"";
			break;
		}
		case 0:
		default: {
			String prefix;
			if(!initrun) {
				prefix = GA_BOT_CLASSNAME + " " + futureBots.get(m.getID(i));
			} else {
				prefix = GA_BOT_CLASSNAME + " " + bots.get(m.getID(i));
			}
			ret += prefix + "\"";
			}
		}
		return ret;
	}

	public LinkedList<Match> createMatches(){ //creates 2pl matches, one planned and one random 4pl match per bot
		LinkedList<Match> matches = new LinkedList<>();

		for(int i = 0; i < numIndividuals; i++) {
			int nid =  (i + 1 + iteration*3)%numIndividuals;
			int nnid =  (i + 2 + iteration*3)%numIndividuals;
			int nnnid = (i + 3 + iteration*3)%numIndividuals;
			for(int gaid = 0; gaid < numIndividuals; gaid++) {
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
			for(int enm = 0; enm < enm; i++) {
				if(possibleEnemies.isEmpty()) {
					resetPossibleEnemies();
				}
				int index = randNum.nextInt(possibleEnemies.size());
				if(possibleEnemies.get(index) == i) { // decrease chance to have 2 same bots playing against each other
					index = randNum.nextInt(possibleEnemies.size());
				}
				int eid = possibleEnemies.get(index);

				if(eid >= numIndividuals + safeBots.size()) {
					eid -= numIndividuals + safeBots.size();
					enemyTypes[enm] = 2;
				} else if (eid >= numIndividuals) {
					eid -= numIndividuals;
					enemyTypes[enm] = 1;
				} else {
					enemyTypes[enm] = 0;
				}
				enemies[enm] = eid;

			}
			
			matches.add(new Match(i, nid, 0, nnid, 0, nnnid, 0));

			matches.add(new Match(i, enemies[0], enemyTypes[0], enemies[1], enemyTypes[1], enemies[2], enemyTypes[2]));
			
		}
		
		return matches;
	}
	
	/*
	 * this should be started from the ke_ba_hal directory (where the .sh script is found too) due to file reading
	 */
    public static void main(final String[] args) throws IOException {
    	randNum = new Random(System.currentTimeMillis());
    	
    	
    	if(args.length==NUM_INIT_ARGS) {
    		System.out.println("HaliteGenAlgo:Initial run");
        	int numIndividuals = Integer.valueOf(args[0]);
        	int iterations = Integer.valueOf(args[1]);
        	int batches = Integer.valueOf(args[2]);
        	String runID = args[3];
        	if(numIndividuals<MIN_BOT_COUNT) {
        		System.out.println("HaliteGenAlgo:Error. Minimum bot amount is " +MIN_BOT_COUNT+", given:" + numIndividuals);
        		return;
        	}
        	if(iterations<NUM_MIN_ITERATIONS) {
        		System.out.println("HaliteGenAlgo:Error. Minimum iteration amount is " +NUM_MIN_ITERATIONS+", given:" + iterations);
        		return;
        	}
        	if(batches<NUM_MIN_BATCHES) {
        		System.out.println("HaliteGenAlgo:Error. Minimum batch amount is " +NUM_MIN_BATCHES+", given:" + batches);
        		return;
        	}

        	HaliteGenAlgo haliteGenAlgo = new HaliteGenAlgo(numIndividuals, iterations, batches, runID);

    	} else if(args.length == NUM_DEF_ARGS) {

        	HaliteGenAlgo haliteGenAlgo = new HaliteGenAlgo(NUM_DEF_INDV, NUM_DEF_ITS, NUM_DEF_BATS, STR_DEF_NAME);

    		
    	} else if(args.length == NUM_CONT_ARGS) {
        	int iterations = Integer.valueOf(args[0]);
    		System.out.println("HaliteGenAlgo:Run " + iterations);
        	
        	HaliteGenAlgo haliteGenAlgo = new HaliteGenAlgo();

    	} else {
    		System.out.println("HaliteGenAlgo:Error. No valid number of arguments (init:"+NUM_INIT_ARGS+", cont:"+NUM_CONT_ARGS+")");
    		return;
    	}
    
    }
 
    
    public static double getAverage(ArrayList<Double> arrayList) {
    	BigDecimal sum = BigDecimal.ZERO;
    	int count = 0;
    	for(Double sc : arrayList) {
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
		System.out.println("c = " + count + " , sum = " + sum + ", avg = " + returnvalue);

    	return returnvalue;
    }
    
	
    
    public static int[] createRandomIntArray(int attNum) {
    	int[] newData = new int[attNum];
    	for(int i = 0; i < newData.length; i++) {
    		newData[i] = randNum.nextInt(ATT_MAX);
    	}
    	return newData;
    }

    
    public static void printInd(Individual indv) {
    	int[] atts = indv.getAttributes();
   
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
    
    
    public static String getSuffixByIt(int it) {
    	String ret = "";
    	int id = it;
    	if(id > 25) {
    		ret += getSuffixByIt((it/26)-1) + getSuffixByIt(it%26);
    	} else {
    		char[] et = Character.toChars(it+65);
    		ret = String.copyValueOf(et);
    	}
    	
    	return ret;
    	
    }
    


    
    
}


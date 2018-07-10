package genAlgo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import hlt.Log;

public class GAFileHandler {
	
	//DEBUG OUTPUTS:
	public static final boolean NO_SH_OUTPUT = true;
	public static final boolean ADD_SH_MATCHNR = true;
	public static final boolean PRINT_INIT_INFO = false;;
	
	public static final boolean OVERWRITE_GAIT = false;
	public static final String CFG_FOLDERNAME = "configs";
	
	public static final String OLD_GA_CFG_FOLDERNAME = "ga_configs_old";

	public static final String BOT_SCR_FOLDERNAME = "scores";
	public static final String SAFE_CFG_FOLDERNAME = "static_configs";
	public static final String GA_CFG_FOLDERNAME = "ga_configs";
	public static final String PRESET_CFG_FOLDERNAME = "preset_configs";
	public static final String RESULT_CFG_FOLDERNAME = "results";
	public static final String MATCHUPS_FILENAME = "matchups.txt";


	public static final String INIT_FILENAME = "init.txt";
	public static final String RANKINGS = "rankings";
	public static final String RANKINGS_FILENAME = "rankings.txt";
	public static final String GAIT_FILENAME = "gaitinfo.txt";
	public static final String SAFE_BOTS_FILENAME = "safebots.txt";
	public static final String EXT_BOTS_FILENAME = "extbots.txt";
	
	public static final String MATCHUP_FILENAME = "_matchup.txt";
	public static final String PRESET_FILENAME = "presets.txt";



	public static final String MATCHES_SH_FILENAME = "matches.sh";
	public static final String MATCHES_SH_INIT = "#!/bin/sh";
	public static final String MATCHES_SH_CALL_NOREPL = "./halite -q -r ";
	public static final String MATCHES_SH_CALL = "./halite ";  //TODO : CURRENTLY ALSO NO OUTPUTS
	public static final String MATCHES_SH_RECALL1 = "java genAlgo/TournamentSelector";
	public static final String MATCHES_SH_RECALL2 = "source configs/matches.sh";
	
	public static final String A_MATCHES_FILENAME = "a_matches.sh";
	public static final String B_MATCHES_FILENAME = "b_matches.sh";
	public static final String C_MATCHES_FILENAME = "c_matches.sh";


	public static final String GA_BOT_JAVANAME = "ModifiedBot.java";
	public static final String GA_BOT_CLASSNAME = "ModifiedBot";
	
	
	public static final String BOT_PREFIX = "GABot";
	public static final String SAFE_BOT_PREFIX = "SGABot";
	
	public static final int INIT_PARAMETERS = 10;
	public static final int IPAM_GA_TYPE = 0; //[0]: standard, [1]: mixed (old/new) [2]:continue (old)
	public static final int IPAM_PLAYERS = 1; //MUST BE [0]:2 or [1...n]:4 Players
	public static final int IPAM_POPSIZE = 2; //8*[1..X]
	public static final int IPAM_INIT_ATT_TYPE = 3; //[0]:random [1]:Q-Weighted [2] Eq-Distributed [3] Distinct-Eq-Distributed
	public static final int IPAM_MATCHES_EXT = 4; //count of repetitions of [GA vs EXT] or [GA vs SB]
	public static final int IPAM_MATCHES_TOURN = 5; //count of repetitions of [GA vs GA]
	public static final int IPAM_OUTPUT_MODE = 6; //which iterations use output (none, first x final, all)
	public static final int IPAM_NUM_MUTATIONS = 7; // maximum number of child mutations
	public static final int IPAM_MUTATION_CHANCE = 8; // chance for a child to mutate
	public static final int IPAM_USE_CUSTOM_SEED = 9; // -s / Seed argument for GA runs on only one map

	//IPAM_GA_TYPE
	public static final int IPAM_GAT_STANDARD = 0;
	public static final int IPAM_GAT_MIXED = 1;
	public static final int IPAM_GAT_CONT = 2;

	//IPAM_INIT_ATT_TYPEs
	public static final int IPAM_IAT_RANDOM = 0;
	public static final int IPAM_IAT_QW = 1;
	public static final int IPAM_IAT_EQDIST = 2;
	public static final int IPAM_IAT_DIST_EQD = 3;
	
	//IPAM_PLAYERS
	public static final int IPAM_PL_2 = 0;
	public static final int IPAM_PL_4 = 1;
	
	//IPAM_OUTPUT_MODEs
	public static final int IPAM_OM_NONE = 0;
	public static final int IPAM_OM_FINAL = 1;
	public static final int IPAM_OM_FIRST_FINAL = 2;
	public static final int IPAM_OM_FIRST = 3;
	public static final int IPAM_OM_ALL = 4;

	
	public static final int GAIT_LINES = 3;
	public static final int GAIT_INIT_ARGS = 6;
	public static final int GAIT_BOTNAME_LINES = 2;
	
	public static final int GAIT_I_IT = 0; //iteration
	public static final int GAIT_I_N_IT = 1; //num iterations
	public static final int GAIT_I_BA = 2; //batch
	public static final int GAIT_I_N_BA = 3; //num batches
	public static final int GAIT_I_FIN = 4; //finished? (int, 1 -> true)
	private static final boolean GET_SF = true;
	
	public static final int CURRENT_LINE = -1;

	
	public static final int ABC_RED_SIMPLE_A = 0;
	public static final int ABC_RED_SIMPLE_B = 1;
	public static final int ABC_RED_SIMPLE_A_1V3 = 2;
	public static final int ABC_RED_SIMPLE_B_1V3 = 3;
	public static final int ABC_RED_FULL = 4;
	public static final int ABC_RED_FULL_1V3 = 5;

	public static final double MEDIAN_SCORE_VAL = 0.45;
	
	private ArrayList<Integer> initParamData;
	private long initStaticSeed;
	private double initMutChance;
	
	boolean initExists;
	boolean gaitLoaded;
	private String gaitID;
	private ArrayList<String> safeBots;
	private ArrayList<String> externBots;
	private ArrayList<Integer> gaitInitData;
	private int matchupLine;
	
	private int matchNum;

	public GAFileHandler() {
		matchupLine = 0;
		gaitID = "_no_id_";
		gaitLoaded = false;
		initExists = false;
		initMutChance = 0;
		initStaticSeed = 0;
	}
	
	
	// ######### GAIT FILE ##########

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
	 *    ## list_bots not needed, can be computed by iteration + num_bots
	 *    
	 * ## LINE 2 ##
	 * num_safe_bots : 0-iteration
	 * list_safe_bots : BOT_PREFIX + "_" + SUFFIX + ID
	 * 
	 * ## LINE 3 ##
	 * num_extern_bots : 0-n
	 * list_extern_bots : name + ".java"

	 * 
	 */
	
	public void readInit(boolean printResults) {
		
		readInit();
		 
		if(!initExists) {
			return;
		}
		
		//String output = "init: ";
		String output = "";
	    for(int i = 0; i < initParamData.size(); i++) {
			int ipam = initParamData.get(i);
			if(i != 0) {
				output += "\n";
			}
	    	switch(i) {
	    	case IPAM_NUM_MUTATIONS:{
	    		output += "max number of child mutations:" + ipam;
	    		break;
	    	}
			case IPAM_OUTPUT_MODE:{
				output += "outputmode= ";
				if(ipam == IPAM_OM_NONE) {
					output+= "none";
				} else if(ipam == IPAM_OM_FINAL) {
					output+= "only last iteration";
				} else if(ipam == IPAM_OM_FIRST_FINAL) {
					output+= "first and last iterations";
				} else if(ipam == IPAM_OM_FIRST) {
					output+= "only first iteration";
				} else{
					output+= "every iteration";
				}
				break;
			}
			case IPAM_MATCHES_TOURN:{
				output += "repeat tourn matches= " + ipam + "x";

				break;
			}
	    	case IPAM_MATCHES_EXT:{
				output += "repeat ext matches= " + ipam + "x";

				break;
			}
	    	case IPAM_INIT_ATT_TYPE:{
				output += "pop attr creation type= ";
				if(ipam == IPAM_IAT_RANDOM) {
					output += "random";
				}
				if(ipam == IPAM_IAT_QW) {
					output += "q-weighted";
				}
				if(ipam == IPAM_IAT_EQDIST) {
					output += "equal_dist";
				}
				if(ipam == IPAM_IAT_DIST_EQD) {
					output += "distinct equal_dist";
				}
				break;
			}
	    	case IPAM_POPSIZE:{
				output += "population size= ";
				output += ipam + "x8 = " + ipam*8;
				break;
			}
			case IPAM_PLAYERS:{
				output += "players per match= ";
				if(ipam == IPAM_PL_2) {
					output += "2";
				}
				if(ipam == IPAM_PL_4) {
					output += "4";
				}
				break;
			}
			case IPAM_GA_TYPE:{
				output += "genetic algorithm type= ";
				if(ipam == IPAM_GAT_STANDARD) {
					output += "standard";
				}
				if(ipam == IPAM_GAT_MIXED) {
					output += "mixed (50% old pop, 50% new pop)";
				}
				if(ipam == IPAM_GAT_CONT) {
					output += "continue (use old pop)";
				}
				break;
			}
			default: {
					
				}
			}
	    }
		output += "\nchild mutation chance:" + initMutChance;

    	if(initStaticSeed == 0) {
    		output += "\nno static seed is used";
    	} else {
    		output += "\nstatic seed: " + initStaticSeed;
    	}
	    System.out.println(output);
	}
	
	public void readInit() {
		
		initStaticSeed = 0;

		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, INIT_FILENAME);
		File file = new File(fPath.toString());
		if(!file.exists()) {
			return;
		}
		
		initParamData = new ArrayList<>(INIT_PARAMETERS);

	    try {
			scanner = new Scanner(new FileInputStream(file), "UTF-8");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    scanner.useLocale(Locale.US);

	    for(int i = 0; i < INIT_PARAMETERS-2; i++) {
		    //if(scanner.hasNext()) {
	    	int nextValue = scanner.nextInt();
	    	initParamData.add(nextValue);
	    }
	    initMutChance = scanner.nextDouble();
	    initStaticSeed = scanner.nextLong();


	    if(initParamData.get(IPAM_POPSIZE) < 2) { //population size should not be less than 2*8
	    	System.out.println("Set populationsize to [2]*8, was too low");
	    	initParamData.set(IPAM_POPSIZE, 2);
	    }
	   
	    /*
	     * 
	     * 
	     * 	public static final int INIT_PARAMETERS = 8;
	public static final int IPAM_GA_TYPE = 0;
	public static final int IPAM_PLAYERS = 1;
	public static final int IPAM_POPSIZE = 2;
	public static final int IPAM_INIT_ATT_TYPE = 3;
	public static final int IPAM_MATCHES_EXT = 4;
	public static final int IPAM_MATCHES_TOURN = 5;
	public static final int IPAM_OUTPUT_MODE = 6;
	public static final int IPAM_USE_CUSTOM_SEED = 7;
	     * 
	     */
	    

	    scanner.close();
	    initExists = true;
		
	}
	
	
	//need to read Init first
	public ArrayList<Integer> getInitData(){
		if(initExists = false) {
			System.out.println("couldnt get initdata (did you call readInit?) ");
			return null;
		}
		return initParamData;
	}

	public long getStaticSeed() {
		return initStaticSeed;
	}
	
	public double getMutationChance() {
		return initMutChance;
	}
	

	public void readGAITinfo(){
		 
		gaitInitData = new ArrayList<>();
		
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
	    	gaitID = scanner.next();
	    }
	    for(int i = 0; i < GAIT_INIT_ARGS - 1; i++) {
		    //if(scanner.hasNext()) {
	    	gaitInitData.add(scanner.nextInt());
	    } //iteration, numIts, batch, numBs, finished


	    //Line 2
	    safeBots = new ArrayList<>();			
	    //if(scanner.hasNext()) {
		int numEntries = scanner.nextInt();
		    //}
		for(int e = 0; e < numEntries; e++) {
			safeBots.add(scanner.next());
		}


	    //Line 3
	    externBots = new ArrayList<>();			
	    //if(scanner.hasNext()) {
		numEntries = scanner.nextInt();
		    //}
		for(int e = 0; e < numEntries; e++) {
			externBots.add(scanner.next());
		}
		
		

	    scanner.close();
	    gaitLoaded = true;
	    //setNextIt(); in haliteGenALgo
	}

	public boolean gaitFileLoaded() {
		return gaitLoaded;
	}
	
	//need to read GAIT first
	public String getGAITID() {
		return gaitID;
	}
	
	//need to read GAIT first
	public ArrayList<String> getBatchBots(){
		return safeBots;
	}
	
	//need to read GAIT first
	public ArrayList<String> getExternBots(){
		return externBots;
	}
	
	//need to read GAIT first
	public ArrayList<Integer> getGaitInit(){
		return gaitInitData;
	}

	
	public void writeToGAIT(String currentRun, int iteration, int numIts, int batch, int nBatches, int fin, ArrayList<String> safeBots, ArrayList<String> extBots){
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
	 	String l1 = currentRun + " " + iteration + " " + numIts + " " + batch + " " + nBatches + " " + fin + " ";
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
	



	// ######### BOT ATTRIBUTES ##########

	
	public void writeBotAtts(int it, int ba, int id, boolean asSafeBot, double[] ds) {
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, GA_CFG_FOLDERNAME, getBotName(it, ba, id) + ".txt");
		if(asSafeBot) {
			fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, SAFE_CFG_FOLDERNAME, getBotName(it, ba, id) + ".txt");
		}
	 	String line = HaliteGenAlgo.NUM_ATTS + " ";
		LinkedList<String> newText = new LinkedList<>();
		for(int i = 0; i < HaliteGenAlgo.NUM_ATTS; i++) {
			line += ds[i];
			if(i < HaliteGenAlgo.NUM_ATTS-1) {
				line += " ";
			}
		}
		//System.out.println("saving bot " +  getBotName(it, ba, id)  + "  data: " + line );
		newText.add(line);
	    try {
			Files.write(fPath, newText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static double[] readBotAtts(int popSize, int id, int it, int ba){
		String name = getBotNames(popSize, it, ba).get(id);
		return readBotAttsByName(name);
	}


	//used by bot, no couts here!
	public static double[] readBotAttsByName(String name){
		double[] res = new double[HaliteGenAlgo.NUM_ATTS]; //5
		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();

		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, GA_CFG_FOLDERNAME, name + ".txt");
		File file = new File(fPath.toString());
    	//if file not exist in bots, look in safeBots else dont load
		if(!file.exists()) {
			Path fPath2 = Paths.get(dir.toString(), CFG_FOLDERNAME, SAFE_CFG_FOLDERNAME, name + ".txt");
    		file = new File(fPath2.toString());
    		if(!file.exists()) {
    			Path fPath3 = Paths.get(dir.toString(), CFG_FOLDERNAME, PRESET_CFG_FOLDERNAME, name + ".txt");
    			Log.log("filename:" + fPath3.toString());
        		file = new File(fPath3.toString());
        		if(!file.exists()) {
        			System.out.println("error! couldnt load given name: " + name);
        		}
    		}

		}
	    try {
			scanner = new Scanner(new FileInputStream(file), "UTF-8");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    
	    scanner.useLocale(Locale.US);
	    String resultstring = "num = ";
	    int numSavedAtts = 0;
	    if(scanner.hasNext()) {
	    	numSavedAtts  = scanner.nextInt();
	    	resultstring += numSavedAtts;
	    }
	    
	    resultstring += ", atts : ";

	    
	    for(int i = 0; i < numSavedAtts; i++) {
	    	//System.out.println("i =" + i + "/"+ numSavedAtts);
	    	if(scanner.hasNext()) {
	    		res[i] = scanner.nextDouble();
	    		resultstring += res[i] + " ";
	    	} else {
				System.out.println("HaliteGenAlgo:Read Error - did not specify enough att values in " + fPath.toString());	
				//throw new IOException();
	    	}
	    }
	    
	   // System.out.println("scanner read the following values:" + resultstring);
	    scanner.close();
	    return res;

	}
	
	public static double[] readOldBotAtts(int popSize, int id){
		String name = getBotNames(popSize, -1, -1).get(id);
		return readOtherBotAttsByName(name, OLD_GA_CFG_FOLDERNAME);
	}
	
	public static double[] readOtherBotAttsByName(String name, String subfoldername){
		double[] res = new double[HaliteGenAlgo.NUM_ATTS]; //5
		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();

		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, subfoldername, name + ".txt");
		File file = new File(fPath.toString());

	    try {
			scanner = new Scanner(new FileInputStream(file), "UTF-8");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    
	    scanner.useLocale(Locale.US);
	    String resultstring = "num = ";
	    int numSavedAtts = 0;
	    if(scanner.hasNext()) {
	    	numSavedAtts  = scanner.nextInt();
	    	resultstring += numSavedAtts;
	    }
	    
	    resultstring += ", atts : ";

	    
	    for(int i = 0; i < numSavedAtts; i++) {
	    	//System.out.println("i =" + i + "/"+ numSavedAtts);
	    	if(scanner.hasNext()) {
	    		res[i] = scanner.nextDouble();
	    		resultstring += res[i] + " ";
	    	} else {
				System.out.println("HaliteGenAlgo:Read Error - did not specify enough att values in " + fPath.toString());	
				//throw new IOException();
	    	}
	    }
	    
	   // System.out.println("scanner read the following values:" + resultstring);
	    scanner.close();
	    return res;

		
	}

	
	public ArrayList<String> readPriorSafeBots() {
		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, SAFE_CFG_FOLDERNAME, SAFE_BOTS_FILENAME);
		File file = new File(fPath.toString());
	    ArrayList<String> prSafeBots = new ArrayList<>();

		if(!file.exists()) { 
			//System.out.println("HaliteGenAlgo:No prior safe bots specified");	
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
		scanner.close();
		return prSafeBots;
	}
	
	public ArrayList<String> readExternBots() {
		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, SAFE_CFG_FOLDERNAME, EXT_BOTS_FILENAME);
		
		File file = new File(fPath.toString());
	    ArrayList<String> externBots = new ArrayList<>();

		if(!file.exists()) { 
			//System.out.println("HaliteGenAlgo:No extern bots specified");	
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
		scanner.close();
		return externBots;
	}
	
	
	// ######### BOT SCORES ##########
	
	public static ArrayList<Double> collectInitScores(int popSize, ArrayList<Integer> botIds, int iteration, int batch, int ebSize, int sbSize, int numExtMatches) {
		ArrayList<Double> scores = new ArrayList<>();
		System.out.println("collectInitScores");
		int numScoresToGet =  getNumInitMatchesPerBot(ebSize, sbSize, numExtMatches);

		for(Integer i : botIds) {
			LinkedList<Double> botScores = GAFileHandler.readBotScores(popSize, i, iteration, batch);

			LinkedList<Double> relevantScores = new LinkedList<Double>();
			for(int j = 0; j < numScoresToGet; j++) { //only count relevant = lastScores
				//System.out.println("cis: botid: " + i + ", numScoreToGet: " +  j  + " / " + numScoresToGet );

				relevantScores.add(botScores.get(j));
			}
			scores.add(HaliteGenAlgo.getAvgScore(relevantScores));
		}
		return scores;
	}

	/**
	 * returns a sorted list of average Scores
	 */
	public static LinkedList<Double> readBotScores(int popSize, int id, int iteration, int batch) {
		
		ArrayList<String> botNames = getBotNames(popSize, iteration, batch);
		if(id >= botNames.size()) {
			System.out.println("ERROR! id does not exist in names");
			return null;
		}
		return readWeightedBotScoresFromName(botNames.get(id));

	}
	
	
	public static LinkedList<Double> readWeightedBotScoresFromName(String botName) {
		LinkedList<Double> botScores = new LinkedList<>();
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, BOT_SCR_FOLDERNAME, botName);
		File folder = new File(fPath.toString());
		if(folder.exists()) {
			File[] fileList = folder.listFiles();

			Arrays.sort(
					fileList, 
					new Comparator<File>(){
			    		public int compare(File f1, File f2){
			    			
			    			String[] splitName1 = f1.getName().split("\\.");
			    			String[] splitName2 = f2.getName().split("\\.");

			    			Integer one = 	Integer.valueOf(splitName1[0]);
			    			Integer two = 	Integer.valueOf(splitName2[0]);

			    			return one.compareTo(two);
			    		} 
			    	}
			);

			
			if(fileList.length != 0) {
				for (File scoreFile : fileList) {
					if (scoreFile.isFile()) {
						double wbotscore = readWeightedBotScore(scoreFile);
						//System.out.println("reading bot "+botNames.get(id)+ " score: " + wbotscore);

						botScores.add(wbotscore);
					}
				}
			} else {
				botScores.add((double) 0);
			}
		} else {
			botScores.add((double) 0);
		}
		if(botScores.isEmpty()) {
			System.out.println("returning empty bot score");
		}
		return botScores;
	}

	public static int readNumBotMatchesFromName(String botName) {
		int numGames = 0;
		
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, BOT_SCR_FOLDERNAME, botName);
		File folder = new File(fPath.toString());
		if(folder.exists()) {
			File[] fileList = folder.listFiles();

			Arrays.sort(
					fileList, 
					new Comparator<File>(){
			    		public int compare(File f1, File f2){
			    			
			    			String[] splitName1 = f1.getName().split("\\.");
			    			String[] splitName2 = f2.getName().split("\\.");

			    			Integer one = 	Integer.valueOf(splitName1[0]);
			    			Integer two = 	Integer.valueOf(splitName2[0]);

			    			return one.compareTo(two);
			    		} 
			    	}
			);

			
			if(fileList.length != 0) {
				for (File scoreFile : fileList) {
					if (scoreFile.isFile()) {

						numGames++;

					}
				}
			} else {
			}
		} else {
		}

		return numGames;
	}

	
	public static int readBotWinsFromName(String botName, boolean preferRefWeighted) {
		int numWins = 0;
		
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, BOT_SCR_FOLDERNAME, botName);
		File folder = new File(fPath.toString());
		if(folder.exists()) {
			File[] fileList = folder.listFiles();

			Arrays.sort(
					fileList, 
					new Comparator<File>(){
			    		public int compare(File f1, File f2){
			    			
			    			String[] splitName1 = f1.getName().split("\\.");
			    			String[] splitName2 = f2.getName().split("\\.");

			    			Integer one = 	Integer.valueOf(splitName1[0]);
			    			Integer two = 	Integer.valueOf(splitName2[0]);

			    			return one.compareTo(two);
			    		} 
			    	}
			);

			
			if(fileList.length != 0) {
				for (File scoreFile : fileList) {
					if (scoreFile.isFile()) {
						if(botScoresIndicWin(scoreFile, false)){
							numWins++;
						}

					}
				}
			} else {
			}
		} else {
		}

		return numWins;
	}

	public static ArrayList<Boolean> readBotWinListFromName(String botName, boolean preferRefWeighted) {
		ArrayList<Boolean> winlist = new ArrayList<>();
		
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, BOT_SCR_FOLDERNAME, botName);
		File folder = new File(fPath.toString());
		if(folder.exists()) {
			File[] fileList = folder.listFiles();

			Arrays.sort(
					fileList, 
					new Comparator<File>(){
			    		public int compare(File f1, File f2){
			    			
			    			String[] splitName1 = f1.getName().split("\\.");
			    			String[] splitName2 = f2.getName().split("\\.");

			    			Integer one = 	Integer.valueOf(splitName1[0]);
			    			Integer two = 	Integer.valueOf(splitName2[0]);

			    			return one.compareTo(two);
			    		} 
			    	}
			);

			
			if(fileList.length != 0) {
				for (File scoreFile : fileList) {
					if (scoreFile.isFile()) {
						if(botScoresIndicWin(scoreFile, false)){
							winlist.add(true);
						} else {
							winlist.add(false);
						}

					}
				}
			} else {
			}
		} else {
		}

		return winlist;
	}

	
	public static LinkedList<Double> readScoresFromFile(File file){
		Scanner scanner = null;
		try {
			scanner = new Scanner(new FileInputStream(file), "UTF-8");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    
	    scanner.useLocale(Locale.US);
	    LinkedList<Double> scores = new LinkedList<>();
		while(scanner.hasNext()) {
			scores.add(scanner.nextDouble());
		}
		scanner.close();
		return scores;
	}
	
	public static ArrayList<Double> getWeightedBotScoreListFromName(String botName) {
		ArrayList<Double> scorelist = new ArrayList<>();

		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, BOT_SCR_FOLDERNAME, botName);
		File folder = new File(fPath.toString());
		if(folder.exists()) {
			File[] fileList = folder.listFiles();

			Arrays.sort(
					fileList, 
					new Comparator<File>(){
			    		public int compare(File f1, File f2){
			    			
			    			String[] splitName1 = f1.getName().split("\\.");
			    			String[] splitName2 = f2.getName().split("\\.");

			    			Integer one = 	Integer.valueOf(splitName1[0]);
			    			Integer two = 	Integer.valueOf(splitName2[0]);

			    			return one.compareTo(two);
			    		} 
			    	}
			);

			
			if(fileList.length != 0) {
				for (File scoreFile : fileList) {
					if (scoreFile.isFile()) {
						LinkedList<Double> scores = readScoresFromFile(scoreFile);
						double wscores = getWeightedBotScore(scores);
						scorelist.add(wscores);
					}
				}
			} 
		}
		return scorelist;
	}

	public static boolean botScoresIndicWin(File file, boolean preferRefWeighted) {

		if(preferRefWeighted) { //check reference scores
			LinkedList<Double> scores = readScoresFromFile(file);
			double scoreToCheck = getWeightedBotScore(scores);
			double refScore = referenceScore(scores.size(), MEDIAN_SCORE_VAL);
			if(scoreToCheck > refScore) {
				return true;
			}  else if(scoreToCheck == refScore) { //check last score
				double lScore = readScoresFromFile(file).getLast();
				if(lScore > 0.501) {
					return true;
				}
			} else {
				return false;
			}
		}
		
		double lastScore = readScoresFromFile(file).getLast();
		if(lastScore > 0.5) { //check last score
			return true;
		}  else if(lastScore <= 0.501 && lastScore >= 0.499) { //check reference scores
			LinkedList<Double> scrs = readScoresFromFile(file);
			double scrsToCheck = getWeightedBotScore(scrs);
			double rScore = referenceScore(scrs.size(), MEDIAN_SCORE_VAL);
			if(scrsToCheck > rScore) {
				return true;
			}
		}
		return false;
	}

	public static double referenceScore(int numScores, double testScore) {
		
		LinkedList<Double> scores = new LinkedList<>();
		for(int i = 0; i < numScores; i++) {
			scores.add(testScore);
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
		return weightedScore;
	}
	
	public static double getWeightedBotScore(LinkedList<Double> scores) {
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
		
		return weightedScore;
		
	}
	
	public static double readWeightedBotScore(File file) {
		
		return getWeightedBotScore(readScoresFromFile(file));
	}
	
	public static void clearAllScoresFolder(){
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
	
	public static void deleteFolder(File dir) {
	    File[] files = dir.listFiles();
	    if(files!=null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            if(f.isDirectory()) {
	                deleteFolder(f);
	            } else {
	                f.delete();
	            }
	        }
	    }
	    dir.delete();
	}
	
	
	
	// ######### RANKINGS ##########
	public static void resetRankingsFile(int it, int ba) {
		Path dir = Paths.get(".").toAbsolutePath().normalize();

		Path renamePath = Paths.get(dir.toString(), CFG_FOLDERNAME, RANKINGS+it+"_"+ba+".txt");
		File rnFile = new File(renamePath.toString());
		if(rnFile.exists()) {
			try {
				Files.delete(renamePath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, RANKINGS_FILENAME);
		File nFile = new File(fPath.toString());
		nFile.renameTo(rnFile);
		
		LinkedList<String> sText = new LinkedList<>();
		String init = "-1";
		sText.add(init);
		
		try {
			Files.write(fPath, sText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	

	
	
	public static int readRankingsLine() {
		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, RANKINGS_FILENAME);
		File file = new File(fPath.toString());
	    try {
			scanner = new Scanner(new FileInputStream(file), "UTF-8");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    
	    return  scanner.nextInt();
	}


	//needs gait loaded
    public double getStarterRatingScore(int popSize, int id, boolean type) {
    	
		int sbotLine = 1;
    	if(!externBots.isEmpty()) {
    		if(type!= GET_SF) {
        		return getRankOf(popSize, id, 1);
    		}
    		sbotLine++;
    	}
    	if(type != GET_SF) {
    		return -1;
    	}
    	
    	
    	if(!safeBots.isEmpty()) {
    		return getRankOf(popSize, id, sbotLine);
    	}
    	return -1;

    	
    }
	
	public static int getRankOf(int popSize, int id, int currentLine) {
		if(currentLine <= 0) {
			return popSize -1;
		}
		ArrayList<Integer> currentRatings = GAFileHandler.readRankings(popSize, currentLine);
		if(!currentRatings.contains(id)) {
			return getRankOf(popSize, id, currentLine-1);
		} else {
			for(int i = 0; i < currentRatings.size(); i++) {
				//System.out.println("getRankOf(" +id+","+currentLine+") : checking i=" +i+" : cr(i)=" + currentRatings.get(i));
				if(currentRatings.get(i) == id) {
					return i;
				}
			}
		}
		return currentRatings.size()-1;
	}
	
	
	public static ArrayList<Integer> readRankings(int popSize, int number){
		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, RANKINGS_FILENAME);
		File file = new File(fPath.toString());
	    try {
			scanner = new Scanner(new FileInputStream(file), "UTF-8");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	  
		ArrayList<Integer> rankings = new ArrayList<>();			
	    //if(scanner.hasNext()) {
		    //}
		
		int currentLine = scanner.nextInt();
		if(number > currentLine) {
			return readRankings(popSize, number-1);
		}
	    //skip lines until at the right pos
	    for(int i = 0; i < number; i++) {
	    	scanner.nextLine();
	    }
		
		for(int e = 0; e < popSize; e++) {
			rankings.add(scanner.nextInt());
		}
	    scanner.close();

		return rankings;
	}
	
	
	public static ArrayList<Integer> readLastRankings(int popSize){
		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, RANKINGS_FILENAME);
		File file = new File(fPath.toString());
	    try {
			scanner = new Scanner(new FileInputStream(file), "UTF-8");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	  
		ArrayList<Integer> rankings = new ArrayList<>();			
	    //if(scanner.hasNext()) {
		    //}
		
		int currentLine = scanner.nextInt();
		if(currentLine == 0) {
			System.out.println("cannot read rankings, empty yet");
		}
		
		//skip lines until at the right pos
	    for(int i = 0; i < currentLine; i++) {
	    	scanner.nextLine();
	    }
		
		for(int e = 0; e <popSize; e++) {
			rankings.add(scanner.nextInt());
		}
	    scanner.close();

		return rankings;
	}

	public static void addRankings(HashMap<Integer, Integer> rankings) { 
		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, RANKINGS_FILENAME);
		File file = new File(fPath.toString());

		LinkedList<String> editText = new LinkedList<>();
	    try {
			scanner = new Scanner(new FileInputStream(file), "UTF-8");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    

		int currentLine = scanner.nextInt();
	    if(currentLine == -1) {
	    	currentLine = 0;
	    }
		editText.add(String.valueOf(currentLine+1));
		scanner.nextLine();
		
		for(int i = 0; i < currentLine; i++) {
			editText.add(scanner.nextLine());
		}
		
		scanner.close();
		//System.out.println("adding "+rankings.size() + "rankings..");

		String ranks = "";
		for(Map.Entry<Integer, Integer> me : rankings.entrySet()) {
			ranks += me.getValue() + " ";
		}
		editText.add(ranks);
		//System.out.println("edittext: " + editText);

		
	    try {
			Files.write(fPath, editText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static HashMap<Integer, Integer> createRankings(int popSize, ArrayList<Integer> botIds, ArrayList<Double> scores, int rankingLine, int rankingSize){
		
		 // rankings: is a map of ids, sorted by ranking:
		 // ranking(key):			0 1 2 3 4 5 6 7..
		 // corresponding id: 	    5 2 1 7 0 6 3 4
		 
		//System.out.println("create rankings: scores = "+ scores.size() + " / botids = "+botIds.size());

		HashMap<Integer, Integer> rankings = new HashMap<>(popSize);
		ArrayList<ScoreRef> scoreList = new ArrayList<>();
		for(int i = 0; i < botIds.size(); i++) {
			//System.out.println("adding to winnerrankings: = " + scores.get(i) + "/Bot:" +botIds.get(i));

			scoreList.add(new ScoreRef(scores.get(i), botIds.get(i)));
		}
		//System.out.println("winnerrankings setsize = " + scoreList.size());
		Collections.sort(
				scoreList, 
				new Comparator<ScoreRef>(){
		    		public int compare(ScoreRef sr1, ScoreRef sr2){
		    			return Double.compare(sr1.getScore(), sr2.getScore());
		    		} 
		    	}
		);
		int wsind = scores.size()-1;
		for(ScoreRef sr : scoreList) {
			rankings.put(wsind, sr.getId());
			//System.out.println("winnerscore-Index = " + wsind + ", index-value in scorelist = " + sr.getId());
			wsind--;

		}
		//System.out.println("rankings with winners = " + rankings.size());
		
		
		int rankLine = rankingLine;
		if(rankLine == CURRENT_LINE) {
			rankLine = readRankingsLine();
		}

		for(int i = 0; i < rankingSize; i++) {
			//boolean rankingAdded = false;
			if(!botIds.contains(i)) { 
				if(GAFileHandler.getRankOf(popSize, i, rankLine) < scores.size()) {
					System.out.println("createRankings: warning - a winner bot gets overwritten");
				}
				rankings.put(GAFileHandler.getRankOf(popSize, i, rankLine), i);
			}
		}
		return rankings;
	}
	
	

	public static HashMap<Integer, Integer> createGroupOrderedRankings(int popSize, ArrayList<Integer> botIds, ArrayList<Double> scores, int rankingLine, int rankingSize, ArrayList<ArrayList<Integer>> groups){
		/*
		 * rankings: is a map of ids, sorted by ranking:
		 * ranking(key):			0 1 2 3 4 5 6 7..
		 * corresponding id: 	    5 2 1 7 0 6 3 4
		 */
		//System.out.println("create rankings: scores = "+ scores.size() + " / botids = "+botIds.size());
		

		
		int groupCount = 0;
		//for(ArrayList<Integer> group : groups) {
		//	String output = "Group: " + groupCount + ": ";
		//	for(Integer i : group) {
		//		output += i + " ";
		//	}
		//	System.out.println(output);
		//	groupCount++;
		//}
		
		
		HashMap<Integer, Integer> rankings = new HashMap<>(popSize);
		ArrayList<ScoreRef> scoreList = new ArrayList<>();
		for(int i = 0; i < botIds.size(); i++) {
			//System.out.println("adding to winnerrankings: = " + scores.get(i) + "/Bot:" +botIds.get(i));

			scoreList.add(new ScoreRef(scores.get(i), botIds.get(i)));
		}
		//System.out.println("winnerrankings setsize = " + scoreList.size());
		Collections.sort(
				scoreList, 
				new Comparator<ScoreRef>(){
		    		public int compare(ScoreRef sr1, ScoreRef sr2){
		    			return Double.compare(sr1.getScore(), sr2.getScore());
		    		} 
		    	}
		);
		int wsind = scores.size()-1;
		for(ScoreRef sr : scoreList) {
			rankings.put(wsind, sr.getId());
			//System.out.println("winnerscore-Index = " + wsind + ", index-value in scorelist = " + sr.getId());
			wsind--;

		}
		//System.out.println("rankings with  " + rankings.size() + " winners before reorder:");
		
		//for(Map.Entry<Integer, Integer> rankentry : rankings.entrySet()) {
		//	System.out.println("rank at " + rankentry.getKey() + " = " + rankentry.getValue());
			
		//}
		
		int numGroups = groups.size();
		int numWinnersPerGroup = groups.get(0).size()/2;
		HashMap<Integer, Integer> groupRankings = new HashMap<>(popSize);
		int grCount = 0;
		for(ArrayList<Integer> gr:groups) { //for each group
			for(int j = 0; j < numWinnersPerGroup; j++) { //for number of winners per group

				for(Map.Entry<Integer, Integer> mapentry : rankings.entrySet()) {
					//String outp = "group " + grCount + ", groupmember it: " + j + ", checking rank: " + mapentry.getKey() + "/index: " + mapentry.getValue();

					int winnerIndex = mapentry.getValue(); //TODO!! CHECK IF VALUES ARE CORRECT
					
					boolean grncontval = false;
					boolean gcontval = false;

					if(!groupRankings.containsValue(winnerIndex)){
						grncontval = true;
					}
					if(gr.contains(winnerIndex)){
						gcontval = true;
					}
					//outp += ", idNotUsed:"+ grncontval +", idInRightGroup:" + gcontval;
					
					if(!groupRankings.containsValue(winnerIndex) && gr.contains(winnerIndex)) {
						int targetRanking = (grCount + (j*numGroups)); //wrong?
						//outp += "=> valid, targetRanking = " + targetRanking;
						//System.out.println(outp);
						groupRankings.put(targetRanking, winnerIndex);
						rankings.remove(mapentry.getKey());
						break;
					} else {
						//System.out.println(outp + " => not valid");
					}
				}
			}
			grCount++;
		}
		
		int currentPos = groupRankings.size();
		for(Map.Entry<Integer, Integer> mapentry : rankings.entrySet()) {
			groupRankings.put(currentPos, mapentry.getValue());
			currentPos++;
		}
		
		
		/*
		for(Map.Entry<Integer, Integer> mapentry : rankings.entrySet()) {
			groupRankings.put(mapentry.getKey(), mapentry.getValue());
		} //add missing rankings to the ranking set
		*/
		rankings = groupRankings;
		//System.out.println("rankings after reorder:");
		
		//for(Map.Entry<Integer, Integer> rankentry : rankings.entrySet()) {
		//	System.out.println("rank at " + rankentry.getKey() + " = " + rankentry.getValue());
		//}
		
		
		int rankLine = rankingLine;
		if(rankLine == CURRENT_LINE) {
			rankLine = readRankingsLine();
		}

		for(int i = 0; i < rankingSize; i++) {
			//boolean rankingAdded = false;
			if(!botIds.contains(i)) { 
				if(GAFileHandler.getRankOf(popSize, i, rankLine) < scores.size()) {
					System.out.println("createRankings: warning - a winner bot gets overwritten");
				}
				rankings.put(GAFileHandler.getRankOf(popSize, i, rankLine), i);
			}
		}
		return rankings;
	}
	
	
	public static int getIndIn(int myId, ArrayList<Integer> targetList) {
		for(int i = 0; i < targetList.size(); i++) {
			if(targetList.get(i) == myId) {
				return i;
			}
		}
		return -1;
	}
	
	/*ArrayList<Integer> id_ranks
	 * Rankings: is a list of rankings for each id:
	 * (id) 	   0 1 2 3 4 5 6..
	 * ranking: 	   5 2 1 4 0 6 3
	 */
	
	
	
	// ######### MATCHUP FILE ##########

	public int getMatchupLine() {
		return matchupLine;
	}

	private static int getNumInitMatchesPerBot(int ebSize, int sbSize, int numExtMatches) {
		int numberMatches = numExtMatches * (ebSize/2 + ebSize%2);
		numberMatches += numExtMatches * (sbSize/2 + sbSize%2);

		return numberMatches;
	}


	
	public void createMatchupFile(int popSize, ArrayList<String> cbots, ArrayList<String> sbots, ArrayList<String> extbots, boolean twoPlayer) {
		//System.out.println("GAFH:creating matchup file");

		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, MATCHUP_FILENAME);
		LinkedList<String> muText = new LinkedList<>();
		
	 	String l_init = "0"; //current Round, initialized with 0 -> indicates which line the tournament selector should read
		muText.add(l_init);
		
		
		

		//EXTBOTS
		muText.add(createExtBotMatchupFileText(popSize, extbots, twoPlayer));
		
		//SAFEBOTS
		muText.add(createSafeBotMatchupFileText(popSize, sbots, twoPlayer));
		
		//GABOTS
		muText.add(createGAMatchupFileText(popSize, cbots, twoPlayer));
		
	    try {
			Files.write(fPath, muText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			System.out.println("createMatchups: saving error");

			e.printStackTrace();
		}
	}
	
	private String createExtBotMatchupFileText(int popSize, ArrayList<String> extbots, boolean twoPlayer){
		//System.out.println("createExtBotMatchupFileText called");

		//NUM_ROUNDS + MATCH_i .. MATCH_n
		//line 1 extern matches: GA1 vs GA2 vs EXT1 vs EXT2 for each
		
		
		
		int numberMatches4 = (extbots.size()/2 + extbots.size()%2) * popSize / 2;
		String l_ext4 = numberMatches4 + " ";
		for(int i = 0; i < popSize; i=i+2) {
			for(int e = 0; e < extbots.size(); e=e+2) {
				l_ext4 += i + " ";
				l_ext4 += i+1 + " ";

				l_ext4+= e + " ";
				if(e+1 >= extbots.size()) {
					l_ext4+= e + " ";
				}
				else l_ext4+= (e+1) + " ";
			}
		}
		return l_ext4;
	}
	
	private String createSafeBotMatchupFileText(int popSize, ArrayList<String> sbots, boolean twoPlayer){
		//System.out.println("createSafeBotMatchupFileText called, sbots size="+sbots.size());
		
		//TWO PLAYERS: line 2 safeBot matches: GA1 vs SB1, GA1 vs SB2 .
		if(twoPlayer) {
			int numberMatches2 = sbots.size() * popSize;

			String l_sb2 = numberMatches2 + " ";
			for(int i = 0; i < popSize; i++) {
				for(int s = 0; s < sbots.size(); s++) {
					l_sb2 += i + " ";
					l_sb2 += s + " ";
				}
			}

			return l_sb2;
		}
		
		//FOUR PLAYERS: line 2 safeBot matches: GA1 vs GA2 vs SB1 vs SB2
		int numberMatches4 = (sbots.size()/2 + sbots.size()%2) * popSize / 2;

		String l_sb4 = numberMatches4 + " ";
		for(int i = 0; i < popSize; i=i+2) {
			for(int s = 0; s < sbots.size(); s=s+2) {
				l_sb4 += i + " ";
				l_sb4 += i+1 + " ";

				l_sb4+= s + " ";
				if(s+1 >= sbots.size()) {
					l_sb4+= s + " ";
				}
				else l_sb4+= (s+1) + " ";
			}
		}

		return l_sb4;
	}

	private String createGAMatchupFileText(int popSize, ArrayList<String> c_bots, boolean twoPlayer){
		//System.out.println("GAFH:createGAMatchupFileText called");
		int playersPerGame = 4; 		//line 3 round1 matches: GA1 vs GA2 vs GA3 vs GA4 (need 4 arguments per match)

		if(twoPlayer) { 		//line 3 round1 matches: GA1 vs GA2 (need 2 arguments per match)
			playersPerGame = 2;
		}
		int numberMatches = popSize / playersPerGame;
		//System.out.println("GAFH:createGAMatchupFileText : numMatches = "+numberMatches+", playersPerGame="+playersPerGame+", numIndv="+HaliteGenAlgo.NUM_INDV);
		String l_t = numberMatches + " ";
		for(int g = 0; g < numberMatches; g++) {
			for(int i = 0; i < playersPerGame; i++) {
				l_t += (g*playersPerGame + i) + " ";
			}
		}
		
		return l_t;
		
				
	}

	
	
	public static void updateMatchupLine() {
		//System.out.println("updating matchup line");
		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, MATCHUP_FILENAME);
		File file = new File(fPath.toString());

		LinkedList<String> editText = new LinkedList<>();
	    try {
			scanner = new Scanner(new FileInputStream(file), "UTF-8");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }

		int currentLine = scanner.nextInt();
		editText.add(String.valueOf(currentLine+1));
		scanner.nextLine();

		while(scanner.hasNextLine()) {
			editText.add(scanner.nextLine());
		}


		scanner.close();
	    try {
			Files.write(fPath, editText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	
	public static void addMatchup(ArrayList<Match> nextMatchups) {	
		//System.out.println("adding to matchup file");

			Scanner scanner = null;
			Path dir = Paths.get(".").toAbsolutePath().normalize();
			Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, MATCHUP_FILENAME);
			File file = new File(fPath.toString());

			LinkedList<String> editText = new LinkedList<>();
		    try {
				scanner = new Scanner(new FileInputStream(file), "UTF-8");
		    } catch (FileNotFoundException e) {
		        e.printStackTrace();  
		    }
		    
			int currentLine = scanner.nextInt();
			editText.add(String.valueOf(currentLine+1));
			scanner.nextLine();
			
			while(scanner.hasNextLine()) {
				editText.add(scanner.nextLine());
			}
			
			scanner.close();
			
			int numberMatches = nextMatchups.size();
			String matchline = numberMatches + " ";
			for(Match m : nextMatchups) {
				if(m.isFourPlayer()) { //four-player matchups
					matchline += m.getID(0)+" "+m.getID(1)+" "+m.getID(2)+" "+m.getID(3) + " ";
				} else { //two-player matchups
					matchline += m.getID(0)+" "+m.getID(1)+" ";
				}
			}
			
			editText.add(matchline);
			//System.out.println("addMatchup: matchuptext = "  +matchline);

		    try {
				Files.write(fPath, editText, Charset.forName("UTF-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	
    
	public ArrayList<Match> readMatchup(int line, boolean twoPlayer) {
    	//System.out.println("readMatchup: line "+ line);


		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, MATCHUP_FILENAME);
	    try {
			scanner = new Scanner(new File(fPath.toString()));
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    
		//read current matchup line (first line (init))
	    if(line == CURRENT_LINE) {
	    	matchupLine = scanner.nextInt();
	    	//System.out.println("GAFH:readMatchup: current line, line nr: "+matchupLine);
	    } else {
	    	scanner.nextInt();
	    	matchupLine = line;
	    }
    	scanner.nextLine();
	    
	    //skip lines until at the right pos
	    for(int i = 0; i < matchupLine; i++) {
	    	scanner.nextLine();
	    }
	    
	    
	    //read number of matches (first value per line)
	    int numMatchesThisLine = scanner.nextInt(); //ERRORED

    	int numEntriesPerMatch = 4;
	    if(twoPlayer) {
	    	numEntriesPerMatch = 2;
	    }
	    
    	int[] playerIDs;
    	int matchupType = 0;
	    ArrayList<Match> matches = new ArrayList<>(numMatchesThisLine);

	    if(matchupLine == 0) {
	    	playerIDs = new int[numEntriesPerMatch];
	    	matchupType = Match.TYPE_EXT;
	    } else if(matchupLine == 1) {
	    	playerIDs = new int[numEntriesPerMatch];
	    	matchupType = Match.TYPE_SAFE;
	    } else {
	    	playerIDs = new int[numEntriesPerMatch];
	    	matchupType = Match.TYPE_GA;
	    }
	    
	    for(int i = 0; i < numMatchesThisLine;i++) {
	    	for(int e = 0; e < numEntriesPerMatch; e++) {
	    		playerIDs[e] = scanner.nextInt();
	    		//System.out.println("scanned : " + playerIDs[e]);
	    	}
	    	if(numEntriesPerMatch == 4) {
	    		//2x GA-Bot + 2x EXT/SAFE/GA-Bot
		    	matches.add(new Match(playerIDs[0], playerIDs[1], Match.TYPE_GA, playerIDs[2], matchupType, playerIDs[3], matchupType));
	    	} else { //1x GA-Bot + 1x EXT/SAFE/GA-Bot
	    		matches.add(new Match(playerIDs[0], playerIDs[1], matchupType));
	    	}
	    }

    	//System.out.println("readmatchup ("+matchupLine+"), entries/Match = " + numEntriesPerMatch + ", matches:" + matches.size()+ "/" +numMatchesThisLine);

	    scanner.close();
	    
	    return matches;
	    
	}
	
	
	// ######### MATCHES.SH FILE ##########

	
	public String getMatchTargetHalArg(Match m, int i, ArrayList<String> targetBots, ArrayList<String> sBots) { //target: init-> current else-> future

		String ret = "\"java ";
		int type = m.getType(i);
		switch(type) {
		case 2:{
			String name = externBots.get(m.getID(i));
			String[] splitName = name.split("\\.");
			ret += splitName[0] + "\"";
			break;
		}

		case 1:{
			String prefix = GA_BOT_CLASSNAME + " " +sBots.get(m.getID(i));
			ret += prefix + "\"";
			break;
		}
		case 0:
		default: {
			String prefix;
				prefix = GA_BOT_CLASSNAME + " " + targetBots.get(m.getID(i));
			ret += prefix + "\"";
			}
		}
		return ret;
	}
	
	public static String getGATargetHalArg(String botName) { //target: init-> current else-> future
		String ret = "\"java "+ GA_BOT_CLASSNAME + " " + botName + "\"";
		return ret;
	}
	
	public String createCompileScript() {
		String ccode = "";
		ccode += getCompCode(0,0) + "\n";
		for(int i = 0; i< externBots.size(); i++) {
			ccode += getCompCode(2,i) + "\n";
		}
		return ccode;
	}	
	
	/*
	//init call! creates first matches sh file
	public void createMatchesSh(int popSize, LinkedList<Match> matches, ArrayList<String> sBots, long seed) {

		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, MATCHES_SH_FILENAME);
		LinkedList<String> shText = new LinkedList<>();

	 	String l1 = MATCHES_SH_INIT;
		shText.add(l1);
		
		shText.add(createCompileScript());

	 	String currentMatchLine = "";
	 
		for(int i= 0; i < matches.size(); i++) {
			currentMatchLine = getMatchShCode(matches.get(i), getBotNames(popSize,0,0), sBots, false, seed);
			shText.add(currentMatchLine);
		}
		
	    try {
			Files.write(fPath, shText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	*/
	/* OLD:
	// read gait first
	public void createNextMatchesSh(LinkedList<Match> matches) {
		
	    int[] next = HaliteGenAlgo.getNextItBa(gaitInitData.get(GAIT_I_IT), gaitInitData.get(GAIT_I_BA), gaitInitData.get(GAIT_I_N_IT), gaitInitData.get(GAIT_I_N_BA));
	    int nextBatch = next[HaliteGenAlgo.NEXT_BA];
	    int nextIt = next[HaliteGenAlgo.NEXT_IT];
	    
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, MATCHES_SH_FILENAME);
		LinkedList<String> shText = new LinkedList<>();
		
	 	String l1 = MATCHES_SH_INIT;
		shText.add(l1);

	 	String currentMatchLine = "";
	 
		for(int i= 0; i < matches.size(); i++) {
			
			currentMatchLine = getMatchShCode(matches.get(i), getBotNames(nextIt, nextBatch));
			shText.add(currentMatchLine);
		}
		
	    try {
			Files.write(fPath, shText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	} */
	
	// read gait first
		public void createNextMatchesSh(int popSize, ArrayList<Match> matches, int numRepeats, boolean noReplay, ArrayList<String> sBots, long seed) {
			
			//System.out.println("GAFH:createNextMatchesSh(popsize:"+popSize+", matchsize"+matches.size()+", numRepeats"+numRepeats+", botsize"+sBots.size()+")");

			Path dir = Paths.get(".").toAbsolutePath().normalize();
			Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, MATCHES_SH_FILENAME);
			LinkedList<String> shText = new LinkedList<>();
			
		 	String l1 = MATCHES_SH_INIT;
			shText.add(l1);

		 	String currentMatchLine = "";
		 
		 	Match.printMatches(matches, numRepeats);
		 	matchNum = 0;
		 	for(int j = 0; j< numRepeats; j++) {

				for(int i= 0; i < matches.size(); i++) {
					currentMatchLine = getMatchShCode(matches.get(i), getBotNames(popSize, gaitInitData.get(GAIT_I_IT), gaitInitData.get(GAIT_I_BA)),sBots, noReplay, seed);
					shText.add(currentMatchLine);
				}
		 	}
			
			shText.add(MATCHES_SH_RECALL1);
			shText.add(MATCHES_SH_RECALL2);

			
		    try {
				Files.write(fPath, shText, Charset.forName("UTF-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	public static void clearMatchesSh() {

		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, MATCHES_SH_FILENAME);
		LinkedList<String> shText = new LinkedList<>();
	
		String l1 = MATCHES_SH_INIT;
		shText.add(l1);
			
		   try {
			Files.write(fPath, shText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getMatchShCode(Match match, ArrayList<String> targetBots, ArrayList<String> sBots , boolean noReplay, long seed) {
		String shcode = MATCHES_SH_CALL;	
		if(noReplay) {
			shcode = MATCHES_SH_CALL_NOREPL;
		}
		if(seed != 0) {
			shcode += "-s " + seed + " ";
		}
		shcode += getMatchTargetHalArg(match, 0, targetBots, sBots) + " " + getMatchTargetHalArg(match, 1, targetBots, sBots);
		if(match.isFourPlayer()) {
			shcode +=  " " + getMatchTargetHalArg(match, 2, targetBots, sBots) + " " + getMatchTargetHalArg(match, 3, targetBots, sBots);
		}
		
		if(NO_SH_OUTPUT) {
			shcode += " > nul";
		}
		
		if(ADD_SH_MATCHNR) {
			shcode += "\n echo \"" + matchNum + "\"";
			matchNum++;
		}
		
		return shcode;
	}
	
	public String getSimpleMatchShCode(String bot1, String bot2, String bot3, String bot4, boolean twoPlayer, boolean noReplay, long seed) {
		String shcode = MATCHES_SH_CALL;	
		if(noReplay) {
			shcode = MATCHES_SH_CALL_NOREPL;
		}
		if(seed != 0) {
			shcode += "-s " + seed + " ";
		}
		shcode += getGATargetHalArg(bot1) + " " + getGATargetHalArg(bot2);
		if(!twoPlayer) {
			shcode +=  " " + getGATargetHalArg(bot3) + " " + getGATargetHalArg(bot4);
		}
		if(NO_SH_OUTPUT) {
			shcode += " > nul";
		}
		
		if(ADD_SH_MATCHNR) {
			shcode += "\n echo \"" + matchNum + "\"";
			matchNum++;
		}
		
		return shcode;
	}
	
	public String getCompCode(int type, int i) {
		String filename = "";
		switch(type) {
		case 2:{
			filename = externBots.get(i);
			break;
		}

		case 1:
		case 0:
		default: filename = GA_BOT_JAVANAME;
		}
		return "javac " + filename;
	}
	
	
	// ################ ABC MATCHUP ################# 	

	public void createAbcMatchup(int popSize, ArrayList<String> firstbots, ArrayList<String> lastbots, ArrayList<String> presetBots, int numAmatches, int numBmatches, int numCmatches, 
								boolean noReplay, long seed, boolean twoPlayer, int reduced) {

		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path afPath = Paths.get(dir.toString(), CFG_FOLDERNAME, A_MATCHES_FILENAME);
		Path bfPath = Paths.get(dir.toString(), CFG_FOLDERNAME, B_MATCHES_FILENAME);
		Path cfPath = Paths.get(dir.toString(), CFG_FOLDERNAME, C_MATCHES_FILENAME);
		
		LinkedList<String> aText = new LinkedList<>();
		LinkedList<String> bText = new LinkedList<>();
		LinkedList<String> cText = new LinkedList<>();

		//THIS MIGHT LEAD TO STRING SIZE PROBLEMS IF MANY MATCHES ARE GIVEN
		
		
	 	String initline = MATCHES_SH_INIT;
	 	aText.add(initline);
	 	bText.add(initline);
	 	cText.add(initline);

	 	aText.add("\n echo \"a-matches(first/final):\"");
	 	bText.add("\n echo \"b-matches(first/preset):\"");
	 	cText.add("\n echo \"c-matches(final/preset):\"");

	 	matchNum = 0;
		System.out.println("GAFH:creating A-matchup sh file");
	 	aText.add(getABCMatchLines(numAmatches, firstbots, lastbots, noReplay, seed, twoPlayer, reduced, 0));
		System.out.println("GAFH:creating B-matchup sh file");
	 	bText.add(getABCMatchLines(numBmatches, presetBots, firstbots, noReplay, seed, twoPlayer, reduced, 1));
		System.out.println("GAFH:creating C-matchup sh file");
	 	cText.add(getABCMatchLines(numCmatches, lastbots, presetBots, noReplay, seed, twoPlayer, reduced, 2));

		
	    try {
			Files.write(afPath, aText, Charset.forName("UTF-8"));
			Files.write(bfPath, bText, Charset.forName("UTF-8"));
			Files.write(cfPath, cText, Charset.forName("UTF-8"));

		} catch (IOException e) {
			System.out.println("createAbcMatchup: saving error");

			e.printStackTrace();
		}
		System.out.println("GAFH: done creating abc matchup sh files");

	}
	
	//create Matches between first & final bots // A-MATCHES
	public String getABCMatchLines(int numRepeats, ArrayList<String> firstBots, ArrayList<String> secondBots, boolean noReplay, long seed, boolean twoPlayer, int reducedType, int progNum) {
		String matchCodeLines = "";
		//public static final int ABC_RED_SIMPLE_A = 0;
		//public static final int ABC_RED_SIMPLE_B = 1;
		//public static final int ABC_RED_SIMPLE_1V3 = 2;
		//public static final int ABC_RED_FULL = 3;
		//reducedType = 0: reduced, 1: not reduced, 
		//2-2 -> this will already create 16000 matches only for first/final bot. Reduced version added.
		ArrayList<Match> saveMatchup; 
		int posOffset = 0;
		if(!twoPlayer) { //4Player Matches
			switch(reducedType) {
				case ABC_RED_FULL_1V3:{
					int size = firstBots.size()*secondBots.size()*secondBots.size()*secondBots.size() + firstBots.size()*firstBots.size()*firstBots.size()*firstBots.size();
					saveMatchup = new ArrayList<>(size);
					
					System.out.println("matchup number reduction: 1v3(and full),4pl");
					//1 firstbot, 3 secondbots
					for(int f1it = 0; f1it < firstBots.size(); f1it++) { //nx
						for(int s1it = 0; s1it < secondBots.size(); s1it++) { //8
							for(int s2it = s1it+1; s2it < secondBots.size(); s2it++) { //
								for(int s3it = s2it+1; s3it < secondBots.size(); s3it++) { //
									for(int i = 0; i < numRepeats; i++) {
										saveMatchup.add(createMatchWithOffset(f1it, 0, s1it, 1, s2it, 1, s3it, 1, posOffset, false));

										matchCodeLines += createCodeWithOffsetAndNewLine(firstBots.get(f1it), secondBots.get(s1it), secondBots.get(s2it), secondBots.get(s3it), posOffset, false, noReplay, seed);
										posOffset = (posOffset+1)%4;
									}
								}
							}
						}
					}
					//3 firstbots, 1 secondbot
					for(int f1it = 0; f1it < firstBots.size(); f1it++) { //nx
						for(int f2it = f1it+1; f2it < firstBots.size(); f2it++) { //nx
							for(int f3it = f2it+1; f3it < firstBots.size(); f3it++) { //nx
								for(int s1it = 0; s1it < secondBots.size(); s1it++) { //8
									for(int i = 0; i < numRepeats; i++) {
										saveMatchup.add(createMatchWithOffset(f1it, 0, f2it, 0, f3it, 0, s1it, 1, posOffset, false));

										matchCodeLines += createCodeWithOffsetAndNewLine(firstBots.get(f1it), firstBots.get(f2it), firstBots.get(f3it), secondBots.get(s1it), posOffset, false, noReplay, seed);
										posOffset = (posOffset+1)%4;
									}
								}
							}
						}
					}
				}//NOBREAK
				case ABC_RED_FULL:{
					int size = firstBots.size()*firstBots.size()*secondBots.size()*secondBots.size();
					saveMatchup = new ArrayList<>(size);
					
					System.out.println("matchup number reduction: full,4pl");
					for(int f1it = 0; f1it < firstBots.size(); f1it++) { //nx
						for(int f2it = f1it+1; f2it < firstBots.size(); f2it++) { //nx
							for(int s1it = 0; s1it < secondBots.size(); s1it++) { //8
								for(int s2it = s1it+1; s2it < secondBots.size(); s2it++) { //
									for(int i = 0; i < numRepeats; i++) {
										saveMatchup.add(createMatchWithOffset(f1it, 0, f2it, 0, s1it, 1, s2it, 1, posOffset, false));

										matchCodeLines += createCodeWithOffsetAndNewLine(firstBots.get(f1it), firstBots.get(f2it), secondBots.get(s1it), secondBots.get(s2it), posOffset, false, noReplay, seed);
										posOffset = (posOffset+1)%4;
									}
								}
							}
						}
					} 
					break;
				}
				case ABC_RED_SIMPLE_B_1V3:{
					System.out.println("matchup number reduction: 1v3(TODO! skip to simple b),4pl");
				}//NOBREAK
				
				case ABC_RED_SIMPLE_B:{
					int size = firstBots.size()*secondBots.size()*4*numRepeats;
					saveMatchup = new ArrayList<>(size);
					
					for(int f1it = 0; f1it < firstBots.size(); f1it++) { //nx
						for(int s1it = 0; s1it < secondBots.size(); s1it++) { //8
							for(int it2 = 0; it2 < 4; it2++) {
								int f2it = (f1it+it2)%firstBots.size();
								int s2it = (s1it+it2)%secondBots.size();
								for(int i = 0; i < numRepeats; i++) {
									saveMatchup.add(createMatchWithOffset(f1it, 0, f2it, 0, s1it, 1, s2it, 1, posOffset, false));

									matchCodeLines += createCodeWithOffsetAndNewLine(firstBots.get(f1it), firstBots.get(f2it), secondBots.get(s1it), secondBots.get(s2it), posOffset, false, noReplay, seed);
									posOffset = (posOffset+1)%4;
								}
							}
						}
					}
					break;
				}
				case ABC_RED_SIMPLE_A_1V3:{
					//REDUCED
					//1 firstbot, 3 secondbots
					System.out.println("matchup number reduction: 1v3(and simple a),4pl");
					int size = firstBots.size()*secondBots.size()*4*numRepeats*2;
					saveMatchup = new ArrayList<>(size);

					for(int f1it = 0; f1it < firstBots.size(); f1it++) { //nx
						for(int s1it = 0; s1it < secondBots.size(); s1it++) { //8
							for(int it23 = 0; it23 < 4; it23++) { //
								int s2it = (s1it+it23)%secondBots.size();
								int s3it = (s1it+it23+1)%secondBots.size();
								for(int i = 0; i < numRepeats; i++) {
									saveMatchup.add(createMatchWithOffset(f1it, 0, s1it, 1, s2it, 1, s3it, 1, posOffset, false));

									matchCodeLines += createCodeWithOffsetAndNewLine(firstBots.get(f1it), secondBots.get(s1it), secondBots.get(s2it), secondBots.get(s3it), posOffset, false, noReplay, seed);
									posOffset = (posOffset+1)%4;
								}
								
							}
						}
					}
					for(int f1it = 0; f1it < firstBots.size(); f1it++) { //nx
						for(int s1it = 0; s1it < secondBots.size(); s1it++) { //8
							for(int it23 = 0; it23 < 4; it23++) { //
								int s2it = (s1it+it23)%firstBots.size();
								int s3it = (s1it+it23+1)%firstBots.size();
								for(int i = 0; i < numRepeats; i++) {
									saveMatchup.add(createMatchWithOffset(f1it, 0, s1it, 1, s2it, 1, s3it, 1, posOffset, false));

									matchCodeLines += createCodeWithOffsetAndNewLine(firstBots.get(f1it), secondBots.get(s1it), secondBots.get(s2it), secondBots.get(s3it), posOffset, false, noReplay, seed);
									posOffset = (posOffset+1)%4;
								}
								
							}
						}
					}//NOBREAK
				}
				case ABC_RED_SIMPLE_A:
					
				default:{
					int size = numRepeats*firstBots.size()*secondBots.size();
					saveMatchup = new ArrayList<>(size);

					for(int i = 0; i < numRepeats; i++) {
						for(int f1it = 0; f1it < firstBots.size(); f1it+=2) { //nx
							//System.out.println("round:"+i+", fb:" + f1it);
							for(int s1it = 0; s1it < secondBots.size(); s1it+=2) { //8
							//int s2it = (s1it+it2)%secondBots.size();
								int f2it = f1it+1;
								int s1itoff = (s1it+(2*i))%secondBots.size();
								int s2itoff = s1itoff+1;
								if(s2itoff >= secondBots.size()) {
									s2itoff = s1itoff;
								}
								saveMatchup.add(createMatchWithOffset(f1it, 0, f2it, 0, s1itoff, 1, s2itoff, 1, posOffset, false));
								matchCodeLines += createCodeWithOffsetAndNewLine(firstBots.get(f1it), firstBots.get(f2it), secondBots.get(s1itoff), secondBots.get(s2itoff), posOffset, false, noReplay, seed);
								posOffset = (posOffset+1)%4;

							}
						}
					} 
				}
			}			
			
		} else {		//2Player Matches
			System.out.println("2pl");
			int size = numRepeats*firstBots.size()*secondBots.size();
			saveMatchup = new ArrayList<>(size);
			for(int f1it = 0; f1it < firstBots.size(); f1it++) { //nx
				for(int s1it = 0; s1it < secondBots.size(); s1it++) { //8
					for(int i = 0; i < numRepeats; i++) {
						saveMatchup.add(createMatchWithOffset(f1it, 0, s1it, 1, 0, 0, 0, 0, posOffset, true));

						matchCodeLines += createCodeWithOffsetAndNewLine(firstBots.get(f1it), secondBots.get(s1it), null, null, posOffset, true, noReplay, seed);
						posOffset = (posOffset+1)%4;
					}
				}
			}	
		}
		
		//1-3
		
		//3-1
		saveABCMatchupList(saveMatchup, progNum);
		return matchCodeLines;
		
	}
	
	public void saveABCMatchupList(ArrayList<Match> matches, int progNum) {
		
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath;
		String gr0, gr1;
		switch(progNum) {
			case 2:{
				fPath= Paths.get(dir.toString(), CFG_FOLDERNAME, RESULT_CFG_FOLDERNAME, "c"+MATCHUPS_FILENAME);
				gr0 = "V"; //final
				gr1 = "W"; //preset
				break;
			}
			case 1:{
				fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, RESULT_CFG_FOLDERNAME, "b"+MATCHUPS_FILENAME);
				gr0 = "W"; //preset
				gr1 = "U"; //first
				break;
			}
			default:{
				fPath = Paths.get(dir.toString(),CFG_FOLDERNAME, RESULT_CFG_FOLDERNAME, "a"+MATCHUPS_FILENAME);
				gr0 = "U"; //first
				gr1 = "V"; //final
				break;
			}
		}

		File file = new File(fPath.toString());

		LinkedList<String> matchText = new LinkedList<>();

		String pl1t, pl2t, pl3t, pl4t;
		String matchline = "";
		int numberMatches = matches.size();
		int count = 0;
		for(Match m : matches) {
			
			if(m.getType(0)==0) {
				pl1t = gr0;
			} else {
				pl1t = gr1;
			}
			if(m.getType(1)==0) {
				pl2t = gr0;
			} else {
				pl2t = gr1;
			}
			
			
			if(m.isFourPlayer()) { //four-player matchups
				if(m.getType(2)==0) {
					pl3t = gr0;
				} else {
					pl3t = gr1;
				}
				if(m.getType(3)==0) {
					pl4t = gr0;
				} else {
					pl4t = gr1;
				}
				matchline = "M"+count +": "+ pl1t+getNumString(m.getID(0))+" "+pl2t+getNumString(m.getID(1))+" "+pl3t+getNumString(m.getID(2))+" "+pl4t+getNumString(m.getID(3)) + " ";
			} else { //two-player matchups
				matchline = "M"+count +": "+ pl1t+getNumString(m.getID(0))+" "+pl2t+getNumString(m.getID(1))+" ";
			}

			matchText.add(matchline);
			count++;
			if(count%8 == 0) {
				matchText.add("");
			}
		}
		

		//System.out.println("addMatchup: matchuptext = "  +matchline);
	    try {
			Files.write(fPath, matchText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public static void editABCMatchupList(int progNum, int plgroup, int plid, ArrayList<Double> wscores) {
		//System.out.println("editABCMatchupList("+progNum+","+plgroup+","+plid+")");
		//if(plid > 2) {
		//	return;
		//}
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath;
		String replaceString;
		switch(progNum) {
			case 2:{
				fPath= Paths.get(dir.toString(), CFG_FOLDERNAME, RESULT_CFG_FOLDERNAME, "c"+MATCHUPS_FILENAME);
				if(plgroup == 0) {
					replaceString = "V"+getNumString(plid); //final
				} else {
					replaceString = "W"+getNumString(plid); //preset
				}
				break;
			}
			case 1:{
				fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, RESULT_CFG_FOLDERNAME, "b"+MATCHUPS_FILENAME);
				if(plgroup == 0) {
					replaceString = "W"+getNumString(plid); //preset
				} else {
					replaceString = "U"+getNumString(plid); //first
				}
				break;
			}
			default:{
				fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, RESULT_CFG_FOLDERNAME, "a"+MATCHUPS_FILENAME);
				if(plgroup == 0) {
					replaceString = "U"+getNumString(plid); //first
				} else {
					replaceString = "V"+getNumString(plid); //final
				}
				break;
			}
		}
		File file = new File(fPath.toString());
		LinkedList<String> writeText = new LinkedList<>();


	    try{
	        String editText;
	        FileReader fr = new FileReader(file);
	        BufferedReader bfr = new BufferedReader(fr);
	        
	        int scoreIt = 0;
	        while( (editText=bfr.readLine()) != null )
	        { 
	            if(editText != null)
	            {
	            	if(editText.contains(replaceString) && scoreIt < wscores.size()) {
		            	//System.out.println("replace string:["+replaceString+"]");
		            	editText = editText.replaceAll(replaceString, replaceString+":"+wscores.get(scoreIt));
		            	//System.out.println("edited string:["+editText+"]");
		            	editText = checkTextForWinner(editText);
		            	//System.out.println("checked string:["+editText+"]\n");
		                scoreIt++;
	            	}
	    			writeText.add(editText);

	            }
	        }
	        bfr.close();
			Files.write(fPath, writeText, Charset.forName("UTF-8"));

	    }catch(IOException e){
	    	e.printStackTrace();
	    }
	}

	public static String getNumString(int num) {
		if(num<10) {
			return ("0"+num);
		}
		return (""+num);
	}
	
	private static String checkTextForWinner(String editText) {
		String[] elementData = editText.split("\\s+");
		if(elementData.length != 5) {
			try {
				throw new Exception("checkTextForWinner:String split did not return 5 strings");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		ArrayList<Double> resultData = new ArrayList<>(elementData.length-1);
		for(int i = 1; i < elementData.length; i++) {
			String[] resExtract = elementData[i].split(":");
			if(resExtract.length != 2) { //no result for every player yet? -> delay for now
				return editText;
			}
			resultData.add(Double.parseDouble(resExtract[1]));
		}
		Collections.sort(resultData, Collections.reverseOrder());
		String newText = editText;
		//System.out.println("newText before edit:["+newText+"]");

		for(int i = resultData.size()-1; i >= 0; i--) {
			newText = newText.replaceAll(resultData.get(i).toString(), resultData.get(i)+"_["+i+"]"); 
			//System.out.println("newText after edit "+i+":["+newText+"]");
		}


		return newText;
	}


	public Match createMatchWithOffset(int i1, int i1t, int i2, int i2t, int i3, int i3t, int i4, int i4t, int offset, boolean twoPlayer) {
		if(twoPlayer) {
			if(offset%2 == 0) {
				return new Match(i2, i2t, i1, i1t);
			} else {
				return new Match(i1, i1t, i2, i2t);
			}

		} else {

			switch(offset) {
			case 3:{
				return new Match(i2, i2t, i3, i3t, i4, i4t, i1, i1t);
			}
			case 2:{
				return new Match(i3, i3t, i4, i4t, i1, i1t, i2, i2t);
			}
			case 1:{
				return new Match(i4, i4t, i1, i1t, i2, i2t, i3, i3t);

			}
			default:{
				return new Match(i1, i1t, i2, i2t, i3, i3t, i4, i4t);
			}
			}

		}
	}
	
	public String createCodeWithOffsetAndNewLine(String b1, String b2, String b3, String b4, int offset, boolean twoPlayer, boolean noReplay, long seed) {
		String ret ="";
		
		if(twoPlayer) {
			if(offset%2 == 0) {
				ret += getSimpleMatchShCode(b2, b1, null, null, true, noReplay, seed);
				ret += "\n";
				return ret;
			} else {
				ret += getSimpleMatchShCode(b1, b2, null, null, true, noReplay, seed);
				ret += "\n";
				return ret;
			}
		}
		
		
		switch(offset) {
		case 3:{
			ret += getSimpleMatchShCode(b2, b3, b4, b1, false, noReplay, seed);
			ret += "\n";
			return ret;
		}
		case 2:{
			ret += getSimpleMatchShCode(b3, b4, b1, b2, false, noReplay, seed);
			ret += "\n";
			return ret;
		}
		case 1:{
			ret += getSimpleMatchShCode(b4, b1, b2, b3, false, noReplay, seed);
			ret += "\n";
			return ret;
		}
		default:{
			ret += getSimpleMatchShCode(b1, b2, b3, b4, false, noReplay, seed);
			ret += "\n";
			return ret;
		}
		}
	}
	
	//create Matches between preset & final bots // C-MATCHES

	
	// ################ BOT NAMES ################# 

	//gets the bot names for this batch
	public static ArrayList<String> getBotNames(int popSize, int it, int ba) {
		ArrayList<String> botNames = new ArrayList<>();
		for(int i = 0; i < popSize; i++) {
			String botName = getBotName(it, ba, i);
			botNames.add(botName);
		}
		return botNames;
	}
	
	public static String getBotName(int it, int ba, int id) {
		return BOT_PREFIX + "_" + getSuffixByBa(ba) + it + "_" + id;
	}
	
    public static String getSuffixByBa(int ba) {
    	String ret = "";
    	if(ba > 25) {
    		ret += getSuffixByBa((ba/26)-1) + getSuffixByBa(ba%26);
    	} else {
    		char[] et = Character.toChars(ba+65);
    		ret = String.copyValueOf(et);
    	}
    	
    	return ret;
    }


	public static ArrayList<String> readPresets() {

		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, PRESET_CFG_FOLDERNAME, PRESET_FILENAME);
		File file = new File(fPath.toString());

	    try {
			scanner = new Scanner(new FileInputStream(file), "UTF-8");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    
	    ArrayList<String> presetBotNames = new ArrayList<>();			
	    //if(scanner.hasNext()) {
		//int numEntries = scanner.nextInt();
		    //}
		while(scanner.hasNext()) {
			presetBotNames.add(scanner.next());
		}		
	    scanner.close();
	    return presetBotNames;
	}

	
	public static double summarizeBotScores(LinkedList<Double> botScores) {
		BigDecimal botScoreSum = BigDecimal.ZERO;
		for(Double score : botScores) { //for each indv
			botScoreSum = botScoreSum.add(BigDecimal.valueOf(score));
    	}

    	return botScoreSum.doubleValue();	
    }

	
	public static void saveResultDataToFile(String filename, LinkedList<String> data) {
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, RESULT_CFG_FOLDERNAME, filename);

		
	    try {
			Files.write(fPath, data, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
	}

}

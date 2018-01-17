package genAlgo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class GAFileHandler {
	public static final boolean OVERWRITE_GAIT = false;
	public static final String CFG_FOLDERNAME = "configs";
	
	public static final String BOT_SCR_FOLDERNAME = "scores";
	public static final String SAFE_CFG_FOLDERNAME = "static_configs";
	public static final String GA_CFG_FOLDERNAME = "ga_configs";
	
	public static final String RANKINGS = "rankings";
	public static final String RANKINGS_FILENAME = "rankings.txt";
	public static final String GAIT_FILENAME = "gaitinfo.txt";
	public static final String SAFE_BOTS_FILENAME = "safebots.txt";
	public static final String EXT_BOTS_FILENAME = "extbots.txt";
	
	public static final String MATCHUP_FILENAME = "matchup.txt";
	public static final int MATCHUP_LAST = -1;
	
	public static final String MATCHES_SH_FILENAME = "matches.sh";
	public static final String MATCHES_SH_INIT = "#!/bin/sh";
	public static final String MATCHES_SH_CALL_NOREPL = "./halite -q -r ";
	public static final String MATCHES_SH_CALL = "./halite -q ";
	public static final String MATCHES_SH_RECALL1 = "java genAlgo/TournamentSelector";
	public static final String MATCHES_SH_RECALL2 = "source configs/matches.sh";


	public static final String GA_BOT_JAVANAME = "ModifiedBot.java";
	public static final String GA_BOT_CLASSNAME = "ModifiedBot";
	
	
	public static final String BOT_PREFIX = "GABot";
	public static final String SAFE_BOT_PREFIX = "SGABot";

	
	public static final int GAIT_LINES = 3;
	public static final int GAIT_INIT_ARGS = 6;
	public static final int GAIT_BOTNAME_LINES = 2;
	
	public static final int GAIT_I_IT = 0; //iteration
	public static final int GAIT_I_N_IT = 1; //num iterations
	public static final int GAIT_I_BA = 2; //batch
	public static final int GAIT_I_N_BA = 3; //num batches
	public static final int GAIT_I_FIN = 4; //finished? (int, 1 -> true)


	boolean gaitLoaded;
	private String gaitID;
	private ArrayList<String> safeBots;
	private ArrayList<String> externBots;
	private ArrayList<Integer> gaitInitData;
	private int matchupLine;
	

	public GAFileHandler() {
		matchupLine = 0;
		gaitID = "_no_id_";
		gaitLoaded = false;
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
	public ArrayList<String> getSafeBots(){
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
		System.out.println("saving bot " +  getBotName(it, ba, id)  + "  data: " + line );
		newText.add(line);
	    try {
			Files.write(fPath, newText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static double[] readBotAtts(int id, int it, int ba){
		String name = getBotNames(it, ba).get(id);
		return readBotAttsByName(name);
	}

	//used by bot, no couts here!
	public static double[] readBotAttsByName(String name){
		double[] res = new double[HaliteGenAlgo.NUM_ATTS]; //5
		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();

		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, GA_CFG_FOLDERNAME, name + ".txt");
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
		scanner.close();
		return externBots;
	}
	
	
	// ######### BOT SCORES ##########
	

	/**
	 * returns a sorted list of average Scores
	 */
	public static LinkedList<Double> readBotScores(int id, int iteration, int batch) {
		
		ArrayList<String> botNames = getBotNames(iteration, batch);
		if(id >= botNames.size()) {
			System.out.println("ERROR! id does not exist in names");
			return null;
		}
		LinkedList<Double> botScores = new LinkedList<>();
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, BOT_SCR_FOLDERNAME, botNames.get(id));
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
			
			System.out.println("all scores of this bot ("+botNames.get(id)+") sorted:");
			for(int i = 0; i < fileList.length; i++) {
				System.out.println(fileList[i].toString());
			}
			
			if(fileList.length != 0) {
				for (File scoreFile : fileList) {
					if (scoreFile.isFile()) {
						botScores.add(readWeightedBotScore(scoreFile));
					}
				}
			} else {
				System.out.println("specified folder:" + fPath.toString() + " does not contain files");
				botScores.add((double) 0);
			}
		} else {
			System.out.println("specified folder:" + fPath.toString() + " DOES NOT EXIST");
			botScores.add((double) 0);
		}
		
		System.out.println("all scores of this bot ("+botNames.get(id)+") sorted:");
		for(int i = 0; i < botScores.size(); i++) {
			System.out.println(botScores.get(i));
		}
		

		return botScores;

	}
	

	public static double readWeightedBotScore(File file) {
		Scanner scanner = null;

	    try {
			scanner = new Scanner(new FileInputStream(file), "UTF-8");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    
	    scanner.useLocale(Locale.US);
	    LinkedList<Double> scores = new LinkedList<>();
	    double weightedScore = 0;
		while(scanner.hasNext()) {
			scores.add(scanner.nextDouble());
		}
		
		int count = scores.size();
		int halfCount = (int) (0.5*count);
		for(int i = 0; i < count; i++) {
			double factor = (((double)i-halfCount) / (double)count) * 1.5 + 1; //weights: ~0-25 - 1.75
			weightedScore += scores.get(i) * factor;
		}
		
		weightedScore = weightedScore / (double)count;
		
		scanner.close();
		return weightedScore;
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
    public boolean hasBadStarterRating(int id) {

    	int sbotLine = 1;
    	if(!externBots.isEmpty()) {
    		if(getRankOf(id, 1) > HaliteGenAlgo.NUM_INDV) {
    			return true;
    		}
    		sbotLine++;
    	}
    	
    	if(!safeBots.isEmpty()) {
    		if(getRankOf(id, sbotLine) > HaliteGenAlgo.NUM_INDV) {
    			return true;
    		}
    	}
    	return false;

    	
    }
	
	public static int getRankOf(int id, int currentLine) {
		if(currentLine <= 0) {
			return HaliteGenAlgo.NUM_INDV -1;
		}
		ArrayList<Integer> currentRatings = GAFileHandler.readRankings(currentLine);
		if(!currentRatings.contains(id)) {
			return getRankOf(id, currentLine-1);
		} else {
			for(int i = 0; i < currentRatings.size(); i++) {
				System.out.println("getRankOf(" +id+","+currentLine+") : checking i=" +i+" : cr(i)=" + currentRatings.get(i));
				if(currentRatings.get(i) == id) {
					return i;
				}
			}
		}
		return currentRatings.size()-1;
	}
	
	
	public static ArrayList<Integer> readRankings(int number){
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
			return readRankings(number-1);
		}
	    //skip lines until at the right pos
	    for(int i = 0; i < number; i++) {
	    	scanner.nextLine();
	    }
		
		for(int e = 0; e < HaliteGenAlgo.NUM_INDV; e++) {
			rankings.add(scanner.nextInt());
		}
	    scanner.close();

		return rankings;
	}
	
	
	public static ArrayList<Integer> readLastRankings(){
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
		
		for(int e = 0; e < HaliteGenAlgo.NUM_INDV; e++) {
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
		System.out.println("adding "+rankings.size() + "rankings..");

		String ranks = "";
		for(Map.Entry<Integer, Integer> me : rankings.entrySet()) {
			ranks += me.getValue() + " ";
		}
		editText.add(ranks);
		System.out.println("edittext: " + editText);

		
	    try {
			Files.write(fPath, editText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	
	
	public void createMatchupFile(ArrayList<String> cbots, ArrayList<String> sbots, ArrayList<String> extbots) {
		System.out.println("creating matchup file");

		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, MATCHUP_FILENAME);
		LinkedList<String> muText = new LinkedList<>();
		
	 	String l_init = "0"; //current Round, initialized with 0 -> indicates which line the tournament selector should read
		muText.add(l_init);
		
		
		
		//NUM_ROUNDS + MATCH_i .. MATCH_n
		//line 1 extern matches: GA1 vs EXTi vs EXTi vs EXTi for each (only need 2 arguments per match)

		int numberMatches = extbots.size() * HaliteGenAlgo.NUM_INDV;
		String l_ext = numberMatches + " ";
		for(int i = 0; i < HaliteGenAlgo.NUM_INDV; i++) {
			for(int e = 0; e < extbots.size(); e++) {
				l_ext += i + " ";
				l_ext += e + " ";
			}
		}
		muText.add(l_ext);
		
		//line 2 safeBot matches: GA1 vs GA1 vs SB1 vs SB1 (only need 2 arguments per match)
		numberMatches = sbots.size() * HaliteGenAlgo.NUM_INDV;
		String l_sb = numberMatches + " ";
		for(int i = 0; i < HaliteGenAlgo.NUM_INDV; i++) {
			for(int e = 0; e < sbots.size(); e++) {
				l_sb += i + " ";
				l_sb += e + " ";
			}
		}
		muText.add(l_sb);

		
		//line 3 round1 matches: GA1 vs GA2 vs GA3 vs GA4 (need 4 arguments per match)
		numberMatches = HaliteGenAlgo.NUM_INDV / TournamentSelector.PLAYERS_PER_GAME;
		String l_t = numberMatches + " ";
		for(int g = 0; g < numberMatches; g++) {
			for(int i = 0; i < TournamentSelector.PLAYERS_PER_GAME; i++) {
				l_t += (g*TournamentSelector.PLAYERS_PER_GAME + i) + " ";
			}
		}
		muText.add(l_t);
		
	    try {
			Files.write(fPath, muText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			System.out.println("createMatchups: saving error");

			e.printStackTrace();
		}
	}
	


	public static void updateMatchupLine() {
		System.out.println("updating matchup line");
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
		System.out.println("adding to matchup file");

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
				matchline += m.getID(0)+" "+m.getID(1)+" "+m.getID(2)+" "+m.getID(3) + " ";
			}
			
			editText.add(matchline);
			System.out.println("addMatchup: matchuptext = "  +matchline);

		    try {
				Files.write(fPath, editText, Charset.forName("UTF-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	
    
	public ArrayList<Match> readMatchup(int line) {
    	System.out.println("readMatchup: line "+ line);


		Scanner scanner = null;
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, MATCHUP_FILENAME);
	    try {
			scanner = new Scanner(new File(fPath.toString()));
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();  
	    }
	    
		//read current matchup line (first line (init))
	    if(line == -1) {
	    	matchupLine = scanner.nextInt();
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
	    int numMatchesThisLine = scanner.nextInt();
    	int numEntriesPerMatch = 4;
    	int[] playerIDs;
    	int matchupType = 0;
	    ArrayList<Match> matches = new ArrayList<>(numMatchesThisLine);

	    if(matchupLine == 0) {
	    	numEntriesPerMatch = 2;
	    	playerIDs = new int[numEntriesPerMatch];
	    	matchupType = Match.TYPE_EXT;
	    } else if(matchupLine == 1) {
	    	numEntriesPerMatch = 2;
	    	playerIDs = new int[numEntriesPerMatch];
	    	matchupType = Match.TYPE_SAFE;
	    } else {
	    	playerIDs = new int[numEntriesPerMatch];
	    	matchupType = Match.TYPE_GA;
	    }
	    
	    //first two lines: 2 Entries per Match
	    for(int i = 0; i < numMatchesThisLine;i++) {
	    	for(int e = 0; e < numEntriesPerMatch; e++) {
	    		playerIDs[e] = scanner.nextInt();
	    		System.out.println("scanned : " + playerIDs[e]);
	    	}
	    	if(numEntriesPerMatch == 4) {
		    	matches.add(new Match(playerIDs[0], playerIDs[1], matchupType, playerIDs[2], matchupType, playerIDs[3], matchupType));
	    	} else { // 1x GA-Bot + 3x EXT/SAFE-Bot
	    		matches.add(new Match(playerIDs[0], playerIDs[1], matchupType, playerIDs[1], matchupType, playerIDs[1], matchupType));
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

	
	public String createCompileScript() {
		String ccode = "";
		ccode += getCompCode(0,0) + "\n";
		for(int i = 0; i< externBots.size(); i++) {
			ccode += getCompCode(2,i) + "\n";
		}
		return ccode;
	}	
	
	
	//init call! creates first matches sh file
	public void createMatchesSh(LinkedList<Match> matches, ArrayList<String> sBots) {

		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, MATCHES_SH_FILENAME);
		LinkedList<String> shText = new LinkedList<>();

	 	String l1 = MATCHES_SH_INIT;
		shText.add(l1);
		
		shText.add(createCompileScript());

	 	String currentMatchLine = "";
	 
		for(int i= 0; i < matches.size(); i++) {
			currentMatchLine = getMatchShCode(matches.get(i), getBotNames(0,0), sBots, false);
			shText.add(currentMatchLine);
		}
		
	    try {
			Files.write(fPath, shText, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
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
		public void createNextMatchesSh(ArrayList<Match> matches, int numRepeats, boolean noReplay, ArrayList<String> sBots) {
			
		    
			Path dir = Paths.get(".").toAbsolutePath().normalize();
			Path fPath = Paths.get(dir.toString(), CFG_FOLDERNAME, MATCHES_SH_FILENAME);
			LinkedList<String> shText = new LinkedList<>();
			
		 	String l1 = MATCHES_SH_INIT;
			shText.add(l1);

		 	String currentMatchLine = "";
		 
		 	Match.printMatches(matches);
		 	
		 	for(int j = 0; j< numRepeats; j++) {

				for(int i= 0; i < matches.size(); i++) {
					currentMatchLine = getMatchShCode(matches.get(i), getBotNames(gaitInitData.get(GAIT_I_IT), gaitInitData.get(GAIT_I_BA)),sBots, noReplay);
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

	public String getMatchShCode(Match match, ArrayList<String> targetBots, ArrayList<String> sBots , boolean noReplay) {
		String shcode = MATCHES_SH_CALL;	
		if(noReplay) {
			shcode = MATCHES_SH_CALL_NOREPL;
		}
		shcode += getMatchTargetHalArg(match, 0, targetBots, sBots) + " " + getMatchTargetHalArg(match, 1, targetBots, sBots);
		if(match.isFourPlayer()) {
			shcode +=  " " + getMatchTargetHalArg(match, 2, targetBots, sBots) + " " + getMatchTargetHalArg(match, 3, targetBots, sBots);
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
	
	
	// ################ BOT NAMES ################# 

	//gets the bot names for this batch
	public static ArrayList<String> getBotNames(int it, int ba) {
		ArrayList<String> botNames = new ArrayList<>();
		for(int i = 0; i < HaliteGenAlgo.NUM_INDV; i++) {
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


	

}

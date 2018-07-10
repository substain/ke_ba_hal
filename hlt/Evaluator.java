package hlt;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import genAlgo.GAFileHandler;

public class Evaluator{

    public static final double SHIP_BONUS_THRESH = 0.20;
    public static final double PLANET_BONUS_THRESH = 0.10;

	public static final double TIME_BONUS_FACTOR = 0.25;
	public static final boolean WRITE_TO_FILE = true;

	
	boolean won;
	int myPlayerId;
	int numActivePlayers;
	int numPlayers;
	int numPlanets;
	int rounds;
	int finalScore;
	String botName;
	Path filePath;
	int iteration;
	
	LinkedList <Integer> roundScore;
	
	public Evaluator(GameMap initialMap, String botID) {
		rounds = 0;
		myPlayerId = initialMap.getMyPlayerId();
		roundScore = new LinkedList<>();
		finalScore = 0;
		won = false;
		numPlayers = initialMap.getAllPlayers().size();
		numPlanets = initialMap.getAllPlanets().size();
		botName = botID;
		initConfigFile();
		iteration = -1;
	}
	
	public void setIteration(int currentIteration) {
		iteration = currentIteration;
	}
	
	/**
	 * evaluates a score for the current Round 
	 * @return true if the game is almost over
	 */
	public void evaluateRound(GameMap currentRoundMap) {
		rounds++;
		numActivePlayers = 0;
		int numShips = 0;
		double score = 0;

		int[] playerShips = new int[numPlayers];
        Arrays.fill(playerShips, 0);
        
		for(Ship s : currentRoundMap.getAllShips()) {
			numShips++;
			playerShips[s.getOwner()]++;
		}
		
		
		//boolean playerAlmostDead = false;
		for(int i = 0; i < numPlayers; i++) {
			if(playerShips[i] > 0) {
				numActivePlayers++;
			}
			if(playerShips[i] <= 2) {
				//playerAlmostDead = true;
			}
		}
		
		int[] ownedPlanets = new int[numPlayers];
        Arrays.fill(ownedPlanets, 0);
		int numPlanets = 0;
		//int numUnownedPlanets = 0;
		for(Map.Entry<Integer, Planet> plEntry : currentRoundMap.getAllPlanets().entrySet()) {
			numPlanets++;
			Planet pl = plEntry.getValue();
			if(pl.isOwned()) {
				ownedPlanets[pl.getOwner()]++;
			} else {
				//numUnownedPlanets++;
			}
		}
		
		double myShipPercentage = ((double)playerShips[myPlayerId])/(double)numShips;
		double myPlanetPercentage = ((double)ownedPlanets[myPlayerId])/(double)numPlanets;

		double playerFraction = 1/(double)numActivePlayers;
		
		//Log.log("myShipPercentage:" + myShipPercentage + ", myPlanetPercentage=" + myPlanetPercentage + ", playerFraction = " + playerFraction);

		/*
		if(myShipPercentage > playerFraction) {
			if(myShipPercentage > playerFraction + (playerFraction*SHIP_BONUS_THRESH)) {
				score += 1;
			} else {
				score += 0.5;			
			}
		}
		
		if(myPlanetPercentage > playerFraction) {
			if(myPlanetPercentage > playerFraction + (playerFraction*PLANET_BONUS_THRESH)) {
					score += 1;			
				} else {
					score += 0.5;			
				}
		}
*/
		if(WRITE_TO_FILE) {
			addToFile(myShipPercentage+myPlanetPercentage);
		}
		
		/*
		if(numUnownedPlanets < numPlanets/2) {
			if(numActivePlayers<=2 && playerAlmostDead) {
				if(playerShips[myPlayerId] > 3) {
					won = true;
				}
				return true;
			}
		}
		if(rounds == Constants.MAX_NUM_ROUNDS-1) {
			if(myShipPercentage > playerFraction + (playerFraction*SHIP_BONUS_THRESH)) {
				finalScore+=30;
			}
			return true;
		}
		*/
		//return false; 
	}
	
	public void evaluateRoundDom(double domFactor) {
		if(WRITE_TO_FILE) {
			addToFile(domFactor);
		}

	}
	
	private void accumulateScore() {
		int timeBonus = 0;
		
		for(Integer i : roundScore) {
			finalScore += i;
		}
		
		if(won) {
			timeBonus += Constants.MAX_NUM_ROUNDS - rounds * TIME_BONUS_FACTOR;
			finalScore += 75;
		} else {
			timeBonus += TIME_BONUS_FACTOR * rounds;
		}
		finalScore += timeBonus;
		
	}
	
	/*
	public void setWon() {
		won = true;
	} */
	
	public int getFinalScore() {
		return finalScore;
	}
	
	public String scoresToString() {
		String ret = "Evaluator scores per round: \n";
		int roundNum = 0;
		for(Integer i : roundScore) {
			ret += "R" + roundNum + ": " + i.toString() + "\n";
			roundNum++;
		}
		if(finalScore > 0) {
			ret += "FS:" + finalScore;
		}
		
		return ret;
	}

	public void initConfigFile() {

		if(!botName.startsWith("GABot")) {
			Log.log("Evaluator:init: non-GABot");
		}
		Path dir = Paths.get(".").toAbsolutePath().normalize();
		Path botFolder = Paths.get(dir.toString(), GAFileHandler.CFG_FOLDERNAME, GAFileHandler.BOT_SCR_FOLDERNAME, botName);
		//Path iterationFolder = Paths.get(botFolder.toString(), String.valueOf(iteration));
		LinkedList<String> noText = new LinkedList<>();
		//Log.log("initconfig file : folderPath = " + iterationFolder.toString());
		int scoreFileNumber = 0;

		File bfolder = new File(botFolder.toString());
		if(!bfolder.exists()) {
			try{
				bfolder.mkdir();
	    	} 
	    	catch(SecurityException se){
	        //TODO				

	    	}   
	    }
		/*File ifolder = new File(iterationFolder.toString());
		if(!ifolder.exists()) {
			try{
				ifolder.mkdir();
	    	} 
	    	catch(SecurityException se){
	        //TODO				

	    	}   
	    }*/
		//Log.log("folder written.");

		File[] fileList = bfolder.listFiles();

		for (File scoreFile : fileList) {
			if (scoreFile.isFile()) {
				scoreFileNumber++;
			}
		}
		filePath = Paths.get(botFolder.toString(), scoreFileNumber + ".txt");

		try {
		    Files.write(filePath, noText, Charset.forName("UTF-8"));
		}catch (IOException e) {
		    //TODO
		}
	
	}
	
	public void addToFile(Double addedNum){
		String addedText = " " + addedNum.toString();
		try {
		    Files.write(filePath, addedText.getBytes(), StandardOpenOption.APPEND);
		}catch (IOException e) {
		    //TODO
		}
	}
	

	

}

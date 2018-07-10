package genAlgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class TournamentSelector {
	
	//public static final int NUM_NGA_ROUNDS = 2; //initAtts.get(GAFileHandler.IPAM_MATCHES_EXT)
	//public static final int NUM_GA_ROUNDS = 3; //initAtts.get(GAFileHandler.IPAM_MATCHES_TOURN)
	public static final int NUM_INIT_ARGS = 1;
	public static final int NUM_TEST_ARGS = 3;
	public static final int NUM_CONT_ARGS = 0;
	public static final int EXT_PLAYER_ARGS = 2;
	
	int populationSize;
	int playersPerGame;

	int currentRound;
	ArrayList<String> currentBots;
	ArrayList<String> batchBots;

	HashMap<Integer, Integer> rankings;
	LinkedList<Integer> winners;
	ArrayList<Match> nextMatches;

	int iteration;
	int batch;
	boolean initRun;
	//ArrayList<ArrayList<Double>> scores;
	
	ArrayList<Integer> initAtts;
	long staticSeed;

	/*
	 * 
	 */
	public TournamentSelector(boolean initRun) { //is initrun if its the first round of an iteration - matchup file also may be empty

		//read init data from file, initialize current round, population size, players per game, ...
		GAFileHandler ioHandler = new GAFileHandler();
		ioHandler.readInit();
		initAtts = ioHandler.getInitData();
		staticSeed = ioHandler.getStaticSeed();
		populationSize = initAtts.get(GAFileHandler.IPAM_POPSIZE)*8;
		playersPerGame = 2;
		if(initAtts.get(GAFileHandler.IPAM_PLAYERS) != GAFileHandler.IPAM_PL_2) {
			playersPerGame = 4;
		}

		ioHandler.readGAITinfo();	
		ArrayList<Integer> gaitinitinfo = ioHandler.getGaitInit();
		iteration = gaitinitinfo.get(GAFileHandler.GAIT_I_IT);
		batch = gaitinitinfo.get(GAFileHandler.GAIT_I_BA);
		boolean noReplay = true;
		int numIterations = gaitinitinfo.get(GAFileHandler.GAIT_I_N_IT);
		int numBatches = gaitinitinfo.get(GAFileHandler.GAIT_I_N_BA);
		
		if(initRun) {
			if(iteration == -1 || batch == -1) {
				//System.out.println("TS:initrun: IT BA finished");
				return;
			}
			System.out.println("TS:<initRun>");
			
			if(iteration == 0 && batch == 0) {
				GAFileHandler.resetRankingsFile(-1,-1);
			} else {
				int lastit = iteration;
				int lastba = batch;
				if(iteration == 0) {
					lastba--;
					lastit = numIterations-1;
				} else {
					lastit--;
				}
				GAFileHandler.resetRankingsFile(lastit, lastba);
			}
		}
		
		batchBots = ioHandler.getBatchBots();
		int outputMode = initAtts.get(GAFileHandler.IPAM_OUTPUT_MODE);
		int[] next = HaliteGenAlgo.getNextItBa(iteration, batch, numIterations, numBatches);
		if(next[0] == -1 || next[1] == -1) { //last run
			if(outputMode == GAFileHandler.IPAM_OM_ALL || outputMode == GAFileHandler.IPAM_OM_FINAL || outputMode == GAFileHandler.IPAM_OM_FIRST_FINAL) {
				
			}
		}
		if(next[0] == -1 || next[1] == -1 || (iteration == 0 && batch == 0)) {
			noReplay = false;
		}
		
		
		currentBots = GAFileHandler.getBotNames(populationSize, iteration, batch);
		
		boolean twoPlayer = false;
		if(playersPerGame == 2) {
			twoPlayer = true;
		}

		ArrayList<Match> currentMatches = ioHandler.readMatchup(GAFileHandler.CURRENT_LINE, twoPlayer);
		currentRound = ioHandler.getMatchupLine();
		//System.out.println("TS:after round = " + currentRound);
		
		

		if(initRun) {
			nextMatches = currentMatches;
			int numRepeats = initAtts.get(GAFileHandler.IPAM_MATCHES_TOURN);
			if(!nextMatches.isEmpty() && nextMatches.get(0).getType(1) != Match.TYPE_GA) {
				initAtts.get(GAFileHandler.IPAM_MATCHES_EXT);
			}
			Match.printMatches(nextMatches, numRepeats);
			ioHandler.createNextMatchesSh(populationSize, nextMatches, initAtts.get(GAFileHandler.IPAM_MATCHES_EXT), noReplay, batchBots, staticSeed); //uses matchup file
			//System.out.println("TS: finished this it (initRound)");

			return;
		}
		
		
		setResults(currentMatches); //creates rankings and writes to the rankings file
		if(currentRound == (initAtts.get(GAFileHandler.IPAM_MATCHES_TOURN) + initAtts.get(GAFileHandler.IPAM_MATCHES_EXT))-1) {
			GAFileHandler.clearMatchesSh();
			//System.out.println("TS: finished this it (final round: " + currentRound + ")");
			return;
		}
		if(currentRound >= initAtts.get(GAFileHandler.IPAM_MATCHES_EXT)) {
			//System.out.println("TS: currentRound("+currentRound+") >= num ext matches("+initAtts.get(GAFileHandler.IPAM_MATCHES_EXT)+") -> updateMatchup, createNextMatchesSH");

			//GAFileHandler.updateMatchupLine();

			updateMatchup(); // sets nextMatches for next round (depends on winners)
			ioHandler.createNextMatchesSh(populationSize, nextMatches, initAtts.get(GAFileHandler.IPAM_MATCHES_TOURN), noReplay, batchBots, staticSeed);
		} else { //first rounds: no need to update the whole matchup
			GAFileHandler.updateMatchupLine(); //+1 for matchup line in the matchup file
			nextMatches = ioHandler.readMatchup(currentRound+1, twoPlayer); // get the corresponding 
			if(currentRound +1 == initAtts.get(GAFileHandler.IPAM_MATCHES_EXT)) { //this is the last round for ext matches
				ioHandler.createNextMatchesSh(populationSize, nextMatches, initAtts.get(GAFileHandler.IPAM_MATCHES_TOURN), noReplay, batchBots, staticSeed);
			} else {
				ioHandler.createNextMatchesSh(populationSize, nextMatches, initAtts.get(GAFileHandler.IPAM_MATCHES_EXT), noReplay, batchBots, staticSeed);
			}

		}

		//System.out.println("TS: finished this it (round: " + currentRound + ")");

	}
	

	
	
	private void setResults(ArrayList<Match> matches) { //creates rankings and writes to the rankings file

		
		if(matches.isEmpty()) {
			//no matches last round 
			winners = new LinkedList<>();
			for(int i = 0; i < populationSize; i++) {
				winners.add(i);
			}
			
			rankings = new HashMap<>();
			for(int i = 0; i < populationSize; i++) {
				rankings.put(i, i);
			}

			System.out.println("TS:no matches(keep winners/rankings)");
			return;
		}
		winners = new LinkedList<>();
		//System.out.println("setResults: matches.size" + matches.size());
		
		
		
		ArrayList<Integer> botIds = getBotIdsByMatches(populationSize,matches,playersPerGame);
		int numPlayers = botIds.size();
		int numWinners = numPlayers/2;
		int numGroups = numPlayers/playersPerGame;
		
		int numMatchRepeats; 
		//check if matchtype is ga or extern to determine numMatchRepeats
		int matchtype = Match.TYPE_GA;
		Match firstmatch = matches.get(0);
		if(firstmatch.isFourPlayer()) { //4 PL match?
			matchtype = firstmatch.getType(3);
		} else {
			matchtype = firstmatch.getType(1);
		}
		if(matchtype == Match.TYPE_GA) {
			numMatchRepeats = initAtts.get(GAFileHandler.IPAM_MATCHES_TOURN);
		} else {
			numMatchRepeats = initAtts.get(GAFileHandler.IPAM_MATCHES_EXT);
		}
		
		System.out.println("setResults: nPlayers = "+numPlayers+", nWinners="+numWinners+", numGroups"+numGroups+", numMatchesPerBot"+numMatchRepeats);
		
		if(currentRound < initAtts.get(GAFileHandler.IPAM_MATCHES_EXT)) {	 //TODO
			numPlayers = populationSize;
			numWinners = populationSize;
		}
		
		ArrayList<Double> scores = new ArrayList<>(numPlayers);

		//System.out.println("numMatchesPerBot = " + numMatchesPerBot);
		int ic = 0;
		for(Integer i : botIds) {
			LinkedList<Double> botScores = GAFileHandler.readBotScores(populationSize, i, iteration, batch);
			int botScoresSize = botScores.size();

			LinkedList<Double> relevantScores = new LinkedList<Double>();
			for(int j = 1; j <= numMatchRepeats; j++) { //only count relevant = lastScores
				//System.out.println("sc"+j+" of "+ic+ " = " + botScores.get((botScoresSize)-j));
				relevantScores.add(botScores.get((botScoresSize)-j));
			}
			//System.out.println("avgsc of "+ic+ " = " + HaliteGenAlgo.getAvgScore(relevantScores));

			scores.add(HaliteGenAlgo.getAvgScore(relevantScores));
			ic++;
		}
		ArrayList<ArrayList<Integer>> groups = new ArrayList<>(numGroups);
		int numGroupsAdded = 0;
		for(Match m:matches) {
			if(numGroupsAdded == numGroups) {
				break;
			}
			ArrayList<Integer> gr = new ArrayList<>();
			for(int i = 0; i < playersPerGame; i++) {
				gr.add(m.getID(i));
			}
			groups.add(gr);
			
			numGroupsAdded++;
			
		}
		rankings = GAFileHandler.createGroupOrderedRankings(populationSize, botIds, scores, GAFileHandler.CURRENT_LINE, populationSize, groups);
		GAFileHandler.addRankings(rankings);


		//int rcount = 0;
		for(Map.Entry<Integer, Integer> me : rankings.entrySet()) {
			System.out.println("ranking at "+ me.getKey() + " is " + me.getValue());
		}

		if(currentRound >= initAtts.get(GAFileHandler.IPAM_MATCHES_EXT) && currentRound < (initAtts.get(GAFileHandler.IPAM_MATCHES_EXT)+initAtts.get(GAFileHandler.IPAM_MATCHES_TOURN)-1)) { //winners needed
			for(int i = 0; i < rankings.size(); i++) {
				if(i < numWinners) { //get the ranking of index i, if its 
					winners.add(rankings.get(i));
				} else {
					break;
				}
			}
		}
		for(int i = 0; i < winners.size(); i++) {
			//System.out.println("Winner IDS: " + winners.get(i));
		}
		
	}
	
	public static int recDivBy2(int number, int times) {
		if(times <= 0) {
			return number;
		}
		return (recDivBy2(number, times - 1)/2);
	}
	


	private void updateMatchup() {//depends on winners
		ArrayList<Match> matches = new ArrayList<>();
		
		//System.out.println("TS: update Matchup: creating winners-set, calculating next matches, GAFH:addMatchup");

		int num_next_matches = winners.size()/playersPerGame;
		// PLAYERS_PER_GAME
		//int num_winners = 2; // evtl nicht gebraucht
		
		// %winners.size();
		
		for(int i = 0; i < num_next_matches; i++) {
			int[] players = new int[playersPerGame];
			for(int j = 0; j < playersPerGame; j++) {
				players[j] = winners.get(j+i*playersPerGame);
			}
			if(playersPerGame==2) {
				matches.add(new Match(players[0], players[1], Match.TYPE_GA));			
			} else {
				matches.add(new Match(players[0], players[1], Match.TYPE_GA, players[2], Match.TYPE_GA, players[3], Match.TYPE_GA));			
			}
		}
		nextMatches = matches;
		GAFileHandler.addMatchup(matches);

	}

	
	private static ArrayList<Integer> getBotIdsByMatches(int popSize, ArrayList<Match> matches, int playersPerGame){
		ArrayList<Integer> bots = new ArrayList<>(popSize);
		for(Match m : matches) {
			for(int i = 0; i < playersPerGame; i++) {
				if(!bots.contains(m.getID(i)) && m.getType(i) == Match.TYPE_GA) {
					bots.add(m.getID(i));
				}
			}
		}
		return bots;
	}


	public static void main(final String[] args) {
//		GAFileHandler.readBotAttsByName("GABot_A0_0");
		if(args.length == NUM_INIT_ARGS) {
			GAFileHandler.clearAllScoresFolder();
			new TournamentSelector(true);
		} else if(args.length == NUM_CONT_ARGS) {
			new TournamentSelector(false);
		}	else if(args.length == NUM_TEST_ARGS) {
			System.out.println("test_TS");
			GAFileHandler ioHandler = new GAFileHandler();
			ioHandler.readGAITinfo();
			
			ArrayList<Integer> botIds = new ArrayList<>();
			ArrayList<Double> scores = new ArrayList<>();
			//rankings: 0 1 2 3 4 5 6 7 14 15 12 13 10 11 8 9
			botIds.add(0);
			botIds.add(9);
			botIds.add(2);
			botIds.add(3);
			
			botIds.add(15);
			botIds.add(5);
			botIds.add(13);
			botIds.add(7);
			
			scores.add(0.4);
			scores.add(0.2);
			scores.add(0.5);
			scores.add(1.4);
			
			scores.add(0.4);
			scores.add(4.4);
			scores.add(0.3);
			scores.add(2.4);

			//HashMap<Integer, Integer> abc = GAFileHandler.createGroupOrderedRankings(16, botIds,scores,GAFileHandler.CURRENT_LINE, 16, groups);
			//for(Map.Entry<Integer, Integer> me : abc.entrySet()) {
			//	System.out.println("key "+ me.getKey() + ": id" + me.getValue());
			//}
		}  
		

	}
		

	public static void test() {
		
	}

	//public LinkedList<Match> createNewMatches(){
		/*
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
			
		}*/
		
		//return matches;
	//}
	
}

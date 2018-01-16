package genAlgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class TournamentSelector {
	
	public static final int NUM_NGA_ROUNDS = 2;
	public static final int NUM_GA_ROUNDS = 3;
	public static final int PLAYERS_PER_GAME = 4;
	public static final int NUM_INIT_ARGS = 1;
	public static final int NUM_TEST_ARGS = 3;
	public static final int NUM_CONT_ARGS = 0;
	public static final int EXT_PLAYER_ARGS = 2;

	int currentRound;
	ArrayList<String> currentBots;
	ArrayList<String> safeBots;

	HashMap<Integer, Integer> rankings;
	LinkedList<Integer> winners;
	ArrayList<Match> nextMatches;

	int iteration;
	int batch;
	boolean initRun;
	//ArrayList<ArrayList<Double>> scores;

	/*
	 * 
	 */
	public TournamentSelector(boolean initRun) { //is initrun if its the first round of an iteration - matchup file also may be empty

		//get needed data		
		GAFileHandler ioHandler = new GAFileHandler();
		ioHandler.readGAITinfo();	
		ArrayList<Integer> gaitinitinfo = ioHandler.getGaitInit();
		iteration = gaitinitinfo.get(GAFileHandler.GAIT_I_IT);
		batch = gaitinitinfo.get(GAFileHandler.GAIT_I_BA);
		boolean noReplay = true;
		int numIterations = gaitinitinfo.get(GAFileHandler.GAIT_I_N_IT);
		int numBatches = gaitinitinfo.get(GAFileHandler.GAIT_I_N_BA);
		
		if(initRun) {
			if(iteration == -1 || batch == -1) {
				System.out.println("initrun: IT BA finished");
				return;
			}
			System.out.println("initRun!");
			
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
		
		safeBots = ioHandler.getSafeBots();

		int[] next = HaliteGenAlgo.getNextItBa(iteration, batch, numIterations, numBatches);
		if(next[0] == -1 || next[1] == -1 || (iteration == 0 && batch == 0)) {
			noReplay = false;
		}
		
		
		currentBots = GAFileHandler.getBotNames(iteration, batch);

		ArrayList<Match> currentMatches = ioHandler.readMatchup(GAFileHandler.MATCHUP_LAST);
		currentRound = ioHandler.getMatchupLine();
		System.out.println("after round = " + currentRound);
		
		

		if(initRun) {
			nextMatches = currentMatches;
			Match.printMatches(nextMatches);
			ioHandler.createNextMatchesSh(nextMatches, HaliteGenAlgo.MATCHES_PER_EXT_ROUND, noReplay, safeBots); //uses matchup file
			System.out.println("tournamentselector: finished this it (initRound)");

			return;
		}
		
		
		setResults(currentMatches); //creates rankings and writes to the rankings file
		if(currentRound == (NUM_GA_ROUNDS + NUM_NGA_ROUNDS)-1) {
			GAFileHandler.clearMatchesSh();
			System.out.println("tournamentselector: finished this it (final round: " + currentRound + ")");
			return;
		}
		if(currentRound >= NUM_NGA_ROUNDS) {
			//GAFileHandler.updateMatchupLine();

			updateMatchup(); // sets nextMatches for next round (depends on winners)
			ioHandler.createNextMatchesSh(nextMatches, HaliteGenAlgo.MATCHES_PER_TOURN_ROUND, noReplay, safeBots);
		} else { //first rounds: no need to update the whole matchup
			GAFileHandler.updateMatchupLine(); //+1 for matchup line in the matchup file
			nextMatches = ioHandler.readMatchup(currentRound+1); // get the corresponding 
			if(currentRound +1 == NUM_NGA_ROUNDS) {
				ioHandler.createNextMatchesSh(nextMatches, HaliteGenAlgo.MATCHES_PER_TOURN_ROUND, noReplay, safeBots);
			} else {
				ioHandler.createNextMatchesSh(nextMatches, HaliteGenAlgo.MATCHES_PER_EXT_ROUND, noReplay, safeBots);
			}

		}

		System.out.println("tournamentselector: finished this it (round: " + currentRound + ")");

	}
	

	
	
	private void setResults(ArrayList<Match> matches) { //creates rankings and writes to the rankings file
		if(matches.isEmpty()) {
			//no matches last round 
			winners = new LinkedList<>();
			for(int i = 0; i < HaliteGenAlgo.NUM_INDV; i++) {
				winners.add(i);
			}
			
			rankings = new HashMap<>();
			for(int i = 0; i < HaliteGenAlgo.NUM_INDV; i++) {
				rankings.put(i, i);
			}

			System.out.println("warning: matches was empty!");
			return;
		}
		winners = new LinkedList<>();
		System.out.println("setResults: matches.size" + matches.size());
		
		ArrayList<Integer> botIds = getBotIdsByMatches(matches);
		int numPlayersLastMatch = botIds.size();
		int numWinners = numPlayersLastMatch/2;
		int numMatchRounds = numPlayersLastMatch/PLAYERS_PER_GAME;
		int numMatchesPerBot = matches.size() / numMatchRounds;
		
		if(currentRound < NUM_NGA_ROUNDS) {	
			numPlayersLastMatch = HaliteGenAlgo.NUM_INDV;
			numMatchesPerBot = matches.size() / HaliteGenAlgo.NUM_INDV;
			numWinners = HaliteGenAlgo.NUM_INDV;
		}
		
		ArrayList<Double> scores = new ArrayList<>(numPlayersLastMatch);


		System.out.println("numMatchesPerBot = " + numMatchesPerBot);
		for(Integer i : botIds) {
			LinkedList<Double> botScores = GAFileHandler.readBotScores(i, iteration, batch);
			int botScoresSize = botScores.size();

			LinkedList<Double> relevantScores = new LinkedList<Double>();
			for(int j = 1; j == numMatchesPerBot; j++) { //only count relevant = lastScores
				relevantScores.add(botScores.get((botScoresSize)-j));
			}
			scores.add(HaliteGenAlgo.getAverage(relevantScores));
		}
		
		rankings = createRankings(botIds, scores);
		GAFileHandler.addRankings(rankings);


		//int rcount = 0;
		for(Map.Entry<Integer, Integer> me : rankings.entrySet()) {
			System.out.println("ranking at "+ me.getKey() + " is " + me.getValue());
		}

		if(currentRound >= NUM_NGA_ROUNDS && currentRound < (NUM_NGA_ROUNDS+NUM_GA_ROUNDS-1)) { //winners needed
			for(int i = 0; i < rankings.size(); i++) {
				if(i < numWinners) { //get the ranking of index i, if its 
					winners.add(rankings.get(i));
				} else {
					break;
				}
			}
		}
		for(int i = 0; i < winners.size(); i++) {
			System.out.println("Winner IDS: " + winners.get(i));
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

		int num_next_matches = winners.size()/4;
		// PLAYERS_PER_GAME
		//int num_winners = 2; // evtl nicht gebraucht
		
		// %winners.size();
		
		for(int i = 0; i < num_next_matches; i++) {
			int[] players = new int[PLAYERS_PER_GAME];
			for(int j = 0; j < PLAYERS_PER_GAME; j++) {
				players[j] = winners.get(j+i*PLAYERS_PER_GAME);
			}
			matches.add(new Match(players[0], players[1], Match.TYPE_GA, players[2], Match.TYPE_GA, players[3], Match.TYPE_GA));			
		}
		nextMatches = matches;
		GAFileHandler.addMatchup(matches);

	}


	
	private static ArrayList<Integer> getBotIdsByMatches(ArrayList<Match> matches){
		ArrayList<Integer> bots = new ArrayList<>(HaliteGenAlgo.NUM_INDV);
		for(Match m : matches) {
			for(int i = 0; i < PLAYERS_PER_GAME; i++) {
				if(!bots.contains(m.getID(i)) && m.getType(i) == Match.TYPE_GA) {
					bots.add(m.getID(i));
				}
			}
		}
		return bots;
	}
	private static HashMap<Integer, Integer> createRankings(ArrayList<Integer> botIds, ArrayList<Double> scores){
		/*
		 * rankings: is a map of ids, sorted by ranking:
		 * ranking(key):			0 1 2 3 4 5 6 7..
		 * corresponding id: 	    5 2 1 7 0 6 3 4
		 */
		System.out.println("create rankings: scores = "+ scores.size() + " / botids = "+botIds.size());

		HashMap<Integer, Integer> rankings = new HashMap<>(HaliteGenAlgo.NUM_INDV);
		ArrayList<ScoreRef> scoreList = new ArrayList<>();
		for(int i = 0; i < botIds.size(); i++) {
			//System.out.println("adding to wr: = " + scores.get(i) + "/" +botIds.get(i));

			scoreList.add(new ScoreRef(scores.get(i), botIds.get(i)));
		}
		//System.out.println("wr set = " + scoreList.size());
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
			//System.out.println("wsind = " + wsind + ", val = " + sr.getId());
			wsind--;

		}
		//System.out.println("rankings with winners = " + rankings.size());
		

		for(int i = 0; i < HaliteGenAlgo.NUM_INDV; i++) {
			//boolean rankingAdded = false;
			if(!botIds.contains(i)) { 
				if(GAFileHandler.getRankOf(i, GAFileHandler.readRankingsLine()) < scores.size()) {
					System.out.println("createRankings: warning - a winner bot gets overwritten");
				}
				rankings.put(GAFileHandler.getRankOf(i, GAFileHandler.readRankingsLine()), i);
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
	

	public static void main(final String[] args) {
//		GAFileHandler.readBotAttsByName("GABot_A0_0");
		if(args.length == NUM_INIT_ARGS) {
			new TournamentSelector(true);
		} else if(args.length == NUM_CONT_ARGS) {
			new TournamentSelector(false);
		}	else if(args.length == NUM_TEST_ARGS) {
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

			HashMap<Integer, Integer> abc = createRankings(botIds,scores);
			for(Map.Entry<Integer, Integer> me : abc.entrySet()) {
				System.out.println("key "+ me.getKey() + ": id" + me.getValue());
			}
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

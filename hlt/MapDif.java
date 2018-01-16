package hlt;

import java.util.List;
import java.util.Map;

public class MapDif {
	
	public static final int NUM_INFO_VARS = 5;
	
	public static final int ALL_PLANETS = 0;
	public static final int ALL_OWNEDPLANETS = 1;
	public static final int ALL_ENEMYPLANETS = 2;
	public static final int ALL_SHIPS = 3;
	public static final int ALL_ENEMYSHIPS = 4;
	
	public static final int NUM_PERC_VARS = 3;

	public static final int OWNEDPLANETS_PERC = 0;
	public static final int MYSHIPS_PERC = 1;
	public static final int MYPLANETS_PERC = 2;

	//Planets

	int[] info;
	int[] infoDif;
	double[] percentages;
	double[] percDif;
	
	int[] planetsPerId;
	int[] planetsPIDif;

	int[] shipsPerId;
	int[] shipsPIDif;

	//Ships
	
	
	private int myId;
	
	int numPlayers;
	
	public MapDif(GameMap initMap) {
		myId = initMap.getMyPlayerId();
		numPlayers = initMap.getAllPlayers().size();
		info = new int[NUM_INFO_VARS];
		infoDif = new int[NUM_INFO_VARS];

		planetsPerId = new int[numPlayers];
		planetsPIDif = new int[numPlayers];

		shipsPerId = new int[numPlayers];
		shipsPIDif = new int[numPlayers];

		percentages = new double[NUM_PERC_VARS];
		percDif = new double[NUM_PERC_VARS];
		
		update(initMap, true);
	}
	

	public void update(GameMap gameMap, boolean init) {


		List<Ship> ships = gameMap.getAllShips();
		Map<Integer, Planet> planets = gameMap.getAllPlanets();
		if(!init) {
			infoDif = info;
			planetsPIDif = planetsPerId;
			shipsPIDif = shipsPerId;
			percDif = percentages;
		}

		info[ALL_OWNEDPLANETS] = 0;
		info[ALL_ENEMYPLANETS] = 0;
		info[ALL_PLANETS] = planets.size();

        for(Map.Entry<Integer, Planet> entry : planets.entrySet()) {
        	Planet planet = entry.getValue();
        	if(planet.isOwned()) {
        		info[ALL_OWNEDPLANETS]++;
        		planetsPerId[planet.getOwner()] ++;
        		if(planet.getOwner() != myId) {
        			info[ALL_ENEMYPLANETS]++;
        		}
        	}
        }


		info[ALL_ENEMYSHIPS] = 0;
		info[ALL_SHIPS] = ships.size();

        for(Ship s: ships) {
        	shipsPerId[s.getOwner()] ++;

        	if(s.getOwner() != myId) {
        		info[ALL_ENEMYSHIPS]++;
        	}
        }
		Log.log("MDif: update, ");
		

	    percentages[OWNEDPLANETS_PERC] = (double) info[ALL_OWNEDPLANETS] / (double)info[ALL_PLANETS];
		
        percentages[MYSHIPS_PERC] = (double) shipsPerId[myId]/ (double)info[ALL_SHIPS];

        if(info[ALL_OWNEDPLANETS] != 0) {
            percentages[MYPLANETS_PERC] = (double) planetsPerId[myId]/ (double)info[ALL_OWNEDPLANETS];
        } else {
        	percentages[MYPLANETS_PERC] = 0;
        }

		Log.log("MDif: update, test 3");

        if(!init) { // the _Div ints store the old size
        	for(int i = 0; i < infoDif.length;i++) {
    			infoDif[i] = info[i] - infoDif[i];
        	}
        	for(int i = 0; i < planetsPIDif.length;i++) {
        		planetsPIDif[i] = planetsPerId[i] - planetsPIDif[i];
        	}
        	for(int i = 0; i < shipsPIDif.length;i++) {
        		shipsPIDif[i] = shipsPerId[i] - shipsPIDif[i];
        	}
        	for(int i = 0; i < percDif.length;i++) {
        		percDif[i] = percentages[i] - percDif[i];
        	}
        } else {
        	for(int i = 0; i < infoDif.length;i++) {
    			infoDif[i] = 0;
        	}
        	for(int i = 0; i < planetsPIDif.length;i++) {
        		planetsPIDif[i] = 0;
        	}
        	for(int i = 0; i < shipsPIDif.length;i++) {
        		shipsPIDif[i] = 0;
        	}
        	for(int i = 0; i < percDif.length;i++) {
        		percDif[i] = 0.0;
        	}
        }
	}
	
	public int getBestShipPlayerId() {
    	
    	int highestId = 0;
    	int highestScore = 0;
    	int thisScore;
    	for(int i = 0; i < numPlayers; i++) {
    		thisScore = shipsPerId[i];
    		if(thisScore > highestScore) {
    			highestScore = thisScore;
    			highestId = i;
    		}
    	}
    	return highestId;
	}
	
	public int getLowestShipPlayerId() {
    	
    	int lowestId = 0;
    	int lowestScore = Integer.MAX_VALUE;
    	int thisScore;
    	for(int i = 0; i < numPlayers; i++) {
    		thisScore = shipsPerId[i];
    		if(thisScore < lowestScore) {
    			lowestScore = thisScore;
    			lowestId = i;
    		}
    	}

    	return lowestId;
	}
	public int getBestPlanetPlayerId() {
    	
    	int highestId = 0;
    	int highestScore = 0;
    	int thisScore;
    	for(int i = 0; i < numPlayers; i++) {
    		thisScore = planetsPerId[i];
    		if(thisScore > highestScore) {
    			highestScore = thisScore;
    			highestId = i;
    		}
    	}
    	return highestId;
	}
	
	public int getLowestPlanetPlayerId() {
    	
    	int lowestId = 0;
    	int lowestScore = Integer.MAX_VALUE;
    	int thisScore;
    	for(int i = 0; i < numPlayers; i++) {
    		thisScore = planetsPerId[i];
    		if(thisScore < lowestScore) {
    			lowestScore = thisScore;
    			lowestId = i;
    		}
    	}

    	return lowestId;
	}
	
	public boolean infoOverThresh(int index, int threshold) {
		if(info[index] > threshold) {
			return true;
		}
		return false;
	}
	public boolean percExceedThresh(int index, double threshold, boolean over) {
		if(over) {
			if(percentages[index] > threshold) {
				return true;
			}
			return false;
		} else {
			if(percentages[index] < threshold) {
				return true;
			}
			return false;
		}
		

	}
	
	public int getInfoDif(int index) {
		return infoDif[index];
	}
	public double getPercDif(int index) {
		return percDif[index];
	}
	public int getMyPlanetDif() {
		return planetsPIDif[myId];
	}
	public int getMyShipDif() {
		return shipsPIDif[myId];
	}
}

package hlt;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MapDif {
	
	//attribute indices
	public static final int NUM_MAPDIF_ATTS = 11; //these atts will be mapped from 0 .. 1  to  -0.5 .. 0.5
	public static final int NUM_MOP_ATTS = 5;
	public static final int NUM_OPOT_ATTS = 5;

	//for changes in myOwnedPlanets/allOwnedPlanets
	public static final int MOP_CH_EXP = 0;
	public static final int MOP_CH_REI = 1;
	public static final int MOP_CH_ATT = 2;
	public static final int MOP_CH_CON = 3;
	public static final int MOP_CH_DIV = 4;

	//for changes when allOwnedPlanets/alPlanets is over a threshold

	public static final int OPOT_CH_EXP = 5;
	public static final int OPOT_CH_REI = 6;
	public static final int OPOT_CH_ATT = 7;
	public static final int OPOT_CH_CON = 8;
	public static final int OPOT_CH_DIV = 9;
	
	public static final int OPOT_THRESH_V = 10;

	public static final int NUM_INFO_VARS = 5;
	
	public static final int ALL_PLANETS = 0;
	public static final int ALL_OWNEDPLANETS = 1;
	public static final int ALL_ENEMYPLANETS = 2;
	public static final int ALL_SHIPS = 3;
	public static final int ALL_ENEMYSHIPS = 4;
	
	public static final int NUM_PERC_VARS = 3;

	public static final int OWNEDPLANETS_PERC = 0; //OWNEDPLANETS/ALLPLANETS
	public static final int MYSHIPS_PERC = 1; //MYSHIPS/ENEMYSHIPS
	public static final int MYPLANETS_PERC = 2; //MYPLANETS/ALLPLANETS

	//Planets

	int[] info;
	int[] infoDif;
	double[] percentages;
	double[] percDif;
	
	int[] planetsPerId;
	int[] planetsPIDif;

	int[] shipsPerId;
	int[] shipsPIDif;
	
	int strongestEnemyPlayerID;
	int weakestEnemyPlayerID;

	
	HashSet<Integer> activeShips;
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
		activeShips = new HashSet<>();


		if(!init) {
			System.arraycopy(planetsPerId, 0, planetsPIDif, 0, planetsPerId.length);
			System.arraycopy(info, 0, infoDif, 0, info.length);
			
			System.arraycopy(shipsPerId, 0, shipsPIDif, 0, shipsPerId.length);
			
			System.arraycopy(percentages, 0, percDif, 0, percentages.length);

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
        		activeShips.add(s.getId());
        		info[ALL_ENEMYSHIPS]++;
        	}
        }

	    percentages[OWNEDPLANETS_PERC] = (double) info[ALL_OWNEDPLANETS] / (double)info[ALL_PLANETS];
        percentages[MYSHIPS_PERC] = (double) shipsPerId[myId]/ (double)info[ALL_SHIPS];

        if(info[ALL_OWNEDPLANETS] != 0) {
            percentages[MYPLANETS_PERC] = (double) planetsPerId[myId]/ (double)info[ALL_OWNEDPLANETS];
        } else {
        	percentages[MYPLANETS_PERC] = 0;
        }

        if(!init) { // the _Dif arrays store the old size
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
        
        if(myId == 0) {
        	strongestEnemyPlayerID = 1;
        	weakestEnemyPlayerID = 1;
        } else {
        	strongestEnemyPlayerID = 0;
        	weakestEnemyPlayerID = 0;
        }

    	
        for(int i = 0; i < shipsPerId.length; i++) {
        	if(i == myId) {
        		continue;
        	}
        	if(shipsPerId[strongestEnemyPlayerID] < shipsPerId[i]) {
        		strongestEnemyPlayerID = i;
        	}
        	if(shipsPerId[strongestEnemyPlayerID] < shipsPerId[i]) {
        		strongestEnemyPlayerID = i;
        	}
        }
        
        //printAll();
	}
	
	public HashSet<Integer> getMyActiveShips(){
		return activeShips;
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
	public double getPerc(int index) {
		return percentages[index];
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

	public void printAll() {
		Log.log("MapDif:printAll : ");
		

    	for(int i = 0; i <  shipsPerId.length; i++) {
    		Log.log("ships of " +i+":" +shipsPerId[i]);
    	}
    	for(int i = 0; i <  shipsPIDif.length; i++) {
    		Log.log("ships of " +i+" dif:" +shipsPIDif[i]);
    	}
    	
    	for(int i = 0; i <  percentages.length; i++) {
    		Log.log("percentages " +i+":" +percentages[i]);
    	}
    	for(int i = 0; i < percDif.length; i++) {
    		Log.log("percDif " +i+":" +percDif[i]);
    	}

    	
    	for(int i = 0; i <  planetsPerId.length; i++) {
    		Log.log("planets of " +i+":" +planetsPerId[i]);
    	}
    	for(int i = 0; i <  planetsPIDif.length; i++) {
    		Log.log("planetsPIDif " +i+":" +planetsPIDif[i]);
    	}    
    	
    	for(int i = 0; i <  info.length; i++) {
    		Log.log("info " +i+":" +info[i]);
    	}    

    	for(int i = 0; i <  infoDif.length; i++) {
    		Log.log("infoDif " +i+":" +infoDif[i]);
    	}    
    	

	}
	
	public int getStrongestEnemyPlID() {
		return strongestEnemyPlayerID;
	}
	
	public int getWeakestEnemyPlID() {
		return weakestEnemyPlayerID;
		
	}

	
	public static String mapDifAttString(int id) {
	    switch(id) {
	      case MOP_CH_EXP: return "[myPlanets change expand]";
	      case MOP_CH_REI: return "[myPlanets change reinforce]"; 
	      case MOP_CH_ATT: return "[myPlanets change attack]";
	      case MOP_CH_CON: return "[myPlanets change conquer]";
	      case MOP_CH_DIV: return "[myPlanets change diversion]";
	      
	      case OPOT_CH_EXP: return "[planetsOwned thresh changes expand]";
	      case OPOT_CH_REI: return "[planetsOwned thresh changes reinforce]";
	      case OPOT_CH_ATT: return "[planetsOwned thresh changes attack]";
	      case OPOT_CH_CON: return "[planetsOwned thresh changes conquer]";
	      case OPOT_CH_DIV: return "[planetsOwned thresh changes diversion]";
	      
	      case OPOT_THRESH_V: return "[planetsOwned thresh]";

	      default: return "[attack]";
	    }
	}
	  


}

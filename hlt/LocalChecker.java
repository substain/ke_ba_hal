package hlt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import hlt.Ship.DockingStatus;
import hlt.Task.TaskType;

public class LocalChecker {
	
	public static final int NUM_LC_WEIGHTS = 12;
	
	public static final double CON_RNG_FACTOR = 0.17;
	public static final double CON_MYHEALTH_FACTOR = 0.48;
	public static final double CON_DP_SIZE_FACTOR = 0.35;
	
	public static final double ATT_RNG_FACTOR = 0.6;
	public static final double ATT_MYHEALTH_FACTOR = 0.25;
	public static final double ATT_ENEMHEALTH_FACTOR = 0.18;

	public static final double REI_RNG_FACTOR = 0.30;
	public static final double REI_PL_SIZE_FACTOR = 0.45;
	public static final double REI_PL_FULL_FACTOR = 0.25;
	
	public static final double EXP_RNG_FACTOR = 0.45;
	public static final double EXP_PL_SIZE_FACTOR = 0.25;
	public static final double EXP_PL_TARGETED_FACTOR = 0.3;
	
	
	public static final int CON_RNG_FACTOR_I = 0;
	public static final int CON_MYHEALTH_FACTOR_I = 1;
	public static final int CON_DP_SIZE_FACTOR_I = 2;
	
	public static final int ATT_RNG_FACTOR_I = 3;
	public static final int ATT_MYHEALTH_FACTOR_I = 4;
	public static final int ATT_ENEMHEALTH_FACTOR_I = 5;

	
	public static final int REI_RNG_FACTOR_I = 6;
	public static final int REI_PL_SIZE_FACTOR_I = 7;
	public static final int REI_PL_FULL_FACTOR_I = 8;
	
	public static final int EXP_RNG_FACTOR_I = 9;
	public static final int EXP_PL_SIZE_FACTOR_I = 10;
	public static final int EXP_PL_TARGETED_FACTOR_I = 11;

	public static final double DIV_NORM_FACTOR = 0.5;

	
	
	//private TreeMap<Integer, Integer> shipRatings;
	//private TreeMap<Integer, Integer> planetRatings;
	private ArrayList<Entity> entitiesInRange;
	private Entity[] highestEntities;
	private double[] highestTaskRatings;
	private GameMap currentGameMap;
	private int myId;
	private double maxRange;
	private double diversionThresh;
	private double maxPlanetSize;
	private Ship myCurrentShip;
		
	private double targetSpecificPlayer;
	
	public LocalChecker(int myPlayerId, double range, double divThresh, double maxPlSize, double targetStrPlayer) {
		myId = myPlayerId;
		maxRange = range;
		diversionThresh = divThresh * DIV_NORM_FACTOR;
		maxPlanetSize = maxPlSize;
		targetSpecificPlayer = targetStrPlayer;
	}
	
	public void updateRange(double range) {
		maxRange = range;
	}
	
	//maybe also turn in obstacles
	public void compute(Ship myShip, Map<Double, Entity> entitiesByDist, HashMap<Integer, Integer> targetedPlanets, GameMap gmap, int strPlayer, int wkPlayer) {
				
		currentGameMap = gmap;
		myCurrentShip = myShip;
		entitiesInRange = new ArrayList<>();
		boolean nextEnemySet = false;
		//shipRatings = new TreeMap<>();
		//planetRatings = new TreeMap<>();
		highestTaskRatings = new double[Task.NUM_ACTIVE_TYPES];
		for(int i = 0; i < highestTaskRatings.length; i++) {
			highestTaskRatings[i] = -1;
		}
		highestEntities = new Entity[Task.NUM_ACTIVE_TYPES];
		highestTaskRatings[Task.getTaskTypeIndex(TaskType.Diversion)] = diversionThresh;
		Ship nextEnemyShip = null;
		double thisTaskRating = 0;
		//Log.log("LC:compute(), ship " + myShip.getId() + ", entitiesByDist = " + entitiesByDist.size());
		
		for(Map.Entry<Double,Entity> targetEntity : entitiesByDist.entrySet()) {
			double dist = targetEntity.getKey();
			if(dist <= Constants.FLY_RANGE) {
				entitiesInRange.add(targetEntity.getValue());
			}
			if(targetEntity.getValue() instanceof Planet) {

				Planet targetPlanet = (Planet) targetEntity.getValue();
				//Log.log("LC:compute(), planet " + targetPlanet.getId());
				if(targetPlanet.isOwned() && targetPlanet.getOwner() != myId) {
					continue;
				}
				
				int targets = 0; //the number of ships already going for this planet
				if(targetedPlanets.containsKey(targetPlanet.getId())) {
					  targets = targetedPlanets.get(targetPlanet.getId());
				}
				
				
				
				if(targetPlanet.getOwner() == myId && !targetPlanet.isFull()) {
					thisTaskRating = compReinforceScore(myShip, targetPlanet, targets, dist);
					//Log.log("LC:compute(),reinforce planet " + targetPlanet.getId() + ", rating = " + thisTaskRating);

					int index = Task.getTaskTypeIndex(TaskType.Reinforce);
					if(thisTaskRating > highestTaskRatings[index]) {
						highestTaskRatings[index] = thisTaskRating;
						highestEntities[index] = targetPlanet;
					}
					
					
				}	else if(!targetPlanet.isOwned()) {
					thisTaskRating = compExpandScore(myShip, targetPlanet, targets, dist);
					//Log.log("LC:compute(),expand planet " + targetPlanet.getId() + ", rating = " + thisTaskRating);

					int index = Task.getTaskTypeIndex(TaskType.Expand);
					if(thisTaskRating > highestTaskRatings[index]) {
						highestTaskRatings[index] = thisTaskRating;
						highestEntities[index] = targetPlanet;

					}
				}
				
				
			} else if(targetEntity.getValue() instanceof Ship) {
				Ship targetShip = (Ship) targetEntity.getValue();

				if(targetShip.getOwner() == myId) {
					continue;
				}
				boolean strongPlayer = false;
				boolean weakPlayer = false;
				
				if(targetShip.getOwner() == strPlayer) {
					strongPlayer = true;
				} else if (targetShip.getOwner() == wkPlayer) {
					weakPlayer = true;
				}
				DockingStatus dockStatus = targetShip.getDockingStatus();
				if(dockStatus == DockingStatus.Docked || dockStatus == DockingStatus.Docking) {
					Planet dockedPlanet;
					if(dockStatus == DockingStatus.Docked) {
						dockedPlanet = gmap.getPlanet(targetShip.getDockedPlanet());
					} else {
						dockedPlanet = gmap.getNearestPlanet(targetShip);
					}
					thisTaskRating = compConquerScore(myShip, targetShip, dockedPlanet,dist, strongPlayer, weakPlayer);
					//Log.log("LC:compute(),conquer ship " + targetShip.getId() + ", rating = " + thisTaskRating);

					int index = Task.getTaskTypeIndex(TaskType.Conquer);
					if(thisTaskRating > highestTaskRatings[index]) {
						highestTaskRatings[index] = thisTaskRating;
						highestEntities[index] = targetShip;
					}
				} else {
					if(!nextEnemySet) {
					nextEnemyShip = targetShip;
					nextEnemySet = true;
					}

				}
				thisTaskRating = compAttackScore(myShip, targetShip, dist, strongPlayer, weakPlayer);
				//Log.log("LC:compute(),attackany ship " + targetShip.getId() + ", rating = " + thisTaskRating);

				int indexA = Task.getTaskTypeIndex(TaskType.AttackAny);
				if(thisTaskRating > highestTaskRatings[indexA]) {
				highestTaskRatings[indexA] = thisTaskRating;
				highestEntities[indexA] = targetShip;

					
				}
			}
		}
		if(nextEnemyShip == null) {
			highestTaskRatings[Task.getTaskTypeIndex(TaskType.Diversion)] = -1;
		} else {
			highestTaskRatings[Task.getTaskTypeIndex(TaskType.Diversion)] = diversionThresh;
			highestEntities[Task.getTaskTypeIndex(TaskType.Diversion)] = nextEnemyShip;

		}
		
		/*
		String ret = "LC:compute(), taskratings for ship " + myShip.getId() + " = ";
		for(int i = 0; i < highestTaskRatings.length; i++) {
		ret += highestTaskRatings[i] + " ";
		}
		Log.log(ret);
		*/

		
	}
	//distance
	//ship health (attackAny) and dist to my owned planets
	//planet size (reinforce, conquer, 
	//conquer: num ships around planet, num docking ships
	// REI_RNG_FACTOR  REI_PL_SIZE_FACTOR REI_PL_FULL_FACTOR 

	private double compReinforceScore(Ship myShip, Planet tPlanet, int numTargets, double distance) {
		double dscore;
		double dist = myShip.getDistanceTo(tPlanet);
		if(dist > maxRange) {
			dscore = 0;
		} else {
		dscore = (maxRange-dist)/maxRange * REI_RNG_FACTOR;

		}
		
		double capscore = 1;
		int remainingProd  = tPlanet.getRemainingProduction();
		if(remainingProd - numTargets == 1) {
			capscore = 0.8;
		} else if(remainingProd -numTargets <= 0) {
			capscore = 0;
		}
		capscore = capscore * REI_PL_FULL_FACTOR;

		double sizescore = (tPlanet.getRadius() / maxPlanetSize) * REI_PL_SIZE_FACTOR;

		
		return  (dscore + capscore + sizescore)*0.5;

	}
	
	//EXP_RNG_FACTOR  EXP_PL_SIZE_FACTOR  EXP_PL_TARGETED_FACTOR 

	private double compExpandScore(Ship myShip, Planet tPlanet, int numTargets, double distance) {
		double dscore;
		double dist = myShip.getDistanceTo(tPlanet);
		if(dist > maxRange) {
			dscore = 0;
		} else {
			 dscore = (maxRange-dist)/maxRange * EXP_RNG_FACTOR;

		}

		int remainingProd  = tPlanet.getRemainingProduction();
		
		double reiexpscore = 1;
		if(numTargets > remainingProd) {
			reiexpscore = 0;
		} else {
			reiexpscore = 1 - ((double) numTargets / (double) remainingProd);
		}
		reiexpscore = reiexpscore * EXP_PL_TARGETED_FACTOR;

		double sizescore = (tPlanet.getRadius() / maxPlanetSize) * EXP_PL_SIZE_FACTOR;

		return  (dscore + reiexpscore + sizescore)*0.5;
	}
	private double compConquerScore(Ship myShip, Ship tShip, Planet dockedPlanet, double distance, boolean strongPlayer, boolean weakPlayer) {
		double targetSpecFactor;
		if(weakPlayer) {
			targetSpecFactor = 1-targetSpecificPlayer;
		} else if(strongPlayer) {
			targetSpecFactor = targetSpecificPlayer;
		} else {
			targetSpecFactor = 0.5;
		}
		
		double dscore;
		double dist = myShip.getDistanceTo(tShip);
		if(dist > maxRange) {
			dscore = 0;
		} else {
			 dscore = (maxRange-dist)/maxRange * CON_RNG_FACTOR;

		}
		double mhscore = (myShip.getHealth() / Constants.MAX_SHIP_HEALTH) * ATT_MYHEALTH_FACTOR;
		
		double sizescore = (dockedPlanet.getRadius() / maxPlanetSize) * CON_DP_SIZE_FACTOR;
		
		return (dscore + mhscore + sizescore)*targetSpecFactor;
	}
	
	private double compAttackScore(Ship myShip, Ship tShip, double distance, boolean strongPlayer, boolean weakPlayer) {
		double targetSpecFactor;
		if(weakPlayer) {
			targetSpecFactor = 1-targetSpecificPlayer;
		} else if(strongPlayer) {
			targetSpecFactor = targetSpecificPlayer;
		} else {
			targetSpecFactor = 0.5;
		}
		
		double dscore = 0;
		double dist = myShip.getDistanceTo(tShip);
		if(dist > maxRange) {
			dscore = 0;
		} else {
			 dscore = (maxRange-dist)/maxRange * ATT_RNG_FACTOR;

		}
		
		
		double mhscore = (myShip.getHealth() / Constants.MAX_SHIP_HEALTH) * ATT_MYHEALTH_FACTOR;
		
		double ehscore = ((Constants.MAX_SHIP_HEALTH - tShip.getHealth()) / Constants.MAX_SHIP_HEALTH) * ATT_ENEMHEALTH_FACTOR;

		return (dscore + mhscore + ehscore)*targetSpecFactor;
	}
	/*
	private double getDiversionScore() {
		return 0;
	}*/
	
	public ArrayList<Entity> getEntitiesInRange(){
		return entitiesInRange;
	}
	
	
	public Task getHighestTastPreferType(TaskType tt) {
		int index = Task.getTaskTypeIndex(tt);
		
		//maybe no Task of this type exists
		if(highestTaskRatings[index] <= 0) {
			return getHighestTask();
		}

		Entity targetEntity = highestEntities[index];

		return new Task(myCurrentShip, currentGameMap, tt, targetEntity);
	}
	
	public double getHighestTaskScore() {
		double highestTaskScore = 0;
		for(int i = 0; i < highestTaskRatings.length; i++) {
			if(highestTaskRatings[i] > highestTaskScore) {
				highestTaskScore = highestTaskRatings[i];
			}
		}
		return highestTaskScore;
	}
	
	public Task getHighestTask() {
		double highestTaskScore = 0;
		int highestTaskIndex = 0;
		for(int i = 0; i < highestTaskRatings.length; i++) {
			if(highestTaskRatings[i] > highestTaskScore) {
				highestTaskScore = highestTaskRatings[i];
				highestTaskIndex = i;
			}
		}
		Entity targetEntity = highestEntities[highestTaskIndex];
		return new Task(myCurrentShip, currentGameMap, Task.getTaskTypeByIndex(highestTaskIndex), targetEntity);
	}
	
	
	public Ship getDivTarget() {
		return  (Ship)highestEntities[Task.getTaskTypeIndex(TaskType.Diversion)];
	}
	
	public double getHighestDefTask() {
		// TODO

		return 0;
	}
	
	
	public double getHighestOffTask() {
		// TODO

		return 0;
	}
	
	/*
	  int targets = 0;
	  if(targetedFreePlanets.containsKey(targetPlanet.getId())) {
		  targets = targetedFreePlanets.get(targetPlanet.getId());

	  }
	  
	  if((targetPlanet.isOwned() && targetPlanet.getOwner() != myId) || targetPlanet.isFull()) { //not a planet to dock on
		  continue; 
	  }
	  
	  if(!anyPlSet) { //nothing to compare yet, init bestAnyPlanet
		  bestAnyPlanet = targetPlanet;
		  anyPlSet = true;
		  min_a_range = Math.max(thisShip.getDistanceTo(targetPlanet), min_a_range);
		  //logstr += "a_range = " + min_a_range;
		  //logstr += " (n=" + targetPlanet.getDistanceTo(thisShip) + "/" + targetPlanet.getRadius() +") -> pl:" + targetPlanet.getId() + " /// ";

		  
		  if(!targetPlanet.isOwned() && prioritizeUnowned && targets==0) { // init bestUnownedPlanet
			  
			  bestUnownedPlanet = targetPlanet;
			  unownedPlSet = true;
			  min_u_range = min_a_range;
			  //logstr += "u_range = " + min_a_range;
			  //logstr += " (n=" + targetPlanet.getDistanceTo(thisShip) + "/" + targetPlanet.getRadius() +") -> upl:" + targetPlanet.getId() + " /// ";
		  }
		  continue;
	  }
	  

	  double thisPlDist = thisShip.getDistanceTo(targetPlanet);
	  double thisPlRadius = targetPlanet.getRadius();*/
	
}

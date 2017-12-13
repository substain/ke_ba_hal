import hlt.*;
import hlt.Ship.DockingStatus;
import hlt.Task.TaskStatus;
import hlt.Task.TaskType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ModifiedBot {

	private static double distanceUnit;
	
    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("ModdedBot");

        int gmWidth = gameMap.getWidth();
        int gmHeight = gameMap.getHeight();
        final double usedFudge = Constants.FORECAST_FUDGE_FACTOR;
        Map<Integer, Planet> lastKnownPlanets = gameMap.getAllPlanets();
        Map<Integer, Ship> lastKnownShips = gameMap.getMyPlayer().getShips();

        // We now have 1 full minute to analyse the initial map.
        final String initialMapIntelligence =
                "width: " + gmWidth +
                "; height: " + gmHeight +
                "; players: " + gameMap.getAllPlayers().size() +
                "; planets: " + gameMap.getAllPlanets().size();
        Log.log(initialMapIntelligence);
        
        distanceUnit =(gmWidth + gmHeight)/2 / (lastKnownPlanets.size()*2);
        Log.log("distUnit = " + distanceUnit);

        //create a map of coordinates, where true means a planet (or its safety-zone) is on that coordinate
        boolean[][] hitmap = createHitmap(gameMap, usedFudge);
        
        HashMap<Integer, Task> tasks = new HashMap<>();
        HashMap<Integer, Position> lastPositions = new HashMap<>();

        final ArrayList<Move> moveList = new ArrayList<>();

        for (;;) {
            moveList.clear();
            networking.updateMap(gameMap);
            HashMap<Integer, Task> newTasks = new HashMap<>();
        	HashMap<Integer, Position> expectedPositions = new HashMap<>();

            Map<Integer, Planet> currentPlanets = gameMap.getAllPlanets();
        	if(currentPlanets.size() < lastKnownPlanets.size()) {
        		int numDestroyedPlanets = lastKnownPlanets.size() - currentPlanets.size();
        		updateHitmap(hitmap, lastKnownPlanets, currentPlanets, numDestroyedPlanets, usedFudge);
        		lastKnownPlanets = currentPlanets;
        	}
            HashMap<Integer, Position> currentPostions = listEnemyShipPositions(gameMap);
            if(!lastPositions.isEmpty()) {
            	expectedPositions = estimateShipPositions(currentPostions, lastPositions);
            }
            
            ArrayList<Planet> targetedFreePlanets = new ArrayList<>();
            		
            int ship_id = 0;		
            double range = Math.min(gmWidth, (double)(0.10*gameMap.getMyPlayer().getShips().size()*distanceUnit));
            
            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {

            	if(tasks.containsKey(ship.getId())) {
            		Task currentTask = tasks.get(ship.getId());
            		TaskStatus currentStatus = currentTask.checkStatus();
            		if(currentStatus != TaskStatus.Invalid) {
            			if(currentTask.isAttackType() && expectedPositions.containsKey(currentTask.getTargetId())) {
            				currentTask.setEstimatedPos(expectedPositions.get(currentTask.getTargetId()));
            			}
            			//setObstructedPositions
            			Move move = currentTask.computeMove();
            			//getExpectedPos() and set this in an updated liveHitMap
            			if(move != null) {
            				moveList.add(move);
            				if(currentStatus == TaskStatus.WillDock) {
            					newTasks.put(ship.getId(), new Task(ship, gameMap, TaskType.Dock, currentTask.getTarget()));
            				} else {
                				newTasks.put(ship.getId(), currentTask);
            				}
                			continue;
            			}
            		}
            	}
            	
            	
            
            	
                Map<Double, Entity> entities_by_dist = gameMap.nearbyEntitiesByDistance(ship);
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
            		//Log.log("Ship " + ship.getId() + ": (docked) \n");
                    continue;
                }
                
            	ship_id++;

                //not very sophisticated: once a ship is destroyed or docks, the order and the behavior of an individual ship will probably change
            	//2 out of 5 ships will attack (or all of the rest, if the amount of ships reaches a specific threshold
                if((ship_id % 5) > 2 || ship_id > (gameMap.getAllPlanets().size() * 2)) {
                	Move move = offensiveBehavior(ship, entities_by_dist, gameMap);
                	if(move != null) {
                		moveList.add(move);
                	}
                } else {  //some ships will dock to planets (or all of the rest, if the amount of ships reaches a specific threshold
                	boolean target_new_planets = false;
                	if((ship_id % 5) >= 1) { //some ships will go to unowned planets
                		target_new_planets = true;
                	}
                	Move move = defensiveBehavior(ship, entities_by_dist, gameMap, target_new_planets, targetedFreePlanets, range);
                	if(move != null) {
                		moveList.add(move);
                	}
                } 


            }
            tasks = newTasks;
            Networking.sendMoves(moveList);
        }
    }
	
	static private Move offensiveBehavior(Ship thisShip, Map<Double, Entity> dist_sorted_entities, GameMap gameMap) {
        //final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED/2);
		for(Map.Entry<Double,Entity> targetEntity : dist_sorted_entities.entrySet()) {
			  if(targetEntity.getValue() instanceof Ship) {
				  Ship targetShip = (Ship) targetEntity.getValue();
				  if(targetShip.getOwner() != gameMap.getMyPlayerId()) { //none of my own ships
					  if(thisShip.getDistanceTo(targetShip) <= Constants.WEAPON_RADIUS + 1) {
						  return Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetShip, Constants.MAX_SPEED/2);
					  } else {
						  return Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetShip, (int)(Constants.MAX_SPEED * 0.9));
					  }
				  }
			  }
		}

		return null;
	}
	
	
	static private Move defensiveBehavior(Ship thisShip, Map<Double, Entity> dist_sorted_entities, GameMap gameMap, boolean prioritizeUnowned, ArrayList<Planet> targeted, double range) {
		Planet targetPlanet = findBestPlanet(thisShip, dist_sorted_entities, prioritizeUnowned, gameMap.getMyPlayerId(), targeted, range);
		if(targetPlanet != null) {
			Log.log("Ship " + thisShip.getId() + ": (move to planet) -> " + targetPlanet.getId() +" \n");
			return moveToPlanet(thisShip, targetPlanet, gameMap);
		}
		return offensiveBehavior(thisShip, dist_sorted_entities, gameMap); //no free planet
	}
	
	
	
	static private Move moveToPlanet(Ship thisShip, Planet targetPlanet, GameMap gameMap) {
		if (thisShip.canDock(targetPlanet)) {
			return new DockMove(thisShip, targetPlanet);
		} else {
			return Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetPlanet, Constants.MAX_SPEED);
		}
	}
	
	static private Ship findBestEnemyShip(Ship thisShip, Map<Double, Entity> dist_sorted_entities, GameMap gameMap, double range){
		
		Ship bestTarget = null;
		boolean bestTargetSet = false;
		boolean bestTargetDocks = false;

		double min_range = 2*distanceUnit + range;
		
		for(Map.Entry<Double,Entity> targetEntity : dist_sorted_entities.entrySet()) {
			if(targetEntity.getValue() instanceof Ship) {
				  Ship targetShip = (Ship) targetEntity.getValue();
				  
				  if(targetShip.getOwner() == gameMap.getMyPlayerId()) { //not a planet to dock on
					  continue; 
				  }
				  
				  if(!bestTargetSet) { //nothing to compare yet, init bestAnyPlanet
					  bestTarget = targetShip;
					  bestTargetSet = true;
					  min_range = Math.max(thisShip.getDistanceTo(bestTarget), min_range);
					  //logstr += "a_range = " + min_a_range;
					  //logstr += " (n=" + targetPlanet.getDistanceTo(thisShip) + "/" + targetPlanet.getRadius() +") -> pl:" + targetPlanet.getId() + " /// ";

					  continue;
				  } else { //out of searching range
					  if(thisShip.getDistanceTo(targetShip) > min_range + (2*distanceUnit + range)) {
						  break;
					  }
				  }
				  
				  DockingStatus dock_status = targetShip.getDockingStatus();
				  if(dock_status == DockingStatus.Docked || dock_status == DockingStatus.Docking) {
					  if(bestTargetDocks) {
						  Planet dockedPlanet = gameMap.getPlanet(targetShip.getDockedPlanet());
						  Planet lastDockedPlanet = gameMap.getPlanet(bestTarget.getDockedPlanet());
						  if(lastDockedPlanet == null) {
							  if(dockedPlanet == null) {
								  continue; //both ships equal, prefer previous (closer) ship
							  } else {
								  bestTarget = targetShip; //current ship is docked to a planet -> better target
							  }
							  
						  } else { //both ships docked to a planet
							  if(dockedPlanet != null && dockedPlanet.getRadius() > lastDockedPlanet.getRadius()) {
								  bestTarget = targetShip;
							  }
						  }

					  }
					  else {
						  bestTargetDocks = true;
						  bestTarget = targetShip;
					  }
				  }

				  				  
			}

		}
				
		return bestTarget;
	}


	static private Planet findBestPlanet(Ship thisShip, Map<Double, Entity> dist_sorted_entities, boolean prioritizeUnowned, int myId, ArrayList<Planet> targetedPlanets, double range){
		String logstr;
		if(prioritizeUnowned) {
			logstr = "Ship " + thisShip.getId() + ": (ufind)"; //debug

		} else {
			logstr = "Ship " + thisShip.getId() + ": (find)"; //debug
		}

		
		Planet bestAnyPlanet = null;
		Planet bestUnownedPlanet = null;
		boolean anyPlSet = false;
		boolean unownedPlSet = false;
		
		double min_a_range = 2*distanceUnit + range;
		double min_u_range = 2*distanceUnit + range;

		for(Map.Entry<Double,Entity> targetEntity : dist_sorted_entities.entrySet()) {
			if(targetEntity.getValue() instanceof Planet) {
				  Planet targetPlanet = (Planet) targetEntity.getValue();
				  
				  if((targetPlanet.isOwned() && targetPlanet.getOwner() != myId) || targetPlanet.isFull()) { //not a planet to dock on
					  continue; 
				  }
				  
				  if(!anyPlSet) { //nothing to compare yet, init bestAnyPlanet
					  bestAnyPlanet = targetPlanet;
					  anyPlSet = true;
					  min_a_range = Math.max(thisShip.getDistanceTo(targetPlanet), min_a_range);
					  //logstr += "a_range = " + min_a_range;
					  //logstr += " (n=" + targetPlanet.getDistanceTo(thisShip) + "/" + targetPlanet.getRadius() +") -> pl:" + targetPlanet.getId() + " /// ";

					  if(!targetPlanet.isOwned() && prioritizeUnowned && !targetedPlanets.contains(targetPlanet)) { // init bestUnownedPlanet
						  
						  bestUnownedPlanet = targetPlanet;
						  unownedPlSet = true;
						  min_u_range = min_a_range;
						  //logstr += "u_range = " + min_a_range;
						  //logstr += " (n=" + targetPlanet.getDistanceTo(thisShip) + "/" + targetPlanet.getRadius() +") -> upl:" + targetPlanet.getId() + " /// ";
					  }
					  continue;
				  }
				  

				  double thisPlDist = thisShip.getDistanceTo(targetPlanet);
				  double thisPlRadius = targetPlanet.getRadius();
				  
				  if(thisPlRadius > bestAnyPlanet.getRadius() * 1.1 && thisPlDist < min_a_range + 2*distanceUnit + range) {
					  bestAnyPlanet = targetPlanet;
					  logstr += "=>(b=" + targetPlanet.getDistanceTo(thisShip) + "/" + targetPlanet.getRadius() +") -> pl:" + targetPlanet.getId() + " /// ";

				  }
				  
				  if(prioritizeUnowned && !targetPlanet.isOwned() && !targetedPlanets.contains(targetPlanet)) {
					  if(!unownedPlSet) {
						  bestUnownedPlanet = targetPlanet;
						  unownedPlSet = true;
						  min_u_range = Math.max(thisShip.getDistanceTo(targetPlanet), min_u_range);
						  //logstr += "u_range = " + min_a_range;
						  //logstr += " (n=" + targetPlanet.getDistanceTo(thisShip) + "/" + targetPlanet.getRadius() +") -> upl:" + targetPlanet.getId() + " /// ";
						  
					  } else {
						  
						  //equivalent computation as with bestAnyPlanet
						  if(thisPlRadius > bestUnownedPlanet.getRadius() * 1.1 && thisPlDist < min_u_range + 2*distanceUnit + range) {
							  bestUnownedPlanet = targetPlanet;
							  //logstr += "=>(b=" + targetPlanet.getDistanceTo(thisShip) + "/" + targetPlanet.getRadius() +") -> upl:" + targetPlanet.getId() + " /// ";
						  }
						  
					  }
				  }
				  
			}

		}
		
		logstr += "\n";
		//Log.log(logstr);
		if(prioritizeUnowned && unownedPlSet) {
			targetedPlanets.add(bestUnownedPlanet);
			return bestUnownedPlanet;
		}
				
		return bestAnyPlanet; //may be null -> all Planets owned by someone else
	}
	
	static HashMap<Integer, Position> listEnemyShipPositions(GameMap gameMap){
		HashMap<Integer, Position> shipPositions = new HashMap<>();
		for(Ship s : gameMap.getAllShips()) {
			if(s.getOwner() != gameMap.getMyPlayerId()) {
				shipPositions.put(s.getId(), s);
			}
		}
		return shipPositions;
	}
	
    static HashMap<Integer, Position> estimateShipPositions(HashMap<Integer, Position> currentPositions, HashMap<Integer, Position> oldPositions){
		HashMap<Integer, Position> estimatedPositions = new HashMap<>();
		
		for(Map.Entry<Integer, Position> shipEntry : oldPositions.entrySet()) {
			Position lastPos = shipEntry.getValue();
			Position currentPos = currentPositions.get(shipEntry.getKey());
			estimatedPositions.put(shipEntry.getKey(), Position.getOppositePos(currentPos, lastPos));
		}
		
		return estimatedPositions;
    }
    
    static boolean[][] createHitmap(GameMap gameMap, double fudge){
		Log.log(" ##### creating hitmap ##### ");

    	int gm_width = gameMap.getWidth();
        int gm_height = gameMap.getHeight();
    	boolean[][] hitmap = new boolean[gm_width][gm_height];
        for(int i = 0; i < hitmap.length; i++) {
            Arrays.fill( hitmap[i], false);
        }
        
        for(Map.Entry<Integer, Planet> entry : gameMap.getAllPlanets().entrySet()) {
        	Planet planet = entry.getValue();
            double radius = planet.getRadius();

        	int minX = (int) (planet.getXPos() - (radius + fudge));
        	int maxX = (int) (planet.getXPos() + radius + fudge);
        	int minY = (int) (planet.getYPos() - (radius + fudge));
        	int maxY = (int) (planet.getYPos() + radius + fudge);
        	for(int i = minX; i < maxX; i++) {
        		for(int j = minY; j < maxY; j++) {
        			if(Collision.pointInsideCircle(new Position(i,j), planet, fudge)) {
        				hitmap[i][j] = true;
        				//Log.log("(creating collision point, " + i + ")");
        			}
        		}
        	}
        }
        return hitmap;
    }
	

    static boolean[][] updateHitmap(boolean[][] hitmap, Map<Integer,Planet> previousPlanets, Map<Integer, Planet> currentPlanets, int numDestroyedPlanets, double fudge){    	int planetsToRemove = numDestroyedPlanets;
		Log.log(" ##### updating hitmap ##### ");

    
    	boolean[][] updatedHitmap = hitmap;
        for(Map.Entry<Integer, Planet> prEntry : previousPlanets.entrySet()) {
    		if(!currentPlanets.containsKey(prEntry.getKey())) {
    		
    			planetsToRemove--;
    			Planet destroyedPlanet = prEntry.getValue();
    			double radius = destroyedPlanet.getRadius();
    			
    			int minX = (int) (destroyedPlanet.getXPos() - (radius + fudge));
            	int maxX = (int) (destroyedPlanet.getXPos() + radius + fudge);
            	int minY = (int) (destroyedPlanet.getYPos() - (radius + fudge));
            	int maxY = (int) (destroyedPlanet.getYPos() + radius + fudge);
            	
            	for(int i = minX; i < maxX; i++) {
            		for(int j = minY; j < maxY; j++) {        				
            			updatedHitmap[i][j] = false;
            		}
            	}
    			
    			if(planetsToRemove==0) {
    				return updatedHitmap;
    			}
    		}

        }
		return null;

    }
    
    static Task createTask(Ship ship, GameMap gameMap, Status gameStatus) {
		return null;
    	
    }
	
}

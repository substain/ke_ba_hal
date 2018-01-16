import hlt.*;
import hlt.Move.MoveType;
import hlt.Ship.DockingStatus;
import hlt.Task.TaskStatus;
import hlt.Task.TaskType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import genAlgo.GAFileHandler;



public class ModifiedBot {
	public static final boolean useBotID = true;

	private static double distanceUnit;
	
    public static void main(final String[] args) throws IOException {
        final Networking networking = new Networking();
        boolean usingBotID = false;
        String botname = "ModdedBot";
        if(useBotID && args.length > 0) {
        	botname = args[0];
        	usingBotID = true;
        }
        final GameMap gameMap = networking.initialize(botname);
        
        
        int myId = gameMap.getMyPlayerId();
        int gmWidth = gameMap.getWidth();
        int gmHeight = gameMap.getHeight();
        
        

        double[] attributes = new double[Control.NUM_ATTS];
        //double[] finalShipDistr = new double[Task.NUM_ACTIVE_TYPES];
        //double roundAttNormFactor = (Constants.MAX_NUM_ROUNDS/HaliteGenAlgo.NUM_ATT_SETTINGS_PER_GAME)/HaliteGenAlgo.ATT_MAX;
        ///int roundsWithoutChange = 20;
        //int roundsUntilFinalDist = 20;
        int currentIt = -1;
        
		boolean fileNotFound = false;

        if(usingBotID) {
        	GAFileHandler gaIOHandler = new GAFileHandler();
        	gaIOHandler.readGAITinfo();
        	ArrayList<Integer> gaitInitInfo = gaIOHandler.getGaitInit();
        	currentIt = gaitInitInfo.get(GAFileHandler.GAIT_I_IT);

        	//if file not exist in bots, look in safeBots else dont load
    		Path dir = Paths.get(".").toAbsolutePath().normalize();
    		Path fPath = Paths.get(dir.toString(), GAFileHandler.CFG_FOLDERNAME, GAFileHandler.GA_CFG_FOLDERNAME, botname + ".txt");
    		File file = new File(fPath.toString());
    		if(!file.exists()) {
    			Path fPath2 = Paths.get(dir.toString(), GAFileHandler.CFG_FOLDERNAME, GAFileHandler.SAFE_CFG_FOLDERNAME, botname + ".txt");
        		file = new File(fPath2.toString());
        		if(!file.exists()) {
        			fileNotFound = true;
        		}

    		}
    		Log.log("attribute file: " + !fileNotFound);
    		if(!fileNotFound) {
    			attributes =  GAFileHandler.readBotAttsByName(botname);
            	Log.log("Attribute file found and loaded.");
    		}

        }
        if(!usingBotID || fileNotFound) {
        	Log.log("static ship distr");

	        for(int i = 0; i < attributes.length; i++) {
	        	attributes[i] = 0;
	        }
	        attributes[Task.getTaskTypeIndex(TaskType.AttackAny)] = 0.15;
	        attributes[Task.getTaskTypeIndex(TaskType.Reinforce)] = 0.35;
	        attributes[Task.getTaskTypeIndex(TaskType.Expand)] = 0.5;

        }
        
        
        
        for(int i = 0; i < attributes.length; i++) {
        	Log.log("shipdistr("+i+"): " + Task.getTaskTypeByIndex(i).toString() + " : " + attributes[i]);
        }
        
        double[] shipDistribution = new double[Task.NUM_ACTIVE_TYPES];
        for(int i = 0; i < shipDistribution.length; i++) {
        	shipDistribution[i] = attributes[i];
        }
        
        double taskChangeTime = attributes[Task.NUM_ACTIVE_TYPES];
        double addFactor = attributes[Task.NUM_ACTIVE_TYPES+1];

        final Control controller = new Control(myId, shipDistribution, taskChangeTime, addFactor); 
        //controller.changeRatioOverTime(finalShipDistr,roundsWithoutChange, roundsUntilFinalDist);
        
        final Evaluator evaluator = new Evaluator(gameMap, botname);
        
        
        evaluator.setIteration(currentIt);
        //final double usedFudge = Constants.FORECAST_FUDGE_FACTOR;
        Map<Integer, Planet> lastKnownPlanets = gameMap.getAllPlanets();
        //Map<Integer, Ship> lastKnownShips = gameMap.getMyPlayer().getShips();

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
        //boolean[][] hitmap = createHitmap(gameMap, usedFudge); //TODO only keep this in PathFinder
        //final PathFinder pfinder = new PathFinder(hitmap);
        ShadowPathFinder shPathFinder = new ShadowPathFinder(gameMap);
        final MapDif mdif = new MapDif(gameMap);

        //DEBUG
		Log.log(shPathFinder.allPlanetsToString());
		
		
		


        HashMap<Integer, Task> tasks = new HashMap<>();
        HashMap<Integer, Position> lastPositions = new HashMap<>();

        final ArrayList<Move> moveList = new ArrayList<>();
        
        //HashMap<Integer, Position> lastShipPositions = new HashMap<>();
        //HashMap<Integer, Position> currentShipPositions = new HashMap<>();
        
        //int rounds = 0;
        for (;;) {
        	       	
        	moveList.clear();
            networking.updateMap(gameMap);
            controller.setDynPossibleTasks(gameMap);

            evaluator.evaluateRound(gameMap); //computes current score
            
            
        	// ALL_PLANETS  ALL_OWNEDPLANETS ALL_ENEMYPLANETS  ALL_SHIPS ALL_ENEMYSHIPS 
        	// OWNEDPLANETS_PERC MYSHIPS_PERC  MYPLANETS_PERC 
            mdif.update(gameMap, false);
            
            int myOwnedPlanetDif = mdif.getMyPlanetDif();
        	double ownedPlanetsPercDif = mdif.getPercDif(MapDif.OWNEDPLANETS_PERC);
        	double myShipsPerc = mdif.getPercDif(MapDif.MYSHIPS_PERC);

        	if(mdif.percExceedThresh(MapDif.OWNEDPLANETS_PERC, 0.5, true)) {
        		controller.changeRatioField(TaskType.AttackAny, true);
        		controller.changeRatioField(TaskType.AttackAny, true);
        		controller.changeRatioField(TaskType.Conquer, true);
        	}
            if(myOwnedPlanetDif > 0) {
        		controller.changeRatioField(TaskType.AttackAny, true);
        		controller.changeRatioField(TaskType.Reinforce, true);
        		controller.changeRatioField(TaskType.Expand, false);

            } else if (myOwnedPlanetDif < 0) {
        		controller.changeRatioField(TaskType.Reinforce, false);
            }
            if(ownedPlanetsPercDif < 0) {
        		controller.changeRatioField(TaskType.Expand, true);

            } else if(ownedPlanetsPercDif > 0) {
        		controller.changeRatioField(TaskType.Expand, false);
        		controller.changeRatioField(TaskType.Reinforce, true);
            } 
            if(myShipsPerc < 0) {
        		controller.changeRatioField(TaskType.AttackAny, false);
        		controller.changeRatioField(TaskType.Conquer, false);

            } else if(myShipsPerc > 0) {
        		controller.changeRatioField(TaskType.Conquer, true);
            } 
            
            
            
            Position fleePos = getFleePos(gameMap);

            HashMap<Integer, Task> newTasks = new HashMap<>();
            
            
            
        	//check if hitmap is up to date
            Map<Integer, Planet> currentPlanets = gameMap.getAllPlanets();
            if(mdif.getInfoDif(MapDif.ALL_PLANETS) < 0) {
        		int numDestroyedPlanets = lastKnownPlanets.size() - currentPlanets.size();
        		shPathFinder = updateShadowPathFinder(shPathFinder, lastKnownPlanets, currentPlanets, numDestroyedPlanets);
        		//hitmap = updateHitmap(hitmap, lastKnownPlanets, currentPlanets, numDestroyedPlanets, usedFudge);
        		//pfinder.updateMap(hitmap);
        		
        		lastKnownPlanets = currentPlanets;
            }

        	//boolean[][] currentHitmap = hitmap; // this copy is updated with the ship positions computed this turn to avoid collisions
        	
        	
        	
        	//compute expected ship movements this turn, only using the last turns positions
        	HashMap<Integer, Position> expectedPositions = new HashMap<>();
            HashMap<Integer, Position> currentPostions = listEnemyShipPositions(gameMap);
            if(!lastPositions.isEmpty()) {
            	expectedPositions = estimateShipPositions(currentPostions, lastPositions);
            }
            
            HashMap<Integer, Integer> targetedPlanets = new HashMap<>();
            		
//            int ship_id = 0;	
            int ship_count = 0;
            double range = Math.min(gmWidth, (double)(0.20*gameMap.getMyPlayer().getShips().size()*distanceUnit));

            
            Collection<Ship> shiplist = gameMap.getMyPlayer().getShips().values();
            ArrayList<Entity> myExpectedPositions = new ArrayList<>(shiplist.size());

            for (final Ship ship : shiplist) {
            	Log.log("debug: ship" + ship_count + ", id:" + ship.getId() + ", pos:" + ship.getXPos() +"|"+ ship.getYPos());
            	ship_count++;
            	if(tasks.containsKey(ship.getId())) {
            		Task currentTask = tasks.get(ship.getId());
            		currentTask.update(gameMap, ship);
            		TaskStatus currentStatus = currentTask.getStatus();
            		if(currentStatus != TaskStatus.Invalid) {
            			currentTask.setObstructedPositions(myExpectedPositions);

            			if(currentTask.isAttackType() && expectedPositions.containsKey(currentTask.getTargetId())) {
            				currentTask.setEstimatedPos(expectedPositions.get(currentTask.getTargetId()));
            			}
            			
            			if(currentTask.isDiversion()) {
            				

            				currentTask.setFleePos(fleePos);
                			Log.log("computing fleePath");

                			LinkedList<Position> fleePath = shPathFinder.getPathToPos(ship, fleePos, gameMap);
                			if(!fleePath.isEmpty()) {
                				currentTask.setFleePath(fleePath);
                			} else {
                    			Log.log("fleePath empty :(");
                			}

            			}
            			
            			//TODO: use pathfinder here
            			
            			if(currentTask.needsPath()) {
            				LinkedList<Position> shpath = shPathFinder.getPathToPos(ship, currentTask.getTarget(), gameMap);
            				if(!shpath.isEmpty()) {
                    			currentTask.setPath(shpath);
            				}
            			}
            			
            			//TODO: setObstructedPositions here
            			Move move = currentTask.computeMove();
            			//getExpectedPos() and set this in an updated liveHitMap
            			if(move != null) {
            				moveList.add(move);
            				if(move.getType() == MoveType.Thrust) {
            					ThrustMove tm = (ThrustMove) move;
            					Position thisExpectedPos = tm.getExpectedPosition(ship);
            					//currentHitmap[expectedX][expectedY] = true;
            					myExpectedPositions.add(new Entity(-1, -1, thisExpectedPos.getXPos(), thisExpectedPos.getYPos(), 10, Constants.SHIP_RADIUS));
            				}
                        	
            				if(currentTask.getType() != TaskType.Dock) {
                				controller.increaseShipNum(currentTask.getType());
            				}
                   
            				Log.log("Ship: " + ship.getId() + " setting move (cont, "+ currentTask.getType().toString() +" ). \n");
            				if(currentStatus == TaskStatus.WillDock) {
            					newTasks.put(ship.getId(), new Task(ship, gameMap, TaskType.Dock, currentTask.getTarget()));
            				} else {
                				newTasks.put(ship.getId(), currentTask);
            				}
                			continue;
            			}
            		} else {
        				Log.log("Ship: " + ship.getId() + " move (cont, "+ currentTask.getType().toString() +" was invalid, get new one ). \n");
        				//tasks.remove(ship.getId());
            		}
            	}
            	
                Map<Double, Entity> entities_by_dist = gameMap.nearbyEntitiesByDistance(ship);

            	/*
            
            	
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
            		//Log.log("Ship " + ship.getId() + ": (docked) \n");
                    continue;
                }
                */
//            	ship_id++;
            	
            	Task nTask;
            	TaskType newType = controller.getNextTypeAndUpdate(); 
            	switch(newType) {
            	case Diversion:
            		Ship tShip = getNearestEnemyShip(ship, entities_by_dist, gameMap);
                	if(tShip != null) {
                    	nTask = new Task(ship, gameMap, TaskType.Diversion, tShip);
                    	nTask.setObstructedPositions(myExpectedPositions);

                    	if(expectedPositions.containsKey(tShip.getId())) {
                    		Position estPos = expectedPositions.get(tShip.getId());
                    		nTask.setEstimatedPos(estPos);
            				LinkedList<Position> shpath = shPathFinder.getPathToPos(ship, estPos, gameMap);
            				if(!shpath.isEmpty()) {
            					nTask.setPath(shpath);
            				}

            			} else {
            				LinkedList<Position> shpath = shPathFinder.getPathToPos(ship, tShip, gameMap);
            				if(!shpath.isEmpty()) {
            					nTask.setPath(shpath);
            				}

            			}

            			
            			nTask.setFleePos(fleePos);
            			Log.log("computing fleePath");

            			LinkedList<Position> fleePath = shPathFinder.getPathToPos(ship, fleePos, gameMap);
            			if(!fleePath.isEmpty()) {
                			nTask.setFleePath(fleePath);
            			} else {
                			Log.log("fleePath empty :(");

            			}
                    	Move move = nTask.computeMove();
            			if(move != null) {
            				ThrustMove tm = (ThrustMove) move;
        					Position thisExpectedPos = tm.getExpectedPosition(ship);
        					//currentHitmap[expectedX][expectedY] = true;
        					myExpectedPositions.add(new Entity(-1, -1, thisExpectedPos.getXPos(), thisExpectedPos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR));
        					
            				moveList.add(move);
            				Log.log("Ship: " + ship.getId() + " setting move (new, "+ nTask.getType().toString() +" ). \n");
                        	newTasks.put(ship.getId(), nTask);
            			}
            			
                	}
					break;
				case AttackAny:
					Ship targetShip = getNearestEnemyShip(ship, entities_by_dist, gameMap);
                	if(targetShip != null) {
                    	nTask = new Task(ship, gameMap, TaskType.AttackAny, targetShip);
                    	nTask.setObstructedPositions(myExpectedPositions);

                    	if(expectedPositions.containsKey(targetShip.getId())) {
                    		Position estPos = expectedPositions.get(targetShip.getId());
                    		nTask.setEstimatedPos(estPos);
                    		LinkedList<Position> shpath = shPathFinder.getPathToPos(ship, estPos, gameMap);
            				if(!shpath.isEmpty()) {
            					nTask.setPath(shpath);
            				}
            			} else {
            				LinkedList<Position> shpath = shPathFinder.getPathToPos(ship, targetShip, gameMap);
            				if(!shpath.isEmpty()) {
            					nTask.setPath(shpath);
            				}

            			}
                    	Move move = nTask.computeMove();
            			if(move != null) {
            				ThrustMove tm = (ThrustMove) move;
        					Position thisExpectedPos = tm.getExpectedPosition(ship);
        					//currentHitmap[expectedX][expectedY] = true;
        					myExpectedPositions.add(new Entity(-1, -1, thisExpectedPos.getXPos(), thisExpectedPos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR_S));
        					
            				moveList.add(move);
            				Log.log("Ship: " + ship.getId() + " setting move (new, "+ nTask.getType().toString() +" ). \n");
                        	newTasks.put(ship.getId(), nTask);
            			}
                	}
					break;
				case Expand:
					Planet targetEPlanet = findBestPlanet(ship, entities_by_dist, true, myId, targetedPlanets, range);
            		if(targetEPlanet != null) {
            			int tepid = targetEPlanet.getId();
            			if(targetedPlanets.containsKey(tepid)) {
                			targetedPlanets.put(tepid, targetedPlanets.get(tepid)+1);
            			} else {
                			targetedPlanets.put(tepid, 1);
            			}
                    	nTask = new Task(ship, gameMap, TaskType.Expand, targetEPlanet);
                    	nTask.setObstructedPositions(myExpectedPositions);

        				LinkedList<Position> shpath = shPathFinder.getPathToPlanet(ship, targetEPlanet);
        				if(!shpath.isEmpty()) {
        					nTask.setPath(shpath);
        				}

                    	Move move = nTask.computeMove();
            			if(move != null) {
            				if(move instanceof ThrustMove) {
                				ThrustMove tm = (ThrustMove) move;
            					Position thisExpectedPos = tm.getExpectedPosition(ship);
            					//currentHitmap[expectedX][expectedY] = true;
            					myExpectedPositions.add(new Entity(-1, -1, thisExpectedPos.getXPos(), thisExpectedPos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR_S));
            					
            				}
            				moveList.add(move);
            				Log.log("Ship: " + ship.getId() + " setting move (new, "+ nTask.getType().toString() +" ). \n");

                        	newTasks.put(ship.getId(), nTask);
            			}
                	}
					break;
				case Reinforce:
					Planet targetRPlanet = findBestPlanet(ship, entities_by_dist, false, myId, targetedPlanets, range);
            		if(targetRPlanet != null) {
            			int repid = targetRPlanet.getId();
            			if(targetedPlanets.containsKey(repid)) {
                			targetedPlanets.put(repid, targetedPlanets.get(repid)+1);
            			} else {
                			targetedPlanets.put(repid, 1);
            			}
            			
                    	nTask = new Task(ship, gameMap, TaskType.Reinforce, targetRPlanet);
                    	nTask.setObstructedPositions(myExpectedPositions);

        				LinkedList<Position> shpath = shPathFinder.getPathToPlanet(ship, targetRPlanet);
        				if(!shpath.isEmpty()) {
        					nTask.setPath(shpath);
        				}
                    	Move move = nTask.computeMove();
            			if(move != null) {
            				if(move instanceof ThrustMove) {
                				ThrustMove tm = (ThrustMove) move;
            					Position thisExpectedPos = tm.getExpectedPosition(ship);
            					//currentHitmap[expectedX][expectedY] = true;
            					myExpectedPositions.add(new Entity(-1, -1, thisExpectedPos.getXPos(), thisExpectedPos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR_S));
            					
            				}
            				moveList.add(move);
            				Log.log("Ship: " + ship.getId() + " setting move (new, "+ nTask.getType().toString() +" ). \n");

                        	newTasks.put(ship.getId(), nTask);
            			}
                	}
					break;
				case Dock:
					break;				
				case Conquer:
					Ship targetCShip = findBestEnemyShip(ship, entities_by_dist, gameMap, range);
                	if(targetCShip != null) {
                    	nTask = new Task(ship, gameMap, TaskType.AttackAny, targetCShip);
                    	nTask.setObstructedPositions(myExpectedPositions);

                    	if(expectedPositions.containsKey(targetCShip.getId())) {
                    		Position estPos = expectedPositions.get(targetCShip.getId());
                    		nTask.setEstimatedPos(estPos);
            				LinkedList<Position> shpath = shPathFinder.getPathToPos(ship, estPos, gameMap);
            				if(!shpath.isEmpty()) {
            					nTask.setPath(shpath);
            				}

            			} else {
            				LinkedList<Position> shpath = shPathFinder.getPathToPos(ship, targetCShip, gameMap);
            				if(!shpath.isEmpty()) {
            					nTask.setPath(shpath);
            				}

            			}
                    	Move move = nTask.computeMove();
            			if(move != null) {
                			ThrustMove tm = (ThrustMove) move;
            				Position thisExpectedPos = tm.getExpectedPosition(ship);
            				//currentHitmap[expectedX][expectedY] = true;
            				myExpectedPositions.add(new Entity(-1, -1, thisExpectedPos.getXPos(), thisExpectedPos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR_S));

            				moveList.add(move);
            				Log.log("Ship: " + ship.getId() + " setting move (new, "+ nTask.getType().toString() +" ). \n");
                        	newTasks.put(ship.getId(), nTask);
            			}
                	}
				case Defensive:
				default:
					break;
            	
            	}
            	

            }
            /*
            if(rounds < 10) {
        		Log.log("(checking newtasks list)");
        		int i = 0;
        		for(Map.Entry<Integer, Task> mapentry : newTasks.entrySet()) {
        			Log.log("entry: "+ i+ ": id:" + mapentry.getKey() + ", type: " + mapentry.getValue().getType().toString() + "\n");
        			i++;
        		}
        	}
        	*/
            tasks = newTasks;
//            rounds++;
            controller.initNextRound();
            Networking.sendMoves(moveList);
        }
    }
	/*
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
	}	*/
	
	static private Ship getNearestEnemyShip	(Ship thisShip, Map<Double,Entity> dist_sorted_entities, GameMap gameMap) {
		for(Map.Entry<Double,Entity> targetEntity : dist_sorted_entities.entrySet()) {
			  if(targetEntity.getValue() instanceof Ship) {
				  Ship targetShip = (Ship) targetEntity.getValue();
				  if(targetShip.getOwner() != gameMap.getMyPlayerId()) {
					  return targetShip;
				  }
			  }
		}
		return null;
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


	static private Planet findBestPlanet(Ship thisShip, Map<Double, Entity> dist_sorted_entities, boolean prioritizeUnowned, int myId, HashMap<Integer, Integer> targetedFreePlanets, double range){
		//String logstr;
		if(prioritizeUnowned) {
			//logstr = "Ship " + thisShip.getId() + ": (ufind)"; //debug

		} else {
			//logstr = "Ship " + thisShip.getId() + ": (find)"; //debug
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
				  double thisPlRadius = targetPlanet.getRadius();
				  
				  if(thisPlRadius > bestAnyPlanet.getRadius() * 1.1 && thisPlDist < min_a_range + 2*distanceUnit + range && targets <targetPlanet.getRemainingProduction()-1) {
					  bestAnyPlanet = targetPlanet;
					  //logstr += "=>(b=" + targetPlanet.getDistanceTo(thisShip) + "/" + targetPlanet.getRadius() +") -> pl:" + targetPlanet.getId() + " /// ";

				  }
				  
				  if(prioritizeUnowned && !targetPlanet.isOwned() && targets== 0) {
					  if(!unownedPlSet) {
						  bestUnownedPlanet = targetPlanet;
						  unownedPlSet = true;
						  min_u_range = Math.max(thisShip.getDistanceTo(targetPlanet), min_u_range);
						  //logstr += "u_range = " + min_a_range;
						  //logstr += " (n=" + targetPlanet.getDistanceTo(thisShip) + "/" + targetPlanet.getRadius() +") -> upl:" + targetPlanet.getId() + " /// ";
						  
					  } else {
						  
						  //equivalent computation as with bestAnyPlanet
						  if(thisPlRadius > bestUnownedPlanet.getRadius() * 1.1 && thisPlDist < min_u_range + 2*distanceUnit + range && targets <targetPlanet.getRemainingProduction()-1) {
							  bestUnownedPlanet = targetPlanet;
							  //logstr += "=>(b=" + targetPlanet.getDistanceTo(thisShip) + "/" + targetPlanet.getRadius() +") -> upl:" + targetPlanet.getId() + " /// ";
						  }
						  
					  }
				  }
				  
			}

		}
		
//		logstr += "\n";
		//Log.log(logstr);
		if(prioritizeUnowned && unownedPlSet) {
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
	

    static boolean[][] updateHitmap(boolean[][] hitmap, Map<Integer,Planet> previousPlanets, Map<Integer, Planet> currentPlanets, int numDestroyedPlanets, double fudge){    	
    	int planetsToRemove = numDestroyedPlanets;
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
		return updatedHitmap;

    }

    static ShadowPathFinder updateShadowPathFinder(ShadowPathFinder spf, Map<Integer,Planet> previousPlanets, Map<Integer, Planet> currentPlanets, int numDestroyedPlanets){    	
    	int planetsToRemove = numDestroyedPlanets;
		Log.log(" ##### updating shadowpathfinder ##### ");

            for(Map.Entry<Integer, Planet> prEntry : previousPlanets.entrySet()) {
    		if(!currentPlanets.containsKey(prEntry.getKey())) {
    		
    			planetsToRemove--;
    			Planet destroyedPlanet = prEntry.getValue();

    			spf.removePlanet(destroyedPlanet);
    			
    			if(planetsToRemove==0) {
    				return spf;
    			}
    		}

        }
		return spf;

    }
    
    static Position getFleePos(GameMap gameMap) {
    	int[] spaces = new int[4];
    	spaces[0] = 0; //hxhy
    	spaces[1] = 0; //hxly
    	spaces[2] = 0; //lxhy
    	spaces[3] = 0; //lxly

    	int myId = gameMap.getMyPlayerId();

    	Map<Integer, Planet> planets = gameMap.getAllPlanets();
    	for(Map.Entry<Integer, Planet> me : planets.entrySet()) {
    		Planet p = me.getValue();
    		int value = 0;
    		if(p.isOwned()) {
    			if(p.getOwner() == myId) {
    				value = 1;
    			} else {
    				value = -1;
    			}
    		}
    		
    		if(p.getXPos() > gameMap.getWidth()/2) {
    			if(p.getYPos() > gameMap.getHeight()/2) {
    				spaces[0] += value;
    			} else {
    				spaces[1] += value;
    			}
    		} else {
    			if(p.getYPos() > gameMap.getHeight()/2) {
    				spaces[2] += value;
    			} else {
    				spaces[3] += value;
    			}
    		}
    	}
    	
    	
    	Position[] corners = new Position[4];
    	corners[0] = new Position(gameMap.getWidth()-1, gameMap.getHeight()-1); //hxhy
    	corners[1] = new Position(gameMap.getWidth()-1, 1); //hxly
    	corners[2] = new Position(1, gameMap.getHeight()-1); //lxhy
    	corners[3] = new Position(1, 1); //lxly

    	
    	int highestId = 0;
    	int highestScore = 0;
    	int thisScore;
    	for(int i = 0; i < 4; i++) {
    		thisScore = spaces[i];

    		if(thisScore > highestScore) {
    			highestScore = thisScore;
    			highestId = i;
    		}
    	}
    	
    	return corners[highestId];
    	
    	
    }
    
    
    static Task createTask(Ship ship, GameMap gameMap, Control gameStatus) {
		return null;
    	
    }
	
    //computes a "local" priority for ships
    static Task getLocalPriority(Ship ship, GameMap gameMap, Control gameStatus) {
    	//TODO
		return null;
    	
    }
 


    
}

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import genAlgo.GAFileHandler;



public class ModifiedBot {
	
	public static final double PLANET_DOM_FACTOR = 0.2;
	public static final double SHIP_DOM_FACTOR = 0.6;
	public static final double MIN_DOM_VAL = 0.2;

	public static final double DIST_RANGE_FACTOR = 2.5;
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
    		//Log.log("attribute file: " + !fileNotFound);
    		if(!fileNotFound) {
    			attributes =  GAFileHandler.readBotAttsByName(botname);
            	//Log.log("Attribute file found and loaded.");
    		}

        }
        if(!usingBotID || fileNotFound) {
        	//Log.log("static ship distr");

	        for(int i = 0; i < attributes.length; i++) {
	        	attributes[i] = 0.1; //TODO CHANGE
	        }
	      //21 MyBot3
		    attributes[0] = 0.5803125976743176;
		    attributes[1] = 0.046003300352238506;
		    attributes[2] = 0.17665620421171335;
		    attributes[3] = 0.0364037598385752;
		    attributes[4] = 0.1606241379231553;
		         
		    
		    attributes[5] = 0.010796072792501105;
		    attributes[6] = 0.10754372226464769;
		    attributes[7] = 0.20395802689134113;
		    attributes[8] = 0.2586096123823068;
		    attributes[9] = 0.1291208823231525;

		    attributes[10] = 0.10123385897589084;
		    attributes[11] = 0.0863916292566951;
		    attributes[12] = 0.052303964173575336;
		    attributes[13] = 0.0058783615264336436;
		    attributes[14] = 0.025008782108161508;

		    attributes[15] = 0.019155087305294506;
		    attributes[16] = 0.6761910266983268;
		    attributes[17] = 0.2763319750287725;
		    attributes[18] = 0.16527308394326368;
		    attributes[19] = 0.31661848922604774;

		    attributes[20] = 0.6124013939277902;


        }
        
        
    
        double[] shipDistribution = new double[Control.NUM1ATTS];
        
        for(int i = 0; i < shipDistribution.length; i++) {
        	//Log.log("shipdistr("+i+"): " + Task.getTaskTypeByIndex(i).toString() + " : " + attributes[i]);
        	shipDistribution[i] = attributes[i];
        }
        
        
        double[] mapDifValues = new double[Control.NUM2ATTSIZE];
        
        for(int i = 0; i < mapDifValues.length; i++) {
        	//Log.log("mapDif("+i+"): " + MapDif.mapDifAttString(i) + " : " + attributes[i]);
        	mapDifValues[i] = attributes[i+Control.NUM1ATTS]-0.5;
        }
        
        double globalPrioThresh = attributes[Control.NUM2ATTS] * Control.GLOBAL_PRIO_FACTOR;
        double mapDifTaskChangeTime = attributes[Control.NUM2ATTS+1];
        double mapDifChangeFactor = attributes[Control.NUM2ATTS+2];
        double attDistUnitFactor = attributes[Control.NUM2ATTS+3];
        double targetSpecificPlayer = attributes[Control.NUM2ATTS+4]; 
       
        
        

        int numPlayers = gameMap.getAllPlayers().size();
        Map<Integer, Planet> lastKnownPlanets = gameMap.getAllPlanets();
        // We now have 1 full minute to analyse the initial map.
        final String initialMapIntelligence =
                "width: " + gmWidth +
                "; height: " + gmHeight +
                "; players: " + lastKnownPlanets.size() +
                "; planets: " + lastKnownPlanets.size();
        //Log.log(initialMapIntelligence);
        
        distanceUnit =(gmWidth + gmHeight)/2 * attDistUnitFactor; 
        double initRange = DIST_RANGE_FACTOR * distanceUnit * (1 / (double) numPlayers);
        //Log.log("distUnit = " + distanceUnit + ", range = " + initRange);
        

        ShadowPathFinder shPathFinder = new ShadowPathFinder(gameMap);
        double maxPlanetSize = shPathFinder.getMaxPlanetSize();
        
        final Control controller = new Control(myId, shipDistribution, mapDifTaskChangeTime, mapDifChangeFactor);
        controller.setGlobalDifThresh(globalPrioThresh);
        final LocalChecker localPrio = new LocalChecker(myId, initRange, shipDistribution[Task.getTaskTypeIndex(TaskType.Diversion)], maxPlanetSize, targetSpecificPlayer);

        final Evaluator evaluator = new Evaluator(gameMap, botname);
        evaluator.setIteration(currentIt);
        //Log.log("maxPlanetSize = " + maxPlanetSize);

        final MapDif mdif = new MapDif(gameMap);

        HashMap<Integer, Task> tasks = new HashMap<>();
        HashMap<Integer, Position> lastPositions = new HashMap<>();

             
        final ArrayList<Move> moveList = new ArrayList<>();

        
        for (;;) {
        	       	
        	moveList.clear();
            networking.updateMap(gameMap);
            mdif.update(gameMap, false);
            //HashSet<Integer> activeships = mdif.getMyActiveShips();
/*
            Iterator<Integer> taskIt = tasks.keySet().iterator();
            while (taskIt.hasNext()) {
            	Integer shipId = taskIt.next();
            	if (!activeships.contains(shipId)) {
            		taskIt.remove();
            	}
            }*/
            
            controller.initRound(tasks);

            controller.setDynPossibleTasks(gameMap);
            evaluator.evaluateRound(gameMap); //computes current score


            
        	// ALL_PLANETS  ALL_OWNEDPLANETS ALL_ENEMYPLANETS  ALL_SHIPS ALL_ENEMYSHIPS 
        	// OWNEDPLANETS_PERC MYSHIPS_PERC  MYPLANETS_PERC 

            
            //int myOwnedPlanetDif = mdif.getMyPlanetDif();
        	double myOwnedShipsPerc = mdif.getPerc(MapDif.MYSHIPS_PERC);
        	double myOwnedPlanetsPerc = mdif.getPerc(MapDif.MYPLANETS_PERC);
        	double myOwnedPlanetsPercDif = mdif.getPercDif(MapDif.MYPLANETS_PERC);
        	
        	//OWNED PLANETS OVER THRESHOLD: 
        	double opotThresh = 0;
        	if(mdif.percExceedThresh(MapDif.OWNEDPLANETS_PERC, mapDifValues[MapDif.OPOT_THRESH_V], true)) {
        		opotThresh = 1;
        	}

        	double attDif = mapDifValues[MapDif.MOP_CH_ATT] * myOwnedPlanetsPercDif
        			 	+	mapDifValues[MapDif.OPOT_CH_ATT] * opotThresh;
        	
            double conDif = mapDifValues[MapDif.MOP_CH_CON] * myOwnedPlanetsPercDif
            			+	mapDifValues[MapDif.OPOT_CH_CON] * opotThresh;
            
            double expDif = mapDifValues[MapDif.MOP_CH_EXP] * myOwnedPlanetsPercDif
       					+	mapDifValues[MapDif.OPOT_CH_EXP] * opotThresh;
            
            double reiDif = mapDifValues[MapDif.MOP_CH_REI] * myOwnedPlanetsPercDif
       			 		+	mapDifValues[MapDif.OPOT_CH_REI] * opotThresh;
            
            double divDif = mapDifValues[MapDif.MOP_CH_DIV] * myOwnedPlanetsPercDif
       			 		+	mapDifValues[MapDif.OPOT_CH_DIV] * opotThresh;
            
            controller.changeRatioField(TaskType.AttackAny, attDif);
            controller.changeRatioField(TaskType.Conquer, conDif);
            controller.changeRatioField(TaskType.Expand, expDif);
            controller.changeRatioField(TaskType.Reinforce, reiDif);
            controller.changeRatioField(TaskType.Diversion, divDif);
            
            int strongestPlID = mdif.getStrongestEnemyPlID();
            int weakestPlID = mdif.getWeakestEnemyPlID();
            
            // PLANET_DOM_FACTOR SHIP_DOM_FACTOR MIN_DOM_VAL
            //Log.log("myOwnedShipsperc = " + myOwnedShipsPerc + "myOwnedPlanetsPerc = " + myOwnedPlanetsPerc + "myOwnedPlanetsPercDif = " + myOwnedPlanetsPercDif + "opotThresh" + opotThresh);
            
            double dominationFactor = myOwnedPlanetsPerc * PLANET_DOM_FACTOR + myOwnedShipsPerc * SHIP_DOM_FACTOR + MIN_DOM_VAL; //should be slowly increasing, with planets_owned and ownedShips/allships
            double range = Math.min(gmWidth,  DIST_RANGE_FACTOR * distanceUnit * dominationFactor);
            localPrio.updateRange(range);
            
            //Log.log("domfactor = " + dominationFactor + ", range = " + range);
            
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
            
            HashMap<Integer, Integer> targetedPlanets = new HashMap<>(); //planets I want to go to
            Collection<Ship> shiplist = gameMap.getMyPlayer().getShips().values();
            ArrayList<Entity> myShipPositions = new ArrayList<>(shiplist.size()); //my ship positions that could collide with other ships
            ArrayList<Entity> diversionObstructions = new ArrayList<>(shiplist.size());	 //my ship positions and enemy ship positions
            ArrayList<Entity> obstructedPositions;
            
            for(Map.Entry<Integer, Position> mapentry : expectedPositions.entrySet()) {
            	Position pos = mapentry.getValue();
            	diversionObstructions.add(new Entity(-1, -1, pos.getXPos(), pos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR_DIV));
            }
            
            
//            int ship_id = 0;	
            //int ship_count = 0;

            
            for (final Ship ship : shiplist) {
            	//Log.log("debug: ship" + ship_count + ", id:" + ship.getId() + ", pos:" + ship.getXPos() +"|"+ ship.getYPos());
            	//ship_count++;
                Map<Double, Entity> entities_by_dist = gameMap.nearbyEntitiesByDistance(ship);

            	localPrio.compute(ship, entities_by_dist, targetedPlanets, gameMap, strongestPlID, weakestPlID);
            	obstructedPositions = localPrio.getEntitiesInRange();
            	for(Entity e: myShipPositions) {
            		if(ship.getDistanceTo(e) <= Constants.FLY_RANGE )
            		obstructedPositions.add(e);
            		diversionObstructions.add(e);
            	}
            	if(tasks.containsKey(ship.getId())) {
            		Task currentTask = tasks.get(ship.getId());
            		currentTask.update(gameMap, ship);
            		TaskStatus currentStatus = currentTask.getStatus();
            		if(currentStatus != TaskStatus.Invalid) {
            			

            			if(currentTask.isShipTargetType() && expectedPositions.containsKey(currentTask.getTargetId())) {
            				currentTask.setEstimatedPos(expectedPositions.get(currentTask.getTargetId()));
            			}
            			
            			if(currentTask.isDiversion()) {
            				
            				Ship newDivTarget = localPrio.getDivTarget();
            				if(newDivTarget != null) {
                				currentTask.updateTarget(localPrio.getDivTarget());
            				}
                			currentTask.setObstructedPositions(diversionObstructions);

            				currentTask.setFleePos(fleePos);

                			LinkedList<Position> fleePath = shPathFinder.getPathToPos(ship, fleePos, gameMap);
                			if(!fleePath.isEmpty()) {
                				currentTask.setFleePath(fleePath);
                			} else {
                			}

            			} else {
                			currentTask.setObstructedPositions(obstructedPositions);
            			}
            			
            			
            			
            			// USE PATHFINDER
            			if(currentTask.needsPath()) {
            				LinkedList<Position> shpath = shPathFinder.getPathToPos(ship, currentTask.getTarget(), gameMap);
            				if(!shpath.isEmpty()) {
                    			currentTask.setPath(shpath);
            				}
            			}
            			
            			Move move = currentTask.computeMove();
            			if(move != null) {
            				moveList.add(move);
            				if(move.getType() == MoveType.Thrust) { 	// SET OBSTRUCTED POSITIONS
            					ThrustMove tm = (ThrustMove) move;
            					Position thisExpectedPos = tm.getExpectedPosition(ship);
            					myShipPositions.add(new Entity(-1, -1, thisExpectedPos.getXPos(), thisExpectedPos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR_S));
            					diversionObstructions.add(new Entity(-1, -1, thisExpectedPos.getXPos(), thisExpectedPos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR_S));

            				}
                        	
            				if(currentTask.getType() != TaskType.Dock) {
                				controller.increaseShipNum(currentTask.getType());
            				}
                   
            				if(currentStatus == TaskStatus.WillDock) {
            					newTasks.put(ship.getId(), new Task(ship, gameMap, TaskType.Dock, currentTask.getTarget()));
            				} else {
                				newTasks.put(ship.getId(), currentTask);
            				}
                			continue;
            			}
            		} else {
        				controller.decreaseShipNum(currentTask.getType());
        				tasks.remove(ship.getId());
            		}
            	}
            	
            	
            	
            	// ########## COMPUTE NEW TASKS ###############

            	

            	
            	Task nTask;
            	Move move;
            	if(controller.isWithinGlobalDifThresh()) {
          
                	//Log.log("within global dif thresh -> local prio");

            		nTask = localPrio.getHighestTask();

                  	if(nTask.getTarget() == null) {
                	}
            		controller.increaseShipNum(nTask.getType());
            	} else {
            		
                	//Log.log("exceed global dif thresh -> global prio");

                	TaskType t = controller.getNextTypeAndUpdate();
                	//Log.log("wanted type: " + t.toString());

            		nTask = localPrio.getHighestTastPreferType(t);

                	if(nTask.getTarget() == null) {
                	}
            	}
            	//Log.log("Got new Task for " + ship.getId() + ", nTask = " + nTask.getType().toString());
            	Entity ntTarget = nTask.getTarget();
            	if(ntTarget instanceof Planet) { // EXPAND OR REINFORCE

            		int nttid = ntTarget.getId();
            		if(targetedPlanets.containsKey(nttid)) {
            			targetedPlanets.put(nttid, targetedPlanets.get(nttid)+1);
        			} else {
            			targetedPlanets.put(nttid, 1);
        			}
            		
    				LinkedList<Position> shpath = shPathFinder.getPathToPlanet(ship, (Planet) ntTarget);
    				if(!shpath.isEmpty()) {
    					nTask.setPath(shpath);
    				}
            	} else {  // ATTACKANY OR CONQUER OR DIVERSION
            		Ship tShip = (Ship) ntTarget;
                	tShip.getId();

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
            		if(nTask.getType() == TaskType.Diversion){
                    	nTask.setObstructedPositions(diversionObstructions);
            			nTask.setFleePos(fleePos);

            			LinkedList<Position> fleePath = shPathFinder.getPathToPos(ship, fleePos, gameMap);
            			if(!fleePath.isEmpty()) {
                			nTask.setFleePath(fleePath);
            			} else {

            			}
            		} else {
                    	nTask.setObstructedPositions(obstructedPositions);

            		}
            	}
            	//Log.log("MBmain: computeMove"); 

            	move = nTask.computeMove();
    			if(move != null) {
    				if(move instanceof ThrustMove) {
        				ThrustMove tm = (ThrustMove) move;
    					Position thisExpectedPos = tm.getExpectedPosition(ship);
    					//currentHitmap[expectedX][expectedY] = true;
    					myShipPositions.add(new Entity(-1, -1, thisExpectedPos.getXPos(), thisExpectedPos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR_S));
    					diversionObstructions.add(new Entity(-1, -1, thisExpectedPos.getXPos(), thisExpectedPos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR_S));
    				}

    				moveList.add(move);
    				//Log.log("Ship: " + ship.getId() + " setting move (new, "+ nTask.getType().toString() +" ). \n");
                	newTasks.put(ship.getId(), nTask);
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
            Networking.sendMoves(moveList);
        }
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
		//Log.log(" ##### creating hitmap ##### ");

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
		//Log.log(" ##### updating hitmap ##### ");

    
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
		//Log.log(" ##### updating shadowpathfinder ##### ");

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
    	//String fleePosScores = "fleepos scores: ";
    	for(int i = 0; i < 4; i++) {
    		// += spaces[i] + " ";
    		thisScore = spaces[i];

    		if(thisScore > highestScore) {
    			highestScore = thisScore;
    			highestId = i;
    		}
    	}
    	//Log.log(fleePosScores + " found corner: " + corners[highestId].toString());
    	return corners[highestId];
    	
    	
    }


    
}

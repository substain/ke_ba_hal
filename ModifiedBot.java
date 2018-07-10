import hlt.*;
import hlt.Move.MoveType;
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
	
	public static final double PLANET_DOM_FACTOR = 0.15;
	public static final double SHIP_DOM_FACTOR = 0.85;
	public static final double MIN_DOM_VAL = 0.2;

	public static final double DIST_RANGE_FACTOR = 2.5;
	public static final boolean useBotID = true;
	public static final int USE_PATHFINDING = 0;
	
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
        Log.log("Given Botname:" + botname);
        
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
        			Path fPath3 = Paths.get(dir.toString(), GAFileHandler.CFG_FOLDERNAME, GAFileHandler.PRESET_CFG_FOLDERNAME, botname + ".txt");
            		file = new File(fPath3.toString());
            		if(!file.exists()) {
            			fileNotFound = true;
            		}
        		}

    		}
    		if(!fileNotFound) {
        		Log.log("loading file..");
    			attributes =  GAFileHandler.readBotAttsByName(botname);
            	Log.log("Attribute file found and loaded.");
    		}

        }
        if(!usingBotID || fileNotFound) {
        	Log.log("using hardcoded ship distr");
        	
	        for(int i = 0; i < attributes.length; i++) {
	        	attributes[i] = 0.5;
	        }
	      // 34, tournament submission1
		    attributes[0] = 0.16128010237000992; //Distribution: ATTACKANY
		    attributes[1] = 0.21018824112627071; //Distribution: CONQUER
		    attributes[2] = 0.46385332518297434; //Distribution: EXPAND
		    attributes[3] = 0.12759510912733688; //Distribution: REINFORCE
		    attributes[4] = 0.03708322219340824; //Distribution: DIVERSION
		        
		    //these will change the global priority distribution
		    attributes[5] = 0.16690335406074644; //MapDif-ShipDom: EXPAND
		    attributes[6] = 0.13591316478026633; //MapDif-ShipDom: REINFORCE
		    attributes[7] = 0.12230542706490301; //MapDif-ShipDom: ATTACKANY
		    attributes[8] = 0.1400997689043241; //MapDif-ShipDom: CONQUER
		    attributes[9] = 0.054442438832794676; //MapDif-ShipDom: DIVERSION

		    attributes[10] = 0.07441911281969811; //MapDif-PlanetDomThresh: EXPAND
		    attributes[11] = 0.006962784240112823; //MapDif-PlanetDomThresh: REINFORCE
		    attributes[12] = 0.0800958169238448; //MapDif-PlanetDomThresh: ATTACKANY
		    attributes[13] = 0.022786133860076697; //MapDif-PlanetDomThresh: CONQUER
		    attributes[14] = 0.17731770491793555; //MapDif-PlanetDomThresh: DIVERSION

		    attributes[15] = 0.018754293595297507; //PlanetDomThresh-Value

		    //these are the weights used for local priority
		    attributes[16] = 0.5422803038206806; //conquer range factor
		    attributes[17] = 0.10470054470011947; //conquer myhealth factor
		    attributes[18] = 0.754302248169142; //conquer d planet size factor
		    attributes[19] = 0.1588282653750418; //attack range factor

		    attributes[20] = 0.3478014825992751; //attack myhealth factor
		    attributes[21] = 0.3864514726259606; //attack enemyhealth factor
		    attributes[22] = 0.9788798341609057; //reinforce range factor
		    attributes[23] = 0.7705577763951784; //reinforce planetsize factor
		    attributes[24] = 0.39680445807168296; //reinforce planet full
 
		    attributes[25] = 0.9337443865215; //expand range facctor
		    attributes[26] = 0.005591313971846645; //expand planet size factor
		    attributes[27] = 0.826340383829781; //expand planet targeted factor
		    
		    //and additional "weakness weights" for local priority used when the player is in a "weak", unfortunate or initial state
		    attributes[28] = 0.887538594366468; //localprio weakness factor: ATTACKANY
		    attributes[29] = 0.10523846700036599; //localprio weakness factor: CONQUER

		    attributes[30] = 0.9112920515179099; //localprio weakness factor: EXPAND
		    attributes[31] = 0.8837209063672257; //localprio weakness factor: REINFORCE
		    attributes[32] = 0.3832702474266323; //localprio weakness factor: DIVERSION
		     
		    
		    attributes[33] = 0.12946162607416778; //global prio thresh
		    attributes[34] = 0.4321168467813249; //map dif task change time
		    
		    attributes[35] = 0.6732232290439085; //mapDifChangeFactor
		    attributes[36] = 0.7391295026134085; //attDistUnitFactor
		    attributes[37] = 0.8389591290648059; //targetSpecificPlayer
		    attributes[38] = 0.6568180408881218; //dockTypeHealthThr
		    attributes[39] = 0.5321147616068685; //weaknessModThr used for the "weakness weights
		    attributes[40] = 0.6; //pathfinding type

        }
        
        Log.log(attrSemantics(attributes, botname));
    
        double[] shipDistribution = new double[Control.NUM1ATTS];

        for(int i = 0; i < shipDistribution.length; i++) {
        	//Log.log("shipdistr("+i+"): " + Task.getTasskTypeByIndex(i).toString() + " : " + attributes[i]);
        	shipDistribution[i] = attributes[i];
        }
        
        
        double[] mapDifValues = new double[Control.NUM2ATTSIZE];

        for(int i = 0; i < mapDifValues.length; i++) {
        	//Log.log("mapDif("+i+"): " + MapDif.mapDifAttString(i) + " : " + attributes[i]);
        	mapDifValues[i] = attributes[i+Control.NUM1ATTS]-0.5;
        }
    	
        double[] lcweights = new double[Control.NUM3ATTSIZE];
        for(int i = 0; i < lcweights.length; i++) {
        	lcweights[i] = attributes[i+Control.NUM2ATTS];
        }
        
        double[] wmweights = new double[Control.NUM4ATTSIZE];
        for(int i = 0; i < wmweights.length; i++) {
        	wmweights[i] = attributes[i+Control.NUM3ATTS];
        }
        
        
    	Log.log("reading rest bot attributes");
        //double globalPrioThresh = attributes[Control.NUM3ATTS] * Control.GLOBAL_PRIO_FACTOR;
        double globalPrioThresh = attributes[Control.NUM4ATTS];

        double mapDifTaskChangeTime = attributes[Control.NUM4ATTS+1];
        double mapDifChangeFactor = attributes[Control.NUM4ATTS+2];
        double attDistUnitFactor = attributes[Control.NUM4ATTS+3];

        double targetSpecificPlayer = attributes[Control.NUM4ATTS+4]; 

        double dockTypeHealthThr = attributes[Control.NUM4ATTS+5] * Constants.MAX_SHIP_HEALTH;
        double weaknessModThr = attributes[Control.NUM4ATTS+6];
        double pathfindingType = attributes[Control.NUM4ATTS+7];

    	Log.log("finished reading atts!");
    	boolean noPathfinding = true;
    	if(pathfindingType > 0.5) {
    		noPathfinding = false;
    	}


        int numPlayers = gameMap.getAllPlayers().size();
        Map<Integer, Planet> lastKnownPlanets = gameMap.getAllPlanets();
        // We now have 1 full minute to analyse the initial map.
        
        
        distanceUnit =(gmWidth + gmHeight)/2 * attDistUnitFactor; 
        double initRange = DIST_RANGE_FACTOR * distanceUnit * (1 / (double) numPlayers);
        //Log.log("distUnit = " + distanceUnit + ", range = " + initRange);
        

        ShadowPathFinder shPathFinder = new ShadowPathFinder(gameMap);
        double maxPlanetSize = shPathFinder.getMaxPlanetSize();
        
        final Control controller = new Control(myId, shipDistribution, mapDifTaskChangeTime, mapDifChangeFactor);
        controller.setGlobalDifThresh(globalPrioThresh);
        final LocalChecker localPrio = new LocalChecker(myId, initRange, shipDistribution[Task.getTaskTypeIndex(TaskType.Diversion)], maxPlanetSize, targetSpecificPlayer, lcweights, wmweights, weaknessModThr, noPathfinding);

        final Evaluator evaluator = new Evaluator(gameMap, botname);
        evaluator.setIteration(currentIt);
        //Log.log("maxPlanetSize = " + maxPlanetSize);

        final MapDif mdif = new MapDif(gameMap);

        HashMap<Integer, Task> tasks = new HashMap<>();
        HashMap<Integer, Position> lastPositions = new HashMap<>();

             
        final ArrayList<Move> moveList = new ArrayList<>();

    	Log.log("start computing ships");
    	
    	int round = 0;
    	
    	
    	Position anchorPos = null;
    	boolean anchorPosSet = false;
    	
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
            Log.log("myOwnedShipsperc = " + myOwnedShipsPerc + "myOwnedPlanetsPerc = " + myOwnedPlanetsPerc + "myOwnedPlanetsPercDif = " + myOwnedPlanetsPercDif + "opotThresh" + opotThresh);
            
            double dominationFactor = myOwnedPlanetsPerc * PLANET_DOM_FACTOR + myOwnedShipsPerc * SHIP_DOM_FACTOR; //should be slowly increasing, with planets_owned and ownedShips/allships
            double range = Math.min(gmWidth,  DIST_RANGE_FACTOR * distanceUnit * Math.min(dominationFactor,MIN_DOM_VAL));
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
            
            int remainingUnusedProd = getRemainingUsableProd(currentPlanets, myId);
            
            HashMap<Integer, Integer> targetedPlanets = new HashMap<>(); //planets I want to go to
            Collection<Ship> shiplist = gameMap.getMyPlayer().getShips().values();
            ArrayList<Entity> myShipPositions = new ArrayList<>(shiplist.size()); //my ship positions that could collide with other ships
            ArrayList<Entity> diversionObstructions = new ArrayList<>(shiplist.size());	 //my ship positions and enemy ship positions //NOT USED!!!!!!
            ArrayList<Entity> obstructedPositions;
            
            for(Map.Entry<Integer, Position> mapentry : expectedPositions.entrySet()) {
            	Position pos = mapentry.getValue();
            	//diversionObstructions.add(new Entity(-1, -1, pos.getXPos(), pos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR_DIV));
            }
            
            
//            int ship_id = 0;	
            //int ship_count = 0;
            localPrio.initRound(gameMap, strongestPlID, weakestPlID);

            Log.log("evaluator: saving dominationFactor:" + dominationFactor);
            evaluator.evaluateRoundDom(dominationFactor); //computes current score

            for (final Ship ship : shiplist) {
            	
                
                if(round == 0 && ship.getId() == 0) {
                	anchorPos = ship;
                	anchorPosSet = true;
                }
                
            	//Log.log("debug: ship" + ship_count + ", id:" + ship.getId() + ", pos:" + ship.getXPos() +"|"+ ship.getYPos());
            	//ship_count++;
                Map<Double, Entity> entities_by_dist = gameMap.nearbyEntitiesByDistance(ship);
            	localPrio.compute(ship, entities_by_dist, targetedPlanets);
            	obstructedPositions = localPrio.getEntitiesInRange();
            	for(Entity e: myShipPositions) {
            		if(ship.getDistanceTo(e) <= Constants.FLY_RANGE )
            		obstructedPositions.add(e);
            		//diversionObstructions.add(e);
            	}
            	if(tasks.containsKey(ship.getId())) {
            		Task currentTask = tasks.get(ship.getId());
            		currentTask.update(gameMap, ship);
            		TaskStatus currentStatus = currentTask.getStatus(dockTypeHealthThr, remainingUnusedProd);
            		if(currentStatus != TaskStatus.Invalid) {
            			
            			if(currentTask.getType() == Task.TaskType.Dock) {
                            if(!anchorPosSet) {
                            	anchorPos = ship;
                            	anchorPosSet = true;
                            }
            			}

            			if(currentTask.isShipTargetType() && expectedPositions.containsKey(currentTask.getTargetId())) {
            				currentTask.setEstimatedPos(expectedPositions.get(currentTask.getTargetId()));
            			}
            			
            			if(currentTask.isDiversion()) {
            				
            				Ship newDivTarget = localPrio.getDivTarget();
            				if(newDivTarget != null) {
                				currentTask.updateTarget(localPrio.getDivTarget());
            				}
            				currentTask.setAnchorPos(anchorPos);
                			currentTask.setObstructedPositions(obstructedPositions); //NOT USED: diversionObstructions

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
            					
            					myShipPositions.add(new Entity(-1, -1, thisExpectedPos.getXPos(),thisExpectedPos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR));
            					/*
            					int numParts = tm.getThrust();
            					double xPart = (thisExpectedPos.getXPos() - ship.getXPos())/numParts;
            					double yPart = (thisExpectedPos.getYPos() - ship.getYPos())/numParts;

            				
            					for(int i = 0; i < numParts; i++) {
                					myShipPositions.add(new Entity(-1, -1, ship.getXPos()+(xPart*i), ship.getYPos()+(yPart*i), 10, Constants.FORECAST_FUDGE_FACTOR));
                					//diversionObstructions.add(new Entity(-1, -1, ship.getXPos()+(xPart*i), thisExpectedPos.getYPos()+(yPart*i), 10, Constants.FORECAST_FUDGE_FACTOR2));
            					} 
            					*/
            				}
                        	
            				if(currentTask.getType() != TaskType.Dock) {
                				controller.increaseShipNum(currentTask.getType());
            				}
                   
            				if(currentStatus == TaskStatus.WillDock) {
            					newTasks.put(ship.getId(), new Task(ship, gameMap, TaskType.Dock, currentTask.getTarget(), noPathfinding));
            				} else {
                				newTasks.put(ship.getId(), currentTask);
            				}
                			continue;
            			}
            		} else {
            			
        				if(currentTask.getType() == TaskType.Dock) {
            				tasks.remove(ship.getId());
                        	Move move = new UndockMove(ship);

            				moveList.add(move);
            				continue;

        				}
            			
        				controller.decreaseShipNum(currentTask.getType());
        				tasks.remove(ship.getId());
        				
        				
            		}
            	}
            	
            	
            	
            	// ########## COMPUTE NEW TASKS ###############

            	Task nTask;
            	Move move;
            	if(controller.isWithinGlobalDifThresh(round)) {
          
                	//Log.log("within global dif thresh -> local prio");
            		if(ship.getHealth() <= (int) dockTypeHealthThr ) {
                		nTask = localPrio.getHighestDockTask();

             		} else {
                		nTask = localPrio.getHighestTask();
             			
             		}

                  	//if(nTask.getTarget() == null) {
                	//}
            		controller.increaseShipNum(nTask.getType());
            	} else {
            		
                	//Log.log("exceed global dif thresh -> global prio");
            		TaskType t;
            		if(ship.getHealth() <= (int) dockTypeHealthThr ) {
                    	t = controller.getNextDockTypeAndUpdate();

             		} else {
                    	t = controller.getNextTypeAndUpdate();
             			
             		}
                	//Log.log("wanted type: " + t.toString());

            		nTask = localPrio.getHighestTaskPreferType(t);

                	//if(nTask.getTarget() == null) {
                	//}
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
                    	nTask.setObstructedPositions(diversionObstructions); //NOT USED
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
    					myShipPositions.add(new Entity(-1, -1, thisExpectedPos.getXPos(), thisExpectedPos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR2));
    					//diversionObstructions.add(new Entity(-1, -1, thisExpectedPos.getXPos(), thisExpectedPos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR_S));
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
            anchorPosSet = false;
//            rounds++;
            Networking.sendMoves(moveList);
            round++;
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

    
    public static String attrSemantics(double[] attributes, String botname) {
    	String result = "Bot '"+botname+"' config:";
    
    	result += "Ship distribution (initial):";
        for(int i = 0; i < Control.NUM1ATTS; i++) {
			result += "\n";
			result +=  Task.getTaskTypeByIndex(i).toString() + ":"+ (attributes[i] * 100) + "%,";
        }
		result += "\n";
        result += "Map change effects:";
        for(int i = Control.NUM1ATTS; i < Control.NUM2ATTS; i++) {
			result += "\n";
			result += MapDif.mapDifAttString(i) + ":"+ (attributes[i+Control.NUM1ATTS]-0.5);
			/*
			switch(i) {
			//myOwnedPlanets/allOwnedPlanets
				case Control.NUM1ATTS+MapDif.MOP_CH_EXP:{
					result += " changes to expand ship distribution(caused by changes in myOwnedPlanets/allOwnedPlanets)";
					break;
				}
				case Control.NUM1ATTS+MapDif.MOP_CH_REI:{
					result += " changes to reinforce ship distribution(caused by changes in myOwnedPlanets/allOwnedPlanets)";

					break;
				}
				case Control.NUM1ATTS+MapDif.MOP_CH_ATT:{
					result += " changes to attack ship distribution(caused by changes in myOwnedPlanets/allOwnedPlanets)";

					break;
				}
				case Control.NUM1ATTS+MapDif.MOP_CH_CON:{
					result += " changes to conquer ship distribution(caused by changes in myOwnedPlanets/allOwnedPlanets)";

					break;
				}
				case Control.NUM1ATTS+MapDif.MOP_CH_DIV:{
					result += " changes to diversion ship distribution(caused by changes in myOwnedPlanets/allOwnedPlanets)";

					break;
				}
				
				//for changes when allOwnedPlanets/allPlanets is over a threshold
				case Control.NUM1ATTS+MapDif.OPOT_CH_EXP:{
					result += " changes to expand ship distribution(caused by crossing a threshold of allOwnedPlanets/allUnownedPlanets)";

					break;
				}
				case Control.NUM1ATTS+MapDif.OPOT_CH_REI:{
					result += " changes to reinforce ship distribution(caused by crossing a threshold of allOwnedPlanets/allUnownedPlanets)";

					break;
				}
				case Control.NUM1ATTS+MapDif.OPOT_CH_ATT:{
					result += " changes to attack ship distribution(caused by crossing a threshold of allOwnedPlanets/allUnownedPlanets)";

					break;
				}
				case Control.NUM1ATTS+MapDif.OPOT_CH_CON:{
					result += " changes to conquer ship distribution(caused by crossing a threshold of allOwnedPlanets/allUnownedPlanets)";

					break;
				}
				case Control.NUM1ATTS+MapDif.OPOT_CH_DIV:{
					result += " changes to diversion ship distribution(caused by crossing a threshold of allOwnedPlanets/allUnownedPlanets)";

					break;
				}
				
				//OPOT_THRESH_V
				case Control.NUM1ATTS+MapDif.OPOT_THRESH_V:{
					result += " threshold of allOwnedPlanets/allUnownedPlanets, will distribution changes";

					break;
				}
				default:{
					result += "?attribute error?";

					break;
				}
				*/
			  //}
        }
    	//Log.log("mapDif("+i+"): " + MapDif.mapDifAttString(i) + " : " + attributes[i]);

        result += "\n";
        result += "Weights for local changes are:";
        for(int i = Control.NUM2ATTS; i < Control.NUM3ATTS; i++) {
			result += "\n" + attributes[i];
			switch(i) {
				case Control.NUM2ATTS+LocalChecker.CON_RNG_FACTOR_I:{
					result += " : how much distance affects conquer priority";
					break;
				}
				case Control.NUM2ATTS+LocalChecker.CON_MYHEALTH_FACTOR_I:{
					result += " : how much shiphealth affects conquer priority";
					break;
				}
				case Control.NUM2ATTS+LocalChecker.CON_DP_SIZE_FACTOR_I:{
					result += " : how much planet radius affects conquer priority";

					break;
				}
				
				case Control.NUM2ATTS+LocalChecker.ATT_RNG_FACTOR_I:{
					result += " : how much distance affects attack priority";
					break;
				}
				case Control.NUM2ATTS+LocalChecker.ATT_MYHEALTH_FACTOR_I:{
					result += " : how much shiphealth affects attack priority";
					break;
				}
				case Control.NUM2ATTS+LocalChecker.ATT_ENEMHEALTH_FACTOR_I:{
					result += " : how much enemy shiphealth affects attack priority";

					break;
				}

				case Control.NUM2ATTS+LocalChecker.REI_RNG_FACTOR_I:{
					result += " : how much distance affects reinforce priority";
					break;
				}
				case Control.NUM2ATTS+LocalChecker.REI_PL_SIZE_FACTOR_I:{
					result += " : how much the planets size affects reinforce priority";
					break;
				}
				case Control.NUM2ATTS+LocalChecker.REI_PL_FULL_FACTOR_I:{
					result += " : how much the dock space remaining affects reinforce priority";
					break;
				}
				
				case Control.NUM2ATTS+LocalChecker.EXP_RNG_FACTOR_I:{
					result += " : how much distance affects expand priority";
					break;
				}
				case Control.NUM2ATTS+LocalChecker.EXP_PL_SIZE_FACTOR_I:{
					result += " : how much the planets size affects expand priority";
					break;
				}
				case Control.NUM2ATTS+LocalChecker.EXP_PL_TARGETED_FACTOR_I:{
					result += " : how much the amount of other own ships targetting a planet affects expand priority";

					break;
				}
				default:{
					result += "?attribute error?";

					break;
				}
			}
				
			  //}
			
        }
        
        result += "\n";
        result += "Weights for weakness changes are:";
        for(int i = Control.NUM3ATTS; i < Control.NUM4ATTS; i++) {
			result += "\n";
			result +=  Task.getTaskTypeByIndex((i-Control.NUM3ATTS)).toString() + ":"+ (attributes[i] * 100) + "%,";
        }
        
        final int unspecAttStart = Control.NUM4ATTS;

		result += "\n Unspecified Attributes:";
        if(attributes.length != Control.NUM4ATTS+Control.NUM_UNSPEC_ATTS) {
        	result += "-- WARNING -- numUnspecAttEnd("+(unspecAttStart+Control.NUM_UNSPEC_ATTS)+") != attributes.length("+attributes.length+")\n";
        }
        for(int i = unspecAttStart; i < attributes.length; i++) {
			result += "\n";
			result += attributes[i];
        	switch(i) {
				case unspecAttStart:{ //globalPrioThresh
					result += " threshold to use global prio instead of local";
	
					break;
				}
				case (unspecAttStart+1):{ //mapDifTaskChangeTime
					result += " time used to apply changes in the distribution";
	
					break;
				}
				case unspecAttStart+2:{ //mapDifChangeFactor
					result += " MapDif change factor";
	
					break;
				}
				case unspecAttStart+3:{ //attDistUnitFactor
					result += " distance factor";
	
					break;
				}
				case unspecAttStart+4:{ //targetSpecificPlayer
					if(attributes[i] > 0.5) {
					result += " greater than 0.5, prioritize stronger players";
					}
					else if(attributes[i] > 0.5) {
							result += " lower than 0.5, prioritize weaker players";
					}
	
	
					break;
				}
				case unspecAttStart+5:{ //dockTypeHealthThr
					result += " threshold for ships to stop attacking";
					break;
				}
				case unspecAttStart+6:{ //weaknessModThr
					result += " threshold of how weak is okay";
					break;
				}
				case unspecAttStart+7:{ //pathfindingType
					if(attributes[i] > 0.5) {
					result += " greater than 0.5, use ShadowPathFinding";
					}
					else if(attributes[i] > 0.5) {
							result += " lower than 0.5, use initial Pathfinding";
					}
					break;
				}
				default:{
					result += "?attribute error?";
        		}
        	}
        	
        }
        /*
        double[] mapDifValues = new double[Control.NUM2ATTSIZE];
        for(int i = 0; i < mapDifValues.length; i++) {
        	mapDifValues[i] = attributes[i+Control.NUM1ATTS]-0.5;
        }
    	
        double[] weights = new double[LocalChecker.NUM_LC_WEIGHTS];
        for(int i = 0; i < weights.length; i++) {
        	weights[i] = attributes[i+Control.NUM1ATTS+Control.NUM2ATTS];
        }
    	
        double globalPrioThresh = attributes[Control.NUM3ATTS] * Control.GLOBAL_PRIO_FACTOR;
        double mapDifTaskChangeTime = attributes[Control.NUM3ATTS+1];
        double mapDifChangeFactor = attributes[Control.NUM3ATTS+2];
        double attDistUnitFactor = attributes[Control.NUM3ATTS+3];
        double targetSpecificPlayer = attributes[Control.NUM3ATTS+4]; 
       //        double dockTypeHealthThr = attributes[Control.NUM3ATTS+5] * Constants.MAX_SHIP_HEALTH;

    	
    	*/
    	return result;
    }
    
    public static int getRemainingUsableProd(Map<Integer, Planet> planets, int myId) {

    	int sumProduction = 0;
    	
    	
        for(Map.Entry<Integer, Planet> entry : planets.entrySet()) {
        	Planet planet = entry.getValue();
        	if(!planet.isOwned() || planet.getOwner() == myId) {
        		sumProduction += planet.getRemainingProduction();
        	}
        }
    	
		return sumProduction;
    	
    }

    
}

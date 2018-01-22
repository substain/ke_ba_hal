package hlt;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

import hlt.Ship.DockingStatus;

public class Task {
	
	public static final int NUM_ACTIVE_TYPES = 5;
	public enum TaskStatus { Valid, WillDock, Invalid }
    public enum TaskType { AttackAny, Defensive, Conquer, Diversion, Expand, Reinforce, Dock;
    	

    	  @Override
    	  public String toString() {
    	    switch(this) {
    	      case AttackAny: return "[attack]";
    	      case Defensive: return "[defensive]"; //unused
    	      case Conquer: return "[conquer]";
    	      case Diversion: return "[diversion]";
    	      case Expand: return "[expand]";
    	      case Reinforce: return "[reinforce]";
    	      case Dock: return "[dock]";
    	      default: throw new IllegalArgumentException();
    	    }
    	  }
    	  
    }
    /*
     * The 2 basic types are Attack and Production, the other types may be more advanced tactics.
     * 
     * Attack: Attack any enemy ship
     * Defensive: Attack ships targeting docked ships (Advanced)
     * Conquer: Attack docked ships (Advanced)
     * Diversion: Try to divert other ships (Advanced)
     * Production: Dock to any planet
     * Expand: Dock to an unowned planet
     * Reinforce: Dock to an owned planet
     */

    private Ship thisShip;
    private Entity target;
	private Position estimatedPos;
	private Position fleePos;
	private TaskType type;
	private GameMap gameMap;
	private LinkedList<Position> path;
	private LinkedList<Position> fleePath;
	private boolean needsPath;
	private boolean needsFleePath;

	private ArrayList<Entity> obstructedPositions;

	public Task(Ship ship, GameMap gmap, TaskType ttype, Entity ttarget) {
		obstructedPositions = new ArrayList<Entity>();
		thisShip = ship;
		target = ttarget;
		type = ttype;
		gameMap = gmap;
		needsPath = true;
		needsFleePath = true;
	}
	
	public boolean needsPath() {
		return needsPath;
	}
	
	public void setEstimatedPos(Position estimatedPosition) {
			estimatedPos = estimatedPosition;
	}
	public void setFleePos(Position fleePosition) {
		fleePos = fleePosition;
	}
	
	/*
	 * computes the current move according to the task and resets estimatedPos to null
	 */
	public Move computeMove() {
		int speed = Constants.MAX_SPEED;

		Move move = null;

		switch(type) {
		case Diversion: //TODO: dont crash into walls/planets
			if(estimatedPos != null) {
				obstructedPositions.add(new Entity(-1, -1, estimatedPos.getXPos(), estimatedPos.getYPos(), 10, Constants.SHIP_RADIUS));
			}
			Ship tShip = (Ship) target;
			double safetyZone = Constants.WEAPON_RADIUS * 2;
			double midRangeZone = Constants.WEAPON_RADIUS * 3;
			double distToShip = thisShip.getDistanceTo(target);
			Position estPos;
			if(estimatedPos != null) {
				estPos = estimatedPos;
			} else {
				estPos = tShip;
			}
			if(distToShip > safetyZone && thisShip.getDistanceTo(estPos) > safetyZone) {
				if(distToShip <= midRangeZone) {
					speed -= 1;
				}
				if(!needsPath) {
					//Log.log("Diversion: hasPath, towardsTarget");
					move = Navigation.navigateShipTowardsPathTarget(gameMap, thisShip, tShip, speed, path.getFirst(),obstructedPositions);
				} else {
					//Log.log("Diversion: noPath, towardsTarget");

					move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, tShip, speed, obstructedPositions);
				}
			} else {
				if(!needsFleePath) {
					//Log.log("Diversion: hasFleePath, away");

					move = Navigation.navigateShipTowardsPathTarget(gameMap, thisShip, fleePos, speed, fleePath.getFirst(),obstructedPositions);
				} else {
					//Log.log("Diversion: noFleePath, away");
					move = Navigation.navigateShipToPoint(gameMap, thisShip, fleePos, speed, obstructedPositions);
				}
			}
			//TODO
			break;

		case Conquer:
			Ship ctargetShip = (Ship) target;
			double distToCTarget = thisShip.getDistanceTo(ctargetShip);

			if(distToCTarget <= Constants.WEAPON_RADIUS) {
				speed = (int)((Constants.WEAPON_RADIUS)-distToCTarget);		
			}
			if(!needsPath) {
				move = Navigation.navigateShipTowardsPathTarget(gameMap, thisShip, ctargetShip, speed, path.getFirst(),obstructedPositions);
			} else {
				move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, ctargetShip, speed, obstructedPositions);
			}
			break;

		case AttackAny:
			Ship targetShip = (Ship) target;
			double distToTarget = thisShip.getDistanceTo(targetShip);
			if(estimatedPos != null) {
				obstructedPositions.add(new Entity(-1, -1, estimatedPos.getXPos(), estimatedPos.getYPos(), 10, Constants.SHIP_RADIUS));
			}
			if(distToTarget <= Constants.WEAPON_RADIUS + 1) {
				speed = (int)((Constants.WEAPON_RADIUS+1)-distToTarget);
			}
			if(estimatedPos == null || distToTarget > 20) { //no position given
				//Log.log("computeMove: navigation to " + targetShip.getXPos() + "|" + targetShip.getYPos() + " with speed " + speed + ", expected pos = " + Navigation.getExpectedPos(thisShip, targetShip, speed));
				if(!needsPath) {
					move = Navigation.navigateShipTowardsPathTarget(gameMap, thisShip, targetShip, speed, path.getFirst(), obstructedPositions);
				} else {
					move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetShip, speed, obstructedPositions);
				}
			} else { //move to the estimated position
				if(distToTarget <= Constants.WEAPON_RADIUS + 3) {
					speed -= 2;
				}
				//Log.log("computeMove: navigation to " + estimatedPos.getXPos() + "|" + estimatedPos.getYPos() + " with speed " + speed + ", expected pos = " + Navigation.getExpectedPos(thisShip, targetShip, speed));
				obstructedPositions.add(new Entity(-1, -1, estimatedPos.getXPos(), estimatedPos.getYPos(), 10, Constants.FORECAST_FUDGE_FACTOR));
				move = Navigation.navigateShipToPoint(gameMap, thisShip, estimatedPos, speed, obstructedPositions);
			}
			break;
		case Expand:
		case Reinforce:
			
			Planet targetPlanet = (Planet) target;
			//Log.log("Task.Move:Production, distance to planet: " + thisShip.getDistanceTo(targetPlanet));
			if (thisShip.canDock(targetPlanet)) {
				move = new DockMove(thisShip, targetPlanet);
			} else {;
				if(thisShip.getDistanceTo(targetPlanet) <= Constants.MAX_BREAK_DISTANCE) {
					speed = speed-1;
				}
				//Log.log("computeMove: navigation to " + targetPlanet.getXPos() + "|" + targetPlanet.getYPos() + " with speed " + speed + ", expected pos = " + Navigation.getExpectedPos(thisShip, targetPlanet, speed));
				if(!needsPath) {
					move = Navigation.navigateShipTowardsPathTarget(gameMap, thisShip, targetPlanet, speed, path.getFirst(), obstructedPositions);
				} else {
					move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetPlanet, speed, obstructedPositions);
				}
			}
			break;
		case Dock:
			Planet tPlanet = (Planet) target;
			if (thisShip.canDock(tPlanet)) {
				move = new DockMove(thisShip, tPlanet);
			} else {
				speed = speed -1;
				move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, tPlanet, speed, obstructedPositions);
			}
			break;
		default:
			break;
		}
		estimatedPos = null;
		return move;
	}
	
	public Position getDestPos() {
		return target;
	}
	
	//public boolean isNoTargetType() { //also means target is of type ship
	//	if(type == TaskType.Diversion) {
	//		return true;
	//	}
	//	return false;
	//}
	
	public boolean isShipTargetType() { //also means target is of type ship
		if(type == TaskType.AttackAny || type == TaskType.Diversion || type == TaskType.Conquer) {
			return true;
		}
		return false;
	}
	
	public boolean isDockType() { //also means target is of type planet
		if(type == TaskType.Expand || type == TaskType.Reinforce || type == TaskType.Dock) {
			return true;
		}
		return false;
	}
	
	public int getTargetId() {
		return target.getId();
	}
	
	public Entity getTarget() {
		return target;
	}
	
	public void setPath(LinkedList<Position> newPath) {
		path = newPath;
		needsPath = false;
	}

	
	public void setFleePath(LinkedList<Position> fPath) {
		fleePath = fPath;
		needsFleePath = false;
	}
	
	public void updateTarget(Ship t) { //for Diversion, should always be the next non-docking ship
		target = t;
	}
	
	public void update(GameMap gameMap, Ship currentShip) {
		
		thisShip = currentShip;

		if(isShipTargetType()) {
			//Log.log("updating Task, target ship id = " + target.getId());

			target = gameMap.findShip(target.getId());
		} else if(isDockType()) {
			target = gameMap.getPlanet(target.getId());
		} else { //Distraction
			for(Map.Entry<Double, Entity> mapentry : gameMap.nearbyEntitiesByDistance(thisShip).entrySet()) {
				if(mapentry.getValue() instanceof Ship) {
					Ship s = (Ship) mapentry.getValue(); //ignore non-moving ships
					if(s.getDockingStatus() == DockingStatus.Docked || s.getDockingStatus() == DockingStatus.Docking || s.getOwner() == gameMap.getMyPlayerId()) {
						continue;
					}
					target = s;
				}
			}
		}
		//Log.log("updating Task, rest assigned");

		
	}
	
	public TaskStatus getStatus() {
		//check if the current Task is valid (target planet is still a planet to dock on etc.)
		
		//!isNoTargetType() && 
		if(target == null) {
			//Log.log("Task:Invalid ->Target does not exist");
			return TaskStatus.Invalid;
		}
		
		if(type == TaskType.Diversion) {
			Ship targetS = (Ship) target; 
			if(targetS.getDockingStatus() == DockingStatus.Docked || targetS.getDockingStatus() == DockingStatus.Docking)
				return TaskStatus.Invalid;
		}

		if(isDockType()){
			int myId = gameMap.getMyPlayerId();
			
			
			if((target.getOwner() != myId && target.getOwner() != -1) || ((Planet)target).isFull()){
				//Log.log("Task:Invalid -> target planet is not dockable");
				return TaskStatus.Invalid;
			}
			if (type == TaskType.Dock) {
				if(thisShip.getDockingStatus() != DockingStatus.Docked && thisShip.getDockingStatus() != DockingStatus.Docking) {
					return TaskStatus.Valid;
				} else if(!thisShip.canDock((Planet)target)) {
					//Log.log("Task:Invalid -> ship can't dock this planet");
					return TaskStatus.Invalid;
				}
			}
			if(type == TaskType.Expand && target.getOwner() != -1) {
				//Log.log("Task:Invalid -> (expand) planet is not unowned");

				return TaskStatus.Invalid;
			} else if(type == TaskType.Reinforce && target.getOwner() != myId) {
				//Log.log("Task:Invalid -> (reinforce) planet is not owned by this player");

				return TaskStatus.Invalid;
			}
			if (thisShip.canDock((Planet)target)) {
				//Log.log("Task:WillDock -> ship can dock this planet");

				return TaskStatus.WillDock;
			}

		}
		return TaskStatus.Valid;
	}
	
	public TaskType getType() {
		return type;
	}
	
	public void setObstructedPositions(ArrayList<Entity> myExpectedPositions) {
		//Log.log("Task.setObstructedPositions:");
		//for(Entity ent : myExpectedPositions) {
			//Log.log("E: " + ent.getXPos() + "/" + ent.getYPos() + ", r=" + ent.getRadius());
		//}
		obstructedPositions = myExpectedPositions;
	}

	public boolean isDiversion() {
		if(type == TaskType.Diversion) {
			return true;
		}
		return false;
	}


	public static int getTaskTypeIndex(TaskType taskt) {
		switch(taskt) {
		case AttackAny:
			return 0;
		case Conquer:
			return 1;
		case Expand:
			return 2;
		case Reinforce:
			return 3;
		case Diversion:
			return 4;
		default: // 
			return -1;
		}
	}
	
	public static TaskType getTaskTypeByIndex(int i) {
		switch(i) {
		case 0:
			return TaskType.AttackAny;
		case 1:
			return TaskType.Conquer;
		case 2:
			return TaskType.Expand;
		case 3:
			return TaskType.Reinforce;
		case 4: 
			return TaskType.Diversion;
		default:
			return TaskType.Dock;
		}
	}

	public static boolean isControlTask(TaskType tt) {
		if(getTaskTypeIndex(tt) == -1) {
			return false;
		}
		return true;
	}
}

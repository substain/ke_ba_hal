package hlt;

import java.util.LinkedList;
import java.util.Map;

import hlt.Ship.DockingStatus;

public class Task {
	
	public static final int NUM_ACTIVE_TYPES = 6;
	
	public enum TaskStatus { Valid, WillDock, Invalid }
    public enum TaskType { AttackAny, Defensive, Conquer, Diversion, Expand, Reinforce, Dock;
    	

    	  @Override
    	  public String toString() {
    	    switch(this) {
    	      case AttackAny: return "[attack]";
    	      case Defensive: return "[defensive]"; //unused
    	      case Conquer: return "[conquer]";
    	      case Diversion: return "[diversion]"; //unused
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

    Ship thisShip;
	Entity target;
	Position estimatedPos;
	TaskType type;
	GameMap gameMap;
	LinkedList<Position> path;
	boolean needsPath;
	
	public Task(Ship ship, GameMap gmap, TaskType ttype, Entity ttarget) {
		thisShip = ship;
		target = ttarget;
		type = ttype;
		gameMap = gmap;
		needsPath = true;

	}
	
	public boolean needsPath() {
		return needsPath;
	}
	
	public void setEstimatedPos(Position estimatedPosition) {
		estimatedPos = estimatedPosition;
	}
	
	
	/*
	 * computes the current move according to the task and resets estimatedPos to null
	 */
	public Move computeMove() {
		int speed = Constants.MAX_SPEED;

		Move move = null;
		switch(type) {
		case Diversion: //TODO: dont crash into walls/planets
			Ship tShip = (Ship) target;
			double safetyZone = Constants.WEAPON_RADIUS * 3;
			double midRangeZone = Constants.WEAPON_RADIUS * 5;
			double distToShip = thisShip.getDistanceTo(target);
			boolean noEstimation = false;
			Position estPos;
			if(estimatedPos == null) {
				estPos = estimatedPos;
				noEstimation = true;;
			} else {
				estPos = tShip;
			}
			if(distToShip > safetyZone && thisShip.getDistanceTo(estPos) > safetyZone) {
				if(distToShip > midRangeZone) {
					speed -= 2;
				}
				move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, tShip, speed);

			} else {
				Position targetPos;
				if(noEstimation) {
					targetPos = Position.getOppositePos(thisShip, tShip);
				} else {
					/* TODO:  angle higher than angleToMe: turn right, else turn left
					double angle = tShip.orientTowardsInRad(estimatedPos);
					double angleToMe = tShip.orientTowardsInRad(thisShip);
					if(angle > angleToMe && angle - angleToMe < 180 && angle - angleToMe > 0) {
						this.
					}*/
					targetPos = Position.getOppositePos(thisShip, tShip);

					
				}
				move = Navigation.navigateShipToPoint(gameMap, thisShip, targetPos, speed);

			}
			//TODO
			break;
		case Defensive:
			//TODO
			//break;
		case Conquer:
			Ship ctargetShip = (Ship) target;

			if(thisShip.getDistanceTo(ctargetShip) <= Constants.WEAPON_RADIUS + 3) {
				speed -= 2;
			}
			move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, ctargetShip, speed);
		case AttackAny:
			Ship targetShip = (Ship) target;
			if(thisShip.getDistanceTo(targetShip) <= Constants.WEAPON_RADIUS + 3) {
				speed -= 2;
			}
			if(estimatedPos == null) { //no position given
				//Log.log("computeMove: navigation to " + targetShip.getXPos() + "|" + targetShip.getYPos() + " with speed " + speed + ", expected pos = " + Navigation.getExpectedPos(thisShip, targetShip, speed));
				move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetShip, speed);
			} else { //move to the estimated position
				if(thisShip.getDistanceTo(targetShip) <= Constants.WEAPON_RADIUS + 5) {
					speed -= 1;
				}
				//Log.log("computeMove: navigation to " + estimatedPos.getXPos() + "|" + estimatedPos.getYPos() + " with speed " + speed + ", expected pos = " + Navigation.getExpectedPos(thisShip, targetShip, speed));
				move = Navigation.navigateShipToPoint(gameMap, thisShip, estimatedPos, speed);
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
					speed = speed-4;
				}
				//Log.log("computeMove: navigation to " + targetPlanet.getXPos() + "|" + targetPlanet.getYPos() + " with speed " + speed + ", expected pos = " + Navigation.getExpectedPos(thisShip, targetPlanet, speed));
				move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetPlanet, speed);
			}
			break;
		case Dock:
			Planet tPlanet = (Planet) target;
			if (thisShip.canDock(tPlanet)) {
				move = new DockMove(thisShip, tPlanet);
			} else {
				speed = speed -3;
				move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, tPlanet, speed);
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
	
	public boolean isAttackType() { //also means target is of type ship
		if(type == TaskType.AttackAny || type == TaskType.Defensive || type == TaskType.Conquer) {
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
	
	public void updateTarget(Ship t) { //for Diversion, should always be the next non-docking ship
		target = t;
	}
	
	public void update(GameMap gameMap, Ship currentShip) {
		
		thisShip = currentShip;

		if(isAttackType()) {
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
	
	public Position getExpectedPos() {
		//TODO
		return null;
	}
	
	public void setObstructedPositions() {
		
	}
	/*
	public boolean isEqualTo(Task compTask) {
		//TODO
		//check if the current Task equals compTask

		return false;
	}
	
	public boolean doesNotInterfereWith(Task compTask) {
		//TODO ?
		//check if the current objective does not 
		return true;
	}
	*/

	
}

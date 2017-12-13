package hlt;

import hlt.Ship.DockingStatus;

public class Task {
	
	public enum TaskStatus { Valid, WillDock, Invalid }
    public enum TaskType { Attack, Defensive, Conquer, Diversion, Production, Expand, Reinforce, Dock }
    /*
     * The 2 basic types are Attack and Production, the other types are more advanced tactics.
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
	
	public Task(Ship ship, GameMap gmap, TaskType ttype, Entity ttarget) {
		thisShip = ship;
		target = ttarget;
		type = ttype;
		gameMap = gmap;
	}
	
	
	public void setEstimatedPos(Position estimatedPosition) {
		estimatedPos = estimatedPosition;
	}
	
	/*
	 * computes the current move according to the task and resets estimatedPos to null
	 */
	public Move computeMove() {
		//TODO
		Move move = null;
		switch(type) {
		case Diversion:
			//TODO
			//break;
		case Defensive:
			//TODO
			//break;
		case Conquer:
			//TODO
			//break;
		case Attack:
			Ship targetShip = (Ship) target;

			if(estimatedPos == null) { //no position given
				if(thisShip.getDistanceTo(targetShip) <= Constants.WEAPON_RADIUS + 1) {
					  move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetShip, Constants.MAX_SPEED/2);
				  } else {
					  move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetShip, (int)(Constants.MAX_SPEED * 0.9));
				  }
			} else { //move to the estimated position
				if(thisShip.getDistanceTo(targetShip) <= Constants.WEAPON_RADIUS + 3) {
					move = Navigation.navigateShipToPoint(gameMap, thisShip, estimatedPos, Constants.MAX_SPEED/2);
				} else {
					move = Navigation.navigateShipToPoint(gameMap, thisShip, estimatedPos, Constants.MAX_SPEED);
				}
			}
			break;
		case Expand:
		case Reinforce:
		case Production:
			Planet targetPlanet = (Planet) target;
			if (thisShip.canDock(targetPlanet)) {
				move = new DockMove(thisShip, targetPlanet);
			} else {
				move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetPlanet, Constants.MAX_SPEED);
			}
			break;
		case Dock:
			Planet tPlanet = (Planet) target;
			if (thisShip.canDock(tPlanet)) {
				move = new DockMove(thisShip, tPlanet);
			} else {
				move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, tPlanet, Constants.MAX_SPEED);
			}
			break;
		default:
			break;
		}
		estimatedPos = null;
		return move;
	}
	
	public boolean isAttackType() { //also means target is of type ship
		if(type == TaskType.Attack || type == TaskType.Defensive || type == TaskType.Conquer) {
			return true;
		}
		return false;
	}
	
	public boolean isDockType() { //also means target is of type planet
		if(type == TaskType.Production || type == TaskType.Expand || type == TaskType.Reinforce || type == TaskType.Dock) {
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
	
	public TaskStatus checkStatus() {
		//check if the current Task is valid (target planet is still a planet to dock on etc.)

		if(isAttackType()) {
			if(!gameMap.getAllShips().contains(target)) {
				return TaskStatus.Invalid;
			}
		} else if(isDockType()){
			int myId = gameMap.getMyPlayerId();

			if (type == TaskType.Dock) {
				if(thisShip.getDockingStatus() == DockingStatus.Docked || thisShip.getDockingStatus() == DockingStatus.Docking) {
					return TaskStatus.Valid;
				}
				if(!thisShip.canDock((Planet)target)) {
					return TaskStatus.Invalid;
				}
			}
			if(!gameMap.getAllPlanets().containsValue(target) || (target.getOwner() != myId && target.getOwner() != -1) || ((Planet)target).isFull()){
				return TaskStatus.Invalid;
			}
			if(type == TaskType.Expand && target.getOwner() != -1) {
				return TaskStatus.Invalid;
			} else if(type == TaskType.Reinforce && target.getOwner() != myId) {
				return TaskStatus.Invalid;
			}
			if (thisShip.canDock((Planet)target)) {
				return TaskStatus.WillDock;
			}

		}
		return TaskStatus.Valid;
	}
	
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

	
}

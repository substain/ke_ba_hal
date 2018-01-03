package hlt;

import hlt.Ship.DockingStatus;

public class Task {
	
	public enum TaskStatus { Valid, WillDock, Invalid }
    public enum TaskType { Attack, Defensive, Conquer, Diversion, Production, Expand, Reinforce, Dock;
    	

    	  @Override
    	  public String toString() {
    	    switch(this) {
    	      case Attack: return "[attack]";
    	      case Defensive: return "[defensive]";
    	      case Conquer: return "[conquer]";
    	      case Diversion: return "[diversion]";
    	      case Production: return "[production]";
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
		int speed = Constants.MAX_SPEED;

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
			if(thisShip.getDistanceTo(targetShip) <= Constants.WEAPON_RADIUS + 3) {
				speed -= 3;
			}
			if(estimatedPos == null) { //no position given
				Log.log("computeMove: navigation to " + targetShip.getXPos() + "|" + targetShip.getYPos() + " with speed " + speed + ", expected pos = " + Navigation.getExpectedPos(thisShip, targetShip, speed));
				move = Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetShip, speed);
			} else { //move to the estimated position
				if(thisShip.getDistanceTo(targetShip) <= Constants.WEAPON_RADIUS + 5) {
					speed -= 1;
				}
				Log.log("computeMove: navigation to " + estimatedPos.getXPos() + "|" + estimatedPos.getYPos() + " with speed " + speed + ", expected pos = " + Navigation.getExpectedPos(thisShip, targetShip, speed));
				move = Navigation.navigateShipToPoint(gameMap, thisShip, estimatedPos, speed);
			}
			break;
		case Expand:
		case Reinforce:
		case Production:
			Planet targetPlanet = (Planet) target;
			Log.log("Task.Move:Production, distance to planet: " + thisShip.getDistanceTo(targetPlanet));
			if (thisShip.canDock(targetPlanet)) {
				move = new DockMove(thisShip, targetPlanet);
			} else {;
				if(thisShip.getDistanceTo(targetPlanet) <= Constants.MAX_BREAK_DISTANCE) {
					speed = speed-4;
				}
				Log.log("computeMove: navigation to " + targetPlanet.getXPos() + "|" + targetPlanet.getYPos() + " with speed " + speed + ", expected pos = " + Navigation.getExpectedPos(thisShip, targetPlanet, speed));
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
	public boolean isNoTargetType() { //also means target is of type ship
		if(type == TaskType.Diversion) {
			return true;
		}
		return false;
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
	
	public void update(GameMap gameMap, Ship currentShip) {
		
		thisShip = currentShip;

		if(isAttackType()) {
			Log.log("updating Task, target ship id = " + target.getId());

			target = gameMap.findShip(target.getId());
		} else if(isDockType()) {
			target = gameMap.getPlanet(target.getId());
		} else {
			//TODO
		}
		Log.log("updating Task, rest assigned");

		
	}
	
	public TaskStatus getStatus() {
		//check if the current Task is valid (target planet is still a planet to dock on etc.)
		
		if(!isNoTargetType() && target == null) {
			Log.log("Task:Invalid ->Target does not exist");
			return TaskStatus.Invalid;
		}

		if(isDockType()){
			int myId = gameMap.getMyPlayerId();

			
			if((target.getOwner() != myId && target.getOwner() != -1) || ((Planet)target).isFull()){
				Log.log("Task:Invalid -> target planet is not dockable");
				return TaskStatus.Invalid;
			}
			if (type == TaskType.Dock) {
				if(thisShip.getDockingStatus() != DockingStatus.Docked && thisShip.getDockingStatus() != DockingStatus.Docking) {
					return TaskStatus.Valid;
				} else if(!thisShip.canDock((Planet)target)) {
					Log.log("Task:Invalid -> ship can't dock this planet");
					return TaskStatus.Invalid;
				}
			}
			if(type == TaskType.Expand && target.getOwner() != -1) {
				Log.log("Task:Invalid -> (expand) planet is not unowned");

				return TaskStatus.Invalid;
			} else if(type == TaskType.Reinforce && target.getOwner() != myId) {
				Log.log("Task:Invalid -> (reinforce) planet is not owned by this player");

				return TaskStatus.Invalid;
			}
			if (thisShip.canDock((Planet)target)) {
				Log.log("Task:WillDock -> ship can dock this planet");

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

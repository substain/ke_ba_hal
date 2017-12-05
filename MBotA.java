import hlt.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class MBotA {

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("MBot_A");

        // We now have 1 full minute to analyse the initial map.
        final String initialMapIntelligence =
                "width: " + gameMap.getWidth() +
                "; height: " + gameMap.getHeight() +
                "; players: " + gameMap.getAllPlayers().size() +
                "; planets: " + gameMap.getAllPlanets().size();
        Log.log(initialMapIntelligence);

        final ArrayList<Move> moveList = new ArrayList<>();
        for (;;) {
            moveList.clear();
            networking.updateMap(gameMap);

            int id = 0; //count free ships, give them ids to specify more individual behavior
            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
            	id++;
                Map<Double, Entity> entities_by_dist = gameMap.nearbyEntitiesByDistance(ship);
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                	id--; //do not use this ship 
                    continue;
                }
                
                //not very sophisticated: once a ship is destroyed or docks, the order and the behavior of an individual ship will probably change
                //2 out of 5 ships will attack (or all of the rest, if the amount of ships reaches a specific threshold
                if((id % 5) > 2 || id > (gameMap.getAllPlanets().size() * 2)) {
                	Move move = offensiveBehavior(ship, entities_by_dist, gameMap);
                	if(move != null) {
                		moveList.add(move);
                	}
                } else if((id % 5) >= 1) { //1 out of 5 ships will go to free planets (if possible)
                	Move move = exploringBehavior(ship, entities_by_dist, gameMap);
                	if(move != null) {
                		moveList.add(move);
                	}
                } else { //the rest will go to the nearest planet and dock
                	Move move = defensiveBehavior(ship, entities_by_dist, gameMap);
                	if(move != null) {
                		moveList.add(move);
                	}
                }

            }
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
						  return Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetShip, Constants.MAX_SPEED/3);
					  } else {
						  return Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetShip, (int)(Constants.MAX_SPEED * 0.7));
					  }
				  }
			  }
		}

		return null;
	}
	
	static private Move exploringBehavior(Ship thisShip, Map<Double, Entity> dist_sorted_entities, GameMap gameMap) {
		for(Map.Entry<Double,Entity> targetEntity : dist_sorted_entities.entrySet()) {
			  if(targetEntity.getValue() instanceof Planet) {
				  Planet targetPlanet = (Planet) targetEntity.getValue();
				  if(!targetPlanet.isOwned()) {      
					  if (thisShip.canDock(targetPlanet)) {
		                return new DockMove(thisShip, targetPlanet);
					  } else {
						return Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetPlanet, Constants.MAX_SPEED/2);
					  }
				  }
			  }
		}
		
		return defensiveBehavior(thisShip, dist_sorted_entities, gameMap);
	}
	
	static private Move defensiveBehavior(Ship thisShip, Map<Double, Entity> dist_sorted_entities, GameMap gameMap) {
		for(Map.Entry<Double,Entity> targetEntity : dist_sorted_entities.entrySet()) {
			  if(targetEntity.getValue() instanceof Planet) {
				  Planet targetPlanet = (Planet) targetEntity.getValue();
				  if(targetPlanet.getOwner() == gameMap.getMyPlayerId() || !targetPlanet.isOwned()) { //is dockable            
					  if (thisShip.canDock(targetPlanet)) {
		                return new DockMove(thisShip, targetPlanet);
					  } else {
						return Navigation.navigateShipToClosestPoint(gameMap, thisShip, targetPlanet, Constants.MAX_SPEED/2);
					  }
				  }
			  }
		}
		
		//no free planet
		return offensiveBehavior(thisShip, dist_sorted_entities, gameMap);
	}
}

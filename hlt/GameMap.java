package hlt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import hlt.Ship.DockingStatus;

import java.util.Collections;
import java.util.Collection;

public class GameMap {
    private final int width, height;
    private final int playerId;
    private final List<Player> players;
    private final List<Player> playersUnmodifiable;
    private final Map<Integer, Planet> planets;
    private final List<Ship> allShips;
    private final List<Ship> allShipsUnmodifiable;

    // used only during parsing to reduce memory allocations
    private final List<Ship> currentShips = new ArrayList<>();

    public GameMap(final int width, final int height, final int playerId) {
        this.width = width;
        this.height = height;
        this.playerId = playerId;
        players = new ArrayList<>(Constants.MAX_PLAYERS);
        playersUnmodifiable = Collections.unmodifiableList(players);
        planets = new TreeMap<>();
        allShips = new ArrayList<>();
        allShipsUnmodifiable = Collections.unmodifiableList(allShips);
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getMyPlayerId() {
        return playerId;
    }

    public List<Player> getAllPlayers() {
        return playersUnmodifiable;
    }

    public Player getMyPlayer() {
        return getAllPlayers().get(getMyPlayerId());
    }

    public Ship getShip(final int playerId, final int entityId) throws IndexOutOfBoundsException {
        return players.get(playerId).getShip(entityId);
    }
    
    /**
     *@return null if the ship could not be found
     */
    public Ship findShip( final int entityId) {
    	for(Ship s : allShips) {
    		if(s.getId() == entityId) {
    			return s;
    		}
    	}
    	return null;
    }
    
    /*

    public Planet findPlanet( final int entityId) {
    	for(Planet p : planets) {
    		if(p.getId() == entityId) {
    			return p;
    		}
    	}
    	return null;
    }*/
    
    public Planet getPlanet(final int entityId) {
        return planets.get(entityId);
    }

    public Map<Integer, Planet> getAllPlanets() {
        return planets;
    }

    public List<Ship> getAllShips() {
        return allShipsUnmodifiable;
    }

    public ArrayList<Entity> objectsBetween(Position start, Position target, boolean tryShips, ArrayList<Entity> obstructedPos, int myID) {
        final ArrayList<Entity> entitiesFound = new ArrayList<>();

        
        addEntitiesBetween(entitiesFound, start, target, planets.values(), true, myID);

        if(!obstructedPos.isEmpty() || obstructedPos == null) {
        	//Log.log("obstructed Pos not empty!");
            addEntitiesBetween(entitiesFound, start, target, obstructedPos, true, myID);
        } else {
        	//Log.log("obstructed Pos is empty!");
        }
        
        if(tryShips) {
            addEntitiesBetween(entitiesFound, start, target, allShips, true, myID);
        }
        
		for(Entity ent : entitiesFound) {
			String objType = "Entity?=";
			if (ent instanceof Planet) {
				objType = "Planet=";
			}
			if (ent instanceof Ship) {
				objType = "Ship=";
			}
			//Log.log("found obj betw: " + objType + ent.getXPos() + "/" + ent.getYPos() + ", r=" + ent.getRadius());





		}
        return entitiesFound;
    }
   


    private static void addEntitiesBetween(final List<Entity> entitiesFound,
                                           final Position start, final Position target,
                                           final Collection<? extends Entity> entitiesToCheck, boolean targetIsPosition, int myId) {

        for (final Entity entity : entitiesToCheck) {
        	
            if (entity.equals(start) || (entity.equals(target)&&!targetIsPosition)) {
                continue;
            }
            /*
			if (entity instanceof Ship) {
				Ship obsShip = (Ship) entity;
				if(obsShip.getId() == myId && obsShip.getDockingStatus() == DockingStatus.Undocked) {
					continue;
				}
			} */
            if (Collision.segmentCircleIntersect(start, target, entity, Constants.FORECAST_FUDGE_FACTOR)) {
                entitiesFound.add(entity);
            }
        }
    }

    public Map<Double, Entity> nearbyEntitiesByDistance(final Entity entity) {
        final Map<Double, Entity> entityByDistance = new TreeMap<>();

        for (final Planet planet : planets.values()) {
            if (planet.equals(entity)) {
                continue;
            }
            entityByDistance.put(entity.getDistanceTo(planet), planet);
        }

        for (final Ship ship : allShips) {
            if (ship.equals(entity)) {
                continue;
            }
            entityByDistance.put(entity.getDistanceTo(ship), ship);
        }

        return entityByDistance;
    }
    
    public Planet getNearestPlanet(final Position pos) {
        Planet closestPlanet = null;
        double closestDist = Double.POSITIVE_INFINITY;
        double currentDist;
        for (final Planet planet : planets.values()) {
        	currentDist = pos.getDistanceTo(planet);
        	if(currentDist <= closestDist) {
        		closestDist = currentDist;
        		closestPlanet = planet;
        	}
        }
        return closestPlanet;
    }

    public GameMap updateMap(final Metadata mapMetadata) {
        final int numberOfPlayers = MetadataParser.parsePlayerNum(mapMetadata);

        players.clear();
        planets.clear();
        allShips.clear();

        // update players info
        for (int i = 0; i < numberOfPlayers; ++i) {
            currentShips.clear();
            final Map<Integer, Ship> currentPlayerShips = new TreeMap<>();
            final int playerId = MetadataParser.parsePlayerId(mapMetadata);

            final Player currentPlayer = new Player(playerId, currentPlayerShips);
            MetadataParser.populateShipList(currentShips, playerId, mapMetadata);
            allShips.addAll(currentShips);

            for (final Ship ship : currentShips) {
                currentPlayerShips.put(ship.getId(), ship);
            }
            players.add(currentPlayer);
        }

        final int numberOfPlanets = Integer.parseInt(mapMetadata.pop());

        for (int i = 0; i < numberOfPlanets; ++i) {
            final List<Integer> dockedShips = new ArrayList<>();
            final Planet planet = MetadataParser.newPlanetFromMetadata(dockedShips, mapMetadata);
            planets.put(planet.getId(), planet);
        }

        if (!mapMetadata.isEmpty()) {
            throw new IllegalStateException("Failed to parse data from Halite game engine. Please contact maintainers.");
        }

        return this;
    }
}

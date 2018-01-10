package hlt;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

public class ShadowPathFinder {
	
	HashMap<Integer, ShadowTreeNode> shadowMap;
	
	
	public ShadowPathFinder(GameMap gmap) {
		shadowMap = new HashMap<>();
		parsePlanets(gmap);
		Log.log("parsing planets complete");

	}
	
	public void parsePlanets(GameMap gmap) {
		Map<Integer, Planet> allPlanets = gmap.getAllPlanets();
		for(Map.Entry<Integer,Planet> sourceEntry : allPlanets.entrySet()) {
			Planet sourcePlanet = sourceEntry.getValue();
			//create ShadowTreeNode
			ShadowTreeNode stnRoot = new ShadowTreeNode(sourcePlanet);
			//and fill this node
			Map<Double, Entity> nearestPlanets = gmap.nearbyEntitiesByDistance(sourcePlanet);
			for(Entry<Double, Entity> destEntry : nearestPlanets.entrySet()) {
				if(destEntry.getValue() instanceof Planet) {
					//Log.log("parsePlanets: s:"+sourceEntry.getKey()+" t("+destEntry.getValue().getId()+"):" + destEntry.getKey());
					Planet destPlanet = (Planet) destEntry.getValue();
					stnRoot.insert(destPlanet);
				}
				
			}
			
			shadowMap.put(sourcePlanet.getId(), stnRoot);
			
		}
	}

	public void removePlanet(Planet pl) {
		Log.log("debug:SPF: calling removePlanet");
		shadowMap.keySet().removeAll(Collections.singleton(pl.getId()));
		for(Map.Entry<Integer, ShadowTreeNode> mapEntry : shadowMap.entrySet()) {
			mapEntry.getValue().remove(pl);
		}	
	}
	
	public LinkedList<Position> getPathToPlanet(Position fromPos, Planet toPlanet) {
		Log.log("debug:SPF: calling getPathToPlanet");

		ShadowTreeNode stnRoot = shadowMap.get(toPlanet.getId());
		return stnRoot.getPathFromPosition(fromPos);
	}
	

	public LinkedList<Position> getPathToPos(Position fromPos, Position toPos, GameMap gmap) {
		Log.log("debug:SPF: calling getPathToPos");

		return getPathToPlanet(fromPos, gmap.getNearestPlanet(toPos));
	}
	
	public String planetSTNtoString(Planet planet) {

		String retstr = "ShadowTree of Pl " + planet.getId() + ": \n";

		ShadowTreeNode stnRoot = shadowMap.get(planet.getId());
		retstr += stnRoot.getString(0);
		
		return retstr;
	}
	
	public String allPlanetsToString() {
		String retstr = "";
		
		for(Map.Entry<Integer, ShadowTreeNode> mapEntry : shadowMap.entrySet()) {
			retstr += "ShadowTree of Pl " + mapEntry.getKey() + ": \n";
			retstr += mapEntry.getValue().getString(0);
			retstr += "\n \n";
		}			
		return retstr;
	}
	

}

package hlt;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

public class ShadowPathFinder {
	
	HashMap<Integer, ShadowTreeNode> shadowMap;
	double maxPlanetSize;
	
	public ShadowPathFinder(GameMap gmap) {
		shadowMap = new HashMap<>();
		parsePlanets(gmap);
		//Log.log("parsing planets complete");
	}
	
	public double getMaxPlanetSize() {
		return maxPlanetSize;
	}
	
	private void parsePlanets(GameMap gmap) {
		maxPlanetSize = 0;
		
		Map<Integer, Planet> allPlanets = gmap.getAllPlanets();
		for(Map.Entry<Integer,Planet> sourceEntry : allPlanets.entrySet()) {
			Planet sourcePlanet = sourceEntry.getValue();
			
			if(sourcePlanet.getRadius() > maxPlanetSize) {
				maxPlanetSize = sourcePlanet.getRadius();
			}
			
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

		ShadowTreeNode stnRoot = shadowMap.get(toPlanet.getId());
		LinkedList<Position> ret = stnRoot.getPathFromPosition(fromPos);
		if(ret == null) {
			ret = new LinkedList<>();
		}

		return ret;
	}
	

	public LinkedList<Position> getPathToPos(Position fromPos, Position toPos, GameMap gmap) {
		
		LinkedList<Position> res = getPathToPlanet(fromPos, gmap.getNearestPlanet(toPos));

		if(res == null) {
			res = new LinkedList<>();
		}

		return res;
		
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

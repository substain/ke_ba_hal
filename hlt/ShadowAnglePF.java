package hlt;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

public class ShadowAnglePF {
	LinkedList<LinkedList<ShadowAngle>> shadowAnglesForPlanets;
	
	
	public ShadowAnglePF(GameMap gmap) {
		shadowAnglesForPlanets = new LinkedList<>();
		//parsePlanets(gmap);
		Log.log("parsing planets complete");
	}
	
	/*
	public void removePlanet(Planet pl) {
		Log.log("debug:SPF: calling removePlanet");
		shadowMap.keySet().removeAll(Collections.singleton(pl.getId()));
		for(Map.Entry<Integer, ShadowTreeNode> mapEntry : shadowMap.entrySet()) {
			mapEntry.getValue().remove(pl);
		}	
	} */
	
/*
	public LinkedList<Position> getPathToPos(Position fromPos, Position toPos, GameMap gmap) {
		Log.log("debug:SPF: calling getPathToPos");

		return getPathToPlanet(fromPos, gmap.getNearestPlanet(toPos));
		
	}*/
	
}

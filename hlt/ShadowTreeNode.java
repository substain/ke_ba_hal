package hlt;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ShadowTreeNode {
	
	
	TreeMap<Double, ShadowTreeNode> children;
	boolean isLeaf;
	
	Position leftEdge;
	double leftAngle; //higher angle
	
	double leftMaxAngle; //the maximum higher angle that can be found (including children range);

	Position rightEdge;
	double rightAngle; //lower angle
	
	double rightMinAngle; //the minimum lower angle that can be found (including children range);

	
	double dist;
	
	Position sourcePlanet;
	
	int targetPlanetId; //debug
	
	public ShadowTreeNode(Position source) {  // creates the root
		leftEdge = null;
		rightEdge = null;
		leftAngle = 360;
		rightAngle = 0;
		leftMaxAngle = leftAngle;
		rightMinAngle = rightAngle;

		children = new TreeMap<>();
		sourcePlanet = source;
		
	}

	
	public ShadowTreeNode(Position source, Planet target) {
		isLeaf = true;
		children = new TreeMap<>();
		sourcePlanet = source;
    	targetPlanetId = target.getId();


        final double angularStepRad = Math.PI/180.0;
        dist = source.getDistanceTo(target);
        final double stRad = source.orientTowardsInRad(target);
        
        final double rfdist = target.getRadius() + Constants.FORECAST_FUDGE_FACTOR;
        final double tLERad = stRad + 90; //orientation from target center to left "edge"
        
        final double LEdx = Math.cos(tLERad + angularStepRad) * rfdist;
        final double LEdy = Math.sin(tLERad + angularStepRad) * rfdist;
        
        final double LExpos = target.getXPos() + LEdx;
        final double LEypos = target.getYPos() + LEdy;
        
        leftEdge = new Position (LExpos, LEypos);

        // right edge is in the opposite direction from the center of the planet
        final double RExpos = target.getXPos() - LEdx;
        final double REypos = target.getYPos() - LEdy;
        
    	rightEdge = new Position (RExpos, REypos);
    	
    	leftAngle = source.orientTowardsInRad(leftEdge);
    	rightAngle = source.orientTowardsInRad(rightEdge);
		leftMaxAngle = leftAngle;
		rightMinAngle = rightAngle;
	}

	public boolean isLeaf() {
		return isLeaf;
	}
	
	public void insert(Planet pl) {
		ShadowTreeNode stn = new ShadowTreeNode(sourcePlanet, pl);
		insertSTN(stn);
	}
	
	
	//ONLY WORKS FOR PLANETS WITH pl.dist > this.dist
	public void insertSTN(ShadowTreeNode plSTN) {
		//Log.log("debug:STN: calling insert");
		if(leftMaxAngle < plSTN.getLeftAngle()) {
			leftMaxAngle = plSTN.getLeftAngle();
		}
		if(rightMinAngle > plSTN.getRightAngle()) {
			rightMinAngle = plSTN.getRightAngle();
		}
		Double targetChildKey = findKey(getMidAngle());
		if(targetChildKey != null) {
			children.get(targetChildKey).insertSTN(plSTN);
		}else {
			insertChild(plSTN);
		}
	}




	protected void insertChild(ShadowTreeNode stn) {
		//Log.log("debug:STN: calling insertChild");

		isLeaf = false;
		children.put(stn.getRightAngle(), stn);
	}
	
	public boolean remove(Planet pl) {
		Log.log("debug:STN: calling remove");

		ShadowTreeNode stn = findSTNByPos(pl);
		if(stn == null) {
			return removeChild(pl);
		} else {
			return stn.removeChild(pl);
		}
	}
	
	
	protected boolean removeChild(Planet pl) {
		Log.log("debug:STN: calling removeChild");

		double targetAngle = sourcePlanet.orientTowardsInDeg(pl);
		Double key = findKey(targetAngle);
		
		if(key != null) {
			children.remove(key);
			return true;
		}
		return false;
	}

	

	public ShadowTreeNode findSTNByPos(Position target) { //find the shadow in which the position is
		Log.log("debug:STN: calling findSTNByPos");

		return getRecSTN(sourcePlanet.orientTowardsInRad(target), sourcePlanet.getDistanceTo(target));
	}

	
	protected ShadowTreeNode getRecSTN(double angle, double distance) { 
		//Log.log("debug:STN: calling getRecSTN");

		if(!angleInRange(angle) || dist >= distance) {
			return null;
		}
		
		Double currentKey = findKey(angle);
		if(currentKey == null) {
			return this;
		}
		ShadowTreeNode child = children.get(currentKey);
		ShadowTreeNode childResult = child.getRecSTN(angle, distance);
		if(childResult == null) {
			return this;
		} else return childResult;
		
	}
	
	
	public LinkedList<Position> getPathFromPosition(Position p){
		Log.log("debug:STN: calling getPathFromPosition");
		
		LinkedList<Position> path = new LinkedList<>();
		
		double targetAngle = sourcePlanet.orientTowardsInDeg(p);
		double targetDist = sourcePlanet.orientTowardsInDeg(p);

		
		return getPathFromPositionRec(targetAngle, targetDist, path);
	}
	
	public LinkedList<Position> getPathFromPositionRec(double angle, double dist, LinkedList<Position> addedPositions){
		Log.log("debug:STN: calling getPathFromPositionRec");

		Double key = findKey(angle);
		
		if(key != null) {
			addedPositions = children.get(key).getPathFromPositionRec(angle, dist, addedPositions);
		}
		

		if(sourcePlanet.orientTowardsInDeg(addedPositions.getLast()) > getMidAngle()) {
				addedPositions.add(leftEdge);
		} else {
				addedPositions.add(rightEdge);
		}
		
		return addedPositions;
	}

	private Double findKey(double angle) {
		//Log.log("debug:STN: calling findKey");

		Set<Double> keyAngles = children.keySet(); //sorted in ascending order
		Iterator<Double> kit = keyAngles.iterator();
		Iterator<Double> nkit = keyAngles.iterator(); //for peeking elements - lighter solution than creating an additional array to iterate through
		double currentKey;
		double nextKey;
		double closestKey;
		
		if(nkit.hasNext()) {
			nextKey = nkit.next();
			closestKey = nextKey;
		} else {
			return null;
		}
		
		
		while(kit.hasNext()) {
			currentKey = kit.next();
			if(nkit.hasNext()) {
				nextKey = nkit.next();
				
				if(nextKey > angle) {
					closestKey = currentKey;
					break;

				}
			} else {
				closestKey = currentKey;
			}
		}
		
		if(angleInSubTreeRange(closestKey)) {
			return closestKey;
		} else {
			return null;
		}
	}
	
	public boolean angleInRange(double angle) {
		//Log.log("debug:STN: calling angleInRange");

		if(angle > leftAngle || angle < rightAngle) { //TODO
			return false; //angle out of range
		}
		return true;
	}
	
	public boolean angleInSubTreeRange(double angle) {
		//Log.log("debug:STN: calling angleInRange");

		if(angle > leftMaxAngle || angle < rightMinAngle) { //TODO
			return false; //angle out of range
		}
		return true;
	}
	
	
	public double getRightAngle() {
		return rightAngle;
	}
	
	public double getLeftAngle() {
		return leftAngle;
	}
	
	private double getDist() {
		return dist;
	}

	protected double getMidAngle() {
		return (leftAngle + rightAngle)/2;
	}
	
	public String getString(int depth) {
		String ret = "";
		
		for(int i = 0; i < depth; i++) {
			ret += " ";
		}
		
		ret+= ("STN (" + targetPlanetId + "):"+rightMinAngle +"°("+rightAngle+"°)-"+leftMaxAngle+"°("+leftAngle+"°), d=" +dist);
		if(isLeaf) {
			 return ret;
		}
		ret += ", ch: ";
		
		switch(depth%4) {
			case 0:{
				ret+= "(";
				break;
			}
			case 1:{
				ret+= "[";
				break;
			}
			case 2:{
				ret+= "{";
				break;
			}
			default:{
				ret+= "/";
			}
		}
		
		
		for(Map.Entry<Double, ShadowTreeNode> stnEntry : children.entrySet()) {
			ret += "\n";
			ret += stnEntry.getValue().getString(depth+1);
			
		}
		
		switch(depth%4) {
			case 0:{
				ret+= ")";
				break;
			}
			case 1:{
				ret+= "]";
				break;
			}
			case 2:{
				ret+= "}";
				break;
			}
		default:{
				ret+= "\\";
			}
		}
			
		return ret;
		
	}
	
}

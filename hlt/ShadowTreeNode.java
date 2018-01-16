package hlt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ShadowTreeNode {

	public static final double TWO_PI = Math.PI * 2;
	public static final double HALF_PI = Math.PI * 2;

	TreeMap<Double, ShadowTreeNode> children;
	boolean isLeaf;
	boolean isRoot;
	
	
	
	
	Position leftEdge;
	double leftAngle; //higher angle
	double leftMaxAngle; //the maximum higher angle that can be found (including children range);
	boolean overZ;
	boolean maxOverZ;

	Position rightEdge;
	double rightAngle; //lower angle
	double rightMinAngle; //the minimum lower angle that can be found (including children range);

	
	double dist;
	
	Position sourcePos;
	Planet sourcePlanet;
	
	int targetPlanetId; //debug
	private double midAngle;
	
	public ShadowTreeNode(Planet source) {  // creates the root
		leftEdge = null;
		rightEdge = null;
		leftAngle = 2 * Math.PI;
		rightAngle = 0;
		isRoot = true;;
		leftMaxAngle = leftAngle;
		rightMinAngle = rightAngle;
		computeMidAngle();

		overZ = false;
		maxOverZ = false;


		children = new TreeMap<>();
		sourcePos = source;
		sourcePlanet = source;
		
	}

	
	public ShadowTreeNode(Position source, Planet target) {
		isLeaf = true;
		children = new TreeMap<>();
		sourcePos = source;
    	targetPlanetId = target.getId();
    	//Log.log("debug:stn constructor: source = " + source.getXPos() + "," + source.getYPos() + ", targetplanet("+targetPlanetId+") = " + target.getXPos() + "," + target.getYPos() + "," + target.getRadius());


        final double angularStepRad = Math.PI/180.0;
        dist = source.getDistanceTo(target);
        final double stRad = source.orientTowardsInRad(target);
       
        final double rfdist = target.getRadius() + Constants.FORECAST_FUDGE_FACTOR;
        final double tLERad = stRad + HALF_PI; //orientation from target center to left "edge"
        
        final double LEdx = Math.cos(tLERad + angularStepRad) * rfdist;
        final double LEdy = Math.sin(tLERad + angularStepRad) * rfdist;
        
        final double LExpos = target.getXPos() + LEdx;
        final double LEypos = target.getYPos() + LEdy;
        
    	//Log.log("debug:stn constructor: ledx = " + LEdx + ", ledy = " + LEdy + ", LExpos = " + LExpos + ", LEypos = "+ LEypos);

        leftEdge = new Position (LExpos, LEypos);

        // right edge is in the opposite direction from the center of the planet
        final double RExpos = target.getXPos() - LEdx;
        final double REypos = target.getYPos() - LEdy;
        
    	rightEdge = new Position (RExpos, REypos);
    	
    	leftAngle = (source.orientTowardsInRad(leftEdge))%TWO_PI;
    	rightAngle = (source.orientTowardsInRad(rightEdge))%TWO_PI;
    	double tempAng;
    	if(leftAngle < rightAngle) {
			overZ = true;
			maxOverZ = true;
    		tempAng = leftAngle;
    		leftAngle = rightAngle;
    		rightAngle = tempAng;
    	}
    	leftMaxAngle = leftAngle;
    	rightMinAngle = rightAngle;

		computeMidAngle();
//    	Log.log("debug:stn constructor: LA:" + leftAngle + ", RA:" + rightAngle + ", midAngle = " + midAngle + ", leftEdge = " + leftEdge.getXPos() + ", " + leftEdge.getYPos()  + ", right Edge = " + rightEdge.getXPos() + ", " + rightEdge.getYPos());
    	//Log.log("fin stnconst");
	}

	public boolean isLeaf() {
		return isLeaf;
	}
	
	public void insert(Planet pl) {
//		Log.log("########  INSERTING NEW PLANET "+pl.getId()+" ###########");

		
		ShadowTreeNode stn = new ShadowTreeNode(sourcePos, pl);
		insertSTN(stn);
	}
	
	
	public void insertSTN(ShadowTreeNode plSTN) {
		ArrayList<Double> possibleAngleKeys = findClosestSubtrees(plSTN.getMidAngle());

		insertInRightSTN(possibleAngleKeys, plSTN);
		adaptSubTreeRange(plSTN.getLeftAngle(), plSTN.getRightAngle(), plSTN.isMaxOverZ());

	}
	
	private boolean isMaxOverZ() {
		return maxOverZ;
	}


	public void insertInRightSTN(ArrayList<Double> keyList, ShadowTreeNode plSTN) {
		if(keyList == null ||keyList.isEmpty()) {
			insertChild(plSTN);
			return;
		}
		if(keyList.size() < 2) {
			children.get(keyList.get(0)).insertSTN(plSTN);
			return;
		}
		int minSubtreesFirst = getMinSubtreeNumToAngle(children.get(keyList.get(0)).getMidAngle());
		int minSubtreesSecond = getMinSubtreeNumToAngle(children.get(keyList.get(1)).getMidAngle());
		if(minSubtreesSecond > minSubtreesFirst) {
			children.get(keyList.get(0)).insertSTN(plSTN);
		} else {
			children.get(keyList.get(1)).insertSTN(plSTN);
		}

	}
	
	public int getMinSubtreeNumToAngle(double angle) {

		ArrayList<Double> possibleAngleKeys = findClosestSubtrees(angle);
		if(possibleAngleKeys == null || possibleAngleKeys.isEmpty()) {

			return 0;
		}
		int first = children.get(possibleAngleKeys.get(0)).getMinSubtreeNumToAngle(angle);
		if(possibleAngleKeys.size() < 2) {

			return first + 1;
		}
		int second = children.get(possibleAngleKeys.get(1)).getMinSubtreeNumToAngle(angle);
		if(first > second) {
			return second + 1;
		} else {
			return first + 1;
		}
		
	}

	
	private void adaptSubTreeRange(double leftSubAngle, double rightSubAngle, boolean subTreeMaxOverZ) {
		if(maxOverZ || subTreeMaxOverZ) {
//			Log.log("astr: rma: " + rightMinAngle + ", lma: " + leftMaxAngle + ", rsa: " + rightSubAngle + ", lsa: " + leftSubAngle + ", p maxoverz: " + maxOverZ + ", c maxoverz: " + subTreeMaxOverZ);
		}

		if(subTreeMaxOverZ && !maxOverZ && !isRoot) { //change values
			if(leftMaxAngle > rightSubAngle) {
				rightMinAngle = leftMaxAngle;
				leftMaxAngle = rightSubAngle;
//				Log.log("only child maxoverz: setting rma = lma, lma = rsa (switch)");

			} else if(rightMinAngle < leftSubAngle) {
				leftMaxAngle = rightMinAngle;
				rightMinAngle = leftSubAngle;
				
//				Log.log("only child maxoverz: setting lma = rma, rma = lsa (switch)");

			}
			maxOverZ = true;
			return;
		} 
		
		if(maxOverZ) {
			if(subTreeMaxOverZ) {
				if(leftMaxAngle > leftSubAngle) {
					leftMaxAngle = leftSubAngle;
				}
				if(rightMinAngle < rightSubAngle) {
					rightMinAngle = rightSubAngle;
				}
//				Log.log("both subtrees maxoverz: lma = lsa, rma = rsa");

				return;
			} else {
				if(leftMaxAngle > rightSubAngle) {
					leftMaxAngle = rightSubAngle;
				}
				if(rightMinAngle < leftSubAngle) {
					rightMinAngle = leftSubAngle;
				}
//				Log.log("only parent maxoverz: lma = rsa, rma = lsa (childswitch)");

			}
			return;

		}
		if(leftMaxAngle < leftSubAngle) {
			leftMaxAngle = leftSubAngle;
		}
		if(rightMinAngle > rightSubAngle) {
			rightMinAngle = rightSubAngle; 
		}
		//Log.log("no maxoverz: lma = lsa, rma = rsa");

	}


	protected void insertChild(ShadowTreeNode stn) {

		isLeaf = false;
		children.put(stn.getMidAngle(), stn);

	}
	
	
	public boolean remove(Planet pl) { //TODO
//		Log.log("debug:STN: calling remove");
/*
		ShadowTreeNode stn = findSTNByPos(pl);
		if(stn == null) {
			return removeChild(pl);
		} else {
			return stn.removeChild(pl);
		}
		*/
		return true;
	}
	
	
	/*
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
	*/
	
	/*
	public ShadowTreeNode findSTNByPos(Position target) { //find the shadow in which the position is
		Log.log("debug:STN: calling findSTNByPos");

		return getRecSTN(sourcePlanet.orientTowardsInRad(target), sourcePlanet.getDistanceTo(target));
	}
	*/
	
	/*
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
		
	}*/
	
	public boolean angleInRange(double angle) {
		double nAngle = angle % TWO_PI;
		if(overZ) {
			if(nAngle > rightAngle || nAngle < leftAngle) {
				return false; //angle out of range
			}
			return true;
		}
		if(nAngle > leftAngle || nAngle < rightAngle) {
			return false; //angle out of range
		}
		return true;
	}
	
	public boolean angleInSubTreeRange(double angle) {
		double nAngle = angle % TWO_PI;

		if(maxOverZ) {
			if(nAngle > rightMinAngle || nAngle < leftMaxAngle) {
				//Log.log("aistr ("+angle+"): nAngle = " +nAngle +" NOT in range (" + leftMaxAngle + "/" + rightMinAngle +") (maxOverZ)");
				return false; //angle out of range
			}

			//Log.log("aistr ("+angle+"): nAngle = " +nAngle +" IS in range (" + leftMaxAngle + "/" + rightMinAngle +") (maxOverZ)");

			return true;
		}
		if(angle > leftMaxAngle || angle < rightMinAngle) {
			//Log.log("aistr ("+angle+"): nAngle = " +nAngle +" NOT in range (" + rightMinAngle + "/" + leftMaxAngle +")");
			return false; //angle out of range
		}
		//Log.log("aistr ("+angle+"): nAngle = " +nAngle +" IS in range (" + rightMinAngle + "/" + leftMaxAngle +")");
		return true;
	}
	
	public LinkedList<Position> getPathFromPosToPos(Position fromPos, Position targetNearPlanet){
		double targetAngle = sourcePos.orientTowardsInDeg(fromPos);
		double targetDist = sourcePos.orientTowardsInDeg(fromPos);

		LinkedList<Position> fullPath = getPathFromPositionRec(targetAngle, targetDist);
		ShadowTreeNode temp = new ShadowTreeNode(targetNearPlanet, sourcePlanet);
		if(!fullPath.isEmpty() && fullPath != null) {
			fullPath.add(temp.getPathFromPosition(fullPath.getLast()).getFirst());
		} else {
			fullPath.add(temp.getPathFromPosition(fromPos).getFirst());
		}
		
		return fullPath;
	}
	
	public LinkedList<Position> getPathFromPosition(Position p){
		//Log.log("debug:STN: calling getPathFromPosition");
				
		double targetAngle = sourcePos.orientTowardsInDeg(p);
		double targetDist = sourcePos.orientTowardsInDeg(p);


		return getPathFromPositionRec(targetAngle, targetDist);
	}
	
	public LinkedList<Position> getPathFromPositionRec(double angle, double dist){
		//Log.log("debug:STN: calling getPathFromPositionRec");
		
		if(dist < this.dist) {
			return null;
		}
		//Log.log("debug:STN: GPFPR 1");

		ArrayList<Double> possibleAngleKeys = findClosestSubtrees(angle);
		if(possibleAngleKeys == null || possibleAngleKeys.isEmpty()) {
			LinkedList<Position> addedPositions = new LinkedList<>();

			if(isRoot) {
				//Log.log("debug:STN: GPFPR ret ap");

				return addedPositions;
			}
			if(angle >= midAngle) {
				
				addedPositions.add(leftEdge);
			} else {
				addedPositions.add(rightEdge);
			}
			//Log.log("debug:STN: GPFPR ret ap+");

			return addedPositions;
		}
		LinkedList<Position> result = null;
		for(Double d : possibleAngleKeys) {
			result = children.get(d).getPathFromPositionRec(angle, dist);
			if(result != null) {
				break; 
			}
		}
		//Log.log("debug:STN: GPFPR 2");

		if(result == null) {
			//Log.log("debug:STN: GPFPR ret null");

			return null;
		}

		if(isRoot) {

			return result;
		}
		
		if(sourcePos.orientTowardsInDeg(result.getLast()) > midAngle) {
				result.add(leftEdge);
		} else {
			result.add(rightEdge);
		}

		return result;
	}
	
	//find all potential subtree keys
	private ArrayList<Double> findClosestSubtrees(double angle){

		ArrayList<Double> angleMatches = new ArrayList<>();
		double closestRightKey = 0;
		double closestLeftKey = 0;
		boolean onlyRightKey = false;
		
		Set<Double> keyAngles = children.keySet(); //sorted in ascending order
		Iterator<Double> kit = keyAngles.iterator();
		Iterator<Double> nkit = keyAngles.iterator(); //for peeking elements - lighter solution than creating an additional array to iterate through
		double currentKey;
		double nextKey;
		double firstKey;
		
		if(nkit.hasNext()) {
			firstKey = nkit.next();
			nextKey = firstKey;
			//closestRightKey = nextKey;
		} else {
			return null;
		}

		while(kit.hasNext()) {
			currentKey = kit.next();
			if(nkit.hasNext()) {
				nextKey = nkit.next();
				if(nextKey == angle) { 
					onlyRightKey = true; //only return nextKey
					closestRightKey = nextKey;
					break;
				}
				if(nextKey > angle) {
					closestRightKey = currentKey;
					closestLeftKey = nextKey;
					break;

				}
			} else {
				closestRightKey = currentKey;
				if(currentKey != firstKey) {
					closestLeftKey = firstKey;
				} else {
					onlyRightKey = true;
				}
				break;
			}
		}

		if(children.get(closestRightKey).angleInSubTreeRange(angle)) {
			angleMatches.add(closestRightKey);
		}

		if(onlyRightKey) {
			return angleMatches;
		}
		if(children.get(closestLeftKey).angleInSubTreeRange(angle)) {
			angleMatches.add(closestLeftKey);
		}

		return angleMatches;
		
	}
	
	
	public double getRightAngle() {
		return rightAngle;
	}
	
	public double getLeftAngle() {
		return leftAngle;
	}
	
	/*
	private double getDist() {
		return dist;
	}
	*/

	protected double getMidAngle() {
		return midAngle;
	}
	
	private void computeMidAngle() {
		if(overZ) {
			double cLeftAngle = (leftAngle + Math.PI)%TWO_PI;
			double cRightAngle = (leftAngle+Math.PI)%TWO_PI;
			midAngle = (((cLeftAngle + cRightAngle)/2)-Math.PI)%TWO_PI;
		}
		midAngle = ((leftAngle + rightAngle)/2)%TWO_PI;
	}
	
	public String getString(int depth) {
		String ret = "";
		
		for(int i = 0; i < depth; i++) {
			ret += " ";
		}
		
		if(maxOverZ) {
			ret+= ("STN (" + targetPlanetId + "):"+leftMaxAngle +"°("+leftAngle+"°)-"+rightMinAngle+"°("+rightAngle+"°), d=" +dist);		
		} else {	
			ret+= ("STN (" + targetPlanetId + "):"+rightMinAngle +"°("+rightAngle+"°)-"+leftMaxAngle+"°("+leftAngle+"°), d=" +dist);
		}
		if(maxOverZ) {
			ret+= ", maxOverZ";
		}
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

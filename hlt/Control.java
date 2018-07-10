package hlt;

import java.util.HashMap;
import java.util.Map;

import hlt.Task.TaskType;

/*

double globalPrioThresh = attributes[Control.NUM3ATTS] * Control.GLOBAL_PRIO_FACTOR;
double mapDifTaskChangeTime = attributes[Control.NUM3ATTS+1];
double mapDifChangeFactor = attributes[Control.NUM3ATTS+2];
double attDistUnitFactor = attributes[Control.NUM3ATTS+3];

double targetSpecificPlayer = attributes[Control.NUM3ATTS+4]; 
Log.log("reading rest bot attributes");

double dockTypeHealthThr = attributes[Control.NUM3ATTS+5] * Constants.MAX_SHIP_HEALTH;*/

//computes a "global" priority for ships
public class Control {
	
	public static final double GLOBAL_PRIO_FACTOR = 0.5;
	public static final int NUM_UNSPEC_ATTS = 8; //globalPrioThresh, mapDifTaskChangeTime, mapDifChangeFactor, attDistUnitFactor, taretSpecificPlayer, dockTypeHealthThr, initPhaseThresh, pathfinding type
	public static final int NUM2ATTSIZE = MapDif.NUM_MAPDIF_ATTS; //11: {Tasks}x{ownedPlanetPercentage,PlanetsOverThreshold}(10) + PlanetThreshVal(1)
	public static final int NUM3ATTSIZE = LocalChecker.NUM_LC_WEIGHTS; //12: {Tasks}x{3Factors each}
	public static final int NUM4ATTSIZE = LocalChecker.NUM_WM_WEIGHTS; //5: {Tasks};
	
	public static final int NUM1ATTS = Task.NUM_ACTIVE_TYPES;  //5(Tasks): AttackAny, Conquer, Expand, Reinforce, Diversion

	public static final int NUM2ATTS = NUM1ATTS + NUM2ATTSIZE;
	public static final int NUM3ATTS = NUM2ATTS + NUM3ATTSIZE;
	public static final int NUM4ATTS = NUM3ATTS + NUM4ATTSIZE;
	
	public static final int NUM_ATTS = NUM4ATTS + NUM_UNSPEC_ATTS;
	//public static final int NUM_ATTS = LocalChecker.NUM_LC_WEIGHTS +1 ;
	
	
	public static final int NUM_MAX_CHANGE_TURNS = 20;
	private static final double MAX_ADD_RATIO = 0.4;

	//not used
	public enum TaskTypeRatio { AttackVSBuild, ATAnyVSPlanned, ATPConquerVSDiv, BUExpandVsReinforce, OffensiveVSDefensive, LocalVSGlobal;
		  @Override
		  public String toString() {
		    switch(this) {
		      case AttackVSBuild: return "[attack vs build]";
		      case ATAnyVSPlanned: return "[attack: any vs planned]";
		      case ATPConquerVSDiv: return "[attack planned: conquer vs div]";
		      case BUExpandVsReinforce: return "[build: expand vs reinforce]"; 
		      case OffensiveVSDefensive: return "[offensive vs defensive ]"; 
		      case LocalVSGlobal: return "[local vs global decisionmaking]"; 
		      default: throw new IllegalArgumentException();
		    }
		 
		}
	}
	
	int players;
	int myId;
	int numShips;
	int numPlanets;
	int currentRound;

	double[] distr; //the desired ship distribution
	
	double[] nextDistr;
	int waitUntilChange;
	int remainingChangeRounds;
	boolean changesToNextRatio;
	double[] changeAmount;
	int waitTime;
	
	double globalDifThreshold;
	
	int nDynShips;
	int[] dynNumShips; //the actual ship distribution
	boolean dynPossibleTasks[];
	private double addRatio;

	public Control(int myPlayerId, double[] shipDistribution) {
		myId = myPlayerId;
		globalDifThreshold = 0.1;
		currentRound = 0;
		waitUntilChange = 0;
		changesToNextRatio = false;
		
		distr = new double[shipDistribution.length];
		//attributes = convertToRatioNums(shipDistribution);
		System.arraycopy(shipDistribution, 0, distr, 0, shipDistribution.length);
		
        dynPossibleTasks = new boolean[Task.NUM_ACTIVE_TYPES];
		dynNumShips = new int[Task.NUM_ACTIVE_TYPES]; //create an array of numbers to count the amount of each type, each round
		clearDynNums();

	}
	
	public Control(int myPlayerId) {
		myId = myPlayerId;
		globalDifThreshold = 0.1;
		currentRound = 0;
		waitUntilChange = 0;
		changesToNextRatio = false;


		int[] standardRatio = new int[Task.NUM_ACTIVE_TYPES];
		for(int i = 0; i < standardRatio.length; i++) {
			standardRatio[i] = 1;
		}
		
		distr = convertToRatioNums(standardRatio);
		
        dynPossibleTasks = new boolean[Task.NUM_ACTIVE_TYPES];
		dynNumShips = new int[Task.NUM_ACTIVE_TYPES]; //create an array of numbers to count the amount of each type, each round
		clearDynNums();

	}
	
	public Control(int myPlayerId, double[] shipDistribution, double taskChangeDurance, double addFactor) {
		this(myPlayerId, shipDistribution);
		waitTime = (int) (taskChangeDurance * NUM_MAX_CHANGE_TURNS);
		addRatio = addFactor * MAX_ADD_RATIO;
	}

	
	/**
	 * this may influence, whether a ship will use a global priority or local priority.
	 * initially, it is set to 0.1
	 */
	public void setGlobalDifThresh(double newThreshold) {
		globalDifThreshold = newThreshold;
	}

	
	public double[] convertToRatioNums(int[] ratioNums) {
		int ratioSum = 0;
		for(int i = 0; i < ratioNums.length; i++) {
			ratioSum += ratioNums[i];
		}
		double[] resRatio = new double[ratioNums.length];
		for(int j = 0; j < resRatio.length; j++) {
			if(ratioSum != 0) {
				resRatio[j] = (double) ratioNums[j]/ratioSum;
			} else {
				resRatio[j] = 0; 
			}
		}
		return resRatio;
	}
	
	public void initRound(HashMap<Integer, Task> tasks) {
		clearDynNums();
		changeRatio(); //only is changed, if changesToFinalRatio is set
		currentRound++;
		
		//set the initial taskRatio
		for(Map.Entry<Integer, Task> tme : tasks.entrySet()) {
			if(Task.isControlTask(tme.getValue().getType())) {
				increaseShipNum(tme.getValue().getType());
			}
		}
	}

	
	/**
	 * if a task exceeds its ratio by more than globalDifThreshold, this returns false
	 */
	public boolean isWithinGlobalDifThresh(int round) {
		double roundFactor = 1;
		if(round < 10) {
			roundFactor += (10.0d-(double)round)/20;
		}
		
		for(int i = 0; i < distr.length; i++) {
			double dynRatio =  (double)dynNumShips[i] / (double)nDynShips;
			double currentDif = distr[i] - dynRatio;
			if(currentDif > globalDifThreshold*roundFactor) {
				return false;
			}
		}
		return true;
	}
	
	public void increaseShipNum(TaskType type) {
		if(!Task.isControlTask(type)){
			return;
		}
		dynNumShips[Task.getTaskTypeIndex(type)]++;
		nDynShips++;
	}
	
	public void decreaseShipNum(TaskType type) {
		if(!Task.isControlTask(type)){
			return;
		}
		int index = Task.getTaskTypeIndex(type);
		if(dynNumShips[index] > 0 && nDynShips > 0) {
			dynNumShips[index]--;
			nDynShips--;
		}
	}
	
	private void clearDynNums() {
		for(int i = 0; i < dynNumShips.length; i++) {
			dynNumShips[i] = 0;
		}
		nDynShips = 0;
		for(int i = 0; i < dynPossibleTasks.length; i++) {
			dynPossibleTasks[i] = true;
		}
	}
	
	/**
	 * determines which Tasks may be done, TODO:? currently only checks planets
	 */
    public void setDynPossibleTasks(GameMap map) {
		dynPossibleTasks[Task.getTaskTypeIndex(TaskType.Expand)] = false;
		dynPossibleTasks[Task.getTaskTypeIndex(TaskType.Reinforce)] = false;
	
    	
    	for(Map.Entry<Integer, Planet> entry : map.getAllPlanets().entrySet()) {
    		Planet p = entry.getValue();
    		if(p.getOwner() == myId && !p.isFull()) {
    			dynPossibleTasks[Task.getTaskTypeIndex(TaskType.Reinforce)] = true;
    		} else if(!p.isOwned()) {
    			dynPossibleTasks[Task.getTaskTypeIndex(TaskType.Expand)] = true;
    		}
    	}	

    }
    
    public double getHighestDif() {
    	if(nDynShips == 0) { //no entry yet
			double biggestRatio = 0;
			for(int i = 0; i<Task.NUM_ACTIVE_TYPES;i++) {
				if(!dynPossibleTasks[i]) {
					continue;
				}
				if(distr[i] > biggestRatio) {
					biggestRatio = distr[i];
				}
			}
			return biggestRatio;
    	}
    	double maxPosDif = 0;
		for(int i = 0; i < distr.length; i++) {
			if(!dynPossibleTasks[i]) {
				continue;
			}
			double dynRatio =  (double)dynNumShips[i] / (double)nDynShips;
			double currentDif = distr[i] - dynRatio;
			//debug += getTaskTypeByIndex(i).toString() + ":" +  dynNumShips[i] + ", -> " + attributes[i] + " - " + dynRatio + "|| \n";
			if(currentDif > maxPosDif) {
				maxPosDif = currentDif;
			}
		}
		return maxPosDif;
    	  	
    }


	
	public TaskType getNextTypeAndUpdate() {
		TaskType addedType = null;
		
		String debug = "";

		if(nDynShips == 0) { //no entry yet
			double biggestRatio = 0;
			int indexOfBiggest = 0;
			for(int i = 0; i<Task.NUM_ACTIVE_TYPES;i++) {
				if(!dynPossibleTasks[i]) {
					continue;
				}
				if(distr[i] > biggestRatio) {
					biggestRatio = distr[i];
					indexOfBiggest = i;
				}
			}
			addedType = Task.getTaskTypeByIndex(indexOfBiggest);
			debug += "Control:returned type (first): " + addedType.toString();
			//Log.log(debug);

			increaseShipNum(addedType);
			return addedType;
			
		}
		
		//debug += "Control:(dynratios: ";
		//find maximum difference to the specified ship ratio fulfilling one type of task
		double maxPosDif = 0;
		int maxDifIndex = 0;
		for(int i = 0; i < distr.length; i++) {
			if(!dynPossibleTasks[i]) {
				continue;
			}
			double dynRatio =  (double)dynNumShips[i] / (double)nDynShips;
			double currentDif = distr[i] - dynRatio;
			//debug += getTaskTypeByIndex(i).toString() + ":" +  dynNumShips[i] + ", -> " + attributes[i] + " - " + dynRatio + "|| \n";
			if(currentDif > maxPosDif) {
				maxPosDif = currentDif;
				maxDifIndex = i;
			}
		}
		addedType = Task.getTaskTypeByIndex(maxDifIndex);
		//debug += "), Task " + (nDynShips+1) + ", maxPosDif = " + maxPosDif + ", dynNumShips[max] = " + dynNumShips[maxDifIndex];
		
		//debug += "Control: returned type: " + addedType.toString() + "\n";
		//Log.log(debug);
		increaseShipNum(addedType);
		return addedType;
	}

	
	public void changeRatioOverTime(double[] newDistribution, int waitTime, int numberRoundsToChange) {
		waitUntilChange = waitTime;
		remainingChangeRounds = numberRoundsToChange;

		nextDistr = new double[newDistribution.length];
		System.arraycopy(newDistribution, 0, nextDistr, 0, newDistribution.length);

		changeAmount = new double[distr.length];
		for(int i = 0; i < changeAmount.length; i++) {
			changeAmount[i] = (nextDistr[i] - distr[i]) / numberRoundsToChange;
		}
	}
	
	private void changeRatio() {
		if(!changesToNextRatio) {
			return;
		}
		if(waitUntilChange > 0) {
			waitUntilChange--;
			return;
		}
		remainingChangeRounds--;
		if(remainingChangeRounds == 0) {
			System.arraycopy(nextDistr, 0, distr, 0, nextDistr.length);

			distr = nextDistr;
			changesToNextRatio = false;
		} else {
			for(int i = 0; i<distr.length;i++) {
				distr[i] += changeAmount[i];
			}
		}

	}
	
	
	public void changeRatioField(TaskType field, double amount) {
		double[] newAtts;
		if(remainingChangeRounds > 0) {
			newAtts = nextDistr;
		} else {
			newAtts = distr;
		}
		
		newAtts[Task.getTaskTypeIndex(field)] += addRatio * amount;
	
		changeRatioOverTime(normalize(newAtts),0, waitTime);
	}
	
	

	public static double[] normalize(double[] attributes) {
		double sum = 0;
		for(int i = 0; i < attributes.length; i++) {
			sum += attributes[i];
		}
		if(sum == 1) {
			return attributes;
		}
		for(int i = 0; i < attributes.length; i++) {
			attributes[i] = attributes[i] / sum;
		}
		return attributes;
	}

	public TaskType getNextDockTypeAndUpdate() {
		TaskType addedType = null;
		
		String debug = "";

		if(nDynShips == 0) { //no entry yet
			double biggestRatio = 0;
			int indexOfBiggest = 0;
			for(int i = 0; i<Task.NUM_ACTIVE_TYPES;i++) {
				if(!dynPossibleTasks[i]) {
					continue;
				}
				if(distr[i] > biggestRatio && Task.getTaskTypeByIndex(i).isDockType()) {
					biggestRatio = distr[i];
					indexOfBiggest = i;
				}
			}
			addedType = Task.getTaskTypeByIndex(indexOfBiggest);
			debug += "Control:returned type (first): " + addedType.toString();
			//Log.log(debug);

			increaseShipNum(addedType);
			return addedType;
			
		}
		
		//debug += "Control:(dynratios: ";
		//find maximum difference to the specified ship ratio fulfilling one type of task
		double maxPosDif = 0;
		int maxDifIndex = 0;
		for(int i = 0; i < distr.length; i++) {
			if(!dynPossibleTasks[i]) {
				continue;
			}
			double dynRatio =  (double)dynNumShips[i] / (double)nDynShips;
			double currentDif = distr[i] - dynRatio;
			//debug += getTaskTypeByIndex(i).toString() + ":" +  dynNumShips[i] + ", -> " + attributes[i] + " - " + dynRatio + "|| \n";
			if(currentDif > maxPosDif) {
				maxPosDif = currentDif;
				maxDifIndex = i;
			}
		}
		addedType = Task.getTaskTypeByIndex(maxDifIndex);
		//debug += "), Task " + (nDynShips+1) + ", maxPosDif = " + maxPosDif + ", dynNumShips[max] = " + dynNumShips[maxDifIndex];
		
		//debug += "Control: returned type: " + addedType.toString() + "\n";
		//Log.log(debug);
		increaseShipNum(addedType);
		return addedType;
	}


}

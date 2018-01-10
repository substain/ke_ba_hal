package hlt;

import java.util.Map;

import hlt.Task.TaskType;


//computes a "global" priority for ships
public class Control {
	int players;
	int myId;
	int numShips;
	int numPlanets;
	int currentRound;

	double[] taskRatio;
	
	double[] nextTaskRatio;
	int waitUntilChange;
	int remainingChangeRounds;
	boolean changesToNextRatio;
	double[] changeAmount;
	
	double globalDifThreshold;
	
	int nDynShips;
	int[] dynNumShips;
	boolean dynPossibleTasks[];

	public Control(int myPlayerId, int[] taskRatioNums) {
		myId = myPlayerId;
		globalDifThreshold = 0.1;
		currentRound = 0;
		waitUntilChange = 0;
		changesToNextRatio = false;
		
		
		taskRatio = convertToRatioNums(taskRatioNums);
		
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
		
		taskRatio = convertToRatioNums(standardRatio);
		
        dynPossibleTasks = new boolean[Task.NUM_ACTIVE_TYPES];
		dynNumShips = new int[Task.NUM_ACTIVE_TYPES]; //create an array of numbers to count the amount of each type, each round
		clearDynNums();

	}
	
	/**
	 * this influences, whether a ship will use a global priority or local priority.
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
	

	public static int getTaskTypeIndex(TaskType taskt) {
		switch(taskt) {
		case AttackAny:
			return 0;
		case Conquer:
			return 1;
		case Dock:
			return 2;
		case Expand:
			return 3;
		case Reinforce:
			return 4;
		case Diversion:
			return 5;
		default: // 
			return -1;
		}
	}
	
	public static TaskType getTaskTypeByIndex(int i) {
		switch(i) {
		case 0:
			return TaskType.AttackAny;
		case 1:
			return TaskType.Conquer;
		case 2:
			return TaskType.Dock;
		case 3:
			return TaskType.Expand;
		case 4:
			return TaskType.Reinforce;
		case 5: 
			return TaskType.Diversion;
		default:
			return TaskType.AttackAny;
		}
	}
	/*
	/**
	 * changes the ratio number of a specific Task,
	 * @param type the tasktype, which ratio will be changed
	 * @param amount how much it will change
	 * @param increase if true, increase the ratio, otherwise decrease it
	 *
	public void changeTaskRatio(TaskType type, int amount, boolean increase) {
		if(increase) {
			startTaskRatio[getTaskTypeIndex(type)] += amount;
		} else { //decrease
			startTaskRatio[getTaskTypeIndex(type)] -= amount;
		}
	}
	*/
	
	/**
	 * if a task exceeds its ratio by more than globalDifThreshold, this returns false
	 */
	public boolean isWithinGlobalDifThresh() {
		for(int i = 0; i < taskRatio.length; i++) {
			double dynRatio =  (double)dynNumShips[i] / (double)nDynShips;
			double currentDif = taskRatio[i] - dynRatio;
			if(currentDif > globalDifThreshold) {
				return false;
			}
		}
		return true;
	}
	
	public void increaseShipNum(TaskType type) {
		dynNumShips[getTaskTypeIndex(type)]++;
		nDynShips++;
	}
	
	public void initNextRound() {
		clearDynNums();
		changeRatio(); //only is changed, if changesToFinalRatio is set
		currentRound++;
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
	 * determines which Tasks may be done, TODO: currently only checks planets
	 */
    public void setDynPossibleTasks(GameMap map) {
		dynPossibleTasks[getTaskTypeIndex(TaskType.Expand)] = false;
		dynPossibleTasks[getTaskTypeIndex(TaskType.Reinforce)] = false;
	
    	
    	for(Map.Entry<Integer, Planet> entry : map.getAllPlanets().entrySet()) {
    		Planet p = entry.getValue();
    		if(p.getOwner() == myId && !p.isFull()) {
    			dynPossibleTasks[getTaskTypeIndex(TaskType.Reinforce)] = true;
    		} else if(!p.isOwned()) {
    			dynPossibleTasks[getTaskTypeIndex(TaskType.Expand)] = true;
    		}
    	}	

    }


	
	public TaskType getNextTypeAndUpdate() {
		TaskType addedType = null;
		
		//String debug = "";

		if(nDynShips == 0) { //no entry yet
			double biggestRatio = 0;
			int indexOfBiggest = 0;
			for(int i = 0; i<taskRatio.length;i++) {
				if(!dynPossibleTasks[i]) {
					continue;
				}
				if(taskRatio[i] > biggestRatio) {
					biggestRatio = taskRatio[i];
					indexOfBiggest = i;
				}
			}
			addedType = getTaskTypeByIndex(indexOfBiggest);
			//debug += "Control:returned type (first): " + addedType.toString();
			//Log.log(debug);

			increaseShipNum(addedType);
			return addedType;
			
		}
		
		//debug += "Control:(dynratios: ";
		//find maximum difference to the specified ship ratio fulfilling one type of task
		double maxPosDif = 0;
		int maxDifIndex = 0;
		for(int i = 0; i < taskRatio.length; i++) {
			if(!dynPossibleTasks[i]) {
				continue;
			}
			double dynRatio =  (double)dynNumShips[i] / (double)nDynShips;
			double currentDif = taskRatio[i] - dynRatio;
			//debug += getTaskTypeByIndex(i).toString() + ":" +  dynNumShips[i] + ", -> " + taskRatio[i] + " - " + dynRatio + "|| \n";
			if(currentDif > maxPosDif) {
				maxPosDif = currentDif;
				maxDifIndex = i;
			}
		}
		addedType = getTaskTypeByIndex(maxDifIndex);
		//debug += "), Task " + (nDynShips+1) + ", maxPosDif = " + maxPosDif + ", dynNumShips[max] = " + dynNumShips[maxDifIndex];
		
		//debug += "Control: returned type: " + addedType.toString() + "\n";
		//Log.log(debug);
		increaseShipNum(addedType);
		return addedType;
	}

	
	public void changeRatioOverTime(int[] finalRatioNums, int waitTime, int numberRoundsToChange) {
		waitUntilChange = waitTime;
		remainingChangeRounds = numberRoundsToChange;
		nextTaskRatio = convertToRatioNums(finalRatioNums);
		changeAmount = new double[taskRatio.length];
		for(int i = 0; i < changeAmount.length; i++) {
			changeAmount[i] = (nextTaskRatio[i] - taskRatio[i]) / numberRoundsToChange;
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
			taskRatio = nextTaskRatio;
			changesToNextRatio = false;
		} else {
			for(int i = 0; i<taskRatio.length;i++) {
				taskRatio[i] += changeAmount[i];
			}
		}

	}
	
	/*
	public void update(GameMap map) {
		//TODO
	}
	*/


}

package genAlgo;

import java.util.ArrayList;

public class Match {
	
	public static final int TYPE_GA = 0;
	public static final int TYPE_SAFE = 1;
	public static final int TYPE_EXT = 2;

	
	private int firstBotID;
	private int firstBotType;
	
	private int secondBotType; //while first bot is always a GA bot, second bot may be a SGA or extern bot
	//0: GA_Bots , 1:safeBots, 2: externBots
	
	private int secondBotID;
	
	private int thirdBotType;
	private int thirdBotID;
	
	private int forthBotType; 
	
	private int forthBotID;

	private boolean fourPlayerMatch;
	
	public Match(int first, int second, int secondType) {
		firstBotID = first;
		firstBotType = TYPE_GA;
		secondBotID = second;
		secondBotType = secondType;
		fourPlayerMatch = false;
	}
	public Match(int first, int firstType, int second, int secondType) {
		firstBotID = first;
		firstBotType = firstType;
		secondBotID = second;
		secondBotType = secondType;
		fourPlayerMatch = false;
	}
	
	public Match(int first, int second, int secondType, int third, int thirdType, int forth, int forthType) {
		firstBotID = first;
		firstBotType = TYPE_GA;
		secondBotID = second;
		secondBotType = secondType;
		thirdBotID = third;
		thirdBotType = thirdType;
		forthBotID = forth;
		forthBotType = forthType;
		fourPlayerMatch = true;
	}
	
	public Match(int first,int firstType, int second, int secondType, int third, int thirdType, int forth, int forthType) {
		firstBotID = first;
		firstBotType = firstType;
		secondBotID = second;
		secondBotType = secondType;
		thirdBotID = third;
		thirdBotType = thirdType;
		forthBotID = forth;
		forthBotType = forthType;
		fourPlayerMatch = true;
	}
	
	public boolean isFourPlayer() {
		return fourPlayerMatch;

	}
	
	public int getID(int player) {
		switch(player) {
		case 3:
			return forthBotID;
		case 2:
			return thirdBotID;
		case 1:
			return secondBotID;
		case 0:
		default:
			return firstBotID;
		}
	}
	
	public int getType(int pl) {
		switch(pl) {
		case 3:
			return forthBotType;
		case 2:
			return thirdBotType;
		case 1:
			return secondBotType;
		case 0:
		default:
			return firstBotType;
		}
	}
	
	public static String getString(Match m, int numRepeats) {
		String ret;

		if(m.isFourPlayer()) {
			ret = "Match(4P):"+m.getID(0) + "("+ getTypeString(m.getType(0)) +")";
			for(int i = 1; i < 4; i++) {
				ret += "vs" + +m.getID(i) + "("+ getTypeString(m.getType(i)) +")";
			}
			ret += " x"+numRepeats;
		} else {

			ret = "Match(2P):"+m.getID(0) + "("+ getTypeString(m.getType(0)) +")";
			ret += "vs" + +m.getID(1) + "("+ getTypeString(m.getType(1))+") x"+numRepeats;
		}
		return ret;
	}
	
	public static String getTypeString(int type) {
		switch(type) {
		case TYPE_EXT:
			return "extern";
		case TYPE_SAFE:
			return "safe";
		case TYPE_GA:
		default:
			return "ga";
		}
	}
	
	public static void printMatches(ArrayList<Match> matches, int numRepeats) {
		for(Match m : matches) {
			System.out.println(getString(m, numRepeats));
		}
	}

}

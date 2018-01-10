package genAlgo;

public class Match {
	
	private int firstBotID;
	
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
		secondBotID = second;
		secondBotType = secondType;
		fourPlayerMatch = false;
	}
	
	public Match(int first, int second, int secondType, int third, int thirdType, int forth, int forthType) {
		firstBotID = first;
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
	
	public int getType(int player) {
		switch(player) {
		case 3:
			return forthBotType;
		case 2:
			return thirdBotType;
		case 1:
			return secondBotType;
		case 0:
		default:
			return 0;
		}
	}

}

package genAlgo;

import java.util.Random;

public class Individual {
	
	int[] attributes;
	static int count = 0;
	int id = 0;
	
	public Individual(int[] atts) {
		attributes = atts;
		id = count;
		count++;
	}
	
	public int[] getAttributes(){
		return attributes;
	}
	
	public int getId() {
		return id;
	}
	
	public void mutate() {
		int chosenAttribute = GenAlgo.randNum.nextInt(GenAlgo.NUM_ATTS);
		int newValue = GenAlgo.randNum.nextInt(GenAlgo.ATT_MAX);
		attributes[chosenAttribute] = newValue;
	}
	
	
}

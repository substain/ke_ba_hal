package genAlgo;

import java.util.Random;

public class Individual {
	
	int[] attributes;
	static int count = 0;
	int id = 0;
	int attributeMaximum;
	
	public Individual(int[] atts, int attMax) {
		attributes = atts;
		id = count;
		count++;
		attributeMaximum = attMax;
	}
	
	public int[] getAttributes(){
		return attributes;
	}
	
	public int getId() {
		return id;
	}
	
	public void mutate() {
		int chosenAttribute = HaliteGenAlgo.randNum.nextInt(attributes.length);
		int newValue = HaliteGenAlgo.randNum.nextInt(attributeMaximum);
		attributes[chosenAttribute] = newValue;
	}
	
	
}

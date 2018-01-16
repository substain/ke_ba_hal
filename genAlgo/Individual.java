package genAlgo;


public class Individual {

	//public static final int SUM_MAX = HaliteGenAlgo.ATT_MAX;
	
	double[] attributes;
	static int count = 0;
	int id = 0;
	double attributeMaximum;
	
	public Individual(double[] atts, double attMax) {
		//attributes = normalize(atts);
		attributes = atts;
		id = count;
		count++;
		attributeMaximum = attMax;
	}
	
	public double[] getAttributes(){
		return attributes;
	}
	
	public int getId() {
		return id;
	}
	
	public void mutate() {
		int chosenAttribute = HaliteGenAlgo.randNum.nextInt(attributes.length);
		double newValue = HaliteGenAlgo.randNum.nextDouble();
		attributes[chosenAttribute] = newValue;
	}

	public void mutateDistr() {
		int chosenAttribute = HaliteGenAlgo.randNum.nextInt(attributes.length);
		double newValue = HaliteGenAlgo.randNum.nextDouble();
		attributes[chosenAttribute] = newValue;
		attributes = normalize(attributes);
	}
	
	public static Individual recombineDistr(Individual p1, Individual p2) {
		double[] p1atts = p1.getAttributes();
		double[] p2atts = p2.getAttributes();

		int attsFromP1 = HaliteGenAlgo.NUM_ATTS/2;
		int attsFromP2 = HaliteGenAlgo.NUM_ATTS-attsFromP1;
		double[] childAtts = new double[HaliteGenAlgo.NUM_ATTS];
		for(int i = 0; i < childAtts.length; i++) {

			boolean takeP1atts = HaliteGenAlgo.randNum.nextBoolean();

			if((takeP1atts && attsFromP1 > 0) || attsFromP2 == 0) {
				childAtts[i] = p1atts[i];
				attsFromP1--;
			} else {
				childAtts[i] = p2atts[i];
				attsFromP2--;
			}
		} 
		childAtts = normalize(childAtts);
		return new Individual(childAtts, HaliteGenAlgo.ATT_MAX);
		
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
	
	/*
	public int getAttrSum() {
		int sum = 0;
		for(int i = 0; i < attributes.length; i++) {
			sum += attributes[i];
		}
		return sum;
	}*/
	
	
}

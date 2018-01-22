package genAlgo;

import hlt.Control;
import hlt.Task;

public class Individual {

	//public static final int SUM_MAX = HaliteGenAlgo.ATT_MAX;
	
	public static final int NORMALIZE_IND1 = 0;
	public static final int NORMALIZE_NUM1 = Control.NUM1ATTS;
	public static final int NORMALIZE_IND2 = Control.NUM1ATTS;
	public static final int NORMALIZE_NUM2 = Control.NUM2ATTSIZE;

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
		attributes = normalizeA(attributes);
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
		childAtts = normalizeA(childAtts);
		return new Individual(childAtts, HaliteGenAlgo.ATT_MAX);
		
	}
	
	public static double[] normalize(double[] attributes) {
		return normalizeInd(attributes, 0, attributes.length);
	}
	
	public static double[] normalizeA2(double[] attributes) {
		double[] tempAttr1 =  normalizeInd(attributes, 0, 3);
		double[] tempAttr2 =  normalizeInd(tempAttr1, 3, 3);
		double[] tempAttr3 =  normalizeInd(tempAttr2, 6, 3);
		double[] tempAttr4 =  normalizeInd(tempAttr3, 9, 3);

		return tempAttr4;
	}

	public static double[] normalizeA(double[] attributes) {
		double[] tempAttr1 =  normalizeInd(attributes, NORMALIZE_IND1, NORMALIZE_NUM1);
		double[] tempAttr2 =  normalizeInd(tempAttr1, NORMALIZE_IND2, NORMALIZE_NUM2);

		return tempAttr2;
	} 
	
	public static double[] normalizeInd(double[] attributes, int startNormIndex, int normIndSize) {
		if(normIndSize == 0) {
			return attributes;
		}
		double sum = 0;

		for(int i = startNormIndex; i < startNormIndex+normIndSize; i++) {
			sum += attributes[i];
		}
		if(sum == 1) {
			return attributes;
		}
		for(int i = startNormIndex; i < startNormIndex+normIndSize; i++) {
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

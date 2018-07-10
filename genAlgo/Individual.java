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
		double willCreepMut = HaliteGenAlgo.randNum.nextDouble();
		double newValue = 0.0d;
		int chosenAttribute = HaliteGenAlgo.randNum.nextInt(attributes.length);
		double targetValue = attributes[chosenAttribute];
		if(willCreepMut <= HaliteGenAlgo.PRESET_CREEPMUT_CHANCE) {
			
			//set interval [-negrange, posrange], also check for max/min value of the attribute before, instead of clipping (probability issues)
			double posrange = 0.5*HaliteGenAlgo.PRESET_CREEPMUT_INTERV;
			double negrange = posrange;
			if(targetValue+posrange > 1.0) {
				posrange = 1.0-targetValue;
			}
			if(targetValue-negrange < 0.0) {
				negrange = targetValue;
			}
			
			//compute stepsize of the creep
			double creepVal = (HaliteGenAlgo.randNum.nextDouble()*(negrange+posrange))-negrange;
			//add creep to old value
			newValue = targetValue + creepVal;
			System.out.println("used creep on attr ["+chosenAttribute+"] = "+newValue +" (old: "+targetValue+")");

			
		} else {
			newValue = HaliteGenAlgo.randNum.nextDouble();

		}
		attributes[chosenAttribute] = newValue;
		attributes = normalizeA(attributes);
	}
	
	public static Individual recombineDistr(Individual p1, Individual p2) {
		boolean useAvgOnBoth = true;
		boolean useAvgOnFirst = true;
		double[] p1atts = p1.getAttributes();
		double[] p2atts = p2.getAttributes();

		int attsFromP1 = HaliteGenAlgo.NUM_ATTS/2;
		int attsFromP2 = HaliteGenAlgo.NUM_ATTS-attsFromP1;
		double[] childAtts = new double[HaliteGenAlgo.NUM_ATTS];
		for(int i = 0; i < childAtts.length; i++) {

			boolean takeP1atts = HaliteGenAlgo.randNum.nextBoolean();


			if((takeP1atts && attsFromP1 > 0) || attsFromP2 == 0) {
				
				if(useAvgOnBoth || useAvgOnFirst) { //CHECK FOR AVG CROSSOVER CHANCE, MAKE SURE [NOT_AVGCROSS_PICKS] ARE EQUAL
					double useAvgCrossover = HaliteGenAlgo.randNum.nextDouble();
					if(useAvgCrossover < HaliteGenAlgo.PRESET_AVGCROSS_CHANCE) {
						useAvgOnBoth = !useAvgOnBoth;
						useAvgOnFirst = false;
						childAtts[i] = (p1atts[i]+p2atts[i])*0.5d;
						System.out.println("used avgcrossover on attr ["+i+"] = "+childAtts[i] +" (P1TURN)");
						continue;
					}
				}
				
				childAtts[i] = p1atts[i];
				attsFromP1--;

			} else {
				if(useAvgOnBoth || !useAvgOnFirst) { //CHECK FOR AVG CROSSOVER CHANCE, MAKE SURE [NOT_AVGCROSS_PICKS] ARE EQUAL
					double useAvgCrossover = HaliteGenAlgo.randNum.nextDouble();
					if(useAvgCrossover < HaliteGenAlgo.PRESET_AVGCROSS_CHANCE) {
						useAvgOnBoth = !useAvgOnBoth;
						useAvgOnFirst = true;
						childAtts[i] = (p1atts[i]+p2atts[i])*0.5d;
						System.out.println("used avgcrossover on attr ["+i+"] = "+childAtts[i] +" (P2TURN)");
						continue;
					}
				}
				
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

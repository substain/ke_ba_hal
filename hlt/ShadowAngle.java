package hlt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;


public class ShadowAngle {
	LinkedList<AngleArea> angleAreasSorted;
	double leftAngle;
	double rightAngle;

	public ShadowAngle(double leftAngle, double rightAngle) {
		angleAreasSorted = new LinkedList<>();
		this.leftAngle = leftAngle;
		this.rightAngle = rightAngle;
	}
	
	public void insertArea(AngleArea angAr) {
		for(int i = 0; i < angleAreasSorted.size(); i++) { //insert if is lower than a different area
			if(!angleAreasSorted.get(i).distInAreaOrHigher(angAr.getMinDist())) {
				angleAreasSorted.add(i, angAr);
				return;
			}
		}
		angleAreasSorted.add(angAr);
	}
	
	public void insertAreaLast(AngleArea angAr) { //if no distance check is needed
		angleAreasSorted.add(angAr);
	}
	
	/**
	 * splits the ShadowAngle into 2 angles, 
	 * @param angle the angle which divides this SA
	 * @return an array of 2 angles, where [0] is the right and [1] the left angle
	 */
	public ShadowAngle[] splitArea(double angle) {
		ShadowAngle[] splitShadowAngles = new ShadowAngle[2];
		splitShadowAngles[0] = new ShadowAngle(angle, rightAngle);
		splitShadowAngles[1] = new ShadowAngle(leftAngle, angle);
		for(AngleArea aa : angleAreasSorted) {
			AngleArea[] splitAreas = aa.splitAngleArea(angle);
			splitShadowAngles[0].insertArea(splitAreas[0]);
			splitShadowAngles[1].insertArea(splitAreas[1]);
		}
		
		return splitShadowAngles;
	}
	
	

}

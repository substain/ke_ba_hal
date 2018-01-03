package hlt;

import java.util.ArrayList;

public class LinkedSpace {
	private boolean isPl; //most of the space is occupied by a planet
	private int minX;
	private int maxX;
	private int minY;
	private int maxY;
	private ArrayList<LinkedSpace> bottomSpaces;
	private ArrayList<LinkedSpace> topSpaces;
	private ArrayList<LinkedSpace> leftSpaces;
	private ArrayList<LinkedSpace> rightSpaces;

	public LinkedSpace(int xMin, int xMax, int yMin, int yMax, boolean isPlanet) {
		minX = xMin;
		minY = yMin;
		maxX = xMax;
		maxY = yMax;
		bottomSpaces = new ArrayList<>();
		topSpaces = new ArrayList<>();
		leftSpaces = new ArrayList<>();
		rightSpaces = new ArrayList<>();
		isPl = isPlanet;
	}
	
	public LinkedSpace(Planet pl) {
		isPl = true;
		double f_radius = Constants.FORECAST_FUDGE_FACTOR + pl.getRadius();
		minX = (int) (pl.getXPos() - f_radius);
		maxX = (int) (pl.getXPos() + f_radius);
		minY = (int) (pl.getYPos() - f_radius);
		maxY = (int) (pl.getYPos() + f_radius);
		bottomSpaces = new ArrayList<>();
		topSpaces = new ArrayList<>();
		leftSpaces = new ArrayList<>();
		rightSpaces = new ArrayList<>();
	}
	
	public int getMinX() {
		return minX;
	}
	
	public int getMaxX() {
		return maxX;
	}
		
	public int getMinY() {
		return minY;
	}
		
	public int getMaxY() {
		return maxY;
	}
			
	public void addBottomSpace(LinkedSpace bs) {
		bottomSpaces.add(bs);
	}
	public void addTopSpace(LinkedSpace ts) {
		topSpaces.add(ts);
	}
	public void addLeftSpace(LinkedSpace ls) {
		leftSpaces.add(ls);
	}
	public void addRightSpace(LinkedSpace rs) {
		rightSpaces.add(rs);
	}
	
	public boolean isPlanet() {
		return isPl;
	}
	
	

}

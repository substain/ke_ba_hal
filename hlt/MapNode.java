package hlt;


public class MapNode implements Comparable<MapNode>{
	private int x;
	private int y;
	private double heur;
	private double gcost;
	private boolean isStart;
	private MapNode parent;
	
	public MapNode(int x, int y) {
		this.x = x;
		this.y = y;
		heur = Double.POSITIVE_INFINITY;
		gcost = Double.POSITIVE_INFINITY;
		parent = null;
		isStart = false;

	}
		
	public MapNode(int x, int y, MapNode target, MapNode parent) {
		this.x = x;
		this.y = y;
		gcost = Double.POSITIVE_INFINITY;
		heur = getEuclidDist(target);
		parent = null;
		isStart = false;
	}
	
	public MapNode(Position p) {
		this.x = (int) p.getXPos();
		this.y = (int) p.getYPos();
		isStart = false;

	}
	
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public void setH(double score) {
		heur = score;
	}
	
	public double getH() {
		return heur;
	}
	
	public void setCost(double c) {
		gcost = c;
	}
	
	public double getCost() {
		return gcost;
	}
	
	public double getF() {
		return gcost + heur;
	}
	
	public boolean isStart() {
		return isStart;
	}
	public void setAsStart() {
		isStart = true;
	}
	public MapNode getParent() {
		return parent;
	}
	
	public double getEuclidDist(MapNode target) { //serves as distance computation and heuristic
        final int dx = x - target.x;
        final int dy = y - target.y;
		return  Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
	}
	
	public boolean isEquivalentTo(Position p) {
		return(((int)p.getXPos() == x) && ((int)p.getYPos() == y));
	}
	
	
    @Override
    public boolean equals(final Object o) { //for "contains" operations in Collections
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MapNode mn = (MapNode) o;

        return ((mn.getX() == x) && (mn.getY() == y));
    }

	@Override
	public int compareTo(MapNode other) { //for the priority queue
	    return Double.compare(getF(), other.getF());
	}
	
}
/*
@Override
public int compare(MapNode m1, MapNode m2) {
	if(m1.getScore() < m2.getScore()) {
		return -1;
	} else if (m2.getScore() < m1.getScore()) {
		return 1;
	} else return 0;
} */


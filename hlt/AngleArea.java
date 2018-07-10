package hlt;

public class AngleArea {
	
	private double leftAngle;
	private double rightAngle;
	private double midAngle;
	private double minDist;
	private Position left;
	private Position right;
	
	
	public AngleArea(double d, Position leftPos, Position rightPos, double leftAngle, double rightAngle) {
		minDist = d;
		left = leftPos;
		right = rightPos;
		this.leftAngle = leftAngle;
		this.rightAngle = rightAngle;
		setMidAngle();
	}
	
	/**
	 * splits the angleArea into 2 angles, 
	 * @param angle the angle which divides this AA
	 * @return an array of 2 areas, where [0] is the right and [1] the left area
	 */
	public AngleArea[] splitAngleArea(double angle) {
		AngleArea[] splittedAreas = new AngleArea[2];
		splittedAreas[0] = new AngleArea(minDist, right, right, angle, rightAngle);
		splittedAreas[1] = new AngleArea(minDist, left, left, leftAngle, angle);
		return splittedAreas;
	}
	
	public boolean distInAreaOrHigher(double distance) {
		if(distance > minDist) {
			return true;
		}
		return false;
	}
	
	public Position getNextPosition(double angle) {
		if(angle >= midAngle) {
			return left;
		} else {
			return right;
		}
	}
	
	public double getMinDist() {
		return minDist;
	}
	
	public void setMidAngle() {
		double mid = (leftAngle + rightAngle)/2;
		if(Double.isNaN(mid)) {
			Log.log("AngleArea: D" + minDist + ", R" + rightAngle + ", L" + leftAngle + "has NaN mid");
		}
		midAngle = mid;
	}
	
	public double getMidAngle() {
		return midAngle;
	}
	
	public double getRightAngle() {
		return rightAngle;
	}
	public double getLeftAngle() {
		return leftAngle;
	}
	
	public Position getRightPos() {
		return right;
	}
	
	public Position getLeftPos() {
		return left;
	}
	
	public void setRightPos(Position pos) {
		right = pos;
	}
	
	public void setLeftPos(Position pos) {
		left = pos;
	}
	
	
	
}

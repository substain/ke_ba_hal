package hlt;

public class ThrustMove extends Move {

    private final int angleDeg;
    private final int thrust;

    public ThrustMove(final Ship ship, final int angleDeg, final int thrust) {
        super(MoveType.Thrust, ship);
        this.thrust = thrust;
        this.angleDeg = angleDeg;
    }

    public int getAngle() {
        return angleDeg;
    }

    public int getThrust() {
        return thrust;
    }
    
    
    public Position getExpectedPosition(Position origin) {
    	final double angularStepRad = Math.PI/180.0;
        
    	final double moveX = Math.cos(angleDeg + angularStepRad) * thrust;
        final double moveY = Math.sin(angleDeg + angularStepRad) * thrust;
        
    	return new Position(origin.getXPos() + moveX, origin.getYPos() + moveY);
    }
}

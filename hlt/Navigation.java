package hlt;

import java.util.ArrayList;

public class Navigation {
	
	public static final double angularStepRad = Math.PI/180.0;

    public static ThrustMove navigateShipToClosestPoint(
            final GameMap gameMap,
            final Ship ship,
            final Entity target,
            final int maxThrust,
            final ArrayList<Entity> obstructedPos
    		)
    {

        final Position targetPos = ship.getClosestPoint(target);

        return navigateShipToPoint(gameMap, ship, targetPos, maxThrust, obstructedPos);
    }
    
    public static ThrustMove navigateShipToPoint(
            final GameMap gameMap,
            final Ship ship,
            final Position targetPos,
            final int maxThrust,
            final ArrayList<Entity> obstructedPos

    		)
    {
        final int maxCorrections = Constants.MAX_NAVIGATION_CORRECTIONS;
        final boolean avoidObstacles = true;

        return navigateShipTowardsTarget(gameMap, ship, targetPos, maxThrust, avoidObstacles, maxCorrections, obstructedPos);
    }

    public static ThrustMove navigateShipTowardsTarget(
            final GameMap gameMap,
            final Ship ship,
            final Position targetPos,
            final int maxThrust,
            final boolean avoidObstacles,
            final int maxCorrections,
            final ArrayList<Entity> obstructedPos
    		)
    {
        if (maxCorrections <= 0) {
            return null;
        }

        final double distance = ship.getDistanceTo(targetPos);
        final double angleRad = ship.orientTowardsInRad(targetPos);

        if (avoidObstacles && !gameMap.objectsBetween2(ship, targetPos, obstructedPos).isEmpty()) {
            final double newTargetDx = Math.cos(angleRad + angularStepRad) * distance;
            final double newTargetDy = Math.sin(angleRad + angularStepRad) * distance;
            final Position newTarget = new Position(ship.getXPos() + newTargetDx, ship.getYPos() + newTargetDy);

            return navigateShipTowardsTarget(gameMap, ship, newTarget, maxThrust, true, (maxCorrections-1), obstructedPos);
        }

        final int thrust;
        if (distance < maxThrust) {
            // Do not round up, since overshooting might cause collision.
            thrust = (int) distance;
        }
        else {
            thrust = maxThrust;
        }

        final int angleDeg = Util.angleRadToDegClipped(angleRad);

        return new ThrustMove(ship, angleDeg, thrust);
    }

    
    
    
    public static ThrustMove navigateShipTowardsPathTarget(
            final GameMap gameMap,
            final Ship ship,
            final Position targetPos,
            final int maxThrust,
            final Position pathPos,
            final ArrayList<Entity> obstructedPos
            )
    {


        final double distance = ship.getDistanceTo(targetPos);
        final double angleRad = ship.orientTowardsInRad(targetPos);

        if (!gameMap.objectsBetween2(ship, targetPos, obstructedPos).isEmpty()) {

            return navigateShipTowardsTarget(gameMap, ship, pathPos, maxThrust, true, Constants.MAX_NAVIGATION_CORRECTIONS, obstructedPos);
        }

        final int thrust;
        if (distance < maxThrust) {
            // Do not round up, since overshooting might cause collision.
            thrust = (int) distance;
        }
        else {
            thrust = maxThrust;
        }

        final int angleDeg = Util.angleRadToDegClipped(angleRad);

        return new ThrustMove(ship, angleDeg, thrust);
    }
        
    public static Position getExpectedPos(Position origin, Position target, int speed) {
        final double angleRad = origin.orientTowardsInRad(target);
        
    	final double moveX = Math.cos(angleRad + angularStepRad) * speed;
        final double moveY = Math.sin(angleRad + angularStepRad) * speed;
        
    	return new Position(origin.getXPos() + moveX, origin.getYPos() + moveY);
    }
    

    
}

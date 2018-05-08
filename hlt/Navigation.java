package hlt;

import java.util.ArrayList;

public class Navigation {
	
	//   old PI/180
	public static final double angularStepRad = Math.PI/100.0;
	public static final double THRUST_DIV = 60;
	public static final double MAX_DIST = 8;

	
	
	
	
	
	

    public static ThrustMove navigateShipTowardsTargetLA(
            final GameMap gameMap,
            final Ship ship,
            final Position targetPos,
            final int maxThrust,
            final boolean avoidObstacles,
            final ArrayList<Entity> obstructedPos
    		)
    {
        
    	final double distance = ship.getDistanceTo(targetPos);
        final double angleRad = ship.orientTowardsInRad(targetPos);
        
        final double dthrust = Math.min(distance, maxThrust);

        final double targetDx = Math.cos(angleRad) * dthrust;
        final double targetDy = Math.sin(angleRad) * dthrust;
        
        Position tpos = new Position(ship.getXPos() + targetDx, ship.getYPos() + targetDy);
        
        Vector2D targetVector = new Vector2D(targetDx, targetDy);        
               
        for(Entity e : obstructedPos) {
        	double obstrDist = tpos.getDistanceTo(e);
        	double entDist = tpos.getDistanceTo(e);
        	if(obstrDist > Constants.NAV_CRIT && entDist > MAX_DIST) {
        		continue;
        	}
        	double orient = e.orientTowardsInRad(tpos);
            Vector2D inflVector = new Vector2D(Math.cos(orient) * obstrDist, Math.sin(orient) * obstrDist);
            inflVector.normalize();
            double dist_factor  = (Constants.NAV_CRIT-obstrDist)/Constants.NAV_CRIT;
        	double influence =dthrust*0.6;       	
        	double strength = influence * dist_factor;

        	inflVector.nscale(strength);
        	targetVector.add(inflVector);
        }
        
        targetVector.nscale(dthrust);



        Position newPos = new Position(ship.getXPos() + targetVector.getX(), ship.getYPos() + targetVector.getY());

        final int newThr = (int) Math.max(ship.getDistanceTo(newPos), maxThrust);
        final double newRad = ship.orientTowardsInRad(newPos);

        
        
        if (avoidObstacles && !gameMap.objectsBetween2(ship, newPos, obstructedPos).isEmpty()) {
            final double newTargetDx = Math.cos(newRad + angularStepRad) * newThr;
            final double newTargetDy = Math.sin(newRad + angularStepRad) * newThr;
            final Position newTarget = new Position(ship.getXPos() + newTargetDx, ship.getYPos() + newTargetDy);
            return navigateShipTowardsTarget(gameMap, ship, newTarget, newThr, true, Constants.MAX_NAVIGATION_CORRECTIONS, obstructedPos);
        }

        final int thrust;
        if (ship.getDistanceTo(newPos) < maxThrust) {
            // Do not round up, since overshooting might cause collision.
            thrust = (int) ship.getDistanceTo(newPos);
        }
        else {
            thrust = maxThrust;
        }

        final int angleDeg = Util.angleRadToDegClipped(newRad);

        return new ThrustMove(ship, angleDeg, thrust);
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
        
        int maxThrustChanged = maxThrust;
        if (maxCorrections <= 10) {
        	if(maxThrust>4) {
            	maxThrustChanged -= 1;
        	}
        }
        final double distance = ship.getDistanceTo(targetPos);
        final double angleRad = ship.orientTowardsInRad(targetPos);
        

        if (avoidObstacles && !gameMap.objectsBetween2(ship, targetPos, obstructedPos).isEmpty()) {
            final double newTargetDx = Math.cos(angleRad + angularStepRad) * distance;
            final double newTargetDy = Math.sin(angleRad + angularStepRad) * distance;
            final Position newTarget = new Position(ship.getXPos() + newTargetDx, ship.getYPos() + newTargetDy);

            return navigateShipTowardsTarget(gameMap, ship, newTarget, maxThrustChanged, true, (maxCorrections-1), obstructedPos);
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
        	//Log.log("Navigation: objects between ship, target pos (checking obstructedPos) is empty");
            //return navigateShipTowardsTargetLA(gameMap, ship, pathPos, maxThrust, true, Constants.MAX_NAVIGATION_CORRECTIONS, obstructedPos);
            return navigateShipTowardsTargetLA(gameMap, ship, pathPos, maxThrust, true, obstructedPos);

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
    
    public static ThrustMove navigateShipToPoint(
            final GameMap gameMap,
            final Ship ship,
            final Position targetPos,
            final int maxThrust,
            final ArrayList<Entity> obstructedPos

    		)
    {
        final boolean avoidObstacles = true;
        return navigateShipTowardsTargetLA(gameMap, ship, targetPos, maxThrust, avoidObstacles, obstructedPos);

        //return navigateShipTowardsTarget(gameMap, ship, targetPos, maxThrust, avoidObstacles, maxCorrections, obstructedPos);
    }

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

    
    
        
    public static Position getExpectedPos(Position origin, Position target, int speed) {
        final double angleRad = origin.orientTowardsInRad(target);
        
    	final double moveX = Math.cos(angleRad + angularStepRad) * speed;
        final double moveY = Math.sin(angleRad + angularStepRad) * speed;
        
    	return new Position(origin.getXPos() + moveX, origin.getYPos() + moveY);
    }
    
    



}

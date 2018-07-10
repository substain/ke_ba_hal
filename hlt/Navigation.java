package hlt;

import java.util.ArrayList;

public class Navigation {
	
	//   old PI/180
	public static final double angularStepRad = Math.PI/150.0;
	public static final double THRUST_DIV = 60;
	public static final double MAX_DIST = 8;
	
	public static final int maximalCorrections = Constants.MAX_NAVIGATION_CORRECTIONS;
	/*
	 *  final int maxCorrections = Constants.MAX_NAVIGATION_CORRECTIONS;
        final double angularStepRad = Math.PI/180.0;
        final Position targetPos = ship.getClosestPoint(dockTarget);

	 */

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

        
        
        if (avoidObstacles && !gameMap.objectsBetween(ship, newPos, true, obstructedPos, ship.getOwner()).isEmpty()) {
            final double newTargetDx = Math.cos(newRad + angularStepRad) * newThr;
            final double newTargetDy = Math.sin(newRad + angularStepRad) * newThr;
            final Position newTarget = new Position(ship.getXPos() + newTargetDx, ship.getYPos() + newTargetDy);
            return navigateShipTowardsTarget(gameMap, ship, newTarget, newThr, Constants.MAX_NAVIGATION_CORRECTIONS, obstructedPos);
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
        

        if (!gameMap.objectsBetween(ship, targetPos, true, obstructedPos, ship.getOwner()).isEmpty()) {
            final double newTargetDx = Math.cos(angleRad + angularStepRad) * distance;
            final double newTargetDy = Math.sin(angleRad + angularStepRad) * distance;
            final Position newTarget = new Position(ship.getXPos() + newTargetDx, ship.getYPos() + newTargetDy);

            return navigateShipTowardsTarget(gameMap, ship, newTarget, maxThrustChanged, (maxCorrections-1), obstructedPos);
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

        if (!gameMap.objectsBetween(ship, targetPos, true, obstructedPos, ship.getOwner()).isEmpty()) {
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
    
	//LOCAL AVOIDANCE#
	/*
    public static ThrustMove navigateShipTowardsTargetLA(
            final GameMap gameMap,
            final Ship ship,
            final Position targetPos,
            final int maxThrust,
            final ArrayList<Entity> obstructedPos,
            boolean targetIsPosition
    		)
    {
        
    	final double distance = ship.getDistanceTo(targetPos);
        final double angleRad = ship.orientTowardsInRad(targetPos);
        
        final double dthrust;
        
        if(!targetIsPosition) {
            dthrust = Math.min(distance - Constants.FORECAST_FUDGE_FACTOR, maxThrust);

        } else { 
        	dthrust = Math.min(distance, maxThrust);
        }

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

        
        
        if (!gameMap.objectsBetween(ship, newPos, true, obstructedPos, ship.getOwner()).isEmpty()) {
            final double newTargetDx = Math.cos(newRad + angularStepRad) * newThr;
            final double newTargetDy = Math.sin(newRad + angularStepRad) * newThr;
            final Position newTarget = new Position(ship.getXPos() + newTargetDx, ship.getYPos() + newTargetDy);
            return navigateShipTowardsTarget(gameMap, ship, newTarget, newThr,  obstructedPos);
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
            final ArrayList<Entity> obstructedPos
    		)
    {
         

        final double distance = ship.getDistanceTo(targetPos);
        final double angleRad = ship.orientTowardsInRad(targetPos);
        

        if (!gameMap.objectsBetween(ship, targetPos, true, obstructedPos, ship.getOwner()).isEmpty()) {
            final double newTargetDx = Math.cos(angleRad + angularStepRad) * distance;
            final double newTargetDy = Math.sin(angleRad + angularStepRad) * distance;
            final Position correctedPos = new Position(ship.getXPos() + newTargetDx, ship.getYPos() + newTargetDy);

            return navigateShipTowardsTargetRec(gameMap, ship, targetPos, correctedPos, maxThrust, maximalCorrections, true, obstructedPos);
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

        Log.log("Ship "+ ship.getId() + " flies to " + targetPos.toString());

        return new ThrustMove(ship, angleDeg, thrust);
    }

//recursive version with alternating corrections
    public static ThrustMove navigateShipTowardsTargetRec(
            final GameMap gameMap,
            final Ship ship,
            final Position targetPos, //the original target destination
            final Position correctedPos, //the tested correction
            final int maxThrust,
            final int maxCorrections,
            final boolean isLeftAngle, //left or right angle? alternate!
            final ArrayList<Entity> obstructedPos
    		)
    {

        
        final double distance = ship.getDistanceTo(targetPos);
        final double angleRad = ship.orientTowardsInRad(targetPos);
        

        if (!gameMap.objectsBetween(ship, correctedPos, true, obstructedPos, ship.getOwner()).isEmpty()) {
        	
            if (maxCorrections <= 0) {
                return null;
            }
            
            int maxThrustChanged = maxThrust;

            if (maxCorrections <= 5) {
            	if(maxThrust>2) {
                	maxThrustChanged -= 1;
            	}
            }

            if(maxCorrections == maximalCorrections/4 || maxCorrections == maximalCorrections/2 || maxCorrections == 3*(maximalCorrections/4)) {
                Log.log("Ship "+ ship.getId() + " slowing down");
            	maxThrustChanged -= 1;
            }
        	double angleRadAdd = (maxCorrections*angularStepRad);

        	boolean newAngleIsLeft;
        	if(isLeftAngle) {
        		newAngleIsLeft = false;
        	} else {
        		angleRadAdd = -angleRadAdd;
        		newAngleIsLeft = true;
        	}
            double newAngleRad = angleRad + angleRadAdd;
            
            Log.log("Ship "+ ship.getId() + " trying to get to " + targetPos.toString() + ", corrected to: " + correctedPos + ", maxCorr = " + (maxCorrections-1) + ", newRad: " + newAngleRad);

            final double newTargetDx = Math.cos(newAngleRad) * distance;
            final double newTargetDy = Math.sin(newAngleRad) * distance;
            final Position cPos = new Position(ship.getXPos() + newTargetDx, ship.getYPos() + newTargetDy);

            return navigateShipTowardsTargetRec(gameMap, ship, targetPos, cPos, maxThrustChanged, maxCorrections-1, newAngleIsLeft, obstructedPos);
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
        
        Log.log("Ship "+ ship.getId() + " flies to " + correctedPos.toString());

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

        if (!gameMap.objectsBetween(ship, targetPos, false, null, ship.getOwner()).isEmpty()) {
        	//Log.log("Navigation: objects between ship, target pos (checking obstructedPos) is empty");
            //return navigateShipTowardsTargetLA(gameMap, ship, pathPos, maxThrust, true, Constants.MAX_NAVIGATION_CORRECTIONS, obstructedPos);
            return navigateShipTowardsTarget(gameMap, ship, pathPos, maxThrust, obstructedPos);
        }
        
        return navigateShipTowardsTarget(gameMap, ship, targetPos, maxThrust, obstructedPos);
    }
    */
    public static ThrustMove navigateShipToPoint(
            final GameMap gameMap,
            final Ship ship,
            final Position targetPos,
            final int maxThrust,
            final ArrayList<Entity> obstructedPos
    		)
    {
        return navigateShipTowardsTarget(gameMap, ship, targetPos, maxThrust, maximalCorrections,obstructedPos);

        //return navigateShipTowardsTarget(gameMap, ship, targetPos, maxThrust, maxCorrections, obstructedPos);
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

        return navigateShipTowardsTarget(gameMap, ship, targetPos, maxThrust,maximalCorrections, obstructedPos);
    }
    
    
    
    public static Position getExpectedPos(Position origin, Position target, int speed) {
        final double angleRad = origin.orientTowardsInRad(target);
        
    	final double moveX = Math.cos(angleRad + angularStepRad) * speed;
        final double moveY = Math.sin(angleRad + angularStepRad) * speed;
        
    	return new Position(origin.getXPos() + moveX, origin.getYPos() + moveY);
    }

    

    /*

    auto collision_time(
    	    long double r,
    	    const hlt::Location& loc1, const hlt::Location& loc2,
    	    const hlt::Velocity& vel1, const hlt::Velocity& vel2
    	) -> std::pair<bool, double> {
    	    // With credit to Ben Spector
    	    // Simplified derivation:
    	    // 1. Set up the distance between the two entities in terms of time,
    	    //    the difference between their velocities and the difference between
    	    //    their positions
    	    // 2. Equate the distance equal to the event radius (max possible distance
    	    //    they could be)
    	    // 3. Solve the resulting quadratic

    	    const auto dx = loc1.pos_x - loc2.pos_x;
    	    const auto dy = loc1.pos_y - loc2.pos_y;
    	    const auto dvx = vel1.vel_x - vel2.vel_x;
    	    const auto dvy = vel1.vel_y - vel2.vel_y;

    	    // Quadratic formula
    	    const auto a = std::pow(dvx, 2) + std::pow(dvy, 2);
    	    const auto b = 2 * (dx * dvx + dy * dvy);
    	    const auto c = std::pow(dx, 2) + std::pow(dy, 2) - std::pow(r, 2);

    	    const auto disc = std::pow(b, 2) - 4 * a * c;

    	    if (a == 0.0) {
    	        if (b == 0.0) {
    	            if (c <= 0.0) {
    	                // Implies r^2 >= dx^2 + dy^2 and the two are already colliding
    	                return { true, 0.0 };
    	            }
    	            return { false, 0.0 };
    	        }
    	        const auto t = -c / b;
    	        if (t >= 0.0) {
    	            return { true, t };
    	        }
    	        return { false, 0.0 };
    	    }
    	    else if (disc == 0.0) {
    	        // One solution
    	        const auto t = -b / (2 * a);
    	        return { true, t };
    	    }
    	    else if (disc > 0) {
    	        const auto t1 = -b + std::sqrt(disc);
    	        const auto t2 = -b - std::sqrt(disc);

    	        if (t1 >= 0.0 && t2 >= 0.0) {
    	            return { true, std::min(t1, t2) / (2 * a) };
    	        } else if (t1 <= 0.0 && t2 <= 0.0) {
    	            return { true, std::max(t1, t2) / (2 * a) };
    	        } else {
    	            return { true, 0.0 };
    	        }
    	    }
    	    else {
    	        return { false, 0.0 };
    	    }
    	}

    	auto collision_time(long double r, const hlt::Ship& ship1, const hlt::Ship& ship2) -> std::pair<bool, long double> {
    	    return collision_time(r,
    	                          ship1.location, ship2.location,
    	                          ship1.velocity, ship2.velocity);
    	}

    	auto collision_time(long double r, const hlt::Ship& ship1, const hlt::Planet& planet) -> std::pair<bool, long double> {
    	    return collision_time(r,
    	                          ship1.location, planet.location,
    	                          ship1.velocity, { 0, 0 });
    	}

    	auto might_attack(long double distance, const hlt::Ship& ship1, const hlt::Ship& ship2) -> bool {
    	    return distance <= ship1.velocity.magnitude() + ship2.velocity.magnitude()
    	        + ship1.radius + ship2.radius
    	        + hlt::GameConstants::get().WEAPON_RADIUS;
    	}

    	auto might_collide(long double distance, const hlt::Ship& ship1, const hlt::Ship& ship2) -> bool {
    	    return distance <= ship1.velocity.magnitude() + ship2.velocity.magnitude() +
    	        ship1.radius + ship2.radius;
    	}

    	auto round_event_time(double t) -> double {
    	    return std::round(t * EVENT_TIME_PRECISION) / EVENT_TIME_PRECISION;
    	}

    	auto find_events(
    	    std::unordered_set<SimulationEvent>& unsorted_events,
    	    const hlt::EntityId id1, const hlt::EntityId& id2,
    	    const hlt::Ship& ship1, const hlt::Ship& ship2) -> void {
    	    const auto distance = ship1.location.distance(ship2.location);
    	    const auto player1 = id1.player_id();
    	    const auto player2 = id2.player_id();

    	    if (player1 != player2 && might_attack(distance, ship1, ship2)) {
    	        // Combat event
    	        const auto attack_radius = ship1.radius +
    	            ship2.radius + hlt::GameConstants::get().WEAPON_RADIUS;
    	        const auto t = collision_time(attack_radius, ship1, ship2);
    	        if (t.first && t.second >= 0 && t.second <= 1) {
    	            unsorted_events.insert(SimulationEvent{
    	                SimulationEventType::Attack,
    	                id1, id2, round_event_time(t.second),
    	            });
    	        }
    	        else if (distance < attack_radius) {
    	            unsorted_events.insert(SimulationEvent{
    	                SimulationEventType::Attack,
    	                id1, id2, 0
    	            });
    	        }
    	    }

    	    if (id1 != id2 && might_collide(distance, ship1, ship2)) {
    	        // Collision event
    	        const auto collision_radius = ship1.radius + ship2.radius;
    	        const auto t = collision_time(collision_radius, ship1, ship2);
    	        if (t.first) {
    	            if (t.second >= 0 && t.second <= 1) {
    	                unsorted_events.insert(SimulationEvent{
    	                    SimulationEventType::Collision,
    	                    id1, id2, round_event_time(t.second),
    	                });
    	            }
    	        }
    	        else if (distance < collision_radius) {
    	            // This should never happen - the ships should already be dead
    	            assert(false);
    	        }
    	    }
    	}*/

    
    /*
     *  //OLD
     *     public static ThrustMove navigateShipTowardsPathTarget(
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
     * 
     * 
     */
}

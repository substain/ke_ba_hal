package hlt;

public class Collision {
    /**
     * Test whether a given line segment intersects a circular area.
     *
     * @param start  The start of the segment.
     * @param end    The end of the segment.
     * @param circle The circle to test against.
     * @param fudge  An additional safety zone to leave when looking for collisions. Probably set it to ship radius.
     * @return true if the segment intersects, false otherwise
     */
    public static boolean segmentCircleIntersect(final Position start, final Position end, final Entity circle, final double fudge) {
        // Parameterize the segment as start + t * (end - start),
        // and substitute into the equation of a circle
        // Solve for t
        final double circleRadius = circle.getRadius();
        final double startX = start.getXPos();
        final double startY = start.getYPos();
        final double endX = end.getXPos();
        final double endY = end.getYPos();
        final double centerX = circle.getXPos();
        final double centerY = circle.getYPos();
        final double dx = endX - startX;
        final double dy = endY - startY;

        final double a = square(dx) + square(dy);

        final double b = -2 * (square(startX) - (startX * endX)
                            - (startX * centerX) + (endX * centerX)
                            + square(startY) - (startY * endY)
                            - (startY * centerY) + (endY * centerY));

        if (a == 0.0) {
            // Start and end are the same point
            return start.getDistanceTo(circle) <= circleRadius + fudge;
        }

        // Time along segment when closest to the circle (vertex of the quadratic)
        final double t = Math.min(-b / (2 * a), 1.0);
        if (t < 0) {
            return false;
        }

        final double closestX = startX + dx * t;
        final double closestY = startY + dy * t;
        final double closestDistance = new Position(closestX, closestY).getDistanceTo(circle);

        return closestDistance <= circleRadius + fudge;
    }

    public static double square(final double num) {
        return num * num;
    }
    
    public static boolean pointInsideCircle(final Position point, final Entity circle, final double fudge) {
    	final double px = point.getXPos();
    	final double py = point.getYPos();
        final double safeCircleRad = circle.getRadius() + fudge;
        final double circleCX = circle.getXPos();
        final double circleCY = circle.getYPos();
        
        final double dx = Math.abs(px - circleCX);
        final double dy = Math.abs(py - circleCY);
        
        if(dx > safeCircleRad || dy > safeCircleRad) {
        	return true;
        }
        if(dx+dy <= safeCircleRad) {
        	return false;
        	
        }
        
        if(square(dx) + square(dy) > square(safeCircleRad)){
        	return true;
        } else {
        	return false;
        }
    }
    
}

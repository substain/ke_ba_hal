package hlt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.PriorityQueue;

public class PathFinder {

	private boolean[][] hmap;
	private MapNode target;
	private MapNode origin;
	
	public PathFinder(boolean[][] hitmap) {
		hmap = hitmap;
	}
	
	public void updateMap(boolean[][] hitmap) {
		hmap = hitmap;
	}
	
	public ArrayList<MapNode> aStar(Position start, Position dest) {
		origin = new MapNode(start);
		target = new MapNode(dest);

		origin.setAsStart();
		origin.setCost(0);
		//target.setCost(Double.POSITIVE_INFINITY);
		return computeAStar();
	}
	
	public ArrayList<MapNode> computeAStar() {
		
		PriorityQueue<MapNode> openSet = new PriorityQueue<>();
		HashSet<MapNode> closedSet = new HashSet<>();
		openSet.add(origin);

		while(!openSet.isEmpty()) {
			MapNode currentPos = openSet.poll(); // get first element of the open list
				
			closedSet.add(currentPos); //here or before equal-test?
			
			ArrayList<MapNode> neighbors = getNeighbors(currentPos);
			for(MapNode mn : neighbors) {
				
				if(mn.equals(target)) {
					return parsePath(mn);
				}
				if(closedSet.contains(mn)) {
					continue;
				}
				
				mn.setCost(mn.getEuclidDist(currentPos) + currentPos.getCost());
				
				if(!openSet.contains(mn)){
					openSet.add(mn);
				}
			}

			 
			 

		}
		
		return null;
		
	}
	
	public ArrayList<MapNode> getNeighbors(MapNode mn){ //lists valid neighbors (performs hitmap-check)
		ArrayList<MapNode> neighbors = new ArrayList<>();
		int xMax = hmap.length;
		int yMax = hmap[0].length;
		int cx = mn.getX();
		int cy = mn.getY();
		boolean yNotZero = false; 
		boolean yNotMax = false;

		if(cy != 0) {
			if(hmap[cx][cy-1] == false) {
				neighbors.add(new MapNode(cx,cy-1, target, mn));
			}
			yNotZero = true;
		}
		if(cy < yMax) {
			if(hmap[cx][cy+1] == false) {
				neighbors.add(new MapNode(cx,cy+1, target, mn));
			}
			yNotMax = true;
		}
		if(cx != 0) {
			if(hmap[cx-1][cy] == false) {
				neighbors.add(new MapNode(cx-1,cy, target, mn));
			}
			neighbors.add(new MapNode(cx-1,cy));
			if(yNotZero && (hmap[cx-1][cy-1] == false)) {
				neighbors.add(new MapNode(cx-1,cy-1, target, mn));
			} 
			if(yNotMax && (hmap[cx-1][cy+1] == false)) {
				neighbors.add(new MapNode(cx-1,cy+1, target, mn));
			}
		}
		if(cx < xMax) {
			if(hmap[cx+1][cy] == false) {
				neighbors.add(new MapNode(cx+1,cy, target, mn));
			}

			if(yNotZero && (hmap[cx+1][cy-1] == false)) {
				neighbors.add(new MapNode(cx+1,cy-1, target, mn));
			}
			if(yNotMax && (hmap[cx+1][cy+1] == false)) {
				neighbors.add(new MapNode(cx+1,cy+1, target, mn));
			}
		}

		return neighbors;

	}
	
	public ArrayList<MapNode> parsePath(MapNode lastNode){
		
		//ArrayList<Position> resPath = new ArrayList<>();
		ArrayList<MapNode> resPath = new ArrayList<>();

		//resPath.add(new Position(lastNode));
		resPath.add(lastNode);
		
		MapNode currentNode = lastNode;

        while (!currentNode.isStart()) {
            currentNode = currentNode.getParent();
            
        	//resPath.add(new Position(currentNode));
        	resPath.add(currentNode);
        }
        Collections.reverse(resPath);
        return resPath;
	}

	
}

/*
function A*(start, goal)


// For each node, which node it can most efficiently be reached from.
// If a node can be reached from many nodes, cameFrom will eventually contain the
// most efficient previous step.
cameFrom := an empty map

// For each node, the cost of getting from the start node to that node.
gScore := map with default value of Infinity

// The cost of going from start to start is zero.
gScore[start] := 0

// For each node, the total cost of getting from the start node to the goal
// by passing by that node. That value is partly known, partly heuristic.
fScore := map with default value of Infinity

// For the first node, that value is completely heuristic.
fScore[start] := heuristic_cost_estimate(start, goal)

while openSet is not empty
    current := the node in openSet having the lowest fScore[] value
    if current = goal
        return reconstruct_path(cameFrom, current)

    openSet.Remove(current)
    closedSet.Add(current)

    for each neighbor of current
        if neighbor in closedSet
            continue		// Ignore the neighbor which is already evaluated.

        if neighbor not in openSet	// Discover a new node
            openSet.Add(neighbor)
        
        // The distance from start to a neighbor
        //the "dist_between" function may vary as per the solution requirements.
        tentative_gScore := gScore[current] + dist_between(current, neighbor)
        if tentative_gScore >= gScore[neighbor]
            continue		// This is not a better path.

        // This path is the best until now. Record it!
        cameFrom[neighbor] := current
        gScore[neighbor] := tentative_gScore
        fScore[neighbor] := gScore[neighbor] + heuristic_cost_estimate(neighbor, goal) 

return failure

function reconstruct_path(cameFrom, current)
total_path := [current]
while current in cameFrom.Keys:
    current := cameFrom[current]
    total_path.append(current)
return total_path*/

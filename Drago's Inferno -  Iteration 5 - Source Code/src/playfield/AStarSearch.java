/*
 *
 * Drago's Inferno - Iteration 5
 * AStarSearch.java
 * @author Elizabeth Collins
 *
 */

package playfield;

import java.util.*;

import playfield.BSPNode;
import playfield.LeafNode;

public class AStarSearch {

    // Node to hold the root of the BSP tree.
    BSPNode tree;

    // Priority type list to sort List into shortest distance order.
    public static class PriorityList extends LinkedList<AStarNode> {

        // Compare cost to other nodes in list and add in order (from shortest to longest).
        // Note: I had to make this boolean because the signature matched a pre-existing
        //   Java library function. Not sure if this was an error on Brackeen's part. -Scott
        public boolean add(AStarNode node) {
            for (int i = 0; i < size(); ++i) {
                if (node.compareTo(get(i)) <= 0) {
                    add(i, node);
                    return true;
                }
            }

            // If no longer distance nodes are found, add node to the end of the list.
            addLast(node);
            return true;
        }
    }

    // Method to create path list for results of A* search.
    protected LinkedList<AStarNode> constructPath(AStarNode node) {

        // Create a new list to hold the path.
        LinkedList<AStarNode> path = new LinkedList<AStarNode>();

        // Add portals to the list in reverse order until the first portal is reached.
        while (node.pathParent != null) {
            path.addFirst(node);
            node = node.pathParent;
        }

        // Return the finished path.
        return path;
    }

    // Class constructor that needs a link to the root node of the BSP tree.
    public AStarSearch(BSPNode root) { this.tree = root; }

    // Method to find the best path (A* Search).
    public LinkedList<AStarNode> findPath(int startx, int starty,
            int goalx, int goaly,
            Playfield pf)
    {
        // To prevent them from trying to search through the entire map.
        final int CYCLE_LIMIT = 200;
        
        // Find the BSP leaves where the beginning and end of the path are.
        BSPLeaf start = BSPNode.getLeaf(tree, startx, starty);
        BSPLeaf goal = BSPNode.getLeaf(tree, goalx, goaly);
        
        // Make sure that the leaf selected isn't null.
        // FIXME: This probably isn't the most elegant way to do this, and
        //   there are boundary cases (small entities) where it might fail.
        //   Right now, it just checks nearby points to find a leaf to start
        //   with.
        if (start == null) start = BSPNode.getLeaf(tree, startx + 2, starty + 1);
        if (start == null) start = BSPNode.getLeaf(tree, startx - 2, starty - 1);
        if (goal == null) goal = BSPNode.getLeaf(tree, goalx + 2, goaly + 1);
        if (goal == null) goal = BSPNode.getLeaf(tree, goalx - 2, goaly - 1);
        
        // Construct LeafNodes from these (they inherit from AStarNode and can thus
        //   be attached to the graph).
        LeafNode startNode = new LeafNode(start, new Fieldpoint(startx, starty));
        LeafNode goalNode = new LeafNode(goal, new Fieldpoint(goalx, goaly));
        
        // If start and goal node are in the same leaf (or same grid square, if
        //   the leaf is of type TREE_STONE_POLY), return a path straight in
        //   that direction.
        if (    (start == goal)
             && (    (start.getProperty() != LeafProperty.TREE_STONE_POLY)
                  || ((startx>>5) == (goalx>>5) && (starty>>5) == (goaly>>5)) ) )
        {
            LinkedList<AStarNode> path = new LinkedList<AStarNode>();
            path.addFirst(goalNode);
            path.addFirst(startNode);
            return path;
        }
        
        // Temporarily attach to A* graph.
        ArrayList<AStarNode> startNeighbors = new ArrayList<AStarNode>();
        ArrayList<AStarNode> goalNeighbors = new ArrayList<AStarNode>();
        for (int i = 0; i < 2; ++i) {
            BSPLeaf leaf = (i == 0) ? start : goal;
            AStarNode node = (i == 0) ? startNode : goalNode;
            ArrayList<AStarNode> neighbors = (i == 0) ? startNeighbors : goalNeighbors;
            int nodex = (i == 0) ? startx : goalx;
            int nodey = (i == 0) ? starty : goaly;
            if (leaf.getProperty() == LeafProperty.TREE_STONE_POLY) {
                PortalSquare ps = (PortalSquare)pf.gameGrid[nodex >> 5][nodey >> 5]; // Could not be cast to PortalSquare: why?
                for (int side = 0; side < 4; ++side) {
                    if (ps.getPortal(side) != null) { 
                        neighbors.add(ps.getPortal(side));
                        ps.getPortal(side).addNeighbor(node);
                    }
                }
            }
            else {
                for (int j = 0; j < leaf.getNumPortals(); ++j) {
                    neighbors.add(leaf.getPortal(j));
                }
            }
            for (int j = 0; j < neighbors.size(); ++j) {
                node.addNeighbor(neighbors.get(j));
                neighbors.get(j).addNeighbor(node);
            }
        }
        
        // Create variables.
        PriorityList openList = new PriorityList();
        LinkedList<AStarNode> closedList = new LinkedList<AStarNode>();
        //LinkedList<AStarNode> traveledPath;
        
        startNode.costFromStart = 0;
        startNode.estimatedCostToGoal = startNode.getEstimatedCost(goalNode);
        startNode.pathParent = null;
        openList.add(startNode);

        int nodex = 0;
        int nodey = 0;
        int cycles = 0;

        boolean found = false;
        while (!openList.isEmpty()) {
            
            AStarNode node = (AStarNode)openList.removeFirst();

            //System.out.print("Node moving from (" + nodex + ", " + nodey + ")");
            if (node instanceof Portal) {
                Portal portal = (Portal)node;
                nodex = portal.getMidpoint().x;
                nodey = portal.getMidpoint().y;
            }
            else if (node instanceof LeafNode) {
                LeafNode leafnode = (LeafNode)node;
                nodex = leafnode.location.x;
                nodey = leafnode.location.y;
            }
            //System.out.println(" to (" + nodex + ", " + nodey + ")");
            
            if (node == goalNode) {
                found = true;
                break;
            }
            
            ArrayList<AStarNode> neighbors = node.getNeighbors();
            for (int i = 0; i < neighbors.size(); ++i) {
                
                // Get the neighbor node.
                AStarNode neighborNode = (AStarNode)neighbors.get(i);
                
                // Skip closed portals.
                if (neighborNode instanceof Portal) {
                    Portal portal = (Portal)neighborNode;
                    if (!portal.isOpen()) continue;
                }
                
                // Get open/closed status of neighbor found.
                boolean isOpen = openList.contains(neighborNode);
                boolean isClosed = closedList.contains(neighborNode);
                int costFromStart = node.costFromStart + node.getCost(neighborNode);
                
                // Check if the neighbor node has not been traversed or
                //   if a shorter path to this neighbor node is found.
                if (    (!isOpen && !isClosed)
                     || (costFromStart < neighborNode.costFromStart) )
                {
                    neighborNode.pathParent = node;
                    neighborNode.costFromStart = costFromStart;
                    neighborNode.estimatedCostToGoal = neighborNode.getEstimatedCost(goalNode);
                    if (isClosed) {
                        closedList.remove(neighborNode);
                    }
                    if (!isOpen) {
                        openList.add(neighborNode);
                    }
                    
                } // end if (node is unexplored, or shorter path found to it)
            } // end for (by neighbor of current node)
            
            // Add node to 'closed' list.
            closedList.add(node);
            
            // Increment cycles. If too many, abort A* search.
            ++cycles;
            if (cycles > CYCLE_LIMIT) break;
        
        } // end while (A* search loop)
        
        // Delete temporary start and goal nodes from A* graph.
        for (int i = 0; i < startNeighbors.size(); ++i) {
            startNeighbors.get(i).removeNeighbor(startNode);
            startNode.removeNeighbor(startNeighbors.get(i));
        }
        for (int i = 0; i < goalNeighbors.size(); ++i) {
            goalNeighbors.get(i).removeNeighbor(goalNode);
            goalNode.removeNeighbor(goalNeighbors.get(i));
        }
        
        // Return path if found; otherwise return null.
        if (found) {
            //System.out.println("Path found; reconstructing.");
            return constructPath(goalNode);
        } else {
            return null;
        }
        
    } // end method findPath
        

    /*
    
        
        // or each portal of the given leaf. . .
        for(int i=0; i < temp.getNumPortals(); ++i) {
            nextPortal = temp.getPortal(i);

            //if the portal is open, assign costs and parent to it and add it to the list
            if(portal.isOpen()) {
                dx = nextPortal.midpoint.x - startx;
                dy = nextPortal.midpoint.y - starty;
                nextPortal.costFromStart = DragoStatics.sqrt(dx*dx + dy*dy);
                
                dx = nextPortal.midpoint.x - goalx;
                dy = nextPortal.midpoint.y - goaly;
                nextPortal.estimatedCostToGoal = DragoStatics.sqrt(dx*dx + dy*dy);
                nextPortal.pathParent = null;

                openList.add(nextPortal);
             }
        }

        //remove first portal from the list and add the leaf to the traveled path
        prevPortal = openList.removeFirst();
        traveledPath.add(temp);

        //determine which leaf is next on the path and assign it to temp
        if(temp == prevPortal.getFrontLeaf()) {
            temp = prevPortal.getBackLeaf();
        }
        else {
            temp = prevPortal.getFrontLeaf();
        }

        //continue the search until list is empty
        while(!openList.isEmpty()) {

            //as long as the current leaf is not the goal. . .
            if(temp != goal) {

                //for each portal of the given leaf. . .
                for(int i=0; i < temp.getNumPortals(); ++i) {
                    nextPortal = temp.getPortal(i);

                    //if the portal is open and it is not already in the list, assign costs and parent to it and add it to the list
                    if(portal.isOpen() && !openList.contains(nextPortal)) {
                        nextPortal.costFromStart = prevPortal.costFrom Start + nextPortal.getEstimatedCost(prevPortal);

                        dx = nextPortal.midpoint.x - goalx;
                        dy = nextPortal.midpoint.y - goaly;
                        nextPortal.estimatedCostToGoal = DragoStatics.sqrt(dx*dx + dy*dy);
                        nextPortal.pathParent = prevPortal;

                        openList.add(nextPortal);
                    }
                    //if the portal is open, but already on the list. . .
                    else if(portal.isOpen()) {
                        //check the new cost against the current cost
                        int cost = prevPortal.costFromStart + nextPortal.getEstimatedCost(prevPortal);

                        //if the new cost is less, change the cost and parent
                        if(cost < nextPortal.costFromStart) {
                            nextPortal.costFromStart = cost;
                            nextPortal.pathParent = prevPortal;
                        }
                    }
                }

                //remove portal with next shortest path from the list and add the leaf to the traveled path
                prevPortal = openList.removeFirst();
                traveledPath.add(temp);

                //determine which leaf is next on the path and assign it to temp
                if(traveledPath.contains(prevPortal.getFrontLeaf()) {
                    temp = prevPortal.getBackLeaf();
                }
                else {
                    temp = prevPortal.getFrontLeaf();
                }
            }
            //if the current leaf is the goal, construct the path from the final portal
            else {
                return constructPath(prevPortal);
            }
        }
        //if no path is found, return null
        return null;
    }

    */
}
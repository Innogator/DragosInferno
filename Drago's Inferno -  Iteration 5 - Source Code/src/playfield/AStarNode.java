/*
 * AStarNode.java
 * 3/17/2013
 */
package playfield;

import java.util.ArrayList;

import drago.DragoStatics;

// *Liz*:
//
//   Here is the AStarNode class, which Portal now extends. Implementation
// should proceed what is shown in Brackeen's book, though there may be
// instances where certain method calls need to be spelled out (for exmaple,
// there is no 'getNeighbors()' method of class Portal, but you can still
// run through and get all the neighbors using the indexed 'getNeighbor()').
// Also, we will be using 'int' rather than 'float' for our cost calculations.
//
//   Feel free to create any additional classes and methods that you need, so
// long as they do not impede existing functionality. You can call me at
// (850) 575-7484 if you have any difficulties.
//
// -Scott
//
abstract public class AStarNode
{
    public AStarNode pathParent;
    public int costFromStart;
    public int estimatedCostToGoal;
    private ArrayList<AStarNode> neighbors;
    
    // Abstract constructor.
    public AStarNode() {
        neighbors = new ArrayList<AStarNode>();
    }
    
    public int getCost() {
        return costFromStart + estimatedCostToGoal;
    }
    
    public int compareTo(AStarNode other) {
        int otherValue = other.getCost();
        int thisValue = this.getCost();
        return DragoStatics.sign(thisValue - otherValue);
    }
    
    public abstract int getCost(AStarNode node);
    public abstract int getEstimatedCost(AStarNode node);
    
    public ArrayList<AStarNode> getNeighbors() {
        return neighbors;
    }
    
    public AStarNode getNeighbor(int i) {
        return neighbors.get(i);
    }
        
    public void addNeighbor(AStarNode newNeighbor) {
        if (!neighbors.contains(newNeighbor)) {
            neighbors.add(newNeighbor);
        }
    }
    
    public void removeNeighbor(AStarNode oldNeighbor) {
        neighbors.remove(oldNeighbor);
    }
    
    public int numNeighbors() { return neighbors.size(); }
}

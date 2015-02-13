/*
 * LeafNode.java
 * 4/14/2013
 */
package playfield;

import java.util.ArrayList;

import drago.DragoStatics;

import playfield.BSPLeaf;
import playfield.Portal;
import playfield.Fieldpoint;

/**
 * Used to refer to a location in a leaf for the purposes
 * of A* search.
 * 
 * @author Quothmar
 *
 */
public class LeafNode extends AStarNode
{
    public BSPLeaf leaf;
    public Fieldpoint location;
    
    public LeafNode(BSPLeaf leaf, Fieldpoint location) {
        super();
        this.leaf = leaf;
        this.location = location;
    }
    
    public int getCost(AStarNode node) {
        return getEstimatedCost(node);
    }
    
    public int getEstimatedCost(AStarNode node) {
        int otherx;
        int othery;
        if (node instanceof Portal) {
            Portal other = (Portal)node;
            otherx = other.getMidpoint().x;
            othery = other.getMidpoint().y;
        }
        else {
            LeafNode other = (LeafNode)node;
            otherx = other.location.x;
            othery = other.location.y;
        }
        int dx = location.x - otherx;
        int dy = location.y - othery;
        return DragoStatics.sqrt(dx*dx + dy*dy);
    }
    
    /*
    public ArrayList<AStarNode> getNeighbors() {
        ArrayList<AStarNode> neighbors = new ArrayList<AStarNode>();
        for (int i = 0; i < leaf.getNumPortals(); ++i) {
            neighbors.add(leaf.getPortal(i));
        }
        return neighbors;
    }
    */
    
}

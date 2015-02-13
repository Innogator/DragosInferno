/*
 * Portal.java (v1.0)
 * 3/17/2013
 */
package playfield;

import java.util.List;
import java.util.Vector;
import java.util.ArrayList;

import drago.DragoStatics;

/**
 * A portal, used as a node of the A* graph and also as a
 * stepping stone (see BSPLeaf) in the AI search for a path
 * to a given location.
 * 
 * Can be 'open' (active) or 'closed' (inactive).
 * 
 * @author Quothmar
 *
 */
public class Portal extends AStarNode {
    
    private int length;
    private Fieldpoint midpoint;
	private LineSeg segment;
	private Boolean open;
	private BSPLeaf frontLeaf;
	private BSPLeaf backLeaf;
	
	// Data member added to allow the clearing of unused portals from
	//   the level.
	public ArrayList<BSPNode> parentNodes;

	// Creates an open portal from (x1, y1) to (x2, y2).
	public Portal(int x1, int y1, int x2, int y2) {
	    super();
		segment = new LineSeg(x1, y1, x2, y2);
		midpoint = new Fieldpoint((x1 + x2)>>1, (y1 + y2)>>1);
		length = DragoStatics.sqrt((x2 - x1)*(x2 - x1) + (y2 - y1)*(y2 - y1));
		open = true;
		frontLeaf = null;
		backLeaf = null;
		parentNodes = new ArrayList<BSPNode>();
	}
	
	public Fieldpoint getMidpoint() {
		return midpoint;
	}
	
	// An alternative to 'getMidpoint': get a point on the portal
	//   using the modulus of the 'x' or 'y' length (whichever is
	//   the largest).
	public Fieldpoint getPoint(int dividend) {
	    int x1 = this.getSegment().x1();
	    int y1 = this.getSegment().y1();
	    int x2 = this.getSegment().x2();
	    int y2 = this.getSegment().y2();
	    int dx = x2 - x1;
	    int dy = y2 - y1;
	    int x;
	    int y;
	    if (Math.abs(dx) > Math.abs(dy)) {
	        int points = dividend % Math.abs(dx);
	        if (points < 16) points = 16;
	        if (points > length - 16) points = length - 16;
	        x = x1 + points*DragoStatics.sign(dx);
	        y = y1 + (points*dy) / Math.abs(dx);
	    }
	    else {
            int points = dividend % Math.abs(dy);
            if (points < 16) points = 16;
            if (points > length - 16) points = length - 16;
            y = y1 + points*DragoStatics.sign(dy);
            x = x1 + (points*dx) / Math.abs(dy);
	    }
	    return new Fieldpoint(x, y);
	}

	public LineSeg getSegment() {
		return segment;
	}

	public Boolean isOpen() {
		return open;
	}

	public BSPLeaf getFrontLeaf() {
		return frontLeaf;
	}

	public BSPLeaf getBackLeaf() {
		return backLeaf;
	}
	
	public void setFrontLeaf(BSPLeaf leaf) {
		frontLeaf = leaf;
	}
	
	public void setBackLeaf(BSPLeaf leaf) {
		backLeaf = leaf;
	}

	public void setSegment(LineSeg aLine) {
		segment = aLine;
		
		// Midpoint is in the center of the portal (for enemies to walk to).
		midpoint.x = (segment.startingFieldpoint.x + segment.endingFieldpoint.x) / 2;
		midpoint.y = (segment.startingFieldpoint.y + segment.endingFieldpoint.y) / 2;
	}

	public void open() {
		open = true;
	}

	public void close() {
		open = false;
	}
	
	// More functions added for A* search.
	
	public int getCost(AStarNode node) {
	    return getEstimatedCost(node);
	}
	
	public int getEstimatedCost(AStarNode node) {
	    if (node instanceof Portal) {
	        Portal other = (Portal)node;
	        int dx = this.midpoint.x - other.midpoint.x;
	        int dy = this.midpoint.y - other.midpoint.y;
	        return DragoStatics.sqrt(dx*dx + dy*dy);
	    }
	    else return node.getEstimatedCost(this);
	}
}

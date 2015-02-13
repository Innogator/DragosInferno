/*
 * BSPLeaf.java (v1.0)
 * 3/17/2013
 */
package playfield;

import java.util.Vector;

/**
 * A leaf of the BSP tree, representing a final region of
 * the playfield created by the process of successive subdivision.
 * BSP leaves are always convex polygons. Along with portals,
 * they are used as the "stepping stones" of an enemy's AI
 * search for the player.
 * 
 * @author Quothmar/John Thrasher
 *
 */
public class BSPLeaf extends BSPNode {
	private LeafProperty property;
	//private Vector<Portal> portals;
	//private int numPortals;

	public BSPLeaf() {
	    super();
		property = null;
		portals = new Vector<Portal>();
		numPortals = 0;
	}

	public BSPLeaf(LeafProperty aProperty) {
		property = aProperty;
	}

	public LeafProperty getProperty() {
		return property;
	}

	public Portal getPortal(int x) {
		return portals.get(x);
	}

	public void removePortal(Portal ptl) {
	    if (portals.contains(ptl)) {
	        portals.remove(ptl);
	        for (int i = 0; i < ptl.parentNodes.size(); ++i) {
	            ptl.parentNodes.get(i).removePortal(ptl);
	        }
	        --numPortals;
	    }
	}
	
	public int getNumPortals() {
		return numPortals;
	}
	
	public void setProperty(LeafProperty aProperty) {
		property = aProperty;
	}

	public void addPortal(Portal aPortal) {
	    if (!portals.contains(aPortal)) {
    		portals.add(aPortal);
    		++numPortals;
	    }
	}
}

/*
 * PortalSquare.java (v1.0)
 * 3/17/2013
 */
package playfield;

/**
 * A square of the game grid additionally enclosed by four portals.
 * These portals are used in place of the ones generated directly
 * by the BSP in forested regions. Their function is to allow enemy
 * humans to find a path toward the player in the event that the
 * player burns a path through a forest or breaks through a wall
 * of stones.
 * 
 * Each square has a reference to four portals (the portals themselves
 * are shared with neighboring squares -- i.e., they are not unique
 * to the square).
 * 
 * @author Quothmar
 *
 */
public class PortalSquare extends MapGridSquare {
	
	private Portal portals[];	// Index can be UPPER, RIGHT, LOWER, LEFT.

	// Just calls parent constructor without creating portals, as two
	//   neighboring PortalSquares can share a common portal.
	public PortalSquare() {
		super();
		portals = new Portal[4];
	}

	public Portal getPortal(int side) {
		return portals[side];
	}
	
	// Adds a reference to an existing portal (does *not* create a new
	//   portal).
	public void addPortal(Portal portal, int side) {
		portals[side] = portal;
	}

} // end class PortalSquare

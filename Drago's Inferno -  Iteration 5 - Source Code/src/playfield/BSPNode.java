/*
 * BSPNode.java (v1.0)
 * 3/17/2013
 */
package playfield;

import java.util.Vector;
import java.util.ArrayList;

/**
 * A node of the BSP tree. This represents both a full line subdividing
 * a region of space into two further regions of space, and also the
 * region of space that is itself subdivided by the line.
 * 
 * BSP nodes contain references to portals that lie along the extent
 * of the line and are used in the process of searching for any
 * given entity's current leaf location.
 * 
 * @author Quothmar
 *
 */
public class BSPNode {
	private LineSeg fullPartition;
	protected Vector<Portal> portals;
	protected int numPortals;
	private Vector<TexturedLine> AILines;
	private int numAILines;
	private BSPNode front;
	private BSPNode back;
	private BSPNode parent;

	// These are also in Playfield. We must find a place to move them...
	private final int UPPER = 0;
	private final int RIGHT = 1;
	private final int LOWER = 2;
	private final int LEFT = 3;
	
	public BSPNode() {
		fullPartition = null;
		portals = new Vector<Portal>();
		numPortals = 0;
		AILines = new Vector<TexturedLine>();
		numAILines = 0;
		front = null;
		back = null;
		parent = null;
	}
	
	// Creates a BSP tree with the given list of AI lines and
	//   returns a reference to the root node. Here, 'parent'
	//   refers to the parent node to give the node created
	//   ('null' if it is to be created as a root node).
	public BSPNode(ArrayList<TexturedLine> list, BSPNode parentNode, Playfield pf) {

		if (list.size() == 0) error("BSPNode", "constructor called on empty list");
		
		// Attach the newly-created node to the given parent node, if any
		//   (otherwise it will be the root node).
		if (parentNode != null)
			this.parent = parentNode;
		
		// Select a partition from the list (let's say the first, for
		//   simplicity -- this could be made smarter). The 'sp' is
		//   for 'simple partition'.
		TexturedLine sp = list.get(0);
		
		// Now we get the full partition from the simple partition. This
		//   deviates from Brackeen's algorithm but appears necessary to
		//   get the portals.
		
		// First get intersection of extended line with edges of map.
		//   This isn't the best approach, but it will do for now.
		int partx1, party1, partx2, party2;
		if (sp.x1() == sp.x2()) {		// Vertical line
			partx1 = partx2 = sp.x1();
			if (sp.y1() < sp.y2()) {
				party1 = 0;
				party2 = pf.levelHeight();
			} else {
				party1 = pf.levelHeight();
				party2 = 0;
			}
		} else if (sp.y1() == sp.y2()) {
			party1 = party2 = sp.y1();
			if (sp.x1() < sp.x2()) {
				partx1 = 0;
				partx2 = pf.levelWidth();
			} else {
				partx1 = pf.levelWidth();
				partx2 = 0;
			}
		} else if ((sp.y2() - sp.y1())*(sp.x2() - sp.x1()) > 0) {	// This kind of slope: '/'
			if (sp.x1() < sp.x2()) {		// Rightward
				partx1 = Math.max(0, sp.x1() - sp.y1());
				party1 = Math.max(0, sp.y1() - sp.x1());
				partx2 = Math.min(pf.levelWidth(), sp.x2() + pf.levelHeight() - sp.y2());
				party2 = Math.min(pf.levelHeight(), sp.y2() + pf.levelWidth() - sp.x2());
			} else {						// Leftward
				partx1 = Math.min(pf.levelWidth(), sp.x1() + pf.levelHeight() - sp.y1());
				party1 = Math.min(pf.levelHeight(), sp.y1() + pf.levelWidth() - sp.x1());
				partx2 = Math.max(0, sp.x2() - sp.y2());
				party2 = Math.max(0, sp.y2() - sp.x2());
			}
		} else {	// This kind of slope: '\'
			if (sp.x1() < sp.x2()) {			// Rightward
				partx1 = Math.max(0, sp.x1() - (pf.levelHeight() - sp.y1()));
				party1 = Math.min(pf.levelHeight(), sp.y1() + sp.x1());
				partx2 = Math.min(pf.levelWidth(), sp.x2() + sp.y2());
				party2 = Math.max(0, sp.y2() - (pf.levelWidth() - sp.x2()));
			} else {							// Leftward
				partx1 = Math.min(pf.levelWidth(), sp.x1() + sp.y1());
				party1 = Math.max(0, sp.y1() - (pf.levelWidth() - sp.x1()));
				partx2 = Math.max(0, sp.x2() - (pf.levelHeight() - sp.y2()));
				party2 = Math.min(pf.levelHeight(), sp.y2() + sp.x2());
			}
		} // end if (how is the partition sloping?)
		
		// Now shorten the full extended line down to the closest intersections
		//   with full partitions of ancestor nodes.
		Fieldpoint fp = null;				// Used to hold intersection.
		int x1, y1, x2, y2;					// Used to hold parent's full partition.
		BSPNode ancestor = this.parent;		// Used to traverse back up the BSP tree.
		BSPNode ancestor1 = null;			// Used to hold the ancestors whose partitions were hit.
		BSPNode ancestor2 = null;			//
		while (ancestor != null) {
			
			// Get the next ancestor's full partition line.
			x1 = ancestor.getFullPartition().x1();
			y1 = ancestor.getFullPartition().y1();
			x2 = ancestor.getFullPartition().x2();
			y2 = ancestor.getFullPartition().y2();
			
			// Get the intersection of the current line (being shortened) with
			//   this ancestor's full partition line.
			fp = LineSeg.getIntersection(partx1, party1, partx2, party2,
										 x1, y1, x2, y2,
										 false, false);
			
			// Shorten partition if needed. (I simplified some long 'if' statements
			//   to a boolean logic formula. I hope it's readable!)
			if (fp != null) {
				if (    (sp.x1() < sp.x2() && partx1  <= fp.x && fp.x <= sp.x1() )
					 || (sp.x2() < sp.x1() && sp.x1() <= fp.x && fp.x <= partx1  )
					 || (    (sp.x1() == sp.x2() )
					      && (    (sp.y1() <  sp.y2() && party1  <= fp.y && fp.y <= sp.y1() )
					           || (sp.y2() <  sp.y1() && sp.y1() <= fp.y && fp.y <= party1  ) ) ) )
				{
					partx1 = fp.x;
					party1 = fp.y;
					ancestor1 = ancestor;
				}
				else
				if (    (sp.x1() < sp.x2() && sp.x2() <= fp.x && fp.x <= partx2  )
					 || (sp.x2() < sp.x1() && partx2  <= fp.x && fp.x <= sp.x2() )
					 || (    (sp.x1() == sp.x2() )
					      && (    (sp.y1() <  sp.y2() && sp.y2() <= fp.y && fp.y <= party2  )
					           || (sp.y2() <  sp.y1() && party2  <= fp.y && fp.y <= sp.y2() ) ) ) )
				{
					partx2 = fp.x;
					party2 = fp.y;
					ancestor2 = ancestor;
				}
			}
			
			// Go to the next ancestor.
			ancestor = ancestor.parent();

		} // end while (until all ancestors have been checked)
		
		// Split any portals of left and right ancestors that have been intersected
		//   by this node's full partition line. Here, i == 0 is used for the first
		//   ancestor and i == 1 is used for the second ancestor (if either exist).
		for (int i = 0; i < 2; ++i) {
			
			// Get the ancestor and the side of the partition that intersects with it.
			ancestor = (i == 0 ? ancestor1 : ancestor2);
			int partx = (i == 0 ? partx1 : partx2);
			int party = (i == 0 ? party1 : party2);
			if (ancestor1 == ancestor2 && (ancestor1 != null) && (ancestor2 != null))
				error("BSPNode", "identical ancestors of full partition found");
			
			// If it intersects with an ancestor on the given side...
			if (ancestor != null) {
				
				// Split portals contained in node, adding the resulting set of portals
				//   to a new portal list. Check each portal for a splitting point.
				Vector<Portal> newPortalList = new Vector<Portal>();
				for (int portal = 0; portal < ancestor.getNumPortals(); ++portal) {
					
					// Get the segment of the next portal.
					LineSeg pseg = ancestor.portals.get(portal).getSegment();
					
					// Check to see if intersection point with this ancestor's full partition
					//   happens to be on the portal.
					if (    (pseg.x1() == pseg.x2() && (pseg.y1() - party)*(pseg.y2() - party) < 0)		// Vertical, y within portal
					     || (pseg.x1() != pseg.x2() && (pseg.x1() - partx)*(pseg.x2() - partx) < 0) )	// Not vertical, x within portal
					{
						// Replace old portal with two new portals.
						newPortalList.add(new Portal(pseg.x1(), pseg.y1(), partx, party));
						newPortalList.add(new Portal(partx, party, pseg.x2(), pseg.y2()));
					}
					
					// If not, leave the portal unchanged.
					else {
						newPortalList.add(new Portal(pseg.x1(), pseg.y1(), pseg.x2(), pseg.y2()));
					}
					
				} // end for (by portals of given ancestor node)
				
				// Replace the set of portals on the given ancestor node with
				//   the new portals.
				int prevPortalCount = ancestor.getNumPortals();
				for (int portal = 0; portal < prevPortalCount; ++portal)
					ancestor.removePortal(0);
				for (int portal = 0; portal < newPortalList.size(); ++portal)
					ancestor.addPortal(newPortalList.get(portal));
				
			} // end if (ancestor intersected)
		} // end for (do both ancestors)
		
		// Do the same thing for AI lines of type 'WATER'.
		for (int i = 0; i < 2; ++i) {
			
			ancestor = (i == 0 ? ancestor1 : ancestor2);
			int partx = (i == 0 ? partx1 : partx2);
			int party = (i == 0 ? party1 : party2);
			
			if (ancestor != null) {
				
				// Split AI lines of type 'WATER' contained in node.
				Vector<TexturedLine> newAILineList = new Vector<TexturedLine>();
				for (int ailine = 0; ailine < ancestor.getNumAILines(); ++ailine) {
					
					// Get the next textured line of this node.
					TexturedLine l = ancestor.getAILine(ailine);

					// If it's not a line of type 'WATER', skip it and move on.
					if (l.getType() != Texture.WATER) {
						newAILineList.add(new TexturedLine(l.x1(), l.y1(), l.x2(), l.y2(), l.getType()));
						continue;
					}
					
					// Check to see if intersection point on this ancestor's full partition
					//   happens to be on the line of type 'WATER'.
					if (    (l.x1() == l.x2() && (l.y1() - party)*(l.y2() - party) < 0 )	// Vertical, y within water line
					     || (l.x1() != l.x2() && (l.x1() - partx)*(l.x2() - partx) < 0 ) )	// Not vertical, x within water line
					{
						// If so, replace it with two new 'WATER' lines.
						newAILineList.add(new TexturedLine(l.x1(), l.y1(), partx, party, Texture.WATER));
						newAILineList.add(new TexturedLine(partx, party, l.x2(), l.y2(), Texture.WATER));
					}
					
					// Otherwise, leave the 'WATER' line unchanged.
					else
						newAILineList.add(new TexturedLine(l.x1(), l.y1(), l.x2(), l.y2(), Texture.WATER));
		
				} // end for (by AI lines of given ancestor node)
				
				// Replace the set of portals on the given ancestor node with
				//   the new portals.
				int prevAILineCount = ancestor.getNumAILines();
				for (int ailine = 0; ailine < prevAILineCount; ++ailine)
					ancestor.removeAILine(0);
				for (int ailine = 0; ailine < newAILineList.size(); ++ailine)
					ancestor.addAILine(newAILineList.get(ailine));

			} // end if (ancestor intersected)
		} // end for (do both ancestors)
		
		// Classify lines as FRONT, BACK, COLLINEAR, SPANNING.
		// Note: This closely mirrors the code in Brackeen's book.
		ArrayList<TexturedLine> collinearList = new ArrayList<TexturedLine>();
		ArrayList<TexturedLine> frontList = new ArrayList<TexturedLine>();
		ArrayList<TexturedLine> backList = new ArrayList<TexturedLine>();
		for (int i = 0; i < list.size(); ++i) {
			
			// Get next line.
			TexturedLine l = list.get(i);
			
			// Only add lines recognized by AI to the BSP nodes.
			if (!isAILine(l)) continue;
			
			int side = sp.getSide(l);
			if (side == LineSeg.COLLINEAR) {
				collinearList.add(l);
			}
			else if (side == LineSeg.FRONT) {
				frontList.add(l);
			}
			else if (side == LineSeg.BACK) {
				backList.add(l);
			}
			else if (side == LineSeg.SPANNING) {

				// Split (i.e., clip) the line into front and back lines.
				TexturedLine frontPart, backPart;
				
				try {
				    Fieldpoint inters = sp.getIntersection(l, true, false);

	                if (sp.getSide(l.x1(), l.y1()) == LineSeg.FRONT) {
	                    frontPart = new TexturedLine(l.x1(), l.y1(), inters.x, inters.y, l.getType());
	                    backPart = new TexturedLine(inters.x, inters.y, l.x2(), l.y2(), l.getType());
	                } else {
	                    backPart = new TexturedLine(l.x1(), l.y1(), inters.x, inters.y, l.getType());
	                    frontPart = new TexturedLine(inters.x, inters.y, l.x2(), l.y2(), l.getType());
	                }
	                
	                // Add them to separate lists.
	                frontList.add(frontPart);
	                backList.add(backPart);
				
				}
				catch (Exception e) {
				    System.out.println("Values of l:\n x1 = " + l.x1() + ", y1 = " + l.y1()
				            + ", x2 = " + l.x2() + ", y2 = " + l.y2());
				    System.out.println("Values of sp:\n x1 = " + sp.x1() + ", y1 = " + sp.y1()
				            + ", x2 = " + sp.x2() + ", y2 = " + sp.y2());
				    e.printStackTrace();
				    System.exit(1);
				}
				
				
			} // end if (what side is the given AI line on?)
		} // end for (by line in this node's list)
		
		// Save collinear AI lines found into this node's AI line list.
		this.AILines = new Vector<TexturedLine>();
		for (int ailine = 0; ailine < collinearList.size(); ++ailine)
			this.addAILine(collinearList.get(ailine));
		
		// Remember full partition of this node.
		this.fullPartition = new LineSeg(partx1, party1, partx2, party2);
		
		// Now build portals in this node where AI lines are not found.
		this.portals = new Vector<Portal>();
		this.fillWithPortals(partx1, party1, partx2, party2, pf, 0);
		
		// Finally, build front and back nodes using 'this' as the parent
		//   (this is a recursive process).
		this.front = (frontList.size() == 0 ? new BSPLeaf() : new BSPNode(frontList, this, pf));
		this.back = (backList.size() == 0 ? new BSPLeaf() : new BSPNode(backList, this, pf));

	}

	public LineSeg getFullPartition() {
		return fullPartition;
	}
	
	// Need this in order to classify textured lines as AI lines or
	//   mere environment lines
	// FIXME: We should make this dependent on something a little
	//   closer to the Texture class, perhaps in the class itself.
	public static boolean isAILine(TexturedLine t) {
	    if (Texture.isWall(t.getType())) return true;
	    
		switch (t.getType()) {
		case BRICK_WALL_RED:
		case BRICK_WALL_WHITE:
		case BROWN_CAVE_WALL:
		case IRON_WALL:
		case STONE_WALL:
		case FLAMMABLE_TREES:
		case STONES:
		case WATER:
		case SAND_WALL:
		case SPIKES:
		case LAVA:
		case ROCK_WALL:
		case FIXED_TREES:
			return true;
		default:
			return false;
		}
	}
	
	// Fill the given interval on the given node's full partition with portals
	//   where no walls exist.
	// Flags are as follows:
	//   FWP_CONNECT: connect the newly created portals with their front and back leaves
	//     (or back squares in the case of tree portals) and with all neighboring
	//     portals in the given leaf or square
	//   FWP_CLOSE: create the portals so that they start out in a 'closed' state (water, etc.)
	//   FWP_UNIT_SQUARES: create the portals as unit segments and attach to the portals
	//     in the adjacent PortalSquare (used for trees)
	public static final int FWP_CONNECT = 1;
	public static final int FWP_CLOSE = 2;
	public static final int FWP_UNIT_SQUARES = 4;
	public void fillWithPortals(int x1, int y1, int x2, int y2, Playfield pf, int flags) {
		
		// Get the values to step in the direction the given interval is headed.
		int stepx = (x1 < x2 ? 16 : (x1 == x2 ? 0 : -16));
		int stepy = (y1 < y2 ? 16 : (y1 == y2 ? 0 : -16));
		
		// Initialize counter variables (these will move across the interval and
		//   the 'pstart' variables will keep track of when a portal might begin).
		int partx1 = this.getFullPartition().x1();
		int party1 = this.getFullPartition().y1();
		int partx2 = this.getFullPartition().x2();
		int party2 = this.getFullPartition().y2();
		int x = ((partx2 - partx1)*stepx > 0 ? partx1 : partx2);
		int y = ((party2 - party1)*stepy > 0 ? party1 : party2);
		int pstartx = x1;
		int pstarty = y1;
		
		// Variable 'lineCount' will keep track of how many AI lines are at the
		//   current point (portals will be built where this value is '0').
		// 'oldLineCount' will use the old value to determine where a critical
		//   change has occurred.
		int lineCount = 0;
		int oldLineCount = 0;
		
		// Loop by progressive unit intervals until entire interval is filled
		//   with portals where there are no AI lines.
		while (true) {
			
			// Check each line at the given point on the interval, to see if
			//   there is an endpoint.
			oldLineCount = lineCount;
			for (int ailine = 0; ailine < this.AILines.size(); ++ailine) {
				TexturedLine l = this.AILines.get(ailine);
				
				// Check both endpoints of the given line (starting and ending).
				for (int i = 0; i < 2; ++i) {
					
					// Get both endpoints (roles switch for i == 0 and i == 1).
					int ax = (i == 0 ? l.x1() : l.x2());
					int ay = (i == 0 ? l.y1() : l.y2());
					int bx = (i == 0 ? l.x2() : l.x1());
					int by = (i == 0 ? l.y2() : l.y1());
					
					// Adjust linecount if given endpoint coincides with the
					//   current point.
					if (ax == x && ay == y)
						if ((bx - ax)*stepx > 0 || (by - ay)*stepy > 0)
							++lineCount;
						else
							--lineCount;

				} // end for (check both endpoints)
			} // end for (check all lines)
			
			// If line count has dropped to zero on interval, tentatively set the
			//   coordinates to begin a portal at the current point.
			if (oldLineCount != 0 && lineCount == 0)
				if ((x - x1)*stepx > 0 || (y - y1)*stepy > 0)
					{ pstartx = x; pstarty = y; }
			
			// Otherwise, if a space has elapsed on interval and a portal creation point
			//   reached, create portal(s).
			if (    ((x - x1)*stepx > 0 || (y - y1)*stepy > 0)
		   	     && oldLineCount == 0 && (lineCount != 0 || (x == x2 && y == y2)) )
			{
				if ((pstartx - x) != 0 && (pstarty - y) != 0 && Math.abs(pstartx - x) != Math.abs(pstarty - y))
					error("fillWithPortals", "portal angle not a multiple of 45 degrees");
				
				// If creating unit portals (e.g., for trees)...
				if ((flags & FWP_UNIT_SQUARES) != 0) {

					// Error checking.
					if (x1 != x2 && y1 != y2)
						error("fillWithPortals", "cannot create unit portals from diagonal");
					if (2*stepx != 0 && 2*stepy != 0)
						if (x1 % (2*stepx) != 0 || y1 % (2*stepy) != 0)
							error("fillWithPortals", "portal length does not allow creation of unit portals");
					
					// Start at beginning of portal space and progressively add unit portals.
					int xup1 = pstartx;
					int yup1 = pstarty;
					int xup2 = pstartx + 2*stepx;
					int yup2 = pstarty + 2*stepy;
					while (true) {
						
						// Get the next portal and add it to this node.
						Portal ptl = new Portal(xup1, yup1, xup2, yup2);
						this.addPortal(ptl);
						
						// Close the portal if requested.
						if ((flags & FWP_CLOSE) != 0) ptl.close();
						
						// Set property of back leaf (currently specific to tree/stone polgyons).
						// Actually, I'll see if I can do this the easier way and just go by
						//   leaf references of portal squares, after they are created...
						//BSPNode.getLeaf(pf.bsp, ptl.getSegment(), LineSeg.BACK).setProperty(LeafProperty.TREE_STONE_POLY);
						
						// Also connect to leaves/other portals if that option is set.
						if ((flags & FWP_CONNECT) != 0) {
							
							// Find the leaf in front of the unit portal.
							BSPLeaf leaf = getLeaf(pf.bsp, ptl.getSegment(), LineSeg.FRONT);
							ptl.setFrontLeaf(leaf);
							
							// Connect all existing portals of that leaf with the new portal.
							for (int portal = 0; portal < leaf.getNumPortals(); ++portal) {
								ptl.addNeighbor(leaf.getPortal(portal));
								leaf.getPortal(portal).addNeighbor(ptl);
							}
							
							// Add the new portal to that leaf.
							leaf.addPortal(ptl);
							
							// Connect the portal to any dynamic portals in the portal square
							//   behind it.
							int psquarex = (x1==x2 ? (y1<y2 ? (xup1>>5)-1 : xup1>>5) : (x1<x2 ? xup1>>5 : (xup1>>5)-1));
							int psquarey = (y1==y2 ? (x1<x2 ? yup1>>5 : (yup1>>5)-1) : (y1<y2 ? yup1>>5 : (yup1>>5)-1));
							int side = (x1==x2 ? (y1<y2 ? RIGHT : LEFT) : (x1<x2 ? LOWER : UPPER));
							PortalSquare ps = (PortalSquare)pf.gameGrid[psquarex][psquarey];
							ps.addPortal(ptl, side);
							for (int j = 0; j < 4; ++j) {		// 'j' goes: UPPER->RIGHT->LOWER->LEFT
								if (side != j && ps.getPortal(j) != null) {
									ps.getPortal(j).addNeighbor(ptl);
									ptl.addNeighbor(ps.getPortal(j));
								}
							} // end for (check all sides)
							
						} // end if (connect portals)
						
						// Increment unit segment.
						xup1 += 2*stepx;
						yup1 += 2*stepy;
						xup2 += 2*stepx;
						yup2 += 2*stepy;
						if (xup1 == x && yup1 == y)
							break;
						
					} // end while (by unit segments in interval)
				} // end if (unit squares)

				// Otherwise, if adding a single portal...
				else {

					// Add the portal.
					Portal ptl = new Portal(pstartx, pstarty, x, y);
					this.addPortal(ptl);

					// Close if needed.
					if ((flags & FWP_CLOSE) != 0) ptl.close();
					
					// Connect with other portals if needed. Note: This assumes that
					//   neither the front or back leaf contains portal squares. For
					//   this reason, water lines are transformed into portals before
					//   tree polygon lines.
					if ((flags & FWP_CONNECT) != 0) {

						// Find the leaves in front and in back of the unit portal.
						BSPLeaf frontLeaf = getLeaf(pf.bsp, ptl.getSegment(), LineSeg.FRONT);
						BSPLeaf backLeaf = getLeaf(pf.bsp, ptl.getSegment(), LineSeg.BACK);
					
						// Could the 'setFrontLeaf' and 'setBackLeaf' be better placed here...?
						
						// Set property of back leaf (note: this is specific to 'WATER').
						// Again, I'll try to do this the easier way...
						//backLeaf.setProperty(LeafProperty.WATER);
						
						// Connect portals of both leaves to new portal.
						for (int portal = 0; portal < frontLeaf.getNumPortals(); ++portal) {
							ptl.addNeighbor(frontLeaf.getPortal(portal));
							frontLeaf.getPortal(portal).addNeighbor(ptl);
						}
						for (int portal = 0; portal < backLeaf.getNumPortals(); ++portal) {
							ptl.addNeighbor(backLeaf.getPortal(portal));
							backLeaf.getPortal(portal).addNeighbor(ptl);
						}
						
						// Add the portal to both leaves.
						frontLeaf.addPortal(ptl);
						backLeaf.addPortal(ptl);
						
						// Also add the portal to the water portals of the map grid squares.
						//   Note: this is currently specific to 'WATER'.
						int msx = (x2<x1 && pstartx%32==0 ? (pstartx>>5)-1 : pstartx>>5);
						int msy = (y2<y1 && pstarty%32==0 ? (pstarty>>5)-1 : pstarty>>5);
						int mfx = (x1<x2 && x%32==0 ? (x>>5)-1 : x>>5);
						int mfy = (y1<y2 && y%32==0 ? (y>>5)-1 : y>>5);
						int mstepx = (x2<x1 ? -1 : (x1<x2 ? 1 : 0));
						int mstepy = (y2<y1 ? -1 : (y1<y2 ? 1 : 0));
						while (true) {
							pf.gameGrid[msx][msy].attach(ptl);
							if (x1==x2) pf.gameGrid[msx-1][msy].attach(ptl);
							if (y1==y2) pf.gameGrid[msx][msy-1].attach(ptl);
							if (msx == mfx && msy == mfy) break;
							msx += mstepx;
							msy += mstepy;
						}
						
					} // end if (connect portal)
				} // end if (adding single or unit portals?)
			} // end if (portal space found?)

			// Go to next point on interval, or stop if finished.
			if (x == x2 && y == y2)	break; else { x += stepx; y += stepy; }
		
		} // end while (check each applicable point along the interval)

	} // end function fillWithPortals

	// Gets the leaf in front of or behind the specified line
	//   segment. This is a recursive method that uses tree
	//   traversal.
	public static BSPLeaf getLeaf(BSPNode node, LineSeg l, int side) {
		
	    if (node == null || node instanceof BSPLeaf) {
			return (BSPLeaf)node;
		}
		
		int segSide = node.getFullPartition().getSide(l);
		
		if (segSide == LineSeg.COLLINEAR) {
			segSide = side;

			// Note: Brackeen's algorithm uses the node's partition
			//   to get the front and back leaves for each portal.
			//   I will revise his algorithm to use the line segment
			//   (and its unique normal, which may face in the
			//   opposite direction), rather than the full partition.
			// This just reverses the line segment's normal if needed
			//   to place it into correspondence with the full
			//   partition's normal.
			if (    l.nx*node.getFullPartition().nx < 0
				 || l.ny*node.getFullPartition().ny < 0 )
			{
				if (segSide == LineSeg.FRONT) segSide = LineSeg.BACK;
				else segSide = LineSeg.FRONT;
			}
		}
		
		if (segSide == LineSeg.FRONT) {
			return getLeaf(node.front, l, side);
		}
		else if (segSide == LineSeg.BACK) {
			return getLeaf(node.back, l, side);
		}
		else { // BSPLine.SPANNING
			// Shouldn't happen
			return null;
		}
	}
	
	// Gets the leaf containing the specified point. Returns
	//  'null' if coincident with any full partition line.
	public static BSPLeaf getLeaf(BSPNode node, int x, int y) {
		if (node == null || node instanceof BSPLeaf) {
			return (BSPLeaf)node;
		}
		int side = node.getFullPartition().getSide(x, y);
		if (side == LineSeg.COLLINEAR)
			return null;
		else if (side == LineSeg.FRONT) {
			return getLeaf(node.front, x, y);
		} else {
			return getLeaf(node.back, x, y);
		}
	}

	public int getNumAILines() {
		return numAILines;
	}

	public int getNumPortals() {
		return numPortals;
	}

	public TexturedLine getAILine(int ailine) {
		return AILines.get(ailine);
	}

	public Portal getPortal(int portal) {
		return portals.get(portal);
	}

	public BSPNode front() {
		return front;
	}

	public BSPNode back() {
		return back;
	}
	
	public BSPNode parent() {
		return parent;
	}

	public void setFullPartition(LineSeg aLine) {
		fullPartition = aLine;
	}

	public void addPortal(Portal aPortal) {
        aPortal.parentNodes.add(this);
		portals.add(aPortal);
		++numPortals;
	}

	public void removePortal(int portal) {
		portals.remove(portal);
		--numPortals;
	}
	
	public void removePortal(Portal ptl) {
	    if (portals.contains(ptl)) {
	        portals.remove(ptl);
	        --numPortals;
	    }
	}
	
	public void addAILine(TexturedLine l) {
		AILines.add(l);
		++numAILines;
	}

	public void removeAILine(int ailine) {
		AILines.remove(ailine);
		--numAILines;
	}
	
	private void error(String function, String text) {
		System.out.println("BSPNode." + function + "(): " + text);
		System.exit(1);
	}
	
}

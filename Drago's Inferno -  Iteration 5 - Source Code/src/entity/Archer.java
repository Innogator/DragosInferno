/*
 * Archer.java (v1.0)
 * 3/17/2013
 */
package entity;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.ArrayList;

import drago.DragoStatics;

import playfield.BSPLeaf;
import playfield.BSPNode;
import playfield.LeafProperty;
import playfield.LineSeg;
import playfield.AStarNode;
import playfield.AStarSearch;
import playfield.Fieldpoint;
import playfield.LeafNode;
import playfield.Playfield;
import playfield.Polygon;
import playfield.Portal;
import playfield.PortalSquare;
import playfield.MapGridSquare;

/**
 * An enemy archer that shoots arrows.
 * 
 * @author Quothmar
 *
 */
public class Archer extends Walker
{
    private static ArrayList<ArrayList<ArrayList<BufferedImage>>>
        walkingSprites = new ArrayList<ArrayList<ArrayList<BufferedImage>>>();
    
    // Random value to determine which part of a portal to walk toward
    //   during an A* search.
    private boolean needNewSearchVal = false; 
    private int portalSearchVal = 0;
    
    // Archer's constructor.
    public Archer(int x, int y, String beginFacing, Playfield pf) {
        
        // Adjust (x, y) to be in the center of the bounding polygon,
        //   rather than the sprite. Done to see if A* will work better.
        super(x, y - 4, beginFacing, pf);
            
        // An archer has a 32x32 bounding box.
        Fieldpoint fp1 = new Fieldpoint(x - 16, y - 20);
        Fieldpoint fp2 = new Fieldpoint(x + 16, y - 20);
        Fieldpoint fp3 = new Fieldpoint(x + 16, y + 12);
        Fieldpoint fp4 = new Fieldpoint(x - 16, y + 12);
        this.setBoundingPoly(new Polygon(fp1, fp2, fp3, fp4));
        
        this.setHeight(40);
        this.setState(Entity.ST_HUMAN_WALKING);
        this.setStateCounter(40);
        this.setFacing(Entity.F_LEFT);
        this.setSpeed(3);
        this.setdx(this.getSpeed());
        this.setdy(0);
        this.setdz(0);
        this.attach();
        
    }

    // Archer's sprites.
    public static void loadSprites() {
        Walker.loadSprites("ARCHER", walkingSprites);
    }
    private static BufferedImage getSprite(int set, int direction, int number) {
        return walkingSprites.get(set).get(direction).get(number);
    }
    public BufferedImage getCurrentSprite() {
        int set = 0;
        int n = 0;
        int sc = this.getStateCounter();
        switch(this.getState()) {
        case Entity.ST_HUMAN_WALKING:  set = Walker.SP_WALKING;  n = (sc%40)/10; break;
        case Entity.ST_HUMAN_STANDING: set = Walker.SP_STANDING; n = 0; break;
        case Entity.ST_HUMAN_DEAD:     set = Walker.SP_STANDING; n = 0; break; // Not needed?
        default:
            System.out.println("Invalid state found in Archer (" + this.getState() + ")");
            System.exit(1);
        }
        return getSprite(set, this.getFacing(), n);
    }
    
    // Archer's state counter.
    public void tickStateCounter() {
        if (stateCounter == 0) return;
        else --stateCounter;
        if (stateCounter == 0) {
            switch (this.getState()) {
            case Entity.ST_HUMAN_WALKING: stateCounter = 40; break;
            }
        }
    }
    
    // Archer's act method.
    public void act() {
        
        boolean GAUNTLET = false;
        boolean VERBOSE = false;
        
        boolean walkIn = false;
        int dx = 0;
        int dy = 0;
        int destx = 0;
        int desty = 0;

        LinkedList<AStarNode> path = null;
        if (GAUNTLET) {
            
            // For now, just use Gauntlet-style motion.
    
            // Liz: here is where you can test out the AStarSearch class,
            //   obtaining a different direction of motion than the one
            //   calculated directly toward the player.
            
            destx = this.ppf.player.x() - this.x();
            desty = this.ppf.player.y() - this.y();
        }

        // Otherwise, do A* search.
        else {
         
            AStarSearch finder = new AStarSearch(this.ppf.bsp);
            int plx = this.ppf.player.x();
            int ply = this.ppf.player.y();
            path = finder.findPath(this.x(), this.y(), plx, ply, this.ppf);

        }
        
        if (path != null)
        {
            if (path.get(0) instanceof LeafNode) {
                if (VERBOSE) System.out.println("Removing leaf node at beginning of path");
                path.removeFirst();
            }
            
            if (VERBOSE) System.out.println("Path length upon removal of first node: "
                    + path.size());
            for (int i = 0; i < path.size(); ++i) {
                if (VERBOSE) System.out.println("  Neighbors of path node " + i + ": "
                        + path.get(i).numNeighbors());
            }
            
            // If the searcher is *directly on* the midpoint of the destination
            //   portal, save it as the 'standing portal' and use the next node
            //   as the destination; otherwise use the first node as the destination.
            // Note: On second thought, this could create a 'midpoint lock' between
            //   competing enemies. Instead, try to use the portal's overlapping the
            //   searcher as the condition for attempting motion into the next leaf.
            //   Continue calling this the 'standingPortal'.
            Portal standingPortal = null;
            AStarNode destNode = path.removeFirst();
            while (true) {
                if (destNode instanceof Portal) {
                    Portal ptl = (Portal)destNode;
                    if (this.getBoundingPoly().overlaps(ptl.getSegment()))
                    {
                        if (needNewSearchVal) {
                            int rand = DragoStatics.randomList[this.stateCounter];
                            this.portalSearchVal = rand;
                            needNewSearchVal = false;
                        }
                        standingPortal = ptl;
                        destNode = path.removeFirst();
                    }
                    else {
                        needNewSearchVal = true;
                        break;
                    }
                }
                else break;
            }
            
            // If the destination node is a tree/stone portal, get the
            //   square that must be entered to reach it, if any, as follows:

            // If the destination node is a portal...
            PortalSquare crossPortalSq = null;
            if (destNode instanceof Portal) {
                
                // If the portal is of unit length (= 32)...
                Portal destPtl = (Portal)destNode;
                int x1 = destPtl.getSegment().x1();
                int y1 = destPtl.getSegment().y1();
                int x2 = destPtl.getSegment().x2();
                int y2 = destPtl.getSegment().y2();

                if (VERBOSE) System.out.println("Destination portal's segment: "
                        + "(" + x1 + ", " + y1 + ") - (" + x2 + ", " + y2 + ")");
                
                if ((x2 - x1)*(x2 - x1) + (y2 - y1)*(y2 - y1) == 1024) {
                    
                    // If there is already a standing portal...
                    if (standingPortal != null) {
                        
                        // Get both squares to the sides of the
                        //   destination portal.
                        MapGridSquare sideOne = null;
                        MapGridSquare sideTwo = null;
                        int xs1;
                        int ys1;
                        int xs2;
                        int ys2;
                        if (x1 == x2) {
                            xs1 = x1 >> 5;
                            xs2 = xs1 - 1;
                            ys1 = ys2 = (y1 + y2) >> 6;
                        }
                        else {
                            ys1 = y1 >> 5;
                            ys2 = ys1 - 1;
                            xs1 = xs2 = (x1 + x2) >> 6;
                        }
                        sideOne = this.ppf.gameGrid[xs1][ys1];
                        sideTwo = this.ppf.gameGrid[xs2][ys2];
                        
                        // If either (or both) is a portal square, check to see if
                        //   one of its four portals is the standing portal. If so,
                        //   record it as the portal square to cross.
                        if (sideOne instanceof PortalSquare) {
                            PortalSquare psOne = (PortalSquare)sideOne;
                            for (int side = 0; side < 4; ++side) {
                                if (psOne.getPortal(side) == standingPortal) {
                                    if (VERBOSE) System.out.println("Portal square to cross is on the "
                                            + "grid at (" + xs1 + ", " + ys1 + ")");
                                    crossPortalSq = psOne;
                                    break;
                                }
                            }
                        }
                        if ((crossPortalSq == null) && (sideTwo instanceof PortalSquare)) {
                            PortalSquare psTwo = (PortalSquare)sideTwo;
                            for (int side = 0; side < 4; ++side) {
                                if (psTwo.getPortal(side) == standingPortal) {
                                    if (VERBOSE) System.out.println("Portal square to cross is on the "
                                            + "grid at (" + xs2 + ", " + ys2 + ")");
                                    crossPortalSq = psTwo;
                                    break;
                                }
                            }
                        }
                    }
                    
                    // Otherwise, if there is not a standing portal, check to see if the
                    //   searcher is located within a portal square; if so, it is the
                    //   portal square to cross.
                    else {
                        int xs = this.x() >> 5;
                        int ys = this.y() >> 5;
                        MapGridSquare sq = this.ppf.gameGrid[xs][ys];
                        if (sq instanceof PortalSquare) {
                            if (VERBOSE) System.out.println("Portal square to cross is on the "
                                    + "grid at (" + xs + ", " + ys + ")");
                            crossPortalSq = (PortalSquare)sq;
                        }
                    }

                } // end if (destination portal is of unit size)
            } // end if (destination node is a portal)
        
            // Now get all the portals of the region to cross that
            //   currently overlap the searcher, as follows: if the
            //   region to cross is a portal square...
            BSPLeaf crossLeaf = null;
            ArrayList<Portal> overlapList = new ArrayList<Portal>();
            if (crossPortalSq != null) {
                
                // Check the four portals.
                for (int side = 0; side < 4; ++side) {
                    Portal ptl = crossPortalSq.getPortal(side);
                    if (ptl != null) {
                        if (this.getBoundingPoly().overlaps(ptl.getSegment())) {
                            overlapList.add(ptl);
                        }
                    }
                }
            }
            
            // Otherwise, the region to cross must be a leaf. In that case...
            else {

                // If the destination is the target of the search, get the leaf
                //   of the target.
                if (destNode instanceof LeafNode) {
                    if (VERBOSE) System.out.println("Getting cross leaf from leaf node.");
                    LeafNode leafNode = (LeafNode)destNode;
                    crossLeaf = leafNode.leaf;
                }
                
                // Otherwise...
                else {
                    
                    // If there is a standing portal, check both leaves of the destination
                    //   portal to see which one has the standing portal.
                    Portal destPtl = (Portal)destNode;
                    if (standingPortal != null) {
                        if (VERBOSE) System.out.println("Standing portal recognized. "
                                + "Portal is (" + standingPortal.getSegment().x1() + ", "
                                + standingPortal.getSegment().y1() + ") - ("
                                + standingPortal.getSegment().x2() + ", "
                                + standingPortal.getSegment().y2() + ")");
                        BSPLeaf stfront = standingPortal.getFrontLeaf();
                        BSPLeaf stback = standingPortal.getBackLeaf();
                        BSPLeaf destfront = destPtl.getFrontLeaf();
                        BSPLeaf destback = destPtl.getBackLeaf();
                        if (VERBOSE) System.out.println("stfront = " + stfront + "\nstback = " + stback);
                        crossLeaf = ((stfront == destfront) || (stfront == destback))
                                  ? stfront : stback;
                    }
                    
                    // Otherwise, if there is no standing portal, get the leaf of the searcher.
                    else {

                        // FIXME: Not really an elegant solution; see AStarSearch.
                        crossLeaf = BSPNode.getLeaf(this.ppf.bsp, this.x(), this.y());
                        if (crossLeaf == null) crossLeaf = BSPNode.getLeaf(ppf.bsp, x() + 2, y() + 1);
                        if (crossLeaf == null) crossLeaf = BSPNode.getLeaf(ppf.bsp, x() - 2, y() - 1);
                        if (VERBOSE) System.out.println("crossLeaf has " + crossLeaf.getNumPortals() + " portals.");

                    } // end if (whether standing portal exists)

                } // end if (whether destination is target or portal)
                
                // Get all the portals of this leaf that overlap the
                //   searcher's bounding polygon. FIXME: Null pointer exception
                //   here at end of forest.
                for (int i = 0; i < crossLeaf.getNumPortals(); ++i) {
                    Portal ptl = crossLeaf.getPortal(i);
                    if (this.getBoundingPoly().overlaps(ptl.getSegment())) {
                        overlapList.add(ptl);
                    }
                }
                
            } // end if (whether region to cross is portal square or leaf)
                
            // If there aren't any overlapping portals, the searcher is
            //   wholly within the leaf; just move toward destination.
            if (overlapList.isEmpty()) {
                if (VERBOSE) System.out.println("Searcher is wholly within leaf.");
                if (destNode instanceof Portal) {
                    Portal destPtl = (Portal)destNode;
                    destx = destPtl.getMidpoint().x;
                    desty = destPtl.getMidpoint().y;
                    //Fieldpoint fp = destPtl.getPoint(this.portalSearchVal);
                    //destx = fp.x;
                    //desty = fp.y;
                }
                else {
                    LeafNode target = (LeafNode)destNode;
                    destx = target.location.x;
                    desty = target.location.y;
                }
            }
            
            // Otherwise, attempt to move the searcher into the leaf by
            //   combining inward-pointing normal vectors into a single
            //   vector.
            // Also add vector toward destination portal, if in leaf or square?
            else {
                final int NORM_LENGTH = 1000;
                walkIn = true;
                if (VERBOSE) System.out.println("Calculating vector into next region.");
                for (int i = 0; i < overlapList.size(); ++i) {
                    int nx = overlapList.get(i).getSegment().nx;
                    int ny = overlapList.get(i).getSegment().ny;
                    int nlen = DragoStatics.sqrt(nx*nx + ny*ny);
                    int inx = (nx*NORM_LENGTH) / nlen;
                    int iny = (ny*NORM_LENGTH) / nlen;
                    if (    (crossPortalSq == null)
                         && (overlapList.get(i).getBackLeaf() == crossLeaf) )
                    {
                        inx = -inx;
                        iny = -iny;
                    }
                    else if (crossPortalSq != null) {
                        Portal ptl   = overlapList.get(i);
                        Portal upper = crossPortalSq.getPortal(DragoStatics.UPPER);
                        Portal right = crossPortalSq.getPortal(DragoStatics.RIGHT);
                        Portal lower = crossPortalSq.getPortal(DragoStatics.LOWER);
                        Portal left  = crossPortalSq.getPortal(DragoStatics.LEFT);
                        if (    ((ptl == upper) && (upper.getSegment().ny > 0))
                             || ((ptl == right) && (right.getSegment().nx > 0))
                             || ((ptl == lower) && (lower.getSegment().ny < 0))
                             || ((ptl == left)  && (left.getSegment().nx  < 0)) )
                        {
                            inx = -inx;
                            iny = -iny;
                        }
                    }
                    dx += inx;
                    dy += iny;
                }
                
                // This may or may not work...
                if ((crossPortalSq != null) && (destNode instanceof Portal)) {
                    
                    Portal destPtl = (Portal)destNode;
                    int aimx = destPtl.getMidpoint().x - this.x();
                    int aimy = destPtl.getMidpoint().y - this.y();
                    int dist = DragoStatics.sqrt(aimx*aimx + aimy*aimy);
                    dx += (aimx*NORM_LENGTH) / dist;
                    dy += (aimy*NORM_LENGTH) / dist;
                    
                }
                
            }
        } // end if (Gauntlet/A* Search)

        if (!walkIn) {
            dx = destx - this.x();
            dy = desty - this.y();
        }
        else {
            if (VERBOSE) System.out.println("Walk-in vector: "
                    + "(" + dx + ", " + dy + ")");
        }
        if (VERBOSE) System.out.println("Searcher position before move: "
                + "(" + this.x() + ", " + this.y() + ")");
        int dist = dx*dx + dy*dy;
        dist = DragoStatics.sqrt(dist);
        if ((dist != 0) && (dist > this.getSpeed())) {
            int newdx = (dx*this.getSpeed()) / dist;
            int newdy = (dy*this.getSpeed()) / dist;
            if ((newdx == 0) && (dx != 0)) newdx += DragoStatics.sign(dx);
            if ((newdy == 0) && (dy != 0)) newdy += DragoStatics.sign(dy);
            dx = newdx;
            dy = newdy;
        }
        if (dx == 0) this.setFacing((dy < 0) ? Entity.F_DOWN : Entity.F_UP);
        else if (dy == 0) this.setFacing((dx < 0) ? Entity.F_LEFT : Entity.F_RIGHT);
        else if ((dx > 0) && (dy > 0)) this.setFacing(Entity.F_UP_RIGHT);
        else if ((dx > 0) && (dy < 0)) this.setFacing(Entity.F_DOWN_RIGHT);
        else if ((dx < 0) && (dy > 0)) this.setFacing(Entity.F_UP_LEFT);
        else if ((dx < 0) && (dy < 0)) this.setFacing(Entity.F_DOWN_LEFT);
        this.setdx(dx);
        this.setdy(dy);
        this.attemptMotion(this.dx(), this.dy(), this.dz(), false, false);
        this.tickStateCounter();

    } // end method act
}

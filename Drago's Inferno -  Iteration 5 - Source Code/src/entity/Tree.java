/*
 * Tree.java (v1.0)
 * 3/17/2013
 */
package entity;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;

import drago.DragoStatics;

import playfield.MapGridSquare;
import playfield.PortalSquare;
import playfield.Fieldpoint;
import playfield.Playfield;
import playfield.Polygon;

/**
 * A tree that the player can burn down.
 *  
 * @author Quothmar
 *
 */
public class Tree extends Entity{
	
	private static ArrayList<BufferedImage> sprites = new ArrayList<BufferedImage>();

	// Tree's constructor.
	public Tree(int x, int y, String beginFacing, Playfield pf) {
		
		super(x, y, beginFacing, pf);
		
		// A tree has a 32x32 bounding box.
		y -= 16;
		Fieldpoint fp1 = new Fieldpoint(x - 16, y - 16);
		Fieldpoint fp2 = new Fieldpoint(x + 16, y - 16);
		Fieldpoint fp3 = new Fieldpoint(x + 16, y + 16);
		Fieldpoint fp4 = new Fieldpoint(x - 16, y + 16);
		this.setBoundingPoly(new Polygon(fp1, fp2, fp3, fp4));
		
		this.setHeight(64);
        this.setState(Entity.ST_TREE_INTACT);
		this.attach();
		
	}

    // Tree's sprites.
    public static void loadSprites() {
        try {
            String filename = "img\\FLAMMABLE_TREE.gif";
            File imgfile = new File(filename);
            if (!imgfile.exists()) throw new Exception();
            sprites.add(0, ImageIO.read(imgfile));
        }
        catch (Exception e) {
            System.out.println("Exception while loading Tree sprites!");
            System.exit(1);
        }
    }
    public BufferedImage getCurrentSprite() {
        return sprites.get(0);
    }
    
	// Overloaded disappear method checks other squares for trees
	//   or stones; if none are found on that side, a dynamic portal
	//   is opened. We may end up moving this to a 'PathObstacle'
    //   class so that the code can be shared with the 'Stone' class.
    //   For now, we will keep it in the 'Tree' class.
	@Override
	public void disappear() {
	    
	    boolean VERBOSE = false;
	    
	    // Get the square this tree is on.
	    int sqx = this.x() >> 5;
	    int sqy = (this.y() - 16) >> 5;
	    if (VERBOSE) System.out.println("Value of (sqx, sqy) = (" + sqx + ", " + sqy + ")");
	    
	    // Check nearby squares for trees/stones.
        boolean cond = false;
        int chkx = 0;
        int chky = 0;
	    for (int i = 0; i < 4; ++i) {
	        
	        // Get the grid height and width.
	        int gh = this.ppf.gridHeight();
	        int gw = this.ppf.gridWidth();
	        
	        // Determine which square to check next.
	        switch (i) {
	        case 0: cond = (sqx > 1); chkx = sqx - 1; chky = sqy; break;
	        case 1: cond = (sqy > 1); chkx = sqx; chky = sqy - 1; break;
	        case 2: cond = (sqx < gw - 1); chkx = sqx + 1; chky = sqy; break;
	        case 3: cond = (sqy < gh - 1); chkx = sqx; chky = sqy + 1; break;
	        }
	        
	        // If a square exists...
	        if (cond) {
    	        
                // If it's not a portal square, open the portal. Otherwise,
	            //   check its entities to see if a tree or stone lies on it.
	            MapGridSquare ms = this.ppf.gameGrid[chkx][chky];
                int ptlside = 0;
                boolean found = false;
	            if (ms instanceof PortalSquare) {
	                
                    // Check each entity overlapping that square.
        	        for (int j = 0; j < ms.getNumEntities(); ++j) {
        	            
        	            // If, as a tree or stone, it's not positioned exactly
        	            //   on that square, skip it.
        	            Entity t = ms.entities.get(j);
        	            if (VERBOSE) System.out.println("Checking entity of "
        	                    + t.getClass() + " with coordinates ("
        	                    + t.x() + ", " + t.y() + ") for correct tree/stone "
        	                    + "location");
        	            if (    !((t.x()>>5) == chkx)
        	                 || !(((t.y() - 16)>>5) == chky) )
        	            {
        	                continue;
        	            }
        	            
        	            // If it is a tree or stone, mark that boolean.
        	            if ((t instanceof Tree) || (t instanceof Stone)) {
        	                found = true;
        	                if (VERBOSE) System.out.println("Tree/stone found on square");
        	                break;
    
        	            } // end if (entity found was a tree/stone)
        	        } // end for (by entities on square)
	            } // end if (square is a PortalSquare)
    	        
    	        // If an entity was not found there, open the portal.
    	        if (!found) {
    	            if (VERBOSE) System.out.println("Opening portal");
                    switch (i) {
                    case 0: ptlside = DragoStatics.LEFT; break;
                    case 1: ptlside = DragoStatics.LOWER; break;
                    case 2: ptlside = DragoStatics.RIGHT; break;
                    case 3: ptlside = DragoStatics.UPPER; break;
                    }
                    ms = this.ppf.gameGrid[sqx][sqy];
    	            PortalSquare ps = (PortalSquare)ms;
    	            if (ps.getPortal(ptlside) != null)
    	                ps.getPortal(ptlside).open();
    	        }
    	        
	        } // end if (square exists)
	    } // end for (by sides of given square)
	    
	    // Now destroy the tree.
	    super.disappear();
	    
	} // end method disappear	                
	
	// Tree's state counter.
	public void tickStateCounter() {
		if (stateCounter == 0) return;
		--stateCounter;
	}
	
    // Tree's action loop -- nothing yet. (If there were wind
	//   blowing, however...)
    public void act() {}
    
} // end class Tree

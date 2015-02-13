/*
 * Stone.java (v1.0)
 * 3/17/2013
 */
package entity;

import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import playfield.Playfield;
import playfield.Fieldpoint;
import playfield.Polygon;

/**
 * A stone that the player can charge and break through.
 * 
 * @author Quothmar
 *
 */
public class Stone extends Entity {
	
	// Stone object's sprite list.
	private static ArrayList<BufferedImage> sprites = new ArrayList<BufferedImage>();

	// Constructor.
	public Stone(int x, int y, String beginFacing, Playfield pf) {

	    super(x, y, beginFacing, pf);
		
		// A stone has a 32x32 bounding box.
		y -= 16;
		Fieldpoint fp1 = new Fieldpoint(x - 16, y - 16);
		Fieldpoint fp2 = new Fieldpoint(x + 16, y - 16);
		Fieldpoint fp3 = new Fieldpoint(x + 16, y + 16);
		Fieldpoint fp4 = new Fieldpoint(x - 16, y + 16);
		this.setBoundingPoly(new Polygon(fp1, fp2, fp3, fp4));

        this.setHeight(64);
        this.setState(Entity.ST_STONE_INTACT);
		this.attach();

	}

	// Stone's sprites.
	public static void loadSprites() {
        
        // For now, just include the stone sprite.
        try {
            String filename = "img\\STONE.gif";
            File imgfile = new File(filename);
            if (!imgfile.exists()) throw new Exception();
            sprites.add(0, ImageIO.read(imgfile));
        }
        catch (Exception e) {
            System.out.println("Exception while loading Stone sprites!");
            System.exit(1);
        }
        
    }
    public BufferedImage getCurrentSprite() { return sprites.get(0); }
	
	// Stone's state counter.
	public void tickStateCounter() {
		if (stateCounter == 0) return;
		--stateCounter;
	}

	// Stone's act method -- nothing yet.
    public void act() {}

} // end class Stone

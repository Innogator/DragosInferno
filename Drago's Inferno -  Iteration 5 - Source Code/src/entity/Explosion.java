/*
 * Explosion.java (v1.0)
 * 3/17/2013
 */
package entity;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import playfield.Fieldpoint;
import playfield.Playfield;
import playfield.Polygon;

/**
 * A small explosion that vanishes quickly.
 * 
 * @author Quothmar
 *
 */

public class Explosion extends Entity
{
    private static ArrayList<BufferedImage> sprites = new ArrayList<BufferedImage>();
    
    // Constructor.
    public Explosion(int x, int y, String beginFacing, Playfield pf) {
        
        // Call entity constructor with location and
        //   default direction.
        super(x, y, beginFacing, pf);

        // Set attributes.
        this.setHeight(0);
        this.setz(0);
        this.setState(Entity.ST_EXPLOSION);
        this.setStateCounter(24);
        this.setdx(0);
        this.setdy(0);
        this.setdz(0);
        
        // An explosion has a 40x40 bounding box (not really sure
        //   if this will be necessary, though!).
        Fieldpoint fp1 = new Fieldpoint(x - 20, y + 20);
        Fieldpoint fp2 = new Fieldpoint(x - 20, y - 20);
        Fieldpoint fp3 = new Fieldpoint(x + 20, y - 20);
        Fieldpoint fp4 = new Fieldpoint(x + 20, y + 20);
        this.setBoundingPoly(new Polygon(fp1, fp2, fp3, fp4));
        
        // Attach the explosion to the game grid.
        this.attach();
        
    }
    
    // Explosion's state counter.
    public void tickStateCounter() {
        
        // If ticked to zero, no more explosion.
        if (stateCounter == 0) {
            this.disappear();
            return;
        }
        
        // Decrement.
        --stateCounter;

    } // end method tickStateCounter
    
    // Static method to load the sprites.
    public static void loadSprites() {
        
        for (int i = 0; i < 5; ++i) {
            String filename = "img\\EXPLOSION_" + (i + 1) + ".gif";
            try {
                File imgfile = new File(filename);
                if (!imgfile.exists()) throw new Exception();
                sprites.add(i, ImageIO.read(imgfile));
            }
            catch (Exception e) {
                System.out.println("Exception while loading Explosion images!");
                e.printStackTrace();
                System.exit(1);
            }
        }

    } // end method loadSprites
    
    // Static methods to retrieve a sprite from the sprite list.
    private static BufferedImage getSprite(int number) {
        return sprites.get(number);
    }
    
    // Non-static method to obtain what the current sprite should be,
    //   based on the player's state, direction facing, and state
    //   counter.
    public BufferedImage getCurrentSprite() {

        int number = (24 - stateCounter) / 5;
        return getSprite(number);
        
    } // end method getCurrentSprite
    
    // No motion is ever attempted for Explosion object.
    public void act() { this.tickStateCounter(); }
    
}

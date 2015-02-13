/*
 * Fireball.java (v1.0)
 * 3/17/2013
 */
package entity;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import drago.DragoStatics;

import playfield.Fieldpoint;
import playfield.Playfield;
import playfield.Polygon;

/**
 * A small fireball, the player's initial projectile of attack.
 * 
 * @author Quothmar
 *
 */
public class Fireball extends Entity
{
    private static ArrayList<ArrayList<BufferedImage>>
        shotSprites = new ArrayList<ArrayList<BufferedImage>>();
    private static ArrayList<BufferedImage>
        hitSprites = new ArrayList<BufferedImage>();
    
    // Constructor.
    public Fireball(int x, int y, String beginFacing, Playfield pf) {
        
        // Call entity constructor with location and
        //   default direction.
        super(x, y, beginFacing, pf);

        // Set attributes.
        this.setHeight(15);
        this.setz(13);
        this.setState(Entity.ST_PLAYER_FIREBALL);
        this.setStateCounter(8);
        this.setSpeed(14);
        this.setdx(0);
        this.setdy(0);
        this.setdz(0);
        this.addWhereFacing(this.getSpeed(), true);
        
        // A fireball has a 15x15 bounding box.
        Fieldpoint fp1 = new Fieldpoint(x - 4, y + 7);
        Fieldpoint fp2 = new Fieldpoint(x - 4, y - 8);
        Fieldpoint fp3 = new Fieldpoint(x + 11, y - 8);
        Fieldpoint fp4 = new Fieldpoint(x + 11, y + 7);
        this.setBoundingPoly(new Polygon(fp1, fp2, fp3, fp4));
        
        // Attach the fireball to the game grid.
        this.attach();
        
    }
    
    // Method to tick the player's state counter.
    public void tickStateCounter() {
        
        // If ticked to zero in an extinguished state,
        //   the extinguished puff must disappear.
        if (stateCounter == 0) {
            if (this.getState() == Entity.ST_FIREBALL_EXTINGUISHING) {
                this.disappear();
            }
            return;
        }
        
        // Decrement.
        --stateCounter;
        
        // Switch sprite's "position", if needed. This must be done
        //   because a fireball elongates as it moves, requiring the
        //   (x, y)-center of the sprite to be adjusted relative to
        //   its bounding polygon. Also recenter if the fireball is
        //   extinguishing.
        if (    (this.getState() == Entity.ST_PLAYER_FIREBALL)
             && ((stateCounter == 4) || (stateCounter == 0)) )
        {
            //this.addWhereFacing(-5, false);
            this.addWhereFacing(0, false);
        }

    } // end method tickStateCounter
    
    // Static method to load the sprites.
    public static void loadSprites() {
        
        boolean VERBOSE = false;
        
        for (int i = 0; i < 8; ++i) {

            ArrayList<BufferedImage> nextSequence = new ArrayList<BufferedImage>();
            
            for (int j = 0; j < 3; ++j) {
                String direction = null;
                switch(i) {
                case Entity.F_UP: direction = "UP"; break;
                case Entity.F_UP_RIGHT: direction = "UP_RIGHT"; break;
                case Entity.F_RIGHT: direction = "RIGHT"; break;
                case Entity.F_DOWN_RIGHT: direction = "DOWN_RIGHT"; break;
                case Entity.F_DOWN: direction = "DOWN"; break;
                case Entity.F_DOWN_LEFT: direction = "DOWN_LEFT"; break;
                case Entity.F_LEFT: direction = "LEFT"; break;
                case Entity.F_UP_LEFT: direction = "UP_LEFT"; break;
                }
                String number = null;
                switch(j) {
                case 0: number = "1"; break;
                case 1: number = "2"; break;
                case 2: number = "3"; break;
                }
                String filename = "img\\FIREBALL_" + direction + "_" + number + ".gif";
                try {
                    File imgfile = new File(filename);
                    if (!imgfile.exists()) throw new Exception();
                    nextSequence.add(j, ImageIO.read(imgfile));
                }
                catch (Exception e) {
                    System.out.println("Exception while loading Fireball images!");
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            
            if (VERBOSE) System.out.println("Now adding a Fireball sequence (" + i + ")");
            shotSprites.add(i, nextSequence);
        }
        
        // Add fireball 'hit' sprites.
        for (int i = 0; i < 6; ++i) {
            String filename = "img\\FIREBALL_HIT_" + (i + 1) + ".gif";
            try {
                File imgfile = new File(filename);
                if (!imgfile.exists()) throw new Exception();
                hitSprites.add(i, ImageIO.read(imgfile));
            }
            catch (Exception e) {
                System.out.println("Exception while loading Fireball images!");
                e.printStackTrace();
                System.exit(1);
            }
        }
        
    } // end method loadSprites
    
    // Static methods to retrieve a sprite from the sprite list.
    private static BufferedImage getShotSprite(int direction, int number) {
        boolean VERBOSE = false;
        if (VERBOSE) System.out.println("Size of sprite array: " + shotSprites.size());
        return shotSprites.get(direction).get(number);
    }
    private static BufferedImage getHitSprite(int number) {
        return hitSprites.get(number);
    }
    
    // Non-static method to obtain what the current sprite should be,
    //   based on the player's state, direction facing, and state
    //   counter.
    public BufferedImage getCurrentSprite() {

        if (this.getState() == Entity.ST_PLAYER_FIREBALL) {
            int number = 0;
            if (5 <= stateCounter && stateCounter <= 8) number = 0;
            else if (1 <= stateCounter && stateCounter <= 4) number = 1;
            else number = 2;
            return getShotSprite(this.getFacing(), number);
        } else { // if (this.getState() == Entity.ST_FIREBALL_EXTINGUISHING) {
            int number = (17 - stateCounter) / 3;
            return getHitSprite(number);
        }
        
    } // end method getCurrentSprite
    
    // Action method. Called on each frame of the action loop.
    public void act() {
        
        this.attemptMotion(this.dx(), this.dy(), this.dz(), false, false);
        this.tickStateCounter();
        
    }
    
} // end class Fireball

/*
 * Player.java (v1.0)
 * 3/17/2013
 */
package entity;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import playfield.Fieldpoint;
import playfield.Polygon;
import playfield.Playfield;

// *Mike*:
//
//   This is the class whose information you will use to modify the fields of
// your heads-up display class. This will include things like eggs collected,
// amount of health, special moves acquired, score, and the like. You can be
// as creative as you want to be and even add in new attributes of class Player
// (as long as it does not impede existing functionality).
//
//   Feel free to contact me at (850) 575-7484 if you have any difficulties.
//
// -Scott
//

/**
 * The player (dragon).
 * 
 * @author Quothmar
 *
 */
public class Player extends Walker {

	// The player's sprite set.
    protected static ArrayList<ArrayList<ArrayList<BufferedImage>>>
            walkingSprites = new ArrayList<ArrayList<ArrayList<BufferedImage>>>();
    private static ArrayList<ArrayList<ArrayList<ArrayList<BufferedImage>>>>
            damageSprites = new ArrayList<ArrayList<ArrayList<ArrayList<BufferedImage>>>>();

    // The player's constructor.
	public Player(int x, int y, String beginFacing, Playfield pf) {
		
		super(x, y, beginFacing, pf);

        boolean CRAZY_BOUNDING_POLYGON = false; // For testing.
		
		if (CRAZY_BOUNDING_POLYGON) {
		    Fieldpoint fp1 = new Fieldpoint(x - 47, y - 21);
		    Fieldpoint fp2 = new Fieldpoint(x - 38, y - 46);
		    Fieldpoint fp3 = new Fieldpoint(x + 22, y - 13);
		    Fieldpoint fp4 = new Fieldpoint(x + 31, y + 27);
		    Fieldpoint fp5 = new Fieldpoint(x + 16, y + 35);
		    this.setBoundingPoly(new Polygon(fp1, fp2, fp3, fp4, fp5));
		}
		else {
    		// The player has a 64x64 bounding box (this may
    		//   change as our current sprites are not box-shaped).
    		Fieldpoint fp1 = new Fieldpoint(x - 32, y - 32);
    		Fieldpoint fp2 = new Fieldpoint(x + 32, y - 32);
    		Fieldpoint fp3 = new Fieldpoint(x + 32, y + 32);
    		Fieldpoint fp4 = new Fieldpoint(x - 32, y + 32);
    		this.setBoundingPoly(new Polygon(fp1, fp2, fp3, fp4));
		}
		
		this.setHeight(64);
		this.setSpeed(7);
		this.attach();
		
	}

	// Player's sprites.
    public static void loadSprites() {
        Walker.loadSprites("PLAYER", walkingSprites);
    }
    
    // Derived sprites of a 'white-or-black' nature, used to flicker and
    //   signify that the player has just taken damage. Not used yet.
    //   (Maybe move the method to 'Walker' or another superclass?)
    public static void loadDamageSprites() {
        ArrayList<BufferedImage> nextSequence;
        ArrayList<ArrayList<BufferedImage>> nextSet;
        ArrayList<ArrayList<ArrayList<BufferedImage>>> nextColor;
        BufferedImage transformedSprite = null;
        BufferedImage normalSprite = null;
        for (int color = 0; color < 2; ++color) {
            nextColor = new ArrayList<ArrayList<ArrayList<BufferedImage>>>();
            for (int set = 0; set < 2; ++set) {
                nextSet = new ArrayList<ArrayList<BufferedImage>>();
                for (int direction = 0; direction < 8; ++direction) {
                    int numInSet = (set == 0) ? 1 : 4;
                    nextSequence = new ArrayList<BufferedImage>();
                    for (int number = 0; number < numInSet; ++number) {
                        normalSprite = walkingSprites.get(set).get(direction).get(number);
                        transformedSprite = new BufferedImage(
                                normalSprite.getWidth(),
                                normalSprite.getHeight(),
                                BufferedImage.TYPE_INT_ARGB);
                        for (int x = 0; x < transformedSprite.getWidth(); ++x) {
                            for (int y = 0; y < transformedSprite.getHeight(); ++y) {
                                int rgbvalue = normalSprite.getRGB(x, y);
                                int blue  = (rgbvalue       & 255);
                                int green = ((rgbvalue>>8)  & 255);
                                int red   = ((rgbvalue>>16) & 255);
                                int alpha = ((rgbvalue>>24) & 255);
                                int white = 255 + (255<<8) + (255<<16); 
                                if (red != 0 || green != 0 || blue != 0)
                                    transformedSprite.setRGB(x, y, (alpha<<24) + (color*white));
                            }
                        }
                        nextSequence.add(number, transformedSprite);
                    }
                    nextSet.add(direction, nextSequence);
                }
                nextColor.add(set, nextSet);
            }
            damageSprites.add(color, nextColor);
        }
    }               
    
    private static BufferedImage getSprite(int set, int direction, int number) {
        return walkingSprites.get(set).get(direction).get(number);
    }
    public BufferedImage getCurrentSprite() {
        int set = 0;
        int n = 0;
        int sc = this.getStateCounter();
        switch(this.getState()) {
        case Entity.ST_PLAYER_WALKING:  set = Walker.SP_WALKING; n = (sc%40)/10; break;
        case Entity.ST_PLAYER_STANDING: set = Walker.SP_STANDING; n = 0; break;
        default:
            System.out.println("Invalid state found in Player");
            System.exit(1);
        }
        return getSprite(set, this.getFacing(), n);
    }
    
	// Player's state counter.
	public void tickStateCounter() {
		if (stateCounter == 0) return;
		else --stateCounter;
		if (stateCounter == 0) {
			switch (this.getState()) {
			case Entity.ST_PLAYER_WALKING: stateCounter = 40; break;
			case Entity.ST_PLAYER_STANDING: break;
			}
		}
	}
	
	// Player's act method.
	public void act() {
	    this.attemptMotion(this.dx(), this.dy(), this.dz(), false, false);
		this.tickStateCounter();
	}
	
} // end class Player

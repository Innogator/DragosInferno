/*
 * Walker.java (v1.0)
 * 3/17/2013
 */
package entity;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import playfield.Playfield;

/**
 * A 'walking' entity, such as a player or enemy human. Class
 * created to consolidate some repeated code, such as that
 * used to load all the sprites.
 * 
 * @author Quothmar
 *
 */
abstract public class Walker extends Entity
{
    // Sprite indices: sprite set. (Superclass 'Entity' already has
    //   direction indices.)
    protected static final int SP_STANDING = 0;
    protected static final int SP_WALKING = 1;
    
    // Four-parameter constructor.
    public Walker(int xstart, int ystart, String beginFacing, Playfield pf) {
        super(xstart, ystart, beginFacing, pf);
    }

    // Load the walker's sprites: all these entities have the same sprite
    //   file-naming pattern, so the method to load them is the same.
    public static void loadSprites(String entname,
            ArrayList<ArrayList<ArrayList<BufferedImage>>> spriteArray) {
        
        try {
            
            // Loop first by set.
            for (int i = 0; i < 2; ++i) {
                
                // Create an array of sprite sequences for this set (currently STANDING-WALKING).
                ArrayList<ArrayList<BufferedImage>> nextSet = new ArrayList<ArrayList<BufferedImage>>();
                
                // Get the string representing the set.
                String set = null;
                int index = 0;
                switch (i) {
                case 0: set = "STANDING";   index = SP_STANDING;    break;
                case 1: set = "WALKING";    index = SP_WALKING;     break;
                default: System.out.println("Invalid sprite set"); System.exit(1);
                }
                
                // Loop second by eight directions.
                for (int j = 0; j < 8; ++j) {
                    
                    // Create a sequence of sprites for this direction (e.g., 1-2-3-4 for walking).
                    ArrayList<BufferedImage> nextSequence = new ArrayList<BufferedImage>();
                    
                    // Get the string representing the direction.
                    String direction = null;
                    switch (j) {
                    case Entity.F_UP:           direction = "UP";           break;
                    case Entity.F_UP_RIGHT:     direction = "UP_RIGHT";     break;
                    case Entity.F_RIGHT:        direction = "RIGHT";        break;
                    case Entity.F_DOWN_RIGHT:   direction = "DOWN_RIGHT";   break;
                    case Entity.F_DOWN:         direction = "DOWN";         break;
                    case Entity.F_DOWN_LEFT:    direction = "DOWN_LEFT";    break;
                    case Entity.F_LEFT:         direction = "LEFT";         break;
                    case Entity.F_UP_LEFT:      direction = "UP_LEFT";      break;
                    default:
                        System.out.println("Invalid direction");
                        System.exit(1);
                    }
                    
                    // If standing, just get the sprite.
                    if (set == "STANDING") {
                        
                        // Load the file and add it (as singleton) to sprite sequence.
                        String filename = "img\\" + entname + "_STANDING_" + direction + ".gif";
                        File imgfile = new File(filename);
                        if (!imgfile.exists()) throw new Exception();
                        nextSequence.add(0, ImageIO.read(imgfile));
                        
                    }
                    
                    // Otherwise, loop third by the four animation frames.
                    else {
                    
                        for (int k = 0; k < 4; ++k) {
                            
                            // Get the string for the number.
                            String number = null;
                            switch (k) {
                            case 0: number = "1"; break;
                            case 1: number = "2"; break;
                            case 2: number = "3"; break;
                            case 3: number = "4"; break;
                            }
                            
                            // Get filename and load the sprite.
                            String filename = "img\\" + entname + "_" + set + "_" 
                                    + direction + "_" + number + ".gif";
                            File imgfile = new File(filename);
                            if (!imgfile.exists()) throw new Exception();
                            nextSequence.add(k, ImageIO.read(imgfile));
                            
                        } // end for (by animation frame)
                    } // end if (standing or walking)
                    
                    // Add the sprite sequence for this direction to the set.
                    nextSet.add(j, nextSequence);
                    
                } // end for (by each of eight directions)
                
                // Add this set to the final sprite array.
                spriteArray.add(index, nextSet);
                
            } // end for (by sprite set)
        } // end try
        catch (Exception e) {
            
            JOptionPane.showMessageDialog(null,
                    "Exception while loading Player images!");
            System.exit(1);
            
        } // end catch
    } // end method loadSprites
} // end class Walker

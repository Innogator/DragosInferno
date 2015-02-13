/*
 * DragoStatics.java (v1.0)
 * 3/17/2013
 */
package drago;

import java.awt.Graphics2D;
import java.awt.Color;
import java.io.File;
import java.util.Random;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

/**
 * These are constants and functions used universally throughout
 * the game's implementation. Such common functions include 'sign',
 * 'sqrt', and 'playSound'. The call to 'sqrt' uses a square root
 * table to optimize calculation time.
 * 
 * Also included are common geometrical constants that refer to
 * orientations in one of the eight directions (up, up-right, right,
 * etc.).
 * 
 * @author Quothmar
 *
 */
public class DragoStatics
{
    // Some basic geometric constants.
    public static final int UPPER = 0;
    public static final int RIGHT = 1;
    public static final int LOWER = 2;
    public static final int LEFT = 3;
    public static int getOppSide(int side) { return (side + 2) % 4; }
    
    // Integer sign.
    public static int sign(int x) {
        return (x < 0) ? -1 : ((x > 0) ? 1 : 0);
    }
    
    // Long integer sign.
    public static int sign(long x) {
        return (x < 0) ? -1 : ((x > 0) ? 1 : 0);
    }
    
    // Square root table (for faster processing).
    private static final int MAX_SQUARE = 2000;
    private static int[] sqrt = new int[MAX_SQUARE];
    public static void loadSquareRootTable() {
        for (int i = 0; i < MAX_SQUARE; ++i)
            sqrt[i] = (int)Math.sqrt((double)i);
    }
    public static int sqrt(int x) {
        if (x < MAX_SQUARE) return sqrt[x];
        else return (int)(Math.sqrt((double)x));
    }
    
    // Random number table.
    public static final int NUM_RANDOMS = 2000;
    public static final int MAX_RANDOM = 10000;
    public static int[] randomList = new int[NUM_RANDOMS];
    public static void loadRandoms() {
        Random random = new Random();
        for (int i = 0; i < NUM_RANDOMS; ++i) {
            randomList[i] = random.nextInt(MAX_RANDOM);
        }
    }
    
    // Method to draw a shaded string (as can be useful against a white background).
    public static void drawShadedString(String text, int x, int y, Graphics2D g2d) {
        Color color = g2d.getColor();
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, x - 2, y + 2);
        g2d.setColor(color);
        g2d.drawString(text, x, y);
    }
    
    // Play a given sound file in the 'sfx\' directory.
    public static void playSound(String sndfile) {
        try {
            final Clip clip = AudioSystem.getClip();
            File sfx = new File("sfx\\" + sndfile);
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(sfx);
            clip.addLineListener(
                new LineListener()
                {
                public void update(LineEvent e) {
                    if (e.getType() == LineEvent.Type.STOP)
                        clip.close();
                }
            } );
            clip.open(inputStream);
            clip.start();
            inputStream.close();
        }
        catch (Exception e) {
            System.out.println("Exception while playing sound!");
            System.exit(1);
        }
    }

}

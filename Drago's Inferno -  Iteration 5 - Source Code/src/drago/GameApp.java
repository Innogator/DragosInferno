/*
 * GameApp.java (v1.0)
 * 3/17/2013
 */
package drago;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import javax.swing.JFrame;
//import java.awt.Graphics;
//import java.awt.image.BufferStrategy;

import entity.*;

/**
 * The main method that contains the event handling and the
 * game loop.
 * 
 * @author Quothmar
 *
 */
public class GameApp {

    private static final boolean VERBOSE = false;
    
    // Constants to keep track of what keys are down.
	private static boolean prLeft = false;
	private static boolean prRight = false;
	private static boolean prUp = false;
	private static boolean prDown = false;
	
	// Constants for special ability keys being held down
	private static boolean prZ = false;
	private static boolean prX = false;
	private static boolean prC = false;
	private static boolean prV = false;
	
	// The JFrame (window) and the GamePanel, where the playfield
	//   is drawn.
	private static JFrame f;
	private static GamePanel gp;

	// The dimensions of the "active region", i.e., the region within
	//   which certain entities are capable of moving and acting.
	private static final int activeRegionWidth = 2048;
	private static final int activeRegionHeight = 1536;
	
	// Milliseconds per tick; can be used to change the game speed.
	private static final long millisPerTick = 15; // For 60 FPS, use 15
	
	// The game.
	public static void main(String[] args) {
		
	    // Load tables.
        DragoStatics.loadRandoms();
	    DragoStatics.loadSquareRootTable();
	    
	    // Load collision table.
	    CollisionTable.load();
	    
	    // Create JFrame and set properties.
	    f = new JFrame("Drago's Inferno");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setLayout(null);
		f.setBounds(20, 20, 740, 600);
		f.setVisible(true);
		
		//f.setIgnoreRepaint(true); // Double-buffering test
		//f.createBufferStrategy(4);
		//final BufferStrategy bufferStrategy = f.getBufferStrategy();
		
		// Create game panel and set properties.
		gp = new GamePanel();
		gp.setLayout(null);
		gp.setBounds(10, 10, 640, 480);

		// Add the game panel to the JFrame.
        f.add(gp);
		
        // Keyboard event handling.
		gp.addKeyListener(
			new KeyListener()
			{
				// Implementation of key events.
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_LEFT) { prLeft = true; }
					else if (e.getKeyCode() == KeyEvent.VK_RIGHT) { prRight = true; }
					else if (e.getKeyCode() == KeyEvent.VK_UP) { prUp = true; }
					else if (e.getKeyCode() == KeyEvent.VK_DOWN) { prDown = true; }
					else if (e.getKeyCode() == KeyEvent.VK_F) { gp.pf.player.addSpeed(1); }
					else if (e.getKeyCode() == KeyEvent.VK_S) { gp.pf.player.addSpeed(-1);}
					else if (e.getKeyCode() == KeyEvent.VK_Z) { prZ = true; }
					else if (e.getKeyCode() == KeyEvent.VK_X) { prX = true; }
					else if (e.getKeyCode() == KeyEvent.VK_C) { prC = true; }
					else if (e.getKeyCode() == KeyEvent.VK_V) { prV = true;	}
					else if (e.getKeyCode() == KeyEvent.VK_F1) { gp.AILINES = !gp.AILINES; }
					else if (e.getKeyCode() == KeyEvent.VK_F2) { gp.FULLPARTITIONS = !gp.FULLPARTITIONS; }
					else if (e.getKeyCode() == KeyEvent.VK_F3) { gp.PORTALS = !gp.PORTALS; }
					else if (e.getKeyCode() == KeyEvent.VK_F11) { /* Debug entry point */ }
				}
				public void keyReleased(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_LEFT) { prLeft = false; }
					else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {	prRight = false; }
					else if (e.getKeyCode() == KeyEvent.VK_UP) { prUp = false; }
					else if (e.getKeyCode() == KeyEvent.VK_DOWN) { prDown = false; }
					else if (e.getKeyCode() == KeyEvent.VK_Z) { prZ = false; }
					else if (e.getKeyCode() == KeyEvent.VK_X) { prX = false; }
					else if (e.getKeyCode() == KeyEvent.VK_C) { prC = false; }
					else if (e.getKeyCode() == KeyEvent.VK_V) { prV = false; }
				}
				public void keyTyped(KeyEvent e) {}

			}
		);

		// Set focus to the game panel and make it visible.
        gp.setFocusable(true);
		gp.requestFocusInWindow();
		gp.setVisible(true);

		//DragoStatics.loadSounds();
		
        // Initialize active entity list by adding all enemies near
		//   the player's starting point.
        Player ply = gp.pf.player;
        ArrayList<Entity> newEntities = new ArrayList<Entity>();
		int sqxbegin = ply.x() - (activeRegionWidth>>1);
		int sqxend = ply.x() + (activeRegionWidth>>1);
        int sqybegin = ply.y() - (activeRegionHeight>>1);
        int sqyend = ply.y() + (activeRegionHeight>>1);
        sqxbegin /= 32;
        sqxend   /= 32;
        sqybegin /= 32;
        sqyend   /= 32;
        for (int sqx = sqxbegin; sqx <= sqxend; ++sqx) {
            for (int sqy = sqybegin; sqy <= sqyend; ++sqy) {
                if ((sqx < 0) || (sqx > gp.pf.gridWidth() - 1)) continue;
                if ((sqy < 0) || (sqy > gp.pf.gridHeight() - 1)) continue;
                for (int i = 0; i < gp.pf.gameGrid[sqx][sqy].entities.size(); ++i) {
                    Entity t = gp.pf.gameGrid[sqx][sqy].entities.get(i);
                    if (!newEntities.contains(t)) newEntities.add(t);
                }
            }
        }
        for (int i = 0; i < newEntities.size(); ++i) {
            gp.pf.addActiveEntity(newEntities.get(i));
        }
		
        // Some counter variables for use in the game. (Move them somewhere
        //   else, maybe?)
        final int fireballDelay = 4;
        int fireballRecoveryTime = 0;
        
        // The action loop for gameplay. We'll put more stuff in here later.
		//   For now, this just allows the user to scroll and see the map.
		while (true) {
			
			// Record current time for framerate adjustment.
		    long begin = System.currentTimeMillis();
			
			// If player is in a standing or walking state, allow the player
			//   to change his/her own dx, dy. Also allow player to use
			//   special abilities.
			if (    ply.getState() == Entity.ST_PLAYER_STANDING
			     || ply.getState() == Entity.ST_PLAYER_WALKING )
			{
			    // Adjust player's dx/dy if player wants to walk.
			    int speed = ply.getSpeed();
			    if (prUp) ply.setdy(speed);
			    else if (prDown) ply.setdy(-speed);
			    else ply.setdy(0);
			    if (prLeft) ply.setdx(-speed);
			    else if (prRight) ply.setdx(speed);
			    else ply.setdx(0);
			    
		         // Set direction facing based on dx/dy.
			    int dir = ply.getFacing();
	            if      (ply.dx() <  0 && ply.dy() <  0) dir = Entity.F_DOWN_LEFT;
	            else if (ply.dx() <  0 && ply.dy() == 0) dir = Entity.F_LEFT;
	            else if (ply.dx() <  0 && ply.dy() >  0) dir = Entity.F_UP_LEFT;
	            else if (ply.dx() == 0 && ply.dy() <  0) dir = Entity.F_DOWN;
	            else if (ply.dx() == 0 && ply.dy() >  0) dir = Entity.F_UP;
	            else if (ply.dx() >  0 && ply.dy() <  0) dir = Entity.F_DOWN_RIGHT;
	            else if (ply.dx() >  0 && ply.dy() == 0) dir = Entity.F_RIGHT;
	            else if (ply.dx() >  0 && ply.dy() >  0) dir = Entity.F_UP_RIGHT;
	            ply.setFacing(dir);
	            
	            // If now walking, set state to 'walking'. Otherwise, if now standing,
	            //   set state to 'standing'.
	            if (ply.dx() != 0 || ply.dy() != 0) {
	                if (ply.getState() != Entity.ST_PLAYER_WALKING) { 
	                    ply.setState(Entity.ST_PLAYER_WALKING);
	                    ply.setStateCounter(40);
	                }
	            }
	            else ply.setState(Entity.ST_PLAYER_STANDING);
	            
	            // Special ability 1: fireball (granted from start of game).
	            if ((prZ || prX || prC || prV) && (fireballRecoveryTime == 0)) {
	                Fireball fb = new Fireball(
	                        ply.x(),
	                        ply.y(),
	                        Entity.getStringForFacing(ply.getFacing()),
	                        gp.pf);
	                gp.pf.addEntity(fb);
	                gp.pf.addActiveEntity(fb);
	                fb.addWhereFacing(14, false);
	                fireballRecoveryTime = fireballDelay;
	                DragoStatics.playSound("FIREBALL_1.wav");
	            }
	            else if (fireballRecoveryTime > 0) --fireballRecoveryTime;
	            
			} // end if (player can stand/walk)
            
			// Let the player act.
			ply.act();
			
			// If any entities are now within the active region, add
			//   them to the active entity list of the playfield.
			newEntities.clear();
			if (ply.dx() != 0) {
			    int sqstep = DragoStatics.sign(ply.dx());
			    sqxend = ply.x() + sqstep*(activeRegionWidth>>1);
			    sqxbegin = sqxend - ply.dx();
			    sqxbegin >>= 5;
			    sqxend >>= 5;
			    sqybegin = ply.y() - (activeRegionHeight>>1);
			    sqyend = ply.y() + (activeRegionHeight>>1);
			    sqybegin >>= 5;
			    sqyend >>= 5;
			    int sqx = sqxbegin;
			    while (true) {
			        for (int sqy = sqybegin; sqy <= sqyend; ++sqy) {
			            if ((sqx < 0) || (sqx > gp.pf.gridWidth() - 1)) continue;
                        if ((sqy < 0) || (sqy > gp.pf.gridHeight() - 1)) continue;
			            for (int i = 0; i < gp.pf.gameGrid[sqx][sqy].getNumEntities(); ++i) {
			                Entity t = gp.pf.gameGrid[sqx][sqy].entities.get(i);
			                if (!newEntities.contains(t)) newEntities.add(t);
			            }
			        }
                    if (sqx == sqxend) break;
                    else sqx += sqstep;
			    }
			}
            if (ply.dy() != 0) {
                int sqstep = DragoStatics.sign(ply.dy());
                sqyend = ply.y() + sqstep*(activeRegionHeight>>1);
                sqybegin = sqyend - ply.dy();
                sqybegin >>= 5;
                sqyend >>= 5;
                sqxbegin = ply.x() - (activeRegionWidth>>1);
                sqxend = ply.x() + (activeRegionWidth>>1);
                sqxbegin >>= 5;
                sqxend >>= 5;
                int sqy = sqybegin;
                while (true) {
                    for (int sqx = sqxbegin; sqx <= sqxend; ++sqx) {
                        if ((sqx < 0) || (sqx > gp.pf.gridWidth() - 1)) continue;
                        if ((sqy < 0) || (sqy > gp.pf.gridHeight() - 1)) continue;
                        for (int i = 0; i < gp.pf.gameGrid[sqx][sqy].getNumEntities(); ++i) {
                            Entity t = gp.pf.gameGrid[sqx][sqy].entities.get(i);
                            if (!newEntities.contains(t)) newEntities.add(t);
                        }
                    }
                    if (sqy == sqyend) break;
                    else sqy += sqstep;
                }
            }
            for (int i = 0; i < newEntities.size(); ++i)
                gp.pf.addActiveEntity(newEntities.get(i));
                
			// Tell each entity within the active region to act one framestep.
            for (int i = 0; i < gp.pf.numActiveEntities(); ++i) {
                Entity t = gp.pf.getActiveEntity(i);
                int oldentx = t.x();
                int oldenty = t.y();
                if (!(t instanceof Player)) t.act();
                if (t instanceof Fireball) {
                    if (VERBOSE) System.out.println("Old fireball position: (" + oldentx
                            + ", " + oldenty + "), new fireball position: (" + t.x()
                            + ", " + t.y() + ")");
                    if (VERBOSE) System.out.println("Player's position: (" + ply.x()
                            + ", " + ply.y() + ")");
                }
            }
			
			// If any active entities are now off the active region,
			//   remove them from the active entity list. Don't do
            //   this for fireballs and other projectiles as it would
            //   be unrealistic.
			for (int i = 0; i < gp.pf.numActiveEntities(); ++i) {
			    Entity t = gp.pf.getActiveEntity(i);
			    if (t instanceof Fireball) continue;
			    if (t instanceof Explosion) continue;
			    int distx = Math.abs(ply.x() - t.x());
			    int disty = Math.abs(ply.y() - t.y());
			    if (    (distx > (activeRegionWidth>>1))
			         || (disty > (activeRegionHeight>>1)) )
			    {
			        gp.pf.removeActiveEntity(t);
			    }
			}
			
			// Set the camera based on player's position.
			gp.setXCam(ply.x() - 320);
			gp.setYCam(ply.y() - 240);
			if (gp.getXCam() < 0) gp.setXCam(0);
			if (gp.getXCam() > gp.pf.levelWidth() - 640)
			    gp.setXCam(gp.pf.levelWidth() - 640);
			if (gp.getYCam() < 0) gp.setYCam(0);
			if (gp.getYCam() > gp.pf.levelHeight() - 480)
			    gp.setYCam(gp.pf.levelHeight() - 480);
			
			// Repaint the game panel.
			gp.repaint();

			// Wait for the next frame.
			while (System.currentTimeMillis() - begin < millisPerTick) {}
			
			// Get FPS for display in the GamePanel.
			int fps = (int)(1000/(System.currentTimeMillis() - begin));
			gp.fps = fps;
			
		} // end while (main game loop)
	} // end main
} // end class PortalViewApp

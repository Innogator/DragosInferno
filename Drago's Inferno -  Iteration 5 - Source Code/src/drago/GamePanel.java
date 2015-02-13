/*
 * GamePanel.java (v1.0)
 * 3/17/2013
 */
package drago;

import java.util.ArrayList;
import java.util.Vector;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
//import java.awt.image.BufferStrategy;
import java.io.File;
import javax.swing.JPanel;
import javax.imageio.ImageIO;

import playfield.*;

import entity.Archer;
import entity.BlueEgg;
import entity.Explosion;
import entity.Fireball;
import entity.GreenEgg;
import entity.Player;
import entity.RedEgg;
import entity.Stone;
import entity.Tree;
import entity.Entity;
import entity.YellowEgg;

/**
 * The panel that shows the game's playfield.
 * 
 * @author Quothmar
 *
 */
public class GamePanel extends JPanel {

    private boolean VERBOSE = false;

    // Testing booleans.
    public boolean PORTALS = false;
    public boolean AILINES = false;
    public boolean FILLGRID = false;
    public boolean FULLPARTITIONS = false;
    
    // Camera position measured by position of lower-left
	//   corner. Starts at lower-left corner of map.
	private int xcam = 0;
	private int ycam = 0;
	
	// Getters and setters for camera.
	public int getXCam() { return xcam; }
	public int getYCam() { return ycam; }
	public void setXCam(int x) { xcam = x; }
	public void setYCam(int y) { ycam = y; }
	public void setCam(int x, int y) { xcam = x; ycam = y; }
	public void moveCam(int dx, int dy) { xcam += dx; ycam += dy; }
	
	// The walls (each array currently holds
	//   eight buffered images). Prefix them with
	//   SP_ too, to follow Java's enum rule.
	private ArrayList<BufferedImage> SP_BRICK_WALL_RED = new ArrayList<BufferedImage>();
	private ArrayList<BufferedImage> SP_BRICK_WALL_WHITE = new ArrayList<BufferedImage>();
	private ArrayList<BufferedImage> SP_BROWN_CAVE_WALL = new ArrayList<BufferedImage>();
	private ArrayList<BufferedImage> SP_IRON_WALL = new ArrayList<BufferedImage>();
	private ArrayList<BufferedImage> SP_STONE_WALL = new ArrayList<BufferedImage>();
	private ArrayList<BufferedImage> SP_GIMP_BRICK_1 = new ArrayList<BufferedImage>();
	
	// The ground textures (five per array).
	private ArrayList<BufferedImage> SP_GRASS = new ArrayList<BufferedImage>();
	private ArrayList<BufferedImage> SP_HIGH_GRASS = new ArrayList<BufferedImage>();
	private ArrayList<BufferedImage> SP_DIRT = new ArrayList<BufferedImage>();
	private ArrayList<BufferedImage> SP_LAVA = new ArrayList<BufferedImage>();
	private ArrayList<BufferedImage> SP_SAND = new ArrayList<BufferedImage>();
	private ArrayList<BufferedImage> SP_WATER = new ArrayList<BufferedImage>();	
	private ArrayList<BufferedImage> SP_DUNGEON_TILE_1 = new ArrayList<BufferedImage>();
	private ArrayList<BufferedImage> SP_ICE = new ArrayList<BufferedImage>();
	private ArrayList<BufferedImage> SP_GRAY_ROCK_GROUND = new ArrayList<BufferedImage>();

	// Some constants to help us decide which
	//   array index we are on with the wall and
	//   ground sprites.
	private final int TX_FULL = 0;
	private final int TX_LOWER_LEFT = 1;
	private final int TX_LOWER_RIGHT = 2;
	private final int TX_UPPER_LEFT = 3;
	private final int TX_UPPER_RIGHT = 4;
	private final int TX_FRONT = 5;
	private final int TX_LEFT = 6;
	private final int TX_RIGHT = 7;

	// The playfield object (contained in this case
	//   by the game panel).
	public Playfield pf;
	
	// For frames-per-second recording.
	public int fps;
		
	// A quick method to get the sprite array for any given texture.
	private ArrayList<BufferedImage> getSpriteArray(Texture t) {
		switch(t) {
		case BRICK_WALL_RED:      return SP_BRICK_WALL_RED;
		case BRICK_WALL_WHITE:    return SP_BRICK_WALL_WHITE;
		case BROWN_CAVE_WALL:     return SP_BROWN_CAVE_WALL;
		case IRON_WALL:           return SP_IRON_WALL;
		case STONE_WALL:          return SP_STONE_WALL;
		case GIMP_BRICK_1:        return SP_GIMP_BRICK_1;
		case GRASS:               return SP_GRASS;
		case HIGH_GRASS:          return SP_HIGH_GRASS;
		case DIRT:                return SP_DIRT;
		case LAVA:                return SP_LAVA;
		case SAND:                return SP_SAND;
		case WATER:               return SP_WATER;	
		case DUNGEON_TILE_1:      return SP_DUNGEON_TILE_1;
		case GRAY_ROCK_GROUND:    return SP_GRAY_ROCK_GROUND;
		}
		return null;
	}
	
	// Constructor for the game panel.
	public GamePanel() {

		try	{
			
			// Load all the wall and ground images.
			for (int i = 0; i < 15; ++i) {
				
				// Get the next texture name and the array
				//   name corresponding to the index.
				String texname = "";
				ArrayList<BufferedImage> array = null;
				switch(i) {
				case 0:  texname = "BRICK_WALL_RED";    array = SP_BRICK_WALL_RED;      break;
				case 1:  texname = "BRICK_WALL_WHITE";  array = SP_BRICK_WALL_WHITE;    break;
				case 2:  texname = "BROWN_CAVE_WALL";   array = SP_BROWN_CAVE_WALL;     break;
				case 3:  texname = "IRON_WALL";         array = SP_IRON_WALL;           break;
				case 4:  texname = "STONE_WALL";        array = SP_STONE_WALL;          break;
				case 5:  texname = "GIMP_BRICK_1";      array = SP_GIMP_BRICK_1;        break;
				case 6:  texname = "GRASS";             array = SP_GRASS;               break;
				case 7:  texname = "HIGH_GRASS";        array = SP_HIGH_GRASS;          break;
				case 8:  texname = "DIRT";              array = SP_DIRT;                break;
				case 9:  texname = "LAVA";              array = SP_LAVA;                break;
				case 10: texname = "SAND";              array = SP_SAND;                break;
				case 11: texname = "WATER";             array = SP_WATER;               break;
				case 12: texname = "DUNGEON_TILE_1";    array = SP_DUNGEON_TILE_1;      break;
				case 13: texname = "ICE";               array = SP_ICE;                 break;
				default: texname = "GRAY_ROCK_GROUND";  array = SP_GRAY_ROCK_GROUND;
				}
				
				// Load each sprite (5 for flat textures, plus additional
				//   3 for wall textures). Note: the calculation of 'spritecount'
				//   is dependent on the switch statement above.
				int spritecount = (i < 6 ? 8 : 5);
				for (int j = 0; j < spritecount; ++j) {
					
					// Get the suffix of the flat sprite.
					String suffix = "";
					switch(j) {
					case TX_FULL:        suffix = "FULL";          break;
					case TX_LOWER_LEFT:  suffix = "LOWER_LEFT";    break;
					case TX_LOWER_RIGHT: suffix = "LOWER_RIGHT";   break;
					case TX_UPPER_LEFT:  suffix = "UPPER_LEFT";    break;
					case TX_UPPER_RIGHT: suffix = "UPPER_RIGHT";   break;
					case TX_FRONT:       suffix = "FRONT";         break;
					case TX_LEFT:        suffix = "LEFT";          break;
					case TX_RIGHT:       suffix = "RIGHT";
					}
					
					// Concatenate name and suffix to get
					//   file name. Note: this presumes that
					//   ALL of the sprites are named according
					//   to the exact convention that I have
					//   outlined, a convention which matches the
					//   identifiers listed on the Sprite Design
					//   thread (see our Forum for more details).
					String filename = "img\\" + texname + "_" + suffix + ".gif";
					
					// Get the file.
					File imgfile = new File(filename);
					
					// If it doesn't exist, throw an exception.
					if (!imgfile.exists()) throw new Exception();

					// Otherwise, read it into the corresponding
					//   buffered image variable.
					array.add(j, ImageIO.read(imgfile));

				} // end for (by sprite of current texture)
			} // end for (by texture)
			
			// Load entity sprites.
			Player.loadSprites();
			Player.loadDamageSprites(); // Not used yet.
			Archer.loadSprites();
			Tree.loadSprites();
			Stone.loadSprites();
			Fireball.loadSprites();
			Explosion.loadSprites();
			RedEgg.loadSprites();
			GreenEgg.loadSprites();
			BlueEgg.loadSprites();
			YellowEgg.loadSprites();
			
            // Get the playfield object.
            pf = new Playfield();
            pf.loadLevel("Prototype Playfield.dat");
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	
	} // end constructor

	// Method to paint the GamePanel -- i.e., fill it with the playfield's
	//   contents at the camera's location.
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
	
		Graphics2D g2d = (Graphics2D)g;

		// Get upper-left corner of camera.
		int x0 = xcam;
		int y0 = ycam + 480;
	
		// Get leftmost and rightmost columns of grid squares.
		int gridx1 = x0 >> 5;
		int gridx2 = (x0 + 640) >> 5;
			
		// Get uppermost and lowermost columns of grid squares.
		//   Here, y1 > y2 (y1 goes down to y2).
		int gridy1 = y0 >> 5;
		int gridy2 = ycam >> 5 + 2;
		
		// STEP 1: Draw all the flat, ground-based textures.
		for (int y = gridy1; y >= gridy2; --y) {
			for (int x = gridx1; x <= gridx2; ++x) {
				
				// If not in game grid, ignore.
				if (    x < 0 || x > pf.gridWidth() - 1
					 || y < 0 || y > pf.gridHeight() - 1 )
				{
					continue;
				}
				
				// Get information about the textures in this grid square.
				Texture upper = pf.gameGrid[x][y].getTexture(DragoStatics.UPPER);
				Texture lower = pf.gameGrid[x][y].getTexture(DragoStatics.LOWER);
				Texture left  = pf.gameGrid[x][y].getTexture(DragoStatics.LEFT);
				
				// Get sprites to draw.
				ArrayList<BufferedImage> uppertext = this.getSpriteArray(upper);
				ArrayList<BufferedImage> lowertext = this.getSpriteArray(lower);
				
				// Get location of sprite to draw on panel.
				int xsprite = (x << 5) - xcam;
				int ysprite = 480 - ((y << 5) - ycam) - 32;
				
				// Now select the appropriate two textures to draw and
				//   draw them.
				if (!Texture.isWall(upper)) {
    				if (upper == left) {
    					g2d.drawImage(uppertext.get(TX_UPPER_LEFT),  xsprite, ysprite, null);
    				} else {
                        g2d.drawImage(uppertext.get(TX_UPPER_RIGHT), xsprite, ysprite, null);
    				}
				}
				if (!Texture.isWall(lower)) {
				    if (upper == left) {
	                    g2d.drawImage(lowertext.get(TX_LOWER_RIGHT),  xsprite, ysprite, null);
				    } else {
    					g2d.drawImage(lowertext.get(TX_LOWER_LEFT), xsprite, ysprite, null);
				    }
				}
				
			} // end for (by column/grid square)
		} // end for (by row)
		
		// STEP 2: Loop by row of grid squares, first painting the upper walls,
		//   then the objects in between, and then the lower walls.
		// FIXME: This is not a perfect implementation yet. To make sure that
		//   everything works for floating objects and objects with arbitrary
		//   bounding polygons, it may be necessary to determine the sprite
		//   location from the *lowest vertex of the bounding polygon* rather
		//   than (currently) with an (x, y) value of the entity.
		
		// Objects in 'drawList' are held by index of their y-values within the row.
		ArrayList<ArrayList<Entity>> drawList = new ArrayList<ArrayList<Entity>>();
		for (int i = 0; i < 32; ++i) {
		    drawList.add(new ArrayList<Entity>());
		}
		
		// For each visible row..
        for (int y = gridy1; y >= gridy2; --y) {

            // Ignore row if not in game grid.
            if (y < 0 || y > pf.gridHeight() - 1) continue;

            // Clear sprite list.
            for (int i = 0; i < 32; ++i) {
                drawList.get(i).clear();
            }
            
            // For each square in the current row...
            for (int x = gridx1; x <= gridx2; ++x) {
                
                // If not in game grid, ignore.
                if (x < 0 || x > pf.gridWidth() - 1) continue;
                
                // Get entities in this square.
                for (int i = 0; i < pf.gameGrid[x][y].entities.size(); ++i) {
                    
                    // Get the next entity.
                    Entity E = pf.gameGrid[x][y].entities.get(i);

                    // If y-coordinate of lowest vertex of entity's bounding polygon
                    //   lies within this row, add it to the draw list.
                    // OPTIMIZE: Make this precomputed.
                    int lowesty = 2147483647;
                    Polygon poly = E.getBoundingPoly(); // FIXME: Null pointer exception here: why?
                    for (int j = 0; j < poly.getNumVertices(); ++j) {
                        if (poly.getVertex(j).y < lowesty) {
                            lowesty = poly.getVertex(j).y;
                        }
                    }
                    if (lowesty >> 5 == y) {
                        int j = lowesty - (y << 5);
                        if (!drawList.get(j).contains(E)) {
                            drawList.get(j).add(E);
                        }
                    }
                }
                
                // Draw upper wall on this square, if one exists.
                Texture upper = pf.gameGrid[x][y].getTexture(DragoStatics.UPPER);
                if (Texture.isWall(upper)) {
                    
                    // Draw the top part of the wall.
                    int xtop = (x << 5) - xcam;
                    int ytop = 480 - ((y << 5) - ycam) - 96;
                    Texture left = pf.gameGrid[x][y].getTexture(DragoStatics.LEFT);
                    int side = (upper == left) ? TX_UPPER_LEFT : TX_UPPER_RIGHT;
                    BufferedImage top = this.getSpriteArray(upper).get(side);
                    g2d.drawImage(top, xtop, ytop, null);
                    
                    // If there is no wall in front, draw the side of the wall.
                    Texture lower = pf.gameGrid[x][y].getTexture(DragoStatics.LOWER);
                    if (!Texture.isWall(lower)) {
                        side = (side == TX_UPPER_LEFT) ? TX_RIGHT : TX_LEFT;
                        BufferedImage wall = this.getSpriteArray(upper).get(side);
                        g2d.drawImage(wall, xtop, ytop, null);
                        g2d.drawImage(wall, xtop, ytop + 32, null);
                    } 

                } // end if (upper texture is a wall)
            } // end for (by grid square in current row)
            
            // Draw all entities in this row from back to front via
            //   the Painter's algorithm.
            for (int i = 31; i >= 0; --i) {
                for (int j = 0; j < drawList.get(i).size(); ++j) {
                    drawList.get(i).get(j).drawSprite(g2d, xcam, ycam);
                }
            }
            
            // Draw all the front walls.
            for (int x = gridx1; x <= gridx2; ++x) {
                
                // If not in game grid, ignore.
                if (x < 0 || x > pf.gridWidth() - 1) continue;

                // If the lower texture of this square is a wall...
                Texture lower = pf.gameGrid[x][y].getTexture(DragoStatics.LOWER);
                if (Texture.isWall(lower)) {
                    
                    // Draw the top part of the wall.
                    Texture upper = pf.gameGrid[x][y].getTexture(DragoStatics.UPPER);
                    Texture left = pf.gameGrid[x][y].getTexture(DragoStatics.LEFT);
                    int side = (upper == left) ? TX_LOWER_RIGHT : TX_LOWER_LEFT;
                    int xtop = (x << 5) - xcam;
                    int ytop = 480 - ((y << 5) - ycam) - 96;
                    BufferedImage top = this.getSpriteArray(lower).get(side);
                    g2d.drawImage(top, xtop, ytop, null);
                    
                    // Draw the front of the wall (horizontal) if no wall below.
                    Texture below = null;
                    if (y > 0) below = pf.gameGrid[x][y - 1].getTexture(DragoStatics.UPPER);
                    if (!((below != null) && (Texture.isWall(below)))) {
                        BufferedImage front = this.getSpriteArray(lower).get(TX_FRONT);
                        g2d.drawImage(front, xtop, ytop + 32, null);
                        g2d.drawImage(front, xtop, ytop + 64, null);
                    }
                
                } // end if (lower texture is a wall)
            } // end for (by grid square in current row again)
        } // end for (by row of grid squares)
                
                        
        /* *** All of this is old and pending deletion! ***
		
		// Draw walls (to illustrate how the game will look). Does not incorporate
		//   Ryan's algorithm yet.
		boolean WALLS = true;
		for (int y = gridy1; y >= gridy2; --y) {
			for (int x = gridx1; x <= gridx2; ++x) {
				
				// If not in game grid, ignore
				if (    x < 0 || x > pf.gridWidth() - 1
					 || y < 0 || y > pf.gridHeight() - 1 )
				{
					continue;
				}
				
				// If a tree/stone is on this square, draw it instead of any walls.
				boolean entityDrawn = false;
				for (int i = 0; i < pf.gameGrid[x][y].entities.size(); ++i) {
					Entity t = pf.gameGrid[x][y].entities.get(i);
					if (    !(t instanceof Tree)
					     && !(t instanceof Stone) )
					{
					    break;
					}
					if (    ((t.x() - 16)>>5 == x)
						 && ((t.y() - 32)>>5 == y) )
					{
						t.drawSprite(g2d, xcam, ycam);
						entityDrawn = true;
					}
				}
				if (entityDrawn) continue;
				
				// Get information about the textures in this grid square.
				Texture upper = pf.gameGrid[x][y].getTexture(DragoStatics.UPPER);
				Texture lower = pf.gameGrid[x][y].getTexture(DragoStatics.LOWER);
				Texture left  = pf.gameGrid[x][y].getTexture(DragoStatics.LEFT);
				Texture right = pf.gameGrid[x][y].getTexture(DragoStatics.RIGHT);
				Texture below = null;
				if (y != 0)	below = pf.gameGrid[x][y - 1].getTexture(DragoStatics.UPPER);
				
				// Get sprites to draw.
				ArrayList<BufferedImage> uppertext = this.getSpriteArray(upper);
				ArrayList<BufferedImage> lowertext = this.getSpriteArray(lower);
				
				// Get location of sprite to draw on panel.
				int xwallfront = (x << 5) - xcam;
				int ywallfront = 480 - ((y << 5) - ycam) - 32;
				int xwallside = xwallfront;
				int ywallside = ywallfront - 32;
				int xtext = xwallfront;
				int ytext = ywallfront - 64;
				
				if (Texture.isWall(lower)) {
					if (below == null || !Texture.isWall(below)) {
						g2d.drawImage(lowertext.get(TX_FRONT), xwallfront, ywallfront, null);
						g2d.drawImage(lowertext.get(TX_FRONT), xwallfront, ywallfront - 32, null);
					}
					if (lower == left && lower == right) {
						g2d.drawImage(lowertext.get(TX_FULL), xtext, ytext, null);
					} else if (lower == left) {
						g2d.drawImage(lowertext.get(TX_LOWER_LEFT), xtext, ytext, null);
					} else {
						g2d.drawImage(lowertext.get(TX_LOWER_RIGHT), xtext, ytext, null);
					}
				}
				if (Texture.isWall(upper)) {
					if (!Texture.isWall(lower)) {
						if (upper == left) {
							g2d.drawImage(uppertext.get(TX_RIGHT), xwallside, ywallside, null);
							g2d.drawImage(uppertext.get(TX_RIGHT), xwallside, ywallside - 32, null);
						} else {
							g2d.drawImage(uppertext.get(TX_LEFT), xwallside, ywallside, null);
							g2d.drawImage(uppertext.get(TX_LEFT), xwallside, ywallside - 32, null);
						}
					}
					if (upper == left && upper == right) {
						g2d.drawImage(uppertext.get(TX_FULL), xtext, ytext, null);
					} else if (upper == left) {
						g2d.drawImage(uppertext.get(TX_UPPER_LEFT), xtext, ytext, null);
					} else {
						g2d.drawImage(uppertext.get(TX_UPPER_RIGHT), xtext, ytext, null);
					}
				}
				
			} // end for (by column/grid square)
		} // end for (by row)

		// Attempt to draw the player.
		pf.player.drawSprite(g2d, xcam, ycam);
		
		// Draw any entities in the active region.
		for (int i = 0; i < pf.numEntities(); ++i) {
		    Entity t = pf.getEntity(i);
            if (VERBOSE) System.out.println("Now drawing entity of index " + i
                    + ", a " + t.getClass());
		    if (t instanceof Tree) continue;
		    if (t instanceof Stone) continue;
		    if (!(t instanceof Player)) t.drawSprite(g2d, xcam, ycam);
		}		        
		
		*/
        
        
		// Now draw the portals (testing purposes only).
		if (PORTALS) {
			
			// Draw all portals contained in the BSP.
			drawPortals(pf.bsp, g2d);
			
			// Now draw all portals referred to by the
			//   PortalSquares.
			for (int y = pf.gridHeight() - 1; y >= 0; --y) {
				for (int x = 0; x < pf.gridWidth(); ++x) {
					if (pf.gameGrid[x][y] instanceof PortalSquare) {
						
						PortalSquare ps = (PortalSquare)pf.gameGrid[x][y];
						for (int portal = 0; portal < 4; ++portal) {
							if (ps.getPortal(portal) == null) continue;
							int x1 = ps.getPortal(portal).getSegment().x1() - xcam;
							int y1 = 480 - (ps.getPortal(portal).getSegment().y1() - ycam);
							int x2 = ps.getPortal(portal).getSegment().x2() - xcam;
							int y2 = 480 - (ps.getPortal(portal).getSegment().y2() - ycam);
							if (!ps.getPortal(portal).isOpen()) g2d.setColor(Color.RED);
							else g2d.setColor(Color.BLUE);
							drawVector(x1, y1, x2, y2, 3, g2d);
						}
					}
				}
			}
		}
		
		if (AILINES) drawAILines(pf.bsp, g2d);
		if (FILLGRID) checkFillGrid(g2d);
		if (FULLPARTITIONS) drawFullPartitions(pf.bsp, g2d);
		
		g2d.setColor(Color.WHITE);
		g2d.setFont(new Font("Arial", Font.BOLD, 14));
		DragoStatics.drawShadedString("FPS: " + fps, 20, 20, g2d);
		DragoStatics.drawShadedString("F1: View AI Lines", 480, 420, g2d);
        DragoStatics.drawShadedString("F2: View partitions", 480, 440, g2d);
        DragoStatics.drawShadedString("F3: View portals", 480, 460, g2d);
		
		// I heard this assists with garbage collection. Should I use these?
		g2d.dispose();
		g.dispose();
		
	} // end method paintComponent

	// ----- All methods below this point are for testing -----
	// ----- and will not be used in the game.            -----
	
	private void drawPortals(BSPNode bsp, Graphics2D g2d) {
		for (int i = 0; i < bsp.getNumPortals(); ++i) {
			int x1 = bsp.getPortal(i).getSegment().x1();
			int y1 = bsp.getPortal(i).getSegment().y1();
			int x2 = bsp.getPortal(i).getSegment().x2();
			int y2 = bsp.getPortal(i).getSegment().y2();
			x1 = x1 - xcam;
			y1 = 480 - (y1 - ycam);
			x2 = x2 - xcam;
			y2 = 480 - (y2 - ycam);
			
			if (bsp.getPortal(i).isOpen())
				g2d.setColor(Color.BLUE);
			else
				g2d.setColor(Color.RED);
			
			// Draw only if front leaf isn't a tree/stone poly.
			if (    (BSPNode.getLeaf(pf.bsp, bsp.getPortal(i).getSegment(), LineSeg.FRONT).getProperty()
                    != LeafProperty.TREE_STONE_POLY) )
			{
				drawVector(x1, y1, x2, y2, 3, g2d);
			}
		}
		if (!(bsp.front() instanceof BSPLeaf)) drawPortals(bsp.front(), g2d);
		if (!(bsp.back() instanceof BSPLeaf)) drawPortals(bsp.back(), g2d);
	}

	private void drawAILines(BSPNode bsp, Graphics2D g2d) {
		for (int i = 0; i < bsp.getNumAILines(); ++i) {
			int x1 = bsp.getAILine(i).x1();
			int y1 = bsp.getAILine(i).y1();
			int x2 = bsp.getAILine(i).x2();
			int y2 = bsp.getAILine(i).y2();
			x1 = x1 - xcam;
			y1 = 480 - (y1 - ycam);
			x2 = x2 - xcam;
			y2 = 480 - (y2 - ycam);
			g2d.setColor(Color.PINK);
			g2d.drawLine(x1, y1, x2, y2);
		}
		if (!(bsp.front() instanceof BSPLeaf)) drawAILines(bsp.front(), g2d);
		if (!(bsp.back() instanceof BSPLeaf)) drawAILines(bsp.back(), g2d);
	}

	private void drawFullPartitions(BSPNode bsp, Graphics2D g2d) {
		int x1 = bsp.getFullPartition().x1();
		int y1 = bsp.getFullPartition().y1();
		int x2 = bsp.getFullPartition().x2();
		int y2 = bsp.getFullPartition().y2();
		x1 = x1 - xcam;
		y1 = 480 - (y1 - ycam);
		x2 = x2 - xcam;
		y2 = 480 - (y2 - ycam);
		g2d.setColor(Color.ORANGE);
		drawVector(x1, y1, x2, y2, 5, g2d);
		if (!(bsp.front() instanceof BSPLeaf)) drawFullPartitions(bsp.front(), g2d);
		if (!(bsp.back() instanceof BSPLeaf)) drawFullPartitions(bsp.back(), g2d);
	}
	
	private void drawVector(int x1, int y1, int x2, int y2, int shorten, Graphics2D g2d) {

		// Shorten the line to distinguish between portals.
		if (x1 < x2) { x1 += shorten; x2 -= shorten; }
		if (x2 < x1) { x2 += shorten; x1 -= shorten; }
		if (y1 < y2) { y1 += shorten; y2 -= shorten; }
		if (y2 < y1) { y2 += shorten; y1 -= shorten; }
		
		g2d.drawLine(x1, y1, x2, y2);
		
		// Pixellate the beginning of the portal to indicate
		//   direction.
		g2d.drawLine(x1, y1, x1+1, y1);
		g2d.drawLine(x1, y1, x1, y1+1);
		g2d.drawLine(x1, y1, x1+1, y1+1);
		g2d.drawLine(x1, y1, x1-1, y1);
		g2d.drawLine(x1, y1, x1, y1-1);
		g2d.drawLine(x1, y1, x1-1, y1-1);
		g2d.drawLine(x1, y1, x1-1, y1+1);
		g2d.drawLine(x1, y1, x1+1, y1-1);
	}
	
	private void checkFillGrid(Graphics2D g2d) {
		g2d.setColor(Color.GREEN);
		for (int y = pf.gridHeight() - 1; y >= 0; --y) {
			for (int x = 0; x < pf.gridWidth(); ++x) {
				int xsc = (x<<5) - xcam;
				int ysc = 480 - ((y<<5) - ycam);
				if ((pf.fillGrid[x][y] & MapGridSquare.BOUND_UP) != 0)
					g2d.drawLine(xsc, ysc-32, xsc+32, ysc-32);
				if ((pf.fillGrid[x][y] & MapGridSquare.BOUND_RIGHT) != 0)
					g2d.drawLine(xsc+32, ysc-32, xsc+32, ysc);
				if ((pf.fillGrid[x][y] & MapGridSquare.BOUND_DOWN) != 0)
					g2d.drawLine(xsc, ysc, xsc+32, ysc);
				if ((pf.fillGrid[x][y] & MapGridSquare.BOUND_LEFT) != 0)
					g2d.drawLine(xsc, ysc-32, xsc, ysc);
				if ((pf.fillGrid[x][y] & MapGridSquare.BOUND_UPRIGHT) != 0)
					g2d.drawLine(xsc+16, ysc-16, xsc+32, ysc-32);
				if ((pf.fillGrid[x][y] & MapGridSquare.BOUND_DOWNRIGHT) != 0)
					g2d.drawLine(xsc+16, ysc-16, xsc+32, ysc);
				if ((pf.fillGrid[x][y] & MapGridSquare.BOUND_DOWNLEFT) != 0)
					g2d.drawLine(xsc+16, ysc-16, xsc, ysc);
				if ((pf.fillGrid[x][y] & MapGridSquare.BOUND_UPLEFT) != 0)
					g2d.drawLine(xsc+16, ysc-16, xsc, ysc-32);
			}
		}
	}

} // end class GamePanel

/*
 * Playfield.java (v1.0)
 * 3/17/2013
 * 
 * Submitted by Scott Hoge (seh02c) for:
 * Homework 6: Implementation
 * CEN4021
 * Spring 2013
 * 
 */
package playfield;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Vector;
import java.util.ArrayList;

import drago.DragoStatics;

import playfield.MapGridSquare;

import entity.*;

/**
 * The class representing a 'level' in the game, or if large enough,
 * the entire game itself. Contains information about the placement
 * of entities such as walls, ground types, items, terrain obstacles,
 * and enemy humans, as well as more detailed infrastructure such as
 * the binary space partition and the A* search graph.
 * 
 * @author Quothmar (Scott Hoge)
 *
 */
public class Playfield {

	// Entities list for second iteration forward. Add to UML.
	private ArrayList<Entity> entities = new ArrayList<Entity>();
	public Entity getEntity(int i) { return entities.get(i); }
	public void addEntity(Entity t) {
	    if (!entities.contains(t))
	        entities.add(t);
	}
	public void removeEntity(Entity t) { entities.remove(t); }
	public int numEntities() { return entities.size(); }
	    
	// Active entities (i.e., the subset of entities above that
	//   will be included in the action loop -- all the rest will
	//   just stay frozen until the player gets close).
	private ArrayList<Entity> activeEntities = new ArrayList<Entity>();
	public Entity getActiveEntity(int i) { return activeEntities.get(i); }
	public void addActiveEntity(Entity t) {
	    if (!activeEntities.contains(t))
	        activeEntities.add(t);
	}
	public void removeActiveEntity(Entity t) { activeEntities.remove(t); }
	public int numActiveEntities() { return activeEntities.size(); }
	
	// The player, who occupies a special status relative to the
	//   acting entities surrounding him/her.
	public Player player;
	
	// The game grid and the fill grid.
	// 
	// *Ryan*:
	//
	//   The following array, 'gameGrid[][]', contains all the information
	// about the locations of entities that you will need to create your
	// SpriteSequence class. The method 'getCurrentSprite()' of class
	// 'Entity' can be used to retrieve the current sprite of any given
	// entity based on that entity's state. Note that each MapGridSquare
	// has four triangles, described in the documentation 'Drago's Inferno
	// - Design of Playfield Object', each containing a texture that is
	// possibly a wall. Also, each MapGridSquare has a list of Entity
	// references to any entities that are overlapping that square.
	//
	//   The SpriteSequence class can be tested by setting the 'WALLS'
	// boolean to 'false' in the GamePanel class, and writing in your
	// own graphical rendering code.
	//
	//   Feel free to call me at (850) 575-7484 if you have any difficulties.
	//
	// -Scott
	//
	public MapGridSquare[][] gameGrid;
	public int[][] fillGrid;  // Temporarily made public

	// The array for grid selection. This is for the 'makeGridSelection'
	//   method I introduced last semester.
	public final int MAX_POLY_LINES = 20;
	public int gridSelectionLeft[][];
	public int gridSelectionRight[][];
	public int gridSelectionLowestRow[] = new int[MAX_POLY_LINES];
	public int gridSelectionHighestRow[] = new int[MAX_POLY_LINES];
	
	// Dimensions of both the game grid and the playfield.
	private int levelWidth;
	private int levelHeight;
	private int gridWidth;
	private int gridHeight;
	public int gridWidth () { return gridWidth;	}
	public int gridHeight() { return gridHeight; }
	public int levelWidth() { return levelWidth; }
	public int levelHeight() { return levelHeight; }
	
	// Methods to ensure that a given square coordinate
	//   lies within the bounds of the game grid.
	public int clipWidth(int x) {
	    if (x < 0) return 0;
	    if (x > gridWidth - 1) return gridWidth - 1;
	    return x;
	}
	public int clipHeight(int y) {
	    if (y < 0) return 0;
	    if (y > gridHeight - 1) return gridHeight - 1;
	    return y;
	}
	
	// Data member to hold the root of the BSP tree.
	public BSPNode bsp;
	
	// Default constructor.
	public Playfield() {}

	// This is the method that contains all the main stuff (which I wrote about in the
	//   Playfield documentation). Just following the pseudocode.
	public void loadLevel(String filename) {
		
		// This can be used to multiply the size of the level by
		//   a certain fraction.
		final int EXPAND = 2;
		
		Scanner scanner = null;

		String nextString;
		Vector<TexturedPolygon> groundPolys = new Vector<TexturedPolygon>();
		Vector<TexturedPolygon> wallPolys = new Vector<TexturedPolygon>();
		Vector<TexturedPolygon> treeStonePolys = new Vector<TexturedPolygon>();
		Fieldpoint fp = new Fieldpoint();

		try {
			scanner = new Scanner(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// First data elements of file: width and height of level.
		levelWidth = EXPAND*scanner.nextInt();
		levelHeight = EXPAND*scanner.nextInt();

		// Issue error if level width/height are not multiples of 32.
		if (levelWidth % 32 != 0) error("loadLevel", "invalid width (" + levelWidth + ")");
		if (levelHeight % 32 != 0) error("loadLevel", "invalid height (" + levelHeight + ")");
		
		// Get grid width and height from level width and height.
		//   Note: bit shift will be used frequently in place of division
		//   by powers of two. This actually works out conveniently
		//   for 32x32 squares.
		gridWidth = levelWidth >> 5;
		gridHeight = levelHeight >> 5;
		
		// Create the array of game grid squares.
		gameGrid = new MapGridSquare[gridWidth][gridHeight];
		
		// On Java: Do we need to use 'new' a second time?
		for (int i = 0; i < gridWidth; ++i)
			for (int j = 0; j < gridHeight; ++j)
				gameGrid[i][j] = new MapGridSquare();
		
		// Create the fill grid for use in filling textures.
		//   I had to switch from bytes to ints because I ran into
		//   a conversion problem... not sure how one would deal
		//   with this in Java.
		fillGrid = new int[gridWidth][gridHeight];

		// Initialize grid selection for use with method
		//   'makeGridSelection'. Use grid height to define
		//   the size of the array.
		gridSelectionLeft = new int[MAX_POLY_LINES][gridHeight];
		gridSelectionRight = new int[MAX_POLY_LINES][gridHeight];
		
		if (!scanner.next().contains("beginpolys"))
			error ("loadLevel", "Invalid map file: 'beginpolys' not found");

		nextString = scanner.next();
		
		// Previous vertex used to test mapfile for validity.
		int xPrev, yPrev;
		
		// Sort all polygons in the mapfile into three categories: wall polygons,
		//   tree/stone polygons, and ground-based polygons.
		while (!nextString.contains("endpolys")) {

			// Start over
			xPrev = -1;
			yPrev = -1;

			if (Texture.getCategory(nextString) == Texture.WALL_TYPE) {
				wallPolys.add(new TexturedPolygon());
				wallPolys.lastElement().setType(Texture.valueOf(nextString));

				while (scanner.hasNextInt()) {
					fp.x = EXPAND*scanner.nextInt();
					fp.y = EXPAND*scanner.nextInt();

					// Make sure point is snapped to grid.
					if (fp.x % 32 != 0 || fp.y % 32 != 0)
						error("loadLevel", "fieldpoint not snapped to grid at (" + fp.x + ", " + fp.y + ")\n  (Grid is 32x32)");

					// Test fieldpoint read from file for validity by checking to
					//   make sure it is in a 45 degree increment.
					if (xPrev != -1)
						if ((Math.abs(fp.x-xPrev) != Math.abs(fp.y-yPrev) && (fp.x-xPrev != 0) && (fp.y-yPrev != 0)))
							error("loadLevel", "wall angle not a multiple of 45 degrees:\n"
									+ "(" + xPrev + ", " + yPrev + ") - (" + fp.x + ", " + fp.y + ")");
					
					wallPolys.lastElement().addVertex(fp);
					xPrev = fp.x;
					yPrev = fp.y;
				}
				
				// Do same test for first point.
				fp.x = wallPolys.lastElement().getVertex(0).x;
				fp.y = wallPolys.lastElement().getVertex(0).y;
				if (Math.abs(fp.x-xPrev) != Math.abs(fp.y-yPrev) && (fp.x-xPrev != 0) && (fp.y-yPrev != 0))
					error("loadLevel", "wall angle not a multiple of 45 degrees:\n"
							+ "(" + xPrev + ", " + yPrev + ") - (" + fp.x + ", " + fp.y + ")");
				
			} else if (Texture.getCategory(nextString) == Texture.TREE_STONE_TYPE) {
				treeStonePolys.add(new TexturedPolygon());
				treeStonePolys.lastElement().setType(Texture.valueOf(nextString));

				while (scanner.hasNextInt()) {
					fp.x = EXPAND*scanner.nextInt();
					fp.y = EXPAND*scanner.nextInt();
					
					// Make sure point is snapped to grid.
					if (fp.x % 32 != 0 || fp.y % 32 != 0)
						error("loadLevel", "fieldpoint not snapped to grid at (" + fp.x + ", " + fp.y + ")\n  (Grid is 32x32)");
					
					// Test to be sure polygon lines are all at 90 degree angles
					//   (as they hold objects).
					if (xPrev != -1 && fp.x - xPrev != 0 && fp.y - yPrev != 0)
						error("loadLevel", "tree/stone polygon line positioned diagonally:\n"
								+ "(" + xPrev + ", " + yPrev + ") - (" + fp.x + ", " + fp.y + ")");
					
					treeStonePolys.lastElement().addVertex(fp);
					xPrev = fp.x;
					yPrev = fp.y;
				}
			} else {
				
				groundPolys.add(new TexturedPolygon());
				groundPolys.lastElement().setType(Texture.valueOf(nextString));

				while (scanner.hasNextInt()) {
					fp.x = EXPAND*scanner.nextInt();
					fp.y = EXPAND*scanner.nextInt();

					// Make sure point is snapped to grid.
					if (fp.x % 32 != 0 || fp.y % 32 != 0)
						error("loadLevel", "fieldpoint not snapped to grid at (" + fp.x + ", " + fp.y + ")");
					
					// Same test as above.
					if (xPrev != -1)
						if (Math.abs(fp.x-xPrev) != Math.abs(fp.y-yPrev) && (fp.x-xPrev != 0) && (fp.y-yPrev != 0))
							error("loadLevel", "ground polygon line angle not a multiple of 45 degrees:\n"
									+ "(" + xPrev + ", " + yPrev + ") - (" + fp.x + ", " + fp.y + ")");

					groundPolys.lastElement().addVertex(fp);
					xPrev = fp.x;
					yPrev = fp.y;
				}
				
				// Do same test for first point.
				fp.x = groundPolys.lastElement().getVertex(0).x;
				fp.y = groundPolys.lastElement().getVertex(0).y;
				if (Math.abs(fp.x-xPrev) != Math.abs(fp.y-yPrev) && (fp.x-xPrev != 0) && (fp.y-yPrev != 0))
					error("loadLevel", "ground polygon line angle not a multiple of 45 degrees:\n"
							+ "(" + xPrev + ", " + yPrev + ") - (" + fp.x + ", " + fp.y + ")");

			}
			nextString = scanner.next();
		}

		if (!scanner.next().contains("beginobjects"))
			error ("loadLevel", "Invalid map file: 'beginobjects' not found");

		// Place all entities in mapfile on the playfield.
		Entity nextEntity = null;
		nextString = scanner.next();
		while (!nextString.contains("endobjects")) {

			// Get the entity's position.
			int x = EXPAND*scanner.nextInt();
			int y = EXPAND*scanner.nextInt();
			
			// Get the initial direction.
			String beginFacing = scanner.next();
			
			// Call constructor.
			switch (nextString) {
			case "PLAYER": nextEntity = new Player(x + 32, y + 32, beginFacing, this); break;
			case "ARCHER": nextEntity = new Archer(x, y, beginFacing, this); break;
			case "RED_EGG": nextEntity = new RedEgg(x, y, beginFacing, this); break;
            case "GREEN_EGG": nextEntity = new GreenEgg(x, y, beginFacing, this); break;
            case "BLUE_EGG": nextEntity = new BlueEgg(x, y, beginFacing, this); break;
            case "YELLOW_EGG": nextEntity = new YellowEgg(x, y, beginFacing, this); break;
			default: error("loadLevel", "object name not recognized (" + nextString + ")");
			}
			
			// Place newly-constructed entity in playfield.
			if (nextEntity instanceof Player) this.player = (Player)nextEntity;
			else this.addEntity(nextEntity);
			
			// Place the entity on the grid. (NOTE: requires grid
			//   selection algorithm.)

			nextString = scanner.next();
		}
			
		// STEP 1: Create the internal portal structure of the tree and stone polygons.
		//   All portal are arranged in unit squares so that each tree or stone is enclosed
		//   by four portals. Two neighboring trees or stones share a portal. These
		//   portals are needed in case the player burns a path through a forest, for
		//   example, as enemies would need to navigate their way through the burnt path.
		for (TexturedPolygon tp : treeStonePolys) {
			
			// We'll do this for each tree/stone polygon 'tp'. The first
			//   thing to do is initialize the fill grid to 0 and light this
			//   polygon up in it.
			// Note: A tree/stone polygon *cannot* have a complementing
			//   polygon at this stage of development; I plan to change this
			//   at some point.

			// Initialize fill grid to 0.
			for (int x = 0; x < gridWidth; ++x)
				for (int y = 0; y < gridHeight; ++y)
					fillGrid[x][y] = 0;
			
			// Light the current tree/stone polygon up in the fill grid.
			int x1 = 0, y1 = 0;
			int x2 = 0, y2 = 0;
			for (int bound = 0; bound < tp.getNumVertices(); ++bound) {
				
				// Get the coordinates of the starting and ending vertex.
				x1 = tp.getVertex(bound).x;
				y1 = tp.getVertex(bound).y;
				x2 = tp.getVertex((bound + 1) % tp.getNumVertices()).x;
				y2 = tp.getVertex((bound + 1) % tp.getNumVertices()).y;
								
				// Light up all segments covered by this line, which is
				//   horizontal/vertical.
				if (x1 == x2) {
					
					// Make sure line is going up (interchange y values).
					if (y1 > y2) { x2 = y2; y2 = y1; y1 = x2; x2 = x1; }
				
					// Set flags on both sides of a grid segment.
					while (y1 != y2) {
						if (x1 < levelWidth)
						    fillGrid[x1>>5][y1>>5] |= MapGridSquare.BOUND_LEFT;
						if (x1 > 0)
						    fillGrid[(x1>>5) - 1][y1>>5] |= MapGridSquare.BOUND_RIGHT;
						y1 += 32;
					}
				} // end if (line vertical)
				
				else if (y1 == y2) {
					
					// If horizontal, make sure we're going right (interchange x values).
					if (x1 > x2) { y2 = x2; x2 = x1; x1 = y2; y2 = y1; }
					
					while (x1 != x2) {
						if (y1 < levelHeight)
						    fillGrid[x1>>5][y1>>5] |= MapGridSquare.BOUND_DOWN;
						if (y1 > 0)
						    fillGrid[x1>>5][(y1>>5) - 1] |= MapGridSquare.BOUND_UP;
						x1 += 32;
					}
				} // end if (line horizontal)
			} // end while (by tree/stone polygon vertex)
		
			// Now the fill grid should be lit up. Take any square on one side
			//   and perform a recursive call to cast the grid squares to
			//   fill grid squares. This is similar to a 'Fill' command in Paint.
			x1 = tp.getVertex(0).x;
			y1 = tp.getVertex(0).y;
			x2 = tp.getVertex(1).x;
			y2 = tp.getVertex(1).y;
			int markx = x1 >> 5;
			int marky = y1 >> 5;
			if (x1 == x2 && y1 < y2) --markx;
			if (x1 == x2 && y1 > y2) --marky;
			if (y1 == y2 && x1 > x2) { --markx; --marky; }
			castSquarePortals(markx, marky);
			
			// Add portals to the internal portal structure of the polygon.
			boolean C_UP = false;
			boolean C_DOWN = false;
			boolean C_LEFT = false;
			boolean C_RIGHT = false;
			for (int y = 0; y < gridHeight; ++y) {
				for (int x = 0; x < gridWidth; ++x) {
					if (    gameGrid[x][y] instanceof PortalSquare
						 && gameGrid[x][y].getNumEntities() == 0 )
					{
						// Get just-created status of neighboring squares.
						C_UP = C_DOWN = C_LEFT = C_RIGHT = false;
						if (y < gridHeight - 1) {
							C_UP =    gameGrid[x][y + 1] instanceof PortalSquare
					               && gameGrid[x][y + 1].getNumEntities() == 0;
						}
						if (y > 0) {
							C_DOWN =    gameGrid[x][y - 1] instanceof PortalSquare
							         && gameGrid[x][y - 1].getNumEntities() == 0;
						}
						if (x > 0) {
							C_LEFT =    gameGrid[x - 1][y] instanceof PortalSquare
							         && gameGrid[x - 1][y].getNumEntities() == 0;
						}
						if (x < gridWidth - 1) {
							C_RIGHT =    gameGrid[x + 1][y] instanceof PortalSquare
							          && gameGrid[x + 1][y].getNumEntities() == 0;
						}
						
						// Get the center square and some initial values.
						PortalSquare center = (PortalSquare)(gameGrid[x][y]);
						PortalSquare neighbor = null;				// Neighbor's square
						Portal p = null;							// Created portal
						int nbrx = 0, nbry = 0;						// Neighbor coordinates (grid-based)
						int px1 = 0, py1 = 0, px2 = 0, py2 = 0;		// Portal starting and ending coordinates (grid-based)
						boolean flag = false;						// States whether a neighboring portal square is found
						
						// Check each side of the given portal square for other portal squares,
						//   create portals if they don't yet exist, and connect them with
						//   existing portals.
						for (int sidecheck = 0; sidecheck < 4; ++sidecheck) { // sidecheck goes: UPPER->RIGHT->LOWER->LEFT
							
							switch (sidecheck) {
							case DragoStatics.UPPER:
							    flag = C_UP;
							    nbrx = x;     nbry = y + 1;
							    px1 = x;      py1  = y + 1;
							    px2 = x + 1;  py2  = y + 1;
							    break;
							case DragoStatics.RIGHT:
							    flag = C_RIGHT;
							    nbrx = x + 1; nbry = y;
							    px1 = x + 1;  py1 = y;
							    px2 = x + 1;  py2 = y + 1;
							    break;
							case DragoStatics.LOWER:
							    flag = C_DOWN;
							    nbrx = x;     nbry = y - 1;
							    px1 = x;      py1 = y;
							    px2 = x + 1;  py2 = y;
							    break;
							case DragoStatics.LEFT:
							    flag = C_LEFT;
							    nbrx = x - 1; nbry = y;	
							    px1 = x;      py1 = y;
							    px2 = x;      py2 = y + 1;
							    break;
							}
							
							if (flag) {
								
								// Do the cast to get the neighboring portal square.
								neighbor = (PortalSquare)(gameGrid[nbrx][nbry]);
								
								// If neighbor does not have portal yet...
								if (neighbor.getPortal(DragoStatics.getOppSide(sidecheck)) == null) {
									
									// Add a new (closed) portal where none existed.
									p = new Portal(px1<<5, py1<<5, px2<<5, py2<<5);
									neighbor.addPortal(p, DragoStatics.getOppSide(sidecheck));
									p.close();
									
									// Attach to neighbor's existing portals.
									for (int nside = 0; nside < 4; ++nside)		// nside goes: UPPER->RIGHT->LOWER->LEFT
										if (    (nside != DragoStatics.getOppSide(sidecheck))
										     && (neighbor.getPortal(nside) != null) )
										{
											p.addNeighbor(neighbor.getPortal(nside));
											neighbor.getPortal(nside).addNeighbor(p);
										}
								
								} // end if (neighbor had no portal)
								
								// Attach the now-existing portal of the upper neighbor to the
								//   current square.
								p = neighbor.getPortal(DragoStatics.getOppSide(sidecheck));
								center.addPortal(p, sidecheck);
								
								// Connect to any existing portals of the current square.
								for (int cside = 0; cside < 4; ++cside)
									if (cside != sidecheck && center.getPortal(cside) != null) {
										p.addNeighbor(center.getPortal(cside));
										center.getPortal(cside).addNeighbor(p);
									}

							} // end if (neighboring portal square was found)
						} // end for (check all sides)
					} // end if (square was found to be empty portal square)
				} // end for (rows of map grid)
			} // end for (columns of map grid)

			// Now the internal portal structure of this polygon has been created.
			//   The next step is to fill all newly-created portals with tree/stone
			//   objects as appropriate.
			Entity t = null;
			for (int y = 0; y < gridHeight; ++y) {
				for (int x = 0; x < gridWidth; ++x) {
					if (gameGrid[x][y] instanceof PortalSquare)
					{
						// Skip if entity already present on square.
						boolean empty = true;
						for (int i = 0; i < gameGrid[x][y].getNumEntities(); ++i) {
							if (    (gameGrid[x][y].entities.get(i).x() == (x<<5) + 16)
								 && (gameGrid[x][y].entities.get(i).y() == (y<<5) + 32) )
							{
								empty = false;
							}
						}
						if (!empty) continue;
						
						// Otherwise create an entity there.
						if      (tp.getType() == Texture.FLAMMABLE_TREES)
							t = new Tree((x<<5) + 16, (y<<5) + 32, "UP", this);
						else if (tp.getType() == Texture.STONES)
							t = new Stone((x<<5) + 16, (y<<5) + 32, "UP", this);
						else error("loadLevel", "tree/stone error");

						// Attach tree/stone to the collision grid.
						gameGrid[x][y].attach(t);
						
						// Add to entities list.
						this.addEntity(t);

					} // end if (portal is newly created, i.e., num. entities = 0)
				} // end for (grid columns)
			} // end for (grid rows)
		} // end for (by tree/stone polygons)

		// STEP 2: Now all trees and stones have been created and placed on the map grid
		//   with their internal portal structure. The next step is to fill in the
		//   ground-based texture polygons. Do both types (for i == 0 and i == 1)...
		for (int i = 0; i < 2; ++i) {
			Vector<TexturedPolygon> set = (i == 0 ? groundPolys : wallPolys);
		
			// Initialize fill grid for this type (wall textures should overwrite
			//   ground textures).
			for (int y = 0; y < gridHeight; ++y)
				for (int x = 0; x < gridWidth; ++x)
					fillGrid[x][y] = 0;
			
			for (TexturedPolygon poly : set) {
			
				int x1 = 0, y1 = 0;
				int x2 = 0, y2 = 0;
				for (int bound = 0; bound < poly.getNumVertices(); ++bound) {
					
					// Get the coordinates of the starting and ending vertex.
					x1 = poly.getVertex(bound).x;
					y1 = poly.getVertex(bound).y;
					x2 = poly.getVertex((bound + 1) % poly.getNumVertices()).x;
					y2 = poly.getVertex((bound + 1) % poly.getNumVertices()).y;
					
					// Four paths possible -- make sure second coordinate is higher or rightmost.
					int temp = 0;
					if (y1 > y2 || (y1 == y2 && x1 > x2)) {
						temp = x2; x2 = x1; x1 = temp;
						temp = y2; y2 = y1; y1 = temp;
					}
					
					// Take one of four paths.
					if (x1 > x2) { 			// To upper-left
						int x = (x1>>5) - 1;
						int y = (y1>>5);
						while (x >= (x2>>5) && y <= (y2>>5)) {
							fillGrid[x][y] |= MapGridSquare.BOUND_DOWNRIGHT;
							fillGrid[x][y] |= MapGridSquare.BOUND_UPLEFT;
							--x; ++y;
						}
					}
					else if (x1 == x2)		// Upward
						for (int y = (y1>>5); y < (y2>>5); ++y) {
							if (x1 < levelWidth)
							    fillGrid[x1>>5][y] |= MapGridSquare.BOUND_LEFT;
							if (x1 > 0)
							    fillGrid[(x1>>5) - 1][y] |= MapGridSquare.BOUND_RIGHT;
						}
					else if (y1 != y2) {	// To upper-right
						int x = (x1>>5);
						int y = (y1>>5);
						while (x < (x2>>5) && y < (y2>>5)) {
							fillGrid[x][y] |= MapGridSquare.BOUND_DOWNLEFT;
							fillGrid[x][y] |= MapGridSquare.BOUND_UPRIGHT;
							++x; ++y;
						}
					}
					else					// Rightward
						for (int x = (x1>>5); x < (x2>>5); ++x) {
							if (y1 < levelHeight)
							    fillGrid[x][y1>>5] |= MapGridSquare.BOUND_DOWN;
							if (y1 > 0)
							    fillGrid[x][(y1>>5) - 1] |= MapGridSquare.BOUND_UP;
						}
					
				} // end for (by polygon vertex)
			} // end for (by polygon in given set)
		
			// Now all polygons in the set should be lit up in the fill grid.
			//   Go back through and fill each one with its texture.
			for (TexturedPolygon poly : set) {
	
				// Get first line of polygon.
				int x1 = poly.getVertex(0).x;
				int y1 = poly.getVertex(0).y;
				int x2 = poly.getVertex(1).x;
				int y2 = poly.getVertex(1).y;
				
				// Get triangle to paint at. (This just looks at the first triangle
				//    on the left-hand side of any given line.)
				int markx = x1 >> 5;
				int marky = y1 >> 5;
				int markside = 0;
				if      (y1 == y2 && x1 < x2) {                   markside = DragoStatics.LOWER; }
				else if (y1 < y2  && x1 < x2) {                   markside = DragoStatics.LEFT;  }
				else if (x1 == x2 && y1 < y2) { --markx;          markside = DragoStatics.RIGHT; }
				else if (x1 > x2  && y1 < y2) { --markx;          markside = DragoStatics.LOWER; }
				else if (y1 == y2 && x1 > x2) { --markx; --marky; markside = DragoStatics.UPPER; }
				else if (x1 > x2  && y1 > y2) { --markx; --marky; markside = DragoStatics.RIGHT; }
				else if (x1 == x2 && y1 > y2) { --marky;          markside = DragoStatics.LEFT;  }
				else                          { --marky;          markside = DragoStatics.UPPER; }
				
				// Paint there.
				fillWithTexture(markx, marky, markside, poly.getType());
				
			} // end for (by ground-based polygon)
		} // end for (do both sets, ground-based and wall-based)

		// STEP 3: Now all polygon textures have been added to the game grid for
		//   rendering. Our next step is to create the binary space partition,
		//   which will be used to enhance enemy AI. Begin by initializing
		//   a master list of AI lines on the map.
		ArrayList<TexturedLine> masterList = new ArrayList<TexturedLine>();
		
		// Load all AI lines from each of the wall-based polygons (including polygons
		//   of type 'WATER') into the master list for partitioning.
		int x1 = 0, y1 = 0;
		int x2 = 0, y2 = 0;
		for (int i = 0; i < 3; ++i) {
			Vector<TexturedPolygon> set = (i == 0 ? wallPolys : (i == 1 ? treeStonePolys : groundPolys));
			
			for (TexturedPolygon poly : set) {
				
				// Skip all ground-textured polygons but 'WATER' polygons.
				if (set == groundPolys && poly.getType() != Texture.WATER) continue;
			
				for (int j = 0; j < poly.getNumVertices(); ++j) {

					// Get the starting and ending coordinates of this AI line.
					x1 = poly.getVertex(j).x;
					y1 = poly.getVertex(j).y;
					x2 = poly.getVertex((j + 1) % poly.getNumVertices()).x;
					y2 = poly.getVertex((j + 1) % poly.getNumVertices()).y;
					
					// Create a new textured line and add it to the master list.
					masterList.add(new TexturedLine(x1, y1, x2, y2, poly.getType()));
				
				} // end for (by polygon vertex)
			} // end for (by polygon in given set)
		} // end for (by set)
		
		// Create the BSP tree for all the AI lines. The constructor will
		//   perform all operations related to static portal generation and
		//   splitting; the entire process is shown in the BSPNode class.
		// 'null' means we're giving 'bsp' the root. Passing the playfield
		//   ('this') is needed so that the BSP constructor can access data
		//   items such as the width and height of the map.
		bsp = new BSPNode(masterList, null, this);
		
		// STEP 4: Now we link all static portals of the BSP with their neighboring
		//   leaves, and then the static portals of each leaf together with
		//   each other.
		connectStaticPortalsToLeaves(bsp);
		connectStaticPortals(bsp);
		
		// STEP 5: Next, we replace all AI lines of type 'WATER', 'FLAMMABLE_TREES',
		//   and 'STONES' with dynamic portals and use them to create the full A* graph.
		//   Here, 'WATER' lines must be done first to ensure a proper connection
		//   of water portals to portal squares, in the event that they touch.
		connectDynamicPortals(bsp, Texture.WATER);
		connectDynamicPortals(bsp, Texture.FLAMMABLE_TREES);
		connectDynamicPortals(bsp, Texture.STONES);
	
		// Also attach walls to collision detection grid.
		attachAILines(bsp);
		
		// Also assign four BSP leaf references to each MapGridSquare.
		for (int x = 0; x < gridWidth - 1; ++x) {
			for (int y = 0; y < gridHeight - 1; ++y) {
				
				// Assign the leaves.
				gameGrid[x][y].leaves[DragoStatics.UPPER] = BSPNode.getLeaf(
				        bsp, (x<<5) + 16, (y<<5) + 24);
				gameGrid[x][y].leaves[DragoStatics.RIGHT] = BSPNode.getLeaf(
				        bsp, (x<<5) + 24, (y<<5) + 16);
				gameGrid[x][y].leaves[DragoStatics.LOWER] = BSPNode.getLeaf(
				        bsp, (x<<5) + 16, (y<<5) + 8);
				gameGrid[x][y].leaves[DragoStatics.LEFT] = BSPNode.getLeaf(
				        bsp, (x<<5) + 8, (y<<5) + 16);
				
				// If the square is a PortalSquare, assign Tree/Stone property
				//   to leaves.
				if (gameGrid[x][y] instanceof PortalSquare) {
					gameGrid[x][y].leaves[DragoStatics.UPPER].setProperty(
					        LeafProperty.TREE_STONE_POLY);
					gameGrid[x][y].leaves[DragoStatics.RIGHT].setProperty(
					        LeafProperty.TREE_STONE_POLY);
					gameGrid[x][y].leaves[DragoStatics.LOWER].setProperty(
					        LeafProperty.TREE_STONE_POLY);
					gameGrid[x][y].leaves[DragoStatics.LEFT].setProperty(
					        LeafProperty.TREE_STONE_POLY);
				}
				
				// If any of the four textures are water, assign the Water
				//   property to those leave(s).
				for (int side = 0; side < 4; ++side)
					if (gameGrid[x][y].getTexture(side) == Texture.WATER)
						gameGrid[x][y].leaves[side].setProperty(LeafProperty.WATER);

			} // end for (by row)
		} // end for (by column)
		
		// Remove the components of the A* graph that lie within walls.
		for (int y = 0; y < this.gridHeight; ++y) {
		    for (int x = 0; x < this.gridWidth; ++x) {
		        for (int side = 0; side < 4; ++side) {
		            Texture t = this.gameGrid[x][y].getTexture(side);
		            if (Texture.isWall(t)) {
		                BSPLeaf leaf = this.gameGrid[x][y].getLeaf(side);
		                if (leaf != null) {
    		                while (leaf.getNumPortals() > 0) {
    		                    leaf.removePortal(leaf.getPortal(0));
    		                }
		                }
		            }
		        }
		    }
		}
		
		// For testing.
		//System.out.println("\n** PARTITIONS **");
		//displayPartitions(bsp);
		//System.out.println("\n** PORTALS **");
		//System.out.println("Portals grouped by leaf; inner portals of tree/stone polygons excluded.\n");
		//displayPortals(bsp);
		
	} // end loadLevel
	
	// Cast a square and any neighboring squares with a tree or
	//   stone polygon within the limits set in the fill grid to
	//   a portal square.
	// 'x' and 'y' are grid square coordinates, not
	//   fieldpoints.
	private void castSquarePortals(int x, int y) {

		// Cast the given square to a PortalSquare.
		gameGrid[x][y] = new PortalSquare();
		
		// Get passibility flags.
		boolean GO_UP =    ((fillGrid[x][y] & MapGridSquare.BOUND_UP) == 0);
		boolean GO_DOWN =  ((fillGrid[x][y] & MapGridSquare.BOUND_DOWN) == 0);
		boolean GO_LEFT =  ((fillGrid[x][y] & MapGridSquare.BOUND_LEFT) == 0);
		boolean GO_RIGHT = ((fillGrid[x][y] & MapGridSquare.BOUND_RIGHT) == 0);
		GO_UP    = GO_UP    && !(gameGrid[x][y + 1] instanceof PortalSquare);
		GO_DOWN  = GO_DOWN  && !(gameGrid[x][y - 1] instanceof PortalSquare);
		GO_LEFT  = GO_LEFT  && !(gameGrid[x - 1][y] instanceof PortalSquare);
		GO_RIGHT = GO_RIGHT && !(gameGrid[x + 1][y] instanceof PortalSquare);
		
		// Check neighboring squares and recursively cast them to
		//   portal squares, filling up the polygon.
		if (y < gridHeight - 1 && GO_UP   ) castSquarePortals(x, y + 1);
		if (y > 0              && GO_DOWN ) castSquarePortals(x, y - 1);
		if (x > 0              && GO_LEFT ) castSquarePortals(x - 1, y);
		if (x < gridWidth - 1  && GO_RIGHT) castSquarePortals(x + 1, y);

	} // end method castSquarePortals
	
	// Fill a given region of the game grid with a texture. Similar to the above
	//   and used recursively, only it looks at grid triangles instead of squares.
	// Here, 'x' and 'y' are grid square coordinates, not fieldpoints.
	private void fillWithTexture(int x, int y, int side, Texture t) {
		
		// Fill the current triangle with the given texture.
		gameGrid[x][y].setTexture(side, t);
		
		// Check neighboring triangles to see if they are unfilled (don't
		//   pass the fill grid boundaries).
		if (side == DragoStatics.UPPER) {
			if (    ((fillGrid[x][y] & MapGridSquare.BOUND_UPRIGHT) == 0)
			     && (gameGrid[x][y].getTexture(DragoStatics.RIGHT) != t) )
			{
				fillWithTexture(x, y, DragoStatics.RIGHT, t);
			}
			if (    ((fillGrid[x][y] & MapGridSquare.BOUND_UPLEFT) == 0)
			     && (gameGrid[x][y].getTexture(DragoStatics.LEFT) != t) )
			{
				fillWithTexture(x, y, DragoStatics.LEFT, t);
			}
			if (y < gridHeight - 1) {
				if (    ((fillGrid[x][y + 1] & MapGridSquare.BOUND_DOWN) == 0)
				     && (gameGrid[x][y + 1].getTexture(DragoStatics.LOWER) != t) )
				{
					fillWithTexture(x, y + 1, DragoStatics.LOWER, t);
				}
			}
		}
		else if (side == DragoStatics.LOWER) {
			if (    ((fillGrid[x][y] & MapGridSquare.BOUND_DOWNRIGHT) == 0)
			     && (gameGrid[x][y].getTexture(DragoStatics.RIGHT) != t) )
			{
				fillWithTexture(x, y, DragoStatics.RIGHT, t);
			}
			if (    ((fillGrid[x][y] & MapGridSquare.BOUND_DOWNLEFT) == 0)
			     && (gameGrid[x][y].getTexture(DragoStatics.LEFT) != t) )
			{
				fillWithTexture(x, y, DragoStatics.LEFT, t);
			}
			if (y > 0) {
				if (    ((fillGrid[x][y - 1] & MapGridSquare.BOUND_UP) == 0)
				     && (gameGrid[x][y - 1].getTexture(DragoStatics.UPPER) != t) )
				{
					fillWithTexture(x, y - 1, DragoStatics.UPPER, t);
				}
			}
		}
		else if (side == DragoStatics.LEFT) {
			if (    ((fillGrid[x][y] & MapGridSquare.BOUND_DOWNLEFT) == 0)
			     && (gameGrid[x][y].getTexture(DragoStatics.LOWER) != t) )
			{
				fillWithTexture(x, y, DragoStatics.LOWER, t);
			}
			if (    ((fillGrid[x][y] & MapGridSquare.BOUND_UPLEFT) == 0)
			     && (gameGrid[x][y].getTexture(DragoStatics.UPPER) != t) )
			{
				fillWithTexture(x, y, DragoStatics.UPPER, t);
			}
			if (x > 0) {
				if (    ((fillGrid[x - 1][y] & MapGridSquare.BOUND_RIGHT) == 0)
				     && (gameGrid[x - 1][y].getTexture(DragoStatics.RIGHT) != t) )
				{
					fillWithTexture(x - 1, y, DragoStatics.RIGHT, t);
				}
			}
		}
		else if (side == DragoStatics.RIGHT) {
			if (    ((fillGrid[x][y] & MapGridSquare.BOUND_UPRIGHT) == 0)
			     && (gameGrid[x][y].getTexture(DragoStatics.UPPER) != t) )
			{
				fillWithTexture(x, y, DragoStatics.UPPER, t);
			}
			if (    ((fillGrid[x][y] & MapGridSquare.BOUND_DOWNRIGHT) == 0)
			     && (gameGrid[x][y].getTexture(DragoStatics.LOWER) != t) )
			{
				fillWithTexture(x, y, DragoStatics.LOWER, t);
			}
			if (x < gridWidth - 1) {
				if (    ((fillGrid[x + 1][y] & MapGridSquare.BOUND_LEFT) == 0)
				     && (gameGrid[x + 1][y].getTexture(DragoStatics.LEFT) != t) )
				{
					fillWithTexture(x + 1, y, DragoStatics.LEFT, t);
				}
			}
		}
	}

	// Replace AI lines of type 'WATER' and 'FLAMMABLE_TREES' with dynamic portals
	//   in the 'closed' state, and merge them with the existing static portals
	//   to create the full A* graph (the internal portal structure of tree polygons
	//   is also merged).
	private void connectDynamicPortals(BSPNode node, Texture type) {
		
		// If node is a leaf, do nothing.
		if (node instanceof BSPLeaf)
			return;
		
		// Get flags for this texture type.
		int flags = BSPNode.FWP_CLOSE | BSPNode.FWP_CONNECT;
		if (type == Texture.FLAMMABLE_TREES || type == Texture.STONES)
			flags |= BSPNode.FWP_UNIT_SQUARES;

		// Get all segments of AI lines of the specified type and
		//   put them in a list. Also add requisite properties to
		//   back leaves of segments.
		Vector<LineSeg> AILineSegList = new Vector<LineSeg>();
		for (int ailine = 0; ailine < node.getNumAILines(); ++ailine) {
			TexturedLine l = node.getAILine(ailine);
			if (l.getType() == type)
				AILineSegList.add(new LineSeg(l.x1(), l.y1(), l.x2(), l.y2()));
		} // end for (by AI line in node)

		// Remove all lines of given type from the node.
		Vector<TexturedLine> newAILineList = new Vector<TexturedLine>();
		for (int ailine = 0; ailine < node.getNumAILines(); ++ailine) {
			TexturedLine l = node.getAILine(ailine);
			if (l.getType() != type) {
				newAILineList.add(new TexturedLine(
				        l.x1(), l.y1(), l.x2(), l.y2(), l.getType()));
			}
		}
		int prevAILineCount = node.getNumAILines();
		for (int ailine = 0; ailine < prevAILineCount; ++ailine)
			node.removeAILine(0);
		for (int ailine = 0; ailine < newAILineList.size(); ++ailine)
			node.addAILine(newAILineList.get(ailine));

		// Add dynamic portals in place of old AI lines of given type.
		for (int ailine = 0; ailine < AILineSegList.size(); ++ailine) {
			LineSeg l = AILineSegList.get(ailine);
			node.fillWithPortals(l.x1(), l.y1(), l.x2(), l.y2(), this, flags);
		}

		// Do same for water portals of front and back child nodes.
		connectDynamicPortals(node.front(), type);
		connectDynamicPortals(node.back(), type);
	}

	// Connect all the static portals in the BSP with each other (this involves
	//   a tree traversal). It is assumed that 'connectStaticPortalsToLeaves(node)' has
	//   already been called.
	private void connectStaticPortals(BSPNode node) {
		
		// Keep traversing the tree until a leaf is found, then connect all
		//   portals of that leaf.
		if (node instanceof BSPLeaf) {

			// Connect the portals.
			for (int ptl1 = 0; ptl1 < node.getNumPortals(); ++ptl1) {
				for (int ptl2 = ptl1 + 1; ptl2 < node.getNumPortals(); ++ptl2) {
					node.getPortal(ptl1).addNeighbor(node.getPortal(ptl2));
					node.getPortal(ptl2).addNeighbor(node.getPortal(ptl1));
				}
			}
		}
		else {

			// Not a leaf; check front and back child nodes.
			connectStaticPortals(node.front());
			connectStaticPortals(node.back());
		}
	}
	
	// Connect all portals in the BSP to its leaves (tree traversal used).
	private void connectStaticPortalsToLeaves(BSPNode node) {
		
		// If on a leaf, no portals there yet; return.
		if (node instanceof BSPLeaf) return;
		
		for (int portal = 0; portal < node.getNumPortals(); ++portal) {
			
			// Get next portal in node.
			Portal ptl = node.getPortal(portal);
			
			// Get front and back leaves of portal.
			BSPLeaf frontLeaf = BSPNode.getLeaf(node, ptl.getSegment(), LineSeg.FRONT);
			BSPLeaf backLeaf = BSPNode.getLeaf(node, ptl.getSegment(), LineSeg.BACK);
			
			// Return error if either leaf is found to be 'null'.
			if (frontLeaf == null || backLeaf == null)
				error("connectPortalsToLeaves", "null leaf found beside portal" +
						" at (" + ptl.getSegment().x1() + ", " + ptl.getSegment().y1() + ")"
						+ " - (" + ptl.getSegment().x2() + ", " + ptl.getSegment().y2() + ")");
			
			// Attach portal to leaves.
			frontLeaf.addPortal(ptl);
			backLeaf.addPortal(ptl);
			
			// Assign leaves to portal.
			ptl.setFrontLeaf(frontLeaf);
			ptl.setBackLeaf(backLeaf);
			
		} // end for (by portal)
		
		// Do the same for this node's front and back child.
		connectStaticPortalsToLeaves(node.front());
		connectStaticPortalsToLeaves(node.back());
		
	} // end connectStaticPortalsToLeaves

	// Attach all AI lines to collision grid (call with root).
	private void attachAILines(BSPNode node) {
		if (node instanceof BSPLeaf) return;
		for (int i = 0; i < node.getNumAILines(); ++i) {
			TexturedLine l = node.getAILine(i);
			int msx = (l.x2()<l.x1() && l.x1()%32==0 ? (l.x1()>>5)-1 : l.x1()>>5);
			int msy = (l.y2()<l.y1() && l.y1()%32==0 ? (l.y1()>>5)-1 : l.y1()>>5);
			int mfx = (l.x1()<l.x2() && l.x2()%32==0 ? (l.x2()>>5)-1 : l.x2()>>5);
			int mfy = (l.y1()<l.y2() && l.y2()%32==0 ? (l.y2()>>5)-1 : l.y2()>>5);
			int mstepx = (l.x2()<l.x1() ? -1 : (l.x1()<l.x2() ? 1 : 0));
			int mstepy = (l.y2()<l.y1() ? -1 : (l.y1()<l.y2() ? 1 : 0));
			while (true) {
				if (msx<this.gridWidth-1    && msy<this.gridHeight-1)
				    this.gameGrid[msx][msy].attach(l);
				if (l.x1()==l.x2() && msx>0 && msy<this.gridHeight-1)
				    this.gameGrid[msx-1][msy].attach(l);
				if (l.y1()==l.y2() && msy>0 && msx<this.gridWidth -1)
				    this.gameGrid[msx][msy-1].attach(l);
				if (msx == mfx && msy == mfy)
				    break;
				msx += mstepx;
				msy += mstepy;
				
			} // end while (by grid square)
		} // end for (by AI line in node)
		attachAILines(node.front());
		attachAILines(node.back());
		
	} // end connectWalls
	
	// Make a grid selection given a convex polygon. The method records
	//   essential information about which grid squares the convex polygon
	//   overlaps (including the interior of the polygon), placing it
	//   in the 'gridSelection' data members of this class.
	public void makeGridSelection(Polygon p) {
		
		// Get selection data for each line of polygon, keeping track
		//   of the lowest and highest rows encountered.
	    gridSelectionLowestRow[0] = gridHeight - 1;
		gridSelectionHighestRow[0] = 0;
		for (int i = 0; i < p.getNumVertices(); ++i) {
			int x1 = p.getVertex(i).x;
			int y1 = p.getVertex(i).y;
			int x2 = p.getVertex((i + 1)%p.getNumVertices()).x;
			int y2 = p.getVertex((i + 1)%p.getNumVertices()).y;
			makeGridSelection(i + 1, x1, y1, x2, y2);
			if (gridSelectionLowestRow[i + 1] < gridSelectionLowestRow[0])
				gridSelectionLowestRow[0] = gridSelectionLowestRow[i + 1];
			if (gridSelectionHighestRow[i + 1] > gridSelectionHighestRow[0])
				gridSelectionHighestRow[0] = gridSelectionHighestRow[i + 1];
		}
		
		// Get the full selection spanned on each row.
		for (int row = gridSelectionLowestRow[0];
				row <= gridSelectionHighestRow[0];
				++row)
		{
			gridSelectionLeft[0][row] = gridWidth - 1;
			gridSelectionRight[0][row] = 0;
			for (int i = 0; i < p.getNumVertices(); ++i) {
				if (    (gridSelectionLowestRow[i + 1] <= row)
					 && (row <= gridSelectionHighestRow[i + 1]) )
				{
					if (gridSelectionLeft[i + 1][row] < gridSelectionLeft[0][row])
						gridSelectionLeft[0][row] = gridSelectionLeft[i + 1][row];
					if (gridSelectionRight[i + 1][row] > gridSelectionRight[0][row])
						gridSelectionRight[0][row] = gridSelectionRight[i + 1][row];
				}
			
			} // end for (by polygon line)
		} // end for (by row of selection grid)
	} // end method makeGridSelection (for polygons)
	
	// Make a grid selection using a line as the parameter.
	public void makeGridSelection(int selNum, int x1, int y1, int x2, int y2) {
		
		// Get simple row if y1 == y2.
		if (y1 == y2) {
			
			// Get the grid row that the horizontal segment
			//   lies in.
			int givenRow = clipHeight(y1 >> 5);
			gridSelectionLowestRow[selNum] = givenRow;
			gridSelectionHighestRow[selNum] = givenRow;
			
			// Swap so that x1 <= x2.
			if (x2 < x1) { int temp = x1; x1 = x2; x2 = temp; }
			
			// Get leftmost and rightmost grid squares occupied.
			gridSelectionLeft[selNum][givenRow] = clipWidth(x1 >> 5);
			gridSelectionRight[selNum][givenRow] = clipWidth(x2 >> 5);
			
			// All done.
			return;
			
		} // end if (line is horizontal)
		
		// Swap points so that first point is on top.
		if (y1 < y2) {
			int temp = x1; x1 = x2; x2 = temp;
			temp = y1; y1 = y2; y2 = temp;
		}
		
		// Get left or right direction of line segment,
		//   going down.
		int fieldPointIncrement = (x1 < x2 ? 1 : -1);
		int direction = (x1 < x2 ? 1 : 0);
		
		// Get xDeltaBegin and its fractional remainder.
		int numerator = (x2 - x1)*fieldPointIncrement*(y1 % 32);
		int xDeltaBegin = numerator / (y1 - y2);
		int fractPartBegin = numerator % (y1 - y2);
		int fractPartRunningCount = fractPartBegin;
		
		// Get xDelta and its fractional remainder.
		numerator = (x2 - x1)*fieldPointIncrement*32;
		int xDelta = numerator / (y1 - y2);
		int fractPart = numerator % (y1 - y2);
		
		// Get the squares overlapped before the first
		//   intersection, going down.
		int currentExtremities[] = new int[2];
		currentExtremities[1 - direction] = clipWidth(x1 >> 5);
		x1 += xDeltaBegin*fieldPointIncrement;
		currentExtremities[direction] = clipWidth(x1 >> 5);
		
		// Add the squares to the grid selection arrays.
		//   Also initialize 'currentRow' for scanning loop.
		int currentRow = clipHeight((y1%32 == 0) ? (y1>>5) - 1 : y1>>5);
		gridSelectionHighestRow[selNum] = currentRow;
		gridSelectionLeft[selNum][currentRow] = currentExtremities[0];
		gridSelectionRight[selNum][currentRow] = currentExtremities[1];
		
		// Put the sign back on xDelta.
		xDelta *= fieldPointIncrement;
		
		// Get the lowest row of the grid overlapped by the
		//   line segment, to use as a loop termination
		//   condition.
		int lowestRow = clipHeight(y2 >> 5);
		
		// Main loop. Scan down and add rows of squares to the
		//   grid selection array.
		while (currentRow > lowestRow) {
			
			// Go down one row.
			--currentRow;
			
			// Use the old extremity on one side as the new
			//   extremity on the other side.
			currentExtremities[1 - direction] = currentExtremities[direction];
			
			// Use 'x1' as the variable of interest; add 'xDelta' successively
			//   (remembering to tally the fractional part).
			x1 += xDelta;
			fractPartRunningCount += fractPart;
			if (fractPartRunningCount >= y1 - y2) {
				x1 += fieldPointIncrement;
				fractPartRunningCount -= (y1 - y2);
			}
			
			// Use the new value of 'x1' to collect the overlapped
			//   squares.
			currentExtremities[direction] = clipWidth(x1 >> 5);
			gridSelectionLeft[selNum][currentRow] = currentExtremities[0];
			gridSelectionRight[selNum][currentRow] = currentExtremities[1];
			
		} // end while
		
		// Get the correct overlapped squares for the final row
		//   using (x2, y2).
		currentExtremities[direction] = clipWidth(x2 >> 5);
		gridSelectionLeft[selNum][lowestRow] = currentExtremities[0];
		gridSelectionRight[selNum][lowestRow] = currentExtremities[1];
		
		// Save the lowest row scanned in the grid selection array.
		gridSelectionLowestRow[selNum] = lowestRow;
		
	} // end method makeGridSelection (for lines)
	
	// Get all obstacles -- walls, entities, and water portals -- that
	//   overlap with the grid squares overlapped by a given polygon.
	//   Used, e.g., for the polygon of motion in collision handling.
	// Here, 'p' is the 'in' variable, and 'walls'/'entities'/
	//   'waterPortals' are the 'out' variables.
	public void getObstacles(Polygon p,
	        ArrayList<TexturedLine> walls,
	        ArrayList<Entity> entities,
	        ArrayList<Portal> waterPortals,
	        boolean test)
	{
	    // Get all the grid squares overlapped by the given polygon.
        this.makeGridSelection(p);
    
        // Get one reference for each entity/line/water portal.
        for (int type = 0; type < 3; ++type) {
            for (int row =  this.gridSelectionLowestRow[0];
                     row <= this.gridSelectionHighestRow[0];
                     ++row)
            {
                for (int col =  this.gridSelectionLeft[0][row];
                         col <= this.gridSelectionRight[0][row];
                         ++col)
                {
                    if (type == 0) {
                        for (int i = 0; i < this.gameGrid[col][row].getNumEntities(); ++i) {
                            if (!entities.contains(this.gameGrid[col][row].entities.get(i)))
                            {
                                entities.add(this.gameGrid[col][row].entities.get(i));
                            }
                        }
                    } else if (type == 1) {
                        for (int i = 0; i < this.gameGrid[col][row].getNumAILines(); ++i) {
                            if (    !walls.contains(this.gameGrid[col][row].AILines.get(i))
                                 && Texture.isWall(this.gameGrid[col][row].AILines.get(i).getType()) )
                            {
                                walls.add(this.gameGrid[col][row].AILines.get(i));
                            }
                        }
                    } else if (type == 2) {
                        for (int i = 0; i < this.gameGrid[col][row].getNumWaterPortals(); ++i) {
                            if (!waterPortals.contains(this.gameGrid[col][row].waterPortals.get(i)))
                                waterPortals.add(this.gameGrid[col][row].waterPortals.get(i));
                        }
                        
                    } // end if (what obstacle type?)
                } // end for (by column of grid selection)
            } // end for (by row of grid selection)
        } // end for (by obstacle type)
	} // end method getObstacles
	
	// For testing purposes only.
	private void displayPartitions(BSPNode node) {
		if (node instanceof BSPLeaf)
			return;
		LineSeg l = node.getFullPartition();
		System.out.println("Full partition extends to " +
				"(" + l.x1() + ", " + l.y1() + ") - (" + l.x2() + ", " + l.y2() + ")");
		displayPartitions(node.front());
		displayPartitions(node.back());
	}
	
	// More testing.
	private void displayPortals(BSPNode node) {

		if (node instanceof BSPLeaf) {
			for (int portal = 0; portal < node.getNumPortals(); ++portal) {
				LineSeg l = node.getPortal(portal).getSegment();
				System.out.println((node.getPortal(portal).isOpen() ? "Open" : "Closed") + " portal at "
						+ "(" + l.x1() + ", " + l.y1() + ") - (" + l.x2() + ", " + l.y2() + ")");
			}
			if (node.getNumPortals() > 0) System.out.println("");
			return;
		}
		displayPortals(node.front());
		displayPortals(node.back());
	}

	private void error(String function, String message) {
		System.out.println("Playfield." + function + "(): " + message);
		System.exit(1);
	}
}

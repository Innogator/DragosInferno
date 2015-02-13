/*
 * Entity.java (v1.0)
 * 3/17/2013
 */
package entity;

import java.util.ArrayList;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;

import drago.DragoStatics;
import drago.Fraction; // Not used yet.

import playfield.MapGridSquare;
import playfield.Playfield;
import playfield.Polygon;
import playfield.Fieldpoint;
import playfield.TexturedLine;
import playfield.Portal;
import playfield.LineSeg;

/**
 * The 'Entity' class, which is the most general class that
 * a movable or destructible object in the playfield can be.
 * 
 * @author Quothmar
 *
 */
abstract public class Entity {

    // Playfield that contains the entity as object, i.e., the
    //   parent playfield.
    Playfield ppf;

    // The current health of the entity.
    private int health;
    public int getHealth() { return health; }
    public void setHealth(int newHealth) { health = newHealth; }
    public void addHealth(int healthDelta) {
        health += healthDelta;
        if (health < 0) health = 0;
    }
    
    // The current speed of the entity.
    private int speed;
    public int getSpeed() { return speed; }
    public void setSpeed(int newSpeed) { speed = newSpeed; }
    public void addSpeed(int ds) { speed += ds; }

    // Current location of entity. Note: the (x, y) location
	//   represents the middle of the sprite to be drawn (in
	//   Cartesian coordinates), and may or may not be included in
	//   the *bounding polygon* of the entity. The bounding polygon
	//   is what is used in all physical interactions. The 'z'
	//   coordinate, however, will be used for physical interactions
	//   as well.
	private int xLocation;
	private int yLocation;
	private int zLocation;	// This is altitude. 'z' will be positive for flying objects.
	public int x() { return xLocation; }
	public int y() { return yLocation; }
	public int z() { return zLocation; }
	public void setx(int x) { xLocation = x; }
	public void sety(int y) { yLocation = y; }
	public void setz(int z) { zLocation = z; }
	public void addx(int dx) { xLocation += dx; }
	public void addy(int dy) { yLocation += dy; }
	public void addz(int dz) { zLocation += dz; }
	    
	// Physical height of entity.
	private int height;
	public int getHeight() { return height; }
	public void setHeight(int newHeight) { height = newHeight; }
	
    // Bounding box of entity. Surrounds the bounding polygon and
    //   is used as the basis for sprite-painting and some A* navigation.
	//   (bboxx, bboxy) is the lower-left corner of the bounding box.
    private int bboxx = 0;
    private int bboxy = 0;
    private int bboxwidth = 0;
    private int bboxheight = 0;
    public int bboxx() { return bboxx; }
    public int bboxy() { return bboxy; }
    public int bboxwidth() { return bboxwidth; }
    public int bboxheight() { return bboxheight; }

    // Bounding polygon of entity. (Note: the bounding polygon
	//   does not necessarily "bound" the sprite: it is just used
	//   as the physical boundary of the entity in the playfield.)
    //   Also sets bounding box of entity, as it is dependent on the
    //   polygon.
	private Polygon boundingPoly;
	public Polygon getBoundingPoly() { return boundingPoly; }
	public void setBoundingPoly(Polygon p) {
	    boundingPoly = p;
	    int minx = 2147483647;
	    int miny = 2147483647;
	    int maxx = 0;
	    int maxy = 0;
	    for (int i = 0; i < p.getNumVertices(); ++i) {
	        if (p.getVertex(i).x < minx) minx = p.getVertex(i).x;
	        if (p.getVertex(i).x > maxx) maxx = p.getVertex(i).x;
	        if (p.getVertex(i).y < miny) miny = p.getVertex(i).y;
	        if (p.getVertex(i).y > maxy) maxy = p.getVertex(i).y;
	    }
	    bboxx = minx;
	    bboxy = miny;
	    bboxwidth = maxx - minx;
	    bboxheight = maxy - miny;
	}
	
	// Path-testing polygon of entity, used to render the results
	//   of an 'attemptMotion' call.
	public Polygon pathTestPoly[] = new Polygon[200];
	public int pathTestPolyCount = 0;
	public int pathx = 0;
	public int pathy = 0;
	
	// Current direction of motion.
	private int xDelta;
	private int yDelta;
	private int zDelta;
	public int dx() { return xDelta; }
	public int dy() { return yDelta; }
	public int dz() { return zDelta; }
	public void setdx(int newDX) { xDelta = newDX; }
	public void setdy(int newDY) { yDelta = newDY; }
	public void setdz(int newDZ) { zDelta = newDZ; }
	public void adddx(int ddx) { xDelta += ddx; }
	public void adddy(int ddy) { yDelta += ddy; }
	public void adddz(int ddz) { zDelta += ddz; }
	
	// Previous direction of motion (as distinguished from current
	//   direction of motion or direction facing). This will be used
	//   when handling vertex-to-vertex sliding to give the player
	//   more mobility in twisted corridors.
	private int xPrevDelta;
	private int yPrevDelta;
	public int prevdx() { return xPrevDelta; }
	public int prevdy() { return yPrevDelta; }
	public void setprevdx(int newPrevDX) { xPrevDelta = newPrevDX; }
	public void setprevdy(int newPrevDY) { yPrevDelta = newPrevDY; }
	
	// The direction the entity is facing. This is to be distinguished
	//   from the direction the entity is actually *moving* (when sliding
	//   against a wall, for example, the player will still face the
	//   direction of attempted motion). Uses constants.
	private int facing;
	public static final int F_UP = 0;
	public static final int F_UP_RIGHT = 1;
	public static final int F_RIGHT = 2;
	public static final int F_DOWN_RIGHT = 3;
	public static final int F_DOWN = 4;
	public static final int F_DOWN_LEFT = 5;
	public static final int F_LEFT = 6;
	public static final int F_UP_LEFT = 7;
	public int getFacing() { return facing; }
	public void setFacing(int newFacing) { facing = newFacing; }
	public static String getStringForFacing(int nfacing) {
	    switch (nfacing) {
	    case F_UP: return "UP";
        case F_UP_RIGHT: return "UP_RIGHT";
        case F_RIGHT: return "RIGHT";
        case F_DOWN_RIGHT: return "DOWN_RIGHT";
        case F_DOWN: return "DOWN";
        case F_DOWN_LEFT: return "DOWN_LEFT";
        case F_LEFT: return "LEFT";
        case F_UP_LEFT: return "UP_LEFT";
        default: return "";
	    }
	}
	
	// Adds a given magnitude to the sprite position (x, y) or
	//   entity velocity (dx, dy) in the direction the entity
	//   is facing.
    public void addWhereFacing(int m, boolean velocity) {

        // Advance either the (x, y) sprite position or the
        //   (dx, dy) velocity based on the direction this
        //   entity is currently facing.
        if (    this.getFacing() == Entity.F_UP
             || this.getFacing() == Entity.F_UP_LEFT
             || this.getFacing() == Entity.F_UP_RIGHT )
        {
            if (velocity) this.adddy(m); else this.addy(m);
        }
        else if (    this.getFacing() == Entity.F_DOWN
                  || this.getFacing() == Entity.F_DOWN_LEFT
                  || this.getFacing() == Entity.F_DOWN_RIGHT )
        {
            if (velocity) this.adddy(-m); else this.addy(-m);
        }
        if (    this.getFacing() == Entity.F_RIGHT
             || this.getFacing() == Entity.F_UP_RIGHT
             || this.getFacing() == Entity.F_DOWN_RIGHT )
        {
            if (velocity) this.adddx(m); else this.addx(m);
        }
        else if (    this.getFacing() == Entity.F_LEFT
                  || this.getFacing() == Entity.F_UP_LEFT
                  || this.getFacing() == Entity.F_DOWN_LEFT )
        {
            if (velocity) this.adddx(-m); else this.addx(-m);
        }
    
    } // end method addWhereFacing

	
	// The state counter of the entity. This will decrement once
	//   every frame when set, with assigned ranges for various
	//   effects. Ticking to '0' will cause a state check to be
	//   performed (counter will be reset when walking, transition
	//   to standing will be made when done getting burned, etc.).
	//   This will vary by entity type, so method is made abstract.
	protected int stateCounter;
	public int getStateCounter() { return stateCounter; }
	public void setStateCounter(int frames) { stateCounter = frames; }
	abstract public void tickStateCounter();
	
	// The actual state of the entity, as used in collision response.
	//   Each entity state will appear in the collision table.
	//   (Right now it's only player states, but there will be more:
	//   human states, item states, etc.)
	private int state;
	public int getState() { return state; }
	public void setState(int newState) { state = newState; }
	
	// The default starting state (e.g., standing). Will be
	//   overridden in subclasses.
	private final int defaultState = 0;
	
	// All states possible (they must be distinguishable).
	public static final int ST_PLAYER_WALKING = 0;
	public static final int ST_PLAYER_CHARGING = 1;
	public static final int ST_PLAYER_STANDING = 2;
	public static final int ST_PLAYER_RISING = 3;
	public static final int ST_PLAYER_FLYING = 4;
	public static final int ST_PLAYER_LOWERING = 5;
	public static final int ST_PLAYER_FREEFALL = 6;
	public static final int ST_PLAYER_STOMPING = 7;
	public static final int ST_PLAYER_DEAD = 8;
	public static final int ST_TREE_INTACT = 9;
	public static final int ST_TREE_BURNING = 10;
	public static final int ST_STONE_INTACT = 11;
	public static final int ST_STONE_BREAKING = 12;
	public static final int ST_WALL = 13;
	public static final int ST_WATER = 14;
	public static final int ST_ARROW = 15;
	public static final int ST_REFLECT_ARROW = 16;
	public static final int ST_ROCK = 17;
	public static final int ST_HUMAN_WALKING = 18;
	public static final int ST_HUMAN_TAILSTRUCK = 19;
	public static final int ST_HUMAN_FROZEN = 20;
	public static final int ST_HUMAN_BURNED = 21;
	public static final int ST_FIRE_BREATH = 22;
	public static final int ST_ICE_BREATH = 23;
	public static final int ST_PLAYER_FIREBALL = 24;
	public static final int ST_HUMAN_DEAD = 25;
	public static final int ST_HUMAN_SHATTERING = 26;
	public static final int ST_HUMAN_STANDING = 27;
	public static final int ST_FIREBALL_EXTINGUISHING = 28;
	public static final int ST_EXPLOSION = 29;
	public static final int ST_ITEM = 30;
    public static final int currentNumStates = 31;
	public static int getStateByString(String s) {
	    switch (s) {
	    case "ST_PLAYER_WALKING": return ST_PLAYER_WALKING;
	    case "ST_PLAYER_CHARGING": return ST_PLAYER_CHARGING;
	    case "ST_PLAYER_STANDING": return ST_PLAYER_STANDING;
	    case "ST_PLAYER_RISING": return ST_PLAYER_RISING;
	    case "ST_PLAYER_FLYING": return ST_PLAYER_FLYING;
	    case "ST_PLAYER_LOWERING": return ST_PLAYER_LOWERING;
	    case "ST_PLAYER_FREEFALL": return ST_PLAYER_FREEFALL;
	    case "ST_PLAYER_STOMPING": return ST_PLAYER_STOMPING;
	    case "ST_PLAYER_DEAD": return ST_PLAYER_DEAD;
	    case "ST_TREE_INTACT": return ST_TREE_INTACT;
	    case "ST_TREE_BURNING": return ST_TREE_BURNING;
	    case "ST_STONE_INTACT": return ST_STONE_INTACT;
	    case "ST_STONE_BREAKING": return ST_STONE_BREAKING;
	    case "ST_WALL": return ST_WALL;
	    case "ST_WATER": return ST_WATER;
	    case "ST_ARROW": return ST_ARROW;
	    case "ST_REFLECT_ARROW": return ST_REFLECT_ARROW;
	    case "ST_ROCK": return ST_ROCK;
	    case "ST_HUMAN_WALKING": return ST_HUMAN_WALKING;
	    case "ST_HUMAN_TAILSTRUCK": return ST_HUMAN_TAILSTRUCK;
	    case "ST_HUMAN_FROZEN": return ST_HUMAN_FROZEN;
	    case "ST_HUMAN_BURNED": return ST_HUMAN_BURNED;
	    case "ST_FIRE_BREATH": return ST_FIRE_BREATH;
	    case "ST_ICE_BREATH": return ST_ICE_BREATH;
	    case "ST_PLAYER_FIREBALL": return ST_PLAYER_FIREBALL;
	    case "ST_HUMAN_DEAD": return ST_HUMAN_DEAD;
	    case "ST_HUMAN_SHATTERING": return ST_HUMAN_SHATTERING;
	    case "ST_HUMAN_STANDING": return ST_HUMAN_STANDING;
	    case "ST_FIREBALL_EXTINGUISHING": return ST_FIREBALL_EXTINGUISHING;
	    case "ST_EXPLOSION": return ST_EXPLOSION;
	    case "ST_ITEM": return ST_ITEM;
	    }
	    return 0;
	} // end method getStateByString
	
	// Current grid squares the entity is overlapping.
	private ArrayList<MapGridSquare> squares = new ArrayList<MapGridSquare>();
	public MapGridSquare getSquare(int i) { return squares.get(i); }
	public int getNumSquares() { return squares.size(); }
	
	// Position/facing constructor. Can be used directly
	//   from mapfile contents.
	public Entity(int xstart, int ystart, String beginFacing, Playfield pf) {
		this.setx(xstart);
		this.sety(ystart);
		this.setz(0);
		switch (beginFacing) {
		case "UP": 			facing = F_UP; 			break;
		case "UP_RIGHT": 	facing = F_UP_RIGHT; 	break;
		case "RIGHT": 		facing = F_RIGHT; 		break;
		case "DOWN_RIGHT": 	facing = F_DOWN_RIGHT; 	break;
		case "DOWN": 		facing = F_DOWN; 		break;
		case "DOWN_LEFT": 	facing = F_DOWN_LEFT; 	break;
		case "LEFT": 		facing = F_LEFT; 		break;
		case "UP_LEFT": 	facing = F_UP_LEFT; 	break;
		default: error("Entity", "invalid starting direction");
		}
		this.setState(defaultState);
		ppf = pf;
	}
	
	// 'Act' method. All entities must implement this method to participate
	//   in the game loop.
	abstract public void act();

	// Method to remove this entity from the playfield, including its
	//   squares and its active entities.
	public void disappear() {
        for (int i = 0; i < this.squares.size(); ++i)
            this.squares.get(i).entities.remove(this);
        this.ppf.removeActiveEntity(this);
        this.ppf.removeEntity(this);
	}
	
	// The sprite set for the given entity and the method to draw the sprite.
	// Note: the method 'drawSprite' is temporary until Ryan Alain finishes
	//   implementation of the SpriteSequence class, which should contain
	//   a list of sprites and their locations to be drawn in an order that
	//   causes them to overlap correctly.
	abstract public BufferedImage getCurrentSprite();
	public void drawSprite(Graphics2D g2d, int xcam, int ycam) {
		
	    boolean VERBOSE = false;
	    
		boolean BOUNDING_POLY = false;
		boolean POLY_OF_MOTION = false;
		boolean ATTEMPT_MOTION_TEST = false;
		
		Polygon poly = null;
		
		if (VERBOSE && (this instanceof Fireball)) System.out.println("Now" +
				" attempting to draw a Fireball");
		
		// Draw sprite centered around current location.
		//int drawx = (this.x() - (getCurrentSprite().getWidth() >> 1)) - xcam;
		//int drawy = 480 - ((this.y() + (getCurrentSprite().getHeight() >> 1)) - ycam);
		int drawx = this.bboxx + ((this.bboxwidth - getCurrentSprite().getWidth()) >> 1) - xcam;
		int drawy = 480 - (this.bboxy + getCurrentSprite().getHeight() - ycam);
		g2d.drawImage(getCurrentSprite(), drawx, drawy - this.z(), null);
		if (this instanceof Player) {
		    g2d.setColor(Color.WHITE);
	        g2d.setFont(new Font("Arial", Font.BOLD, 14));
		    //g2d.drawString("Dragon's (x, y) = (" + this.x() + ", " + this.y() + ")", 20, 40);
		    DragoStatics.drawShadedString("Dragon's (x, y) = (" + this.x() + ", " + this.y() + ")", 20, 40, g2d);
		    Player pl = (Player)this;
		    DragoStatics.drawShadedString("Dragon's speed is " + pl.getSpeed(), 20, 60, g2d); 
		}
		
		// Draw either the bounding polygon or the polygon of motion (possibly both)
		//   along with the entity. Used for testing.
		for (int i = 0; i < 2; ++i) {
		    
		    boolean flag = false;
		    Color color = Color.BLACK;
		    switch (i) {
		    case 0: flag = BOUNDING_POLY; poly = boundingPoly; color = Color.WHITE; break;
		    case 1: flag = POLY_OF_MOTION; poly = this.getPolyOfMotion(dx(), dy()); color = Color.BLUE; break;
		    }
		    
   		    if (flag) poly.draw(g2d, xcam, ycam, color);
		    
		} // end for (by flag)

		if (ATTEMPT_MOTION_TEST) {
		    Color color = Color.CYAN;
		    for (int i = 0; i < pathTestPolyCount; ++i)
		        pathTestPoly[i].draw(g2d, xcam, ycam, color);
		}

	} // end method drawSprite
	
	// Methods to attach/remove grid squares to/from entity's list.
	public void attachSquare(MapGridSquare square) { squares.add(square); }
	public void removeSquare(MapGridSquare square) { squares.remove(square); }

	// Method to reattach object to current grid selection of given
	//   Playfield object. Currently just clears the list and
	//   adds new MapGridSquares (could this be optimized?).
	// (I realized also that I'm passing the playfield to several
	//   member functions of class Entity. I'm not sure whether this
	//   is considered good OOP practice, but I guess I will learn
	//   in due time!)
	public void attach() {
		
		// Remove this entity from the old MapGridSquares it
		//   was overlapping.
		for (int i = 0; i < this.getNumSquares(); ++i)
			this.squares.get(i).entities.remove(this);
		
		// Clear the current list of MapGridSquares.
		squares.clear();
		
		// Make a grid selection using this entity's bounding
		//   polygon.
		ppf.makeGridSelection(this.boundingPoly);
		
		// Attach all squares selected to this entity, and attach
		//   entity to those squares.
		for (int row = ppf.gridSelectionLowestRow[0];
				row <= ppf.gridSelectionHighestRow[0];
				++row)
		{
			for (int col = ppf.gridSelectionLeft[0][row];
					col <= ppf.gridSelectionRight[0][row];
					++col)
			{
				this.attachSquare(ppf.gameGrid[col][row]);
				ppf.gameGrid[col][row].entities.add(this);
			}
		}
	
	} // end method attach
	
	// Move the entity in a specified direction (no collision
	//   handling used).
	public void move(int dx, int dy, int dz) {
		
	    boolean VERBOSE = false;
	    
		// Move the physical location of the entity.
		this.setx(this.x() + dx);
		this.sety(this.y() + dy);
		this.setz(this.z() + dz);
		
		// Move the bounding polygon of the entity.
		for (int i = 0; i < this.boundingPoly.getNumVertices(); ++i) {
			this.boundingPoly.getVertex(i).x += dx;
			this.boundingPoly.getVertex(i).y += dy;
			if (VERBOSE && (this instanceof Fireball)) System.out.println("Moving vertex of object of class "
			        + this.getClass() + " to ("
			        + this.boundingPoly.getVertex(i).x + ", "
			        + this.boundingPoly.getVertex(i).y + ")\n"
			        + "  This object's position: (" + this.x() + ", " + this.y() + ")");
		}
		
		// Move the bounding box of the entity.
		this.bboxx += dx;
		this.bboxy += dy;
		
		// Reattach the entity to new grid squares.
		this.attach();

		// Remember direction moved, for use with vertex-to-vertex sliding.
		if (dx != 0 || dy != 0) {
		    this.setprevdx(dx);
		    this.setprevdy(dy);
		}
		
	}
	
	// Obtain the entity's current polygon of motion. This will be
	//   called every frame, but we can optimize it later if needed.
	public Polygon getPolyOfMotion(int dx, int dy) {
		
	    if (this == null) return null;
	    
	    // Test constant used to visibly expand the vector of motion.
	    final int EXPAND = 1;
	    
	    // Initialize polygon of motion.
	    Polygon pom = new Polygon();
	    
	    // Get vertices of leave and return.
	    int leaveVertex = 0;
	    int returnVertex = 0;
        boolean usingx = (dx*dx < dy*dy);
	    int min = 2147483647;
        int max = -2147483648;
        int vertices = this.getBoundingPoly().getNumVertices();
        int[] vals = new int[vertices];
	    for (int i = 0; i < vertices; ++i) {
	        int val = this.getBoundingPoly().getVertex(i).y*dx
	                - this.getBoundingPoly().getVertex(i).x*dy;
	        if (usingx) val = -val;
	        if (val < min) { min = val; leaveVertex = i; }
	        if (val > max) { max = val; returnVertex = i; }
	        vals[i] = val;
	    }
	    if (min == max) return this.getBoundingPoly();
        if (usingx) {
            int temp = leaveVertex;
            leaveVertex = returnVertex;
            returnVertex = temp;
        }
	    while (vals[leaveVertex] == vals[(leaveVertex + 1) % vertices])
	        leaveVertex = (leaveVertex + 1) % vertices;
	    while (vals[returnVertex] == vals[(returnVertex - 1 + vertices) % vertices])
	        returnVertex = (returnVertex - 1 + vertices) % vertices;
	    
	    // Create the polygon of motion.
	    boolean traversed = false;
	    for (int i = leaveVertex;
	         !traversed;
	         i = (i + 1) % vertices)
	    {
	        int x = this.getBoundingPoly().getVertex(i).x + dx*EXPAND;
	        int y = this.getBoundingPoly().getVertex(i).y + dy*EXPAND;
	        pom.addVertex(new Fieldpoint(x, y));
	        if (i == returnVertex) traversed = true;
	    }
	    traversed = false;
	    for (int i = returnVertex;
	         !traversed;
	         i = (i + 1) % vertices)
	    {
	        int x = this.getBoundingPoly().getVertex(i).x;
	        int y = this.getBoundingPoly().getVertex(i).y;
	        pom.addVertex(new Fieldpoint(x, y));
	        if (i == leaveVertex) traversed = true;
	    }
	    
	    // Remove any extra vertices that lie directly on a line
	    //   of the polygon. Qualification added to make 'overlaps'
	    //   method work.
	    Polygon betterPom = new Polygon();
	    int N = pom.getNumVertices();
	    for (int i = 0; i < N; ++i)
	    {
	        int xmid = pom.getVertex(i).x;
	        int ymid = pom.getVertex(i).y;
	        int xnext = pom.getVertex((i + 1) % N).x;
            int ynext = pom.getVertex((i + 1) % N).y;
            int xprev = pom.getVertex((i + N - 1) % N).x;
            int yprev = pom.getVertex((i + N - 1) % N).y;
            if ((ymid - yprev)*(xnext - xmid) != (ynext - ymid)*(xmid - xprev))
                betterPom.addVertex(new Fieldpoint(xmid, ymid));
	    }
	    pom = betterPom;
	    
	    return pom;
		
	} // end method getPolyOfMotion
	
	// The 'attemptMotion' stub. This will contain a *long*
	//   collision algorithm, but right now it just calls 'move'.
	//   This allows the moving entity to walk through walls.
	public void attemptMotionOld(
			int dx,
			int dy,
			int dz,
			boolean stopOnCollision)
	{
		if (this.x() + dx < 0) dx = -this.x();
		if (this.y() + dy < 0) dy = -this.y();
		if (this.x() + dx > ppf.levelWidth() - 1)
			dx = (ppf.levelWidth() - 1) - this.x();
		if (this.y() + dy > ppf.levelHeight() - 1)
			dy = (ppf.levelHeight() - 1) - this.y();

		this.move(dx, dy, dz);
	}
	
	// The 'attemptMotion' method. Uses a collision table to
	//   handle collision response between various entities.
	// FIXME: The bit-shifting with <<2 and >>2 creates a slight
	//   amount of inaccuracy in which certain distances are
	//   judged equal when one is actually closer. This sometimes
	//   results in the player's stopping while sliding. If desired
	//   (as this only seems to happen with the 'crazy' bounding
	//   polygon), adjust the algorithm so that the measure of
	//   distance is perfectly accurate.
	public void attemptMotion(
	        int dx,
	        int dy,
	        int dz,
	        boolean stopOnCollision,
	        boolean TEST_PATH)
	{
	    // For testing. When TEST_PATH is true, an outline of where this
	    //   entity would travel is drawn instead of actually moving the
	    //   entity. (Not currently in use.)
	    // boolean TEST_PATH = false;
	    boolean VERBOSE = (this instanceof Fireball);
	    VERBOSE = (this instanceof Player);
	    VERBOSE = false;
	    
	    if (VERBOSE) System.out.println("\n** Beginning method call Entity.attemptMotion() with "
	            + "this = " + this.getClass() + "\n"
	            + "  This object's starting position: (" + this.x() + ", " + this.y() + ")");
	    
	    if (VERBOSE) {
	        System.out.println("  This entity's bounding polygon vertices: ");
	        for (int i = 0; i < this.getBoundingPoly().getNumVertices(); ++i) {
	            System.out.print("(" + this.getBoundingPoly().getVertex(i).x + ", "
	                    + this.getBoundingPoly().getVertex(i).y + ")  ");
	        }
	    }
	    
	    // Begin by getting this moving entity's polygon of motion.
	    Polygon pom = this.getPolyOfMotion(dx, dy);
	    
	    // Define arrays that will hold information about the
	    //   entities we might collide with, such as the position of
	    //   their obstructing boundary lines and their current state.
	    int MAX_LINES = 200;
	    Entity[] ent = new Entity[MAX_LINES];
	    int[] state = new int[MAX_LINES];
        Entity[] collideeEntity = new Entity[MAX_LINES];
	    int[] collideeState = new int[MAX_LINES];
        int[] collType = new int[MAX_LINES];
	    int[] linex1 = new int[MAX_LINES];
	    int[] liney1 = new int[MAX_LINES];
	    int[] linex2 = new int[MAX_LINES];
	    int[] liney2 = new int[MAX_LINES];
	    int[] lcx1 = new int[MAX_LINES];
	    int[] lcy1 = new int[MAX_LINES];
	    int[] lcx2 = new int[MAX_LINES];
	    int[] lcy2 = new int[MAX_LINES];
	    int[] collnx = new int[MAX_LINES];
	    int[] collny = new int[MAX_LINES];
	    int linecount = 0;
	    
	    // Initialize variables to hold information about the
	    //   distance to the nearest collision.
	    int colldist = 536870911;
	    //Fraction colldist = new Fraction(536870911, 1);
	    int colldx = dx;
	    int colldy = dy;
	    int colldz = dz;
	    int destpocx = 0;
	    int destpocy = 0;
	    
	    // For vertex-to-vertex collisions.
        int collvx = 0;
        int collvy = 0;
	    int collvnum = 0;
	    int vvcount = 0;
	    
	    // Flags for collision type.
	    final int NO_COLLISION = 0;
	    final int VERT_LINE = 1;
	    final int LINE_VERT = 2;
	    final int VERT_VERT_1 = 3;
	    final int VERT_VERT_2 = 4;
	    final int Z_OBST = 5;
	    final int HIT_GROUND = 6;
	    
	    // Define three ArrayLists to hold obstacles overlapping the
	    //   grid selection created by the polygon of motion (and thus
	    //   near, but not necessarily overlapping, the polygon of motion).
	    ArrayList<Entity> nearbyEntities = new ArrayList<Entity>();
	    ArrayList<TexturedLine> nearbyWalls = new ArrayList<TexturedLine>();
	    ArrayList<Portal> nearbyWaterPortals = new ArrayList<Portal>();

	    if (dx*dx + dy*dy + dz*dz != 0 && VERBOSE) System.out.println("");
	    
	    // Get the obstacles overlapping the grid selection.
	    boolean test = false;
	    if (this instanceof Player) test = true;
	    ppf.getObstacles(pom, nearbyWalls, nearbyEntities, nearbyWaterPortals, test);
	    
	    // Don't count this moving object itself as an obstacle.
	    nearbyEntities.remove(this);
	    
    	// Now all the potential obstacles are in three separate arrays. Our next
	    //   task is to determine which of these lines actually obstruct the
	    //   polygon of motion and add these to a "lines of obstruction" list.
	    
	    // First and foremost, get the distance to a ground collision, if a
	    //   ground collision is possible. Start with this as default.
	    if (this.z() + dz < 0) {
	        colldz = -this.z();
	        colldx = (dx*colldz) / dz;
	        colldy = (dy*colldz) / dz;
	        colldist = colldx*colldx + colldy*colldy + colldz*colldz;
	        collType[0] = HIT_GROUND;
	    }
	    else collType[0] = NO_COLLISION;
	    
	    // Check each entity, also doing Z-obstruction tests for these.
	    for (int i = 0; i < nearbyEntities.size(); ++i) {
	        
	        // Get the next entity.
	        Entity E = nearbyEntities.get(i);
	        
	        // Don't count the entity if this entity should 'PASS' it.
	        if ((   CollisionTable.flags[this.getState()][E.getState()]
	              & CollisionTable.CT_PASS ) != 0)
	        {
	            continue;
	        }
	        
	        // If the polygon of motion overlaps the entity's bounding polygon...
	        if (pom.overlaps(E.getBoundingPoly())) {
	            
	            // Check if z-values make a z-collision possible. If so, get
	            //   a test polygon located at the point of a possible z-collision
	            //   (scaled by a factor of 'dz'). 'ztestdz' remains unscaled.
	            Polygon ztestpoly = null;
	            int ztestdx = 0;
	            int ztestdy = 0;
	            int ztestdz = 0;
	            if (    (this.z() + this.getHeight() <= E.z())
	                 && (this.z() + this.getHeight() + dz > E.z()) )
	            {
                    ztestdz = E.z() - this.z() - this.getHeight();
	                ztestdx = ztestdz*dx;
	                ztestdy = ztestdz*dy;
	                ztestpoly = new Polygon(this.getBoundingPoly());
	            }
	            else if (    (this.z() >= E.z() + E.getHeight())
	                      && (this.z() + dz < E.z() + E.getHeight()) )
	            {
	                ztestdz = E.z() + E.getHeight() - this.z();
	                ztestdx = ztestdz*dx;
	                ztestdy = ztestdz*dy;
	                ztestpoly = new Polygon(this.getBoundingPoly());
	            }
	            if (ztestpoly != null) {
    	            for (int j = 0; j < this.getBoundingPoly().getNumVertices(); ++j) {
    	                int scaledx = (ztestpoly.getVertex(j).x)*dz + ztestdx;
    	                int scaledy = (ztestpoly.getVertex(j).y)*dz + ztestdy;
    	                ztestpoly.setVertex(j, scaledx, scaledy);
    	            }
	            }
	            
	            // Now we check to see if the moved test polygon overlaps the
	            //   (similarly scaled) bounding polygon of the obstacle entity.
	            //   If so, this will indicate a z-obstruction.

                // Get the scaled bounding polygon of the obstacle entity.
                Polygon scaledBoundingPoly = new Polygon();
	            if (ztestpoly != null) {
	                for (int j = 0; j < E.getBoundingPoly().getNumVertices(); ++j) {
	                    int scaledx = (E.getBoundingPoly().getVertex(j).x)*dz;
	                    int scaledy = (E.getBoundingPoly().getVertex(j).y)*dz;
	                    scaledBoundingPoly.addVertex(new Fieldpoint(scaledx, scaledy));
	                }
	            }
	                
                // If the moved polygon overlaps with the entity's bounding polygon
                //   and the obstruction distance is closer than the current,
                //   then update information about the next obstruction.
                if (ztestpoly != null && ztestpoly.overlaps(scaledBoundingPoly)) {
                    
                    // Scale back down and get the square distance to the z-obstruction.
                    ztestdx /= dz;
                    ztestdy /= dz;
                    int zcolldist = ztestdx*ztestdx + ztestdy*ztestdy + ztestdz*ztestdz;
                    
                    // If this obstruction is closer than the previous...
                    if (zcolldist < colldist) {
                        
                        // Update obstacle information.
                        colldist = zcolldist;
                        colldx = ztestdx;
                        colldy = ztestdy;
                        colldz = ztestdz;
                        collideeState[0] = E.getState();
                        collType[0] = Z_OBST;
                        collideeEntity[0] = E;
                        
                    } // end if (z-obstruction is closer)
                }
	            
	            // Otherwise, if a normal side-to-side collision is possible,
                //   add lines of obstruction to the ongoing list.
                //   Note: this does not guarantee a collision with the line.
                //   Later in this method, we will do a proximity test
                //   to see if side-to-side collision can occur.
                else if (    !(    (this.z() >= E.z() + E.getHeight())
                                && (this.z() + dz >= E.z() + E.getHeight()) )
                          && !(    (this.z() + this.getHeight() <= E.z())
                                && (this.z() + this.getHeight() + dz <= E.z()) ) )
                {
                    // Get the number of vertices of the entity.
                    int N = E.getBoundingPoly().getNumVertices();
                    
                    // Obtain information about each line.
                    for (int j = 0; j < N; ++j) {
                        
                        // Get the next bounding line of the entity.
                        linex1[linecount] = E.getBoundingPoly().getVertex(j).x;
                        liney1[linecount] = E.getBoundingPoly().getVertex(j).y;
                        linex2[linecount] = E.getBoundingPoly().getVertex((j + 1) % N).x;
                        liney2[linecount] = E.getBoundingPoly().getVertex((j + 1) % N).y;
                        
                        // Accept it as a line of obstruction only if it
                        //   overlaps with the polygon of motion.
                        LineSeg line = new LineSeg(linex1[linecount],
                                                   liney1[linecount],
                                                   linex2[linecount],
                                                   liney2[linecount]);
                        if (pom.overlaps(line)) {
                            ent[linecount] = E;
                            state[linecount] = E.getState();
                            ++linecount;
                            
                        } // end if (line overlaps polygon of motion)
                    } // end for (by vertex of entity's bounding polygon)
                } // end if (by possible collision)
	        } // end if (entity overlaps with polygon of motion)
	    } // end for (by nearby entity)
	    
	    // Add overlapping lines and closed water portals to the obstruction list.
	    //   Don't count water if this entity can 'PASS' it.
	    for (int i = 0; i < 2; ++i) {
	        if ((   CollisionTable.flags[this.getState()][Entity.ST_WATER]
	              & CollisionTable.CT_PASS ) != 0 && i == 1)
	        {
	            continue;
	        }
	        int N = (i == 0) ? nearbyWalls.size() : nearbyWaterPortals.size();
	        for (int j = 0; j < N; ++j) {
	            LineSeg L = (i == 0)
	                      ? nearbyWalls.get(j)
	                      : nearbyWaterPortals.get(j).getSegment();
	            if (pom.overlaps(L) && (i == 0 || !nearbyWaterPortals.get(j).isOpen())) {
    	            linex1[linecount] = L.x1();
    	            liney1[linecount] = L.y1();
    	            linex2[linecount] = L.x2();
    	            liney2[linecount] = L.y2();
    	            ent[linecount] = null;
    	            state[linecount] = (i == 0) ? ST_WALL : ST_WATER;
    	            ++linecount;
    	            
	            } // end if (line of obstruction found)
	        } // end for (by nearby wall or water portal)
	    } // end for (by line type)
	    
	    // Now all the lines of obstruction are in one big list. Our next task
	    //   is to do geometric calculations to find out which one of these lines
	    //   the moving entity will collide with first, together with what
	    //   type of collision it is and the distance toward it.
	    
	    // For each line of obstruction...
	    for (int i = 0; i < linecount; ++i) {
	        
	        // Get a reversed copy of the vector of motion and attach
	        //   it to both endpoints of the line of obstruction.
	        int v1revx1 = linex1[i];
	        int v1revy1 = liney1[i];
	        int v1revx2 = linex1[i] - dx;
	        int v1revy2 = liney1[i] - dy;
	        int v2revx1 = linex2[i];
	        int v2revy1 = liney2[i];
	        int v2revx2 = linex2[i] - dx;
	        int v2revy2 = liney2[i] - dy;
	        
	        // For each line of this entity's bounding polygon...
	        int N = this.getBoundingPoly().getNumVertices();
	        for (int j = 0; j < N; ++j) {
	            
	            // Get the line's endpoints.
	            int lx1 = this.getBoundingPoly().getVertex(j).x;
	            int ly1 = this.getBoundingPoly().getVertex(j).y;
	            int lx2 = this.getBoundingPoly().getVertex((j + 1) % N).x;
	            int ly2 = this.getBoundingPoly().getVertex((j + 1) % N).y;
	            
	            // Check for an intersection of the reversed vector of
	            //   attempted motion with this line. Do so by checking
	            //   both endpoints of the line of obstruction.
	            for (int k = 0; k < 2; ++k) {
	                
	                // Get the vector for this endpoint.
	                int vx1 = (k == 0) ? v1revx1 : v2revx1;
	                int vy1 = (k == 0) ? v1revy1 : v2revy1;
	                int vx2 = (k == 0) ? v1revx2 : v2revx2;
	                int vy2 = (k == 0) ? v1revy2 : v2revy2;
	                
	                // Get the point of contact (here, located on this entity's
	                //   bounding polygon), if any. Use "snap division" (remainder-
	                //   flagged division). 
	                if (VERBOSE) System.out.println("Testing for LINE_VERT and VERT_VERT with reversed vector "
	                        + "(" + vx1 + ", " + vy1 + ")-(" + vx2 + ", " + vy2 + ") and "
	                        + "bounding line (" + lx1 + ", " + ly1 + ")-(" + lx2 + ", " + ly2 + ")");
	                Fieldpoint poc = LineSeg.getIntersection(lx1, ly1, lx2, ly2,
	                                                         vx1, vy1, vx2, vy2,
	                                                         false, true);
	                
	                //if (poc == null && VERBOSE)
	                //    System.out.println("Testing for LINE_VERT/VERT_VERT: 'poc' found to be null");
	                
	                // If an intersection is found...
	                if (poc != null) {
	                    
	                    // Get the distance to the intersection (2-scaled obstdx/obstdy/
	                    //   obstdz and 4-scaled obstdist).
	                    int obstdx = (2*vx1) - poc.x;
	                    int obstdy = (2*vy1) - poc.y;
	                    int obstdz = (dx != 0)
	                               ? (obstdx*dz) / dx
	                               : (obstdy*dz) / dy;
	                    int obstdist = obstdx*obstdx + obstdy*obstdy + obstdz*obstdz;
	                    
	                    // If less than or equal to the current distance...
	                    if (obstdist <= (colldist<<2)) {
	                    
	                        // Check and see if the possible collision is with
	                        //   an entity. If so, do a proximity test to see if
	                        //   a side-to-side (or corner-touching) collision
	                        //   would actually happen.
	                        if (ent[i] != null) {
	                    
	                            if (VERBOSE) System.out.println("Testing for LINE_VERT/VERT_VERT: "
	                                    + "now testing for proximity with entity");
	                            
	                            // OPTIMIZE: Include some culling tests here
	                            //   to eliminate the big scaling algorithm in
	                            //   most cases.
	                            
	                            if (!isProximate(lx1, ly1, lx2, ly2,
	                                    vx1, vy1, vx2, vy2,
	                                    dx, dy, dz,
	                                    this.z(), this.getHeight(),
	                                    ent[i].z(), ent[i].getHeight()))
	                            {
	                                continue;
	                            }
                                
	                        } // end if (obstacle is an entity)

	                        // Line is truly an obstacle. Proceed to obtain data for
	                        //   collision response.
	                        
	                        // If the intersection is with one of this entity's vertices,
	                        //   it is a vertex-to-vertex obstruction.
	                        if (    (    (poc.x == 2*lx1 && poc.y == 2*ly1)
	                                  || (poc.x == 2*lx2 && poc.y == 2*ly2) )
	                             && (    (poc.x == 2*collvx && poc.y == 2*collvy)
	                                  || (obstdist < (colldist<<2)) ) )
	                        {
	                            // If it's with the same vertex as previous obstructions,
	                            //   add one to the number of lines involved in the
	                            //   vertex-to-vertex obstruction.
	                            if (    (poc.x == 2*collvx && poc.y == 2*collvy)
	                                 && (    collType[0] == VERT_VERT_1
	                                      || collType[0] == VERT_VERT_2 ) )
	                            {
	                                ++vvcount;
	                            }
	                            
	                            // Otherwise, initialize the vertex information for a
	                            //   different vertex (of shorter collision distance).
	                            else {
	                                vvcount = 1;
	                                collvx = (poc.x)/2;
	                                collvy = (poc.y)/2;
	                                collvnum = (lx1 == collvx && ly1 == collvy) ? j : (j + 1) % N;
	                            }
	                            
	                            // Get information about the line of obstruction whose
	                            //   vertex is involved (there will usually be at least
	                            //   two such lines).
                                if (VERBOSE) System.out.println("VERT_VERT found closer: (old<<2) = "
                                        + (colldist<<2) + ", new = " + obstdist);
	                            colldist = (obstdist>>2);
	                            if (obstdx % 2 == 1) obstdx -= DragoStatics.sign(obstdx);
	                            if (obstdy % 2 == 1) obstdy -= DragoStatics.sign(obstdy);
                                colldx = obstdx/2;
                                colldy = obstdy/2;
                                colldz = obstdz / 2;
	                            collideeState[vvcount - 1] = state[i];
	                            collideeEntity[vvcount - 1] = ent[i];
	                            lcx1[vvcount - 1] = linex1[i];
	                            lcy1[vvcount - 1] = liney1[i];
	                            lcx2[vvcount - 1] = linex2[i];
	                            lcy2[vvcount - 1] = liney2[i];
	                            collnx[vvcount - 1] = liney2[i] - liney1[i];
	                            collny[vvcount - 1] = linex1[i] - linex2[i];
	                            collType[vvcount - 1] = (k == 0) ? VERT_VERT_1 : VERT_VERT_2;

	                        }
	                        
	                        // Otherwise, if not a vertex-to-vertex obstruction and is
	                        //   closer, then it is a line-to-vertex obstruction. We can also
	                        //   record the destination's point of contact here as it is
	                        //   not affected by rasterized rollback.
	                        else if (    !(poc.x == 2*lx1 && poc.y == 2*ly1)
	                                  && !(poc.x == 2*lx2 && poc.y == 2*ly2)
	                                  && (obstdist < (colldist<<2)) )
	                        {
	                            if (VERBOSE) System.out.println("LINE_VERT found closer: (old<<2) = "
	                                    + (colldist<<2) + ", new = " + obstdist);
	                            colldist = (obstdist>>2);
                                if (obstdx % 2 == 1) obstdx -= DragoStatics.sign(obstdx);
                                if (obstdy % 2 == 1) obstdy -= DragoStatics.sign(obstdy);
                                colldx = obstdx/2;
                                colldy = obstdy/2;
                                colldz = obstdz / 2;
                                destpocx = vx1;
                                destpocy = vy1;
                                collideeState[0] = state[i];
                                collideeEntity[0] = ent[i];
                                lcx1[0] = lx1;
                                lcy1[0] = ly1;
                                lcx2[0] = lx2;
                                lcy2[0] = ly2;
                                collnx[0] = ly2 - ly1;
                                collny[0] = lx1 - lx2;
                                collType[0] = LINE_VERT;
                                
	                        } // end if (is obstruction VERT_VERT or LINE_VERT?)
	                    } // end if (is collision distance equal or closer?)
	                } // end if (point of contact found)
	            } // end for (by both endpoints of given obstacle's line)
	            
	            // Now attach the original vector of attempted motion to this
	            //   vertex of the moving polygon and check for a VERT_LINE obstruction.
	            int vx1 = this.getBoundingPoly().getVertex(j).x;
	            int vy1 = this.getBoundingPoly().getVertex(j).y;
	            int vx2 = vx1 + dx;
	            int vy2 = vy1 + dy;
	            
	            // Check to see if there is a point of contact with the
	            //   obstacle's line. (Here, the point of contact is on the
	            //   line of the obstacle rather than this entity.)
	            if (VERBOSE) System.out.println("Testing for VERT_LINE with obstacle "
	                    + "(" + linex1[i] + ", " + liney1[i] + ")-(" + linex2[i] + ", " + liney2[i] + ") and "
	                    + "vector (" + vx1 + ", " + vy1 + ")-(" + vx2 + ", " + vy2 + "), entity state = "
	                    + state[i]);
	            Fieldpoint poc = LineSeg.getIntersection(
	                    linex1[i], liney1[i], linex2[i], liney2[i],
	                    vx1, vy1, vx2, vy2, false, true);
	            
	            // If point of contact is found and is not a vertex-to-vertex obstruction
	            //   (as that should turn up with the reversed vectors anyway), then
	            //   obtain distance information.
	            if (    (poc != null)
	                 && !(    (poc.x == 2*linex1[i] && poc.y == 2*liney1[i])
	                       || (poc.x == 2*linex2[i] && poc.y == 2*liney2[i]) ) )
	            {
	                
	                // Get distance information (again, 2-scaled and 4-scaled).
	                int obstdx = poc.x - (2*vx1);
	                int obstdy = poc.y - (2*vy1);
	                int obstdz = (dx != 0)
	                           ? (obstdx*dz) / dx
	                           : (obstdy*dz) / dy;
	                int obstdist = obstdx*obstdx + obstdy*obstdy + obstdz*obstdz;
	                
	                if (VERBOSE) System.out.println("Testing: value of 'obstdist' found to be "
	                        + obstdist + ", to be compared with 'colldist' = " + colldist);
	                
	                // If distance to obstruction is closer than current, then
	                //   get information about the line.
	                if (obstdist < (colldist<<2)) {

	                    // Again, if the obstacle is an entity, make sure that a side-to-side
	                    //   collision would occur. If not, skip gathering information about
	                    //   this obstacle line.
	                    if (ent[i] != null) {
                            if (!isProximate(lx1, ly1, lx2, ly2,
                                    vx1, vy1, vx2, vy2,
                                    dx, dy, dz,
                                    this.z(), this.getHeight(),
                                    ent[i].z(), ent[i].getHeight()))
                            {
                                continue;
                            }
                        }
	                    
	                    // Side-to-side collision possibility confirmed; get line information.
	                    //   Also get vertex number of this entity's bounding polygon that is
	                    //   involved in the collision.
                        if (VERBOSE) System.out.println("VERT_LINE found closer: (old<<2) = "
                                + (colldist<<2) + ", new = " + obstdist);
	                    colldist = (obstdist>>2);
                        if (obstdx % 2 == 1) obstdx -= DragoStatics.sign(obstdx);
                        if (obstdy % 2 == 1) obstdy -= DragoStatics.sign(obstdy);
                        colldx = obstdx/2;
                        colldy = obstdy/2;
                        colldz = obstdz / 2;
                        collvnum = j;
                        collideeState[0] = state[i];
                        collideeEntity[0] = ent[i];
                        lcx1[0] = linex1[i];
                        lcy1[0] = liney1[i];
                        lcx2[0] = linex2[i];
                        lcy2[0] = liney2[i];
                        collnx[0] = lcy2[0] - lcy1[0];
                        collny[0] = lcx1[0] - lcx2[0];
                        collType[0] = VERT_LINE;
                        
	                } // end if (VERT_LINE is closer)
	            } // end if (VERT_LINE obstruction found)
	        } // end for (by vertex of this entity's bounding polygon)
	    } // end for (by line of obstruction)
	    
	    // Now the closest point of collision has been found, together with
	    //   the type of collision (LINE_VERT, VERT_LINE, or VERT_VERT), the
	    //   entity(s) involved and their states, and information about
	    //   the line of collision.
	    
	    // If no collision has been found, we just move in that direction
	    //   and we are done.
	    if (collType[0] == NO_COLLISION) {
	        
	        if (TEST_PATH) {
	            pathTestPoly[pathTestPolyCount] = this.getPolyOfMotion(dx, dy);
	            ++pathTestPolyCount;
	        }
	        else this.move(dx, dy, dz);
	        
	        return;
	    }
	    
	    // Otherwise, move up to the point of collision, doing a rasterized
	    //   rollback if necessary to ensure that this entity does not move
	    //   into a collided state (as may occasionally happen due to floor
	    //   division inaccuracy).
	    Polygon rollpoly = new Polygon(this.getBoundingPoly());
	    while (true) {
	        
	        // If distance to collision has reached (or is already) zero,
	        //   no further rollback can be accomplished -- stay in one place.
	        if (colldx == 0 && colldy == 0 && colldz == 0)
	            break;
	        
	        // Get the adjusted bounding polygon in the current rollback position.
	        for (int i = 0; i < rollpoly.getNumVertices(); ++i) {
	            int newx = this.getBoundingPoly().getVertex(i).x + colldx;
	            int newy = this.getBoundingPoly().getVertex(i).y + colldy;
	            rollpoly.setVertex(i, newx, newy);
	        }
	        
	        // Get any nearby obstacles in the rollback area.
	        nearbyEntities.clear();
	        nearbyWalls.clear();
	        nearbyWaterPortals.clear();
	        ppf.getObstacles(rollpoly, nearbyWalls, nearbyEntities, nearbyWaterPortals, test);
	        nearbyEntities.remove(this);
	        
	        // Check for a collided state.
	        boolean collision = false;
	        for (int j = 0; j < nearbyWalls.size(); ++j) {
	            if (rollpoly.overlaps(nearbyWalls.get(j))) {
	                collision = true;
	                break;
	            }
	        }
	        if (!collision) {
	            if ((   CollisionTable.flags[this.getState()][Entity.ST_WATER]
	                  & CollisionTable.CT_PASS) == 0)
	            {
        	        for (int j = 0; j < nearbyWaterPortals.size(); ++j) {
        	            if (rollpoly.overlaps(nearbyWaterPortals.get(j).getSegment())) {
        	                collision = true;
        	                break;
        	            }
        	        }
	            }
	        }
	        if (!collision) {
    	        for (int j = 0; j < nearbyEntities.size(); ++j) {
    	            Entity E = nearbyEntities.get(j);
    	            if (    rollpoly.overlaps(E.getBoundingPoly())
    	                 && this.z() + colldz < E.z() + E.getHeight()
    	                 && this.z() + this.getHeight() + colldz > E.z()
    	                 && ((   CollisionTable.flags[this.getState()][E.getState()]
    	                       & CollisionTable.CT_PASS) == 0) )
    	            {
    	                collision = true;
    	                break;
    	            }
    	        }
	        }
	        
	        // If we are in a collided state, move back one fieldpoint.
	        if (collision) {
	            
	            // Choose the most conservative move based on absolute values
	            //   of dx, dy, dz.
	            if (    Math.abs(dx) >= Math.abs(dy)
	                 && Math.abs(dx) >= Math.abs(dz)
	                 && dx != 0 )
	            {
	                colldx -= DragoStatics.sign(dx);
	                colldy = (colldx*dy) / dx;
	                colldz = (colldx*dz) / dx;
	            }
	            else if (    Math.abs(dy) >= Math.abs(dx)
	                      && Math.abs(dy) >= Math.abs(dz)
	                      && dy != 0 )
	            {
	                colldy -= DragoStatics.sign(dy);
	                colldx = (colldy*dx) / dy;
	                colldz = (colldy*dz) / dy;
	            }
	            else if (dz != 0)
	            {
	                colldz -= DragoStatics.sign(dz);
	                colldx = (colldz*dx) / dz;
	                colldy = (colldz*dy) / dz;
	            }
	            
	        } // end if (rollback was in collided state)
	        
	        // Otherwise, if we are not in a collided state (which should usually
	        //   be the case), we have found a good location to move to.
	        else break;
	        
	    } // end while (rasterized rollback)
	    
	    // If path-testing, draw the polygon of motion toward the
	    //   location of collision. Otherwise (as in playing), move
	    //   to the location.
        if (TEST_PATH) {
            pathTestPoly[pathTestPolyCount] = this.getPolyOfMotion(dx, dy);
            ++pathTestPolyCount;
        }
	    else this.move(colldx, colldy, colldz);
	        
	    // Stop here if requested by boolean argument.
	    if (stopOnCollision) return;
	    
	    // Get the destination point of contact for vertex-to-line or
	    //   vertex-to-vertex collisions.
	    if (collType[0] != LINE_VERT) {
	        destpocx = this.getBoundingPoly().getVertex(collvnum).x + colldx;
	        destpocy = this.getBoundingPoly().getVertex(collvnum).y + colldy;
	    }
	    
	    // Initialize the variable to hold the index of the "line of
	    //   collision response" (e.g., the line to slide against,
	    //   bounce off of, or take damage from), zero by default
	    //   and changed to an appropriate line for vertex-to-vertex
	    //   collisions.
	    int respl = 0;
	    
	    // If the collision type is VERT_VERT, we need the "leftmost" and
	    //   "rightmost" of the lines involved at this vertex.
	    // FIXME: It seems evident that the player should be able to slide
	    //   in a LINE_VERT fashion into hallways when an alternative
	    //   VERT_LINE slide is available. This should be allowed, for
	    //   instance, when a small opening in the wall the size of the
	    //   dragon leads into a concave room (as when EXPAND == 1 with
	    //   prototype playfield). Although the VERT_LINE slide along
	    //   the wall would with the current algorithm be chosen, the
	    //   player would want an easy way to get through with a LINE_VERT
	    //   slide (instead of having to find the exact y-position).
	    //   Fix the collision response algorithm to allow this.
	    if (collType[0] == VERT_VERT_1 || collType[0] == VERT_VERT_2) {
	        
	        // Get the neighboring vertices of this moving polygon to
	        //   the vertex of collision, along with their normals.
	        int N = this.getBoundingPoly().getNumVertices();
	        int collvleft = (collvnum + 1) % N;
	        int collvright = (collvnum + N - 1) % N;
	        int leftx = this.getBoundingPoly().getVertex(collvleft).x;
	        int lefty = this.getBoundingPoly().getVertex(collvleft).y;
	        int rightx = this.getBoundingPoly().getVertex(collvright).x;
	        int righty = this.getBoundingPoly().getVertex(collvright).y;
	        
	        // Initialize variables to hold indices of lines at the
	        //   front and back ends (with respect to lines' normals).
	        //   Also get the adjusted normal of the first line of
	        //   collision.
	        int fend = 0;
	        int bend = 0;
	        int fendnx = (collType[0] == VERT_VERT_1) ? collnx[0] : -collnx[0];
	        int fendny = (collType[0] == VERT_VERT_1) ? collny[0] : -collny[0];
	        int bendnx = (collType[0] == VERT_VERT_1) ? collnx[0] : -collnx[0];
	        int bendny = (collType[0] == VERT_VERT_1) ? collny[0] : -collny[0];
	        
            // The left and right neighbor segments of this entity's bounding
            //   polygon create two overlapping regions of space in front of
	        //   the entity -- the left and the right region. Determine whether
	        //   the 'fend' and 'bend' lines lie in either of these regions.
	        boolean fendleft = ((leftx - collvx)*fendnx + (lefty - collvy)*fendny < 0);
	        boolean fendright = ((rightx - collvx)*fendnx + (righty - collvy)*fendny > 0);
	        boolean fendmiddle = (fendleft && fendright);
	        boolean bendleft = ((leftx - collvx)*bendnx + (lefty - collvy)*bendny < 0);
	        boolean bendright = ((rightx - collvx)*bendnx + (righty - collvy)*bendny > 0);
	        boolean bendmiddle = (bendleft && bendright);

	        // Use dot products to get 'fend' and 'bend' in the right place.
	        for (int i = 1; i < vvcount; ++i) {
	            
	            // Get the adjusted vertices for this line of collision.
	            int x1 = (collType[i] == VERT_VERT_1) ? lcx1[i] : lcx2[i];
	            int y1 = (collType[i] == VERT_VERT_1) ? lcy1[i] : lcy2[i];
	            int x2 = (collType[i] == VERT_VERT_1) ? lcx2[i] : lcx1[i];
	            int y2 = (collType[i] == VERT_VERT_1) ? lcy2[i] : lcy1[i];
	            
	            // Get the adjusted normal for this line of collision.
	            int nx = (collType[i] == VERT_VERT_1) ? collnx[i] : -collnx[i];
	            int ny = (collType[i] == VERT_VERT_1) ? collny[i] : -collny[i];
	            
	            // Get the region(s) this line lies in.
	            boolean left = ((leftx - collvx)*nx + (lefty - collvy)*ny <= 0);
	            boolean right = ((rightx - collvx)*nx + (righty - collvy)*ny >= 0);
	            boolean middle = (left && right);
	            
	            // If this line of collision is in front of 'fend', set
	            //   'fend' to this line.
	            if (    (fendleft && right && (middle != fendmiddle))
	                 || ((x2 - x1)*fendnx + (y2 - y1)*fendny > 0) )
	            {
	                fend = i;
	                fendnx = nx;
	                fendny = ny;
	                fendleft = left;
	                fendright = right;
	                fendmiddle = middle;
	            }
	            
	            // Otherwise, if this line of collision is behind 'bend',
	            //   set 'bend' to this line.
	            else if (    (bendright && left && (middle != bendmiddle))
	                      || ((x2 - x1)*bendnx + (y2 - y1)*bendny < 0) )
	            {
	                bend = i;
	                bendnx = nx;
	                bendny = ny;
	                bendleft = left;
	                bendright = right;
	                bendmiddle = middle;
	            }

	        } // end for (by each line involved in vertex-to-vertex collision)
	        
	        // FIXME: 'fend' and 'bend' do not seem to be working for the point
	        //   of contact (896, 1664) in the expanded playfield, traveling
	        //   up-left. The player stops where two walls meet. 'fend' is
	        //   calculated to be one of the vertical walls and 'bend' is
	        //   calculated correctly.
	        if (VERBOSE) System.out.println("'fend' calculated to be ("
	                + lcx1[fend] + ", " + lcy1[fend] + ")-("
	                + lcx2[fend] + ", " + lcy2[fend] + ")");
	        if (VERBOSE) System.out.println("'bend' calculated to be ("
                    + lcx1[bend] + ", " + lcy1[bend] + ")-("
                    + lcx2[bend] + ", " + lcy2[bend] + ")");
	        
	        // Now we have 'fend' and 'bend'. The next step is to determine whether
	        //   either of these can count as a sliding line. 
	        boolean fendslide = true;
	        boolean bendslide = true;
	        
	        // REFACTOR: Reconcile 'fend' and 'bend' into one variable, looping twice.
	        //
	        // If either neighboring line lies in the back of 'fend', we
	        //   cannot slide along 'fend'. We cannot slide against 'fend' either
	        //   if we are not moving at least partially in its direction.
	        int fendx1 = (collType[fend] == VERT_VERT_1) ? lcx1[fend] : lcx2[fend];
	        int fendy1 = (collType[fend] == VERT_VERT_1) ? lcy1[fend] : lcy2[fend];
	        int fendx2 = (collType[fend] == VERT_VERT_1) ? lcx2[fend] : lcx1[fend];
	        int fendy2 = (collType[fend] == VERT_VERT_1) ? lcy2[fend] : lcy1[fend];
	        int leftdot = (leftx - collvx)*collnx[fend] + (lefty - collvy)*collny[fend];
	        int rightdot = (rightx - collvx)*collnx[fend] + (righty - collvy)*collny[fend];
	        boolean leftBehindFend = (leftdot < 0);
	        boolean rightBehindFend = (rightdot < 0);
	        if (    leftBehindFend || rightBehindFend
	             || (fendx2 - fendx1)*dx + (fendy2 - fendy1)*dy <= 0 )
	        {
	            fendslide = false;
	        }
	        
	        // Same for 'bend'.
            int bendx1 = (collType[bend] == VERT_VERT_1) ? lcx1[bend] : lcx2[bend];
            int bendy1 = (collType[bend] == VERT_VERT_1) ? lcy1[bend] : lcy2[bend];
            int bendx2 = (collType[bend] == VERT_VERT_1) ? lcx2[bend] : lcx1[bend];
            int bendy2 = (collType[bend] == VERT_VERT_1) ? lcy2[bend] : lcy1[bend];
	        leftdot = (leftx - collvx)*collnx[bend] + (lefty - collvy)*collny[bend];
	        rightdot = (rightx - collvx)*collnx[bend] + (righty - collvy)*collny[bend];
	        boolean leftBehindBend = (leftdot < 0);
	        boolean rightBehindBend = (rightdot < 0);
	        if (    leftBehindBend || rightBehindBend
	             || (bendx2 - bendx1)*dx + (bendy2 - bendy1)*dy <= 0 )
	        {
	            bendslide = false;
	        }
	        
	        // If both 'fend' and 'bend' are acceptable sliding lines, use the direction
	        //   that this entity came from to determine which is more acceptable.
	        if (fendslide && bendslide) {
	            if (VERBOSE) System.out.println("Using came_from = ("
	                    + this.prevdx() + ", " + this.prevdy() + ")");
	            if ((fendx2 - fendx1)*this.prevdy() == (fendy2 - fendy1)*this.prevdx())
	                respl = bend;
	            else
	                respl = fend;
	        }
	        
	        // If neither 'fend' nor 'bend' is an acceptable sliding line, resort
	        //   to a LINE_VERT response (just use 'fend' here).
	        else if (!fendslide && !bendslide) {
	            
	            boolean leftslide = true;
	            boolean rightslide = true;
	            
	            // Determine whether lines 'fend' and 'bend' lie behind either of
	            //   the two neighboring lines of this entity's bounding polygon.
	            int leftnx = lefty - collvy;
	            int leftny = collvx - leftx;
	            int rightnx = righty - collvy;
	            int rightny = collvx - rightx;
	            int fenddot = (fendx2 - fendx1)*leftnx + (fendy2 - fendy1)*leftny;
	            int benddot = (bendx2 - bendx1)*leftnx + (bendy2 - bendy1)*leftny;
	            if (    fenddot < 0 || benddot < 0
	                 || (leftx - collvx)*dx + (lefty - collvy)*dy >= 0 )
	            {
	                leftslide = false;
	            }
	            fenddot = (fendx2 - fendx1)*rightnx + (fendy2 - fendy1)*rightny;
	            benddot = (bendx2 - bendx1)*rightnx + (bendy2 - bendy1)*rightny;
	            if (    fenddot < 0 || benddot < 0
	                 || (rightx - collvx)*dx + (righty - collvy)*dy >= 0 )
	            {
	                rightslide = false;
	            }
	            
	            // Determine which line of this entity's bounding polygon to use
	            //   as the sliding line.
	            collideeState[0] = collideeState[fend];
	            collideeEntity[0] = collideeEntity[fend];
	            destpocx = fendx1;
	            destpocy = fendy1;
	            if (leftslide) {
	                lcx1[0] = collvx;
	                lcy1[0] = collvy;
	                lcx2[0] = leftx;
	                lcy2[0] = lefty;
	            }
	            else {
	                lcx1[0] = rightx;
	                lcy1[0] = righty;
	                lcx2[0] = collvx;
	                lcy2[0] = collvy;
	            }
	            collnx[0] = lcy2[0] - lcy1[0];
	            collny[0] = lcx1[0] - lcx2[0];
	            collType[0] = LINE_VERT;
	            if (VERBOSE) System.out.println("Converted from VERT_VERT to LINE_VERT");
	            
	        }
	        
	        // Otherwise, set 'fend' or 'bend' to be the response line.
	        else if (fendslide) respl = fend;
	        else respl = bend;
	        
	    } // end if (get response line for vertex-to-vertex collision)
	    
	    // Get remaining vector of motion to use with collision response.
	    int remdx = dx - colldx;
	    int remdy = dy - colldy;
	    int remdz = dz - colldz;
	    
	    // Initialize the next vector of attempted motion in the recursive
	    //   call sequence (e.g., the slide vector or vector of reflection).
	    int nextdx = remdx;
	    int nextdy = remdy;
	    int nextdz = remdz;
	    
	    // Use collision table to get the flags for this type of collision.
	    int flags = CollisionTable.flags[this.getState()][collideeState[respl]];
	    
	    // Produce any effects that should result from a collision of this type. Many
	    //   of these are stubs; we will evolve them as we test further elements of the
	    //   game. We must keep a close eye on this as we add new entity states!

	    // Effects that are neither dynamical nor destructive.
	    if ((flags & CollisionTable.CT_BURN) != 0) {
	        collideeEntity[respl].setState(ST_HUMAN_BURNED);
	    }
	    if ((flags & CollisionTable.CT_GET_BURNED) != 0) {
	        this.setState(ST_HUMAN_BURNED);
	    }
	    if ((flags & CollisionTable.CT_GET_FROZEN) != 0) {
	        this.setState(ST_HUMAN_FROZEN);
	    }
	    
	    // Dynamical effects on collidee.
        if ((flags & CollisionTable.CT_NUDGE_TARGET) != 0) {} // Not implemented yet
	    if ((flags & CollisionTable.CT_REFLECT_TARGET) != 0) {} // Not implemented yet

	    // Destructive effects on collidee.
        if ((flags & CollisionTable.CT_DAMAGE) != 0) {
            int damage = CollisionTable.damageGiven[this.getState()][collideeState[respl]];
            collideeEntity[respl].addHealth(-damage);
            if (collideeEntity[respl].getHealth() == 0) {
                if (collideeEntity[respl] instanceof Player) {
                    collideeEntity[respl].setState(ST_PLAYER_DEAD);
                }
                else {
                    
                    // Replace enemy with an explosion if the enemy was slain.
                    int slainx = collideeEntity[respl].x();
                    int slainy = collideeEntity[respl].y();
                    collideeEntity[respl].disappear();
                    Explosion expl = new Explosion(
                            slainx,
                            slainy,
                            "UP",
                            this.ppf);
                    this.ppf.addEntity(expl);
                    this.ppf.addActiveEntity(expl);
                    
                    // Play an explosion sound effect.
                    DragoStatics.playSound("EXPLOSION_1.wav");
                    /*
                    try {
                        Clip clip = AudioSystem.getClip();
                        File sfx = new File("sfx\\EXPLOSION_1.wav");
                        AudioInputStream inputStream = AudioSystem.getAudioInputStream(sfx);
                        clip.open(inputStream);
                        clip.start();
                    }
                    catch (Exception e) {
                        System.out.println("Exception while playing sound!");
                        e.printStackTrace();
                        System.exit(1);
                    }
                    */

                }
            }
        }
        if ((flags & CollisionTable.CT_SHATTER) != 0) {
            collideeEntity[respl].setState(ST_HUMAN_SHATTERING);
        }
        if ((flags & CollisionTable.CT_TAILSTRIKE_TARGET) != 0) {
            int damage = CollisionTable.damageGiven[this.getState()][collideeState[respl]];
            collideeEntity[respl].addHealth(-damage);
            if (collideeEntity[respl].getHealth() == 0)
                collideeEntity[respl].setState(ST_HUMAN_DEAD); // Move all this to 'addHealth'?
            else
                collideeEntity[respl].setState(ST_HUMAN_TAILSTRUCK);
        }
	    if ((flags & CollisionTable.CT_DESTROY) != 0) {
	     
	        // For now, cause 'DESTROYED' trees to vanish in an explosion.
	        //   Will work more on specifics later.
	        if (collideeEntity[respl] instanceof Tree) {
                int destroyedx = collideeEntity[respl].x();
                int destroyedy = collideeEntity[respl].y();
                collideeEntity[respl].disappear();
                Explosion expl = new Explosion(
                        destroyedx,
                        destroyedy,
                        "UP",
                        this.ppf);
                this.ppf.addEntity(expl);
                this.ppf.addActiveEntity(expl);
                DragoStatics.playSound("EXPLOSION_1.wav");

	        } // end if (collidee is a tree)
	    } // end if (destroy flag)
	    
	    // The 'take' flag.
	    //
	    //   *Mike*: you may want to do something special here for the player, like
	    // add a stat bonus in a new attribute. Up to you. This currently just plays
	    // a sound and removes the item from the playfield.
	    //
	    if ((flags & CollisionTable.CT_TAKE) != 0) {
	        collideeEntity[respl].disappear();
	        DragoStatics.playSound("EGG_COLLECT_1.wav");
	    }
	    
	    // Dynamical effects on self.
	    if ((flags & CollisionTable.CT_REFLECT) != 0) {
	        
	        // Reflect the remaining vector of attempted motion so as to
	        //   attempt motion in that direction.
	        if (collType[respl] == VERT_VERT_1 || collType[respl] == VERT_VERT_2) {
	            
	            // For vertex-to-vertex reflections, simply bounce back in the
	            //   opposite direction. It could be made more sophisticated
	            //   than this, I suppose.
	            nextdx = -nextdx;
	            nextdy = -nextdy;
	            this.setdx(nextdx);
	            this.setdx(nextdy);
	            
	        }
	        else {
	            
	            // For line-to-vertex or vertex-to-line reflections, reflect
	            //   the remaining vector of attempted motion across the
	            //   line of collision response.
	            
	            // Attach normal of response line to the attempted destination
	            //   point to get a perpendicular "line of reflection" to the
	            //   reflected destination point.
	            int lrx1 = destpocx + remdx;
	            int lry1 = destpocy + remdy;
	            int lrx2 = lrx1 + collnx[respl];
	            int lry2 = lry1 + collny[respl];
	            
	            // Get remote intersection of this line of reflection with the
	            //   line of collision response.
	            Fieldpoint fp = LineSeg.getIntersection(
	                    lcx1[respl], lcy1[respl], lcx2[respl], lcy2[respl],
	                    lrx1, lry1, lrx2, lry2,
	                    true, false);
	            
	            // Get vector of attempted destination toward this point
	            //   and double its length.
	            int refx = 2*(fp.x - lrx1);
	            int refy = 2*(fp.y - lry1);
	            
	            // Get the reflected vector of attempted motion.
	            nextdx = remdx + refx;
	            nextdy = remdy + refy;
	        
	        } // end if (vertex-to-vertex or not)
	    } // end if (reflect flag)
        if (VERBOSE) System.out.println("Now testing for slide flag.");
	    if ((flags & CollisionTable.CT_SLIDE) != 0) {
	        
	        if (VERBOSE) System.out.println("Slide flag is true for this collision type.");
	        
	        if (collType[0] == LINE_VERT && VERBOSE) System.out.println("Collision is LINE_VERT");
	        else if (collType[0] == VERT_LINE && VERBOSE) System.out.println("Collision is VERT_LINE");
	        else if (VERBOSE) System.out.println("Collision is VERT_VERT");
	        
	        if (VERBOSE) System.out.println("Destination point of contact: (" + destpocx + ", " + destpocy + ")");
	        if (VERBOSE) System.out.println("Line of collision: (" + lcx1[respl] + ", " + lcy1[respl]
	                + ") - (" + lcx2[respl] + ", " + lcy2[respl] + ")");
	        if (VERBOSE) System.out.println("Remaining vector of motion: (" + remdx + ", " + remdy + ")");
	        
	        // Get the maximum-length slide vector toward
	        //   either vertex of the line of collision.
	        int maxsvx = 0;
	        int maxsvy = 0;
	        if (collType[0] == LINE_VERT) {
	            int dot = 0;
                if (lcx1[0] == destpocx && lcy1[0] == destpocy)
                    dot = -(remdx*(lcx2[0] - destpocx) + remdy*(lcy2[0] - destpocy));
                else
                    dot = remdx*(lcx1[0] - destpocx) + remdy*(lcy1[0] - destpocy);
                if (VERBOSE) System.out.println("LINE_VERT dot product: " + dot);
	            if (dot > 0) {
	                maxsvx = -(lcx2[0] - destpocx);
	                maxsvy = -(lcy2[0] - destpocy);
	                if (VERBOSE) System.out.println("Block 1");
	            }
	            else if (dot < 0) {
	                maxsvx = -(lcx1[0] - destpocx);
	                maxsvy = -(lcy1[0] - destpocy);
	                if (VERBOSE) System.out.println("Block 2");
	            }
	        }
	        else {
	            int dot = 0;
	            if (lcx1[respl] == destpocx && lcy1[respl] == destpocy)
	                dot = -(remdx*(lcx2[respl] - destpocx) + remdy*(lcy2[respl] - destpocy));
	            else
                    dot = remdx*(lcx1[respl] - destpocx) + remdy*(lcy1[respl] - destpocy);
	            if (VERBOSE) System.out.println("Non-LINE_VERT dot product: " + dot);
                if (collType[0] == VERT_VERT_1 || collType[0] == VERT_VERT_2)
                    if (VERBOSE) System.out.println("Collision is vert-vert");
	            if (dot > 0) {
	                maxsvx = lcx1[respl] - destpocx;
	                maxsvy = lcy1[respl] - destpocy;
	                if (VERBOSE) System.out.println("Block 3");
	            }
	            else if (dot < 0) {
	                maxsvx = lcx2[respl] - destpocx;
	                maxsvy = lcy2[respl] - destpocy;
	                if (VERBOSE) System.out.println("Block 4");
	            }
	        }
	        if (VERBOSE) System.out.println("SQRT: Calculating square magnitude of (" + maxsvx + ", " + maxsvy + ")");
            int maxlength = DragoStatics.sqrt(maxsvx*maxsvx + maxsvy*maxsvy);
            if (VERBOSE) System.out.println("Max slide vector: (" + maxsvx + ", " + maxsvy + ")");
            
	        // Project the remaining vector of attempted motion onto the
	        //   maximum-length slide vector to get the "preslide vector"
	        //   -- that is, the slide vector before it is trimmed to
	        //   the length of the wall segment that it exceeds (if it does).
	        int psvx = 0;
	        int psvy = 0;
	        if (maxsvx != 0 || maxsvy != 0) {
	            int dot = remdx*maxsvx + remdy*maxsvy;
	            if (VERBOSE) System.out.println("Dot product for preslide vector: " + dot);
	            if (VERBOSE) System.out.println("Max length of preslide vector: " + maxlength);
	            psvx = (maxsvx*dot) / (maxlength*maxlength);
	            psvy = (maxsvy*dot) / (maxlength*maxlength);
	        }
            int pslength = DragoStatics.sqrt(psvx*psvx + psvy*psvy);
            if (VERBOSE) System.out.println("Preslide vector: (" + psvx + ", " + psvy + ")");
	        
	        // Trim the preslide vector to the length of the wall to obtain
	        //   the actual slide vector. Then project the actual slide vector
	        //   back against the remaining vector of attempted motion to
	        //   obtain the portion of the vector of attempted motion that
	        //   yet remains after sliding.
            int oldremdx = remdx;
            int oldremdy = remdy;
            int oldremdz = remdz;
            int oldremlength = remdx*remdx + remdy*remdy + remdz*remdz;
	        nextdx = psvx;
	        nextdy = psvy;
	        if (pslength > maxlength) {
	            nextdx = maxsvx;
	            nextdy = maxsvy;
	            nextdz = (remdz*maxlength) / pslength;
	            remdx = (remdx*(pslength - maxlength)) / pslength;
	            remdy = (remdy*(pslength - maxlength)) / pslength;
	            remdz = (remdz*(pslength - maxlength)) / pslength;
	        }
	        else {
	            remdx = 0;
	            remdy = 0;
	            remdz = 0;
	        }
	        if (VERBOSE) System.out.println("Next motion vector: (" + nextdx + ", " + nextdy + ", " + nextdz + ")");
	        if (VERBOSE) System.out.println("Remaining after next vector: (" + remdx + ", " + remdy + ", " + remdz + ")");
	        
	        // Attempt motion in the direction of sliding, stopping at
	        //   collision and projecting what remains of the sliding vector
	        //   back into the remaining vector of attempted motion.
	        if (pslength > 0) {
    	        int startx = this.x();
    	        int starty = this.y();
    	        if (VERBOSE) System.out.println("Coordinates before slide: (" + this.x() + ", " + this.y() + ")");
    	        this.attemptMotion(nextdx, nextdy, nextdz, true, TEST_PATH);
    	        if (VERBOSE) System.out.println("Coordinates after slide: (" + this.x() + ", " + this.y() + ")");
    	        int deltax = this.x() - startx;
    	        int deltay = this.y() - starty;
    	        if (VERBOSE) System.out.println("Delta after slide: (" + deltax + ", " + deltay + ")");
    	        if (deltax != nextdx || deltay != nextdy) {
    	            int movedlength = DragoStatics.sqrt(deltax*deltax + deltay*deltay);
    	            remdx += (oldremdx*movedlength) / pslength;
    	            remdy += (oldremdy*movedlength) / pslength;
    	            remdz += (oldremdz*movedlength) / pslength;
    	        }
	        }
	        
	        if (VERBOSE) System.out.println("Remaining after slide: (" + remdx + ", " + remdy + ", " + remdz + ")");
	        
	        // If we have exhausted the remaining vector of attempted motion,
	        //   or if no further motion is possible, then we are done.
	        int remlength = remdx*remdx + remdy*remdy + remdz*remdz;
	        if (remlength == 0 || remlength >= oldremlength)
	            return;
	        
	        // Otherwise, attempt motion again using the remaining vector of
	        //   attempted motion. This is the point of recursion in the method.
	        this.attemptMotion(remdx, remdy, remdz, false, TEST_PATH);
	        
	    } // end if (slide flag)
	    
	    // Destructive effects on self.
	    if ((flags & CollisionTable.CT_DISAPPEAR) != 0) {
	        
	        // Disappearance often leads to a state transition (fireball
	        //   changes to an 'extinguished' state, represented by the
	        //   engine as a disappearing puff of fire, whereas slain
	        //   humans result in the creation of 'Explosions').
	        if (this instanceof Fireball) {
	            this.setdx(0);
	            this.setdy(0);
	            this.setdz(0);
	            this.setState(Entity.ST_FIREBALL_EXTINGUISHING);
	            int ds = 5;
	            if (this.getStateCounter() == 0) ds = 15;
	            else if (    (1 <= this.getStateCounter())
	                      && (this.getStateCounter() <= 4) )
	            {
	                ds = 10;
	            }
	            //this.addWhereFacing(-ds, false);
	            this.addWhereFacing(0, false);
	            this.setStateCounter(17);
	        }
	        else {
	        
	            // Default action is just to remove the object from the
	            //   playfield.
	            this.disappear();
	            
	        } // end if (what kind of entity?)
	    } // end if (disappear flag)

	} // end method attemptMotion
	    
	// Proximity test function, used in collision handling. I moved this
	//   to a separate function because it's a really detailed algorithm
	//   and it is called twice.
	private boolean isProximate(
	        int lx1, int ly1, int lx2, int ly2,
	        int vx1, int vy1, int vx2, int vy2,
	        int dx, int dy, int dz,
	        int z, int height,
	        int entz, int entheight)
	{
	    
        // Get perfectly-scaled point of contact.
        long a11 = (long)ly2 - (long)ly1;
        long a12 = (long)lx1 - (long)lx2;
        long a21 = (long)vy2 - (long)vy1;
        long a22 = (long)vx1 - (long)vx2;
        long b1 = (long)lx1*a11 + (long)ly1*a12;
        long b2 = (long)vx1*a21 + (long)vy1*a22;
        long scaledpocx = b1*a22 - b2*a12;
        long scaledpocy = b2*a11 - b1*a21;
        long scalefact = a11*a22 - a21*a12;
        if (scalefact < 0) {
            scaledpocx = -scaledpocx;
            scaledpocy = -scaledpocy;
            scalefact = -scalefact;
        }
        
        // Get the scaled z-value difference at the point
        //   of proximity, scaling further by 'dx' or 'dy'.
        long scaleddz = 0;
        if (dx != 0) {
            scaleddz = (scalefact*(long)vx1 - scaledpocx)*(long)dz;
            scaleddz *= DragoStatics.sign(dx);
            scalefact *= DragoStatics.sign(dx)*(long)dx;
        }
        else {
            scaleddz = (scalefact*(long)vy1 - scaledpocy)*(long)dz;
            scaleddz *= DragoStatics.sign(dy);
            scalefact *= DragoStatics.sign(dy)*(long)dy;
        }
        
        // Get scaled height and z-values of both entities.
        long scaledz =         scalefact*(long)z;
        long scaledheight =    scalefact*(long)height;
        long scaledentz =      scalefact*(long)entz;
        long scaledentheight = scalefact*(long)entheight;
        
        // Compare scaled 'z' and 'height' at point of contact
        //   to 'z' and 'height' of obstacle entity. If no contact
        //   is made, bypass this obstacle's vertex -- it is not
        //   a vertex of collision.
        long scaledpocz = scaledz + scaleddz;
        if (scaledpocz > scaledentz + scaledentheight) return false;
        else if (scaledpocz + scaledheight < scaledentz) return false;
        else if (    (    (scaledpocz == scaledentz + scaledentheight)
                       && (dz >= 0) )
                  || (    (scaledpocz + scaledheight == scaledentz)
                       && (dz <= 0) ) )
        {
            return false;
        }
        
        return true;
        
	} // end method isProximate
	        
	private void error(String function, String message) {
		System.out.println("Entity." + function + "(): " + message);
		System.exit(1);
	}
}

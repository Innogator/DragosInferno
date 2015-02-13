/*
 * MapGridSquare.java (v1.0)
 * 3/17/2013
 */
package playfield;

import java.util.ArrayList;

import entity.Entity;

/**
 * A square of the game grid, represented with four triangles
 * containing four textures and belonging to up to four leaves
 * of the binary space partition.
 * 
 * @author Quothmar
 *
 */
public class MapGridSquare {

    public static final int BOUND_UP = 1;         //  ----1----
    public static final int BOUND_RIGHT = 2;      //  |\     /|
    public static final int BOUND_DOWN = 4;       //  | 8   5 |
    public static final int BOUND_LEFT = 8;       //  4  \ /  2
    public static final int BOUND_UPRIGHT = 16;   //  |  / \  |
    public static final int BOUND_DOWNRIGHT = 32; //  | 7   6 |
    public static final int BOUND_DOWNLEFT = 64;  //  |/     \|
    public static final int BOUND_UPLEFT = 128;   //  ----3---- (As in documentation)
    
    public BSPLeaf leaves[];
	public Texture textures[];
	public ArrayList<TexturedLine> AILines;
	public ArrayList<Entity> entities;
	public ArrayList<Portal> waterPortals;

	public MapGridSquare() {
		AILines = new ArrayList<TexturedLine>();
		entities = new ArrayList<Entity>();
		waterPortals = new ArrayList<Portal>();
		textures = new Texture[4];
		leaves = new BSPLeaf[4];
	}

	public BSPLeaf getLeaf(int triangle) {
		return leaves[triangle];	// 'triangle' can be UPPER, RIGHT, LOWER, LEFT.
	}

	public Texture getTexture(int triangle) {
		return textures[triangle];
	}
	
	public void setLeaf(int side, BSPLeaf aLeaf) {
		leaves[side] = aLeaf;
	}

	public void setTexture(int side, Texture aTexture) {
		textures[side] = aTexture;
	}
	
	public void attach(Entity t) {
		if (!entities.contains(t))
		    entities.add(t);
	}
	
	public void attach(TexturedLine ailine) {
		if (!AILines.contains(ailine))
		    AILines.add(ailine);
	}
	
	public void attach(Portal waterPortal) {
		if (!waterPortals.contains(waterPortal))
		    waterPortals.add(waterPortal);
	}
	
	public int getNumEntities()	{
		return entities.size();
	}

	public int getNumAILines() {
		return AILines.size();
	}

	public int getNumWaterPortals()	{
		return waterPortals.size();
	}
	
	
}

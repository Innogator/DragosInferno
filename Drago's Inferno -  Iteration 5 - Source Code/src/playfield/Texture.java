/*
 * Texture.java (v1.0)
 * 3/17/2013
 */
package playfield;

/**
 * An enumeration of all the textures used in the game.
 * 
 * @author Quothmar/John Thrasher
 *
 */
public enum Texture {
	IRON_WALL, STONE_WALL, BRICK_WALL_RED, BRICK_WALL_WHITE, SAND_WALL, ROCK_WALL, STONES, FLAMMABLE_TREES,
	FIXED_TREES, BROWN_CAVE_WALL, GRASS, HIGH_GRASS, SAND, DIRT, LAVA, WATER, GRAY_ROCK_GROUND, BROWN_ROCK_GROUND,
	SPIKES, GRAY_DIRT, DUNGEON_TILE_1, GIMP_BRICK_1;

	// Three categories of textures.
    public static final int WALL_TYPE = 0;
    public static final int GROUND_TYPE = 1;
    public static final int TREE_STONE_TYPE = 2;

    public static Texture getByString(String s) {
        switch (s) {
        case "BROWN_CAVE_WALL": return BROWN_CAVE_WALL; 
        case "IRON_WALL": return IRON_WALL; 
        case "BRICK_WALL_WHITE": return BRICK_WALL_WHITE;    
        case "BRICK_WALL_RED": return BRICK_WALL_RED;  
        case "SAND_WALL": return SAND_WALL;           
        case "ROCK_WALL": return ROCK_WALL;           
        case "STONE_WALL": return STONE_WALL;
        case "DIRT": return DIRT;
        case "GRAY_DIRT": return GRAY_DIRT;
        case "SAND": return SAND;
        case "GRASS": return GRASS;
        case "HIGH_GRASS": return HIGH_GRASS;
        case "LAVA": return LAVA;
        case "WATER": return WATER;
        case "GRAY_ROCK_GROUND": return GRAY_ROCK_GROUND;
        case "BROWN_ROCK_GROUND": return BROWN_ROCK_GROUND;
        case "DUNGEON_TILE_1": return DUNGEON_TILE_1;
        case "FLAMMABLE_TREES": return FLAMMABLE_TREES;
        case "STONES": return STONES;
        case "GIMP_BRICK_1": return GIMP_BRICK_1;
        default: return GRASS;
        }
    } // end method getByString
    
    // Returns the category of the given texture: ground-based,
    //   wall, or tree/stone.
    public static int getCategory(Texture t) {
        switch (t) {
        case BROWN_CAVE_WALL: 
        case IRON_WALL:           
        case BRICK_WALL_WHITE:    
        case BRICK_WALL_RED:  
        case SAND_WALL:           
        case ROCK_WALL:           
        case STONE_WALL:
        case GIMP_BRICK_1:
            return WALL_TYPE;
        case DIRT:
        case GRAY_DIRT:
        case SAND:
        case GRASS:
        case HIGH_GRASS:
        case LAVA:
        case WATER:       // Note: water is classified as a 'ground' polygon here.
        case GRAY_ROCK_GROUND:
        case BROWN_ROCK_GROUND:
        case DUNGEON_TILE_1:
            return GROUND_TYPE;
        case FLAMMABLE_TREES:
        case STONES:
            return TREE_STONE_TYPE;
        default:
            return -1;
        }
    } // end method getCategory        

    public static int getCategory(String s) {
        return getCategory(getByString(s));
    }

    public static boolean isWall(Texture t) {
        return (getCategory(t) == WALL_TYPE);
    }

    
}

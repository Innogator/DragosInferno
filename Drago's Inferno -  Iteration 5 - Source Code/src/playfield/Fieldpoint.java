/*
 * Fieldpoint.java (v1.0)
 * 3/17/2013
 */
package playfield;

/**
 * A simple pair of integer coordinates representing
 * a point on the playfield.
 * 
 * @author Quothmar
 *
 */
public class Fieldpoint {
	public int x;
	public int y;

	public Fieldpoint() {
		x = 0;
		y = 0;
	}

	public Fieldpoint(int startX, int startY) {
		x = startX;
		y = startY;
	}
	
	public Fieldpoint(Fieldpoint fp) {
	    x = fp.x;
	    y = fp.y;
	}

}

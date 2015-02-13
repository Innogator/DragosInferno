/*
 * LineSeg.java (v1.0)
 * 3/17/2013
 */
package playfield;

import drago.DragoStatics;
import drago.Fraction;

import playfield.Fieldpoint;

/**
 * Originally called 'Line.java', but later changed. Represents
 * a line segment with two endpoints in integer coordinates.
 * 
 * @author Quothmar
 *
 */
public class LineSeg {

    // Starting and ending coordinates, as well as the line's
    //   geometric normal.
    public Fieldpoint startingFieldpoint;
	public Fieldpoint endingFieldpoint;
	public int nx;
	public int ny;
	
	// I decided these were really necessary. The long variable names were a mistake on my part.
	//   (x1, y1) -> (x2, y2)
	public int x1() { return startingFieldpoint.x; }
	public int y1() { return startingFieldpoint.y; }
	public int x2() { return endingFieldpoint.x; }
	public int y2() { return endingFieldpoint.y; }
	
	// Again, not sure where to put these flags... making them specific to the
	//   'LineSeg' class for now.
	public static final int FRONT = 0;
	public static final int BACK = 1;
	public static final int COLLINEAR = 2;
	public static final int SPANNING = 3;

	public LineSeg() {
		startingFieldpoint = new Fieldpoint();
		endingFieldpoint = new Fieldpoint();
		nx = 0;
		ny = 0;
	}

	// Creates a new line segment from two fieldpoints
	public LineSeg(Fieldpoint start, Fieldpoint end) { 
		startingFieldpoint = new Fieldpoint(start.x, start.y);
		endingFieldpoint = new Fieldpoint(end.x, end.y);
		nx = endingFieldpoint.y - startingFieldpoint.y;
		ny = startingFieldpoint.x - endingFieldpoint.x;
	}
	
	// Same, but with integer coordinates.
	//   (Should we do away with the Fieldpoint class? It seems
	//   a bit confusing...)
	public LineSeg(int x1, int y1, int x2, int y2) {
		startingFieldpoint = new Fieldpoint(x1, y1);
		endingFieldpoint = new Fieldpoint(x2, y2);
		nx = y2 - y1;
		ny = x1 - x2;
	}
	
	// Returns what side the specified pair of coordinates is on relative
	//   to the line (FRONT, BACK, or COLLINEAR).
	public int getSide(int x, int y) {
		
		// Use dot product. From geometry: A . B = |A||B|cos(theta). Thus
		//   it's positive for an acute angle and negative for an obtuse angle.
		int dotProduct = -((this.x1() - x)*nx + (this.y1() - y)*ny);
		if (dotProduct > 0)
			return FRONT;
		else if (dotProduct < 0) {
			return BACK;
		}
		else
			return COLLINEAR;
	}
	
	// Same test for line from (x1, y1) - (x2, y2). One additional flag
	//   'SPANNING' for when the passed line wholly crosses this line.
	// Note: An intersection that just touches the line is not considered
	//   'SPANNING'.
	public int getSide(int x1, int y1, int x2, int y2) {
		int dotProduct1 = -((this.x1() - x1)*nx + (this.y1() - y1)*ny);
		int dotProduct2 = -((this.x1() - x2)*nx + (this.y1() - y2)*ny);
		
		if (    (dotProduct1 < 0 && dotProduct2 < 0)
		     || (dotProduct1 > 0 && dotProduct2 > 0) )	// Same side
		{
			return (dotProduct1 > 0 ? FRONT : BACK);
		}
		else if (dotProduct1 == 0 || dotProduct2 == 0)
			if (dotProduct1 > 0 || dotProduct2 > 0)
				return FRONT;
			else if (dotProduct1 < 0 || dotProduct2 < 0)
				return BACK;
			else
				return COLLINEAR;
		else {
			return SPANNING;
		}
	}
	
	// Same test, for another line.
	public int getSide(LineSeg line)
	{
		return getSide(line.startingFieldpoint.x, line.startingFieldpoint.y,
						line.endingFieldpoint.x, line.endingFieldpoint.y);
	}
	
	// Get the intersection of two line segments, as given by integer coordinates.
	//   I put it in class LineSeg because that seemed reasonable, although this
	//   could be made into a global somehow.
	// First line segment: (x11, y11) -> (x12, y12)
	// Second line segment: (x21, y21) -> (x22, y22)
	// Returns 'null' if the segments don't intersect.
	// Uses Cramer's rule to get an approximate answer. The answer is *exact* if
	//   the coordinates are even and the angle of each segment is a multiple of
	//   45 degrees (as is the case for AI lines during the BSP build).
	// Set 'extended' to 'true' to get the intersection of the extended lines,
	//   i.e., not the line segments.
	// Method updated to work with 'long' instead of 'int' (2/10/13). There was
	//   a numerator overflow when dealing with a double-sized level that led
	//   to the value of x0 taking on 110.
	// Consolidated 'getExtendedIntersection' with 'getIntersection' using a
	//   'remote' flag. Also added a new and important option called 'snap'.
	//   This multiplies the intersection point's coordinates by 2 and adds
	//   a remainder of 1 to either coordinate, to signify simply that there
	//   was a remainder when calculating that coordinate. This will be used
	//   to completely obviate the inaccuracy of floor division.
	public static Fieldpoint getIntersection(int ix11, int iy11, int ix12, int iy12,
										     int ix21, int iy21, int ix22, int iy22,
										     boolean remote, boolean snap)
	{
	    long x11 = (long)ix11;
	    long y11 = (long)iy11;
        long x12 = (long)ix12;
        long y12 = (long)iy12;
        long x21 = (long)ix21;
        long y21 = (long)iy21;
        long x22 = (long)ix22;
        long y22 = (long)iy22;
		long a11 = y12 - y11;
		long a12 = x11 - x12;
		long a21 = y22 - y21;
		long a22 = x21 - x22;
		long b1 = x11*a11 + y11*a12;
		long b2 = x21*a21 + y21*a22;
		long denom = a11*a22 - a21*a12;
		if (denom == 0)	return null;
		long x0num = b1*a22 - b2*a12;
		long y0num = b2*a11 - b1*a21;
		long x0 = x0num / denom;
		long y0 = y0num / denom;
		if (snap) {
		    x0 *= 2; // OPTIMIZE: If (x0, y0) are always positive with snap
		    y0 *= 2; //   division, this can be converted to bit shift.
	        boolean xneg = (x0num > 0 && denom < 0) || (x0num < 0 && denom > 0);
	        boolean yneg = (y0num > 0 && denom < 0) || (y0num < 0 && denom > 0);
		    if (!xneg && (x0num % denom != 0)) ++x0;
		    if (!yneg && (y0num % denom != 0)) ++y0;
		}
		if (!remote) {
		    
		    // Fixed to make failsafe.
		    int xtest11 = DragoStatics.sign(x11*denom - x0num);
		    int xtest12 = DragoStatics.sign(x12*denom - x0num);
		    int xtest21 = DragoStatics.sign(x21*denom - x0num);
		    int xtest22 = DragoStatics.sign(x22*denom - x0num);
		    boolean line1hasx = !(xtest11 != 0 && xtest11 == xtest12);
		    boolean line2hasx = !(xtest21 != 0 && xtest21 == xtest22);
		    if (!line1hasx || !line2hasx) return null;
            int ytest11 = DragoStatics.sign(y11*denom - y0num);
            int ytest12 = DragoStatics.sign(y12*denom - y0num);
            int ytest21 = DragoStatics.sign(y21*denom - y0num);
            int ytest22 = DragoStatics.sign(y22*denom - y0num);
            boolean line1hasy = !(ytest11 != 0 && ytest11 == ytest12);
            boolean line2hasy = !(ytest21 != 0 && ytest21 == ytest22);
		    if (!line1hasy || !line2hasy) return null;
		}
		return new Fieldpoint((int)x0, (int)y0);

	}
	
	// Get intersection of specified line with this line.
	public Fieldpoint getIntersection(LineSeg l, boolean remote, boolean snap) {
		return getIntersection(this.x1(), this.y1(), this.x2(), this.y2(),
				               l.x1(), l.y1(), l.x2(), l.y2(),
				               remote, snap);
	}
	
	// Get the intersection of the line of specified endpoints with this line.
	public Fieldpoint getIntersection(int x1, int y1, int x2, int y2,
	                                  boolean remote, boolean snap)
	{
	    return getIntersection(this.x1(), this.y1(), this.x2(), this.y2(),
	                           x1, y1, x2, y2, remote, snap);
	}

	// Get an exact value for an intersection using a fraction (see the 'drago'
	//   package). Will be useful in failsafe collision response handling.
	// Can be overloaded, as the 'snap' boolean is not used here.
	// Much of this code is duplicated from the other 'getIntersection'...
	//   does it matter, I wonder?
	public static FractionalFieldpoint getIntersection(
	        int ix11, int iy11, int ix12, int iy12,
            int ix21, int iy21, int ix22, int iy22,
            boolean remote)
	{
        long x11 = (long)ix11;
        long y11 = (long)iy11;
        long x12 = (long)ix12;
        long y12 = (long)iy12;
        long x21 = (long)ix21;
        long y21 = (long)iy21;
        long x22 = (long)ix22;
        long y22 = (long)iy22;
        long a11 = y12 - y11;
        long a12 = x11 - x12;
        long a21 = y22 - y21;
        long a22 = x21 - x22;
        long b1 = x11*a11 + y11*a12;
        long b2 = x21*a21 + y21*a22;
        long denom = a11*a22 - a21*a12;
        if (denom == 0) return null;
        long x0num = b1*a22 - b2*a12;
        long y0num = b2*a11 - b1*a21;

        if (!remote) {
            
            // Fixed to make failsafe.
            int xtest11 = DragoStatics.sign(x11*denom - x0num);
            int xtest12 = DragoStatics.sign(x12*denom - x0num);
            int xtest21 = DragoStatics.sign(x21*denom - x0num);
            int xtest22 = DragoStatics.sign(x22*denom - x0num);
            boolean line1hasx = !(xtest11 != 0 && xtest11 == xtest12);
            boolean line2hasx = !(xtest21 != 0 && xtest21 == xtest22);
            if (!line1hasx || !line2hasx) return null;
            int ytest11 = DragoStatics.sign(y11*denom - y0num);
            int ytest12 = DragoStatics.sign(y12*denom - y0num);
            int ytest21 = DragoStatics.sign(y21*denom - y0num);
            int ytest22 = DragoStatics.sign(y22*denom - y0num);
            boolean line1hasy = !(ytest11 != 0 && ytest11 == ytest12);
            boolean line2hasy = !(ytest21 != 0 && ytest21 == ytest22);
            if (!line1hasy || !line2hasy) return null;
        }
        
	    // Get the fractional fieldpoint corresponding to
        //   the exact point of intersection.
        if (denom < 0) { x0num = -x0num; y0num = -y0num; denom = -denom; }
        Fraction x0 = new Fraction(x0num, denom);
        Fraction y0 = new Fraction(y0num, denom);
        return new FractionalFieldpoint(x0, y0);
	
	} // end method getIntersection (returns exact point with fractions)
        
    // The two derived functions.
    public FractionalFieldpoint getIntersection(LineSeg l, boolean remote) {
        return getIntersection(this.x1(), this.y1(), this.x2(), this.y2(),
                               l.x1(), l.y1(), l.x2(), l.y2(),
                               remote);
    }
    public FractionalFieldpoint getIntersection(int x1, int y1, int x2, int y2,
                                      boolean remote)
    {
        return getIntersection(this.x1(), this.y1(), this.x2(), this.y2(),
                               x1, y1, x2, y2, remote);
    }

} // end class LineSeg

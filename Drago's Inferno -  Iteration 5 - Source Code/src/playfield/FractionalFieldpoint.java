/*
 * FractionalFieldpoint.java (v1.0)
 * 3/17/2013
 */
package playfield;

import drago.Fraction;

/**
 * A more precise fieldpoint that is used in collision
 * response handling (see also, Fraction).
 * 
 * @author Quothmar
 *
 */
public class FractionalFieldpoint
{
    public Fraction x;
    public Fraction y;

    public FractionalFieldpoint() {
        x = new Fraction(0, 1);
        y = new Fraction(0, 1);
    }

    public FractionalFieldpoint(Fraction startx, Fraction starty) {
        x = new Fraction(startx);
        y = new Fraction(starty);
    }
    
    public FractionalFieldpoint(FractionalFieldpoint fp) {
        x = new Fraction(fp.x);
        y = new Fraction(fp.y);
    }
}

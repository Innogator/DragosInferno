/*
 * Fraction.java (v1.0)
 * 3/17/2013
 */
package drago;

/**
 * Represents a rational number or fraction, with a numerator and
 * denominator of type 'long'. Created in response to a slight error
 * in collision response handling in order to obtain perfectly
 * accurate collision detection results.
 * 
 * @author Quothmar
 *
 */
public class Fraction
{
    // Numerator and denominator; getters and setters.
    private long num;
    public long num() { return num; }
    public void setnum(long newnum) { num = newnum; }
    private long denom;
    public long denom() { return denom; }
    public void setdenom(long newdenom) {
        if (newdenom <= 0) {
            System.out.println("Fraction.setdenom(): Denominator must be a positive number");
            System.exit(1);
        }
        denom = newdenom;
    }
    
    // Default constructor.
    public Fraction() {
        num = 0;
        denom = 1;
    }
    
    // Two-argument constructor.
    public Fraction(long m, long n) {
        if (n <= 0) {
            System.out.println("Fraction.Fraction(): Denominator must be a positive number");
            System.exit(1);
        }
        num = m;
        denom = n;
    }
        
    // Copy constructor.
    public Fraction(Fraction q) {
        num = q.num;
        denom = q.denom;
    }
    
    // Greatest common divisor of two positive integers.
    public static long gcd(long x, long y) {
        if (x <= 0 || y <= 0) {
            System.out.println("Fraction.gcd(): Argument is not a positive number");
            System.exit(1);
        }
        while (x != y)
            if (x < y) y -= x;
            else x -= y;
        return x;
    }
    
    // Least common multiple.
    public static long lcm(long x, long y) {
        return (x / gcd(x, y))*y;
    }
    
    // Adds a given fraction to this fraction.
    public void add(Fraction q) {
        long reduct = gcd(this.denom, q.denom);
        this.num = ((q.denom)*(this.num) + (this.denom)*(q.num)) / reduct;
        this.denom = (this.denom / reduct)*(q.denom);
    }
    
    // Adds two fractions and returns a third fraction.
    public static Fraction add(Fraction p, Fraction q) {
        Fraction r = new Fraction(p);
        r.add(q);
        return r;
    }
    
    // Multiply this fraction by a given fraction.
    public void mult(Fraction q) {
        this.num   *= q.num;
        this.denom *= q.denom;
    }
    
    // Multiplies two fractions and returns a third fraction.
    public static Fraction mult(Fraction p, Fraction q) {
        Fraction r = new Fraction(p);
        r.mult(q);
        return r;
    }

    // Divide this fraction by a given fraction.
    public void divBy(Fraction q) {
        if (q.num == 0) {
            System.out.println("Fraction.divBy(): Cannot divide by zero");
            System.exit(1);
        }
        this.num   *= q.denom;
        this.denom *= q.num;
    }
    
    // Divides one fraction by another and returns a third fraction.
    public static Fraction div(Fraction p, Fraction q) {
        Fraction r = new Fraction(p);
        r.divBy(q);
        return r;
    }
    
    // Reduces a fraction, if possible.
    public void reduce() {
        if (this.num == 0) return;
        long posnum = (num < 0) ? -num : num;
        long reduct = gcd(posnum, denom);
        this.num   /= reduct;
        this.denom /= reduct;
    }
    
    // Returns the result of floor division for this fraction.
    public long floor() {
        return this.num / this.denom;
    }
    
} // end class Fraction
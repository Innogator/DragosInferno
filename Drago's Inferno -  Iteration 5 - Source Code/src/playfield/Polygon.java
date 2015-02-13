/*
 * Polygon.java (v1.0)
 * 3/17/2013
 */
package playfield;

import java.util.Vector;
import java.awt.Graphics2D;
import java.awt.Color;

/**
 * Represents a simple geometric polygon as an array of
 * vertices with integer coordinates.
 * 
 * @author Quothmar
 *
 */
public class Polygon
{
	private Vector<Fieldpoint> vertices;
	private int numVertices;

	public Polygon() {
		vertices = new Vector<Fieldpoint>();
		numVertices = 0;
	}

	public Polygon(Fieldpoint... fieldpoints) {
		vertices = new Vector<Fieldpoint>();
		for (Fieldpoint f : fieldpoints) {
			vertices.add(f);
			++numVertices;
		}
	}
	
	public Polygon(Polygon p) {
	    vertices = new Vector<Fieldpoint>();
        numVertices = p.numVertices;
	    for (int i = 0; i < p.numVertices; ++i)
	        vertices.add(new Fieldpoint(p.getVertex(i)));
	}
	
	public Fieldpoint getVertex(int n) {
		return vertices.get(n);
	}

	public int getNumVertices() {
		return numVertices;
	}

	// Edited to create a new fieldpoint from given fieldpoint
	//   (as John had used a temporary fieldpoint to create the playfield object)
	public void addVertex(Fieldpoint fp) {
		vertices.add(new Fieldpoint(fp.x, fp.y));
		++numVertices;
	}
	
	// Added to allow direct transformation of polygons.
	public void setVertex(int n, int newx, int newy) {
	    vertices.get(n).x = newx;
	    vertices.get(n).y = newy;
	}

	public boolean removeVertex(Fieldpoint aFieldpoint) {
		if (vertices.remove(aFieldpoint)) {
			--numVertices;
			return true;
		} else {
			return false;
		}
	}

	// Returns 'true' iff the line segment either *exists within*
	//   or *protrudes into* the polygon (not if the segment merely
	//   touches or borders the polygon). 
	public boolean overlaps(LineSeg l) {
	    
	    // Get first intersection of line with polygon. Use
	    //   "snap division" (remainder-flagged division) to ensure that
	    //   all protrusions are recognized.
	    int v = 0;
	    int x1 = 0;
	    int y1 = 0;
	    int x2 = 0;
	    int y2 = 0;
        Fieldpoint fp = null;
	    while (v < this.getNumVertices()) {
	        x1 = this.getVertex(v).x;
	        y1 = this.getVertex(v).y;
	        x2 = this.getVertex((v + 1) % this.getNumVertices()).x;
	        y2 = this.getVertex((v + 1) % this.getNumVertices()).y;
	        fp = l.getIntersection(x1, y1, x2, y2, false, true);
	        if (fp != null) break;
	        else ++v;
	    }
	    
	    // If there is an intersection, see what kind it is.
	    if (fp != null) {
	        
	        // Scale for comparison with remainder-flagged point.
	        x1 *= 2; y1 *= 2;
	        x2 *= 2; y2 *= 2;
	        int lx1 = 2*(l.x1());
	        int ly1 = 2*(l.y1());
	        int lx2 = 2*(l.x2());
	        int ly2 = 2*(l.y2());
	        
	        // Is it 5-point? If so, it's a protrusion.
	        if (    fp.x != x1  && fp.y != y1  && fp.x != x2  && fp.y != y2
	             && fp.x != lx1 && fp.y != ly1 && fp.x != lx2 && fp.y != ly2 )
	        {
	            return true;
	        }
	        
	        // Otherwise, if it's 4-point or 3-point...
	        else {
	            
	            // Get the two neighboring vertices of the intersection.
	            int v1 = (fp.x == x1 && fp.y == y1) ? (v - 1) : v;
	            int v2 = (fp.x == x2 && fp.y == y2) ? (v + 2) : (v + 1);
	            if (v1 < 0) v1 += this.getNumVertices();
	            if (v2 >= this.getNumVertices()) v2 -= this.getNumVertices();
	            int v1x = 2*(this.getVertex(v1).x);
	            int v1y = 2*(this.getVertex(v1).y);
	            int v2x = 2*(this.getVertex(v2).x);
	            int v2y = 2*(this.getVertex(v2).y);
	            
	            // Get the next intersection of the line with the polygon,
	            //   if any.
	            ++v;
	            Fieldpoint fp2 = null;
	            while (v < this.getNumVertices()) {
	                x1 = this.getVertex(v).x;
	                y1 = this.getVertex(v).y;
	                x2 = this.getVertex((v + 1) % this.getNumVertices()).x;
	                y2 = this.getVertex((v + 1) % this.getNumVertices()).y;
	                fp2 = l.getIntersection(x1, y1, x2, y2, false, true);
	                if ((fp2 != null) && (fp2.x != fp.x || fp2.y != fp.y))
	                    break;
	                else { fp2 = null; ++v; }
	            }
	            
	            // If there is another intersection...
	            if (fp2 != null) {
	                
	                // Check to see if it lies on a neighboring vertex.
	                //   If it does, the line does not protrude (it borders).
	                if ((fp2.x == v1x && fp2.y == v1y) || (fp2.x == v2x && fp2.y == v2y))
	                    return false;
	                
	                // Otherwise, if it intersects somewhere else, it
	                //   protrudes.
	                else return true;

	            } // end if (second intersection found)
	        } // end if (first intersection is 4-point or 3-point)
	    } // end if (first intersection found)
	    
	    // If tests weren't conclusive, check to see if
	    //   either vertex lies strictly inside the polygon.
	    for (int i = 0; i < 2; ++i) {
	        boolean inside = true;
	        int x = (i == 0) ? l.x1() : l.x2();
	        int y = (i == 0) ? l.y1() : l.y2();
	        for (v = 0; v < this.getNumVertices(); ++v) {
	            x1 = this.getVertex(v).x;
	            y1 = this.getVertex(v).y;
	            x2 = this.getVertex((v + 1) % this.getNumVertices()).x;
	            y2 = this.getVertex((v + 1) % this.getNumVertices()).y;
	            int nx = y2 - y1;
	            int ny = x1 - x2;
	            if ((x - x1)*nx + (y - y1)*ny >= 0) inside = false;
	        }
	        if (inside) return true;
	    }
	    
	    return false;
	    
	} // end method overlaps

	// Same function, for polygons.
	public boolean overlaps(Polygon p) {
	    for (int v = 0; v < p.getNumVertices(); ++v) {
	        int x1 = p.getVertex(v).x;
	        int y1 = p.getVertex(v).y;
	        int x2 = p.getVertex((v + 1) % p.getNumVertices()).x;
	        int y2 = p.getVertex((v + 1) % p.getNumVertices()).y;
	        LineSeg l = new LineSeg(x1, y1, x2, y2);
	        if (this.overlaps(l)) return true;
	    }
	    return false;
	}
	
	// Draw the polygon in a specified color.
	public void draw(Graphics2D g2d, int xcam, int ycam, Color color) {
	    
        for (int j = 0; j < this.getNumVertices(); ++j) {
            int x1 = this.getVertex(j).x;
            int y1 = this.getVertex(j).y;
            int x2 = this.getVertex((j+1)%this.getNumVertices()).x;
            int y2 = this.getVertex((j+1)%this.getNumVertices()).y;
            int linex1 = x1 - xcam;
            int liney1 = 480 - (y1 - ycam);
            int linex2 = x2 - xcam;
            int liney2 = 480 - (y2 - ycam);
            g2d.setColor(color);
            g2d.drawLine(linex1, liney1, linex2, liney2);
        }
        
	} // end method draw
} // end class Polygon

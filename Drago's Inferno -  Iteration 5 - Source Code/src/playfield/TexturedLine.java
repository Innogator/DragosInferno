/*
 * TexturedLine.java (v1.0)
 * 3/17/2013
 */
package playfield;

/**
 * A LineSeg plus an additional texture that defines what kind
 * of line it is (wall, water boundary, etc.).
 * 
 * @author Quothmar/John Thrasher
 *
 */
public class TexturedLine extends LineSeg {
	private Texture type;

	public TexturedLine() {
		super();
		type = null;
	}

	public TexturedLine(Texture aTexture) {
		super();
		type = aTexture;
	}
	
	public TexturedLine(int x1, int y1, int x2, int y2, Texture t) {
		super(x1, y1, x2, y2);
		type = t;
	}
	
	public Texture getType() {
		return type;
	}

	public void setType(Texture aTexture) {
		type = aTexture;
	}
}

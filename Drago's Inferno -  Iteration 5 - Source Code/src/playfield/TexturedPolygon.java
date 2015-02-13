/*
 * TexturedPolygon.java (v1.0)
 * 3/17/2013
 */
package playfield;

/**
 * A polygon plus an additional texture, used to generate
 * TexturedLines for use in the playfield.
 * 
 * @author John Thrasher
 *
 */
public class TexturedPolygon extends Polygon {
	private Texture type;

	public TexturedPolygon() {
		super();
		type = null;
	}

	public TexturedPolygon(Texture aTexture) {
		super();
		type = aTexture;
	}

	public TexturedPolygon(Texture aTexture, Fieldpoint... fieldpoints) {
		super(fieldpoints);
		type = aTexture;
	}

	public Texture getType() {
		return type;
	}
	public void setType(Texture aTexture) {
		type = aTexture;
	}
}

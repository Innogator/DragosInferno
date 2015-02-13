package entity;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import playfield.Fieldpoint;
import playfield.Playfield;
import playfield.Polygon;

public class YellowEgg extends Entity
{
    private static BufferedImage sprite;
    
    // Egg constructor.
    public YellowEgg(int x, int y, String beginFacing, Playfield pf) {
        super(x, y, beginFacing, pf);
        Fieldpoint fp1 = new Fieldpoint(x - 7, y + 4);
        Fieldpoint fp2 = new Fieldpoint(x - 7, y - 10);
        Fieldpoint fp3 = new Fieldpoint(x + 7, y - 10);
        Fieldpoint fp4 = new Fieldpoint(x + 7, y + 4);
        this.setBoundingPoly(new Polygon(fp1, fp2, fp3, fp4));
        this.setHeight(20);
        this.setz(0);
        this.setState(Entity.ST_ITEM);
        this.setdx(0);
        this.setdy(0);
        this.setdz(0);
        this.attach();
    }
    
    public void tickStateCounter() { }
    public static void loadSprites() {
        String filename = "img\\YELLOW_EGG_2.gif";
        try {
            File imgfile = new File(filename);
            if (!imgfile.exists()) throw new Exception();
            sprite = ImageIO.read(imgfile);
        }
        catch (Exception e) {
            System.out.println("Exception while loading Egg image!");
            e.printStackTrace();
            System.exit(1);
        }
    }
    private static BufferedImage getSprite() { return sprite; }
    public BufferedImage getCurrentSprite() { return sprite; }
    public void act() {}

}

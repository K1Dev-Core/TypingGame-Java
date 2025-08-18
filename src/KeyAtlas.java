import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class KeyAtlas {
    private static class Frames {
        BufferedImage normal, pressed;
        Frames(BufferedImage n, BufferedImage p) {
            normal = n;
            pressed = p;
        }
    }
    
    private final Map<Character, Frames> map = new HashMap<>();
    private Dimension baseSize = new Dimension(34, 16);
    
    public KeyAtlas(String dir, String ext, boolean twoFrames, String chars) {
        for (char raw : chars.toCharArray()) {
            char c = Character.toUpperCase(raw);
            File f = new File(dir, c + ext);
            try {
                BufferedImage img = ImageIO.read(f);
                if (img == null) continue;
                if (twoFrames) {
                    int w = img.getWidth() / 2, h = img.getHeight();
                    baseSize = new Dimension(w, h);
                    map.put(c, new Frames(img.getSubimage(0, 0, w, h), img.getSubimage(w, 0, w, h)));
                } else {
                    baseSize = new Dimension(img.getWidth(), img.getHeight());
                    map.put(c, new Frames(img, img));
                }
            } catch (Exception ignored) {}
        }
    }
    
    public boolean isReady() {
        return !map.isEmpty();
    }
    
    public Dimension keySize() {
        return baseSize;
    }
    
    public BufferedImage getNormal(char c) {
        Frames f = map.get(Character.toUpperCase(c));
        return f != null ? f.normal : fallback();
    }
    
    public BufferedImage getPressed(char c) {
        Frames f = map.get(Character.toUpperCase(c));
        return f != null ? f.pressed : fallback();
    }
    
    private BufferedImage fallback() {
        BufferedImage img = new BufferedImage(baseSize.width, baseSize.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(60, 60, 60));
        g.fillRect(0, 0, baseSize.width, baseSize.height);
        g.setColor(new Color(200, 60, 60));
        g.drawRect(1, 1, baseSize.width - 3, baseSize.height - 3);
        g.dispose();
        return img;
    }
}
import java.awt.*;
import java.awt.image.BufferedImage;

public class RenderUtils {
    
    public static void centerTextAt(Graphics2D g2, String text, int x, int y) {
        if (text == null || text.isEmpty()) return;
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text);
        g2.drawString(text, x - w / 2, y);
    }
    
    public static void drawRoundedPanel(Graphics2D g2, int x, int y, int width, int height, Color fillColor, Color borderColor) {
        g2.setColor(fillColor);
        g2.fillRoundRect(x, y, width, height, 12, 12);
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(x, y, width, height, 12, 12);
        }
    }
    
    public static void drawHighlightedKey(Graphics2D g2, int x, int y, int keyW, int keyH, Color color) {
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(color);
        g2.drawRoundRect(x - 3, y - 3, keyW + 6, keyH + 6, 12, 12);
        g2.setStroke(old);
    }
    
    public static void drawKeyWithShadow(Graphics2D g2, BufferedImage keyImage, int x, int y, int keyW, int keyH) {
        g2.setColor(UIConfig.BOX_SHADOW);
        g2.fillRoundRect(x - 6, y - 6, keyW + 12, keyH + 12, 18, 18);
        g2.drawImage(keyImage, x, y, keyW, keyH, null);
    }
    
    public static int calculateSpriteWidth(BufferedImage sprite, int targetHeight) {
        if (sprite == null) return 0;
        return (int) (sprite.getWidth() * (targetHeight / (double) sprite.getHeight()));
    }
    
    public static void drawKeyPrompt(Graphics2D g2, String preText, String postText, BufferedImage keySprite, 
                                   int centerX, int y, Font font) {
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int spriteH = 22;
        int spriteW = calculateSpriteWidth(keySprite, spriteH);
        int totalW = fm.stringWidth(preText) + spriteW + fm.stringWidth(postText);
        int x = centerX - totalW / 2;
        
        g2.setColor(Color.WHITE);
        g2.drawString(preText, x, y);
        int curX = x + fm.stringWidth(preText);
        
        if (keySprite != null) {
            g2.drawImage(keySprite, curX, y - spriteH + 2, spriteW, spriteH, null);
            curX += spriteW;
        }
        
        g2.drawString(postText, curX, y);
    }
}
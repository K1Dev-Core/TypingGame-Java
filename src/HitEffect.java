import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class HitEffect implements Effect {
    private BufferedImage spriteSheet;
    private final List<Rectangle> frames = new ArrayList<>();
    private int currentFrame = 0;
    private boolean playing = false;
    private int x, y;
    private double scale = 3.0;

    public HitEffect(String spritePath) {
        try {
            spriteSheet = ImageIO.read(new File(spritePath));
            int fw = 32, fh = 32;
            for (int i = 0; i < 8; i++) {
                frames.add(new Rectangle(i * fw, 0, fw, fh));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Timer(GameConfig.HIT_EFFECT_TIMER_DELAY, e -> update()).start();
    }

    public void setScale(double s) {
        this.scale = s;
    }

    public void playAt(int mx, int my) {
        this.x = mx;
        this.y = my;
        this.playing = true;
        this.currentFrame = 0;
    }

    @Override
    public void update() {
        if (playing) {
            currentFrame++;
            if (currentFrame >= frames.size()) {
                playing = false;
                currentFrame = 0;
            }
        }
    }

    @Override
    public boolean isActive() {
        return playing;
    }

    @Override
    public void reset() {
        playing = false;
        currentFrame = 0;
    }

    @Override
    public void render(Graphics2D g2) {
        draw(g2);
    }

    public void draw(Graphics2D g2) {
        if (playing && spriteSheet != null) {
            Rectangle rect = frames.get(currentFrame);
            int w = (int) (rect.width * scale);
            int h = (int) (rect.height * scale);

            g2.drawImage(spriteSheet,
                    x - w / 2, y - h / 2,
                    x + w / 2, y + h / 2,
                    rect.x, rect.y, rect.x + rect.width, rect.y + rect.height,
                    null);
        }
    }
}

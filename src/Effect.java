import java.awt.Graphics2D;

public interface Effect {
    void update();
    void render(Graphics2D g2);
    boolean isActive();
    void reset();
}
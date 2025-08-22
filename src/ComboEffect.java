import java.awt.*;

public class ComboEffect implements Effect {
    private int comboCount;
    private int comboMultiplier;
    private long displayUntil;
    private float scale;
    private float angle;
    private float x;
    private float y;
    private Color color;
    private boolean active;

    public ComboEffect() {
        reset();
    }

    public void reset() {
        comboCount = 0;
        comboMultiplier = 1;
        displayUntil = 0;
        scale = 1.0f;
        angle = 0;
        active = false;
    }

    public void triggerCombo(int comboCount, int comboMultiplier, int x, int y) {
        this.comboCount = comboCount;
        this.comboMultiplier = comboMultiplier;
        this.x = x;
        this.y = y;
        this.scale = 1.6f;
        this.angle = (float) (Math.random() * 30 - 15);
        this.displayUntil = System.currentTimeMillis() + GameConfig.COMBO_DISPLAY_DURATION;
        this.active = true;

        if (comboCount >= 20) {
            this.color = new Color(255, 50, 50);
        } else if (comboCount >= 10) {
            this.color = new Color(255, 165, 0);
        } else if (comboCount >= 5) {
            this.color = new Color(255, 215, 0);
        } else {
            this.color = new Color(0, 255, 127);
        }
    }

    @Override
    public void update() {
        if (!active)
            return;

        long now = System.currentTimeMillis();
        if (now > displayUntil) {
            active = false;
            return;
        }

        long elapsed = displayUntil - now;
        if (elapsed < GameConfig.COMBO_FADE_DURATION) {
            scale *= 0.97f;
        } else if (scale > 1.0f) {
            scale *= 0.95f;
            if (scale < 1.0f)
                scale = 1.0f;
        }

        if (elapsed < 1000) {
            y -= 0.5f;
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void render(Graphics2D g2) {
        render(g2, new Font("Arial", Font.BOLD, 24), new Font("Arial", Font.PLAIN, 18));
    }

    public void render(Graphics2D g2, Font boldFont, Font normalFont) {
        if (!active)
            return;

        Graphics2D g = (Graphics2D) g2.create();
        g.translate(x, y);
        g.rotate(Math.toRadians(angle), 0, 0);
        g.scale(scale, scale);

        g.setColor(color);
        g.setFont(boldFont.deriveFont(Font.BOLD, 24f));

        String comboText = comboCount + " COMBO!";
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(comboText);

        g.drawString(comboText, -textWidth / 2, 0);

        if (comboMultiplier > 1) {
            g.setFont(normalFont.deriveFont(Font.BOLD, 18f));
            String multText = "x" + comboMultiplier;
            fm = g.getFontMetrics();
            int multWidth = fm.stringWidth(multText);
            g.drawString(multText, -multWidth / 2, 25);
        }

        g.dispose();
    }
}

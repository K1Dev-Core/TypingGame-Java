import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class SplashScreen {
    private static final int FADE_DURATION = 2000;
    private static final int DISPLAY_DURATION = 3000;

    private int screenWidth;
    private int screenHeight;
    private BufferedImage logo;
    private long startTime;
    private boolean isDone = false;

    // enum ตัวแปรประเภทไว้เก็บเช้ต
    public enum State {
        FADE_IN,
        DISPLAY,
        FADE_OUT,
        DONE
    }

    private State currentState = State.FADE_IN;

    public SplashScreen(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        startTime = System.currentTimeMillis();

        try {
            logo = ImageIO.read(new File("./res/logo/logo.png"));
        } catch (Exception e) {
            logo = createDefaultLogo();
        }
    }

    private BufferedImage createDefaultLogo() {
        BufferedImage img = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint gp = new GradientPaint(0, 0, new Color(40, 70, 100), 300, 300, new Color(100, 30, 60));
        g.setPaint(gp);
        g.fillRect(0, 0, 300, 300);

        g.setColor(new Color(220, 220, 220));
        g.fillRoundRect(50, 100, 200, 100, 20, 20);
        g.setColor(new Color(40, 40, 40));

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 6; c++) {
                g.fillRoundRect(60 + c * 30, 110 + r * 30, 25, 25, 5, 5);
            }
        }

        g.dispose();
        return img;
    }

    public void update() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;

        switch (currentState) {
            case FADE_IN:
                if (elapsedTime > FADE_DURATION) {
                    currentState = State.DISPLAY;
                    startTime = currentTime;
                }
                break;

            case DISPLAY:
                if (elapsedTime > DISPLAY_DURATION) {
                    currentState = State.FADE_OUT;
                    startTime = currentTime;
                }
                break;

            case FADE_OUT:
                if (elapsedTime > FADE_DURATION) {
                    currentState = State.DONE;
                    isDone = true;
                }
                break;

            case DONE:
                isDone = true;
                break;
        }
    }

    public void draw(Graphics2D g2) {
        if (isDone)
            return;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float alpha = 1.0f;
        long elapsedTime = System.currentTimeMillis() - startTime;

        if (currentState == State.FADE_IN) {
            alpha = Math.min(1.0f, (float) elapsedTime / FADE_DURATION);
        } else if (currentState == State.FADE_OUT) {
            alpha = Math.max(0.0f, 1.0f - ((float) elapsedTime / FADE_DURATION));
        }

        g2.setColor(new Color(0, 0, 0, 255));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        if (logo != null) {
            double maxLogoWidth = screenWidth * 0.4;
            double maxLogoHeight = screenHeight * 0.4;

            double scale = 1.0;
            if (logo.getWidth() > maxLogoWidth || logo.getHeight() > maxLogoHeight) {
                double scaleX = maxLogoWidth / (double) logo.getWidth();
                double scaleY = maxLogoHeight / (double) logo.getHeight();
                scale = Math.min(scaleX, scaleY);
            }

            int scaledWidth = (int) (logo.getWidth() * scale);
            int scaledHeight = (int) (logo.getHeight() * scale);

            int logoX = (screenWidth - scaledWidth) / 2;
            int logoY = (screenHeight - scaledHeight) / 2;

            g2.drawImage(logo, logoX, logoY, scaledWidth, scaledHeight, null);
        }

        g2.setComposite(oldComposite);
    }

    public boolean isDone() {
        return isDone;
    }

    public void updateDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }
}

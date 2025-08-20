import java.awt.image.BufferedImage;

public class Sprite {
    private BufferedImage sheet;
    private int frames, w, h, frame, msPerFrame;
    private long lastAt;
    private boolean loop, finished;

    public Sprite(BufferedImage sheet, int frames, int msPerFrame, boolean loop) {
        this.sheet = sheet;
        this.frames = frames;
        this.msPerFrame = msPerFrame;
        this.loop = loop;
        this.w = sheet.getWidth() / frames;
        this.h = sheet.getHeight();
        this.frame = 0;
        this.lastAt = System.currentTimeMillis();
        this.finished = false;
    }

    public BufferedImage current() {
        long now = System.currentTimeMillis();
        if (now - lastAt >= msPerFrame) {
            lastAt = now;
            frame++;
            if (frame >= frames) {
                if (loop) frame = 0;
                else { frame = frames - 1; finished = true; }
            }
        }
        return sheet.getSubimage(frame * w, 0, w, h);
    }

    public boolean finishedOnce() { return finished; }

    public void reset() { frame = 0; finished = false; lastAt = System.currentTimeMillis(); }
}

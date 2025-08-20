import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumMap;

public class CharacterPack {
    public enum Anim { IDLE, WALK, ATTACK, TAKE_HIT, DEATH }
    private int baselineOffset;
    public static class AnimSpec {
        public int frames;
        public int msPerFrame;
        public boolean loop;
        public AnimSpec(int frames, int msPerFrame, boolean loop) {
            this.frames = frames;
            this.msPerFrame = msPerFrame;
            this.loop = loop;
        }
        public AnimSpec copy() { return new AnimSpec(frames, msPerFrame, loop); }
    }

    public static class Config {
        private final EnumMap<Anim, AnimSpec> map = new EnumMap<>(Anim.class);
        public Config set(Anim a, int frames, int msPerFrame, boolean loop) {
            map.put(a, new AnimSpec(frames, msPerFrame, loop));
            return this;
        }
        public AnimSpec getOrDefault(Anim a, AnimSpec def) {
            AnimSpec s = map.get(a);
            return (s != null) ? s : def;
        }
    }

    private Sprite idle, walk, attack, hit, death;
    private Anim current;
    private boolean facingLeft;
    public int x, y;

    private BufferedImage idleSheet, walkSheet, attackSheet, hitSheet, deathSheet;

    private AnimSpec defIdle   = new AnimSpec(4, 120, true);
    private AnimSpec defWalk   = new AnimSpec(4, 110, false);
    private AnimSpec defAttack = new AnimSpec(8,  80, false);
    private AnimSpec defHit    = new AnimSpec(4,  90, false);
    private AnimSpec defDeath  = new AnimSpec(4, 150, false);
    public CharacterPack(String basePath, int x, int y, boolean facingLeft) {
        this(basePath, x, y, facingLeft, null, 0);
    }

    public CharacterPack(String basePath, int x, int y, boolean facingLeft, Config cfg) {
        this(basePath, x, y, facingLeft, cfg, 0);
    }

    public CharacterPack(String basePath, int x, int y, boolean facingLeft, Config cfg, int baselineOffset) {
        try { idleSheet   = ImageIO.read(new File(basePath + "/Idle.png"));     } catch (Exception ignored) { idleSheet = dummySheet(); }
        try { walkSheet   = ImageIO.read(new File(basePath + "/Walk.png"));     } catch (Exception ignored) { walkSheet = dummySheet(); }
        try { attackSheet = ImageIO.read(new File(basePath + "/Attack.png"));   } catch (Exception ignored) { attackSheet = dummySheet(); }
        try { hitSheet    = ImageIO.read(new File(basePath + "/Take Hit.png")); } catch (Exception ignored) { hitSheet = dummySheet(); }
        try { deathSheet  = ImageIO.read(new File(basePath + "/Death.png"));    } catch (Exception ignored) { deathSheet = dummySheet(); }

        AnimSpec sIdle   = (cfg == null) ? defIdle.copy()   : cfg.getOrDefault(Anim.IDLE,     defIdle).copy();
        AnimSpec sWalk   = (cfg == null) ? defWalk.copy()   : cfg.getOrDefault(Anim.WALK,     defWalk).copy();
        AnimSpec sAttack = (cfg == null) ? defAttack.copy() : cfg.getOrDefault(Anim.ATTACK,   defAttack).copy();
        AnimSpec sHit    = (cfg == null) ? defHit.copy()    : cfg.getOrDefault(Anim.TAKE_HIT, defHit).copy();
        AnimSpec sDeath  = (cfg == null) ? defDeath.copy()  : cfg.getOrDefault(Anim.DEATH,    defDeath).copy();

        idle   = new Sprite(idleSheet,   Math.max(1, sIdle.frames),   Math.max(1, sIdle.msPerFrame),   sIdle.loop);
        walk   = new Sprite(walkSheet,   Math.max(1, sWalk.frames),   Math.max(1, sWalk.msPerFrame),   sWalk.loop);
        attack = new Sprite(attackSheet, Math.max(1, sAttack.frames), Math.max(1, sAttack.msPerFrame), sAttack.loop);
        hit    = new Sprite(hitSheet,    Math.max(1, sHit.frames),    Math.max(1, sHit.msPerFrame),    sHit.loop);
        death  = new Sprite(deathSheet,  Math.max(1, sDeath.frames),  Math.max(1, sDeath.msPerFrame),  sDeath.loop);

        this.x = x;
        this.y = y;
        this.facingLeft = facingLeft;
        this.baselineOffset = baselineOffset;
        setAnim(Anim.IDLE);
    }


    private BufferedImage dummySheet() {
        BufferedImage b = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = b.createGraphics();
        g.setColor(new Color(255,255,255,60));
        g.fillRect(0,0,10,10);
        g.dispose();
        return b;
    }

    private Sprite dummy(boolean loop) {
        BufferedImage b = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        return new Sprite(b, 1, 1000, loop);
    }

    public void setAnim(Anim a) {
        this.current = a;
        switch (a) {
            case IDLE -> idle.reset();
            case WALK -> walk.reset();
            case ATTACK -> attack.reset();
            case TAKE_HIT -> hit.reset();
            case DEATH -> death.reset();
        }
    }

    public boolean animFinished() {
        return switch (current) {
            case WALK -> walk.finishedOnce();
            case ATTACK -> attack.finishedOnce();
            case TAKE_HIT -> hit.finishedOnce();
            case DEATH -> death.finishedOnce();
            default -> false;
        };
    }

    public void draw(Graphics2D g2, int scale) {
        BufferedImage f = switch (current) {
            case WALK -> walk.current();
            case ATTACK -> attack.current();
            case TAKE_HIT -> hit.current();
            case DEATH -> death.current();
            default -> idle.current();
        };
        int drawW = f.getWidth() * scale;
        int drawH = f.getHeight() * scale;
        int oy = baselineOffset * scale;

        if (facingLeft) {
            g2.drawImage(f, x + drawW, y - drawH + oy, -drawW, drawH, null);
        } else {
            g2.drawImage(f, x, y - drawH + oy, drawW, drawH, null);
        }
    }

    public void updateSpec(Anim anim, int frames, int msPerFrame, boolean loop) {
        frames = Math.max(1, frames);
        msPerFrame = Math.max(1, msPerFrame);
        switch (anim) {
            case IDLE    -> idle   = new Sprite(idleSheet,   frames, msPerFrame, loop);
            case WALK    -> walk   = new Sprite(walkSheet,   frames, msPerFrame, loop);
            case ATTACK  -> attack = new Sprite(attackSheet, frames, msPerFrame, loop);
            case TAKE_HIT-> hit    = new Sprite(hitSheet,    frames, msPerFrame, loop);
            case DEATH   -> death  = new Sprite(deathSheet,  frames, msPerFrame, loop);
        }
        if (current == anim) setAnim(anim);
    }
}

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GamePanel extends JPanel implements KeyListener, ActionListener {
    private final Timer timer = new Timer(16, this);
    private final Random rng = new Random();
    private final KeyAtlas atlas = new KeyAtlas(GameConfig.ASSETS_DIR, GameConfig.FILE_EXT, GameConfig.TWO_FRAMES_PER_FILE, GameConfig.CHAR_SET);
    private final SoundPool sStart = new SoundPool("./wav/click6_1.wav", 4);
    private final SoundPool sType = new SoundPool(GameConfig.CLICK_WAV_PATH, 6);
    private final SoundPool sErr = new SoundPool("./wav/click14_3.wav", 4);
    private final SoundPool sClick = new SoundPool("./wav/click15_1.wav", 6);
    private HitEffect hit = new HitEffect("./effect/hit-sprite-sheet.png");
    private final List<WordEntry> wordBank = WordBank.loadWordBank();

    private GameConfig.State state = GameConfig.State.READY;
    private WordEntry current = new WordEntry("HELLO", "HELLO", "เฮลโล", "สวัสดี");
    private int idx = 0;
    private int mistakes = 0, correct = 0, totalTyped = 0;
    private int wordsCompleted = 0;
    private long startMs = 0;
    private long wordStartMs = 0;
    private long flashUntil = 0;
    private long shakeUntil = 0;
    private int shakeAmp = 0;
    private long popUntil = 0;
    private long bonusUntil = 0;
    private final long[] lastPressAt = new long[256];
    private int health = GameConfig.MAX_HEALTH;
    private int bonusStreak = 0;
    private static final int BONUS_TIME_MS = 3000;

    private BufferedImage spaceSheet, spaceFrameNormal, spaceFramePressed;
    private BufferedImage backspaceSheet, backspaceFrameNormal, backspaceFramePressed;
    private boolean isSpaceHeld = false;
    private long spaceAnimLast = 0;
    private boolean spaceAnimOn = false;
    private static final int SPACE_ANIM_MS = 400;

    private long lastFpsTime = 0;
    private int frames = 0;
    private int fps = 0;

    private static final Color CLR_FLASH = new Color(180, 30, 40, 80);
    private static final Color CLR_BONUS_TINT = new Color(255, 215, 0, 60);
    private static final Color CLR_PANEL_BG = new Color(18, 20, 24);
    private static final Color CLR_BOX_SHADOW = new Color(0, 0, 0, 70);
    private static final Color CLR_HUD_PANEL = new Color(0, 0, 0, 120);
    private static final Color CLR_HUD_TEXT = Color.WHITE;
    private static final Color CLR_WPM = new Color(100, 200, 255);
    private static final Color CLR_ACC = new Color(150, 255, 150);
    private static final Color CLR_DONE = new Color(255, 150, 255);
    private static final Color CLR_BONUS = new Color(255, 100, 100);
    private static final Color CLR_TIME = new Color(255, 215, 0);
    private static final Color CLR_BAR_BG_OUTLINE = new Color(255, 255, 255, 100);
    private static final Color CLR_BAR_BG = new Color(60, 60, 60);
    private static final Color CLR_BAR_FRAME = new Color(0, 0, 0, 120);
    private static final Color CLR_PROG_BG = new Color(40, 40, 40);
    private static final Color CLR_PROG_LABEL = new Color(200, 200, 200);
    private Font fontSmall12, fontSmall11, fontBold16, fontBold20, fontPlain16, fontPlain6, fontTitle48;

    private boolean showSettings = false;
    private boolean ttsEnabled = true;
    private int masterVolume = 100;
    private final int[] ttsRates = new int[]{110, 140, 180};
    private int ttsSpeedLevel = 1;
    private Rectangle panelRect, toggleRect, sliderTrack, sliderKnob;
    private Rectangle speedRectSlow, speedRectNormal, speedRectFast;
    private boolean draggingSlider = false;

    enum Anim {IDLE, WALK, ATTACK, TAKE_HIT, DEATH}

    static class Sprite {
        BufferedImage sheet;
        int frames;
        int w;
        int h;
        int frame;
        long lastAt;
        int msPerFrame;
        boolean loop;
        Sprite(BufferedImage sheet, int frames, int msPerFrame, boolean loop) {
            this.sheet = sheet;
            this.frames = frames;
            this.msPerFrame = msPerFrame;
            this.loop = loop;
            this.w = sheet.getWidth() / frames;
            this.h = sheet.getHeight();
            this.frame = 0;
            this.lastAt = System.currentTimeMillis();
        }
        BufferedImage current() {
            long now = System.currentTimeMillis();
            if (now - lastAt >= msPerFrame) {
                if (loop) frame = (frame + 1) % frames;
                else if (frame < frames - 1) frame++;
                lastAt = now;
            }
            return sheet.getSubimage(frame * w, 0, w, h);
        }
        void reset() { frame = 0; lastAt = System.currentTimeMillis(); }
        boolean finishedOnce() { return !loop && frame >= frames - 1; }
    }

    static class SpritePack {
        Sprite idle, walk, attack, hit, death;
        SpritePack() {
            try { idle = new Sprite(javax.imageio.ImageIO.read(new java.io.File("./res/characters/Idle.png")), 4, 120, true); } catch (Exception e) { idle = dummy(true); }
            try { walk = new Sprite(javax.imageio.ImageIO.read(new java.io.File("./res/characters/Walk.png")), 4, 110, true); } catch (Exception e) { walk = dummy(true); }
            try { attack = new Sprite(javax.imageio.ImageIO.read(new java.io.File("./res/characters/Attack.png")), 8, 80, false); } catch (Exception e) { attack = dummy(true); }
            try { hit = new Sprite(javax.imageio.ImageIO.read(new java.io.File("./res/characters/Take Hit.png")), 4, 90, true); } catch (Exception e) { hit = dummy(true); }
            try { death = new Sprite(javax.imageio.ImageIO.read(new java.io.File("./res/characters/Death.png")), 4, 150, false); } catch (Exception e) { death = dummy(false); }
        }
        private Sprite dummy(boolean loop) {
            BufferedImage b = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
            return new Sprite(b, 1, 1000, loop);
        }
    }

    private final SpritePack charPack = new SpritePack();
    private Anim currentAnim = Anim.IDLE;
    private long animUntil = 0L;
    private boolean pendingNextWord = false;

    public GamePanel() {
        initFonts();
        initPanel();
        initKeySprites();
        initCursor();
        initMouse();
        warmupAtlas();
        layoutSettingsRects();
        timer.start();
    }

    private void setAnim(Anim a, long holdMs) {
        currentAnim = a;
        switch (a) {
            case IDLE -> charPack.idle.reset();
            case WALK -> charPack.walk.reset();
            case ATTACK -> charPack.attack.reset();
            case TAKE_HIT -> charPack.hit.reset();
            case DEATH -> charPack.death.reset();
        }
        animUntil = holdMs > 0 ? System.currentTimeMillis() + holdMs : 0L;
    }

    private void initFonts() {
        try {
            Font mcFont = Font.createFont(Font.TRUETYPE_FONT, new File("./Minecraft-TenTH.ttf"));
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(mcFont);
            fontSmall12 = mcFont.deriveFont(Font.PLAIN, 20f);
            fontSmall11 = mcFont.deriveFont(Font.PLAIN, 11f);
            fontPlain6 = mcFont.deriveFont(Font.PLAIN, 6f);
            fontBold16 = mcFont.deriveFont(Font.BOLD, 16f);
            fontBold20 = mcFont.deriveFont(Font.BOLD, 20f);
            fontPlain16 = mcFont.deriveFont(Font.PLAIN, 16f);
            fontTitle48 = mcFont.deriveFont(Font.BOLD, 48f);
        } catch (Exception ignored) { }
    }

    private void initPanel() {
        setPreferredSize(new Dimension(1100, 620));
        setBackground(CLR_PANEL_BG);
        setFocusable(true);
        setDoubleBuffered(true);
        addKeyListener(this);
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { layoutSettingsRects(); repaint(); }
        });
    }

    private void initKeySprites() {
        try {
            spaceSheet = javax.imageio.ImageIO.read(new java.io.File("./keys/SPACE.png"));
            if (spaceSheet != null) {
                int fw = spaceSheet.getWidth() / 2;
                int fh = spaceSheet.getHeight();
                spaceFrameNormal = spaceSheet.getSubimage(0, 0, fw, fh);
                spaceFramePressed = spaceSheet.getSubimage(fw, 0, fw, fh);
            }
        } catch (Exception ignored) { }
        try {
            backspaceSheet = javax.imageio.ImageIO.read(new java.io.File("./keys/BACKSPACE.png"));
            if (backspaceSheet != null) {
                int fw = Math.max(1, backspaceSheet.getWidth() / 2);
                int fh = backspaceSheet.getHeight();
                backspaceFrameNormal = backspaceSheet.getSubimage(0, 0, fw, fh);
                backspaceFramePressed = backspaceSheet.getSubimage(Math.min(fw, backspaceSheet.getWidth()-1), 0, fw, fh);
            }
        } catch (Exception ignored) { }
    }

    private void initCursor() {
        try {
            Image raw = javax.imageio.ImageIO.read(new java.io.File("./keys/hand_small_point.png"));
            int target = 24;
            int w = raw.getWidth(null), h = raw.getHeight(null);
            float s = Math.min(target / (float) w, target / (float) h);
            int nw = Math.max(12, Math.round(w * s));
            int nh = Math.max(12, Math.round(h * s));
            Image scaled = raw.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
            BufferedImage buf = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = buf.createGraphics();
            gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gg.drawImage(scaled, 0, 0, null);
            gg.dispose();
            Point hot = new Point(Math.max(1, nw / 6), Math.max(1, nh / 8));
            Toolkit tk = Toolkit.getDefaultToolkit();
            Dimension best = tk.getBestCursorSize(nw, nh);
            if (best != null && best.width > 0 && best.height > 0 && (best.width != nw || best.height != nh)) {
                Image res = buf.getScaledInstance(best.width, best.height, Image.SCALE_SMOOTH);
                BufferedImage adj = new BufferedImage(best.width, best.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = adj.createGraphics();
                g2.drawImage(res, 0, 0, null);
                g2.dispose();
                hot = new Point(Math.round(hot.x * (best.width / (float) nw)), Math.round(hot.y * (best.height / (float) nh)));
                buf = adj;
            }
            Cursor customCursor = tk.createCustomCursor(buf, hot, "customCursor");
            setCursor(customCursor);
        } catch (Exception ignored) { }
    }

    private void initMouse() {
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (showSettings) return;
                playIfAudible(sClick);
                hit.playAt(e.getX(), e.getY());
            }
            @Override public void mousePressed(MouseEvent e) {
                if (!showSettings) return;
                if (toggleRect != null && toggleRect.contains(e.getPoint())) {
                    ttsEnabled = !ttsEnabled;
                    repaint();
                }
                if (speedRectSlow != null && speedRectSlow.contains(e.getPoint())) {
                    setTtsSpeedLevel(0);
                } else if (speedRectNormal != null && speedRectNormal.contains(e.getPoint())) {
                    setTtsSpeedLevel(1);
                } else if (speedRectFast != null && speedRectFast.contains(e.getPoint())) {
                    setTtsSpeedLevel(2);
                }
                if (sliderKnob != null) {
                    Rectangle big = new Rectangle(sliderKnob.x - 6, sliderKnob.y - 6, sliderKnob.width + 12, sliderKnob.height + 12);
                    if (big.contains(e.getPoint())) draggingSlider = true;
                }
                if (sliderTrack != null && sliderTrack.contains(e.getPoint())) {
                    updateSliderFromMouse(e.getX());
                    draggingSlider = true;
                }
            }
            @Override public void mouseReleased(MouseEvent e) { draggingSlider = false; }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (showSettings) {
                    if (draggingSlider) updateSliderFromMouse(e.getX());
                    return;
                }
                hit.playAt(e.getX(), e.getY());
            }
        });
    }

    private void warmupAtlas() {
        try {
            String set = GameConfig.CHAR_SET == null ? "" : GameConfig.CHAR_SET;
            for (char c : set.toCharArray()) { atlas.getNormal(c); atlas.getPressed(c); }
            for (char c = 'A'; c <= 'Z'; c++) { atlas.getNormal(c); atlas.getPressed(c); }
            for (char c = '0'; c <= '9'; c++) { atlas.getNormal(c); atlas.getPressed(c); }
            atlas.getNormal(' '); atlas.getPressed(' ');
        } catch (Exception ignored) { }
    }

    private void layoutSettingsRects() {
        int panelW = Math.max(360, Math.min(480, getWidth() - 220));
        int panelH = 270;
        int x = (getWidth() - panelW) / 2;
        int y = (getHeight() - panelH) / 2;
        panelRect = new Rectangle(x, y, panelW, panelH);
        toggleRect = new Rectangle(x + 30, y + 70, 64, 30);
        int trackLeft = x + 30;
        int trackRight = x + panelW - 90;
        int trackW = Math.max(140, trackRight - trackLeft);
        sliderTrack = new Rectangle(trackLeft, y + 140, trackW, 10);
        int centerX = sliderTrack.x + (int) Math.round((masterVolume / 100.0) * sliderTrack.width);
        int knobW = 18, knobH = 24;
        sliderKnob = new Rectangle(centerX - knobW / 2, sliderTrack.y - (knobH - sliderTrack.height) / 2, knobW, knobH);
        int btnW = (panelW - 60 - 20) / 3;
        int btnH = 32;
        int by = y + 190;
        speedRectSlow = new Rectangle(x + 30, by, btnW, btnH);
        speedRectNormal = new Rectangle(x + 30 + btnW + 10, by, btnW, btnH);
        speedRectFast = new Rectangle(x + 30 + (btnW + 10) * 2, by, btnW, btnH);
    }

    private void updateSliderFromMouse(int mouseX) {
        if (sliderTrack == null) return;
        int clamped = Math.max(sliderTrack.x, Math.min(sliderTrack.x + sliderTrack.width, mouseX));
        double t = (clamped - sliderTrack.x) / (double) sliderTrack.width;
        masterVolume = (int) Math.round(t * 100.0);
        sliderKnob.x = clamped - sliderKnob.width / 2;
        applyVolumeToPools();
        repaint();
    }

    private void applyVolumeToPools() {
        try {
            float v = masterVolume / 100f;
            sStart.setVolume(v);
            sType.setVolume(v);
            sErr.setVolume(v);
            sClick.setVolume(v);
        } catch (Throwable ignored) { }
    }

    private boolean isAudible() { return masterVolume > 0; }

    private void playIfAudible(SoundPool p) { if (isAudible()) p.play(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        long now = System.currentTimeMillis();
        if (now < flashUntil) { g2.setColor(CLR_FLASH); g2.fillRect(0, 0, getWidth(), getHeight()); }
        if (now < bonusUntil) { g2.setColor(CLR_BONUS_TINT); g2.fillRect(0, 0, getWidth(), getHeight()); }
        drawHUD(g2, now);
        drawCharacter(g2);
        int shakeX = 0, shakeY = 0;
        if (now < shakeUntil) { shakeX = rng.nextInt(shakeAmp * 2 + 1) - shakeAmp; shakeY = rng.nextInt(shakeAmp * 2 + 1) - shakeAmp; }
        g2.translate(shakeX, shakeY);
        drawWord(g2, now);
        g2.translate(-shakeX, -shakeY);
        drawFooterInfo(g2);
        if (state == GameConfig.State.PLAYING) drawBars(g2);
        hit.draw(g2);
        if (showSettings) drawSettingsOverlay(g2);
        g2.dispose();
    }

    private void drawCharacter(Graphics2D g2) {
        BufferedImage frame = switch (currentAnim) {
            case IDLE -> charPack.idle.current();
            case WALK -> charPack.walk.current();
            case ATTACK -> charPack.attack.current();
            case TAKE_HIT -> charPack.hit.current();
            case DEATH -> charPack.death.current();
        };
        int th = Math.min(240, getHeight() / 3);
        int tw = (int) ((frame.getWidth() / (double) frame.getHeight()) * th);
        int x = 40;
        int y = getHeight() - th - 40;
        g2.drawImage(frame, x, y, tw, th, null);
    }

    private void drawSettingsOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (panelRect == null) layoutSettingsRects();
        g2.setColor(new Color(24, 28, 34));
        g2.fillRoundRect(panelRect.x, panelRect.y, panelRect.width, panelRect.height, 16, 16);
        g2.setColor(new Color(255, 255, 255, 80));
        g2.drawRoundRect(panelRect.x, panelRect.y, panelRect.width, panelRect.height, 16, 16);
        g2.setFont(fontBold20);
        g2.setColor(Color.WHITE);
        centerTextAt(g2, "ตั้งค่าเสียง", panelRect.x + panelRect.width / 2, panelRect.y + 36);
        g2.setFont(fontPlain16);
        g2.setColor(new Color(220, 220, 220));
        g2.drawString("TTS", panelRect.x + 30, panelRect.y + 60);
        int tx = toggleRect.x, ty = toggleRect.y, tw = toggleRect.width, th = toggleRect.height;
        g2.setColor(ttsEnabled ? new Color(70, 170, 110) : new Color(90, 90, 90));
        g2.fillRoundRect(tx, ty, tw, th, th, th);
        int knob = th - 6;
        int kx = ttsEnabled ? tx + tw - knob - 3 : tx + 3;
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(kx, ty + 3, knob, knob, knob, knob);
        g2.setColor(new Color(220, 220, 220));
        g2.drawString("ระดับเสียงรวม", panelRect.x + 30, panelRect.y + 120);
        g2.setColor(new Color(60, 60, 60));
        g2.fillRoundRect(sliderTrack.x, sliderTrack.y, sliderTrack.width, sliderTrack.height, 8, 8);
        g2.setColor(new Color(255, 255, 255, 100));
        g2.drawRoundRect(sliderTrack.x, sliderTrack.y, sliderTrack.width, sliderTrack.height, 8, 8);
        int fill = (int) Math.round((masterVolume / 100.0) * sliderTrack.width);
        fill = Math.max(0, Math.min(sliderTrack.width, fill));
        g2.setColor(new Color(100, 200, 255));
        g2.fillRoundRect(sliderTrack.x, sliderTrack.y, fill, sliderTrack.height, 8, 8);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(sliderKnob.x, sliderKnob.y, sliderKnob.width, sliderKnob.height, 10, 10);
        g2.setFont(fontSmall12);
        g2.setColor(Color.WHITE);
        g2.drawString(masterVolume + "%", sliderTrack.x + sliderTrack.width + 10, sliderTrack.y + 14);
        g2.setFont(fontPlain16);
        g2.setColor(new Color(220, 220, 220));
        g2.drawString("ความเร็ว TTS", panelRect.x + 30, panelRect.y + 180 - 6);
        drawSpeedButton(g2, speedRectSlow, "ช้า", ttsSpeedLevel == 0);
        drawSpeedButton(g2, speedRectNormal, "ปกติ", ttsSpeedLevel == 1);
        drawSpeedButton(g2, speedRectFast, "เร็ว", ttsSpeedLevel == 2);
        g2.setFont(fontSmall11);
        g2.setColor(new Color(200, 200, 200));
        centerTextAt(g2, "กด ESC เพื่อปิด", panelRect.x + panelRect.width / 2, panelRect.y + panelRect.height - 16);
    }

    private void drawSpeedButton(Graphics2D g2, Rectangle r, String label, boolean active) {
        g2.setColor(active ? new Color(70,170,110) : new Color(60,60,60));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g2.setColor(new Color(255,255,255,100));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setFont(fontBold16);
        FontMetrics fm = g2.getFontMetrics();
        int cx = r.x + (r.width - fm.stringWidth(label)) / 2;
        int cy = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2 - 2;
        g2.drawString(label, cx, cy);
    }

    private void drawHUD(Graphics2D g2, long now) {
        long left = state == GameConfig.State.PLAYING ? Math.max(0, GameConfig.ROUND_SECONDS * 1000L - (now - startMs)) : GameConfig.ROUND_SECONDS * 1000L;
        int mm = (int) (left / 1000) / 60;
        int ss = (int) (left / 1000) % 60;
        double minutes = state == GameConfig.State.PLAYING ? Math.max(1e-6, (now - startMs) / 60000.0) : 0;
        double wpm = minutes > 0 ? (correct / 5.0) / minutes : 0.0;
        double acc = totalTyped == 0 ? 100.0 : 100.0 * (totalTyped - mistakes) / totalTyped;
        if (state == GameConfig.State.PLAYING) {
            drawGameHUD(g2, mm, ss, wpm, acc);
            drawHealthBar(g2);
            drawBonusTimer(g2, now);
        }
        g2.setFont(fontSmall12);
        g2.setColor(CLR_HUD_TEXT);
        g2.drawString("FPS: " + fps, getWidth() - 75, getHeight() - 20);
        g2.drawString("@Hex | version dev.", 10, getHeight() - 20);
        g2.setFont(fontPlain6);
    }

    private void drawGameHUD(Graphics2D g2, int mm, int ss, double wpm, double acc) {
        g2.setFont(fontBold16);
        g2.setColor(CLR_TIME);
        g2.drawString(String.format("เวลา: %02d:%02d", mm, ss), 28, 42);
        g2.setColor(CLR_WPM);
        g2.drawString(String.format(Locale.US, "WPM: %.1f", wpm), 28, 70);
        g2.setColor(CLR_ACC);
        g2.drawString(String.format(Locale.US, "ความแม่นยำ: %.0f%%", acc), 28, 95);
        g2.setColor(CLR_DONE);
        g2.drawString(String.format("คำที่ทำได้: %d", wordsCompleted), 28, 120);
        if (bonusStreak > 0) {
            g2.setColor(CLR_BONUS);
            g2.drawString(String.format("โบนัส: x%d", bonusStreak), 28, 148);
        }
    }

    private void drawHealthBar(Graphics2D g2) {
        int barX = getWidth() - 220;
        int barY = 20;
        int barW = 180;
        int barH = 20;
        g2.setColor(CLR_HUD_PANEL);
        g2.fillRoundRect(barX - 4, barY - 4, barW + 8, barH + 8, 12, 12);
        g2.setColor(new Color(60, 60, 60));
        g2.fillRoundRect(barX, barY, barW, barH, 8, 8);
        double healthPercent = health / (double) GameConfig.MAX_HEALTH;
        int healthW = (int) (barW * healthPercent);
        Color healthColor = healthPercent > 0.6 ? new Color(100, 200, 100) : healthPercent > 0.3 ? new Color(255, 200, 100) : new Color(255, 100, 100);
        g2.setColor(healthColor);
        g2.fillRoundRect(barX, barY, healthW, barH, 8, 8);
        g2.setColor(CLR_BAR_BG_OUTLINE);
        g2.drawRoundRect(barX, barY, barW, barH, 8, 8);
        g2.setFont(fontSmall12);
        g2.setColor(CLR_HUD_TEXT);
        String healthText = String.format("HP %d/%d", health, GameConfig.MAX_HEALTH);
        FontMetrics fm = g2.getFontMetrics();
        int textX = barX + (barW - fm.stringWidth(healthText)) / 2;
        g2.drawString(healthText, textX, barY + 16);
    }

    private void drawBonusTimer(Graphics2D g2, long now) {
        if (state != GameConfig.State.PLAYING || wordStartMs == 0) return;
        long elapsed = now - wordStartMs;
        if (elapsed >= BONUS_TIME_MS) return;
        double timeLeft = (BONUS_TIME_MS - elapsed) / (double) BONUS_TIME_MS;
        int timerX = getWidth() - 220;
        int timerY = 65;
        int timerW = 180;
        int timerH = 10;
        g2.setColor(CLR_HUD_PANEL);
        g2.fillRoundRect(timerX - 2, timerY - 2, timerW + 4, timerH + 4, 8, 8);
        g2.setColor(CLR_PROG_BG);
        g2.fillRoundRect(timerX, timerY, timerW, timerH, 6, 6);
        int bonusW = (int) (timerW * timeLeft);
        g2.setColor(CLR_TIME);
        g2.fillRoundRect(timerX, timerY, bonusW, timerH, 6, 6);
        g2.setFont(fontSmall11);
        g2.setColor(CLR_TIME);
        g2.drawString("BONUS TIME", timerX, timerY - 4);
    }

    private void drawWord(Graphics2D g2, long now) {
        if (current == null || current.word.isEmpty()) return;
        Dimension keyDim = atlas.keySize();
        int keyW = keyDim.width * GameConfig.SCALE;
        int keyH = keyDim.height * GameConfig.SCALE;
        int totalW = current.word.length() * keyW + (current.word.length() - 1) * GameConfig.KEY_SPACING;
        double scale = (now < popUntil) ? 1.06 : 1.0;
        int cx = getWidth() / 2, cy = getHeight() / 2 - 60;
        int x0 = cx - (int) (totalW * scale) / 2;
        int y = cy - (int) (keyH * scale) / 2;
        Graphics2D gg = (Graphics2D) g2.create();
        gg.translate(cx, cy); gg.scale(scale, scale); gg.translate(-cx, -cy);
        for (int i = 0; i < current.word.length(); i++) {
            char ch = current.word.charAt(i);
            int x = x0 + i * (keyW + GameConfig.KEY_SPACING);
            gg.setColor(CLR_BOX_SHADOW);
            gg.fillRoundRect(x - 6, y - 6, keyW + 12, keyH + 12, 18, 18);
            drawWordInfo(g2, cx, cy - 80);
            BufferedImage img = (i < idx) ? atlas.getPressed(ch) : atlas.getNormal(ch);
            gg.drawImage(img, x, y, keyW, keyH, null);
            if (i == idx && state == GameConfig.State.PLAYING) {
                Stroke old = gg.getStroke();
                gg.setStroke(new BasicStroke(3f));
                gg.setColor(new Color(255, 215, 0, 210));
                gg.drawRoundRect(x - 3, y - 3, keyW + 6, keyH + 6, 12, 12);
                gg.setStroke(old);
            }
        }
        gg.dispose();
    }

    private void drawWordInfo(Graphics2D g2, int cx, int cy) {
        if (current == null) return;
        String title = current.display.toUpperCase(Locale.US);
        String sub1 = current.pronun;
        String sub2 = current.meaning;
        int boxW = Math.min(500, getWidth() - 40);
        int boxX = cx - boxW / 2;
        int boxH = 70;
        int boxY = cy - boxH / 2;
        g2.setColor(CLR_HUD_PANEL);
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 15, 15);
        g2.setColor(Color.WHITE);
        g2.setFont(fontBold20);
        centerTextAt(g2, title, cx, cy - 15);
        g2.setColor(new Color(220, 220, 220));
        g2.setFont(fontPlain16);
        centerTextAt(g2, sub1 + " • " + sub2, cx, cy + 8);
    }

    private void drawFooterInfo(Graphics2D g2) {
        if (state == GameConfig.State.READY) {
            drawSpacePrompt(g2, "กด ", " เพื่อเริ่มเกม");
            drawBackspacePrompt(g2, "กด ", " เพื่อตั้งค่า");
        } else if (state == GameConfig.State.GAMEOVER) {
            drawSpacePrompt(g2, "กด ", " เพื่อเริ่มใหม่");
            drawBackspacePrompt(g2, "กด ", " เพื่อตั้งค่า");
        }
    }

    private void drawSpacePrompt(Graphics2D g2, String pre, String post) {
        BufferedImage frame = isSpaceHeld && spaceFramePressed != null ? spaceFramePressed : (spaceAnimOn && spaceFramePressed != null ? spaceFramePressed : spaceFrameNormal);
        g2.setFont(fontBold20);
        FontMetrics fm = g2.getFontMetrics();
        int spriteH = 26;
        int spriteW = (frame != null) ? (int) (frame.getWidth() * (spriteH / (double) frame.getHeight())) : 0;
        String preText = pre;
        String postText = post;
        int totalW = fm.stringWidth(preText) + spriteW + fm.stringWidth(postText);
        int y = getHeight() - 90;
        int x = (getWidth() - totalW) / 2;
        g2.setColor(Color.WHITE);
        g2.drawString(preText, x, y);
        int curX = x + fm.stringWidth(preText);
        if (frame != null) {
            g2.drawImage(frame, curX, y - spriteH + 3, spriteW, spriteH, null);
            curX += spriteW;
        }
        g2.drawString(postText, curX, y);
    }

    private void drawBackspacePrompt(Graphics2D g2, String pre, String post) {
        BufferedImage frame = backspaceFrameNormal != null ? backspaceFrameNormal : null;
        g2.setFont(fontBold16);
        FontMetrics fm = g2.getFontMetrics();
        int spriteH = 22;
        int spriteW = (frame != null) ? (int) (frame.getWidth() * (spriteH / (double) frame.getHeight())) : 0;
        String preText = pre;
        String postText = post;
        int totalW = fm.stringWidth(preText) + spriteW + fm.stringWidth(postText);
        int y = getHeight() - 60;
        int x = (getWidth() - totalW) / 2;
        g2.setColor(Color.WHITE);
        g2.drawString(preText, x, y);
        int curX = x + fm.stringWidth(preText);
        if (frame != null) {
            g2.drawImage(frame, curX, y - spriteH + 2, spriteW, spriteH, null);
            curX += spriteW;
        }
        g2.drawString(postText, curX, y);
    }

    private void drawBars(Graphics2D g2) {
        int barW = Math.min((int) (getWidth() * 0.45), 400);
        int x = (getWidth() - barW) / 2;
        int topY = getHeight() - 100;
        int h = 12;
        double p = current != null && current.word.length() > 0 ? (idx / (double) current.word.length()) : 0;
        drawProgressBar(g2, x, topY, barW, h, p, "ความคืบหน้าคำ", new Color(70, 170, 110), new Color(50, 120, 80));
    }

    private void drawProgressBar(Graphics2D g2, int x, int y, int w, int h, double progress, String label, Color color1, Color color2) {
        g2.setColor(CLR_BAR_FRAME);
        g2.fillRoundRect(x - 2, y - 2, w + 4, h + 4, 16, 16);
        g2.setColor(CLR_PROG_BG);
        g2.fillRoundRect(x, y, w, h, 12, 12);
        if (progress > 0) {
            Paint old = g2.getPaint();
            g2.setPaint(new GradientPaint(x, y, color1, x + w, y + h, color2));
            g2.fillRoundRect(x, y, (int) (w * progress), h, 12, 12);
            g2.setPaint(new GradientPaint(x, y, new Color(255, 255, 255, 60), x, y + h / 2, new Color(255, 255, 255, 20)));
            g2.fillRoundRect(x, y, (int) (w * progress), h / 2, 12, 12);
            g2.setPaint(old);
        }
        g2.setColor(new Color(255, 255, 255, 80));
        g2.drawRoundRect(x, y, w, h, 12, 12);
        g2.setFont(fontSmall12);
        g2.setColor(CLR_PROG_LABEL);
        FontMetrics fm = g2.getFontMetrics();
        int labelX = x + (w - fm.stringWidth(label)) / 2;
        g2.drawString(label, labelX, y - 6);
    }

    private void centerText(Graphics2D g2, String s, int y) {
        FontMetrics fm = g2.getFontMetrics();
        int cx = (getWidth() - fm.stringWidth(s)) / 2;
        g2.drawString(s, cx, y);
    }

    private void centerTextAt(Graphics2D g2, String s, int x, int y) {
        FontMetrics fm = g2.getFontMetrics();
        int cx = x - fm.stringWidth(s) / 2;
        g2.drawString(s, cx, y);
    }

    private void setTtsSpeedLevel(int level) {
        level = Math.max(0, Math.min(2, level));
        if (ttsSpeedLevel == level) return;
        ttsSpeedLevel = level;
        if (ttsEnabled) {
            new Thread(() -> {
                try {
                    SlowTTS tts = new SlowTTS("kevin16", ttsRates[ttsSpeedLevel]);
                    tts.speak("Hello Hello");
                    tts.close();
                } catch (Exception ignored) { }
            }).start();
        }
        repaint();
    }

    private void nextWord() {
        if (wordBank.isEmpty()) {
            current = new WordEntry("HELLO", "HELLO", "เฮลโล", "สวัสดี");
        } else {
            WordEntry newWord;
            do { newWord = wordBank.get(rng.nextInt(wordBank.size())); }
            while (wordBank.size() > 1 && newWord.word.equals(current.word));
            current = newWord;
        }
        idx = 0;
        wordStartMs = System.currentTimeMillis();
        if (ttsEnabled) {
            new Thread(() -> {
                try {
                    SlowTTS tts = new SlowTTS("kevin16", ttsRates[ttsSpeedLevel]);
                    tts.speak(current.word);
                    tts.close();
                } catch (Exception ignored) { }
            }).start();
        }
        if (state == GameConfig.State.PLAYING) setAnim(Anim.IDLE, 0);
    }

    private void resetRun() {
        mistakes = correct = totalTyped = wordsCompleted = bonusStreak = 0;
        health = GameConfig.MAX_HEALTH;
        nextWord();
        startMs = System.currentTimeMillis();
        state = GameConfig.State.PLAYING;
        setAnim(Anim.IDLE, 0);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (code >= 0 && code < lastPressAt.length) {
            long now = System.currentTimeMillis();
            if (now - lastPressAt[code] < GameConfig.DEBOUNCE_MS) return;
            lastPressAt[code] = now;
        }

        if (code == KeyEvent.VK_ESCAPE) {
            showSettings = !showSettings;
            repaint();
            return;
        }

        if (state == GameConfig.State.READY) {
            if (code == KeyEvent.VK_SPACE) {
                isSpaceHeld = true;
                resetRun();
                return;
            }
            if (code == KeyEvent.VK_BACK_SPACE) {
                showSettings = !showSettings;
                repaint();
                return;
            }
            return;
        }

        if (state == GameConfig.State.GAMEOVER) {
            if (code == KeyEvent.VK_SPACE) {
                isSpaceHeld = true;
                resetRun();
                return;
            }
            if (code == KeyEvent.VK_BACK_SPACE) {
                showSettings = !showSettings;
                repaint();
                return;
            }
            return;
        }

        if (state != GameConfig.State.PLAYING) return;

        if (code == KeyEvent.VK_SPACE) {
            isSpaceHeld = true;
            return;
        }

        if (code == KeyEvent.VK_BACK_SPACE) {
            if (idx > 0) {
                idx--;
                playIfAudible(sStart);
            }
            return;
        }

        if (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z) {
            handleChar((char) ('A' + code - KeyEvent.VK_A));
        } else if (code >= KeyEvent.VK_0 && code <= KeyEvent.VK_9) {
            handleChar((char) ('0' + code - KeyEvent.VK_0));
        }
    }

    @Override public void keyTyped(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) isSpaceHeld = false;
    }

    private void handleChar(char c) {
        totalTyped++;
        if (idx < current.word.length() && c == current.word.charAt(idx)) {
            playIfAudible(sType);
            idx++;
            correct++;
            popUntil = System.currentTimeMillis() + 90;
            if (idx >= current.word.length()) {
                wordsCompleted++;
                setAnim(Anim.ATTACK, 0);
                pendingNextWord = true;
                checkBonus();
            } else {
                setAnim(Anim.IDLE, 0);
            }
        } else {
            mistakes++;
            bonusStreak = 0;
            playIfAudible(sErr);
            long now = System.currentTimeMillis();
            flashUntil = now + 120;
            shakeUntil = now + 220;
            shakeAmp = 2;
            health = Math.max(0, health - 1);
            setAnim(Anim.TAKE_HIT, 350);
            if (health <= 0) {
                state = GameConfig.State.GAMEOVER;
                current = new WordEntry("OVER", "OVER", "โอเวอร์", "จบ");
                idx = 0;
                setAnim(Anim.DEATH, 0);
            }
        }
    }

    private void checkBonus() {
        long elapsed = System.currentTimeMillis() - wordStartMs;
        if (elapsed <= BONUS_TIME_MS) {
            bonusStreak++;
            bonusUntil = System.currentTimeMillis() + 1500;
            if (bonusStreak % 3 == 0) {
                health = Math.min(GameConfig.MAX_HEALTH, health + 1);
                popUntil = System.currentTimeMillis() + 200;
            }
        } else {
            bonusStreak = 0;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.currentTimeMillis();
        if (state == GameConfig.State.PLAYING) {
            if (now - startMs >= GameConfig.ROUND_SECONDS * 1000L) {
                state = GameConfig.State.GAMEOVER;
                current = new WordEntry("OVER", "OVER", "", "");
                idx = 0;
                setAnim(Anim.DEATH, 0);
            }
        }
        if (state == GameConfig.State.READY || state == GameConfig.State.GAMEOVER) {
            if (now - spaceAnimLast >= SPACE_ANIM_MS) { spaceAnimOn = !spaceAnimOn; spaceAnimLast = now; }
        } else {
            spaceAnimOn = false;
        }
        if (state == GameConfig.State.PLAYING) {
            if (currentAnim == Anim.ATTACK) {
                if (pendingNextWord && charPack.attack.finishedOnce()) {
                    pendingNextWord = false;
                    nextWord();
                    setAnim(Anim.IDLE, 0);
                }
            } else if (animUntil > 0 && now > animUntil) {
                setAnim(Anim.IDLE, 0);
            }
        }
        if (showSettings && sliderKnob != null && draggingSlider) {
            int clamped = Math.max(sliderTrack.x, Math.min(sliderTrack.x + sliderTrack.width, sliderKnob.x + sliderKnob.width / 2));
            sliderKnob.x = clamped - sliderKnob.width / 2;
        }
        frames++;
        if (lastFpsTime == 0) lastFpsTime = now;
        if (now - lastFpsTime >= 1000) { fps = frames; frames = 0; lastFpsTime = now; }
        repaint();
    }

}

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GamePanel extends JPanel implements KeyListener, ActionListener {
    private final Timer timer = new Timer(16, this);
    private final Random rng = new Random();
    private final KeyAtlas atlas = new KeyAtlas(GameConfig.ASSETS_DIR, GameConfig.FILE_EXT, GameConfig.TWO_FRAMES_PER_FILE, GameConfig.CHAR_SET);
    private final SoundFX sfx = new SoundFX(GameConfig.CLICK_WAV_PATH);
    private final List<WordEntry> wordBank = WordBank.loadWordBank();

    private GameConfig.State state = GameConfig.State.READY;
    private WordEntry current = new WordEntry("HELLO", "HELLO", "เฮลโล", "สวัสดี");
    private int idx = 0;
    private int mistakes = 0, correct = 0, totalTyped = 0;
    private int wordsCompleted = 0;
    private long startMs = 0;
    private long flashUntil = 0;
    private long shakeUntil = 0;
    private int shakeAmp = 0;
    private long popUntil = 0;
    private final long[] lastPressAt = new long[256];
    private int health = GameConfig.MAX_HEALTH;

    public GamePanel() {
        setPreferredSize(new Dimension(1100, 620));
        setBackground(new Color(18, 20, 24));
        setFocusable(true);
        addKeyListener(this);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (System.currentTimeMillis() < flashUntil) {
            g2.setColor(new Color(180, 30, 40, 80));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        
        drawHUD(g2);
        
        int shakeX = 0, shakeY = 0;
        if (System.currentTimeMillis() < shakeUntil) {
            shakeX = rng.nextInt(shakeAmp * 2 + 1) - shakeAmp;
            shakeY = rng.nextInt(shakeAmp * 2 + 1) - shakeAmp;
        }
        g2.translate(shakeX, shakeY);
        drawWord(g2);
        g2.translate(-shakeX, -shakeY);
        
        drawFooterInfo(g2);
        
        if (state == GameConfig.State.PLAYING) {
            drawBars(g2);
        }
        
        if (state == GameConfig.State.READY) splash(g2, "ENTER เพื่อเริ่ม");
        else if (state == GameConfig.State.GAMEOVER) splash(g2, "เกมจบ • ENTER เพื่อเริ่มใหม่");
        
        g2.dispose();
    }

    private void drawHUD(Graphics2D g2) {
        long left = state == GameConfig.State.PLAYING ? Math.max(0, GameConfig.ROUND_SECONDS * 1000L - (System.currentTimeMillis() - startMs)) : GameConfig.ROUND_SECONDS * 1000L;
        int mm = (int) (left / 1000) / 60;
        int ss = (int) (left / 1000) % 60;
        double minutes = state == GameConfig.State.PLAYING ? Math.max(1e-6, (System.currentTimeMillis() - startMs) / 60000.0) : 0;
        double wpm = minutes > 0 ? (correct / 5.0) / minutes : 0.0;
        double acc = totalTyped == 0 ? 100.0 : 100.0 * (totalTyped - mistakes) / totalTyped;
        
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.setColor(new Color(235, 235, 235));
        g2.drawString(String.format("เวลา: %02d:%02d", mm, ss), 24, 32);
        g2.drawString(String.format(Locale.US, "WPM: %.1f", wpm), 24, 56);
        g2.drawString(String.format(Locale.US, "ความแม่นยำ: %.0f%%", acc), 24, 80);
        g2.drawString(String.format("คำที่ทำได้: %d", wordsCompleted), 24, 104);
        
        g2.setColor(new Color(170, 170, 170));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.drawString("พิมพ์ตามคีย์ให้ถูกต้อง", 24, 128);
    }

    private void splash(Graphics2D g2, String msg) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 30));
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(msg)) / 2;
        int y = getHeight() / 2 + 120;
        g2.setColor(new Color(0, 0, 0, 100));
        g2.drawString(msg, x + 2, y + 2);
        g2.setColor(new Color(240, 240, 240));
        g2.drawString(msg, x, y);
    }

    private void drawWord(Graphics2D g2) {
        if (current == null || current.word.isEmpty()) return;
        
        Dimension keyDim = atlas.keySize();
        int keyW = keyDim.width * GameConfig.SCALE;
        int keyH = keyDim.height * GameConfig.SCALE;
        int totalW = current.word.length() * keyW + (current.word.length() - 1) * GameConfig.KEY_SPACING;
        double scale = (System.currentTimeMillis() < popUntil) ? 1.06 : 1.0;
        int cx = getWidth() / 2, cy = getHeight() / 2 - 60;
        int x0 = cx - (int) (totalW * scale) / 2;
        int y = cy - (int) (keyH * scale) / 2;
        
        drawWordInfo(g2, cx, cy - 80);
        
        Graphics2D gg = (Graphics2D) g2.create();
        gg.translate(cx, cy);
        gg.scale(scale, scale);
        gg.translate(-cx, -cy);
        
        for (int i = 0; i < current.word.length(); i++) {
            char ch = current.word.charAt(i);
            int x = x0 + i * (keyW + GameConfig.KEY_SPACING);
            gg.setColor(new Color(0, 0, 0, 70));
            gg.fillRoundRect(x - 6, y - 6, keyW + 12, keyH + 12, 18, 18);
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
        
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 15, 15);
        
        g2.setColor(new Color(255, 255, 255));
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        centerTextAt(g2, title, cx, cy - 15);
        
        g2.setColor(new Color(220, 220, 220));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        centerTextAt(g2, sub1 + " • " + sub2, cx, cy + 8);
    }
    
    private void drawFooterInfo(Graphics2D g2) {
        int y = getHeight() - 25;
        g2.setColor(new Color(140, 140, 140));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        centerText(g2, "กด ENTER เพื่อเริ่ม/เริ่มใหม่ • กด BACKSPACE เพื่อลบ", y);
    }

    private void drawBars(Graphics2D g2) {
        int barW = Math.min((int) (getWidth() * 0.45), 400);
        int x = (getWidth() - barW) / 2;
        int topY = getHeight() - 150;
        int gap = 20;
        int h = 12;
        double p = current != null && current.word.length() > 0 ? (idx / (double) current.word.length()) : 0;
        
        drawProgressBar(g2, x, topY, barW, h, p, "ความคืบหน้าคำ", 
                       new Color(70, 170, 110), new Color(50, 120, 80));
        
        int y2 = topY + h + gap + 20;
        double hp = Math.max(0, Math.min(1.0, health / (double) GameConfig.MAX_HEALTH));
        drawProgressBar(g2, x, y2, barW, h, hp, "พลังชีวิต", 
                       new Color(210, 80, 80), new Color(150, 40, 40));
    }
    
    private void drawProgressBar(Graphics2D g2, int x, int y, int w, int h, double progress, 
                                String label, Color color1, Color color2) {
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(x - 2, y - 2, w + 4, h + 4, 16, 16);
        
        g2.setColor(new Color(40, 40, 40));
        g2.fillRoundRect(x, y, w, h, 12, 12);
        
        if (progress > 0) {
            Paint old = g2.getPaint();
            g2.setPaint(new GradientPaint(x, y, color1, x + w, y + h, color2));
            g2.fillRoundRect(x, y, (int) (w * progress), h, 12, 12);
            
            g2.setPaint(new GradientPaint(x, y, new Color(255, 255, 255, 60), 
                                        x, y + h/2, new Color(255, 255, 255, 20)));
            g2.fillRoundRect(x, y, (int) (w * progress), h/2, 12, 12);
            g2.setPaint(old);
        }
        
        g2.setColor(new Color(255, 255, 255, 80));
        g2.drawRoundRect(x, y, w, h, 12, 12);
        
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(new Color(200, 200, 200));
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

    private void nextWord() {
        current = wordBank.isEmpty() ? new WordEntry("HELLO", "HELLO", "เฮลโล", "สวัสดี") : wordBank.get(rng.nextInt(wordBank.size()));
        idx = 0;
    }

    private void resetRun() {
        mistakes = correct = totalTyped = wordsCompleted = 0;
        health = GameConfig.MAX_HEALTH;
        nextWord();
        startMs = System.currentTimeMillis();
        state = GameConfig.State.PLAYING;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < lastPressAt.length) {
            long now = System.currentTimeMillis();
            if (now - lastPressAt[code] < GameConfig.DEBOUNCE_MS) return;
            lastPressAt[code] = now;
        }
        
        if (code == KeyEvent.VK_ENTER) {
            if (state == GameConfig.State.READY || state == GameConfig.State.GAMEOVER) resetRun();
            return;
        }
        
        if (state != GameConfig.State.PLAYING) return;
        
        if (code == KeyEvent.VK_BACK_SPACE) {
            if (idx > 0) {
                idx--;
                sfx.type();
            }
            return;
        }
        
        if (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z) {
            handleChar((char) ('A' + code - KeyEvent.VK_A));
        } else if (code >= KeyEvent.VK_0 && code <= KeyEvent.VK_9) {
            handleChar((char) ('0' + code - KeyEvent.VK_0));
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    private void handleChar(char c) {
        sfx.type();
        totalTyped++;
        
        if (idx < current.word.length() && c == current.word.charAt(idx)) {
            idx++;
            correct++;
            popUntil = System.currentTimeMillis() + 90;
            if (idx >= current.word.length()) {
                wordsCompleted++;
                nextWord();
            }
        } else {
            mistakes++;
            long now = System.currentTimeMillis();
            flashUntil = now + 120;
            shakeUntil = now + 220;
            shakeAmp = 2;
            health = Math.max(0, health - 1);
            if (health <= 0) state = GameConfig.State.GAMEOVER;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameConfig.State.PLAYING) {
            if (System.currentTimeMillis() - startMs >= GameConfig.ROUND_SECONDS * 1000L) {
                state = GameConfig.State.GAMEOVER;
            }
        }
        repaint();
    }
}
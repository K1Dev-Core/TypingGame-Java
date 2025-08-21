import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Locale;

public class GameRenderer {
    private final GamePanel gamePanel;

    private final GameState gameState;
    private final UISettings uiSettings;

    private static final Color CLR_FLASH = new Color(180, 30, 40, 80);
    private static final Color CLR_BOX_SHADOW = new Color(0, 0, 0, 70);
    private static final Color CLR_HUD_PANEL = new Color(0, 0, 0, 120);
    private static final Color CLR_HUD_TEXT = Color.WHITE;
    private static final Color CLR_WPM = new Color(100, 200, 255);
    private static final Color CLR_DONE = new Color(255, 150, 255);
    private static final Color CLR_TIME = new Color(255, 215, 0);

    public GameRenderer(GamePanel gamePanel, GameState gameState, UISettings uiSettings) {
        this.gamePanel = gamePanel;
        this.gameState = gameState;
        this.uiSettings = uiSettings;
    }

    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        long now = System.currentTimeMillis();
        if (now < gameState.flashUntil) {
            g2.setColor(CLR_FLASH);
            g2.fillRect(0, 0, gamePanel.getWidth(), gamePanel.getHeight());
        }

        drawHUD(g2, now);

        int shakeX = 0, shakeY = 0;
        if (now < gameState.shakeUntil) {
            shakeX = gameState.rng.nextInt(gameState.shakeAmp * 2 + 1) - gameState.shakeAmp;
            shakeY = gameState.rng.nextInt(gameState.shakeAmp * 2 + 1) - gameState.shakeAmp;
        }
        g2.translate(shakeX, shakeY);

        try {
            gameState.bot.draw(g2, gameState.CHAR_SCALE);
        } catch (Throwable ignored) {
        }

        try {
            gameState.player.draw(g2, gameState.CHAR_SCALE);
        } catch (Throwable ignored) {
        }

        g2.translate(-shakeX, -shakeY);

        drawWord(g2, now);
        drawFooterInfo(g2);
        if (gameState.state != GameConfig.State.PLAYING) {
            drawScoreInfo(g2);
        }
        if (gameState.state == GameConfig.State.PLAYING) {
            drawComboEffect(g2);
            drawDamageText(g2, now);
        }
        gameState.hit.draw(g2);

        if (uiSettings.showSettings)
            drawSettingsOverlay(g2);

        g2.dispose();
    }

    private void drawHUD(Graphics2D g2, long now) {
        long elapsed = gameState.state == GameConfig.State.PLAYING
                ? (now - gameState.startMs)
                : 0;
        int mm = (int) (elapsed / 1000) / 60;
        int ss = (int) (elapsed / 1000) % 60;
        double minutes = gameState.state == GameConfig.State.PLAYING
                ? Math.max(1e-6, elapsed / 60000.0)
                : 0;
        double wpm = minutes > 0 ? (gameState.wordsCompleted * 5.0) / minutes : 0.0;
        double acc = 100.0;

        if (gameState.state == GameConfig.State.PLAYING) {
            drawGameHUD(g2, mm, ss, wpm, acc);
            drawHealthBars(g2);
        }

        g2.setFont(uiSettings.fontSmall12);
        g2.setColor(CLR_HUD_TEXT);
        g2.drawString("FPS: " + gameState.fps, gamePanel.getWidth() - 75, gamePanel.getHeight() - 20);
        g2.drawString("@Hex | version dev.", 10, gamePanel.getHeight() - 20);
        g2.setFont(uiSettings.fontPlain6);
    }

    private void drawGameHUD(Graphics2D g2, int mm, int ss, double wpm, double acc) {
        g2.setFont(uiSettings.fontBold16);
        g2.setColor(CLR_TIME);
        g2.drawString(String.format("เวลา: %02d:%02d", mm, ss), 28, 42);
        g2.setColor(CLR_WPM);
        g2.drawString(String.format(Locale.US, "WPM: %.1f", wpm), 28, 70);
        g2.setColor(CLR_DONE);
        g2.drawString(String.format("คำที่พิมพ์ได้: %d", gameState.wordsCompleted), 28, 95);
    }

    private void drawHealthBars(Graphics2D g2) {
        int barW = 120;
        int barH = 14;

        int playerX = 50;
        int playerY = 160;
        drawSingleHealthBar(g2, playerX, playerY, barW, barH,
                gameState.playerHealth / (double) GameConfig.MAX_HEALTH,
                "Player");

        int botX = gamePanel.getWidth() - barW - 50;
        int botY = 160;
        drawSingleHealthBar(g2, botX, botY, barW, barH,
                gameState.botHealth / (double) GameConfig.MAX_HEALTH,
                "Bot");
    }

    private void drawSingleHealthBar(Graphics2D g2, int barX, int barY, int barW, int barH,
            double healthPercent, String healthText) {
        g2.setColor(CLR_HUD_PANEL);
        g2.fillRoundRect(barX - 6, barY - 6, barW + 12, barH + 20, 12, 12);

        g2.setColor(new Color(40, 40, 40));
        g2.fillRoundRect(barX, barY, barW, barH, 8, 8);

        int healthW = (int) (barW * healthPercent);

        Color healthColor1 = healthPercent > 0.6
                ? new Color(100, 255, 100)
                : healthPercent > 0.3
                        ? new Color(255, 220, 100)
                        : new Color(255, 100, 100);
        Color healthColor2 = healthPercent > 0.6
                ? new Color(70, 200, 70)
                : healthPercent > 0.3
                        ? new Color(255, 180, 70)
                        : new Color(200, 70, 70);

        if (healthW > 0) {
            Paint oldPaint = g2.getPaint();
            g2.setPaint(new GradientPaint(barX, barY, healthColor1, barX, barY + barH, healthColor2));
            g2.fillRoundRect(barX, barY, healthW, barH, 8, 8);

            g2.setPaint(new GradientPaint(barX, barY, new Color(255, 255, 255, 80),
                    barX, barY + barH / 2, new Color(255, 255, 255, 0)));
            g2.fillRoundRect(barX, barY, healthW, barH / 2, 8, 8);
            g2.setPaint(oldPaint);
        }

        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(255, 255, 255, 100));
        g2.drawRoundRect(barX, barY, barW, barH, 8, 8);
        g2.setStroke(new BasicStroke(1f));

        if (healthText != null) {
            g2.setFont(uiSettings.fontSmall12);
            g2.setColor(CLR_HUD_TEXT);
            FontMetrics fm = g2.getFontMetrics();
            int textX = barX + (barW - fm.stringWidth(healthText)) / 2;
            g2.drawString(healthText, textX, barY - 8);

            String healthValue = String.format("%d/%d", (int) (healthPercent * GameConfig.MAX_HEALTH),
                    GameConfig.MAX_HEALTH);
            g2.setFont(uiSettings.fontSmall11);
            FontMetrics healthFm = g2.getFontMetrics();
            int valueX = barX + (barW - healthFm.stringWidth(healthValue)) / 2;
            g2.setColor(Color.WHITE);
            g2.drawString(healthValue, valueX, barY + barH / 2 + 3);
        }
    }

    private void drawWord(Graphics2D g2, long now) {
        if (gameState.current == null || gameState.current.word.isEmpty())
            return;

        Dimension keyDim = gameState.atlas.keySize();
        int keyW = keyDim.width * GameConfig.SCALE;
        int keyH = keyDim.height * GameConfig.SCALE;
        int totalW = gameState.current.word.length() * keyW
                + (gameState.current.word.length() - 1) * GameConfig.KEY_SPACING;

        double scale = (now < gameState.popUntil) ? 1.06 : 1.0;
        int cx = gamePanel.getWidth() / 2, cy = gamePanel.getHeight() / 2 - 60;
        int x0 = cx - (int) (totalW * scale) / 2;
        int y = cy - (int) (keyH * scale) / 2;

        Graphics2D gg = (Graphics2D) g2.create();
        gg.translate(cx, cy);
        gg.scale(scale, scale);
        gg.translate(-cx, -cy);

        for (int i = 0; i < gameState.current.word.length(); i++) {
            char ch = gameState.current.word.charAt(i);
            int x = x0 + i * (keyW + GameConfig.KEY_SPACING);

            gg.setColor(CLR_BOX_SHADOW);
            gg.fillRoundRect(x - 6, y - 6, keyW + 12, keyH + 12, 18, 18);
            drawWordInfo(g2, cx, cy - 80);

            BufferedImage img = (i < gameState.playerIdx)
                    ? gameState.atlas.getPressed(ch)
                    : gameState.atlas.getNormal(ch);
            gg.drawImage(img, x, y, keyW, keyH, null);

            if (i == gameState.playerIdx && gameState.state == GameConfig.State.PLAYING) {
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
        if (gameState.current == null)
            return;

        String title = gameState.current.display.toUpperCase(Locale.US);
        String sub1 = gameState.current.pronun;
        String sub2 = gameState.current.meaning;

        int boxW = Math.min(500, gamePanel.getWidth() - 40);
        int boxX = cx - boxW / 2;
        int boxH = 70;
        int boxY = cy - boxH / 2;

        g2.setColor(CLR_HUD_PANEL);
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 15, 15);
        g2.setColor(Color.WHITE);
        g2.setFont(uiSettings.fontBold20);
        centerTextAt(g2, title, cx, cy - 15);

        g2.setColor(new Color(220, 220, 220));
        g2.setFont(uiSettings.fontPlain16);
        centerTextAt(g2, sub1 + " • " + sub2, cx, cy + 8);
    }

    private void drawFooterInfo(Graphics2D g2) {
        if (gameState.state == GameConfig.State.READY) {
            drawSpacePrompt(g2, "กด ", " เพื่อเริ่มเกม");
            drawArrowPrompt(g2, "กด ", " เพื่อเลือกตัวละคร");
            drawBackspacePrompt(g2, "กด ", " เพื่อตั้งค่า");
        } else if (gameState.state == GameConfig.State.GAMEOVER) {
            drawSpacePrompt(g2, "กด ", " เพื่อเริ่มใหม่");
            drawArrowPrompt(g2, "กด ", " เพื่อเลือกตัวละคร");
            drawBackspacePrompt(g2, "กด ", " เพื่อตั้งค่า");
        }
    }

    private void drawScoreInfo(Graphics2D g2) {
        ScoreManager scoreManager = ScoreManager.getInstance();
        int highScore = scoreManager.getHighScore();
        int lastScore = scoreManager.getLastScore();

        int panelWidth = 220;
        int panelHeight = 90;
        int panelX = gamePanel.getWidth() - panelWidth - 10;
        int panelY = 20;

        g2.setColor(CLR_HUD_PANEL);
        g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 12, 12);
        g2.setColor(new Color(255, 255, 255, 40));
        g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 12, 12);

        g2.setColor(Color.WHITE);
        g2.setFont(uiSettings.fontBold16);
        centerTextAt(g2, "สถิติการเล่น", panelX + panelWidth / 2, panelY + 25);

        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawLine(panelX + 20, panelY + 35, panelX + panelWidth - 20, panelY + 35);

        g2.setColor(new Color(255, 215, 0));
        g2.setFont(uiSettings.fontBold16);
        g2.drawString("คะแนนสูงสุด: " + highScore, panelX + 15, panelY + 55);

        g2.setColor(new Color(200, 200, 200));
        g2.setFont(uiSettings.fontPlain16);
        g2.drawString("คะแนนล่าสุด: " + lastScore, panelX + 15, panelY + 80);
    }

    private void drawSpacePrompt(Graphics2D g2, String pre, String post) {
        BufferedImage frame = gameState.isSpaceHeld && uiSettings.spaceFramePressed != null
                ? uiSettings.spaceFramePressed
                : (gameState.spaceAnimOn && uiSettings.spaceFramePressed != null
                        ? uiSettings.spaceFramePressed
                        : uiSettings.spaceFrameNormal);

        g2.setFont(uiSettings.fontBold20);
        FontMetrics fm = g2.getFontMetrics();
        int spriteH = 26;
        int spriteW = (frame != null)
                ? (int) (frame.getWidth() * (spriteH / (double) frame.getHeight()))
                : 0;
        int totalW = fm.stringWidth(pre) + spriteW + fm.stringWidth(post);
        int y = gamePanel.getHeight() - 120;
        int x = (gamePanel.getWidth() - totalW) / 2;

        g2.setColor(Color.WHITE);
        g2.drawString(pre, x, y);
        int curX = x + fm.stringWidth(pre);

        if (frame != null) {
            g2.drawImage(frame, curX, y - spriteH + 3, spriteW, spriteH, null);
            curX += spriteW;
        }

        g2.drawString(post, curX, y);
    }

    private void drawArrowPrompt(Graphics2D g2, String pre, String post) {
        BufferedImage leftFrame = (uiSettings.arrowLeftFrame != null)
                ? uiSettings.arrowLeftFrame
                : uiSettings.arrowLeftNormal;

        BufferedImage rightFrame = (uiSettings.arrowRightFrame != null)
                ? uiSettings.arrowRightFrame
                : uiSettings.arrowRightNormal;

        g2.setFont(uiSettings.fontBold16);
        FontMetrics fm = g2.getFontMetrics();
        int spriteH = 22;
        int spriteW = (leftFrame != null)
                ? (int) (leftFrame.getWidth() * (spriteH / (double) leftFrame.getHeight()))
                : 0;
        int spriteW2 = (rightFrame != null)
                ? (int) (rightFrame.getWidth() * (spriteH / (double) rightFrame.getHeight()))
                : 0;
        int totalW = fm.stringWidth(pre) + spriteW + spriteW2 + fm.stringWidth(post) + 10;
        int y = gamePanel.getHeight() - 90;
        int x = (gamePanel.getWidth() - totalW) / 2;

        g2.setColor(Color.WHITE);
        g2.drawString(pre, x, y);
        int curX = x + fm.stringWidth(pre);

        if (leftFrame != null) {
            g2.drawImage(leftFrame, curX, y - spriteH + 2, spriteW, spriteH, null);
            curX += spriteW + 5;
        }

        if (rightFrame != null) {
            g2.drawImage(rightFrame, curX, y - spriteH + 2, spriteW2, spriteH, null);
            curX += spriteW2;
        }

        g2.drawString(post, curX, y);
    }

    private void drawBackspacePrompt(Graphics2D g2, String pre, String post) {
        BufferedImage frame = uiSettings.backspaceFrameNormal != null
                ? uiSettings.backspaceFrameNormal
                : null;

        g2.setFont(uiSettings.fontBold16);
        FontMetrics fm = g2.getFontMetrics();
        int spriteH = 22;
        int spriteW = (frame != null)
                ? (int) (frame.getWidth() * (spriteH / (double) frame.getHeight()))
                : 0;
        int totalW = fm.stringWidth(pre) + spriteW + fm.stringWidth(post);
        int y = gamePanel.getHeight() - 60;
        int x = (gamePanel.getWidth() - totalW) / 2;

        g2.setColor(Color.WHITE);
        g2.drawString(pre, x, y);
        int curX = x + fm.stringWidth(pre);

        if (frame != null) {
            g2.drawImage(frame, curX, y - spriteH + 2, spriteW, spriteH, null);
            curX += spriteW;
        }

        g2.drawString(post, curX, y);
    }

    public void drawSettingsOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, gamePanel.getWidth(), gamePanel.getHeight());

        Rectangle panelRect = uiSettings.panelRect;
        Rectangle toggleRect = uiSettings.toggleRect;
        Rectangle ttsRepeatToggleRect = uiSettings.ttsRepeatToggleRect;
        Rectangle masterSliderTrack = uiSettings.masterSliderTrack;
        Rectangle masterSliderKnob = uiSettings.masterSliderKnob;
        Rectangle ttsSliderTrack = uiSettings.ttsSliderTrack;
        Rectangle ttsSliderKnob = uiSettings.ttsSliderKnob;
        Rectangle sfxSliderTrack = uiSettings.sfxSliderTrack;
        Rectangle sfxSliderKnob = uiSettings.sfxSliderKnob;
        Rectangle musicSliderTrack = uiSettings.musicSliderTrack;
        Rectangle musicSliderKnob = uiSettings.musicSliderKnob;
        Rectangle speedRectSlow = uiSettings.speedRectSlow;
        Rectangle speedRectNormal = uiSettings.speedRectNormal;
        Rectangle speedRectFast = uiSettings.speedRectFast;

        if (panelRect == null)
            uiSettings.layoutSettingsRects(gamePanel.getWidth(), gamePanel.getHeight());

        g2.setColor(new Color(24, 28, 34));
        g2.fillRoundRect(panelRect.x, panelRect.y, panelRect.width, panelRect.height, 16, 16);
        g2.setColor(new Color(255, 255, 255, 80));
        g2.drawRoundRect(panelRect.x, panelRect.y, panelRect.width, panelRect.height, 16, 16);

        g2.setFont(uiSettings.fontBold20);
        g2.setColor(Color.WHITE);
        centerTextAt(g2, "ตั้งค่าเสียง", panelRect.x + panelRect.width / 2, panelRect.y + 36);

        g2.setFont(uiSettings.fontPlain16);
        g2.setColor(new Color(220, 220, 220));
        g2.drawString("TTS", panelRect.x + 30, toggleRect.y - 10);

        int tx = toggleRect.x, ty = toggleRect.y, tw = toggleRect.width, th = toggleRect.height;
        g2.setColor(uiSettings.ttsEnabled ? new Color(70, 170, 110) : new Color(90, 90, 90));
        g2.fillRoundRect(tx, ty, tw, th, th, th);
        int knob = th - 6;
        int kx = uiSettings.ttsEnabled ? tx + tw - knob - 3 : tx + 3;
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(kx, ty + 3, knob, knob, knob, knob);

        g2.setColor(new Color(220, 220, 220));
        g2.drawString("TTS พูดซ้ำ", ttsRepeatToggleRect.x - 100, ttsRepeatToggleRect.y - 10);

        tx = ttsRepeatToggleRect.x;
        ty = ttsRepeatToggleRect.y;
        tw = ttsRepeatToggleRect.width;
        th = ttsRepeatToggleRect.height;
        g2.setColor(uiSettings.ttsRepeatEnabled ? new Color(70, 170, 110) : new Color(90, 90, 90));
        g2.fillRoundRect(tx, ty, tw, th, th, th);
        kx = uiSettings.ttsRepeatEnabled ? tx + tw - knob - 3 : tx + 3;
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(kx, ty + 3, knob, knob, knob, knob);

        g2.setColor(new Color(220, 220, 220));
        g2.drawString("ระดับเสียงรวม", panelRect.x + 30, masterSliderTrack.y - 10);
        drawVolumeSlider(g2, masterSliderTrack, masterSliderKnob, uiSettings.masterVolume);

        g2.setColor(new Color(220, 220, 220));
        g2.drawString("ระดับเสียง TTS", panelRect.x + 30, ttsSliderTrack.y - 10);
        drawVolumeSlider(g2, ttsSliderTrack, ttsSliderKnob, uiSettings.ttsVolume);

        g2.setColor(new Color(220, 220, 220));
        g2.drawString("ระดับเสียงเอฟเฟกต์", panelRect.x + 30, sfxSliderTrack.y - 10);
        drawVolumeSlider(g2, sfxSliderTrack, sfxSliderKnob, uiSettings.sfxVolume);

        g2.setColor(new Color(220, 220, 220));
        g2.drawString("ระดับเสียงเพลง", panelRect.x + 30, musicSliderTrack.y - 10);
        drawVolumeSlider(g2, musicSliderTrack, musicSliderKnob, uiSettings.musicVolume);

        g2.setColor(new Color(220, 220, 220));
        g2.drawString("ความเร็ว TTS", panelRect.x + 30, speedRectSlow.y - 10);
        drawSpeedButton(g2, speedRectSlow, "ช้า", uiSettings.ttsSpeedLevel == 0);
        drawSpeedButton(g2, speedRectNormal, "ปกติ", uiSettings.ttsSpeedLevel == 1);
        drawSpeedButton(g2, speedRectFast, "เร็ว", uiSettings.ttsSpeedLevel == 2);

        g2.setFont(uiSettings.fontSmall11);
        g2.setColor(new Color(200, 200, 200));
        centerTextAt(g2, "กด ESC เพื่อปิด", panelRect.x + panelRect.width / 2, panelRect.y + panelRect.height - 16);
    }

    private void drawVolumeSlider(Graphics2D g2, Rectangle track, Rectangle knob, int value) {
        g2.setColor(new Color(60, 60, 60));
        g2.fillRoundRect(track.x, track.y, track.width, track.height, 8, 8);
        g2.setColor(new Color(255, 255, 255, 100));
        g2.drawRoundRect(track.x, track.y, track.width, track.height, 8, 8);

        int fill = (int) Math.round((value / 100.0) * track.width);
        fill = Math.max(0, Math.min(track.width, fill));
        g2.setColor(new Color(100, 200, 255));
        g2.fillRoundRect(track.x, track.y, fill, track.height, 8, 8);

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(knob.x, knob.y, knob.width, knob.height, 10, 10);

        g2.setFont(uiSettings.fontSmall12);
        g2.setColor(Color.WHITE);
        g2.drawString(value + "%", track.x + track.width + 10, track.y + 14);
    }

    private void drawSpeedButton(Graphics2D g2, Rectangle r, String label, boolean active) {
        g2.setColor(active ? new Color(70, 170, 110) : new Color(60, 60, 60));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g2.setColor(new Color(255, 255, 255, 100));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);

        g2.setColor(Color.WHITE);
        g2.setFont(uiSettings.fontBold16);
        FontMetrics fm = g2.getFontMetrics();
        int cx = r.x + (r.width - fm.stringWidth(label)) / 2;
        int cy = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2 - 2;
        g2.drawString(label, cx, cy);
    }

    public void drawCharacterPreview(Graphics2D g2) {
        Rectangle previewBoxRect = uiSettings.previewBoxRect;
        CharacterPack previewPack = uiSettings.previewPack;

        if (previewBoxRect == null)
            return;
        if (previewPack == null)
            uiSettings.updatePreviewPack();

        g2.setColor(new Color(30, 34, 40));
        g2.fillRoundRect(previewBoxRect.x, previewBoxRect.y, previewBoxRect.width, previewBoxRect.height, 10, 10);
        g2.setColor(new Color(255, 255, 255, 70));
        g2.drawRoundRect(previewBoxRect.x, previewBoxRect.y, previewBoxRect.width, previewBoxRect.height, 10, 10);

        int baseY = previewBoxRect.y + previewBoxRect.height - 10;
        previewPack.x = previewBoxRect.x + 28;
        previewPack.y = baseY;

        try {
            previewPack.draw(g2, 2);
        } catch (Throwable ignored) {
        }

        g2.setFont(uiSettings.fontSmall11);
        g2.setColor(Color.WHITE);

        String[] playerCharIds = CharacterConfig.getInstance().getPlayerCharacterIds();
        String characterName = "Character";
        if (playerCharIds.length > 0) {
            CharacterConfig.CharacterData data = CharacterConfig.getInstance()
                    .getCharacter(playerCharIds[uiSettings.selectedCharIdx]);
            if (data != null) {
                characterName = data.name;
            }
        }

        g2.drawString("ตัวละคร: " + characterName,
                previewBoxRect.x + 10, previewBoxRect.y + 16);
    }

    private void centerTextAt(Graphics2D g2, String s, int x, int y) {
        FontMetrics fm = g2.getFontMetrics();
        int cx = x - fm.stringWidth(s) / 2;
        g2.drawString(s, cx, y);
    }

    private void drawComboEffect(Graphics2D g2) {
        if (gameState.comboEffect != null && gameState.comboEffect.isActive()) {
            gameState.comboEffect.render(g2, uiSettings.fontBold20, uiSettings.fontPlain16);
        }
    }

    private void drawDamageText(Graphics2D g2, long now) {
        if (gameState.showPlayerDamage) {
            long elapsed = now - (gameState.playerDamageUntil - 1000);
            float alpha = Math.max(0f, 1f - (elapsed / 1000f));
            int yOffset = (int) (elapsed * 0.05f);

            g2.setColor(new Color(255, 80, 80, (int) (255 * alpha)));
            g2.setFont(uiSettings.fontBold20);
            g2.drawString("-1", gameState.playerDamageX, gameState.playerDamageY - yOffset);
        }

        if (gameState.showBotDamage) {
            long elapsed = now - (gameState.botDamageUntil - 1000);
            float alpha = Math.max(0f, 1f - (elapsed / 1000f));
            int yOffset = (int) (elapsed * 0.05f);

            g2.setColor(new Color(255, 80, 80, (int) (255 * alpha)));
            g2.setFont(uiSettings.fontBold20);
            g2.drawString("-1", gameState.botDamageX, gameState.botDamageY - yOffset);
        }
    }
}

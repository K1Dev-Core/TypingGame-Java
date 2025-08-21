import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.*;
import javax.imageio.ImageIO;

public class UISettings {
    public Font fontSmall12, fontSmall11, fontBold16, fontBold20, fontPlain16, fontPlain6, fontTitle48;

    public boolean showSettings = false;
    public boolean ttsEnabled = true;
    public boolean ttsRepeatEnabled = false;
    public int masterVolume = 100;
    public int ttsVolume = 100;
    public int sfxVolume = 100;
    public int musicVolume = 1;
    public final int[] ttsRates = new int[] { 110, 140, 180 };
    public int ttsSpeedLevel = 1;

    public Rectangle panelRect, toggleRect, ttsToggleRect, ttsRepeatToggleRect;
    public Rectangle masterSliderTrack, masterSliderKnob;
    public Rectangle ttsSliderTrack, ttsSliderKnob;
    public Rectangle sfxSliderTrack, sfxSliderKnob;
    public Rectangle musicSliderTrack, musicSliderKnob;
    public Rectangle speedRectSlow, speedRectNormal, speedRectFast;
    public Rectangle sliderTrack, sliderKnob;
    public boolean draggingSlider = false;
    public String draggingSliderType = null;

    public final String[] CHAR_NAMES = { "Mushroom", "MedievalKing", "Skeleton" };
    public final String[] CHAR_PATHS = {
            "./res/characters/Mushroom",
            "./res/characters/MedievalKing",
            "./res/characters/Skeleton"
    };
    public final int[] CHAR_BASELINE = { 20, -20, 20 };
    public int selectedCharIdx = 1;

    public Rectangle prevCharRect, nextCharRect, applyCharRect, previewBoxRect;
    public CharacterPack previewPack = null;

    public BufferedImage spaceSheet, spaceFrameNormal, spaceFramePressed;
    public BufferedImage backspaceSheet, backspaceFrameNormal, backspaceFramePressed;
    public BufferedImage arrowLeftSheet, arrowLeftNormal, arrowLeftPressed;
    public BufferedImage arrowRightSheet, arrowRightNormal, arrowRightPressed;
    public BufferedImage arrowLeftFrame;
    public BufferedImage arrowRightFrame;

    public UISettings() {
        initFonts();
        initKeySprites();
    }

    public void initFonts() {
        try {
            Font mcFont = Font.createFont(Font.TRUETYPE_FONT, new File("./res/fonts/Minecraft-TenTH.ttf"));
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(mcFont);
            fontSmall12 = mcFont.deriveFont(Font.PLAIN, 20f);
            fontSmall11 = mcFont.deriveFont(Font.PLAIN, 11f);
            fontPlain6 = mcFont.deriveFont(Font.PLAIN, 6f);
            fontBold16 = mcFont.deriveFont(Font.BOLD, 16f);
            fontBold20 = mcFont.deriveFont(Font.BOLD, 20f);
            fontPlain16 = mcFont.deriveFont(Font.PLAIN, 16f);
            fontTitle48 = mcFont.deriveFont(Font.BOLD, 48f);
        } catch (Exception ignored) {
        }
    }

    public void initKeySprites() {
        try {
            spaceSheet = ImageIO.read(new File("./res/keys/SPACE.png"));
            if (spaceSheet != null) {
                int fw = spaceSheet.getWidth() / 2;
                int fh = spaceSheet.getHeight();
                spaceFrameNormal = spaceSheet.getSubimage(0, 0, fw, fh);
                spaceFramePressed = spaceSheet.getSubimage(fw, 0, fw, fh);
            }
        } catch (Exception ignored) {
        }

        try {
            backspaceSheet = ImageIO.read(new File("./res/keys/BACKSPACE.png"));
            if (backspaceSheet != null) {
                int fw = backspaceSheet.getWidth() / 2;
                int fh = backspaceSheet.getHeight();
                backspaceFrameNormal = backspaceSheet.getSubimage(0, 0, fw, fh);
                backspaceFramePressed = backspaceSheet.getSubimage(fw, 0, fw, fh);
            }
        } catch (Exception ignored) {
        }

        try {
            arrowLeftSheet = ImageIO.read(new File("./res/keys/ARROWLEFT.png"));
            if (arrowLeftSheet != null) {
                int fw = arrowLeftSheet.getWidth() / 2;
                int fh = arrowLeftSheet.getHeight();
                arrowLeftNormal = arrowLeftSheet.getSubimage(0, 0, fw, fh);
                arrowLeftPressed = arrowLeftSheet.getSubimage(fw, 0, fw, fh);
            }
        } catch (Exception ignored) {
        }

        try {
            arrowRightSheet = ImageIO.read(new File("./res/keys/ARROWRIGHT.png"));
            if (arrowRightSheet != null) {
                int fw = arrowRightSheet.getWidth() / 2;
                int fh = arrowRightSheet.getHeight();
                arrowRightNormal = arrowRightSheet.getSubimage(0, 0, fw, fh);
                arrowRightPressed = arrowRightSheet.getSubimage(fw, 0, fw, fh);
            }
        } catch (Exception ignored) {
        }
    }

    public void layoutSettingsRects(int width, int height) {
        int panelW = Math.max(400, Math.min(520, width - 220));
        int panelH = 480;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        panelRect = new Rectangle(x, y, panelW, panelH);

        toggleRect = new Rectangle(x + 30, y + 70, 64, 30);
        ttsRepeatToggleRect = new Rectangle(x + panelW - 110, y + 70, 64, 30);

        int trackLeft = x + 30;
        int trackRight = x + panelW - 90;
        int trackW = Math.max(140, trackRight - trackLeft);
        int knobW = 18, knobH = 24;

        int sliderBaseY = y + 130;
        int sliderSpacing = 60;

        masterSliderTrack = new Rectangle(trackLeft, sliderBaseY, trackW, 10);
        int centerX = masterSliderTrack.x + (int) Math.round((masterVolume / 100.0) * masterSliderTrack.width);
        masterSliderKnob = new Rectangle(centerX - knobW / 2,
                masterSliderTrack.y - (knobH - masterSliderTrack.height) / 2, knobW, knobH);

        ttsSliderTrack = new Rectangle(trackLeft, sliderBaseY + sliderSpacing, trackW, 10);
        centerX = ttsSliderTrack.x + (int) Math.round((ttsVolume / 100.0) * ttsSliderTrack.width);
        ttsSliderKnob = new Rectangle(centerX - knobW / 2,
                ttsSliderTrack.y - (knobH - ttsSliderTrack.height) / 2, knobW, knobH);

        sfxSliderTrack = new Rectangle(trackLeft, sliderBaseY + sliderSpacing * 2, trackW, 10);
        centerX = sfxSliderTrack.x + (int) Math.round((sfxVolume / 100.0) * sfxSliderTrack.width);
        sfxSliderKnob = new Rectangle(centerX - knobW / 2,
                sfxSliderTrack.y - (knobH - sfxSliderTrack.height) / 2, knobW, knobH);

        musicSliderTrack = new Rectangle(trackLeft, sliderBaseY + sliderSpacing * 3, trackW, 10);
        centerX = musicSliderTrack.x + (int) Math.round((musicVolume / 100.0) * musicSliderTrack.width);
        musicSliderKnob = new Rectangle(centerX - knobW / 2,
                musicSliderTrack.y - (knobH - musicSliderTrack.height) / 2, knobW, knobH);

        int btnW = (panelW - 60 - 30) / 3;
        int btnH = 32;
        int by = sliderBaseY + sliderSpacing * 4;
        speedRectSlow = new Rectangle(x + 30, by, btnW, btnH);
        speedRectNormal = new Rectangle(x + 30 + btnW + 15, by, btnW, btnH);
        speedRectFast = new Rectangle(x + 30 + (btnW + 15) * 2, by, btnW, btnH);

        sliderTrack = masterSliderTrack;
        sliderKnob = masterSliderKnob;

        previewBoxRect = new Rectangle(x, y - 150, 220, 120);
    }

    public void updateSliderFromMouse(int mouseX) {
        if (draggingSliderType == null)
            return;

        Rectangle track;
        Rectangle knob;

        switch (draggingSliderType) {
            case "master":
                track = masterSliderTrack;
                knob = masterSliderKnob;
                if (track != null) {
                    int clamped = Math.max(track.x, Math.min(track.x + track.width, mouseX));
                    double t = (clamped - track.x) / (double) track.width;
                    masterVolume = (int) Math.round(t * 100.0);
                    knob.x = clamped - knob.width / 2;
                }
                break;

            case "tts":
                track = ttsSliderTrack;
                knob = ttsSliderKnob;
                if (track != null) {
                    int clamped = Math.max(track.x, Math.min(track.x + track.width, mouseX));
                    double t = (clamped - track.x) / (double) track.width;
                    ttsVolume = (int) Math.round(t * 100.0);
                    knob.x = clamped - knob.width / 2;
                }
                break;

            case "sfx":
                track = sfxSliderTrack;
                knob = sfxSliderKnob;
                if (track != null) {
                    int clamped = Math.max(track.x, Math.min(track.x + track.width, mouseX));
                    double t = (clamped - track.x) / (double) track.width;
                    sfxVolume = (int) Math.round(t * 100.0);
                    knob.x = clamped - knob.width / 2;
                }
                break;

            case "music":
                track = musicSliderTrack;
                knob = musicSliderKnob;
                if (track != null) {
                    int clamped = Math.max(track.x, Math.min(track.x + track.width, mouseX));
                    double t = (clamped - track.x) / (double) track.width;
                    musicVolume = (int) Math.round(t * 100.0);
                    knob.x = clamped - knob.width / 2;
                }
                break;
        }

        if (draggingSliderType.equals("master")) {
            sliderKnob.x = masterSliderKnob.x;
        }
    }

    public boolean isAudible() {
        return masterVolume > 0;
    }

    public void applyVolumeToPools(GameState gameState) {
        try {
            float masterVol = masterVolume / 100f;
            float sfxVol = (sfxVolume / 100f) * masterVol;
            float musicVol = (musicVolume / 100f) * masterVol;

            gameState.sStart.setVolume(sfxVol);
            gameState.sType.setVolume(sfxVol);
            gameState.sErr.setVolume(sfxVol);
            gameState.sClick.setVolume(sfxVol);
            gameState.sSlash.setVolume(sfxVol);
            gameState.sHit.setVolume(sfxVol);
            gameState.sDeath.setVolume(sfxVol);

            if (gameState.bgMusic != null) {
                gameState.bgMusic.setVolume(musicVol);
            }
        } catch (Throwable ignored) {
        }
    }

    public CharacterPack.Config configFor(int idx) {
        if (idx == 1) {
            return new CharacterPack.Config()
                    .set(CharacterPack.Anim.IDLE, 8, 120, true)
                    .set(CharacterPack.Anim.ATTACK, 4, 80, false)
                    .set(CharacterPack.Anim.TAKE_HIT, 4, 90, false)
                    .set(CharacterPack.Anim.DEATH, 6, 150, false)
                    .set(CharacterPack.Anim.WALK, 1, 1000, true);
        }
        return null;
    }

    public void updatePreviewPack() {
        CharacterPack.Config cfg = configFor(selectedCharIdx);
        previewPack = new CharacterPack(CHAR_PATHS[selectedCharIdx], 0, 0, false, cfg, CHAR_BASELINE[selectedCharIdx]);
        previewPack.setAnim(CharacterPack.Anim.IDLE);
    }

    public void setTtsSpeedLevel(int level) {
        level = Math.max(0, Math.min(2, level));
        if (ttsSpeedLevel == level)
            return;
        ttsSpeedLevel = level;
        if (ttsEnabled) {
            new Thread(() -> {
                try {
                    SlowTTS tts = new SlowTTS("kevin16", ttsRates[ttsSpeedLevel]);
                    tts.speak("Hello");
                    tts.close();
                } catch (Exception ignored) {
                }
            }).start();
        }
    }

    public void speakCurrentWord(WordEntry current) {
        if (ttsEnabled && current != null) {
            new Thread(() -> {
                try {
                    SlowTTS tts = new SlowTTS("kevin16", ttsRates[ttsSpeedLevel]);
                    tts.speak(current.word);

                    if (ttsRepeatEnabled) {
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException ignored) {
                        }
                        tts.speak(current.word);
                    }

                    tts.close();
                } catch (Exception ignored) {
                }
            }).start();
        }
    }
}

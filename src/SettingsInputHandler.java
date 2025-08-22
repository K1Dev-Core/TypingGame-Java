import java.awt.Rectangle;
import java.awt.event.MouseEvent;

public class SettingsInputHandler {
    
    public static void handleSettingsMousePress(MouseEvent e, UISettings uiSettings, GameState gameState, GamePanel gamePanel) {
        if (!uiSettings.showSettings) return;
        
        handleToggleClicks(e, uiSettings, gamePanel);
        handleSpeedButtonClicks(e, uiSettings);
        handleSliderClicks(e, uiSettings, gameState, gamePanel);
    }
    
    private static void handleToggleClicks(MouseEvent e, UISettings uiSettings, GamePanel gamePanel) {
        if (uiSettings.toggleRect != null && uiSettings.toggleRect.contains(e.getPoint())) {
            uiSettings.ttsEnabled = !uiSettings.ttsEnabled;
            gamePanel.repaint();
        }

        if (uiSettings.ttsRepeatToggleRect != null && uiSettings.ttsRepeatToggleRect.contains(e.getPoint())) {
            uiSettings.ttsRepeatEnabled = !uiSettings.ttsRepeatEnabled;
            gamePanel.repaint();
        }
    }
    
    private static void handleSpeedButtonClicks(MouseEvent e, UISettings uiSettings) {
        if (uiSettings.speedRectSlow != null && uiSettings.speedRectSlow.contains(e.getPoint())) {
            uiSettings.setTtsSpeedLevel(0);
        } else if (uiSettings.speedRectNormal != null && uiSettings.speedRectNormal.contains(e.getPoint())) {
            uiSettings.setTtsSpeedLevel(1);
        } else if (uiSettings.speedRectFast != null && uiSettings.speedRectFast.contains(e.getPoint())) {
            uiSettings.setTtsSpeedLevel(2);
        }
    }
    
    private static void handleSliderClicks(MouseEvent e, UISettings uiSettings, GameState gameState, GamePanel gamePanel) {
        checkSliderKnob(e, uiSettings.masterSliderKnob, "master", uiSettings);
        checkSliderKnob(e, uiSettings.ttsSliderKnob, "tts", uiSettings);
        checkSliderKnob(e, uiSettings.sfxSliderKnob, "sfx", uiSettings);
        checkSliderKnob(e, uiSettings.musicSliderKnob, "music", uiSettings);

        checkSliderTrack(e, uiSettings.masterSliderTrack, "master", uiSettings, gameState, gamePanel);
        checkSliderTrack(e, uiSettings.ttsSliderTrack, "tts", uiSettings, gameState, gamePanel);
        checkSliderTrack(e, uiSettings.sfxSliderTrack, "sfx", uiSettings, gameState, gamePanel);
        checkSliderTrack(e, uiSettings.musicSliderTrack, "music", uiSettings, gameState, gamePanel);
    }
    
    private static void checkSliderKnob(MouseEvent e, Rectangle knob, String type, UISettings uiSettings) {
        if (knob != null) {
            Rectangle expandedKnob = new Rectangle(
                    knob.x - 6,
                    knob.y - 6,
                    knob.width + 12,
                    knob.height + 12);

            if (expandedKnob.contains(e.getPoint())) {
                uiSettings.draggingSlider = true;
                uiSettings.draggingSliderType = type;
            }
        }
    }
    
    private static void checkSliderTrack(MouseEvent e, Rectangle track, String type, UISettings uiSettings, GameState gameState, GamePanel gamePanel) {
        if (track != null && track.contains(e.getPoint())) {
            uiSettings.draggingSliderType = type;
            uiSettings.updateSliderFromMouse(e.getX());
            uiSettings.draggingSlider = true;
            uiSettings.applyVolumeToPools(gameState);
            gamePanel.repaint();
        }
    }
    
    public static void handleSliderDrag(MouseEvent e, UISettings uiSettings, GameState gameState, GamePanel gamePanel) {
        if (uiSettings.showSettings && uiSettings.draggingSlider) {
            uiSettings.updateSliderFromMouse(e.getX());
            uiSettings.applyVolumeToPools(gameState);
            gamePanel.repaint();
        }
    }
    
    public static void handleMouseRelease(UISettings uiSettings) {
        uiSettings.draggingSlider = false;
        uiSettings.draggingSliderType = null;
    }
}
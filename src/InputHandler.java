import client.NetworkClient;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import shared.NetworkMessage;
import shared.Player;

public class InputHandler implements KeyListener, MouseListener {
    private final GamePanel gamePanel;
    private final GameState gameState;
    private final UISettings uiSettings;
    private final AnimationController animController;

    private NetworkClient networkClient;
    private Player localPlayer;

    public InputHandler(GamePanel gamePanel, GameState gameState, UISettings uiSettings,
            AnimationController animController) {
        this.gamePanel = gamePanel;
        this.gameState = gameState;
        this.uiSettings = uiSettings;
        this.animController = animController;

        initKeyListeners();
        initMouseListeners();
        initCursor();
    }

    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
    }

    public void setLocalPlayer(Player player) {
        this.localPlayer = player;
    }

    private void initKeyListeners() {
        gamePanel.addKeyListener(this);
    }

    private void initMouseListeners() {
        gamePanel.addMouseListener(this);
        gamePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (uiSettings.showSettings)
                    return;
                animController.playIfAudible(gameState.sClick);
                gameState.hit.playAt(e.getX(), e.getY());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!uiSettings.showSettings)
                    return;

                if (uiSettings.toggleRect != null && uiSettings.toggleRect.contains(e.getPoint())) {
                    uiSettings.ttsEnabled = !uiSettings.ttsEnabled;
                    gamePanel.repaint();
                }

                if (uiSettings.ttsRepeatToggleRect != null && uiSettings.ttsRepeatToggleRect.contains(e.getPoint())) {
                    uiSettings.ttsRepeatEnabled = !uiSettings.ttsRepeatEnabled;
                    gamePanel.repaint();
                }

                if (uiSettings.speedRectSlow != null && uiSettings.speedRectSlow.contains(e.getPoint())) {
                    uiSettings.setTtsSpeedLevel(0);
                } else if (uiSettings.speedRectNormal != null && uiSettings.speedRectNormal.contains(e.getPoint())) {
                    uiSettings.setTtsSpeedLevel(1);
                } else if (uiSettings.speedRectFast != null && uiSettings.speedRectFast.contains(e.getPoint())) {
                    uiSettings.setTtsSpeedLevel(2);
                }

                checkSliderKnob(e, uiSettings.masterSliderKnob, "master");
                checkSliderKnob(e, uiSettings.ttsSliderKnob, "tts");
                checkSliderKnob(e, uiSettings.sfxSliderKnob, "sfx");
                checkSliderKnob(e, uiSettings.musicSliderKnob, "music");

                checkSliderTrack(e, uiSettings.masterSliderTrack, "master");
                checkSliderTrack(e, uiSettings.ttsSliderTrack, "tts");
                checkSliderTrack(e, uiSettings.sfxSliderTrack, "sfx");
                checkSliderTrack(e, uiSettings.musicSliderTrack, "music");
            }

            private void checkSliderKnob(MouseEvent e, Rectangle knob, String type) {
                if (knob != null) {
                    java.awt.Rectangle big = new java.awt.Rectangle(
                            knob.x - 6,
                            knob.y - 6,
                            knob.width + 12,
                            knob.height + 12);

                    if (big.contains(e.getPoint())) {
                        uiSettings.draggingSlider = true;
                        uiSettings.draggingSliderType = type;
                    }
                }
            }

            private void checkSliderTrack(MouseEvent e, Rectangle track, String type) {
                if (track != null && track.contains(e.getPoint())) {
                    uiSettings.draggingSliderType = type;
                    uiSettings.updateSliderFromMouse(e.getX());
                    uiSettings.draggingSlider = true;
                    uiSettings.applyVolumeToPools(gameState);
                    gamePanel.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                uiSettings.draggingSlider = false;
                uiSettings.draggingSliderType = null;
            }
        });

        gamePanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (uiSettings.showSettings) {
                    if (uiSettings.draggingSlider) {
                        uiSettings.updateSliderFromMouse(e.getX());
                        uiSettings.applyVolumeToPools(gameState);
                        gamePanel.repaint();
                    }
                    return;
                }
                gameState.hit.playAt(e.getX(), e.getY());
            }
        });
    }

    private void initCursor() {
        try {
            Image raw = ImageIO.read(new File("./res/keys/hand_small_point.png"));
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
                hot = new Point(
                        Math.round(hot.x * (best.width / (float) nw)),
                        Math.round(hot.y * (best.height / (float) nh)));
                buf = adj;
            }

            Cursor customCursor = tk.createCustomCursor(buf, hot, "customCursor");
            gamePanel.setCursor(customCursor);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (OnlineUI.isShowingNameInput()) {
            handleNameInputKeys(e);
            return;
        }
        
        if (OnlineUI.isShowingExitConfirmation()) {
            if (code == KeyEvent.VK_Y) {
                OnlineUI.handleExitConfirmationKey('Y');
                gamePanel.repaint();
                return;
            }
            if (code == KeyEvent.VK_N) {
                OnlineUI.handleExitConfirmationKey('N');
                gamePanel.repaint();
                return;
            }
            if (code == KeyEvent.VK_ESCAPE) {
                OnlineUI.handleExitConfirmationKey((char) 27);
                gamePanel.repaint();
                return;
            }
            return;
        }

        if (code >= 0 && code < gameState.lastPressAt.length) {
            long now = System.currentTimeMillis();
            if (now - gameState.lastPressAt[code] < GameConfig.DEBOUNCE_MS)
                return;
            gameState.lastPressAt[code] = now;
        }

        if (code == KeyEvent.VK_E) {
            if (OnlineUI.isShowingExitConfirmation()) {
                OnlineUI.handleExitConfirmationKey((char) 27);
                gamePanel.repaint();
                return;
            }
            
            if (OnlineMatchManager.getInstance().isOnline() && gameState.state == GameConfig.State.READY) {
                OnlineUI.showExitConfirmation();
                gamePanel.repaint();
                return;
            }
        }
        
        if (code == KeyEvent.VK_ESCAPE) {
            if (!OnlineMatchManager.getInstance().isOnline()) {
                uiSettings.showSettings = !uiSettings.showSettings;
                gamePanel.repaint();
                return;
            }
        }

        if (gameState.state == GameConfig.State.READY) {
            handleReadyStateKeyPress(code);
            return;
        }

        if (gameState.state == GameConfig.State.GAMEOVER) {
            handleGameOverStateKeyPress(code);
            return;
        }

        if (gameState.state != GameConfig.State.PLAYING)
            return;

        if (code == KeyEvent.VK_SPACE) {
            gameState.isSpaceHeld = true;
            return;
        }

        if (code == KeyEvent.VK_BACK_SPACE) {
            if (gameState.playerIdx > 0) {
                gameState.playerIdx--;
                animController.playIfAudible(gameState.sStart);
            }
            return;
        }

        if (gameState.botSeq || gameState.playerSeq)
            return;

        if (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z) {
            char typedChar = (char) ('A' + code - KeyEvent.VK_A);
            
            OnlineMatchManager.getInstance().handleCharacterTyped(typedChar);
            
            int oldPlayerIdx = gameState.playerIdx;
            gameState.handleChar(typedChar, animController);

            if (OnlineMatchManager.getInstance().isRacing() && gameState.playerIdx > oldPlayerIdx) {
                OnlineMatchManager.getInstance().sendPlayerProgress(gameState.playerIdx);
                
                if (gameState.playerIdx >= gameState.current.word.length()) {
                    OnlineMatchManager.getInstance().handleWordCompleted();
                }
            }
            
            if (gameState.isMultiplayerMode() && gameState.playerIdx > oldPlayerIdx &&
                    networkClient != null && localPlayer != null) {
                try {
                    networkClient.sendMessage(new NetworkMessage(
                            NetworkMessage.MessageType.PLAYER_PROGRESS,
                            localPlayer.id, null, gameState.playerIdx));
                } catch (Exception ex2) {
                    System.err.println("Error sending player progress: " + ex2.getMessage());
                }
            }
        } else if (code >= KeyEvent.VK_0 && code <= KeyEvent.VK_9) {
            char typedChar = (char) ('0' + code - KeyEvent.VK_0);
            
            OnlineMatchManager.getInstance().handleCharacterTyped(typedChar);
            
            int oldPlayerIdx = gameState.playerIdx;
            gameState.handleChar(typedChar, animController);

            if (OnlineMatchManager.getInstance().isRacing() && gameState.playerIdx > oldPlayerIdx) {
                OnlineMatchManager.getInstance().sendPlayerProgress(gameState.playerIdx);
                
                if (gameState.playerIdx >= gameState.current.word.length()) {
                    OnlineMatchManager.getInstance().handleWordCompleted();
                }
            }
            
            if (gameState.isMultiplayerMode() && gameState.playerIdx > oldPlayerIdx &&
                    networkClient != null && localPlayer != null) {
                try {
                    networkClient.sendMessage(new NetworkMessage(
                            NetworkMessage.MessageType.PLAYER_PROGRESS,
                            localPlayer.id, null, gameState.playerIdx));
                } catch (Exception ex2) {
                    System.err.println("Error sending player progress: " + ex2.getMessage());
                }
            }
        }
    }

    private void handleNameInputKeys(KeyEvent e) {
        int code = e.getKeyCode();
        char keyChar = e.getKeyChar();
        
        if (code == KeyEvent.VK_ENTER) {
            OnlineUI.handleNameInput('\n');
            gamePanel.repaint();
            return;
        }
        
        if (code == KeyEvent.VK_BACK_SPACE) {
            OnlineUI.handleNameInput('\b');
            gamePanel.repaint();
            return;
        }
        
        if (code == KeyEvent.VK_SPACE) {
            OnlineUI.handleNameInput(' ');
            gamePanel.repaint();
            return;
        }
        
        if (code == KeyEvent.VK_E) {
            OnlineUI.exitOnlineMode();
            gamePanel.repaint();
            return;
        }
        
        if (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z) {
            char typedChar = (char) ('A' + code - KeyEvent.VK_A);
            OnlineUI.handleNameInput(typedChar);
            gamePanel.repaint();
        } else if (code >= KeyEvent.VK_0 && code <= KeyEvent.VK_9) {
            char typedChar = (char) ('0' + code - KeyEvent.VK_0);
            OnlineUI.handleNameInput(typedChar);
            gamePanel.repaint();
        }
        
        else if (Character.isLetterOrDigit(keyChar) && keyChar != KeyEvent.CHAR_UNDEFINED) {
            OnlineUI.handleNameInput(Character.toUpperCase(keyChar));
            gamePanel.repaint();
        }
    }

    private void handleReadyStateKeyPress(int code) {
        if (code == KeyEvent.VK_E && OnlineMatchManager.getInstance().isOnline()) {
            OnlineUI.exitOnlineMode();
            gamePanel.repaint();
            return;
        }
        
        if (code == KeyEvent.VK_TAB) {
            if (!OnlineMatchManager.getInstance().isOnline()) {
                OnlineUI.enterOnlineMode();
                gamePanel.repaint();
            }
            return;
        }

        if (code == KeyEvent.VK_SPACE) {
            gameState.isSpaceHeld = true;
            if (!OnlineMatchManager.getInstance().isOnline()) {
                gameState.resetRun(animController);
            }
            gamePanel.repaint();
            return;
        }

        if (code == KeyEvent.VK_BACK_SPACE) {
            uiSettings.showSettings = !uiSettings.showSettings;
            gamePanel.repaint();
            return;
        }

        if (code == KeyEvent.VK_LEFT && !OnlineMatchManager.getInstance().isOnline()) {
            animController.cycleCharacter(-1);
            uiSettings.arrowLeftFrame = uiSettings.arrowLeftPressed;
            gamePanel.repaint();
            return;
        }

        if (code == KeyEvent.VK_RIGHT && !OnlineMatchManager.getInstance().isOnline()) {
            animController.cycleCharacter(+1);
            uiSettings.arrowRightFrame = uiSettings.arrowRightPressed;
            gamePanel.repaint();
            return;
        }
    }

    private void handleGameOverStateKeyPress(int code) {
        if (code == KeyEvent.VK_SPACE) {
            gameState.isSpaceHeld = true;
            gameState.resetRun(animController);
            gamePanel.repaint();
            return;
        }
        
        if (code == KeyEvent.VK_TAB) {
            if (!OnlineMatchManager.getInstance().isOnline()) {
                OnlineUI.enterOnlineMode();
                gamePanel.repaint();
            }
            return;
        }

        if (code == KeyEvent.VK_BACK_SPACE) {
            uiSettings.showSettings = !uiSettings.showSettings;
            gamePanel.repaint();
            return;
        }

        if (code == KeyEvent.VK_LEFT && !OnlineMatchManager.getInstance().isOnline()) {
            animController.cycleCharacter(-1);
            gamePanel.repaint();
            return;
        }

        if (code == KeyEvent.VK_RIGHT && !OnlineMatchManager.getInstance().isOnline()) {
            animController.cycleCharacter(+1);
            gamePanel.repaint();
            return;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            uiSettings.arrowLeftFrame = uiSettings.arrowLeftNormal;
        }

        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            uiSettings.arrowRightFrame = uiSettings.arrowRightNormal;
        }

        if (e.getKeyCode() == KeyEvent.VK_SPACE)
            gameState.isSpaceHeld = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        OnlineUI.handleClick(e.getX(), e.getY(), gamePanel.getWidth(), gamePanel.getHeight());
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

}

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import client.*;
import shared.*;

public class GamePanel extends JPanel implements ActionListener {
    private final Timer timer;
    private GameState gameState;
    private final UISettings uiSettings;
    private GameRenderer renderer;
    private AnimationController animController;
    private InputHandler inputHandler;
    private SplashScreen splashScreen;
    private boolean showingSplash;

    private boolean isOnlineMode = false;

    public GamePanel() {
        this(true);
    }

    public GamePanel(boolean showSplash) {
        gameState = new GameState();
        uiSettings = new UISettings();
        gameState.setUISettings(uiSettings);
        animController = new AnimationController(gameState, uiSettings);
        renderer = new GameRenderer(this, gameState, uiSettings);
        timer = new Timer(16, this);

        initPanel();

        showingSplash = showSplash;
        isOnlineMode = false;

        if (showSplash) {
            int width = getWidth() > 0 ? getWidth() : 1100;
            int height = getHeight() > 0 ? getHeight() : 620;
            splashScreen = new SplashScreen(width, height);

            new Thread(() -> {
                initGameResources();
            }).start();
        } else {
            initGameResources();
            showMainMenu();
        }

        timer.start();
    }

    private void initGameResources() {
        gameState.initAtlas();
        gameState.warmupAtlas();
        uiSettings.layoutSettingsRects(getWidth(), getHeight());
        animController.initializeCharacters();
        gameState.updateGroundAndBases(getWidth(), getHeight());
        uiSettings.applyVolumeToPools(gameState);
        inputHandler = new InputHandler(this, gameState, uiSettings, animController);
    }

    private void initPanel() {
        setPreferredSize(new Dimension(1100, 620));
        setBackground(new Color(18, 20, 24));
        setFocusable(true);
        setDoubleBuffered(true);

        setFocusTraversalKeysEnabled(false);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                uiSettings.layoutSettingsRects(getWidth(), getHeight());
                gameState.updateGroundAndBases(getWidth(), getHeight());

                if (showingSplash && splashScreen != null) {
                    splashScreen.updateDimensions(getWidth(), getHeight());
                }
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (showingSplash && splashScreen != null && !splashScreen.isDone()) {
            splashScreen.draw((Graphics2D) g);
        } else {
            renderer.render(g);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.currentTimeMillis();

        if (showingSplash && splashScreen != null) {
            splashScreen.update();

            if (splashScreen.isDone()) {
                showingSplash = false;
            }
        } else {
            gameState.update(now, animController);

            if (uiSettings.showSettings && uiSettings.sliderKnob != null && uiSettings.draggingSlider) {
                int clamped = Math.max(
                        uiSettings.sliderTrack.x,
                        Math.min(
                                uiSettings.sliderTrack.x + uiSettings.sliderTrack.width,
                                uiSettings.sliderKnob.x + uiSettings.sliderKnob.width / 2));
                uiSettings.sliderKnob.x = clamped - uiSettings.sliderKnob.width / 2;
            }
        }

        repaint();
    }

    public void createOnlineRoom() {
        OnlineRoomDialog dialog = new OnlineRoomDialog(this, uiSettings);
        dialog.setVisible(true);
    }

    public GameState getGameState() {
        return gameState;
    }

    public boolean isOnlineMode() {
        return isOnlineMode;
    }

    public void setOnlineMode(boolean online) {
        this.isOnlineMode = online;
    }

    public void exitOnlineMode() {
        isOnlineMode = false;
        setFocusable(true);
        requestFocusInWindow();
        revalidate();
        repaint();
    }

    private void showMainMenu() {

    }

    public void startOfflineGame(String playerName, int characterIndex) {
        isOnlineMode = false;
        removeAll();

        gameState.resetToReady();
        gameState.startGame();

        if (inputHandler == null) {
            inputHandler = new InputHandler(this, gameState, uiSettings, animController);
        }

        setFocusable(true);
        requestFocusInWindow();

        System.out.println("Started offline game - Player: " + playerName + ", Character: " + characterIndex);
    }

    public void startOnlineGame(Player player, NetworkClient client, String roomId, int characterIndex) {
        isOnlineMode = true;
        removeAll();

        gameState.resetToReady();
        gameState.setMultiplayerMode(true);

        if (inputHandler == null) {
            inputHandler = new InputHandler(this, gameState, uiSettings, animController);
        }

        setFocusable(true);
        requestFocusInWindow();

        System.out.println("Started online game - Player: " + player.name + ", Room: " + roomId + ", Character: "
                + characterIndex);
    }

    public void startOnlineGame() {
        isOnlineMode = true;
        removeAll();

        gameState.resetToReady();
        gameState.startGame();

        if (inputHandler == null) {
            inputHandler = new InputHandler(this, gameState, uiSettings, animController);
        }

        setFocusable(true);
        requestFocusInWindow();

        System.out.println("Started online game");
    }

    public void startOnlineGame(GameRoom room, NetworkClient client, Player player, boolean isHost) {
        isOnlineMode = true;
        removeAll();

        gameState.resetToReady();
        gameState.setMultiplayerMode(true);
        gameState.setCurrentWord(room.currentWord);

        // Set opponent name and character (the other player in the room)
        for (Player p : room.players) {
            if (!p.id.equals(player.id)) {
                gameState.setOpponentName(p.name);
                // Set opponent character pack to match the current player's character
                gameState.setOpponentCharacterPack(gameState.player);
                break;
            }
        }

        if (inputHandler == null) {
            inputHandler = new InputHandler(this, gameState, uiSettings, animController);
        }

        inputHandler.setNetworkClient(client);
        inputHandler.setLocalPlayer(player);

        setFocusable(true);
        requestFocusInWindow();

        if (isHost) {
            gameState.setStatusMessage("รอผู้เล่นเข้าร่วม...");
        } else {
            gameState.setStatusMessage("เข้าร่วมห้องแล้ว รอเริ่มเกม...");
        }
    }

    public void handleOnlineMessage(NetworkMessage message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.type) {
                case COUNTDOWN_START:
                    gameState.setStatusMessage("ผู้เล่นครบแล้ว! กำลังเริ่มเกม...");
                    break;
                case COUNTDOWN_UPDATE:
                    if (message.data instanceof Integer) {
                        int countdown = (Integer) message.data;
                        gameState.setStatusMessage("เริ่มเกมใน " + countdown + " วินาที");
                    }
                    break;
                case GAME_START:
                    if (message.data instanceof String) {
                        String firstWord = (String) message.data;
                        gameState.setCurrentWord(firstWord);
                        gameState.startGame();
                        gameState.setStatusMessage("เริ่มแข่ง!");
                    }
                    break;
                case PLAYER_JOIN:
                    gameState.setStatusMessage("ผู้เล่นใหม่เข้าร่วม...");
                    break;
                case PLAYER_PROGRESS:
                    if (message.data instanceof Integer) {
                        int opponentProgress = (Integer) message.data;
                        gameState.setOpponentIdx(opponentProgress);
                        // Don't set status message for progress updates to avoid spam
                    }
                    break;
                default:
                    // Handle other message types if needed
                    break;
            }
            repaint();
        });
    }
}

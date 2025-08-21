import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GamePanel extends JPanel implements ActionListener {
    private final Timer timer;
    private final GameState gameState;
    private final UISettings uiSettings;
    private final GameRenderer renderer;
    private final AnimationController animController;
    private InputHandler inputHandler;
    private SplashScreen splashScreen;
    private boolean showingSplash;

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
        if (showSplash) {
            int width = getWidth() > 0 ? getWidth() : 1100;
            int height = getHeight() > 0 ? getHeight() : 620;
            splashScreen = new SplashScreen(width, height);

            new Thread(() -> {
                initGameResources();
            }).start();
        } else {
            initGameResources();
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
                if (inputHandler == null) {
                    inputHandler = new InputHandler(this, gameState, uiSettings, animController);
                }
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

}

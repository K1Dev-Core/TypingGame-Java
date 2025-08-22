
import client.NetworkClient;
import java.util.UUID;
import javax.swing.SwingUtilities;
import shared.*;

public class OnlineMatchManager implements NetworkClient.NetworkListener {

    private static OnlineMatchManager instance;
    private NetworkClient networkClient;
    private GamePanel gamePanel;
    private GameState gameState;
    private UISettings uiSettings;
    private Player localPlayer;
    private Player opponent;
    private String currentRoomId;
    private boolean isConnected = false;
    private boolean isHost = false;
    private MatchState matchState = MatchState.OFFLINE;
    private int currentCountdown = 0;
    private int previousCountdown = -1;
    private long countdownStartTime = 0;
    private boolean gameOverOverlayShowing = false;
    private String gameOverResult = "";
    private long gameOverOverlayStartTime = 0;
    private static final int GAME_OVER_OVERLAY_DURATION = 3000;

    public enum MatchState {
        OFFLINE, CONNECTING, CONNECTED, WAITING_FOR_OPPONENT, PROFILE_DISPLAY, COUNTDOWN, RACING, FINISHED
    }

    private OnlineMatchManager() {
        networkClient = new NetworkClient(this);
    }

    public static OnlineMatchManager getInstance() {
        if (instance == null) {
            instance = new OnlineMatchManager();
        }
        return instance;
    }

    public void initialize(GamePanel gamePanel, GameState gameState, UISettings uiSettings) {
        this.gamePanel = gamePanel;
        this.gameState = gameState;
        this.uiSettings = uiSettings;
    }

    public boolean connectToServer(String host, int port) {
        if (isConnected) {
            return true;
        }

        matchState = MatchState.CONNECTING;
        NotificationSystem.showInfo("Connecting...");

        if (networkClient.connect(host, port)) {
            isConnected = true;
            matchState = MatchState.CONNECTED;
            NotificationSystem.showSuccess("Connected!");
            return true;
        } else {
            matchState = MatchState.OFFLINE;
            NotificationSystem.showError("Connection failed");
            return false;
        }
    }

    public boolean connectToServer() {
        return connectToServer("89.38.101.103", 8888);
    }

    public void cancelMatchmaking() {
        if (matchState == MatchState.WAITING_FOR_OPPONENT || matchState == MatchState.COUNTDOWN || matchState == MatchState.PROFILE_DISPLAY) {
            if (currentRoomId != null && localPlayer != null) {
                NetworkMessage leaveMsg = new NetworkMessage(
                        NetworkMessage.MessageType.LEAVE_ROOM,
                        localPlayer.id,
                        currentRoomId,
                        null
                );
                networkClient.sendMessage(leaveMsg);
                currentRoomId = null;
            }

            matchState = MatchState.CONNECTED;
            opponent = null;
            localPlayer = null;
            currentCountdown = 0;
            previousCountdown = -1;

            if (gameState != null) {
                gameState.setMultiplayerMode(false);
                gameState.resetToReady();
            }

            NotificationSystem.showInfo("Cancelled");
        }
    }

    public void startMatchmaking(String playerName) {
        if (!isConnected) {
            if (!connectToServer("89.38.101.103", 8888)) {
                return;
            }
        }

        String selectedCharId = getCurrentSelectedCharacter();

        localPlayer = new Player(UUID.randomUUID().toString(), playerName, selectedCharId);

        if (gameState != null) {
            CharacterConfig config = CharacterConfig.getInstance();
            CharacterPack localCharPack = config.createCharacterPack(
                    selectedCharId,
                    gameState.playerBaseX,
                    gameState.groundY,
                    false
            );
            gameState.player = localCharPack;
        }

        NetworkMessage playerInfoMsg = new NetworkMessage(
                NetworkMessage.MessageType.PLAYER_JOIN,
                localPlayer.id,
                null,
                localPlayer
        );
        networkClient.sendMessage(playerInfoMsg);

        String roomName = "QuickMatch_General";
        NetworkMessage createRoomMsg = new NetworkMessage(
                NetworkMessage.MessageType.CREATE_ROOM,
                localPlayer.id,
                null,
                roomName
        );

        matchState = MatchState.WAITING_FOR_OPPONENT;
        NotificationSystem.showInfo("Searching...");
        networkClient.sendMessage(createRoomMsg);
    }

    private String getCurrentSelectedCharacter() {
        CharacterConfig config = CharacterConfig.getInstance();
        String[] playerCharIds = config.getPlayerCharacterIds();

        if (playerCharIds.length == 0) {
            return "medieval_king";
        }

        String selectedCharId = "medieval_king";

        if (uiSettings != null) {
            int selectedIdx = uiSettings.selectedCharIdx;
            if (selectedIdx >= 0 && selectedIdx < playerCharIds.length) {
                selectedCharId = playerCharIds[selectedIdx];
            } else {
                for (String charId : playerCharIds) {
                    if ("medieval_king".equals(charId)) {
                        selectedCharId = charId;
                        uiSettings.selectedCharIdx = java.util.Arrays.asList(playerCharIds).indexOf(charId);
                        break;
                    }
                }
                if (!"medieval_king".equals(selectedCharId)) {
                    selectedCharId = playerCharIds[0];
                    uiSettings.selectedCharIdx = 0;
                }
            }
        } else {
            for (String charId : playerCharIds) {
                if ("medieval_king".equals(charId)) {
                    selectedCharId = charId;
                    break;
                }
            }
            if (!"medieval_king".equals(selectedCharId)) {
                selectedCharId = playerCharIds[0];
            }
        }

        return selectedCharId;
    }

    public void updateLocalPlayerCharacter() {
        if (localPlayer != null) {
            String newCharId = getCurrentSelectedCharacter();
            if (!newCharId.equals(localPlayer.selectedCharacterId)) {
                localPlayer.selectedCharacterId = newCharId;

                if (gameState != null) {
                    CharacterConfig config = CharacterConfig.getInstance();
                    CharacterPack newPlayerCharPack = config.createCharacterPack(
                            newCharId,
                            gameState.playerBaseX,
                            gameState.groundY,
                            false
                    );
                    gameState.player = newPlayerCharPack;
                }

                if (isConnected) {
                    NetworkMessage updateMsg = new NetworkMessage(
                            NetworkMessage.MessageType.PLAYER_JOIN,
                            localPlayer.id,
                            currentRoomId,
                            localPlayer
                    );
                    networkClient.sendMessage(updateMsg);
                }
            }
        }
    }

    public void sendPlayerProgress(int progress) {
        if (isConnected && currentRoomId != null && localPlayer != null) {
            NetworkMessage progressMsg = new NetworkMessage(
                    NetworkMessage.MessageType.PLAYER_PROGRESS,
                    localPlayer.id,
                    currentRoomId,
                    progress
            );
            networkClient.sendMessage(progressMsg);
        }
    }

    public void handleCharacterTyped(char c) {
        if (matchState != MatchState.RACING) {
            return;
        }
    }

    public void handleWordCompleted() {
        if (matchState != MatchState.RACING || !isConnected || currentRoomId == null || localPlayer == null) {
            return;
        }

        gameState.startPlayerAttackSequence();

        NetworkMessage wordCompleteMsg = new NetworkMessage(
                NetworkMessage.MessageType.PLAYER_TYPED,
                localPlayer.id,
                currentRoomId,
                "WORD_COMPLETE"
        );
        networkClient.sendMessage(wordCompleteMsg);

        NotificationSystem.showSuccess("Word complete!");
    }

    @Override
    public void onMessageReceived(NetworkMessage message) {
        if (message == null || message.type == null) {
            System.err.println("Received invalid message: " + message);
            return;
        }

        try {
            switch (message.type) {
                case CREATE_ROOM:
                    handleRoomCreated(message);
                    break;
                case PLAYER_JOIN:
                    handlePlayerJoined(message);
                    break;
                case COUNTDOWN_START:
                    handleCountdownStart(message);
                    break;
                case COUNTDOWN_UPDATE:
                    handleCountdownUpdate(message);
                    break;
                case GAME_START:
                    handleGameStart(message);
                    break;
                case PLAYER_PROGRESS:
                    handlePlayerProgress(message);
                    break;
                case PLAYER_TYPED:
                    handlePlayerTyped(message);
                    break;
                case GAME_STATE_UPDATE:
                    handleGameStateUpdate(message);
                    break;
                case ROOM_UPDATE:
                    handleRoomUpdate(message);
                    break;
                case GAME_OVER:
                    handleGameOver(message);
                    break;
                case PLAYER_DISCONNECTED:
                    handlePlayerDisconnected(message);
                    break;
                default:
                    System.err.println("Unknown message type: " + message.type);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error handling message " + message.type + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleRoomCreated(NetworkMessage message) {
        currentRoomId = (String) message.data;
        isHost = true;
        NotificationSystem.showInfo("Room created!");
    }

    private void handlePlayerJoined(NetworkMessage message) {
        if (message.data instanceof Player) {
            Player joinedPlayer = (Player) message.data;

            if (!joinedPlayer.id.equals(localPlayer.id)) {
                if (opponent == null) {
                    opponent = joinedPlayer;
                    gameState.setOpponentName(opponent.name);
                } else {
                    opponent.selectedCharacterId = joinedPlayer.selectedCharacterId;
                }

                if (opponent.selectedCharacterId != null) {
                    CharacterConfig config = CharacterConfig.getInstance();
                    CharacterPack opponentCharPack = config.createCharacterPack(
                            opponent.selectedCharacterId,
                            gameState.botBaseX,
                            gameState.groundY,
                            true
                    );
                    gameState.setOpponentCharacterPack(opponentCharPack);
                    gameState.bot = opponentCharPack;
                    gameState.opponent = opponentCharPack;

                    if (gamePanel != null) {
                        gamePanel.repaint();
                    }
                } else {
                    CharacterConfig config = CharacterConfig.getInstance();
                    CharacterPack defaultCharPack = config.createCharacterPack(
                            "medieval_king",
                            gameState.botBaseX,
                            gameState.groundY,
                            true
                    );
                    gameState.setOpponentCharacterPack(defaultCharPack);
                    gameState.bot = defaultCharPack;
                    gameState.opponent = defaultCharPack;
                }

                if (opponent.name != null && opponent == joinedPlayer) {
                    NotificationSystem.showSuccess("Opponent found: " + opponent.name);
                }

            }
        }
    }

    private void handleCountdownStart(NetworkMessage message) {
        System.out.println("=== COUNTDOWN START RECEIVED ===");
        System.out.println("Previous state: " + matchState);
        matchState = MatchState.COUNTDOWN;
        previousCountdown = -1;
        if (message.data instanceof Integer) {
            currentCountdown = (Integer) message.data;
            System.out.println("Countdown value: " + currentCountdown);
        } else {
            currentCountdown = 10;
            System.out.println("Using fallback countdown: " + currentCountdown);
        }
        countdownStartTime = System.currentTimeMillis();
        System.out.println("New state: " + matchState + ", Start time: " + countdownStartTime);
        NotificationSystem.showSuccess("Match starting!");

        if (gamePanel != null) {
            gamePanel.repaint();
        }
    }

    private void handleCountdownUpdate(NetworkMessage message) {
        if (message.data instanceof Integer) {
            int countdown = (Integer) message.data;
            previousCountdown = currentCountdown;
            currentCountdown = countdown;
            System.out.println("Countdown update: " + countdown);

            if (countdown != previousCountdown) {
                if (countdown >= 1 && countdown <= 5) {

                    if (gameState != null && gameState.sCountdown != null && uiSettings != null && uiSettings.isAudible()) {
                        gameState.sCountdown.play();
                    }
                    if (countdown == 1) {

                        if (gameState != null && gameState.sStart != null && uiSettings != null && uiSettings.isAudible()) {
                            gameState.sStart.play();
                        }
                    }
                }
            }

            if (gamePanel != null) {
                gamePanel.repaint();
            }

            if (countdown > 0) {
                NotificationSystem.showWarning("Starting in " + countdown);
            }
        }
    }

    private void handleGameStart(NetworkMessage message) {
        if (message.data instanceof String) {
            String firstWord = (String) message.data;

            if (firstWord == null || firstWord.trim().isEmpty()) {
                System.err.println("Invalid first word received: " + firstWord);
                return;
            }

            matchState = MatchState.RACING;

            gameState.setMultiplayerMode(true);

            gameState.setCurrentWord(firstWord);
            gameState.playerIdx = 0;
            gameState.setOpponentIdx(0);

            if (localPlayer != null && localPlayer.name != null) {
                gameState.setPlayerName(localPlayer.name);
            }
            if (opponent != null && opponent.name != null) {
                gameState.setOpponentName(opponent.name);
            }

            gameState.playerHealth = GameConfig.MAX_HEALTH;
            gameState.botHealth = GameConfig.MAX_HEALTH;
            gameState.wordsCompleted = 0;

            if (localPlayer != null) {
                localPlayer.health = GameConfig.MAX_HEALTH;
                localPlayer.wordsCompleted = 0;
            }
            if (opponent != null) {
                opponent.health = GameConfig.MAX_HEALTH;
                opponent.wordsCompleted = 0;
            }

            gameState.resetToReady();
            gameState.startGame();

            gameState.resetTypingProgress();

            if (gamePanel != null) {
                gamePanel.repaint();
            }

            NotificationSystem.showSuccess("Fight! Type: " + firstWord);

            System.out.println("Game started successfully. Player health: " + gameState.playerHealth
                    + ", Bot health: " + gameState.botHealth + ", Word: " + firstWord);
        } else {
            System.err.println("Invalid GAME_START message data: "
                    + (message.data != null ? message.data.getClass() : "null"));
        }
    }

    private void handlePlayerProgress(NetworkMessage message) {
        if (message.data instanceof Integer && !message.playerId.equals(localPlayer.id)) {
            int opponentProgress = (Integer) message.data;
            gameState.setOpponentIdx(opponentProgress);
        }
    }

    private void handlePlayerTyped(NetworkMessage message) {

        if (message.data instanceof String && !message.playerId.equals(localPlayer.id)) {
            String eventType = (String) message.data;
            if ("WORD_COMPLETE".equals(eventType)) {

                gameState.startBotAttackSequence();

                if (gamePanel != null) {
                    gamePanel.repaint();
                }

                NotificationSystem.showWarning("Hit taken!");

            }
        } else if (message.data instanceof String && message.playerId.equals(localPlayer.id)) {
            String eventType = (String) message.data;
            if ("WORD_COMPLETE".equals(eventType)) {

                NotificationSystem.showSuccess("Attack hit!");
            }
        }
    }

    private void handleGameStateUpdate(NetworkMessage message) {

        if (message.data instanceof String) {
            String newWord = (String) message.data;
            String oldWord = gameState.getCurrentWord();

            gameState.playerIdx = 0;
            gameState.setOpponentIdx(0);

            gameState.resetTypingProgress();

            gameState.popUntil = 0;

            gameState.setCurrentWord(newWord);

            if (gamePanel != null) {
                gamePanel.requestFocusInWindow();
                gamePanel.repaint();
            }

        } else if (message.data instanceof GameRoom) {
            GameRoom room = (GameRoom) message.data;

            if (room.currentWord != null && !room.currentWord.equals(gameState.getCurrentWord())) {
                String oldWord = gameState.getCurrentWord();
                gameState.setCurrentWord(room.currentWord);
                gameState.playerIdx = 0;
                gameState.setOpponentIdx(0);
                gameState.resetTypingProgress();

                if (gamePanel != null) {
                    gamePanel.repaint();
                }

            }

            for (Player serverPlayer : room.players) {
                if (serverPlayer.id.equals(localPlayer.id)) {
                    gameState.playerHealth = serverPlayer.health;
                    localPlayer.health = serverPlayer.health;
                } else {
                    gameState.botHealth = serverPlayer.health;
                    if (opponent != null) {
                        opponent.health = serverPlayer.health;
                    }
                }
            }

        }
    }

    private void handleRoomUpdate(NetworkMessage message) {
        if (message.data instanceof GameRoom) {
            GameRoom room = (GameRoom) message.data;

            for (Player serverPlayer : room.players) {
                if (serverPlayer.id.equals(localPlayer.id)) {
                    int oldHealth = localPlayer.health;
                    int oldGameHealth = gameState.playerHealth;
                    int oldWords = localPlayer.wordsCompleted;

                    localPlayer.health = serverPlayer.health;
                    localPlayer.wordsCompleted = serverPlayer.wordsCompleted;

                    if (gameState.playerHealth != serverPlayer.health) {
                        gameState.playerHealth = serverPlayer.health;
                        if (serverPlayer.health < oldGameHealth) {
                            gameState.playerTakingHit = true;
                            gameState.playerHitUntil = System.currentTimeMillis() + 500;
                        }
                    }

                } else {
                    if (opponent == null) {
                        opponent = new Player(serverPlayer.id, serverPlayer.name, serverPlayer.selectedCharacterId);
                        gameState.setOpponentName(opponent.name);

                        if (opponent.selectedCharacterId != null) {
                            CharacterConfig config = CharacterConfig.getInstance();
                            CharacterPack opponentCharPack = config.createCharacterPack(
                                    opponent.selectedCharacterId,
                                    gameState.botBaseX,
                                    gameState.groundY,
                                    true
                            );
                            gameState.setOpponentCharacterPack(opponentCharPack);
                            gameState.bot = opponentCharPack;
                            gameState.opponent = opponentCharPack;
                        } else {
                            CharacterConfig config = CharacterConfig.getInstance();
                            CharacterPack defaultCharPack = config.createCharacterPack(
                                    "medieval_king",
                                    gameState.botBaseX,
                                    gameState.groundY,
                                    true
                            );
                            gameState.setOpponentCharacterPack(defaultCharPack);
                            gameState.bot = defaultCharPack;
                            gameState.opponent = defaultCharPack;
                        }
                    } else {
                        int oldHealth = opponent.health;
                        int oldBotHealth = gameState.botHealth;
                        int oldWords = opponent.wordsCompleted;

                        opponent.health = serverPlayer.health;
                        opponent.wordsCompleted = serverPlayer.wordsCompleted;
                        opponent.selectedCharacterId = serverPlayer.selectedCharacterId;

                        if (gameState.botHealth != serverPlayer.health) {
                            gameState.botHealth = serverPlayer.health;
                            if (serverPlayer.health < oldBotHealth) {
                                gameState.opponentTakingHit = true;
                                gameState.opponentHitUntil = System.currentTimeMillis() + 500;
                            }
                        }
                    }
                }
            }

            if (gamePanel != null) {
                gamePanel.repaint();
            }

            boolean validGameState = matchState == MatchState.RACING
                    && gameState.playerHealth > 0 && gameState.botHealth > 0;

            if (gameState.playerHealth <= 0 && validGameState) {
                gameOverResult = "DEFEAT";
                gameOverOverlayShowing = true;
                gameOverOverlayStartTime = System.currentTimeMillis();
                matchState = MatchState.FINISHED;
                if (localPlayer != null) {
                    String characterUsed = getCurrentSelectedCharacter();
                    PlayerDatabase.saveOnlineMatchResult(localPlayer.name, false, characterUsed);
                    PlayerDatabase.savePlayerScore(localPlayer.name, localPlayer.wordsCompleted, 0);
                }
                scheduleReturnToOnlineMode();

            } else if (gameState.botHealth <= 0 && validGameState) {
                gameOverResult = "VICTORY";
                gameOverOverlayShowing = true;
                gameOverOverlayStartTime = System.currentTimeMillis();
                matchState = MatchState.FINISHED;
                if (localPlayer != null) {
                    String characterUsed = getCurrentSelectedCharacter();
                    PlayerDatabase.saveOnlineMatchResult(localPlayer.name, true, characterUsed);
                    PlayerDatabase.savePlayerScore(localPlayer.name, localPlayer.wordsCompleted, 0);
                }
                scheduleReturnToOnlineMode();
            }

        }
    }

    private void handleGameOver(NetworkMessage message) {
        if (message.data instanceof String) {
            String result = (String) message.data;
            boolean won = result.equals("WIN");
            gameOverResult = won ? "VICTORY" : "DEFEAT";
            gameOverOverlayShowing = true;
            gameOverOverlayStartTime = System.currentTimeMillis();
            matchState = MatchState.FINISHED;

            if (localPlayer != null) {
                String characterUsed = getCurrentSelectedCharacter();
                PlayerDatabase.saveOnlineMatchResult(localPlayer.name, won, characterUsed);
            }
        }

        if (localPlayer != null) {
            PlayerDatabase.savePlayerScore(localPlayer.name, localPlayer.wordsCompleted, 0);
        }

        scheduleReturnToOnlineMode();
    }

    private void handlePlayerDisconnected(NetworkMessage message) {
        if (matchState == MatchState.RACING) {
            gameOverResult = "VICTORY";
            gameOverOverlayShowing = true;
            gameOverOverlayStartTime = System.currentTimeMillis();
            matchState = MatchState.FINISHED;

            if (localPlayer != null) {
                String characterUsed = getCurrentSelectedCharacter();
                PlayerDatabase.saveOnlineMatchResult(localPlayer.name, true, characterUsed);
                PlayerDatabase.savePlayerScore(localPlayer.name, localPlayer.wordsCompleted, 0);
            }

            scheduleReturnToOnlineMode();
        } else if (matchState == MatchState.WAITING_FOR_OPPONENT || matchState == MatchState.COUNTDOWN) {
            NotificationSystem.showWarning("Opponent left");
            matchState = MatchState.CONNECTED;
            opponent = null;
            currentRoomId = null;

            if (gameState != null) {
                gameState.resetToReady();
            }
        }
    }

    private void scheduleReturnToOnlineMode() {

        new Thread(() -> {
            try {
                Thread.sleep(3000);

                resetToOnlineReady();

                if (gamePanel != null) {
                    SwingUtilities.invokeLater(() -> {
                        gamePanel.repaint();
                    });
                }

                NotificationSystem.showInfo("Ready for next match! Press + to find opponent");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void scheduleReturnToMainMenu() {

        new Thread(() -> {
            try {
                Thread.sleep(3000);

                resetToMainMenu();

                if (gamePanel != null) {
                    SwingUtilities.invokeLater(() -> {
                        gamePanel.repaint();
                    });
                }

                NotificationSystem.showInfo("Ready");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void resetToOnlineReady() {

        String savedPlayerName = localPlayer != null ? localPlayer.name : null;
        String savedCharacterId = localPlayer != null ? localPlayer.selectedCharacterId : null;

        matchState = MatchState.CONNECTED;
        isHost = false;
        currentRoomId = null;
        opponent = null;
        currentCountdown = 0;
        previousCountdown = -1;
        countdownStartTime = 0;
        gameOverOverlayShowing = false;
        gameOverResult = "";
        gameOverOverlayStartTime = 0;

        if (savedPlayerName != null) {
            localPlayer = new Player(java.util.UUID.randomUUID().toString(), savedPlayerName, savedCharacterId);
        }

        if (gameState != null) {
            gameState.resetToReady();
            gameState.setOpponentName("");
            if (savedPlayerName != null) {
                gameState.setPlayerName(savedPlayerName);
            }
            gameState.setStatusMessage("Ready for online match");
            gameState.wordsCompleted = 0;

            resetCharacterAnimations();
        }

        OnlineUI.resetToMatchReady();
    }

    public void resetToMainMenu() {

        matchState = MatchState.CONNECTED;
        isHost = false;
        currentRoomId = null;
        opponent = null;
        localPlayer = null;
        currentCountdown = 0;
        previousCountdown = -1;
        countdownStartTime = 0;
        gameOverOverlayShowing = false;
        gameOverResult = "";
        gameOverOverlayStartTime = 0;

        if (gameState != null) {
            gameState.resetToReady();
            gameState.setOpponentName("");
            gameState.setPlayerName("");
            gameState.setStatusMessage("Ready for online match");
            gameState.wordsCompleted = 0;

            resetCharacterAnimations();
        }

        OnlineUI.resetToMatchReady();

    }

    private void resetCharacterAnimations() {
        if (gameState != null) {

            if (gameState.player != null) {
                try {
                    gameState.player.setAnim(CharacterPack.Anim.IDLE);
                    gameState.player.x = gameState.playerBaseX;
                    gameState.player.y = gameState.groundY;
                } catch (Exception e) {
                }
            }

            if (gameState.bot != null) {
                try {
                    gameState.bot.setAnim(CharacterPack.Anim.IDLE);
                    gameState.bot.x = gameState.botBaseX;
                    gameState.bot.y = gameState.groundY;
                } catch (Exception e) {
                }
            }

            if (gameState.isMultiplayerMode() && gameState.bot != null) {
                gameState.opponent = gameState.bot;
            }

            gameState.playerSeq = false;
            gameState.botSeq = false;
            gameState.playerPhase = 0;
            gameState.botPhase = 0;
            gameState.playerTakingHit = false;
            gameState.opponentTakingHit = false;
        }
    }

    @Override
    public void onDisconnected() {

        isConnected = false;
        matchState = MatchState.OFFLINE;
        currentRoomId = null;
        opponent = null;
        localPlayer = null;
        isHost = false;
        currentCountdown = 0;
        previousCountdown = -1;
        countdownStartTime = 0;

        if (gameState != null) {
            gameState.setMultiplayerMode(false);
            gameState.resetToReady();
            gameState.setOpponentName("");
            gameState.setPlayerName("");
            gameState.setStatusMessage("");

            gameState.playerHealth = GameConfig.MAX_HEALTH;
            gameState.botHealth = GameConfig.MAX_HEALTH;
        }

        if (gamePanel != null) {
            SwingUtilities.invokeLater(() -> {
                gamePanel.repaint();
            });
        }

        NotificationSystem.showWarning("Disconnected");
    }

    public void disconnect() {
        if (networkClient != null) {
            networkClient.disconnect();
        }
        onDisconnected();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isOnline() {
        return matchState != MatchState.OFFLINE;
    }

    public boolean isRacing() {
        return matchState == MatchState.RACING;
    }

    public MatchState getMatchState() {
        return matchState;
    }

    public Player getLocalPlayer() {
        return localPlayer;
    }

    public Player getOpponent() {
        return opponent;
    }

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    public int getCurrentCountdown() {
        return currentCountdown;
    }

    public long getCountdownStartTime() {
        return countdownStartTime;
    }

    public boolean isGameOverOverlayShowing() {
        if (!gameOverOverlayShowing) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - gameOverOverlayStartTime;
        if (elapsed >= GAME_OVER_OVERLAY_DURATION) {
            gameOverOverlayShowing = false;
            return false;
        }
        return true;
    }

    public String getGameOverResult() {
        return gameOverResult;
    }

    public long getGameOverOverlayStartTime() {
        return gameOverOverlayStartTime;
    }
}

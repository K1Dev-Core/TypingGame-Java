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
    private long countdownStartTime = 0;
    
    public enum MatchState {
        OFFLINE, CONNECTING, CONNECTED, WAITING_FOR_OPPONENT, COUNTDOWN, RACING, FINISHED
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
        NotificationSystem.showInfo("Connecting to server...");
        
        if (networkClient.connect(host, port)) {
            isConnected = true;
            matchState = MatchState.CONNECTED;
            NotificationSystem.showSuccess("Connected to server!");
            return true;
        } else {
            matchState = MatchState.OFFLINE;
            NotificationSystem.showError("Failed to connect to server");
            return false;
        }
    }
    
    public void cancelMatchmaking() {
        if (matchState == MatchState.WAITING_FOR_OPPONENT || matchState == MatchState.COUNTDOWN) {
            System.out.println("=== CANCELING MATCHMAKING ===");
            
            // Leave current room if in one
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
            
            // Reset match state
            matchState = MatchState.CONNECTED;
            opponent = null;
            localPlayer = null;
            currentCountdown = 0;
            
            // Reset game state to menu
            if (gameState != null) {
                gameState.setMultiplayerMode(false);
                gameState.resetToReady();
            }
            
            NotificationSystem.showInfo("Matchmaking canceled");
            System.out.println("=== MATCHMAKING CANCELED ===");
        }
    }

    public void startMatchmaking(String playerName) {
        if (!isConnected) {
            if (!connectToServer("localhost", 8888)) {
                return;
            }
        }
        
        String selectedCharId = getCurrentSelectedCharacter();
        
        localPlayer = new Player(UUID.randomUUID().toString(), playerName, selectedCharId);
        
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
        NotificationSystem.showInfo("Searching for opponent...");
        networkClient.sendMessage(createRoomMsg);
    }
    
    private String getCurrentSelectedCharacter() {
        CharacterConfig config = CharacterConfig.getInstance();
        String[] playerCharIds = config.getPlayerCharacterIds();
        String selectedCharId = "medieval_king";
        
        if (uiSettings != null && playerCharIds.length > 0) {
            int selectedIdx = uiSettings.selectedCharIdx;
            if (selectedIdx >= 0 && selectedIdx < playerCharIds.length) {
                selectedCharId = playerCharIds[selectedIdx];
            }
        }
        
        return selectedCharId;
    }
    
    public void updateLocalPlayerCharacter() {
        if (localPlayer != null) {
            String newCharId = getCurrentSelectedCharacter();
            if (!newCharId.equals(localPlayer.selectedCharacterId)) {
                localPlayer.selectedCharacterId = newCharId;
                
                // Send update immediately if connected, regardless of room state
                if (isConnected) {
                    NetworkMessage updateMsg = new NetworkMessage(
                        NetworkMessage.MessageType.PLAYER_JOIN,
                        localPlayer.id,
                        currentRoomId,
                        localPlayer
                    );
                    networkClient.sendMessage(updateMsg);
                    System.out.println("📡 Sent character update: " + newCharId + " to server");
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
        if (matchState != MatchState.RACING) return;
        
        // For now, we'll focus on progress tracking rather than character-by-character typing events
        // The main game logic already handles character validation
    }
    
    public void handleWordCompleted() {
        if (matchState != MatchState.RACING || !isConnected || currentRoomId == null || localPlayer == null) {
            return;
        }
        
        // Trigger player attack animation (same as bot mode)
        gameState.startPlayerAttackSequence();
        
        // Send word completion to server
        NetworkMessage wordCompleteMsg = new NetworkMessage(
            NetworkMessage.MessageType.PLAYER_TYPED,
            localPlayer.id,
            currentRoomId,
            "WORD_COMPLETE"
        );
        networkClient.sendMessage(wordCompleteMsg);
        
        NotificationSystem.showSuccess("Word completed! Attacking opponent...");
    }
    
    @Override
    public void onMessageReceived(NetworkMessage message) {
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
        }
    }
    
    private void handleRoomCreated(NetworkMessage message) {
        currentRoomId = (String) message.data;
        isHost = true;
        NotificationSystem.showInfo("Room created! Waiting for opponent...");
    }
    
    private void handlePlayerJoined(NetworkMessage message) {
        if (message.data instanceof Player) {
            Player joinedPlayer = (Player) message.data;
            System.out.println("=== PLAYER_JOINED DEBUG ===");
            System.out.println("Joined player: " + joinedPlayer.name + " (ID: " + joinedPlayer.id + ") Character: " + joinedPlayer.selectedCharacterId);
            System.out.println("Local player: " + (localPlayer != null ? localPlayer.name + " (ID: " + localPlayer.id + ")" : "null"));
            
            if (!joinedPlayer.id.equals(localPlayer.id)) {
                // This is an opponent or opponent character update
                if (opponent == null) {
                    // New opponent
                    opponent = joinedPlayer;
                    gameState.setOpponentName(opponent.name);
                    System.out.println("🎯 New opponent: " + opponent.name + " with character: " + opponent.selectedCharacterId);
                } else {
                    // Update existing opponent (character change)
                    System.out.println("🔄 Character update: " + opponent.name + " changed from " + 
                                     opponent.selectedCharacterId + " to " + joinedPlayer.selectedCharacterId);
                    opponent.selectedCharacterId = joinedPlayer.selectedCharacterId;
                }
                
                System.out.println("Setting opponent: " + opponent.name + " with character: " + opponent.selectedCharacterId);
                
                // Set opponent character based on their selection
                if (opponent.selectedCharacterId != null) {
                    CharacterConfig config = CharacterConfig.getInstance();
                    CharacterPack opponentCharPack = config.createCharacterPack(
                        opponent.selectedCharacterId,
                        gameState.botBaseX,
                        gameState.groundY,
                        true  // facing left
                    );
                    gameState.setOpponentCharacterPack(opponentCharPack);
                    // Also update the bot field for rendering compatibility
                    gameState.bot = opponentCharPack;
                    System.out.println("✓ Successfully set opponent character: " + opponent.selectedCharacterId);
                    
                    // Force UI update to show character change immediately
                    if (gamePanel != null) {
                        gamePanel.repaint();
                    }
                } else {
                    System.out.println("⚠ WARNING: Opponent has no character selection, using default");
                    // Set default character
                    CharacterConfig config = CharacterConfig.getInstance();
                    CharacterPack defaultCharPack = config.createCharacterPack(
                        "medieval_king",
                        gameState.botBaseX,
                        gameState.groundY,
                        true
                    );
                    gameState.setOpponentCharacterPack(defaultCharPack);
                    gameState.bot = defaultCharPack;
                }
                
                if (opponent.name != null && opponent == joinedPlayer) {
                    // Only show this for new opponents, not character updates
                    NotificationSystem.showSuccess("Opponent found: " + opponent.name);
                }
                
                // The server will automatically start countdown for full rooms
                // We just need to wait for COUNTDOWN_START message
            }
            System.out.println("=== END PLAYER_JOINED DEBUG ===");
        }
    }
    
    private void handleCountdownStart(NetworkMessage message) {
        matchState = MatchState.COUNTDOWN;
        currentCountdown = 10; // Updated to 10 seconds
        countdownStartTime = System.currentTimeMillis();
        NotificationSystem.showSuccess("Match starting! Get ready!");
        System.out.println("Countdown started - switching to COUNTDOWN state with 10 seconds");
    }
    
    private void handleCountdownUpdate(NetworkMessage message) {
        if (message.data instanceof Integer) {
            int countdown = (Integer) message.data;
            currentCountdown = countdown;
            if (countdown > 0) {
                NotificationSystem.showWarning("Starting in " + countdown + "...");
            }
        }
    }
    
    private void handleGameStart(NetworkMessage message) {
        matchState = MatchState.RACING;
        
        if (message.data instanceof String) {
            String firstWord = (String) message.data;
            
            System.out.println("=== GAME START SYNCHRONIZATION ===");
            System.out.println("Received first word from server: " + firstWord);
            
            // Switch to multiplayer mode and start the game like vs bot
            gameState.setMultiplayerMode(true);
            
            // FORCE set the word and reset all progress
            gameState.setCurrentWord(firstWord);
            gameState.playerIdx = 0;
            gameState.setOpponentIdx(0);
            
            // Set names properly
            if (localPlayer != null && localPlayer.name != null) {
                gameState.setPlayerName(localPlayer.name);
                System.out.println("Set local player name: " + localPlayer.name);
            }
            if (opponent != null && opponent.name != null) {
                gameState.setOpponentName(opponent.name);
                System.out.println("Set opponent name: " + opponent.name);
            }
            
            // Reset health to FULL for both players at game start
            gameState.playerHealth = GameConfig.MAX_HEALTH;
            gameState.botHealth = GameConfig.MAX_HEALTH;
            
            // Update local player objects
            if (localPlayer != null) {
                localPlayer.health = GameConfig.MAX_HEALTH;
                localPlayer.wordsCompleted = 0;
            }
            if (opponent != null) {
                opponent.health = GameConfig.MAX_HEALTH;
                opponent.wordsCompleted = 0;
            }
            
            // Force reset of all game state
            gameState.resetToReady();
            gameState.startGame();
            
            // Force complete typing progress reset
            gameState.resetTypingProgress();
            
            // Force UI update
            if (gamePanel != null) {
                gamePanel.repaint();
            }
            
            NotificationSystem.showSuccess("Fight! Type: " + firstWord);
            System.out.println("Game synchronized - Word: " + firstWord + ", Local: " + gameState.getPlayerName() + 
                             ", Opponent: " + gameState.getOpponentName());
            System.out.println("Health state - Local: " + gameState.playerHealth + ", Opponent: " + gameState.botHealth);
            System.out.println("=== END GAME START SYNCHRONIZATION ===");
        }
    }
    
    private void handlePlayerProgress(NetworkMessage message) {
        if (message.data instanceof Integer && !message.playerId.equals(localPlayer.id)) {
            int opponentProgress = (Integer) message.data;
            gameState.setOpponentIdx(opponentProgress);
        }
    }
    
    private void handlePlayerTyped(NetworkMessage message) {
        System.out.println("PLAYER_TYPED received from: " + message.playerId + " (local: " + localPlayer.id + ")");
        
        if (message.data instanceof String && !message.playerId.equals(localPlayer.id)) {
            String eventType = (String) message.data;
            if ("WORD_COMPLETE".equals(eventType)) {
                System.out.println("Opponent completed word - triggering attack animation");
                
                // Opponent completed a word - trigger bot attack animation
                gameState.startBotAttackSequence();
                
                // Force UI update to show animation
                if (gamePanel != null) {
                    gamePanel.repaint();
                }
                
                NotificationSystem.showWarning("Opponent attacked! You took damage!");
                
                // Health will be updated by ROOM_UPDATE message
            }
        } else if (message.data instanceof String && message.playerId.equals(localPlayer.id)) {
            // This is our own attack being echoed back - confirm attack
            String eventType = (String) message.data;
            if ("WORD_COMPLETE".equals(eventType)) {
                System.out.println("Own attack confirmed by server");
                
                // Don't update health here - will be handled by ROOM_UPDATE
                NotificationSystem.showSuccess("You attacked your opponent!");
            }
        }
    }
    
    private void handleGameStateUpdate(NetworkMessage message) {
        System.out.println("=== GAME_STATE_UPDATE RECEIVED ===");
        System.out.println("Data type: " + (message.data != null ? message.data.getClass().getSimpleName() : "null"));
        
        if (message.data instanceof String) {
            String newWord = (String) message.data;
            String oldWord = gameState.getCurrentWord();
            
            System.out.println("Word synchronization: " + oldWord + " -> " + newWord);
            
            // CRITICAL: Complete reset before setting new word
            System.out.println("Performing complete word transition reset...");
            
            // 1. Reset all typing progress FIRST
            gameState.playerIdx = 0;
            gameState.setOpponentIdx(0);
            
            // 2. Reset typing state completely
            gameState.resetTypingProgress();
            
            // 3. Clear any visual effects or animations
            // Temporarily clear current word to force UI refresh
            gameState.popUntil = 0; // Clear any pop animations
            
            // 4. NOW set the new word
            gameState.setCurrentWord(newWord);
            
            // 5. Force immediate UI update
            if (gamePanel != null) {
                gamePanel.requestFocusInWindow();
                gamePanel.repaint();
            }
            
            System.out.println("✓ Word synchronized successfully: " + newWord);
            System.out.println("    Local progress: " + gameState.playerIdx);
            System.out.println("    Opponent progress: " + gameState.getOpponentIdx());
            
            // Removed: NotificationSystem.showInfo("New word: " + newWord); - too cluttered
        } else if (message.data instanceof GameRoom) {
            // Legacy support for GameRoom updates
            GameRoom room = (GameRoom) message.data;
            System.out.println("Legacy GameRoom update - current word: " + room.currentWord);
            
            // Update word if it changed with full sync
            if (room.currentWord != null && !room.currentWord.equals(gameState.getCurrentWord())) {
                String oldWord = gameState.getCurrentWord();
                gameState.setCurrentWord(room.currentWord);
                gameState.playerIdx = 0;
                gameState.setOpponentIdx(0);
                gameState.resetTypingProgress();
                
                // Force UI update
                if (gamePanel != null) {
                    gamePanel.repaint();
                }
                
                // Removed: NotificationSystem.showInfo("New word: " + room.currentWord); - too cluttered
                System.out.println("Legacy word synchronized: " + oldWord + " -> " + room.currentWord);
            }
            
            // Update health from room state
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
            
            System.out.println("Legacy health sync - Local: " + gameState.playerHealth + 
                             ", Opponent: " + gameState.botHealth);
        } else {
            System.out.println("⚠ WARNING: Unknown GAME_STATE_UPDATE data type: " + 
                             (message.data != null ? message.data.getClass() : "null"));
        }
        System.out.println("=== END GAME_STATE_UPDATE ===");
    }
    
    private void handleRoomUpdate(NetworkMessage message) {
        if (message.data instanceof GameRoom) {
            GameRoom room = (GameRoom) message.data;
            
            System.out.println("=== ROOM_UPDATE RECEIVED ===");
            System.out.println("Players in room: " + room.players.size());
            
            // Print all server player states
            System.out.println("SERVER PLAYER STATES:");
            for (Player serverPlayer : room.players) {
                System.out.println("  " + serverPlayer.name + " (ID: " + serverPlayer.id + 
                                 ") Health: " + serverPlayer.health + " Words: " + serverPlayer.wordsCompleted +
                                 " Character: " + serverPlayer.selectedCharacterId);
            }
            
            // Print current local states BEFORE update
            System.out.println("LOCAL STATES BEFORE UPDATE:");
            System.out.println("  Local player health: " + (localPlayer != null ? localPlayer.health : "null") + 
                             " GameState.playerHealth: " + gameState.playerHealth);
            System.out.println("  Opponent health: " + (opponent != null ? opponent.health : "null") + 
                             " GameState.botHealth: " + gameState.botHealth);
            
            // Update player health and scores from server
            for (Player serverPlayer : room.players) {
                if (serverPlayer.id.equals(localPlayer.id)) {
                    // Update local player
                    int oldHealth = localPlayer.health;
                    int oldGameHealth = gameState.playerHealth;
                    int oldWords = localPlayer.wordsCompleted;
                    
                    localPlayer.health = serverPlayer.health;
                    localPlayer.wordsCompleted = serverPlayer.wordsCompleted;
                    gameState.playerHealth = serverPlayer.health;
                    
                    System.out.println("✓ LOCAL PLAYER UPDATE:");
                    System.out.println("    Health: " + oldHealth + " -> " + serverPlayer.health);
                    System.out.println("    Words: " + oldWords + " -> " + serverPlayer.wordsCompleted);
                    System.out.println("    GameState playerHealth: " + oldGameHealth + " -> " + serverPlayer.health);
                } else {
                    // Update opponent
                    if (opponent == null) {
                        System.out.println("⚠ Creating new opponent from server data");
                        opponent = new Player(serverPlayer.id, serverPlayer.name, serverPlayer.selectedCharacterId);
                        gameState.setOpponentName(opponent.name);
                        
                        // CRITICAL: Set opponent character based on their selection
                        if (opponent.selectedCharacterId != null) {
                            CharacterConfig config = CharacterConfig.getInstance();
                            CharacterPack opponentCharPack = config.createCharacterPack(
                                opponent.selectedCharacterId,
                                gameState.botBaseX,
                                gameState.groundY,
                                true  // facing left
                            );
                            gameState.setOpponentCharacterPack(opponentCharPack);
                            // Also update the bot field for rendering compatibility
                            gameState.bot = opponentCharPack;
                            System.out.println("✓ RoomUpdate: Set opponent character to " + opponent.selectedCharacterId);
                        } else {
                            // Fallback to default character
                            CharacterConfig config = CharacterConfig.getInstance();
                            CharacterPack defaultCharPack = config.createCharacterPack(
                                "medieval_king",
                                gameState.botBaseX,
                                gameState.groundY,
                                true
                            );
                            gameState.setOpponentCharacterPack(defaultCharPack);
                            gameState.bot = defaultCharPack;
                            System.out.println("⚠ RoomUpdate: Used default character for opponent (no selection)");
                        }
                    } else {
                        int oldHealth = opponent.health;
                        int oldWords = opponent.wordsCompleted;
                        
                        // COPY ALL DATA from server player to ensure sync
                        opponent.health = serverPlayer.health;
                        opponent.wordsCompleted = serverPlayer.wordsCompleted;
                        opponent.selectedCharacterId = serverPlayer.selectedCharacterId;
                        
                        System.out.println("✓ OPPONENT UPDATE:");
                        System.out.println("    Health: " + oldHealth + " -> " + serverPlayer.health);
                        System.out.println("    Words: " + oldWords + " -> " + serverPlayer.wordsCompleted);
                    }
                    
                    int oldBotHealth = gameState.botHealth;
                    gameState.botHealth = serverPlayer.health;
                    System.out.println("✓ BOT HEALTH UPDATE: " + oldBotHealth + " -> " + serverPlayer.health);
                }
            }
            
            // Force UI update to reflect health changes
            if (gamePanel != null) {
                gamePanel.repaint();
            }
            
            // Print final local states AFTER update
            System.out.println("LOCAL STATES AFTER UPDATE:");
            System.out.println("  Local player health: " + (localPlayer != null ? localPlayer.health : "null") + 
                             " GameState.playerHealth: " + gameState.playerHealth);
            System.out.println("  Opponent health: " + (opponent != null ? opponent.health : "null") + 
                             " GameState.botHealth: " + gameState.botHealth);
            
            // Check for game over conditions
            if (gameState.playerHealth <= 0) {
                NotificationSystem.showError("You lost the match!");
                matchState = MatchState.FINISHED;
                if (localPlayer != null) {
                    String characterUsed = getCurrentSelectedCharacter();
                    PlayerDatabase.saveOnlineMatchResult(localPlayer.name, false, characterUsed);
                    PlayerDatabase.savePlayerScore(localPlayer.name, localPlayer.wordsCompleted, 0);
                }
                scheduleReturnToMainMenu();
                
            } else if (gameState.botHealth <= 0) {
                NotificationSystem.showSuccess("You won the match!");
                matchState = MatchState.FINISHED;
                if (localPlayer != null) {
                    String characterUsed = getCurrentSelectedCharacter();
                    PlayerDatabase.saveOnlineMatchResult(localPlayer.name, true, characterUsed);
                    PlayerDatabase.savePlayerScore(localPlayer.name, localPlayer.wordsCompleted, 0);
                }
                scheduleReturnToMainMenu();
            }
            
            System.out.println("FINAL MATCH STATE: " + matchState);
            System.out.println("=== END ROOM_UPDATE ===");
        }
    }
    
    private void handleGameOver(NetworkMessage message) {
        matchState = MatchState.FINISHED;
        
        if (message.data instanceof String) {
            String result = (String) message.data;
            boolean won = result.equals("WIN");
            if (won) {
                NotificationSystem.showSuccess("You won the match!");
            } else {
                NotificationSystem.showWarning("You lost the match!");
            }
            
            if (localPlayer != null) {
                String characterUsed = getCurrentSelectedCharacter();
                PlayerDatabase.saveOnlineMatchResult(localPlayer.name, won, characterUsed);
            }
        }
        
        if (localPlayer != null) {
            PlayerDatabase.savePlayerScore(localPlayer.name, localPlayer.wordsCompleted, 0);
        }
        
        scheduleReturnToMainMenu();
    }
    
    private void handlePlayerDisconnected(NetworkMessage message) {
        if (matchState == MatchState.RACING) {
            NotificationSystem.showSuccess("Opponent disconnected - You win!");
            matchState = MatchState.FINISHED;
            
            if (localPlayer != null) {
                String characterUsed = getCurrentSelectedCharacter();
                PlayerDatabase.saveOnlineMatchResult(localPlayer.name, true, characterUsed);
                PlayerDatabase.savePlayerScore(localPlayer.name, localPlayer.wordsCompleted, 0);
            }
            
            scheduleReturnToMainMenu();
        } else if (matchState == MatchState.WAITING_FOR_OPPONENT || matchState == MatchState.COUNTDOWN) {
            NotificationSystem.showWarning("Opponent disconnected - returning to lobby");
            matchState = MatchState.CONNECTED;
            opponent = null;
            currentRoomId = null;
            
            if (gameState != null) {
                gameState.resetToReady();
            }
        }
    }
    
    /**
     * Schedules automatic return to main menu after game ends
     */
    private void scheduleReturnToMainMenu() {
        System.out.println("=== SCHEDULING RETURN TO MAIN MENU ===");
        
        new Thread(() -> {
            try {
                Thread.sleep(3000); // Wait 3 seconds
                
                System.out.println("=== RETURNING TO MAIN MENU ===");
                
                // Disconnect from server and clean up
                if (networkClient != null) {
                    networkClient.disconnect();
                }
                
                // Reset all states to main menu
                resetToMainMenu();
                
                // Force UI update
                if (gamePanel != null) {
                    SwingUtilities.invokeLater(() -> {
                        gamePanel.repaint();
                    });
                }
                
                NotificationSystem.showInfo("Returned to main menu");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Resets the game state but keeps online mode active for new matches
     */
    public void resetToMainMenu() {
        System.out.println("=== RESETTING FOR NEW ONLINE MATCH ===");
        
        // Reset match manager state but KEEP online connection
        matchState = MatchState.CONNECTED; // Stay connected instead of OFFLINE
        isHost = false;
        currentRoomId = null;
        opponent = null;
        localPlayer = null; // Will be recreated on next match
        currentCountdown = 0;
        countdownStartTime = 0;
        
        // Reset game state but KEEP multiplayer mode
        if (gameState != null) {
            // DON'T call setMultiplayerMode(false) - stay in online mode
            gameState.resetToReady();
            gameState.setOpponentName("");
            gameState.setPlayerName("");
            gameState.setStatusMessage("Ready for online match");
            
            // Reset character animations properly
            resetCharacterAnimations();
        }
        
        System.out.println("=== READY FOR NEW ONLINE MATCH ===");
    }
    
    /**
     * Resets character animations to idle state
     */
    private void resetCharacterAnimations() {
        if (gameState != null) {
            // Reset player character to idle
            if (gameState.player != null) {
                try {
                    gameState.player.setAnim(CharacterPack.Anim.IDLE);
                    gameState.player.x = gameState.playerBaseX;
                    gameState.player.y = gameState.groundY;
                } catch (Exception e) {
                    System.out.println("Warning: Could not reset player animation: " + e.getMessage());
                }
            }
            
            // Reset bot/opponent character to idle
            if (gameState.bot != null) {
                try {
                    gameState.bot.setAnim(CharacterPack.Anim.IDLE);
                    gameState.bot.x = gameState.botBaseX;
                    gameState.bot.y = gameState.groundY;
                } catch (Exception e) {
                    System.out.println("Warning: Could not reset bot animation: " + e.getMessage());
                }
            }
            
            // Reset all animation states
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
        System.out.println("=== DISCONNECTION EVENT ===");
        
        // Complete state reset
        isConnected = false;
        matchState = MatchState.OFFLINE;
        currentRoomId = null;
        opponent = null;
        localPlayer = null;
        isHost = false;
        currentCountdown = 0;
        countdownStartTime = 0;
        
        // Reset game state to offline mode
        if (gameState != null) {
            gameState.setMultiplayerMode(false);
            gameState.resetToReady();
            gameState.setOpponentName("");
            gameState.setPlayerName("");
            gameState.setStatusMessage("");
            
            // Reset health to default
            gameState.playerHealth = GameConfig.MAX_HEALTH;
            gameState.botHealth = GameConfig.MAX_HEALTH;
        }
        
        // Force UI update
        if (gamePanel != null) {
            SwingUtilities.invokeLater(() -> {
                gamePanel.repaint();
            });
        }
        
        NotificationSystem.showWarning("Disconnected from server");
        System.out.println("=== DISCONNECTION CLEANUP COMPLETE ===");
    }
    
    public void disconnect() {
        if (networkClient != null) {
            networkClient.disconnect();
        }
        onDisconnected();
    }
    
    // Getters
    public boolean isOnline() { return matchState != MatchState.OFFLINE; }
    public boolean isRacing() { return matchState == MatchState.RACING; }
    public MatchState getMatchState() { return matchState; }
    public Player getLocalPlayer() { return localPlayer; }
    public Player getOpponent() { return opponent; }
    public String getCurrentRoomId() { return currentRoomId; }
    public int getCurrentCountdown() { return currentCountdown; }
    public long getCountdownStartTime() { return countdownStartTime; }
}
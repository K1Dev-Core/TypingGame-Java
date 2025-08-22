public interface StateManager {
    // State transition methods
    void startGame();
    void pauseGame();
    void resumeGame();
    void endGame();
    void resetToReady();
    
    // State query methods
    boolean isPlaying();
    boolean isReady();
    boolean isGameOver();
    GameConfig.State getCurrentState();
    
    // Multiplayer state methods
    void setMultiplayerMode(boolean multiplayer);
    boolean isMultiplayerMode();
    
    // Game progress methods
    void completeWord();
    void takeDamage(boolean isPlayer);
    void updateScore(int score);
    
    // Time management
    long getElapsedTime();
    void updateGameTime();
}
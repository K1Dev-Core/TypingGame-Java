public interface StateManager {
    void startGame();
    void pauseGame();
    void resumeGame();
    void endGame();
    void resetToReady();
    
    boolean isPlaying();
    boolean isReady();
    boolean isGameOver();
    GameConfig.State getCurrentState();
    
    void setMultiplayerMode(boolean multiplayer);
    boolean isMultiplayerMode();
    
    void completeWord();
    void takeDamage(boolean isPlayer);
    void updateScore(int score);
    
    long getElapsedTime();
    void updateGameTime();
}
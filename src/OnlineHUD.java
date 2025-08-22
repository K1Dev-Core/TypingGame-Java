import java.awt.*;
import shared.Player;

public class OnlineHUD {
    
    public static void renderOnlineHUD(Graphics2D g2, GameState gameState, UISettings uiSettings, 
                                      int screenWidth, int screenHeight) {
        OnlineMatchManager manager = OnlineMatchManager.getInstance();
        
        if (!manager.isRacing()) return;
        
        Player localPlayer = manager.getLocalPlayer();
        Player opponent = manager.getOpponent();
        
        if (localPlayer == null || opponent == null) return;
        

        renderOnlineHealthBars(g2, gameState, localPlayer, opponent, screenWidth);
        

        renderMatchScores(g2, localPlayer, opponent, screenWidth);
        

        renderConnectionStatus(g2, screenWidth, screenHeight);
    }
    
    private static void renderOnlineHealthBars(Graphics2D g2, GameState gameState, 
                                             Player localPlayer, Player opponent, int screenWidth) {
        int barW = UIConfig.HEALTH_BAR_WIDTH;
        int barH = UIConfig.HEALTH_BAR_HEIGHT;
        

        String localPlayerName = null;
        String opponentName = null;
        

        if (localPlayer != null && localPlayer.name != null && !localPlayer.name.trim().isEmpty()) {
            localPlayerName = localPlayer.name;
        }
        if (opponent != null && opponent.name != null && !opponent.name.trim().isEmpty()) {
            opponentName = opponent.name;
        }
        

        if (localPlayerName == null || localPlayerName.trim().isEmpty()) {
            localPlayerName = gameState.getPlayerName();
        }
        if (opponentName == null || opponentName.trim().isEmpty()) {
            opponentName = gameState.getOpponentName();
        }
        

        if (localPlayerName == null || localPlayerName.trim().isEmpty()) {
            localPlayerName = "You";
        }
        if (opponentName == null || opponentName.trim().isEmpty()) {
            opponentName = "Opponent";
        }
        
        

        int playerX = 50;
        int playerY = 160;
        drawOnlineHealthBar(g2, playerX, playerY, barW, barH,
                gameState.playerHealth / (double) GameConfig.MAX_HEALTH,
                localPlayerName + " (You)", Color.GREEN);
        

        int opponentX = screenWidth - barW - 50;
        int opponentY = 160;
        drawOnlineHealthBar(g2, opponentX, opponentY, barW, barH,
                gameState.botHealth / (double) GameConfig.MAX_HEALTH,
                opponentName, Color.RED);
    }
    
    private static void drawOnlineHealthBar(Graphics2D g2, int barX, int barY, int barW, int barH,
                                          double healthPercent, String playerName, Color nameColor) {

        g2.setColor(UIConfig.HUD_PANEL);
        g2.fillRoundRect(barX - 6, barY - 6, barW + 12, barH + 25, 12, 12);
        

        g2.setColor(new Color(40, 40, 40));
        g2.fillRoundRect(barX, barY, barW, barH, 8, 8);
        

        int healthW = (int) (barW * healthPercent);
        if (healthW > 0) {
            Color healthColor1 = healthPercent > 0.6 ? UIConfig.HEALTH_HIGH_1 :
                                healthPercent > 0.3 ? UIConfig.HEALTH_MED_1 : UIConfig.HEALTH_LOW_1;
            Color healthColor2 = healthPercent > 0.6 ? UIConfig.HEALTH_HIGH_2 :
                                healthPercent > 0.3 ? UIConfig.HEALTH_MED_2 : UIConfig.HEALTH_LOW_2;
            
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
        

        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(nameColor);
        FontMetrics fm = g2.getFontMetrics();
        int textX = barX + (barW - fm.stringWidth(playerName)) / 2;
        g2.drawString(playerName, textX, barY - 8);
        

        String healthValue = String.format("%d/%d", (int) (healthPercent * GameConfig.MAX_HEALTH),
                GameConfig.MAX_HEALTH);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        FontMetrics healthFm = g2.getFontMetrics();
        int valueX = barX + (barW - healthFm.stringWidth(healthValue)) / 2;
        g2.setColor(Color.WHITE);
        g2.drawString(healthValue, valueX, barY + barH / 2 + 3);
    }
    
    private static void renderMatchScores(Graphics2D g2, Player localPlayer, Player opponent, int screenWidth) {

        String localPlayerName = (localPlayer != null && localPlayer.name != null && !localPlayer.name.trim().isEmpty()) 
                                 ? localPlayer.name : "You";
        String opponentName = (opponent != null && opponent.name != null && !opponent.name.trim().isEmpty()) 
                             ? opponent.name : "Opponent";
        

        Font scoreFont = new Font("Arial", Font.BOLD, 18);
        g2.setFont(scoreFont);
        
        int localScore = (localPlayer != null) ? localPlayer.wordsCompleted : 0;
        int opponentScore = (opponent != null) ? opponent.wordsCompleted : 0;
        
        String scoreText = localScore + " - " + opponentScore;
        FontMetrics fm = g2.getFontMetrics();
        int scoreX = (screenWidth - fm.stringWidth(scoreText)) / 2;
        

        int panelWidth = fm.stringWidth(scoreText) + 20;
        int panelHeight = 30;
        int panelX = scoreX - 10;
        int panelY = 10;
        
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);
        
        g2.setColor(new Color(255, 215, 0));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);
        

        g2.setColor(Color.WHITE);
        g2.drawString(scoreText, scoreX, panelY + 20);
        

    }
    
    private static void renderConnectionStatus(Graphics2D g2, int screenWidth, int screenHeight) {

        Font font = new Font("Arial", Font.PLAIN, 11);
        g2.setFont(font);
        g2.setColor(new Color(0, 255, 0));
        
        String status = "● ONLINE";
        FontMetrics fm = g2.getFontMetrics();
        int x = screenWidth - fm.stringWidth(status) - 10;
        int y = screenHeight - 30;
        
        g2.drawString(status, x, y);
    }
}
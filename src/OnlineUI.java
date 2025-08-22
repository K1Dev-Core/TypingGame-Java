import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class OnlineUI {
    private static String playerName = null;
    private static boolean showNameInput = false;
    private static boolean showMatchmakingButton = false;
    private static boolean showExitConfirmation = false;
    private static StringBuilder nameBuffer = new StringBuilder();
    
    public static void setPlayerName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            playerName = name.trim();
        }
    }
    
    public static String getPlayerName() {
        return playerName;
    }
    
    public static void enterOnlineMode() {
        if (playerName == null) {
            showNameInput = true;
            NotificationSystem.showInfo("Enter your name and press Enter");
        } else {
            showMatchmakingButton = true;
            NotificationSystem.showInfo("Press + button to find a match!");
        }
    }
    
    public static void startOnlineMode() {
        if (playerName == null) {
            showNameInput = true;
            NotificationSystem.showInfo("Enter your name and press Enter");
            return;
        }
        
        OnlineMatchManager.getInstance().startMatchmaking(playerName);
    }
    
    public static void handleNameInput(char c) {
        if (!showNameInput) return;
        
        if (c == '\n' || c == '\r') {
            if (nameBuffer.length() > 0) {
                String name = nameBuffer.toString().trim();
                if (name.length() > 0) {
                    setPlayerName(name);
                    nameBuffer.setLength(0);
                    showNameInput = false;
                    showMatchmakingButton = true;
                    
                    PlayerDatabase.recordOnlineLogin(name);
                    
                    PlayerDatabase.PlayerRecord record = PlayerDatabase.getPlayerRecord(name);
                    if (record != null) {
                        double winRate = record.getWinRate() * 100;
                        int totalMatches = record.onlineWins + record.onlineLosses;
                        String statsMessage = String.format(
                            "Welcome %s! Stats: Logins: %d | Score: %d | WPM: %d | Matches: %d | Win Rate: %.1f%% | Character: %s",
                            name, record.onlineLogins, record.bestScore, record.bestWPM, 
                            totalMatches, winRate, record.favoriteCharacter.replace("_", " ")
                        );
                        NotificationSystem.showSuccess(statsMessage);
                    } else {
                        NotificationSystem.showSuccess("Welcome new player " + name + "! Click + to find your first match!");
                    }
                } else {
                    NotificationSystem.showWarning("Please enter a valid name");
                }
            } else {
                NotificationSystem.showWarning("Please enter your name first");
            }
        } else if (c == '\b') {
            if (nameBuffer.length() > 0) {
                nameBuffer.deleteCharAt(nameBuffer.length() - 1);
            }
        } else if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-') {
            if (nameBuffer.length() < 20) {
                if (c == ' ' && nameBuffer.length() == 0) {
                    return;
                }
                nameBuffer.append(c);
            } else {
                NotificationSystem.showWarning("Name too long (max 20 characters)");
            }
        }
    }
    
    public static void renderOnlineUI(Graphics2D g2, int screenWidth, int screenHeight) {
        OnlineMatchManager manager = OnlineMatchManager.getInstance();
        OnlineMatchManager.MatchState state = manager.getMatchState();
        
        switch (state) {
            case OFFLINE:
                if (showMatchmakingButton) {
                    renderMatchmakingButton(g2, screenWidth, screenHeight);
                }
                break;
            case CONNECTING:
                renderStatus(g2, screenWidth, screenHeight, "Connecting...", Color.YELLOW);
                break;
            case CONNECTED:
                if (showMatchmakingButton) {
                    renderMatchmakingButton(g2, screenWidth, screenHeight);
                }
                break;
            case WAITING_FOR_OPPONENT:
                renderWaitingForOpponent(g2, screenWidth, screenHeight);
                if (showMatchmakingButton) {
                    renderMatchmakingButton(g2, screenWidth, screenHeight);
                }
                break;
            case COUNTDOWN:
                renderCountdown(g2, screenWidth, screenHeight);
                break;
            case RACING:
                renderRaceInfo(g2, screenWidth, screenHeight);
                break;
            case FINISHED:
                renderMatchFinished(g2, screenWidth, screenHeight);
                break;
        }
        
        if (showNameInput) {
            renderNameInput(g2, screenWidth, screenHeight);
        }
        
        if (showExitConfirmation) {
            renderExitConfirmation(g2, screenWidth, screenHeight);
        }
        
        if (manager.isGameOverOverlayShowing()) {
            renderGameOverOverlay(g2, screenWidth, screenHeight);
        }
    }
    
    public static void showExitConfirmation() {
        showExitConfirmation = true;
    }
    
    public static boolean isShowingExitConfirmation() {
        return showExitConfirmation;
    }
    
    private static void renderNameInput(Graphics2D g2, int screenWidth, int screenHeight) {
        int panelWidth = 350;
        int panelHeight = 150;
        int x = (screenWidth - panelWidth) / 2;
        int y = (screenHeight - panelHeight) / 2;
        
        RoundRectangle2D panel = new RoundRectangle2D.Float(x, y, panelWidth, panelHeight, 15, 15);
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fill(panel);
        
        g2.setColor(new Color(25, 118, 210, 150));
        g2.setStroke(new BasicStroke(3));
        g2.draw(panel);
        
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1));
        g2.draw(panel);
        
        Font titleFont = new Font("Arial", Font.BOLD, 18);
        g2.setFont(titleFont);
        g2.setColor(new Color(255, 255, 100));
        String title = "Enter Your Player Name";
        FontMetrics titleFm = g2.getFontMetrics();
        int titleX = x + (panelWidth - titleFm.stringWidth(title)) / 2;
        g2.drawString(title, titleX, y + 35);
        
        int fieldWidth = 250;
        int fieldHeight = 30;
        int fieldX = x + (panelWidth - fieldWidth) / 2;
        int fieldY = y + 55;
        
        g2.setColor(new Color(30, 30, 30));
        g2.fillRoundRect(fieldX, fieldY, fieldWidth, fieldHeight, 5, 5);
        
        g2.setColor(new Color(25, 118, 210));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(fieldX, fieldY, fieldWidth, fieldHeight, 5, 5);
        
        Font inputFont = new Font("Arial", Font.BOLD, 16);
        g2.setFont(inputFont);
        g2.setColor(Color.WHITE);
        
        String displayText = nameBuffer.toString();
        if (displayText.isEmpty()) {
            g2.setColor(new Color(150, 150, 150));
            g2.drawString("Type your name here...", fieldX + 10, fieldY + 20);
        } else {
            g2.drawString(displayText, fieldX + 10, fieldY + 20);
        }
        
        if (System.currentTimeMillis() % 1000 < 500) {
            int cursorX = fieldX + 10 + g2.getFontMetrics().stringWidth(displayText);
            g2.setColor(Color.WHITE);
            g2.drawLine(cursorX, fieldY + 8, cursorX, fieldY + 22);
        }
        
        Font instrFont = new Font("Arial", Font.PLAIN, 13);
        g2.setFont(instrFont);
        g2.setColor(new Color(200, 200, 200));
        
        String[] instructions = {
            "Press ENTER to confirm",
            "Press ESC to cancel"
        };
        
        for (int i = 0; i < instructions.length; i++) {
            FontMetrics instrFm = g2.getFontMetrics();
            int instrX = x + (panelWidth - instrFm.stringWidth(instructions[i])) / 2;
            g2.drawString(instructions[i], instrX, y + panelHeight - 30 + (i * 15));
        }
        
        g2.setColor(new Color(150, 150, 150));
        String countText = nameBuffer.length() + "/20";
        FontMetrics countFm = g2.getFontMetrics();
        g2.drawString(countText, fieldX + fieldWidth - countFm.stringWidth(countText) - 5, fieldY - 5);
    }
    
    private static void renderExitConfirmation(Graphics2D g2, int screenWidth, int screenHeight) {
        int panelWidth = 400;
        int panelHeight = 180;
        int x = (screenWidth - panelWidth) / 2;
        int y = (screenHeight - panelHeight) / 2;
        
        RoundRectangle2D panel = new RoundRectangle2D.Float(x, y, panelWidth, panelHeight, 15, 15);
        g2.setColor(new Color(0, 0, 0, 240));
        g2.fill(panel);
        
        g2.setColor(new Color(220, 50, 50, 150));
        g2.setStroke(new BasicStroke(3));
        g2.draw(panel);
        
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1));
        g2.draw(panel);
        
        Font titleFont = new Font("Arial", Font.BOLD, 20);
        g2.setFont(titleFont);
        g2.setColor(new Color(255, 200, 200));
        String title = "Exit Online Mode?";
        FontMetrics titleFm = g2.getFontMetrics();
        int titleX = x + (panelWidth - titleFm.stringWidth(title)) / 2;
        g2.drawString(title, titleX, y + 40);
        
        Font msgFont = new Font("Arial", Font.PLAIN, 14);
        g2.setFont(msgFont);
        g2.setColor(new Color(200, 200, 200));
        String message = "Are you sure you want to exit online mode?";
        FontMetrics msgFm = g2.getFontMetrics();
        int msgX = x + (panelWidth - msgFm.stringWidth(message)) / 2;
        g2.drawString(message, msgX, y + 75);
        
        int buttonWidth = 100;
        int buttonHeight = 35;
        int buttonY = y + 105;
        int yesX = x + (panelWidth / 2) - buttonWidth - 10;
        int noX = x + (panelWidth / 2) + 10;
        
        RoundRectangle2D yesButton = new RoundRectangle2D.Float(yesX, buttonY, buttonWidth, buttonHeight, 8, 8);
        g2.setColor(new Color(220, 50, 50, 180));
        g2.fill(yesButton);
        g2.setColor(new Color(255, 100, 100));
        g2.setStroke(new BasicStroke(2));
        g2.draw(yesButton);
        
        RoundRectangle2D noButton = new RoundRectangle2D.Float(noX, buttonY, buttonWidth, buttonHeight, 8, 8);
        g2.setColor(new Color(50, 150, 50, 180));
        g2.fill(noButton);
        g2.setColor(new Color(100, 200, 100));
        g2.draw(noButton);
        
        Font buttonFont = new Font("Arial", Font.BOLD, 14);
        g2.setFont(buttonFont);
        g2.setColor(Color.WHITE);
        
        String yesText = "Yes";
        FontMetrics fm = g2.getFontMetrics();
        int yesTextX = yesX + (buttonWidth - fm.stringWidth(yesText)) / 2;
        int yesTextY = buttonY + (buttonHeight + fm.getAscent()) / 2 - 2;
        g2.drawString(yesText, yesTextX, yesTextY);
        
        String noText = "No";
        int noTextX = noX + (buttonWidth - fm.stringWidth(noText)) / 2;
        int noTextY = buttonY + (buttonHeight + fm.getAscent()) / 2 - 2;
        g2.drawString(noText, noTextX, noTextY);
        
        Font instrFont = new Font("Arial", Font.PLAIN, 12);
        g2.setFont(instrFont);
        g2.setColor(new Color(180, 180, 180));
        String instr = "Press Y for Yes, N for No, or ESC to cancel";
        FontMetrics instrFm = g2.getFontMetrics();
        int instrX = x + (panelWidth - instrFm.stringWidth(instr)) / 2;
        g2.drawString(instr, instrX, y + panelHeight - 20);
    }
    
    private static void renderStatus(Graphics2D g2, int screenWidth, int screenHeight, String message, Color color) {
        Font font = new Font("Arial", Font.BOLD, 16);
        g2.setFont(font);
        g2.setColor(color);
        
        FontMetrics fm = g2.getFontMetrics();
        int x = (screenWidth - fm.stringWidth(message)) / 2;
        int y = 50;
        g2.drawString(message, x, y);
    }
    
    private static void renderWaitingForOpponent(Graphics2D g2, int screenWidth, int screenHeight) {
        renderStatus(g2, screenWidth, screenHeight, "Searching for opponent...", Color.YELLOW);
        
        long time = System.currentTimeMillis();
        int dots = (int) ((time / 500) % 4);
        String dotString = "";
        for (int i = 0; i < dots; i++) {
            dotString += ".";
        }
        
        Font font = new Font("Arial", Font.BOLD, 16);
        g2.setFont(font);
        g2.setColor(Color.YELLOW);
        FontMetrics fm = g2.getFontMetrics();
        int x = screenWidth / 2 + fm.stringWidth("Searching for opponent") / 2;
        g2.drawString(dotString, x, 50);
    }
    

    private static void renderCountdown(Graphics2D g2, int screenWidth, int screenHeight) {
        renderImprovedCountdown(g2, screenWidth, screenHeight);
    }
    
    private static void renderRaceInfo(Graphics2D g2, int screenWidth, int screenHeight) {
        g2.setColor(Color.YELLOW);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.drawString("Online Match", screenWidth / 2 - 40, screenHeight - 40);
    }
    
    private static void renderMatchFinished(Graphics2D g2, int screenWidth, int screenHeight) {
        renderStatus(g2, screenWidth, screenHeight, "Match finished! Returning to menu...", Color.CYAN);
    }
    
    public static boolean handleClick(int x, int y, int screenWidth, int screenHeight) {
        if (showExitConfirmation) {
            return handleExitConfirmationClick(x, y, screenWidth, screenHeight);
        }
        
        OnlineMatchManager manager = OnlineMatchManager.getInstance();
        
        if ((manager.getMatchState() == OnlineMatchManager.MatchState.OFFLINE ||
             manager.getMatchState() == OnlineMatchManager.MatchState.CONNECTED ||
             manager.getMatchState() == OnlineMatchManager.MatchState.WAITING_FOR_OPPONENT) &&
            showMatchmakingButton && handleMatchmakingButtonClick(x, y, screenWidth, screenHeight)) {
            return true;
        }
        
        return false;
    }
    
    private static boolean handleExitConfirmationClick(int x, int y, int screenWidth, int screenHeight) {
        int panelWidth = 400;
        int panelHeight = 180;
        int panelX = (screenWidth - panelWidth) / 2;
        int panelY = (screenHeight - panelHeight) / 2;
        
        int buttonWidth = 100;
        int buttonHeight = 35;
        int buttonY = panelY + 105;
        int yesX = panelX + (panelWidth / 2) - buttonWidth - 10;
        int noX = panelX + (panelWidth / 2) + 10;
        
        if (x >= yesX && x <= yesX + buttonWidth && y >= buttonY && y <= buttonY + buttonHeight) {
            confirmExitOnlineMode();
            return true;
        }
        
        if (x >= noX && x <= noX + buttonWidth && y >= buttonY && y <= buttonY + buttonHeight) {
            showExitConfirmation = false;
            return true;
        }
        
        return false;
    }
    
    public static void handleExitConfirmationKey(char key) {
        if (key == 'y' || key == 'Y') {
            confirmExitOnlineMode();
        } else if (key == 'n' || key == 'N' || key == 27) {
            showExitConfirmation = false;
        }
    }
    
    private static void confirmExitOnlineMode() {
        showExitConfirmation = false;
        exitOnlineMode();
        playerName = null;
        showMatchmakingButton = false;
    }
    
    public static void exitOnlineMode() {
        OnlineMatchManager.getInstance().disconnect();
        showNameInput = false;
        showMatchmakingButton = false;
        nameBuffer.setLength(0);
        NotificationSystem.showInfo("Returned to offline mode");
    }
    
    public static boolean isShowingNameInput() {
        return showNameInput;
    }
    
    public static void resetToMatchReady() {
        if (playerName != null) {
            showMatchmakingButton = true;
            showNameInput = false;
            NotificationSystem.showInfo("Ready for next match! Press + to find opponent");
        }
    }
    
    private static void renderMatchmakingButton(Graphics2D g2, int screenWidth, int screenHeight) {
        int leftMargin = 20;
        int rightMargin = 40;
        int gapFromWord = 48;
        int cx = screenWidth / 2;
        
        int estimatedBotX = cx + 240;
        int estimatedBotY = screenHeight - 60 - 120;
        
        OnlineMatchManager manager = OnlineMatchManager.getInstance();
        boolean isWaiting = manager.getMatchState() == OnlineMatchManager.MatchState.WAITING_FOR_OPPONENT;
        
        int buttonSize = 70;
        int buttonX = estimatedBotX - buttonSize / 2;
        int buttonY = estimatedBotY - buttonSize / 2;
        
        Color primaryColor = isWaiting ? new Color(220, 50, 50) : new Color(50, 205, 50);
        Color glowColor = isWaiting ? new Color(255, 100, 100) : new Color(100, 255, 100);
        
        for (int i = 20; i >= 0; i -= 3) {
            int alpha = 30 - i;
            g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), alpha));
            g2.fillOval(buttonX - i, buttonY - i, buttonSize + (i * 2), buttonSize + (i * 2));
        }
        
        g2.setColor(primaryColor);
        g2.fillOval(buttonX, buttonY, buttonSize, buttonSize);
        
        long time = System.currentTimeMillis();
        float borderAlpha = 0.7f + 0.3f * (float)Math.sin(time * 0.008);
        g2.setColor(new Color(255, 255, 255, (int)(255 * borderAlpha)));
        g2.setStroke(new BasicStroke(3));
        g2.drawOval(buttonX, buttonY, buttonSize, buttonSize);
        
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(4));
        int centerX = buttonX + buttonSize / 2;
        int centerY = buttonY + buttonSize / 2;
        
        if (isWaiting) {
            int crossSize = 18;
            g2.drawLine(centerX - crossSize, centerY - crossSize, centerX + crossSize, centerY + crossSize);
            g2.drawLine(centerX - crossSize, centerY + crossSize, centerX + crossSize, centerY - crossSize);
        } else {
            int crossSize = 18;
            g2.drawLine(centerX - crossSize, centerY, centerX + crossSize, centerY);
            g2.drawLine(centerX, centerY - crossSize, centerX, centerY + crossSize);
        }
        
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        String text = isWaiting ? "Click to cancel" : "Click to find match!";
        FontMetrics fm = g2.getFontMetrics();
        int textX = centerX - fm.stringWidth(text) / 2;
        int textY = buttonY + buttonSize + 30;
        
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(textX - 12, textY - 16, fm.stringWidth(text) + 24, 22, 8, 8);
        
        g2.setColor(isWaiting ? new Color(255, 150, 150) : new Color(150, 255, 150));
        g2.drawString(text, textX, textY);
        
        if (!isWaiting) {
            double pulseScale = 1.0 + 0.05 * Math.sin(time * 0.01);
        }
        
        matchmakingButtonBounds = new java.awt.Rectangle(
            buttonX - 15, 
            buttonY - 15, 
            buttonSize + 30, 
            buttonSize + 30
        );
    }
    
    private static java.awt.Rectangle matchmakingButtonBounds = null;
    
    private static boolean handleMatchmakingButtonClick(int x, int y, int screenWidth, int screenHeight) {
        if (matchmakingButtonBounds != null && matchmakingButtonBounds.contains(x, y)) {
            OnlineMatchManager manager = OnlineMatchManager.getInstance();
            
            if (manager.getMatchState() == OnlineMatchManager.MatchState.WAITING_FOR_OPPONENT) {
                manager.cancelMatchmaking();
                showMatchmakingButton = true;
                NotificationSystem.showWarning("Matchmaking canceled");
            } else {
                showMatchmakingButton = true;
                manager.startMatchmaking(playerName);
            }
            return true;
        }
        return false;
    }
    
    private static void renderImprovedCountdown(Graphics2D g2, int screenWidth, int screenHeight) {
        OnlineMatchManager manager = OnlineMatchManager.getInstance();
        int countdown = manager.getCurrentCountdown();
        long countdownStartTime = manager.getCountdownStartTime();
        
        long elapsed = System.currentTimeMillis() - countdownStartTime;
        double exactCountdown = Math.max(0, 10.0 - (elapsed / 1000.0));
        
        long time = System.currentTimeMillis();
        
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        if (exactCountdown > 0) {
            int currentNum = (int)Math.ceil(exactCountdown);
            
            Color numberColor;
            Color glowColor;
            if (currentNum >= 8) {
                numberColor = new Color(100, 255, 100);
                glowColor = new Color(50, 200, 50);
            } else if (currentNum >= 5) {
                numberColor = new Color(255, 255, 100);
                glowColor = new Color(200, 200, 50);
            } else if (currentNum >= 3) {
                numberColor = new Color(255, 150, 50);
                glowColor = new Color(200, 100, 25);
            } else {
                numberColor = new Color(255, 50, 50);
                glowColor = new Color(200, 25, 25);
            }
            
            double timeInSecond = exactCountdown - Math.floor(exactCountdown);
            double entranceScale = 1.0;
            if (timeInSecond > 0.85) {
                entranceScale = (1.0 - timeInSecond) * 6.67;
                entranceScale = Math.min(1.0, entranceScale);
            }
            
            double pulseScale = 1.0 + 0.15 * Math.sin(time * 0.015);
            int fontSize = (int)(300 * entranceScale * pulseScale);
            Font countdownFont = new Font("Arial", Font.BOLD, fontSize);
            g2.setFont(countdownFont);
            
            String countText = String.valueOf(currentNum);
            FontMetrics fm = g2.getFontMetrics();
            int x = (screenWidth - fm.stringWidth(countText)) / 2;
            int y = screenHeight / 2 + fm.getAscent() / 2;
            
            int circleSize = (int)(500 * entranceScale * pulseScale);
            int circleX = screenWidth / 2 - circleSize / 2;
            int circleY = screenHeight / 2 - circleSize / 2;
            
            for (int i = 0; i < 8; i++) {
                int glowSize = circleSize + (i * 60);
                int glowX = screenWidth / 2 - glowSize / 2;
                int glowY = screenHeight / 2 - glowSize / 2;
                int alpha = Math.max(5, 50 - i * 6);
                g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), alpha));
                g2.fillOval(glowX, glowY, glowSize, glowSize);
            }
            
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillOval(circleX, circleY, circleSize, circleSize);
            
            double borderPulse = 1.0 + 0.4 * Math.sin(time * 0.025);
            g2.setColor(numberColor);
            g2.setStroke(new BasicStroke((int)(12 * borderPulse)));
            g2.drawOval(circleX, circleY, circleSize, circleSize);
            
            for (int i = 20; i >= 0; i -= 3) {
                int shadowAlpha = Math.max(10, 100 - i * 4);
                g2.setColor(new Color(0, 0, 0, shadowAlpha));
                g2.drawString(countText, x + i, y + i);
            }
            
            g2.setColor(numberColor);
            g2.drawString(countText, x, y);
            
            for (int i = 0; i < 12; i++) {
                double angle = (time * 0.008 + i * Math.PI / 6) % (2 * Math.PI);
                int sparkleX = (int)(screenWidth / 2 + Math.cos(angle) * (circleSize * 0.7));
                int sparkleY = (int)(screenHeight / 2 + Math.sin(angle) * (circleSize * 0.7));
                g2.setColor(new Color(255, 255, 255, 200));
                g2.fillOval(sparkleX - 6, sparkleY - 6, 12, 12);
            }
            
            String titleText = "MATCH STARTING";
            Font titleFont = new Font("Arial", Font.BOLD, 48);
            g2.setFont(titleFont);
            FontMetrics titleFm = g2.getFontMetrics();
            int titleX = (screenWidth - titleFm.stringWidth(titleText)) / 2;
            int titleY = screenHeight / 2 - circleSize / 2 - 60;
            
            g2.setColor(new Color(0, 0, 0, 150));
            g2.drawString(titleText, titleX + 3, titleY + 3);
            g2.setColor(new Color(255, 255, 255, 220));
            g2.drawString(titleText, titleX, titleY);
            
        } else {
            Font fightFont = new Font("Arial", Font.BOLD, 150);
            g2.setFont(fightFont);
            
            String fightText = "FIGHT!";
            FontMetrics fightFm = g2.getFontMetrics();
            
            int shakeX = (int)(8 * Math.sin(time * 0.15));
            int shakeY = (int)(6 * Math.cos(time * 0.18));
            
            int fightX = (screenWidth - fightFm.stringWidth(fightText)) / 2 + shakeX;
            int fightY = screenHeight / 2 + shakeY;
            
            g2.setColor(new Color(255, 0, 0, 150));
            g2.fillRect(0, 0, screenWidth, screenHeight);
            
            for (int dx = -6; dx <= 6; dx++) {
                for (int dy = -6; dy <= 6; dy++) {
                    if (dx != 0 || dy != 0) {
                        g2.setColor(new Color(0, 0, 0, 80));
                        g2.drawString(fightText, fightX + dx, fightY + dy);
                    }
                }
            }
            
            g2.setColor(new Color(255, 255, 100));
            g2.drawString(fightText, fightX, fightY);
            
            g2.setStroke(new BasicStroke(4));
            g2.setColor(new Color(255, 255, 255, 250));
            for (int i = 0; i < 16; i++) {
                double angle = i * Math.PI / 8;
                int x1 = screenWidth / 2;
                int y1 = screenHeight / 2;
                int x2 = x1 + (int)(Math.cos(angle) * 400);
                int y2 = y1 + (int)(Math.sin(angle) * 400);
                g2.drawLine(x1, y1, x2, y2);
            }
        }
    }
    
    private static void renderGameOverOverlay(Graphics2D g2, int screenWidth, int screenHeight) {
        OnlineMatchManager manager = OnlineMatchManager.getInstance();
        String result = manager.getGameOverResult();
        long elapsed = System.currentTimeMillis() - manager.getGameOverOverlayStartTime();
        
        float alpha = 1.0f;
        if (elapsed > 2000) {
            alpha = 1.0f - ((elapsed - 2000) / 1000.0f);
            alpha = Math.max(0.0f, alpha);
        }
        
        if (alpha <= 0) return;
        
        // Simple dark background
        g2.setColor(new Color(0, 0, 0, (int)(180 * alpha)));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        // Determine colors
        boolean isVictory = "VICTORY".equals(result);
        Color textColor = isVictory ? new Color(100, 255, 100) : new Color(255, 100, 100);
        
        // Simple text display
        Font resultFont = new Font("Arial", Font.BOLD, 80);
        g2.setFont(resultFont);
        
        String resultText = isVictory ? "VICTORY!" : "DEFEAT!";
        FontMetrics fm = g2.getFontMetrics();
        int textX = (screenWidth - fm.stringWidth(resultText)) / 2;
        int textY = screenHeight / 2;
        
        // Simple text with transparency
        g2.setColor(new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), (int)(255 * alpha)));
        g2.drawString(resultText, textX, textY);
    }
}
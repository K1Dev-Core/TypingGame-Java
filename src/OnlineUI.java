import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import shared.Player;

public class OnlineUI {
    private static String playerName = null;
    private static boolean showNameInput = false;
    private static boolean showMatchmakingButton = false;
    private static StringBuilder nameBuffer = new StringBuilder();
    
    public static void setPlayerName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            playerName = name.trim();
        }
    }
    
    public static String getPlayerName() {
        return playerName;
    }
    
    /**
     * Enter online mode by showing name input or starting matchmaking
     */
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
            // Enter pressed
            if (nameBuffer.length() > 0) {
                String name = nameBuffer.toString().trim();
                if (name.length() > 0) {
                    setPlayerName(name);
                    nameBuffer.setLength(0);
                    showNameInput = false;
                    showMatchmakingButton = true; // Show + button instead of auto-starting
                    NotificationSystem.showSuccess("Name set: " + name + ". Click + to find match!");
                } else {
                    NotificationSystem.showWarning("Please enter a valid name");
                }
            } else {
                NotificationSystem.showWarning("Please enter your name first");
            }
        } else if (c == '\b') {
            // Backspace
            if (nameBuffer.length() > 0) {
                nameBuffer.deleteCharAt(nameBuffer.length() - 1);
            }
        } else if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-') {
            // Valid character (expanded to include common username characters)
            if (nameBuffer.length() < 20) {
                // Don't allow spaces at the beginning
                if (c == ' ' && nameBuffer.length() == 0) {
                    return;
                }
                nameBuffer.append(c);
            } else {
                NotificationSystem.showWarning("Name too long (max 20 characters)");
            }
        }
        // Invalid characters are simply ignored
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
                renderStatus(g2, screenWidth, screenHeight, "Connected to server", Color.GREEN);
                break;
            case WAITING_FOR_OPPONENT:
                renderWaitingForOpponent(g2, screenWidth, screenHeight);
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
        
        // Render name input if needed
        if (showNameInput) {
            renderNameInput(g2, screenWidth, screenHeight);
        }
    }
    
    private static void renderNameInput(Graphics2D g2, int screenWidth, int screenHeight) {
        int panelWidth = 350;
        int panelHeight = 150;
        int x = (screenWidth - panelWidth) / 2;
        int y = (screenHeight - panelHeight) / 2;
        
        // Background panel with glow effect
        RoundRectangle2D panel = new RoundRectangle2D.Float(x, y, panelWidth, panelHeight, 15, 15);
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fill(panel);
        
        // Add border glow
        g2.setColor(new Color(25, 118, 210, 150));
        g2.setStroke(new BasicStroke(3));
        g2.draw(panel);
        
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1));
        g2.draw(panel);
        
        // Title
        Font titleFont = new Font("Arial", Font.BOLD, 18);
        g2.setFont(titleFont);
        g2.setColor(new Color(255, 255, 100));
        String title = "Enter Your Player Name";
        FontMetrics titleFm = g2.getFontMetrics();
        int titleX = x + (panelWidth - titleFm.stringWidth(title)) / 2;
        g2.drawString(title, titleX, y + 35);
        
        // Input field with better styling
        int fieldWidth = 250;
        int fieldHeight = 30;
        int fieldX = x + (panelWidth - fieldWidth) / 2;
        int fieldY = y + 55;
        
        // Field background
        g2.setColor(new Color(30, 30, 30));
        g2.fillRoundRect(fieldX, fieldY, fieldWidth, fieldHeight, 5, 5);
        
        // Field border - highlight if active
        g2.setColor(new Color(25, 118, 210));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(fieldX, fieldY, fieldWidth, fieldHeight, 5, 5);
        
        // Display current input with cursor
        Font inputFont = new Font("Arial", Font.BOLD, 16);
        g2.setFont(inputFont);
        g2.setColor(Color.WHITE);
        
        String displayText = nameBuffer.toString();
        if (displayText.isEmpty()) {
            // Show placeholder text
            g2.setColor(new Color(150, 150, 150));
            g2.drawString("Type your name here...", fieldX + 10, fieldY + 20);
        } else {
            g2.drawString(displayText, fieldX + 10, fieldY + 20);
        }
        
        // Blinking cursor
        if (System.currentTimeMillis() % 1000 < 500) {
            int cursorX = fieldX + 10 + g2.getFontMetrics().stringWidth(displayText);
            g2.setColor(Color.WHITE);
            g2.drawLine(cursorX, fieldY + 8, cursorX, fieldY + 22);
        }
        
        // Instructions with better formatting
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
        
        // Character count indicator
        g2.setColor(new Color(150, 150, 150));
        String countText = nameBuffer.length() + "/20";
        FontMetrics countFm = g2.getFontMetrics();
        g2.drawString(countText, fieldX + fieldWidth - countFm.stringWidth(countText) - 5, fieldY - 5);
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
        
        // Add animated dots
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
        
        // Add cancel button
        renderCancelButton(g2, screenWidth, screenHeight);
    }
    
    private static void renderCancelButton(Graphics2D g2, int screenWidth, int screenHeight) {
        int buttonWidth = 100;
        int buttonHeight = 35;
        int x = (screenWidth - buttonWidth) / 2;
        int y = 100;
        
        // Button background
        RoundRectangle2D button = new RoundRectangle2D.Float(x, y, buttonWidth, buttonHeight, 8, 8);
        g2.setColor(new Color(220, 50, 50, 180)); // Red with transparency
        g2.fill(button);
        
        // Button border
        g2.setColor(new Color(255, 100, 100));
        g2.setStroke(new BasicStroke(2));
        g2.draw(button);
        
        // Button text
        Font buttonFont = new Font("Arial", Font.BOLD, 14);
        g2.setFont(buttonFont);
        g2.setColor(Color.WHITE);
        
        String text = "Cancel";
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (buttonWidth - fm.stringWidth(text)) / 2;
        int textY = y + (buttonHeight + fm.getAscent()) / 2 - 2;
        g2.drawString(text, textX, textY);
    }
    
    private static void renderCountdown(Graphics2D g2, int screenWidth, int screenHeight) {
        OnlineMatchManager manager = OnlineMatchManager.getInstance();
        Player opponent = manager.getOpponent();
        
        if (opponent != null) {
            Font font = new Font("Arial", Font.BOLD, 20);
            g2.setFont(font);
            g2.setColor(Color.WHITE);
            
            String vsText = (playerName != null ? playerName : "You") + " vs " + opponent.name;
            FontMetrics fm = g2.getFontMetrics();
            int x = (screenWidth - fm.stringWidth(vsText)) / 2;
            g2.drawString(vsText, x, screenHeight / 2 - 50);
        }
        
        renderImprovedCountdown(g2, screenWidth, screenHeight);
    }
    
    private static void renderRaceInfo(Graphics2D g2, int screenWidth, int screenHeight) {
        OnlineMatchManager manager = OnlineMatchManager.getInstance();
        Player localPlayer = manager.getLocalPlayer();
        Player opponent = manager.getOpponent();
        
        if (localPlayer != null && opponent != null) {
            Font font = new Font("Arial", Font.BOLD, 14);
            g2.setFont(font);
            
            // Player info
            g2.setColor(Color.GREEN);
            String playerInfo = localPlayer.name + " (You)";
            g2.drawString(playerInfo, 20, 30);
            
            // Opponent info  
            g2.setColor(Color.RED);
            String opponentInfo = opponent.name;
            FontMetrics fm = g2.getFontMetrics();
            int opponentX = screenWidth - fm.stringWidth(opponentInfo) - 20;
            g2.drawString(opponentInfo, opponentX, 30);
            
            // Connection status
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.drawString("Online Match", screenWidth / 2 - 40, screenHeight - 40);
        }
    }
    
    private static void renderMatchFinished(Graphics2D g2, int screenWidth, int screenHeight) {
        renderStatus(g2, screenWidth, screenHeight, "Match finished! Returning to menu...", Color.CYAN);
    }
    
    public static boolean handleClick(int x, int y, int screenWidth, int screenHeight) {
        OnlineMatchManager manager = OnlineMatchManager.getInstance();
        
        if (manager.getMatchState() == OnlineMatchManager.MatchState.OFFLINE) {
            if (showMatchmakingButton && handleMatchmakingButtonClick(x, y, screenWidth, screenHeight)) {
                return true;
            }
        } else if (manager.getMatchState() == OnlineMatchManager.MatchState.WAITING_FOR_OPPONENT) {
            // Check cancel button click
            int buttonWidth = 100;
            int buttonHeight = 35;
            int buttonX = (screenWidth - buttonWidth) / 2;
            int buttonY = 100;
            
            if (x >= buttonX && x <= buttonX + buttonWidth && y >= buttonY && y <= buttonY + buttonHeight) {
                manager.cancelMatchmaking();
                return true;
            }
        }
        
        return false;
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
    
    private static void renderMatchmakingButton(Graphics2D g2, int screenWidth, int screenHeight) {
        int leftMargin = 20;
        int rightMargin = 40;
        int gapFromWord = 48;
        int cx = screenWidth / 2;
        
        // Position button over bot/opponent character location
        int estimatedBotX = cx + 200; // Bot is on the right side
        int estimatedBotY = screenHeight - 60 - 80;
        
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
                // Cancel matchmaking
                manager.cancelMatchmaking();
                showMatchmakingButton = true; // Show the + button again
                NotificationSystem.showWarning("Matchmaking canceled");
            } else {
                // Start matchmaking
                showMatchmakingButton = false;
                manager.startMatchmaking(playerName);
                // No notification - too cluttered
            }
            return true;
        }
        return false;
    }
    
    private static void renderImprovedCountdown(Graphics2D g2, int screenWidth, int screenHeight) {
        OnlineMatchManager manager = OnlineMatchManager.getInstance();
        int countdown = manager.getCurrentCountdown();
        long countdownStartTime = manager.getCountdownStartTime();
        
        // Calculate current countdown based on elapsed time for smooth animation
        long elapsed = System.currentTimeMillis() - countdownStartTime;
        double exactCountdown = Math.max(0, 10.0 - (elapsed / 1000.0)); // Updated to 10 seconds
        
        if (exactCountdown > 3) {
            // Enhanced "GET READY" phase with pulsing animation
            long time = System.currentTimeMillis();
            
            // Dynamic background effect
            double pulseIntensity = 0.3 + 0.2 * Math.sin(time * 0.005);
            g2.setColor(new Color(0, 20, 40, (int)(100 * pulseIntensity)));
            g2.fillRect(0, 0, screenWidth, screenHeight);
            
            // Animated "GET READY" text with scaling
            double scale = 1.0 + 0.1 * Math.sin(time * 0.008);
            Font readyFont = new Font("Arial", Font.BOLD, (int)(60 * scale));
            g2.setFont(readyFont);
            
            // Rainbow color cycling
            float hue = (time * 0.002f) % 1.0f;
            Color readyColor = Color.getHSBColor(hue, 0.8f, 1.0f);
            
            String readyText = "GET READY!";
            FontMetrics readyFm = g2.getFontMetrics();
            int readyX = (screenWidth - readyFm.stringWidth(readyText)) / 2;
            int readyY = screenHeight / 2 - 80;
            
            // Multiple shadow layers for glow effect
            for (int i = 8; i >= 0; i -= 2) {
                g2.setColor(new Color(readyColor.getRed(), readyColor.getGreen(), readyColor.getBlue(), 40 - i * 4));
                g2.drawString(readyText, readyX + i, readyY + i);
            }
            
            // Main text
            g2.setColor(readyColor);
            g2.drawString(readyText, readyX, readyY);
            
            // Large countdown number with dramatic scaling
            int currentNum = (int)Math.ceil(exactCountdown);
            double numberScale = 1.2 + 0.3 * Math.sin(time * 0.01);
            Font numberFont = new Font("Arial", Font.BOLD, (int)(80 * numberScale));
            g2.setFont(numberFont);
            
            String countText = String.valueOf(currentNum);
            FontMetrics numberFm = g2.getFontMetrics();
            int numberX = (screenWidth - numberFm.stringWidth(countText)) / 2;
            int numberY = screenHeight / 2 + 50;
            
            // Number background circle
            int circleSize = (int)(150 * numberScale);
            int circleX = screenWidth / 2 - circleSize / 2;
            int circleY = numberY - 60 - circleSize / 2;
            
            // Animated circle with gradient
            g2.setColor(new Color(40, 40, 40, 150));
            g2.fillOval(circleX, circleY, circleSize, circleSize);
            
            // Circle border with pulsing
            g2.setColor(new Color(255, 255, 255, (int)(200 * pulseIntensity)));
            g2.setStroke(new BasicStroke(4));
            g2.drawOval(circleX, circleY, circleSize, circleSize);
            
            // Number with dramatic shadow
            g2.setColor(new Color(0, 0, 0, 100));
            g2.drawString(countText, numberX + 4, numberY + 4);
            g2.setColor(Color.WHITE);
            g2.drawString(countText, numberX, numberY);
            
        } else if (exactCountdown > 0) {
            // ENHANCED final countdown (3, 2, 1) with MASSIVE animated numbers
            long time = System.currentTimeMillis();
            int currentNum = (int)Math.ceil(exactCountdown);
            
            // Dramatic screen effect based on number
            Color screenTint;
            if (currentNum == 3) {
                screenTint = new Color(255, 0, 0, 80); // Red
            } else if (currentNum == 2) {
                screenTint = new Color(255, 165, 0, 80); // Orange
            } else {
                screenTint = new Color(255, 255, 0, 80); // Yellow
            }
            g2.setColor(screenTint);
            g2.fillRect(0, 0, screenWidth, screenHeight);
            
            // MASSIVE font size with entrance animation
            double timeInSecond = exactCountdown - Math.floor(exactCountdown);
            double entranceScale = 1.0;
            if (timeInSecond > 0.8) {
                // Number entrance: grows from small to large
                entranceScale = (1.0 - timeInSecond) * 5.0;
                entranceScale = Math.min(1.0, entranceScale);
            }
            
            int fontSize = (int)(200 * entranceScale); // MASSIVE 200px font!
            Font countdownFont = new Font("Arial", Font.BOLD, fontSize);
            g2.setFont(countdownFont);
            
            String countText = String.valueOf(currentNum);
            FontMetrics fm = g2.getFontMetrics();
            int x = (screenWidth - fm.stringWidth(countText)) / 2;
            int y = screenHeight / 2 + fm.getAscent() / 2;
            
            // Color matching the screen tint but more vibrant
            Color textColor;
            if (currentNum == 3) {
                textColor = new Color(255, 100, 100);
            } else if (currentNum == 2) {
                textColor = new Color(255, 180, 50);
            } else {
                textColor = new Color(255, 255, 100);
            }
            
            // MASSIVE background circle with multiple glow layers
            int circleSize = (int)(400 * entranceScale);
            int circleX = screenWidth / 2 - circleSize / 2;
            int circleY = screenHeight / 2 - circleSize / 2;
            
            // Multiple glow layers for dramatic effect
            for (int i = 0; i < 5; i++) {
                int glowSize = circleSize + (i * 40);
                int glowX = screenWidth / 2 - glowSize / 2;
                int glowY = screenHeight / 2 - glowSize / 2;
                g2.setColor(new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 30 - i * 5));
                g2.fillOval(glowX, glowY, glowSize, glowSize);
            }
            
            // Main circle background
            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillOval(circleX, circleY, circleSize, circleSize);
            
            // Animated circle border
            double borderPulse = 1.0 + 0.3 * Math.sin(time * 0.02);
            g2.setColor(textColor);
            g2.setStroke(new BasicStroke((int)(8 * borderPulse)));
            g2.drawOval(circleX, circleY, circleSize, circleSize);
            
            // MASSIVE text shadow with multiple layers
            for (int i = 10; i >= 0; i -= 2) {
                g2.setColor(new Color(0, 0, 0, 80 - i * 6));
                g2.drawString(countText, x + i, y + i);
            }
            
            // Main countdown text - HUGE and dramatic
            g2.setColor(textColor);
            g2.drawString(countText, x, y);
            
            // Additional sparkle effects around the number
            for (int i = 0; i < 8; i++) {
                double angle = (time * 0.01 + i * Math.PI / 4) % (2 * Math.PI);
                int sparkleX = (int)(screenWidth / 2 + Math.cos(angle) * (circleSize * 0.6));
                int sparkleY = (int)(screenHeight / 2 + Math.sin(angle) * (circleSize * 0.6));
                g2.setColor(new Color(255, 255, 255, 150));
                g2.fillOval(sparkleX - 4, sparkleY - 4, 8, 8);
            }
            
        } else {
            // Enhanced "FIGHT!" with screen-shaking effect
            Font fightFont = new Font("Arial", Font.BOLD, 120);
            g2.setFont(fightFont);
            
            String fightText = "FIGHT!";
            FontMetrics fightFm = g2.getFontMetrics();
            
            // Screen shake effect
            long time = System.currentTimeMillis();
            int shakeX = (int)(5 * Math.sin(time * 0.1));
            int shakeY = (int)(3 * Math.cos(time * 0.15));
            
            int fightX = (screenWidth - fightFm.stringWidth(fightText)) / 2 + shakeX;
            int fightY = screenHeight / 2 + shakeY;
            
            // Dramatic background flash with multiple colors
            g2.setColor(new Color(255, 0, 0, 120));
            g2.fillRect(0, 0, screenWidth, screenHeight);
            
            // MASSIVE text outline
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(6));
            for (int dx = -4; dx <= 4; dx++) {
                for (int dy = -4; dy <= 4; dy++) {
                    if (dx != 0 || dy != 0) {
                        g2.drawString(fightText, fightX + dx, fightY + dy);
                    }
                }
            }
            
            // Main fight text with gradient effect
            g2.setColor(new Color(255, 255, 100));
            g2.drawString(fightText, fightX, fightY);
            
            // Add explosion lines radiating from center
            g2.setStroke(new BasicStroke(3));
            g2.setColor(new Color(255, 255, 255, 200));
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6;
                int x1 = screenWidth / 2;
                int y1 = screenHeight / 2;
                int x2 = x1 + (int)(Math.cos(angle) * 300);
                int y2 = y1 + (int)(Math.sin(angle) * 300);
                g2.drawLine(x1, y1, x2, y2);
            }
        }
    }
}
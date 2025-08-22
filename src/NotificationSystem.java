import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationSystem {
    private static final List<Notification> notifications = new ArrayList<>();
    private static final int MAX_NOTIFICATIONS = 5;
    private static final int NOTIFICATION_DURATION = 3000; // 3 seconds
    private static final int FADE_DURATION = 500; // 0.5 seconds
    
    public static void showNotification(String message, NotificationType type) {
        if (notifications.size() >= MAX_NOTIFICATIONS) {
            notifications.remove(0);
        }
        notifications.add(new Notification(message, type, System.currentTimeMillis()));
    }
    
    public static void showInfo(String message) {
        showNotification(message, NotificationType.INFO);
    }
    
    public static void showSuccess(String message) {
        showNotification(message, NotificationType.SUCCESS);
    }
    
    public static void showWarning(String message) {
        showNotification(message, NotificationType.WARNING);
    }
    
    public static void showError(String message) {
        showNotification(message, NotificationType.ERROR);
    }
    
    public static void update() {
        long currentTime = System.currentTimeMillis();
        Iterator<Notification> iterator = notifications.iterator();
        
        while (iterator.hasNext()) {
            Notification notification = iterator.next();
            if (currentTime - notification.startTime > NOTIFICATION_DURATION + FADE_DURATION) {
                iterator.remove();
            }
        }
    }
    
    public static void render(Graphics2D g2, int screenWidth, int screenHeight) {
        if (notifications.isEmpty()) return;
        
        Font font = new Font("Arial", Font.BOLD, 10);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        
        int x = 10;
        int baseY = screenHeight - 30;
        long currentTime = System.currentTimeMillis();
        
        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notification notification = notifications.get(i);
            long elapsed = currentTime - notification.startTime;
            float alpha = 1.0f;
            
            if (elapsed > NOTIFICATION_DURATION) {
                alpha = 1.0f - (float)(elapsed - NOTIFICATION_DURATION) / FADE_DURATION;
                alpha = Math.max(0f, alpha);
            }
            
            if (alpha > 0) {
                int y = baseY - (i * 18);
                renderNotification(g2, notification, x, y, fm, alpha);
            }
        }
    }
    
    private static void renderNotification(Graphics2D g2, Notification notification, int x, int y, FontMetrics fm, float alpha) {
        Color textColor = getTextColor(notification.type);
        textColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), (int)(255 * alpha));
        
        g2.setColor(textColor);
        g2.drawString("+ "+notification.message, x, y);
    }

    
    private static Color getTextColor(NotificationType type) {
        return new Color(255, 255, 255);
    }
    
    public static void clear() {
        notifications.clear();
    }
    
    public enum NotificationType {
        INFO, SUCCESS, WARNING, ERROR
    }
    
    private static class Notification {
        final String message;
        final NotificationType type;
        final long startTime;
        
        Notification(String message, NotificationType type, long startTime) {
            this.message = message;
            this.type = type;
            this.startTime = startTime;
        }
    }
}
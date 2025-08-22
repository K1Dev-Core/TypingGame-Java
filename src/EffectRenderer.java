import java.awt.*;

public class EffectRenderer {
    
    public static void renderComboEffect(Graphics2D g2, ComboEffect comboEffect, Font boldFont, Font plainFont) {
        if (comboEffect != null && comboEffect.isActive()) {
            comboEffect.render(g2, boldFont, plainFont);
        }
    }
    
    public static void renderDamageText(Graphics2D g2, GameState gameState, Font damageFont, long currentTime) {
        renderPlayerDamage(g2, gameState, damageFont, currentTime);
        renderBotDamage(g2, gameState, damageFont, currentTime);
    }
    
    private static void renderPlayerDamage(Graphics2D g2, GameState gameState, Font damageFont, long currentTime) {
        if (!gameState.showPlayerDamage) return;
        
        long elapsed = currentTime - (gameState.playerDamageUntil - 1000);
        float alpha = Math.max(0f, 1f - (elapsed / 1000f));
        int yOffset = (int) (elapsed * 0.05f);
        
        g2.setColor(new Color(255, 80, 80, (int) (255 * alpha)));
        g2.setFont(damageFont);
        g2.drawString("-1", gameState.playerDamageX, gameState.playerDamageY - yOffset);
    }
    
    private static void renderBotDamage(Graphics2D g2, GameState gameState, Font damageFont, long currentTime) {
        if (!gameState.showBotDamage) return;
        
        long elapsed = currentTime - (gameState.botDamageUntil - 1000);
        float alpha = Math.max(0f, 1f - (elapsed / 1000f));
        int yOffset = (int) (elapsed * 0.05f);
        
        g2.setColor(new Color(255, 80, 80, (int) (255 * alpha)));
        g2.setFont(damageFont);
        g2.drawString("-1", gameState.botDamageX, gameState.botDamageY - yOffset);
    }
    
    public static void renderHitEffect(Graphics2D g2, HitEffect hitEffect) {
        if (hitEffect != null) {
            hitEffect.draw(g2);
        }
    }
}
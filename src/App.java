import javax.swing.*;
import java.awt.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Typing • Words");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            GamePanel gp = new GamePanel();
            f.add(gp);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            gp.requestFocusInWindow();
        });
    }
}
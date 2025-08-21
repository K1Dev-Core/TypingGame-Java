import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ScoreManager {
    private static final String DB_FILE = "./db.txt";
    private static ScoreManager instance;

    private int highScore = 0;
    private int lastScore = 0;

    private ScoreManager() {
        loadScores();
    }

    public static ScoreManager getInstance() {
        if (instance == null) {
            instance = new ScoreManager();
        }
        return instance;
    }

    public void saveScore(int score) {
        lastScore = score;
        if (score > highScore) {
            highScore = score;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DB_FILE))) {
            writer.write(highScore + "\n" + lastScore);
        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
        }
    }

    private void loadScores() {
        File file = new File(DB_FILE);

        if (!file.exists()) {
            try {
                file.createNewFile();
                saveScore(0);
            } catch (IOException e) {
                System.err.println("error: " + e.getMessage());
            }
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String highScoreStr = reader.readLine();
            String lastScoreStr = reader.readLine();

            if (highScoreStr != null) {
                try {
                    highScore = Integer.parseInt(highScoreStr.trim());
                } catch (NumberFormatException e) {
                    highScore = 0;
                }
            }

            if (lastScoreStr != null) {
                try {
                    lastScore = Integer.parseInt(lastScoreStr.trim());
                } catch (NumberFormatException e) {
                    lastScore = 0;
                }
            }
        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
        }
    }

    public int getHighScore() {
        return highScore;
    }

    public int getLastScore() {
        return lastScore;
    }
}

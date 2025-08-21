import java.io.*;

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
            
            highScore = parseIntSafe(highScoreStr, 0);
            lastScore = parseIntSafe(lastScoreStr, 0);
        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
        }
    }

    private int parseIntSafe(String str, int defaultValue) {
        if (str == null) return defaultValue;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getHighScore() {
        return highScore;
    }

    public int getLastScore() {
        return lastScore;
    }
}

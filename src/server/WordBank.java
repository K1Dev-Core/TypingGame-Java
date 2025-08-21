package server;

import java.io.*;
import java.util.*;

public class WordBank {
    private List<String> words;
    private Random random;

    public WordBank() {
        words = new ArrayList<>();
        random = new Random();
        loadWords();
    }

    private void loadWords() {
        try (BufferedReader br = new BufferedReader(new FileReader("./res/words.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    words.add(line.toUpperCase());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not load words file, using default words");
            loadDefaultWords();
        }

        if (words.isEmpty()) {
            loadDefaultWords();
        }
    }

    private void loadDefaultWords() {
        String[] defaultWords = {
                "HELLO", "WORLD", "JAVA", "CODE", "GAME", "TYPE", "FAST", "GOOD",
                "BEST", "PLAY", "WIN", "LOSE", "FIGHT", "POWER", "SKILL", "MAGIC"
        };
        words.addAll(Arrays.asList(defaultWords));
    }

    public String getRandomWord() {
        if (words.isEmpty()) {
            return "HELLO";
        }
        return words.get(random.nextInt(words.size()));
    }

    public int getWordCount() {
        return words.size();
    }
}

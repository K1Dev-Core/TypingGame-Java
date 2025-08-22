package server;

import java.io.*;
import java.util.*;

public class WordBank {
    private List<String> words;
    private Random random;
    private String lastGeneratedWord; // Track last word to prevent immediate duplicates
    private Set<String> recentWords; // Track recent words for better duplicate prevention
    private static final int RECENT_WORDS_MEMORY = 5; // Remember last 5 words

    public WordBank() {
        words = new ArrayList<>();
        random = new Random();
        recentWords = new LinkedHashSet<>();
        lastGeneratedWord = null;
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
        
        String newWord = generateNonDuplicateWord(null);
        updateWordHistory(newWord);
        System.out.println("WordBank: Generated random word: " + newWord);
        return newWord;
    }
    
    public String getRandomWord(String currentWord) {
        if (words.isEmpty()) {
            return "HELLO";
        }
        
        String cleanCurrentWord = (currentWord != null) ? currentWord.toUpperCase().trim() : null;
        String newWord = generateNonDuplicateWord(cleanCurrentWord);
        updateWordHistory(newWord);
        
        System.out.println("WordBank: Generated new word after '" + cleanCurrentWord + "': " + newWord);
        System.out.println("WordBank: Recent words: " + recentWords);
        return newWord;
    }
    
    private String generateNonDuplicateWord(String avoidWord) {
        if (words.size() <= 1) {
            return words.get(0);
        }
        
        // Create list of available words (excluding recent words)
        List<String> availableWords = new ArrayList<>();
        for (String word : words) {
            // Skip if it's the current word to avoid
            if (avoidWord != null && word.equals(avoidWord)) {
                continue;
            }
            // Skip if it's in recent words memory (unless we have very few words)
            if (words.size() > RECENT_WORDS_MEMORY && recentWords.contains(word)) {
                continue;
            }
            availableWords.add(word);
        }
        
        // If no available words (all are recent), use any word except current
        if (availableWords.isEmpty()) {
            System.out.println("WordBank: All words are recent, using fallback selection");
            for (String word : words) {
                if (avoidWord == null || !word.equals(avoidWord)) {
                    availableWords.add(word);
                }
            }
        }
        
        // If still empty, return a fallback
        if (availableWords.isEmpty()) {
            return "GAME";
        }
        
        return availableWords.get(random.nextInt(availableWords.size()));
    }
    
    private void updateWordHistory(String word) {
        lastGeneratedWord = word;
        recentWords.add(word);
        
        // Maintain memory size limit
        if (recentWords.size() > RECENT_WORDS_MEMORY) {
            Iterator<String> iterator = recentWords.iterator();
            iterator.next();
            iterator.remove();
        }
    }

    public int getWordCount() {
        return words.size();
    }
}

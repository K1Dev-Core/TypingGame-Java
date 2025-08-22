import java.io.*;
import java.util.*;

public class PlayerDatabase {
    private static final String DB_FILE = "player_scores.txt";
    private static final String SEPARATOR = ",";
    
    public static void recordOnlineLogin(String playerName) {
        try {
            Map<String, PlayerRecord> records = loadAllRecords();
            
            PlayerRecord existing = records.get(playerName);
            int score = existing != null ? existing.bestScore : 0;
            int wpm = existing != null ? existing.bestWPM : 0;
            int wins = existing != null ? existing.onlineWins : 0;
            int losses = existing != null ? existing.onlineLosses : 0;
            int logins = existing != null ? existing.onlineLogins : 0;
            String favChar = existing != null ? existing.favoriteCharacter : "medieval_king";
            
            // Increment login count
            logins++;
            
            records.put(playerName, new PlayerRecord(playerName, score, wpm, wins, losses, logins, favChar, System.currentTimeMillis()));
            writeAllRecords(records);
        } catch (IOException e) {
            System.err.println("Error recording online login: " + e.getMessage());
        }
    }
    
    public static void savePlayerScore(String playerName, int score, int wpm) {
        try {
            Map<String, PlayerRecord> records = loadAllRecords();
            
            PlayerRecord existing = records.get(playerName);
            if (existing == null || score > existing.bestScore) {
                int wins = existing != null ? existing.onlineWins : 0;
                int losses = existing != null ? existing.onlineLosses : 0;
                int logins = existing != null ? existing.onlineLogins : 0;
                String favChar = existing != null ? existing.favoriteCharacter : "medieval_king";
                records.put(playerName, new PlayerRecord(playerName, score, wpm, wins, losses, logins, favChar, System.currentTimeMillis()));
                writeAllRecords(records);
            }
        } catch (IOException e) {
            System.err.println("Error saving player score: " + e.getMessage());
        }
    }
    
    public static void saveOnlineMatchResult(String playerName, boolean won, String characterUsed) {
        try {
            Map<String, PlayerRecord> records = loadAllRecords();
            
            PlayerRecord existing = records.get(playerName);
            int score = existing != null ? existing.bestScore : 0;
            int wpm = existing != null ? existing.bestWPM : 0;
            int wins = existing != null ? existing.onlineWins : 0;
            int losses = existing != null ? existing.onlineLosses : 0;
            int logins = existing != null ? existing.onlineLogins : 0;
            
            if (won) {
                wins++;
            } else {
                losses++;
            }
            
            records.put(playerName, new PlayerRecord(playerName, score, wpm, wins, losses, logins, characterUsed, System.currentTimeMillis()));
            writeAllRecords(records);
        } catch (IOException e) {
            System.err.println("Error saving online match result: " + e.getMessage());
        }
    }
    
    public static PlayerRecord getPlayerRecord(String playerName) {
        try {
            Map<String, PlayerRecord> records = loadAllRecords();
            return records.get(playerName);
        } catch (IOException e) {
            System.err.println("Error loading player record: " + e.getMessage());
            return null;
        }
    }
    
    public static List<PlayerRecord> getTopPlayers(int limit) {
        try {
            Map<String, PlayerRecord> records = loadAllRecords();
            return records.values().stream()
                    .sorted((a, b) -> Integer.compare(b.bestScore, a.bestScore))
                    .limit(limit)
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        } catch (IOException e) {
            System.err.println("Error loading top players: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private static Map<String, PlayerRecord> loadAllRecords() throws IOException {
        Map<String, PlayerRecord> records = new HashMap<>();
        File file = new File(DB_FILE);
        
        if (!file.exists()) {
            return records;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(SEPARATOR);
                if (parts.length >= 4) {
                    String name = parts[0];
                    int score = Integer.parseInt(parts[1]);
                    int wpm = Integer.parseInt(parts[2]);
                    
                    if (parts.length >= 7) {
                        int wins = Integer.parseInt(parts[3]);
                        int losses = Integer.parseInt(parts[4]);
                        String favChar = parts[5];
                        long timestamp = Long.parseLong(parts[6]);
                        records.put(name, new PlayerRecord(name, score, wpm, wins, losses, favChar, timestamp));
                    } else {
                        long timestamp = Long.parseLong(parts[3]);
                        records.put(name, new PlayerRecord(name, score, wpm, 0, 0, "medieval_king", timestamp));
                    }
                }
            }
        }
        
        return records;
    }
    
    private static void writeAllRecords(Map<String, PlayerRecord> records) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DB_FILE))) {
            for (PlayerRecord record : records.values()) {
                writer.println(record.name + SEPARATOR + 
                             record.bestScore + SEPARATOR + 
                             record.bestWPM + SEPARATOR + 
                             record.onlineWins + SEPARATOR +
                             record.onlineLosses + SEPARATOR +
                             record.favoriteCharacter + SEPARATOR +
                             record.timestamp);
            }
        }
    }
    
    public static class PlayerRecord {
        public final String name;
        public final int bestScore;
        public final int bestWPM;
        public final int onlineWins;
        public final int onlineLosses;
        public final String favoriteCharacter;
        public final long timestamp;
        
        public PlayerRecord(String name, int bestScore, int bestWPM, int onlineWins, int onlineLosses, String favoriteCharacter, long timestamp) {
            this.name = name;
            this.bestScore = bestScore;
            this.bestWPM = bestWPM;
            this.onlineWins = onlineWins;
            this.onlineLosses = onlineLosses;
            this.favoriteCharacter = favoriteCharacter != null ? favoriteCharacter : "medieval_king";
            this.timestamp = timestamp;
        }
        
        public double getWinRate() {
            int totalGames = onlineWins + onlineLosses;
            return totalGames > 0 ? (double) onlineWins / totalGames : 0.0;
        }
    }
}
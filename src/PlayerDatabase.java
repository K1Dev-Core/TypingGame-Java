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
    
    public static void updateFavoriteCharacter(String playerName, String characterId) {
        try {
            Map<String, PlayerRecord> records = loadAllRecords();
            
            PlayerRecord existing = records.get(playerName);
            if (existing != null) {
                records.put(playerName, new PlayerRecord(
                    playerName, 
                    existing.bestScore, 
                    existing.bestWPM, 
                    existing.onlineWins, 
                    existing.onlineLosses, 
                    existing.onlineLogins, 
                    characterId, 
                    System.currentTimeMillis()
                ));
                writeAllRecords(records);
            } else {
                records.put(playerName, new PlayerRecord(playerName, 0, 0, 0, 0, 0, characterId, System.currentTimeMillis()));
                writeAllRecords(records);
            }
        } catch (IOException e) {
            System.err.println("Error updating favorite character: " + e.getMessage());
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
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] parts = line.split(SEPARATOR);
                    if (parts.length >= 4) {
                        String name = parts[0].trim();
                        
                        // Skip lines with empty names or invalid data
                        if (name.isEmpty()) {
                            System.err.println("Skipping line " + lineNumber + " with empty name: " + line);
                            continue;
                        }
                        
                        int score = parseIntSafe(parts[1], 0);
                        int wpm = parseIntSafe(parts[2], 0);
                        
                        if (parts.length >= 8) {
                            int wins = parseIntSafe(parts[3], 0);
                            int losses = parseIntSafe(parts[4], 0);
                            int logins = parseIntSafe(parts[5], 0);
                            String favChar = parts[6].trim();
                            if (favChar.isEmpty()) favChar = "medieval_king";
                            long timestamp = parseLongSafe(parts[7], System.currentTimeMillis());
                            records.put(name, new PlayerRecord(name, score, wpm, wins, losses, logins, favChar, timestamp));
                        } else if (parts.length >= 7) {
                            int wins = parseIntSafe(parts[3], 0);
                            int losses = parseIntSafe(parts[4], 0);
                            String favChar = parts[5].trim();
                            if (favChar.isEmpty()) favChar = "medieval_king";
                            long timestamp = parseLongSafe(parts[6], System.currentTimeMillis());
                            records.put(name, new PlayerRecord(name, score, wpm, wins, losses, 0, favChar, timestamp));
                        } else {
                            long timestamp = parseLongSafe(parts[3], System.currentTimeMillis());
                            records.put(name, new PlayerRecord(name, score, wpm, 0, 0, 0, "medieval_king", timestamp));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line " + lineNumber + ": " + line + " - " + e.getMessage());
                    // Continue processing other lines
                }
            }
        }
        
        return records;
    }
    
    private static int parseIntSafe(String str, int defaultValue) {
        if (str == null || str.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid integer: " + str + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    private static long parseLongSafe(String str, long defaultValue) {
        if (str == null || str.trim().isEmpty()) return defaultValue;
        try {
            return Long.parseLong(str.trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid long: " + str + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    private static void writeAllRecords(Map<String, PlayerRecord> records) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DB_FILE))) {
            for (PlayerRecord record : records.values()) {
                writer.println(record.name + SEPARATOR + 
                             record.bestScore + SEPARATOR + 
                             record.bestWPM + SEPARATOR + 
                             record.onlineWins + SEPARATOR +
                             record.onlineLosses + SEPARATOR +
                             record.onlineLogins + SEPARATOR +
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
        public final int onlineLogins;
        public final String favoriteCharacter;
        public final long timestamp;
        
        public PlayerRecord(String name, int bestScore, int bestWPM, int onlineWins, int onlineLosses, int onlineLogins, String favoriteCharacter, long timestamp) {
            this.name = name;
            this.bestScore = bestScore;
            this.bestWPM = bestWPM;
            this.onlineWins = onlineWins;
            this.onlineLosses = onlineLosses;
            this.onlineLogins = onlineLogins;
            this.favoriteCharacter = favoriteCharacter != null ? favoriteCharacter : "medieval_king";
            this.timestamp = timestamp;
        }
        
        public double getWinRate() {
            int totalGames = onlineWins + onlineLosses;
            return totalGames > 0 ? (double) onlineWins / totalGames : 0.0;
        }
    }
}
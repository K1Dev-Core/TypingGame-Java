package shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameRoom implements Serializable {
    private static final long serialVersionUID = 2L;

    public enum RoomState {
        WAITING_FOR_PLAYERS,
        WAITING_FOR_HOST,
        COUNTDOWN,
        GAME_STARTED,
        GAME_ENDED
    }

    public String id;
    public String name;
    public List<Player> players;
    public Player host;
    public boolean isGameStarted;
    public String currentWord;
    public long gameStartTime;
    public int maxPlayers;
    public RoomState roomState;
    public int countdown;
    public long countdownStartTime;

    public GameRoom(String id, String name, Player host) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.players = new ArrayList<>();
        this.players.add(host);
        this.isGameStarted = false;
        this.currentWord = "";
        this.maxPlayers = 2;
        this.roomState = RoomState.WAITING_FOR_PLAYERS;
        this.countdown = 0;
        this.countdownStartTime = 0;
    }

    public GameRoom(GameRoom other) {
        this.id = other.id;
        this.name = other.name;
        
        this.players = new ArrayList<>();
        for (Player p : other.players) {
            Player playerCopy = new Player(p.id, p.name, p.selectedCharacterId);
            playerCopy.health = p.health;
            playerCopy.wordsCompleted = p.wordsCompleted;
            playerCopy.wpm = p.wpm;
            playerCopy.isReady = p.isReady;
            playerCopy.isAlive = p.isAlive;
            this.players.add(playerCopy);
        }
        
        if (other.host != null) {
            this.host = this.players.stream()
                .filter(p -> p.id.equals(other.host.id))
                .findFirst()
                .orElse(null);
        }
        
        this.isGameStarted = other.isGameStarted;
        this.currentWord = other.currentWord;
        this.gameStartTime = other.gameStartTime;
        this.maxPlayers = other.maxPlayers;
        this.roomState = other.roomState;
        this.countdown = other.countdown;
        this.countdownStartTime = other.countdownStartTime;
    }

    public boolean addPlayer(Player player) {
        if (players.size() < maxPlayers && !isGameStarted) {
            if (players.stream().anyMatch(p -> p.id.equals(player.id))) {
                System.out.println("Player " + player.name + " is already in room " + name);
                return true;
            }

            players.add(player);
            System.out
                    .println("Player " + player.name + " added to room " + name + ". Total players: " + players.size());
            return true;
        }
        System.out.println("Failed to add player " + player.name + " to room " + name + ". Players: " + players.size()
                + "/" + maxPlayers + ", GameStarted: " + isGameStarted);
        return false;
    }

    public void removePlayer(String playerId) {
        players.removeIf(p -> p.id.equals(playerId));
        if (host.id.equals(playerId) && !players.isEmpty()) {
            host = players.get(0);
        }
    }

    public Player getPlayer(String playerId) {
        return players.stream().filter(p -> p.id.equals(playerId)).findFirst().orElse(null);
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public void startCountdown() {
        if (isFull() && roomState == RoomState.WAITING_FOR_PLAYERS) {
            roomState = RoomState.COUNTDOWN;
            countdown = 10;
            countdownStartTime = System.currentTimeMillis();
        }
    }

    public boolean updateCountdown() {
        if (roomState != RoomState.COUNTDOWN)
            return false;

        long elapsed = System.currentTimeMillis() - countdownStartTime;
        int newCountdown = 10 - (int) (elapsed / 1000);

        if (newCountdown != countdown) {
            countdown = Math.max(0, newCountdown);
            if (countdown == 0) {
                roomState = RoomState.GAME_STARTED;
                isGameStarted = true;
                gameStartTime = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }

    public boolean shouldStartGame() {
        return roomState == RoomState.GAME_STARTED && isGameStarted;
    }
}

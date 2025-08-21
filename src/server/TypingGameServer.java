package server;

import shared.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class TypingGameServer {
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private Map<String, GameRoom> rooms;
    private Map<String, ClientHandler> clients;
    private WordBank wordBank;
    private ExecutorService threadPool;
    private ScheduledExecutorService countdownScheduler;

    public TypingGameServer() {
        rooms = new ConcurrentHashMap<>();
        clients = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();
        countdownScheduler = Executors.newScheduledThreadPool(2);
        wordBank = new WordBank();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        isRunning = true;

        System.out.println("TypingGame Server started on port " + PORT);

        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                threadPool.submit(handler);
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        }
    }

    public void stop() throws IOException {
        isRunning = false;
        if (serverSocket != null) {
            serverSocket.close();
        }
        threadPool.shutdown();
    }

    public synchronized String createRoom(String roomName, Player host) {
        String roomId = UUID.randomUUID().toString().substring(0, 8);
        GameRoom room = new GameRoom(roomId, roomName, host);
        rooms.put(roomId, room);
        return roomId;
    }

    public synchronized boolean joinRoom(String roomId, Player player) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            System.out.println("Before joining - Room " + room.name + " has " + room.players.size() + " players");
            boolean result = room.addPlayer(player);
            System.out.println("After joining - Room " + room.name + " has " + room.players.size() + " players");

            // Check if room is now full and start countdown
            if (result && room.isFull() && room.roomState == GameRoom.RoomState.WAITING_FOR_PLAYERS) {
                System.out.println("Room " + room.name + " is full! Ready to start (waiting for host to start)...");
                room.roomState = GameRoom.RoomState.WAITING_FOR_HOST;

                // Notify all players that room is full and waiting for host to start
                broadcastToRoom(roomId, new NetworkMessage(NetworkMessage.MessageType.ROOM_UPDATE,
                        null, roomId, new GameRoom(room)));
            }

            return result;
        }
        return false;
    }

    public synchronized void leaveRoom(String roomId, String playerId) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            room.removePlayer(playerId);
            if (room.players.isEmpty()) {
                rooms.remove(roomId);
            }
        }
    }

    public synchronized List<GameRoom> getRoomList() {
        System.out.println("Getting room list - Total rooms: " + rooms.size());
        for (String roomId : rooms.keySet()) {
            GameRoom room = rooms.get(roomId);
            System.out.println("  Room: " + room.name + " (ID: " + roomId + ") - Players: " + room.players.size() + "/"
                    + room.maxPlayers);
        }

        List<GameRoom> roomCopies = new ArrayList<>();
        for (GameRoom room : rooms.values()) {
            roomCopies.add(new GameRoom(room));
        }
        return roomCopies;
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public void addClient(String playerId, ClientHandler handler) {
        clients.put(playerId, handler);
    }

    public void removeClient(String playerId) {
        clients.remove(playerId);
    }

    public void broadcastToRoom(String roomId, NetworkMessage message) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            System.out.println("Broadcasting to room " + room.name + " with " + room.players.size() + " players");
            for (Player player : room.players) {
                System.out.println("  Sending to player: " + player.name + " (ID: " + player.id + ")");
                ClientHandler handler = clients.get(player.id);
                if (handler != null) {
                    handler.sendMessage(message);
                } else {
                    System.out.println("  Warning: No handler found for player " + player.name);
                }
            }
        }
    }

    public String getNextWord() {
        return wordBank.getRandomWord();
    }

    // Schedule countdown for a room
    public void scheduleCountdown(String roomId) {
        countdownScheduler.scheduleAtFixedRate(() -> {
            GameRoom room = rooms.get(roomId);
            if (room != null && room.roomState == GameRoom.RoomState.COUNTDOWN) {
                boolean gameStarted = room.updateCountdown();

                if (gameStarted) {
                    // Game should start now
                    System.out.println("Game starting in room " + room.name + "!");

                    // Get first word for the game
                    String firstWord = getNextWord();
                    room.currentWord = firstWord;

                    // Notify all players game has started
                    broadcastToRoom(roomId, new NetworkMessage(NetworkMessage.MessageType.GAME_START,
                            null, roomId, firstWord));

                } else if (room.countdown > 0) {
                    // Update countdown
                    System.out.println("Room " + room.name + " countdown: " + room.countdown);
                    broadcastToRoom(roomId, new NetworkMessage(NetworkMessage.MessageType.COUNTDOWN_UPDATE,
                            null, roomId, room.countdown));
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    // Broadcast updated room list to all connected clients for real-time updates
    public void broadcastRoomListUpdate() {
        List<GameRoom> roomList = getRoomList();
        NetworkMessage roomListMessage = new NetworkMessage(NetworkMessage.MessageType.ROOM_LIST,
                null, null, roomList);

        // Send to all connected clients
        for (ClientHandler client : clients.values()) {
            try {
                client.sendMessage(roomListMessage);
            } catch (Exception e) {
                System.err.println("Error broadcasting room list to client: " + e.getMessage());
            }
        }
        System.out.println("Broadcasted room list update to " + clients.size() + " clients");
    }

    public static void main(String[] args) {
        TypingGameServer server = new TypingGameServer();
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}

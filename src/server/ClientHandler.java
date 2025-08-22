package server;

import java.io.*;
import java.net.*;
import java.util.List;
import shared.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private TypingGameServer server;
    private Player player;
    private String currentRoomId;
    private boolean isConnected;

    public ClientHandler(Socket socket, TypingGameServer server) {
        this.socket = socket;
        this.server = server;
        this.isConnected = true;

        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println("Error setting up client streams: " + e.getMessage());
            isConnected = false;
        }
    }

    @Override
    public void run() {
        try {
            while (isConnected) {
                NetworkMessage message = (NetworkMessage) input.readObject();
                handleMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected: " + (player != null ? player.name : "Unknown"));
            
            if (player != null && currentRoomId != null) {
                NetworkMessage disconnectMsg = new NetworkMessage(
                    NetworkMessage.MessageType.PLAYER_DISCONNECTED,
                    player.id,
                    currentRoomId,
                    player.name + " disconnected"
                );
                server.broadcastToRoom(currentRoomId, disconnectMsg);
            }
        } finally {
            disconnect();
        }
    }

    private void handleMessage(NetworkMessage message) {
        System.out.println(
                "Handling message type: " + message.type + " from player: " + (player != null ? player.name : "null"));
        switch (message.type) {
            case PLAYER_JOIN:
                if (message.data instanceof Player) {
                    Player playerData = (Player) message.data;
                    
                    if (player == null) {
                        player = new Player(playerData.id, playerData.name, playerData.selectedCharacterId);
                        server.addClient(player.id, this);
                        System.out.println("Registered player: " + player.name + " (ID: " + player.id + 
                                          ") with character: " + player.selectedCharacterId);
                        sendMessage(new NetworkMessage(NetworkMessage.MessageType.PLAYER_JOIN,
                                player.id, null, "Player registered successfully"));
                    } else {
                        if (!playerData.selectedCharacterId.equals(player.selectedCharacterId)) {
                            System.out.println("📡 Character update: " + player.name + " changed from " + 
                                             player.selectedCharacterId + " to " + playerData.selectedCharacterId);
                            player.selectedCharacterId = playerData.selectedCharacterId;
                            
                            if (currentRoomId != null) {
                                GameRoom room = server.getRoom(currentRoomId);
                                if (room != null) {
                                    Player roomPlayer = room.getPlayer(player.id);
                                    if (roomPlayer != null) {
                                        roomPlayer.selectedCharacterId = playerData.selectedCharacterId;
                                        
                                        server.broadcastToRoom(currentRoomId,
                                                new NetworkMessage(NetworkMessage.MessageType.PLAYER_JOIN,
                                                        player.id, currentRoomId, roomPlayer));
                                        
                                        server.broadcastToRoom(currentRoomId,
                                                new NetworkMessage(NetworkMessage.MessageType.ROOM_UPDATE,
                                                        player.id, currentRoomId, new GameRoom(room)));
                                    }
                                }
                            }
                        }
                    }
                }
                break;
                
            case JOIN_ROOM:
                if (player == null) {
                    player = (Player) message.data;
                    server.addClient(player.id, this);
                }

                System.out.println("Player " + player.name + " attempting to join room " + message.roomId);

                if (server.joinRoom(message.roomId, player)) {
                    currentRoomId = message.roomId;
                    GameRoom joinedRoom = server.getRoom(message.roomId);
                    System.out.println("Player " + player.name + " successfully joined room. Room now has "
                            + joinedRoom.players.size() + " players");
                    sendMessage(new NetworkMessage(NetworkMessage.MessageType.JOIN_ROOM,
                            player.id, currentRoomId, new GameRoom(joinedRoom)));

                    server.broadcastToRoom(currentRoomId,
                            new NetworkMessage(NetworkMessage.MessageType.PLAYER_JOIN,
                                    player.id, currentRoomId, player));

                    server.broadcastToRoom(currentRoomId,
                            new NetworkMessage(NetworkMessage.MessageType.ROOM_UPDATE,
                                    player.id, currentRoomId, new GameRoom(joinedRoom)));
                } else {
                    System.out.println("Failed to join room " + message.roomId + " for player " + player.name);
                    sendMessage(new NetworkMessage(NetworkMessage.MessageType.PLAYER_JOIN,
                            player.id, message.roomId, "Room full or started"));
                }
                break;

            case CREATE_ROOM:
                System.out.println("Received CREATE_ROOM request");
                if (message.data instanceof String) {
                    String roomName = (String) message.data;
                    if (player == null) {
                        System.err.println("WARNING: CREATE_ROOM without registered player! Using fallback...");
                        String playerName = "Player_" + message.playerId.substring(0, 6);
                        player = new Player(message.playerId, playerName, "medieval_king");
                        server.addClient(player.id, this);
                        System.out.println("Created fallback player: " + playerName + " (ID: " + player.id + ")");
                    }

                    System.out.println("Creating room: " + roomName + " for player: " + player.name + 
                                     " with character: " + player.selectedCharacterId);
                    String roomId = server.createRoom(roomName, player);
                    currentRoomId = roomId;
                    
                    System.out.println("Room created/joined successfully: " + roomId);
                    sendMessage(new NetworkMessage(NetworkMessage.MessageType.CREATE_ROOM,
                            player.id, roomId, roomId));
                    
                    GameRoom room = server.getRoom(roomId);
                    if (room != null && room.isFull()) {
                        System.out.println("Room is now full! Starting countdown...");
                        

                        room.roomState = GameRoom.RoomState.COUNTDOWN;
                        room.countdown = 10;
                        room.countdownStartTime = System.currentTimeMillis();
                        

                        room.startCountdown();
                        
                        for (Player roomPlayer : room.players) {
                            server.broadcastToRoom(roomId,
                                new NetworkMessage(NetworkMessage.MessageType.PLAYER_JOIN,
                                    null, roomId, roomPlayer));
                        }
                        

                        server.broadcastToRoom(roomId, new NetworkMessage(
                            NetworkMessage.MessageType.COUNTDOWN_START,
                            null, roomId, room.countdown));
                        
                        server.scheduleCountdown(roomId);
                    }

                } else if (message.data instanceof GameRoom) {
                    GameRoom roomToCreate = (GameRoom) message.data;
                    if (player == null) {
                        player = roomToCreate.host;
                        server.addClient(player.id, this);
                    }

                    System.out.println("Creating room: " + roomToCreate.name + " for player: " + player.name);
                    String roomId = server.createRoom(roomToCreate.name, player);
                    currentRoomId = roomId;
                    GameRoom createdRoom = server.getRoom(roomId);

                    System.out.println("Room created successfully, sending response");
                    sendMessage(new NetworkMessage(NetworkMessage.MessageType.CREATE_ROOM,
                            player.id, roomId, new GameRoom(createdRoom)));

                    server.broadcastRoomListUpdate();
                } else {
                    System.err.println("Invalid CREATE_ROOM data type: "
                            + (message.data != null ? message.data.getClass() : "null"));
                }
                break;

            case LEAVE_ROOM:
                if (currentRoomId != null) {
                    server.leaveRoom(currentRoomId, player.id);
                    server.broadcastToRoom(currentRoomId,
                            new NetworkMessage(NetworkMessage.MessageType.PLAYER_LEAVE,
                                    player.id, currentRoomId, player));
                    currentRoomId = null;
                }
                break;

            case START_GAME:
                if (currentRoomId != null) {
                    GameRoom room = server.getRoom(currentRoomId);
                    if (room != null && room.host.id.equals(player.id) && room.players.size() >= 2) {
                        if (room.roomState == GameRoom.RoomState.WAITING_FOR_HOST ||
                                room.roomState == GameRoom.RoomState.WAITING_FOR_PLAYERS) {
                            room.roomState = GameRoom.RoomState.COUNTDOWN;
                            room.countdown = 3;
                            room.countdownStartTime = System.currentTimeMillis();

                            server.broadcastToRoom(currentRoomId,
                                    new NetworkMessage(NetworkMessage.MessageType.COUNTDOWN_START,
                                            player.id, currentRoomId, room.countdown));

                            server.scheduleCountdown(currentRoomId);
                        }
                    }
                }
                break;

            case PLAYER_TYPED:
                if (currentRoomId != null) {
                    if (message.data instanceof String && "WORD_COMPLETE".equals(message.data)) {
                        GameRoom room = server.getRoom(currentRoomId);
                        if (room != null) {
                            System.out.println("=== WORD COMPLETION ATTACK ===");
                            System.out.println("Attacker: " + player.name + " (ID: " + player.id + ") with character: " + player.selectedCharacterId);
                            
                            System.out.println("BEFORE ATTACK - Room players:");
                            for (Player p : room.players) {
                                System.out.println("  Player: " + p.name + " (ID: " + p.id + ") Health: " + p.health + 
                                                 " Character: " + p.selectedCharacterId + " Words: " + p.wordsCompleted);
                            }
                            
                            Player completingPlayer = room.getPlayer(player.id);
                            if (completingPlayer != null) {
                                completingPlayer.wordsCompleted++;
                                System.out.println("Updated attacker score to: " + completingPlayer.wordsCompleted);
                            }
                            
                            for (Player p : room.players) {
                                if (!p.id.equals(player.id)) {
                                    int oldHealth = p.health;
                                    p.health = Math.max(0, p.health - 1);
                                    System.out.println("DAMAGE APPLIED: Player " + p.name + " health: " + oldHealth + " -> " + p.health);
                                }
                            }
                            
                            System.out.println("AFTER ATTACK - Room players:");
                            for (Player p : room.players) {
                                System.out.println("  Player: " + p.name + " (ID: " + p.id + ") Health: " + p.health + 
                                                 " Character: " + p.selectedCharacterId + " Words: " + p.wordsCompleted);
                            }
                            
                            String oldWord = room.currentWord;
                            String newWord = server.getNextWord(room.currentWord);
                            room.currentWord = newWord;
                            System.out.println("Word synchronized for all players: " + oldWord + " -> " + newWord);
                            
                            System.out.println("1. Broadcasting PLAYER_TYPED attack message...");
                            server.broadcastToRoom(currentRoomId,
                                    new NetworkMessage(NetworkMessage.MessageType.PLAYER_TYPED,
                                            player.id, currentRoomId, "WORD_COMPLETE"));
                            
                            System.out.println("2. Broadcasting GAME_STATE_UPDATE with new word: " + newWord);
                            server.broadcastToRoom(currentRoomId,
                                    new NetworkMessage(NetworkMessage.MessageType.GAME_STATE_UPDATE,
                                            null, currentRoomId, newWord));
                            
                            System.out.println("3. Broadcasting ROOM_UPDATE with health and scores...");
                            GameRoom roomCopy = new GameRoom(room);
                            server.broadcastToRoom(currentRoomId,
                                    new NetworkMessage(NetworkMessage.MessageType.ROOM_UPDATE,
                                            null, currentRoomId, roomCopy));
                            
                            System.out.println("=== ATTACK SEQUENCE COMPLETE ===");
                        }
                    } else {
                        TypingEvent event = (TypingEvent) message.data;
                        GameRoom room = server.getRoom(currentRoomId);

                        if (room != null && event.isComplete) {
                            Player roomPlayer = room.getPlayer(player.id);
                            if (roomPlayer != null) {
                                roomPlayer.wordsCompleted++;
                                roomPlayer.wpm = event.wpm;

                                for (Player p : room.players) {
                                    if (!p.id.equals(player.id)) {
                                        p.health = Math.max(0, p.health - 1);
                                        System.out.println("Legacy: Player " + p.name + " health reduced to: " + p.health);
                                    }
                                }

                                String newWord = server.getNextWord(room.currentWord);
                                room.currentWord = newWord;
                                
                                server.broadcastToRoom(currentRoomId,
                                        new NetworkMessage(NetworkMessage.MessageType.PLAYER_TYPED,
                                                player.id, currentRoomId, "WORD_COMPLETE"));
                                
                                server.broadcastToRoom(currentRoomId,
                                        new NetworkMessage(NetworkMessage.MessageType.GAME_STATE_UPDATE,
                                                null, currentRoomId, newWord));
                                
                                server.broadcastToRoom(currentRoomId,
                                        new NetworkMessage(NetworkMessage.MessageType.ROOM_UPDATE,
                                                null, currentRoomId, new GameRoom(room)));
                            }
                        }
                    }
                }
                break;

            case ROOM_LIST:
                System.out.println(
                        "Received ROOM_LIST request from player: " + (player != null ? player.name : "unknown"));
                List<GameRoom> roomList = server.getRoomList();
                System.out.println("Sending " + roomList.size() + " rooms to client");
                sendMessage(new NetworkMessage(NetworkMessage.MessageType.ROOM_LIST,
                        player != null ? player.id : "unknown",
                        null, roomList));
                break;

            case PLAYER_PROGRESS:
                if (currentRoomId != null && message.data instanceof Integer) {
                    server.broadcastToRoom(currentRoomId,
                            new NetworkMessage(NetworkMessage.MessageType.PLAYER_PROGRESS,
                                    player.id, currentRoomId, message.data));
                }
                break;

            default:
                break;
        }
    }

    public void sendMessage(NetworkMessage message) {
        try {
            if (output != null && isConnected) {
                output.writeObject(message);
                output.flush();
            }
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        if (player != null && currentRoomId != null) {
            server.leaveRoom(currentRoomId, player.id);
            server.broadcastToRoom(currentRoomId,
                    new NetworkMessage(NetworkMessage.MessageType.PLAYER_LEAVE,
                            player.id, currentRoomId, player));
        }

        if (player != null) {
            server.removeClient(player.id);
        }

        isConnected = false;
        try {
            if (socket != null)
                socket.close();
            if (input != null)
                input.close();
            if (output != null)
                output.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}
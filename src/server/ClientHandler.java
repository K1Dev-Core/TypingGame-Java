package server;

import shared.*;
import java.io.*;
import java.net.*;
import java.util.List;

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
        } finally {
            disconnect();
        }
    }

    private void handleMessage(NetworkMessage message) {
        System.out.println(
                "Handling message type: " + message.type + " from player: " + (player != null ? player.name : "null"));
        switch (message.type) {
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
                    // Send fresh copy of the room
                    sendMessage(new NetworkMessage(NetworkMessage.MessageType.JOIN_ROOM,
                            player.id, currentRoomId, new GameRoom(joinedRoom)));

                    // Notify all players in the room about the new player
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
                if (message.data instanceof GameRoom) {
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

                    // Broadcast new room to all connected clients for real-time updates
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
                        // Check if room state allows starting
                        if (room.roomState == GameRoom.RoomState.WAITING_FOR_HOST ||
                                room.roomState == GameRoom.RoomState.WAITING_FOR_PLAYERS) {
                            room.roomState = GameRoom.RoomState.COUNTDOWN;
                            room.countdown = 3;
                            room.countdownStartTime = System.currentTimeMillis();

                            // Notify all players that countdown has started
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
                    TypingEvent event = (TypingEvent) message.data;
                    GameRoom room = server.getRoom(currentRoomId);

                    if (room != null && event.isComplete) {
                        Player roomPlayer = room.getPlayer(player.id);
                        if (roomPlayer != null) {
                            roomPlayer.wordsCompleted++;
                            roomPlayer.wpm = event.wpm;

                            for (Player p : room.players) {
                                if (!p.id.equals(player.id)) {
                                    p.health--;
                                    if (p.health <= 0) {
                                        p.isAlive = false;
                                    }
                                }
                            }

                            room.currentWord = server.getNextWord();
                        }

                        server.broadcastToRoom(currentRoomId,
                                new NetworkMessage(NetworkMessage.MessageType.GAME_STATE_UPDATE,
                                        player.id, currentRoomId, room));
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
                // Broadcast player progress to other players in the room
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

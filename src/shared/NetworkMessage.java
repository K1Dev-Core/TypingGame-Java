package shared;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    public enum MessageType {
        JOIN_ROOM, CREATE_ROOM, LEAVE_ROOM, START_GAME, PLAYER_TYPED, GAME_STATE_UPDATE,
        ROOM_LIST, PLAYER_JOIN, PLAYER_LEAVE, GAME_OVER, HEARTBEAT, ROOM_UPDATE,
        COUNTDOWN_START, COUNTDOWN_UPDATE, GAME_START, PLAYER_PROGRESS
    }

    public MessageType type;
    public String playerId;
    public String roomId;
    public Object data;

    public NetworkMessage(MessageType type, String playerId, String roomId, Object data) {
        this.type = type;
        this.playerId = playerId;
        this.roomId = roomId;
        this.data = data;
    }
}

package shared;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        JOIN_ROOM, CREATE_ROOM, LEAVE_ROOM, START_GAME, PLAYER_TYPED, GAME_STATE_UPDATE,
        ROOM_LIST, PLAYER_JOIN, PLAYER_LEAVE, GAME_OVER, HEARTBEAT, ROOM_UPDATE,
        COUNTDOWN_START, COUNTDOWN_UPDATE, GAME_START, PLAYER_PROGRESS, PLAYER_DISCONNECTED
    }

    public MessageType type;
    public String playerId;
    public String roomId;
    public Object data;

    public NetworkMessage(MessageType type, String playerId, String roomId, Object data) {
        this.type = type;
        this.playerId = playerId != null ? playerId : "";
        this.roomId = roomId != null ? roomId : "";
        this.data = data;
    }
    
    @Override
    public String toString() {
        return String.format("NetworkMessage{type=%s, playerId='%s', roomId='%s', data=%s}", 
                           type, playerId, roomId, data != null ? data.getClass().getSimpleName() : "null");
    }
}

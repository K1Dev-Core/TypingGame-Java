package shared;

import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public String id;
    public String name;
    public int health;
    public int wordsCompleted;
    public double wpm;
    public boolean isReady;
    public boolean isAlive;
    public String selectedCharacterId;

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.health = 5;
        this.wordsCompleted = 0;
        this.wpm = 0.0;
        this.isReady = false;
        this.isAlive = true;
        this.selectedCharacterId = "medieval_king";
    }
    
    public Player(String id, String name, String selectedCharacterId) {
        this(id, name);
        this.selectedCharacterId = selectedCharacterId != null && !selectedCharacterId.trim().isEmpty() 
                                 ? selectedCharacterId : "medieval_king";
    }
}

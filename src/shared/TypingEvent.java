package shared;

import java.io.Serializable;

public class TypingEvent implements Serializable {
    public String word;
    public String typedText;
    public boolean isComplete;
    public long timestamp;
    public double wpm;

    public TypingEvent(String word, String typedText, boolean isComplete, double wpm) {
        this.word = word;
        this.typedText = typedText;
        this.isComplete = isComplete;
        this.wpm = wpm;
        this.timestamp = System.currentTimeMillis();
    }
}

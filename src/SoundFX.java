import javax.sound.sampled.*;
import java.io.File;

public class SoundFX {
    private final String path;
    
    public SoundFX(String wavPath) {
        this.path = wavPath;
    }
    
    public void type() {
        try {
            File f = new File(path);
            if (!f.exists()) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.start();
        } catch (Exception ignored) {}
    }
}
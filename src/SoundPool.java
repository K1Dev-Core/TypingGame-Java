import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayDeque;

public class SoundPool {
    private final ArrayDeque<Clip> pool = new ArrayDeque<>();

    public SoundPool(String wav, int size) {
        try {
            for (int i = 0; i < size; i++) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(new File(wav));
                Clip c = AudioSystem.getClip();
                c.open(ais);
                pool.add(c);
            }
        } catch (Exception ignored) {
        }
    }

    public void play() {
        Clip c = pool.pollFirst();
        if (c == null)
            return;
        c.setFramePosition(0);
        c.start();
        pool.addLast(c);
    }
}

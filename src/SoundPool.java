import java.io.File;
import java.util.ArrayDeque;
import javax.sound.sampled.*;

public class SoundPool implements AudioComponent {
    private final ArrayDeque<Clip> pool = new ArrayDeque<>();
    private volatile float volume = 1.0f;

    public SoundPool(String wav, int size) {
        try {
            for (int i = 0; i < size; i++) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(new File(wav));
                Clip c = AudioSystem.getClip();
                c.open(ais);
                AudioManager.applyVolumeToClip(c, volume);
                pool.add(c);
            }
        } catch (Exception ignored) { }
    }

    public void play() {
        Clip c = pool.pollFirst();
        if (c == null) return;
        try {
            if (c.isRunning()) c.stop();
            c.setFramePosition(0);
            c.start();
        } catch (Exception ignored) { }
        pool.addLast(c);
    }

    public void setVolume(float v) {
        float nv = Math.max(0f, Math.min(1f, v));
        volume = nv;
        for (Clip c : pool) AudioManager.applyVolumeToClip(c, nv);
    }



    public void stop() {
        for (Clip c : pool) {
            try { c.stop(); } catch (Exception ignored) { }
        }
    }

    public void close() {
        for (Clip c : pool) {
            try { c.stop(); c.close(); } catch (Exception ignored) { }
        }
        pool.clear();
    }
}

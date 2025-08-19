import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayDeque;

public class SoundPool {
    private final ArrayDeque<Clip> pool = new ArrayDeque<>();
    private volatile float volume = 1.0f;

    public SoundPool(String wav, int size) {
        try {
            for (int i = 0; i < size; i++) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(new File(wav));
                Clip c = AudioSystem.getClip();
                c.open(ais);
                applyVolumeToClip(c, volume);
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
        for (Clip c : pool) applyVolumeToClip(c, nv);
    }

    private void applyVolumeToClip(Clip c, float v) {
        try {
            if (c.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
                float min = gain.getMinimum();
                float max = gain.getMaximum();
                float dB = (v <= 0f) ? min : (float) (20.0 * Math.log10(v));
                dB = Math.max(min, Math.min(max, dB));
                gain.setValue(dB);
                return;
            }
        } catch (Exception ignored) { }
        try {
            if (c.isControlSupported(FloatControl.Type.VOLUME)) {
                FloatControl vol = (FloatControl) c.getControl(FloatControl.Type.VOLUME);
                float min = vol.getMinimum();
                float max = vol.getMaximum();
                float val = Math.max(min, Math.min(max, v));
                vol.setValue(val);
            }
        } catch (Exception ignored) { }
    }

    public void close() {
        for (Clip c : pool) {
            try { c.stop(); c.close(); } catch (Exception ignored) { }
        }
        pool.clear();
    }
}

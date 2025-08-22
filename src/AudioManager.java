import javax.sound.sampled.*;

public class AudioManager {
    public static void applyVolumeToClip(Clip clip, float volume) {
        if (clip == null) return;
        
        float normalizedVolume = Math.max(0f, Math.min(1f, volume));
        
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float min = gain.getMinimum();
                float max = gain.getMaximum();
                float dB = (normalizedVolume <= 0f) ? min : (float) (20.0 * Math.log10(normalizedVolume));
                dB = Math.max(min, Math.min(max, dB));
                gain.setValue(dB);
                return;
            }
        } catch (Exception ignored) { }
        
        try {
            if (clip.isControlSupported(FloatControl.Type.VOLUME)) {
                FloatControl vol = (FloatControl) clip.getControl(FloatControl.Type.VOLUME);
                float min = vol.getMinimum();
                float max = vol.getMaximum();
                float val = Math.max(min, Math.min(max, normalizedVolume));
                vol.setValue(val);
            }
        } catch (Exception ignored) { }
    }
}
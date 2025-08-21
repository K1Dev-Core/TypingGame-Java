import javax.sound.sampled.*;
import java.io.File;

public class BackgroundMusic {
    private Clip clip;
    private float volume = 1.0f;
    private boolean isPlaying = false;
    private String currentMusic;

    public BackgroundMusic(String musicFile) {
        loadMusic(musicFile);
    }

    public void loadMusic(String musicFile) {
        stop();

        try {
            currentMusic = musicFile;
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(musicFile));
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);

            clip.loop(Clip.LOOP_CONTINUOUSLY);
            applyVolume();
        } catch (Exception e) {
            System.err.println("Error loading background music: " + e.getMessage());
        }
    }

    public void play() {
        if (clip != null && !isPlaying) {
            clip.setFramePosition(0);
            clip.start();
            isPlaying = true;
        }
    }

    public void stop() {
        if (clip != null && isPlaying) {
            clip.stop();
            isPlaying = false;
        }
    }

    public void pause() {
        if (clip != null && isPlaying) {
            clip.stop();
            isPlaying = false;
        }
    }

    public void resume() {
        if (clip != null && !isPlaying) {
            clip.start();
            isPlaying = true;
        }
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        applyVolume();
    }

    private void applyVolume() {
        if (clip == null)
            return;

        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float min = gainControl.getMinimum();
                float max = gainControl.getMaximum();
                float dB = (volume <= 0f) ? min : (float) (20.0 * Math.log10(volume));
                dB = Math.max(min, Math.min(max, dB));
                gainControl.setValue(dB);
                return;
            }
        } catch (Exception ignored) {
        }

        try {
            if (clip.isControlSupported(FloatControl.Type.VOLUME)) {
                FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.VOLUME);
                float min = volumeControl.getMinimum();
                float max = volumeControl.getMaximum();
                float val = min + volume * (max - min);
                volumeControl.setValue(val);
            }
        } catch (Exception ignored) {
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public String getCurrentMusic() {
        return currentMusic;
    }

    public void close() {
        if (clip != null) {
            stop();
            clip.close();
        }
    }
}

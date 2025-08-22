import java.io.File;
import javax.sound.sampled.*;

public class BackgroundMusic implements AudioComponent {
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
            AudioManager.applyVolumeToClip(clip, volume);
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
        if (clip != null) {
            AudioManager.applyVolumeToClip(clip, this.volume);
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

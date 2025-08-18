import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class SlowTTS {
    private final Voice voice;

    public SlowTTS(String voiceName, float rate) {
        System.setProperty("freetts.voices",
                "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");

        this.voice = VoiceManager.getInstance().getVoice(voiceName);

        if (this.voice == null) {
            throw new IllegalStateException("?: " + voiceName);
        }
        this.voice.allocate();
        this.voice.setRate(rate);
    }


    public void speak(String text) {
        if (voice != null) {
            voice.speak(text);
        }
    }


    public void close() {
        if (voice != null) {
            voice.deallocate();
        }
    }


}

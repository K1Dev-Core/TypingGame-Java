public class GameConfig {
    public static final String ASSETS_DIR = "./res/keys";
    public static final String FILE_EXT = ".png";
    public static final boolean TWO_FRAMES_PER_FILE = true;
    public static final int SCALE = 4;
    public static final int KEY_SPACING = 10;
    public static final int ROUND_SECONDS = 60;
    public static final String CHAR_SET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String WORDS_TXT_PATH = "./res/words.txt";
    public static final String CLICK_WAV_PATH = "./res/wav/click3_1.wav";
    public static final int MAX_HEALTH = 10;
    public static final int DEBOUNCE_MS = 35;
    
    public static final int GAME_TIMER_DELAY = 16;
    public static final int HIT_EFFECT_TIMER_DELAY = 25;
    public static final int COMBO_DISPLAY_DURATION = 2000;
    public static final int COMBO_FADE_DURATION = 500;
    public static final int SPACE_ANIM_MS = 400;

    public enum State {
        READY, PLAYING, GAMEOVER
    }
}
import java.util.Random;
import java.util.List;

public class GameState {
    public final Random rng = new Random();
    public final int CHAR_SCALE = 3;
    public final int ATTACK_HOLD_MS = 600;
    public final int TAKE_HIT_HOLD_MS = 700;
    public final int BONUS_TIME_MS = 3000;

    public KeyAtlas atlas;
    public SoundPool sStart;
    public SoundPool sType;
    public SoundPool sErr;
    public SoundPool sClick;
    public SoundPool sSlash;
    public SoundPool sHit;
    public SoundPool sDeath;
    public BackgroundMusic bgMusic;
    public HitEffect hit;
    public List<WordEntry> wordBank;

    public CharacterPack player;
    public CharacterPack bot;

    public int groundY;
    public int playerBaseX;
    public int botBaseX;

    public GameConfig.State state = GameConfig.State.READY;
    public GameConfig.State previousState = GameConfig.State.READY;

    public WordEntry current = new WordEntry("HELLO", "HELLO", "เฮลโล", "สวัสดี");
    public int idx = 0;
    public int mistakes = 0;
    public int correct = 0;
    public int totalTyped = 0;
    public int wordsCompleted = 0;
    public long startMs = 0;
    public long wordStartMs = 0;

    public long flashUntil = 0;
    public long shakeUntil = 0;
    public int shakeAmp = 0;
    public long popUntil = 0;
    public long bonusUntil = 0;

    public final long[] lastPressAt = new long[256];

    public int health = GameConfig.MAX_HEALTH;
    public int bonusStreak = 0;

    public boolean isSpaceHeld = false;
    public long spaceAnimLast = 0;
    public boolean spaceAnimOn = false;
    public static final int SPACE_ANIM_MS = 400;

    public long lastFpsTime = 0;
    public int frames = 0;
    public int fps = 0;

    public boolean pendingNextWord = false;

    public boolean botSeq = false;
    public int botPhase = 0;
    public long botPhaseUntil = 0;

    public boolean playerSeq = false;
    public int playerPhase = 0;
    public long playerPhaseUntil = 0;

    public boolean playerTakingHit = false;
    public long playerHitUntil = 0;
    public boolean botTakingHit = false;
    public long botHitUntil = 0;

    private UISettings uiSettings;

    public GameState() {
        initSoundPools();
        loadWordBank();
        initHitEffect();
        initBackgroundMusic();
    }

    public void setUISettings(UISettings uiSettings) {
        this.uiSettings = uiSettings;
    }

    private void initSoundPools() {
        sStart = new SoundPool("./res/wav/click6_1.wav", 4);
        sType = new SoundPool(GameConfig.CLICK_WAV_PATH, 6);
        sErr = new SoundPool("./res/wav/click14_3.wav", 4);
        sClick = new SoundPool("./res/wav/click15_1.wav", 6);
        sSlash = new SoundPool("./res/wav/slash1.wav", 4);
        sHit = new SoundPool("./res/wav/villager.wav", 4);
        sDeath = new SoundPool("./res/wav/classic_hurt.wav", 2);
    }

    private void initBackgroundMusic() {
        try {
            bgMusic = new BackgroundMusic("./res/wav/bg.wav");

            if (uiSettings != null) {
                float musicVol = (uiSettings.musicVolume / 100f) * (uiSettings.masterVolume / 100f);
                bgMusic.setVolume(musicVol);
            } else {
                bgMusic.setVolume(0.02f);
            }

            bgMusic.play();
        } catch (Exception e) {
            System.err.println("Failed to initialize background music: " + e.getMessage());
        }
    }

    private void loadWordBank() {
        wordBank = WordBank.loadWordBank();
    }

    private void initHitEffect() {
        hit = new HitEffect("./res/effect/hit-sprite-sheet.png");
    }

    public void initAtlas() {
        atlas = new KeyAtlas(
                GameConfig.ASSETS_DIR,
                GameConfig.FILE_EXT,
                GameConfig.TWO_FRAMES_PER_FILE,
                GameConfig.CHAR_SET);
    }

    public void warmupAtlas() {
        try {
            String set = GameConfig.CHAR_SET == null ? "" : GameConfig.CHAR_SET;
            for (char c : set.toCharArray()) {
                atlas.getNormal(c);
                atlas.getPressed(c);
            }
            for (char c = 'A'; c <= 'Z'; c++) {
                atlas.getNormal(c);
                atlas.getPressed(c);
            }
            for (char c = '0'; c <= '9'; c++) {
                atlas.getNormal(c);
                atlas.getPressed(c);
            }
            atlas.getNormal(' ');
            atlas.getPressed(' ');
        } catch (Exception ignored) {
        }
    }

    public void updateGroundAndBases(int panelWidth, int panelHeight) {
        groundY = panelHeight - 60;
        computeBasePositions(panelWidth, panelHeight);
        try {
            player.y = groundY;
            player.x = playerBaseX;
        } catch (Throwable ignored) {
        }

        try {
            bot.y = groundY;
            bot.x = botBaseX;
        } catch (Throwable ignored) {
        }
    }

    private void computeBasePositions(int panelWidth, int panelHeight) {
        int leftMargin = 20;
        int rightMargin = 40;
        int gapFromWord = 48;
        int cx = panelWidth / 2;
        int keyW = atlas.keySize().width * GameConfig.SCALE;
        int keyH = atlas.keySize().height * GameConfig.SCALE;
        int totalW;

        if (current != null && current.word != null && current.word.length() > 0) {
            totalW = current.word.length() * keyW + (current.word.length() - 1) * GameConfig.KEY_SPACING;
        } else {
            totalW = 5 * keyW + 4 * GameConfig.KEY_SPACING;
        }

        int wordLeft = cx - totalW / 2;
        int wordRight = cx + totalW / 2;
        playerBaseX = Math.max(leftMargin, wordLeft - gapFromWord);
        botBaseX = Math.max(playerBaseX + 200, Math.min(panelWidth - rightMargin - keyH, wordRight + 12));
        botBaseX = Math.min(botBaseX, panelWidth - rightMargin - 120);
        int biasLeft = 300;
        playerBaseX = Math.max(20, playerBaseX - biasLeft);
        botBaseX = Math.max(playerBaseX + 220, botBaseX - 80);
    }

    public void nextWord() {
        if (wordBank.isEmpty()) {
            current = new WordEntry("HELLO", "HELLO", "เฮลโล", "สวัสดี");
        } else {
            WordEntry newWord;
            do {
                newWord = wordBank.get(rng.nextInt(wordBank.size()));
            } while (wordBank.size() > 1 && newWord.word.equals(current.word));
            current = newWord;
        }

        idx = 0;
        wordStartMs = System.currentTimeMillis();

        if (uiSettings != null) {
            uiSettings.speakCurrentWord(current);
        }
    }

    public void resetRun(AnimationController animController) {
        mistakes = correct = totalTyped = wordsCompleted = bonusStreak = 0;
        health = GameConfig.MAX_HEALTH;
        pendingNextWord = false;
        botSeq = false;
        playerSeq = false;
        playerTakingHit = false;
        botTakingHit = false;
        playerHitUntil = botHitUntil = 0;

        if (animController != null) {
            player.x = playerBaseX;
            player.y = groundY;
            bot.x = botBaseX;
            bot.y = groundY;
            animController.setAnim(player, CharacterPack.Anim.IDLE);
            animController.setAnim(bot, CharacterPack.Anim.IDLE);
        }

        nextWord();
        startMs = System.currentTimeMillis();
        state = GameConfig.State.PLAYING;
    }

    public void checkBonus() {
        long elapsed = System.currentTimeMillis() - wordStartMs;
        if (elapsed <= BONUS_TIME_MS) {
            bonusStreak++;
            bonusUntil = System.currentTimeMillis() + 1500;
            if (bonusStreak % 3 == 0) {
                health = Math.min(GameConfig.MAX_HEALTH, health + 1);
                popUntil = System.currentTimeMillis() + 200;
            }
        } else {
            bonusStreak = 0;
        }
    }

    public void startBotAttackSequence() {
        if (botSeq || state != GameConfig.State.PLAYING)
            return;
        botSeq = true;
        botPhase = 0;
        botPhaseUntil = 0;
    }

    public void startPlayerAttackSequence() {
        if (playerSeq || state != GameConfig.State.PLAYING) {
            pendingNextWord = true;
            return;
        }
        playerSeq = true;
        playerPhase = 0;
        playerPhaseUntil = 0;
    }

    public void updateBotSequence(long now, AnimationController animController) {
        if (!botSeq)
            return;
        if (botPhase == 0) {
            bot.x = Math.max(playerBaseX + 64, player.x + 64);
            bot.y = groundY;
            animController.setAnim(bot, CharacterPack.Anim.ATTACK);
            animController.setAnim(player, CharacterPack.Anim.TAKE_HIT);
            animController.playIfAudible(sHit);
            playerTakingHit = true;
            playerHitUntil = now + TAKE_HIT_HOLD_MS;
            botPhaseUntil = now + ATTACK_HOLD_MS;
            botPhase = 1;
        } else if (botPhase == 1) {
            if (now >= botPhaseUntil) {
                bot.x = botBaseX;
                bot.y = groundY;
                animController.setAnim(bot, CharacterPack.Anim.IDLE);
                botSeq = false;
            }
        }
    }

    public void updatePlayerSequence(long now, AnimationController animController) {
        if (!playerSeq)
            return;
        if (playerPhase == 0) {
            player.x = Math.min(botBaseX - 64, bot.x - 64);
            player.y = groundY;
            animController.setAnim(player, CharacterPack.Anim.ATTACK);
            animController.setAnim(bot, CharacterPack.Anim.TAKE_HIT);
            animController.playIfAudible(sHit);
            botTakingHit = true;
            botHitUntil = now + TAKE_HIT_HOLD_MS;
            playerPhaseUntil = now + ATTACK_HOLD_MS;
            playerPhase = 1;
        } else if (playerPhase == 1) {
            if (now >= playerPhaseUntil) {
                player.x = playerBaseX;
                player.y = groundY;
                animController.setAnim(player, CharacterPack.Anim.IDLE);
                playerSeq = false;
                if (pendingNextWord) {
                    pendingNextWord = false;
                    nextWord();
                } else {
                    nextWord();
                }
            }
        }
    }

    public void handleChar(char c, AnimationController animController) {
        totalTyped++;
        if (idx < current.word.length() && c == current.word.charAt(idx)) {
            animController.playIfAudible(sType);
            idx++;
            correct++;
            popUntil = System.currentTimeMillis() + 90;
            if (idx >= current.word.length()) {
                wordsCompleted++;
                startPlayerAttackSequence();
                checkBonus();
            }
        } else {
            mistakes++;
            bonusStreak = 0;
            animController.playIfAudible(sErr);
            long now = System.currentTimeMillis();
            flashUntil = now + 120;
            shakeUntil = now + 220;
            shakeAmp = 2;
            health = Math.max(0, health - 1);
            startBotAttackSequence();
            if (health <= 0) {
                state = GameConfig.State.GAMEOVER;
                current = new WordEntry("OVER", "OVER", "โอเวอร์", "จบ");
                idx = 0;
                animController.setAnim(player, CharacterPack.Anim.DEATH);
                animController.playIfAudible(sDeath);
                animController.setAnim(bot, CharacterPack.Anim.IDLE);

                // Save the score when game ends
                ScoreManager.getInstance().saveScore(wordsCompleted);
            }
        }
    }

    public void update(long now, AnimationController animController) {
        if (state == GameConfig.State.PLAYING) {
            // Time limit removed - game continues until player loses all health
        }

        if (state == GameConfig.State.READY || state == GameConfig.State.GAMEOVER) {
            if (now - spaceAnimLast >= SPACE_ANIM_MS) {
                spaceAnimOn = !spaceAnimOn;
                spaceAnimLast = now;
            }
        } else {
            spaceAnimOn = false;
        }

        if (state == GameConfig.State.PLAYING) {
            updateBotSequence(now, animController);
            updatePlayerSequence(now, animController);
        }

        if (playerTakingHit && now >= playerHitUntil) {
            animController.setAnim(player, CharacterPack.Anim.IDLE);
            playerTakingHit = false;
        }

        if (botTakingHit && now >= botHitUntil) {
            animController.setAnim(bot, CharacterPack.Anim.IDLE);
            botTakingHit = false;
        }

        frames++;
        if (lastFpsTime == 0)
            lastFpsTime = now;
        if (now - lastFpsTime >= 1000) {
            fps = frames;
            frames = 0;
            lastFpsTime = now;
        }
    }

}

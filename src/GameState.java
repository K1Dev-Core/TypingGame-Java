import java.util.Random;
import java.util.List;

public class GameState {

    public final Random rng = new Random();
    public final int CHAR_SCALE = 3;
    public final int ATTACK_HOLD_MS = 600;
    public final int TAKE_HIT_HOLD_MS = 700;
    public final int BONUS_TIME_MS = 3000;

    public ComboEffect comboEffect = new ComboEffect();

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
    public CharacterPack opponent;

    private boolean isMultiplayerMode = false;
    private String currentWord = "";
    private String playerName = "";
    private boolean showOnlineUI = false;
    private String statusMessage = "";

    // Multiplayer opponent data
    private String opponentName = "";
    private int opponentIdx = 0;
    private CharacterPack opponentCharacterPack;

    public int groundY;
    public int playerBaseX;
    public int botBaseX;

    public int panelWidth;
    public int panelHeight;

    public GameConfig.State state = GameConfig.State.READY;
    public GameConfig.State previousState = GameConfig.State.READY;

    public WordEntry current = new WordEntry("HELLO", "HELLO", "เฮลโล", "สวัสดี");
    public int playerIdx = 0;
    public int mistakes = 0;
    public int wordsCompleted = 0;
    public long startMs = 0;

    public long flashUntil = 0;
    public long shakeUntil = 0;
    public int shakeAmp = 0;
    public long popUntil = 0;

    public final long[] lastPressAt = new long[256];

    public int playerHealth = GameConfig.MAX_HEALTH;
    public int botHealth = GameConfig.MAX_HEALTH;

    public int comboCount = 0;

    public double botTypeSpeedMin = 1000;
    public double botTypeSpeedMax = 2000;

    public boolean isSpaceHeld = false;
    public long spaceAnimLast = 0;
    public boolean spaceAnimOn = false;
    public static final int SPACE_ANIM_MS = 400;

    public long lastFpsTime = 0;
    public int frames = 0;
    public int fps = 0;

    public boolean pendingNextWord = false;

    public boolean pendingGameOver = false;
    public String gameOverWinner = "";

    public boolean botSeq = false;
    public int botPhase = 0;
    public long botPhaseUntil = 0;

    public boolean playerSeq = false;
    public int playerPhase = 0;
    public long playerPhaseUntil = 0;

    public boolean playerTakingHit = false;
    public long playerHitUntil = 0;
    public boolean opponentTakingHit = false;
    public long opponentHitUntil = 0;

    public boolean showPlayerDamage = false;
    public boolean showBotDamage = false;
    public long playerDamageUntil = 0;
    public long botDamageUntil = 0;
    public int playerDamageX = 0;
    public int playerDamageY = 0;
    public int botDamageX = 0;
    public int botDamageY = 0;

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
            System.err.println("Failed : " + e.getMessage());
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
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
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
            WordEntry newWord = wordBank.get(rng.nextInt(wordBank.size()));
            while (current != null && newWord.word.equals(current.word) && wordBank.size() > 1) {
                newWord = wordBank.get(rng.nextInt(wordBank.size()));
            }
            current = newWord;
        }

        playerIdx = 0;

        if (uiSettings != null) {
            uiSettings.speakCurrentWord(current);
        }
    }

    public void resetRun(AnimationController animController) {
        wordsCompleted = 0;
        comboCount = 0;
        playerHealth = GameConfig.MAX_HEALTH;
        botHealth = GameConfig.MAX_HEALTH;
        playerIdx = 0;
        pendingNextWord = false;
        pendingGameOver = false;
        gameOverWinner = "";
        botSeq = false;
        playerSeq = false;
        playerTakingHit = false;
        opponentTakingHit = false;
        playerHitUntil = opponentHitUntil = 0;
        showPlayerDamage = false;
        showBotDamage = false;
        playerDamageUntil = botDamageUntil = 0;

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
            showPlayerDamage = true;
            playerDamageUntil = now + 1000;
            playerDamageX = 100;
            playerDamageY = 250;
            comboCount = 0;
            botPhaseUntil = now + ATTACK_HOLD_MS;
            botPhase = 1;
        } else if (botPhase == 1) {
            if (now >= botPhaseUntil) {
                bot.x = botBaseX;
                bot.y = groundY;
                animController.setAnim(bot, CharacterPack.Anim.IDLE);
                botSeq = false;

                if (pendingGameOver && gameOverWinner.equals("OVER")) {
                    state = GameConfig.State.GAMEOVER;
                    current = new WordEntry("OVER", "OVER", "โอเวอร์", "จบ");
                    animController.setAnim(player, CharacterPack.Anim.DEATH);
                    animController.playIfAudible(sDeath);
                    ScoreManager.getInstance().saveScore(wordsCompleted);
                    pendingGameOver = false;
                }
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
            if (isMultiplayerMode && opponent != null) {
                animController.setAnim(opponent, CharacterPack.Anim.TAKE_HIT);
                opponentTakingHit = true;
                opponentHitUntil = now + TAKE_HIT_HOLD_MS;
            } else if (bot != null) {
                animController.setAnim(bot, CharacterPack.Anim.TAKE_HIT);
            }
            animController.playIfAudible(sHit);
            showBotDamage = true;
            botDamageUntil = now + 1000;
            botDamageX = panelWidth - 100;
            botDamageY = 250;
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
                }
                if (pendingGameOver) {
                    state = GameConfig.State.GAMEOVER;
                    if (gameOverWinner.equals("WIN")) {
                        current = new WordEntry("WIN", "WIN", "ชนะ", "ชัยชนะ");
                        animController.setAnim(bot, CharacterPack.Anim.DEATH);
                    } else {
                        current = new WordEntry("OVER", "OVER", "โอเวอร์", "จบ");
                        animController.setAnim(player, CharacterPack.Anim.DEATH);
                    }
                    animController.playIfAudible(sDeath);
                    ScoreManager.getInstance().saveScore(wordsCompleted);
                    pendingGameOver = false;
                } else {
                    nextWord();
                }
            }
        }
    }

    public void handleChar(char c, AnimationController animController) {
        if (playerIdx < current.word.length() && c == current.word.charAt(playerIdx)) {
            animController.playIfAudible(sType);
            playerIdx++;
            popUntil = System.currentTimeMillis() + 90;

            if (playerIdx >= current.word.length()) {
                comboCount++;
                if (comboCount > 2) {
                    int cx = panelWidth / 2;
                    int cy = panelHeight / 2 + 150;
                    comboEffect.triggerCombo(comboCount, 1, cx, cy);
                }

                wordsCompleted++;
                botHealth = Math.max(0, botHealth - 1);
                startPlayerAttackSequence();

                if (botHealth <= 0) {
                    pendingGameOver = true;
                    gameOverWinner = "WIN";
                }
            }
        } else {
            animController.playIfAudible(sErr);
        }
    }

    public void update(long now, AnimationController animController) {
        if (state == GameConfig.State.PLAYING) {
            comboEffect.update();
            if (isMultiplayerMode) {
                updateMultiplayerTyping(now, animController);
            }
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
    }

    private void updateMultiplayerTyping(long now, AnimationController animController) {
        if (playerTakingHit && now >= playerHitUntil) {
            animController.setAnim(player, CharacterPack.Anim.IDLE);
            playerTakingHit = false;
        }

        if (opponentTakingHit && now >= opponentHitUntil) {
            if (opponent != null) {
                animController.setAnim(opponent, CharacterPack.Anim.IDLE);
            }
            opponentTakingHit = false;
        }

        if (showPlayerDamage && now >= playerDamageUntil) {
            showPlayerDamage = false;
        }

        if (showBotDamage && now >= botDamageUntil) {
            showBotDamage = false;
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

    public void resetToReady() {
        state = GameConfig.State.READY;
        previousState = GameConfig.State.READY;
        playerIdx = 0;
        mistakes = 0;
        wordsCompleted = 0;
        playerHealth = GameConfig.MAX_HEALTH;
        botHealth = GameConfig.MAX_HEALTH;
        comboCount = 0;
        startMs = 0;
        flashUntil = 0;
        shakeUntil = 0;
        popUntil = 0;
        pendingGameOver = false;
        gameOverWinner = "";
        showPlayerDamage = false;
        showBotDamage = false;
        playerTakingHit = false;
        opponentTakingHit = false;
        current = new WordEntry("HELLO", "HELLO", "เฮลโล", "สวัสดี");
    }

    public void startGame() {
        if (state == GameConfig.State.READY) {
            state = GameConfig.State.PLAYING;
            startMs = System.currentTimeMillis();
            if (!isMultiplayerMode) {
                nextWord();
            }
            if (bgMusic != null) {
                bgMusic.stop();
                bgMusic.play();
            }
        }
    }

    public void setMultiplayerMode(boolean multiplayer) {
        this.isMultiplayerMode = multiplayer;
        if (multiplayer && opponent == null) {
            opponent = new CharacterPack("./res/characters/Wizard/", botBaseX, groundY, false);
        }
    }

    public boolean isMultiplayerMode() {
        return isMultiplayerMode;
    }

    public void setCurrentWord(String word) {
        if (word != null && !word.isEmpty()) {
            this.currentWord = word;
            current = new WordEntry(word.toUpperCase(), word.toUpperCase(), word, word);
        }
    }

    public String getCurrentWord() {
        return currentWord;
    }

    public void setPlayerName(String name) {
        this.playerName = name;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setShowOnlineUI(boolean show) {
        this.showOnlineUI = show;
    }

    public boolean isShowOnlineUI() {
        return showOnlineUI;
    }

    public void setStatusMessage(String message) {
        this.statusMessage = message;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setOpponentName(String name) {
        this.opponentName = name;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentIdx(int idx) {
        this.opponentIdx = idx;
    }

    public int getOpponentIdx() {
        return opponentIdx;
    }

    public void setOpponentCharacterPack(CharacterPack pack) {
        this.opponentCharacterPack = pack;
    }

    public CharacterPack getOpponentCharacterPack() {
        return opponentCharacterPack;
    }

}

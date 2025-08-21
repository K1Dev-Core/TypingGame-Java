public class AnimationController {
    private final GameState gameState;
    private final UISettings uiSettings;

    public AnimationController(GameState gameState, UISettings uiSettings) {
        this.gameState = gameState;
        this.uiSettings = uiSettings;
    }

    public void setAnim(CharacterPack c, CharacterPack.Anim a) {
        try {
            c.setAnim(a);
        } catch (Throwable ignored) {
        }

        if (a == CharacterPack.Anim.ATTACK)
            playIfAudible(gameState.sSlash);
    }

    public void playIfAudible(SoundPool p) {
        if (uiSettings.isAudible())
            p.play();
    }

    public void initializeCharacters() {
        CharacterPack.Config heroCfg = new CharacterPack.Config()
                .set(CharacterPack.Anim.IDLE, 8, 120, true)
                .set(CharacterPack.Anim.ATTACK, 4, 80, false)
                .set(CharacterPack.Anim.TAKE_HIT, 4, 90, false)
                .set(CharacterPack.Anim.DEATH, 6, 150, false)
                .set(CharacterPack.Anim.WALK, 1, 1000, true);

        gameState.player = new CharacterPack(
                "./res/characters/MedievalKing",
                160, 470, false,
                heroCfg, -20);

        gameState.bot = new CharacterPack(
                "./res/characters/Skeleton",
                920, 470, true,
                null, 20);

        setAnim(gameState.player, CharacterPack.Anim.IDLE);
        setAnim(gameState.bot, CharacterPack.Anim.IDLE);
    }

    public void cycleCharacter(int dir) {
        uiSettings.selectedCharIdx = (uiSettings.selectedCharIdx + dir + uiSettings.CHAR_NAMES.length)
                % uiSettings.CHAR_NAMES.length;
        applySelectedCharacter();
        playIfAudible(gameState.sClick);
    }

    public void applySelectedCharacter() {
        CharacterPack.Config cfg = uiSettings.configFor(uiSettings.selectedCharIdx);
        gameState.player = new CharacterPack(
                uiSettings.CHAR_PATHS[uiSettings.selectedCharIdx],
                gameState.playerBaseX,
                gameState.groundY, false,
                cfg,
                uiSettings.CHAR_BASELINE[uiSettings.selectedCharIdx]);

        setAnim(gameState.player, CharacterPack.Anim.IDLE);
    }
}

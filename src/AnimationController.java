public class AnimationController {
    private final GameState gameState;
    private final UISettings uiSettings;
    private final CharacterConfig characterConfig;

    public AnimationController(GameState gameState, UISettings uiSettings) {
        this.gameState = gameState;
        this.uiSettings = uiSettings;
        this.characterConfig = CharacterConfig.getInstance();
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

        gameState.player = characterConfig.createCharacterPack(
                "medieval_king",
                160, 470, false);

        gameState.bot = characterConfig.createCharacterPack(
                "flying_eye",
                920, 470, true);

        setAnim(gameState.player, CharacterPack.Anim.IDLE);
        setAnim(gameState.bot, CharacterPack.Anim.IDLE);
    }

    public void cycleCharacter(int dir) {
        String[] playerCharIds = characterConfig.getPlayerCharacterIds();
        uiSettings.selectedCharIdx = (uiSettings.selectedCharIdx + dir + playerCharIds.length)
                % playerCharIds.length;
        applySelectedCharacter();
        playIfAudible(gameState.sClick);
        
        OnlineMatchManager.getInstance().updateLocalPlayerCharacter();
    }

    public void applySelectedCharacter() {
        String[] playerCharIds = characterConfig.getPlayerCharacterIds();
        if (playerCharIds.length > 0) {
            String selectedCharId = playerCharIds[uiSettings.selectedCharIdx];
            gameState.player = characterConfig.createCharacterPack(
                    selectedCharId,
                    gameState.playerBaseX,
                    gameState.groundY,
                    false);

            setAnim(gameState.player, CharacterPack.Anim.IDLE);
        }
    }

    public void randomizeEnemy() {
        CharacterConfig.CharacterData[] enemyChars = characterConfig.getEnemyCharacters();
        String[] enemyCharIds = new String[enemyChars.length];

        for (int i = 0; i < enemyChars.length; i++) {
            enemyCharIds[i] = enemyChars[i].id;
        }

        if (enemyCharIds.length > 0) {
            String randomEnemyId = enemyCharIds[gameState.rng.nextInt(enemyCharIds.length)];
            gameState.bot = characterConfig.createCharacterPack(
                    randomEnemyId,
                    gameState.botBaseX,
                    gameState.groundY,
                    true);

            setAnim(gameState.bot, CharacterPack.Anim.IDLE);
        }
    }
}

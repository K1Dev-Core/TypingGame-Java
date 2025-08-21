import java.util.HashMap;
import java.util.Map;

public class CharacterConfig {
    private static CharacterConfig instance;

    private final Map<String, CharacterData> characters = new HashMap<>();

    public static class CharacterData {
        public final String id;
        public final String name;
        public final String path;
        public final int baselineOffset;
        public final CharacterPack.Config animConfig;
        public boolean isPlayerCharacter;

        public CharacterData(String id, String name, String path, int baselineOffset,
                CharacterPack.Config animConfig, boolean isPlayerCharacter) {
            this.id = id;
            this.name = name;
            this.path = path;
            this.baselineOffset = baselineOffset;
            this.animConfig = animConfig;
            this.isPlayerCharacter = isPlayerCharacter;
        }
    }

    private CharacterConfig() {
        initializeCharacters();
    }

    public static CharacterConfig getInstance() {
        if (instance == null) {
            instance = new CharacterConfig();
        }
        return instance;
    }

    private void initializeCharacters() {
        CharacterPack.Config kingConfig = new CharacterPack.Config()
                .set(CharacterPack.Anim.IDLE, 8, 120, true)
                .set(CharacterPack.Anim.ATTACK, 4, 80, false)
                .set(CharacterPack.Anim.TAKE_HIT, 4, 90, false)
                .set(CharacterPack.Anim.DEATH, 6, 150, false)
                .set(CharacterPack.Anim.WALK, 1, 1000, true);

        addCharacter(new CharacterData(
                "medieval_king",
                "MedievalKing",
                "./res/characters/MedievalKing",
                -20,
                kingConfig,
                true));
        CharacterPack.Config mushroomConfig = new CharacterPack.Config()
                .set(CharacterPack.Anim.IDLE, 4, 120, true)
                .set(CharacterPack.Anim.ATTACK, 8, 80, false)
                .set(CharacterPack.Anim.TAKE_HIT, 4, 90, false)
                .set(CharacterPack.Anim.DEATH, 4, 150, false)
                .set(CharacterPack.Anim.WALK, 8, 110, true);

        addCharacter(new CharacterData(
                "mushroom",
                "Mushroom",
                "./res/characters/Mushroom",
                20,
                mushroomConfig,
                true));
        CharacterPack.Config skeletonConfig = new CharacterPack.Config()
                .set(CharacterPack.Anim.IDLE, 4, 120, true)
                .set(CharacterPack.Anim.ATTACK, 8, 80, false)
                .set(CharacterPack.Anim.TAKE_HIT, 4, 90, false)
                .set(CharacterPack.Anim.DEATH, 4, 150, false)
                .set(CharacterPack.Anim.WALK, 4, 110, true);

        addCharacter(new CharacterData(
                "skeleton",
                "Skeleton",
                "./res/characters/Skeleton",
                20,
                skeletonConfig,
                true));
        CharacterPack.Config flyingEyeConfig = new CharacterPack.Config()
                .set(CharacterPack.Anim.IDLE, 8, 120, true)
                .set(CharacterPack.Anim.ATTACK, 8, 80, false)
                .set(CharacterPack.Anim.TAKE_HIT, 4, 90, false)
                .set(CharacterPack.Anim.DEATH, 4, 150, false);

        addCharacter(new CharacterData(
                "flying_eye",
                "FlyingEye",
                "./res/characters/Flyingeye",
                0,
                flyingEyeConfig,
                false));
    }

    public void addCharacter(CharacterData character) {
        characters.put(character.id, character);
    }

    public CharacterData getCharacter(String id) {
        return characters.get(id);
    }

    public Map<String, CharacterData> getAllCharacters() {
        return new HashMap<>(characters);
    }

    public CharacterData[] getPlayerCharacters() {
        return characters.values().stream()
                .filter(c -> c.isPlayerCharacter)
                .toArray(CharacterData[]::new);
    }

    public CharacterData[] getEnemyCharacters() {
        return characters.values().stream()
                .filter(c -> !c.isPlayerCharacter)
                .toArray(CharacterData[]::new);
    }

    public CharacterPack createCharacterPack(String characterId, int x, int y, boolean facingLeft) {
        CharacterData data = getCharacter(characterId);
        if (data == null) {
            data = getCharacter("medieval_king");
            if (data == null) {
                return new CharacterPack("./res/characters/MedievalKing", x, y, facingLeft);
            }
        }

        return new CharacterPack(
                data.path,
                x, y, facingLeft,
                data.animConfig,
                data.baselineOffset);
    }

    public String[] getCharacterIds() {
        return characters.keySet().toArray(new String[0]);
    }

    public String[] getCharacterNames() {
        return characters.values().stream()
                .map(c -> c.name)
                .toArray(String[]::new);
    }

    public String[] getCharacterPaths() {
        return characters.values().stream()
                .map(c -> c.path)
                .toArray(String[]::new);
    }

    public int[] getCharacterBaselineOffsets() {
        return characters.values().stream()
                .mapToInt(c -> c.baselineOffset)
                .toArray();
    }

    public String[] getPlayerCharacterIds() {
        return characters.values().stream()
                .filter(c -> c.isPlayerCharacter)
                .map(c -> c.id)
                .toArray(String[]::new);
    }

    public CharacterPack.Config getCharacterConfig(String characterId) {
        CharacterData data = getCharacter(characterId);
        return data != null ? data.animConfig : null;
    }
}


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WordBank {

    public static List<WordEntry> loadWordBank() {
        List<WordEntry> list = new ArrayList<>();
        File f = new File(GameConfig.WORDS_TXT_PATH);
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split("\\|");
                    if (parts.length < 3) {
                        continue;
                    }
                    String rawWord = parts[0].trim();
                    String disp = rawWord;
                    String wordUP = rawWord.toUpperCase(Locale.US).replaceAll("[^A-Z0-9]", "");
                    if (wordUP.isEmpty()) {
                        continue;
                    }
                    String pronun = parts[1].trim();
                    String meaning = parts[2].trim();
                    list.add(new WordEntry(wordUP, disp, pronun, meaning));
                }
            } catch (Exception ignored) {
            }
        }

        if (list.isEmpty()) {
            String[][] rows = new String[][]{
                {"TIME", "ไทม์", "เวลา"},
                {"LIGHT", "ไลท์", "แสง"},
                {"SOUND", "ซาวนด์", "เสียง"},
                {"QUIET", "ไควเอ็ต", "เงียบ"},
                {"SOFT", "ซอฟท์", "นุ่ม"},
                {"TOUCH", "ทัช", "สัมผัส"},
                {"DREAM", "ดรีม", "ความฝัน"},
                {"CLOUD", "คลาวด์", "เมฆ"},
                {"NIGHT", "ไนท์", "กลางคืน"},
                {"MOON", "มูน", "ดวงจันทร์"},
                {"STAR", "สตาร์", "ดาว"},
                {"RIVER", "ริเวอร์", "แม่น้ำ"},
                {"OCEAN", "โอเชียน", "มหาสมุทร"},
                {"STONE", "สโตน", "หิน"},
                {"SAND", "แซนด์", "ทราย"},
                {"SMILE", "สไมล์", "ยิ้ม"},
                {"WATER", "วอเทอร์", "น้ำ"},
                {"EARTH", "เอิร์ธ", "โลก"},
                {"GREEN", "กรีน", "สีเขียว"},
                {"HAPPY", "แฮพพี", "มีความสุข"},
                {"PEACE", "พีซ", "สันติภาพ"},
                {"MUSIC", "มิวซิก", "ดนตรี"},
                {"PIZZA", "พิซซ่า", "พิซซ่า"},
                {"SUGAR", "ชูการ์", "น้ำตาล"},
                {"HONEY", "ฮันนี", "น้ำผึ้ง"},
                {"COCOA", "โคโค", "โกโก้"},
                {"MILK", "มิลค์", "นม"},
                {"LATTE", "ลาเต้", "ลาเต้"},
                {"JAVA", "จาวา", "ภาษาจาวา"},
                {"SMOOTH", "สมูธ", "เรียบ/นุ่ม"}
            };
            for (String[] r : rows) {
                list.add(new WordEntry(r[0], r[0], r[1], r[2]));
            }
        }
        return list;
    }
}

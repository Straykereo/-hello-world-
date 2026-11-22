import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main {

    // --- МОДЕЛИ ДАННЫХ ---
    static class DifficultyThresholds {
        int easyMaxMainWordLength;
        int mediumMaxMainWordLength;
        int easyMaxSubWords;
        int mediumMaxSubWords;

        public DifficultyThresholds(int easyLen, int medLen, int easySub, int medSub) {
            this.easyMaxMainWordLength = easyLen;
            this.mediumMaxMainWordLength = medLen;
            this.easyMaxSubWords = easySub;
            this.mediumMaxSubWords = medSub;
        }
    }

    static class Config {
        int minSubWordLength;
        int minWordsToPass;
        DifficultyThresholds difficultyThresholds;

        public Config(int minSubWordLength, int minWordsToPass, DifficultyThresholds difficultyThresholds) {
            this.minSubWordLength = minSubWordLength;
            this.minWordsToPass = minWordsToPass;
            this.difficultyThresholds = difficultyThresholds;
        }
    }

    static class SubWord {
        String text;
        String hint;

        public SubWord(String text, String hint) {
            this.text = text;
            this.hint = hint;
        }
    }

    static class Level {
        int id;
        String mainWord;
        String difficulty;
        int minWordsToPass;
        List<SubWord> subWords;

        public Level(int id, String mainWord, String difficulty, int minWordsToPass, List<SubWord> subWords) {
            this.id = id;
            this.mainWord = mainWord;
            this.difficulty = difficulty;
            this.minWordsToPass = minWordsToPass;
            this.subWords = subWords;
        }
    }

    // --- ГЕНЕРАТОР УРОВНЕЙ (STREAM API + COLLECTIONS) ---
    static class LevelGenerator {
        public List<Level> generate(List<String> dictionary, List<String> mainWords, Config config) {
            Set<String> cleanDictionary = dictionary.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .filter(word -> word.length() >= config.minSubWordLength)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            AtomicInteger idCounter = new AtomicInteger(1);
            List<Level> levels = new ArrayList<>();

            for (String rawMainWord : mainWords) {
                if (rawMainWord == null || rawMainWord.isBlank()) continue;
                String mainWord = rawMainWord.trim().toUpperCase();
                Map<Character, Long> mainWordFreq = getCharFrequency(mainWord);

                List<SubWord> validSubWords = cleanDictionary.stream()
                        .filter(sub -> !sub.equals(mainWord))
                        .filter(sub -> sub.length() < mainWord.length())
                        .filter(sub -> canFormWord(sub, mainWordFreq))
                        .map(sub -> new SubWord(sub, ""))
                        .collect(Collectors.toList());

                if (validSubWords.size() >= config.minWordsToPass) {
                    String difficulty = calculateDifficulty(
                            mainWord.length(),
                            validSubWords.size(),
                            config.difficultyThresholds
                    );
                    levels.add(new Level(
                            idCounter.getAndIncrement(),
                            mainWord,
                            difficulty,
                            config.minWordsToPass,
                            validSubWords
                    ));
                }
            }
            return levels;
        }

        private Map<Character, Long> getCharFrequency(String word) {
            return word.chars()
                    .mapToObj(c -> (char) c)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        }

        private boolean canFormWord(String candidate, Map<Character, Long> mainWordFreq) {
            Map<Character, Long> candidateFreq = getCharFrequency(candidate);
            for (Map.Entry<Character, Long> entry : candidateFreq.entrySet()) {
                if (mainWordFreq.getOrDefault(entry.getKey(), 0L) < entry.getValue()) {
                    return false;
                }
            }
            return true;
        }

        private String calculateDifficulty(int mainLen, int subCount, DifficultyThresholds t) {
            if (mainLen <= t.easyMaxMainWordLength && subCount <= t.easyMaxSubWords) {
                return "EASY";
            } else if (mainLen <= t.mediumMaxMainWordLength && subCount <= t.mediumMaxSubWords) {
                return "MEDIUM";
            } else {
                return "HARD";
            }
        }
    }

    // --- ФОРМАТИРОВАНИЕ В JSON ---
    static String toJson(List<Level> levels) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"levels\": [\n");
        for (int i = 0; i < levels.size(); i++) {
            Level l = levels.get(i);
            sb.append("    {\n");
            sb.append("      \"id\": ").append(l.id).append(",\n");
            sb.append("      \"mainWord\": \"").append(l.mainWord).append("\",\n");
            sb.append("      \"difficulty\": \"").append(l.difficulty).append("\",\n");
            sb.append("      \"minWordsToPass\": ").append(l.minWordsToPass).append(",\n");
            sb.append("      \"subWords\": [\n");
            for (int j = 0; j < l.subWords.size(); j++) {
                SubWord sw = l.subWords.get(j);
                sb.append("        { \"text\": \"").append(sw.text).append("\", \"hint\": \"").append(sw.hint).append("\" }");
                if (j < l.subWords.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("      ]\n    }");
            if (i < levels.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }

    // --- ТОЧКА ВХОДА И ТЕСТЫ ---
    public static void main(String[] args) {
        Config config = new Config(3, 8, new DifficultyThresholds(7, 10, 15, 30));

        List<String> dictionary = Arrays.asList(
                "десант", "АДРЕС", "армия", "место", "метро", "десант",
                "сон", "нос", "мост", "тема", "демон", "дама", "ёж",
                "демонстрация", "трос", "сорт", "рост", "торт", "сено",
                "мир", "рим", "тир", "арена", "смена", "стена", "мера",
                "нота", "тон", "стан", "наст", "сани", "тиран", "район"
        );

        List<String> mainWords = Arrays.asList("ДЕМОНСТРАЦИЯ", "ВЗАИМНОСТЬ", "КОТ");

        LevelGenerator generator = new LevelGenerator();
        List<Level> result = generator.generate(dictionary, mainWords, config);

        System.out.println("=== OUTPUT JSON ===");
        System.out.println(toJson(result));

        System.out.println("\n=== TEST CASES ===");
        System.out.println("Test 1-2 (Multiplicity & Self): " + (generator.generate(Arrays.asList("мама", "маама"), List.of("МАМА"), new Config(3, 1, config.difficultyThresholds)).isEmpty() ? "PASSED" : "FAILED"));
        System.out.println("Test 4-5 (Duplicates & Length): " + (generator.generate(Arrays.asList("кот", "КОТ", "ок", "ток"), List.of("КОТОК"), new Config(3, 2, config.difficultyThresholds)).size() == 1 ? "PASSED" : "FAILED"));
        System.out.println("Test 6 (Letter Yo): " + (generator.generate(Arrays.asList("еж", "ёж"), List.of("ЁЖИК"), new Config(2, 1, config.difficultyThresholds)).get(0).subWords.get(0).text.equals("ЁЖ") ? "PASSED" : "FAILED"));
        System.out.println("Test 7 (No matches): " + (generator.generate(List.of("абвгд"), List.of("ДЕМОНСТРАЦИЯ"), config).isEmpty() ? "PASSED" : "FAILED"));
        System.out.println("Test 8 (Exact threshold): " + (generator.generate(Arrays.asList("сон", "нос", "тон"), List.of("СТОНОГ"), new Config(3, 3, config.difficultyThresholds)).size() == 1 ? "PASSED" : "FAILED"));
    }
}

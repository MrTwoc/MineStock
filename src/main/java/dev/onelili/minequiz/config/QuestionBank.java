package dev.onelili.minequiz.config;

import dev.onelili.minequiz.MineQuiz;
import dev.onelili.minequiz.game.Question;
import dev.onelili.minequiz.game.QuestionType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 题库管理器 — 负责从 questions/ 目录下加载各分类题库文件
 * <p>
 * 目录结构: plugins/MineQuiz/questions/*.yml
 * 支持两种方式:
 *   1. 多个分类文件 (推荐): questions/astronomy_geography.yml, questions/history.yml, ...
 *   2. 单个旧版文件 (兼容): questions.yml
 */
public class QuestionBank {

    private final MineQuiz plugin;
    private final List<Question> allQuestions = new ArrayList<>();
    private final Queue<Question> questionQueue = new LinkedList<>();
    private final Random random = new Random();

    /** 默认内置题库文件名列表（供 saveDefaultQuestions 使用） */
    private static final String[] DEFAULT_QUESTION_FILES = {
            "questions/astronomy_geography.yml",
            "questions/history.yml",
            "questions/cs_math.yml",
            "questions/modern_knowledge.yml"
    };

    public QuestionBank(MineQuiz plugin) {
        this.plugin = plugin;
    }

    /**
     * 从 questions/ 目录加载所有分类题库
     */
    public void load() {
        allQuestions.clear();
        questionQueue.clear();

        int loaded = 0;

        // 1. 保存默认分类题库到 data 目录
        saveDefaultQuestions();

        // 2. 从 questions/ 目录加载所有 .yml 文件
        File questionsDir = new File(plugin.getDataFolder(), "questions");
        if (questionsDir.isDirectory()) {
            File[] files = questionsDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                Arrays.sort(files); // 固定加载顺序
                for (File file : files) {
                    int count = loadQuestionsFromFile(file);
                    if (count > 0) {
                        plugin.getLogger().info("[MineQuiz]   ├─ " + file.getName() + " (" + count + " 题)");
                    }
                    loaded += count;
                }
            }
        }

        // 3. 兼容旧版: 如 questions/ 下未加载到任何题目，尝试回退到 questions.yml
        if (loaded == 0) {
            File legacyFile = new File(plugin.getDataFolder(), "questions.yml");
            if (legacyFile.exists()) {
                plugin.getLogger().info("[MineQuiz] questions/ 目录为空，回退加载 questions.yml");
                loaded = loadQuestionsFromFile(legacyFile);
            }
        }

        if (loaded == 0) {
            plugin.getLogger().warning("[MineQuiz] 题库为空！请检查 questions/ 目录下的题库文件");
            return;
        }

        plugin.getLogger().info("[MineQuiz] 题库加载完成，共 " + allQuestions.size() + " 道题目");
        shuffleQueue();
    }

    /**
     * 从单个 YAML 文件中解析题目列表
     * @param file 题库文件
     * @return 成功解析的题目数
     */
    private int loadQuestionsFromFile(File file) {
        if (!file.exists()) return 0;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> questionsList = config.getMapList("questions");
        int count = 0;

        for (Map<?, ?> entry : questionsList) {
            try {
                int id = (int) entry.get("id");
                QuestionType type = QuestionType.valueOf((String) entry.get("type"));
                String category = (String) entry.get("category");
                String question = (String) entry.get("question");

                @SuppressWarnings("unchecked")
                List<String> options = (List<String>) entry.get("options");
                int answer = (int) entry.get("answer");

                if (options == null || options.size() < 2) {
                    plugin.getLogger().warning("[MineQuiz] 题目 #" + id + " 选项不足，跳过");
                    continue;
                }

                allQuestions.add(new Question(id, type, category, question, options, answer));
                count++;
            } catch (Exception e) {
                plugin.getLogger().warning("[MineQuiz] 解析题目时出错: " + e.getMessage());
            }
        }

        return count;
    }

    /**
     * 将 jar 包中 questions/ 下的默认题库文件保存到插件数据目录（仅首次）
     */
    private void saveDefaultQuestions() {
        File questionsDir = new File(plugin.getDataFolder(), "questions");
        if (!questionsDir.exists()) {
            questionsDir.mkdirs();
        }

        for (String resourcePath : DEFAULT_QUESTION_FILES) {
            File target = new File(plugin.getDataFolder(), resourcePath);
            if (!target.exists()) {
                plugin.saveResource(resourcePath, false);
            }
        }
    }

    /**
     * 打乱题目顺序加入队列
     */
    private void shuffleQueue() {
        List<Question> shuffled = new ArrayList<>(allQuestions);
        Collections.shuffle(shuffled, random);
        questionQueue.addAll(shuffled);
    }

    /**
     * 从队列中取下一题。队列用完时自动重新打乱
     */
    public Question nextQuestion() {
        if (questionQueue.isEmpty()) {
            shuffleQueue();
        }
        return questionQueue.poll();
    }

    /**
     * 获取题目总数
     */
    public int size() {
        return allQuestions.size();
    }
}

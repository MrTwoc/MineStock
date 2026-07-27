package dev.onelili.minequiz.config;

import dev.onelili.minequiz.MineQuiz;
import dev.onelili.minequiz.game.Question;
import dev.onelili.minequiz.game.QuestionType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 题库管理器 — 负责从 questions.yml 中加载和读取题目
 */
public class QuestionBank {

    private final MineQuiz plugin;
    private final List<Question> allQuestions = new ArrayList<>();
    private final Queue<Question> questionQueue = new LinkedList<>();
    private final Random random = new Random();

    public QuestionBank(MineQuiz plugin) {
        this.plugin = plugin;
    }

    /**
     * 从 questions.yml 加载题库
     */
    public void load() {
        allQuestions.clear();
        questionQueue.clear();

        File file = new File(plugin.getDataFolder(), "questions.yml");
        if (!file.exists()) {
            plugin.saveResource("questions.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> questionsList = config.getMapList("questions");

        if (questionsList.isEmpty()) {
            plugin.getLogger().warning("[MineQuiz] 题库为空！");
            return;
        }

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
            } catch (Exception e) {
                plugin.getLogger().warning("[MineQuiz] 解析题目时出错: " + e.getMessage());
            }
        }

        plugin.getLogger().info("[MineQuiz] 已加载 " + allQuestions.size() + " 道题目");
        shuffleQueue();
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

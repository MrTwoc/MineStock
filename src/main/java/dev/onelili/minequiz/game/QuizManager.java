package dev.onelili.minequiz.game;

import dev.onelili.minequiz.MineQuiz;
import dev.onelili.minequiz.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 问答管理器 — 全局单例，控制问答轮次调度
 */
public class QuizManager {

    private static QuizManager instance;

    private final MineQuiz plugin;
    private final DatabaseManager database;
    private final Map<UUID, Integer> scores = new HashMap<>();
    private QuizRound currentRound;
    private BukkitTask autoTask;
    private boolean running;

    private QuizManager(MineQuiz plugin) {
        this.plugin = plugin;
        this.database = new DatabaseManager(plugin);
        try {
            this.database.init();
        } catch (Exception e) {
            plugin.getLogger().severe("[MineQuiz] 数据库初始化失败，插件将无法正常工作: " + e.getMessage());
            throw new RuntimeException("数据库初始化失败", e);
        }
        // 从数据库加载已有分数
        Map<UUID, Integer> loaded = database.loadAllScores();
        scores.putAll(loaded);
        plugin.getLogger().info("[MineQuiz] 已加载 " + loaded.size() + " 位玩家的历史分数");
    }

    public static synchronized QuizManager getInstance() {
        return instance;
    }

    public static synchronized void init(MineQuiz plugin) {
        if (instance == null) {
            instance = new QuizManager(plugin);
        }
    }

    public static synchronized void shutdown() {
        if (instance != null) {
            instance.stop();
            instance.database.close();
            instance = null;
        }
    }

    /**
     * 启动自动问答计时器
     */
    public void startAutoQuiz() {
        if (running) return;
        running = true;
        scheduleNext();
    }

    /**
     * 停止自动问答，并将所有分数持久化到数据库
     */
    public void stop() {
        running = false;
        if (autoTask != null) {
            autoTask.cancel();
            autoTask = null;
        }
        if (currentRound != null) {
            currentRound = null;
        }
        // 停止时将所有内存中的分数写回数据库
        database.saveAllScores(scores);
    }

    /**
     * 手动立即开始一轮问答
     */
    public void forceStart() {
        if (currentRound != null && currentRound.isActive()) {
            return; // 当前有题目正在进行
        }
        startNewRound();
    }

    /**
     * 安排下一次自动问答
     */
    private void scheduleNext() {
        if (!running) return;
        int interval = plugin.getQuizConfig().getQuizInterval();
        autoTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!running) return;
            startNewRound();
        }, interval * 20L);
    }

    /**
     * 开始新一轮问答
     */
    private void startNewRound() {
        // 检查在线玩家数是否达到最低要求
        int online = Bukkit.getOnlinePlayers().size();
        int minPlayers = plugin.getQuizConfig().getMinPlayers();
        if (online < minPlayers) {
            plugin.getLang().broadcast("not-enough-players", "min", String.valueOf(minPlayers));
            if (running) {
                scheduleNext();
            }
            return;
        }

        Question question = plugin.getQuestionBank().nextQuestion();
        if (question == null) {
            plugin.getLang().broadcast("no-questions");
            return;
        }

        int answerTimeSeconds = plugin.getQuizConfig().getAnswerTime();
        int points = question.getType() == QuestionType.RACE
                ? plugin.getQuizConfig().getRacePoints()
                : plugin.getQuizConfig().getQuizPoints();

        currentRound = new QuizRound(plugin, question, answerTimeSeconds, points);
        currentRound.start();
    }

    /**
     * 处理玩家答题
     */
    public void handleAnswer(UUID playerUuid, int questionId, int optionIndex) {
        if (currentRound == null || !currentRound.isActive()) return;
        if (currentRound.getQuestion().getId() != questionId) return;

        boolean correct = currentRound.submitAnswer(playerUuid, optionIndex);
        var player = Bukkit.getPlayer(playerUuid);
        if (player == null) return;

        if (correct) {
            int pts = currentRound.getPoints();
            scores.merge(playerUuid, pts, Integer::sum);
            int total = scores.get(playerUuid);

            // 持久化到 SQLite
            database.saveScore(playerUuid, total);

            String key = currentRound.getQuestion().getType() == QuestionType.RACE
                    ? "race-correct-first" : "quiz-correct";
            plugin.getLang().broadcast(key, "player", player.getName(), "points", String.valueOf(pts));
            player.sendMessage(plugin.getLang().get("personal-score", "score", String.valueOf(total)));
        } else {
            // 检查是否因为重复回答
            if (currentRound.hasAnswered(playerUuid)) {
                plugin.getLang().send(player, "quiz-duplicate", "player", player.getName());
            } else if (currentRound.isAnswered()) {
                plugin.getLang().send(player, "race-wrong-order");
            } else {
                String key = currentRound.getQuestion().getType() == QuestionType.RACE
                        ? "race-incorrect" : "quiz-incorrect";
                plugin.getLang().send(player, key, "player", player.getName());
            }
        }
    }

    /**
     * 获取指定玩家的总分
     */
    public int getScore(UUID playerUuid) {
        return scores.getOrDefault(playerUuid, 0);
    }

    /**
     * 获取所有玩家分数（不可变视图）
     */
    public Map<UUID, Integer> getScores() {
        return Map.copyOf(scores);
    }

    /**
     * 获取当前轮次
     */
    public QuizRound getCurrentRound() {
        return currentRound;
    }

    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        return running;
    }
}

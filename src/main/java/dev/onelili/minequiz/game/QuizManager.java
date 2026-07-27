package dev.onelili.minequiz.game;

import dev.onelili.minequiz.MineQuiz;
import dev.onelili.minequiz.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 问答管理器 — 全局单例，控制轮次调度与分数管理
 *
 * 每轮包含多道题目，题型在 抢答/问答 之间随机分配。
 * 每道题结束后自动进入下一题，全部完成后等待轮次间隔再开启下一轮。
 */
public class QuizManager {

    private static QuizManager instance;

    private final MineQuiz plugin;
    private final DatabaseManager database;
    private final Map<UUID, Integer> scores = new HashMap<>();
    private final Random random = new Random();

    /** 当前正在答题的题目（一轮中的一道题） */
    private QuizRound currentRound;
    /** 自动轮次定时任务 */
    private BukkitTask autoTask;
    /** 自动轮次是否已启动 */
    private boolean running;
    /** 本轮剩余题目数 */
    private int questionsRemaining;

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
     * 启动自动问答轮次
     */
    public void startAutoQuiz() {
        if (running) return;
        running = true;
        scheduleNextRound();
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
        finishCurrentRound();
        database.saveAllScores(scores);
    }

    /**
     * 手动立即开始一轮问答（若已在答题则忽略）
     */
    public void forceStart() {
        if (currentRound != null && currentRound.isActive()) {
            return;
        }
        // 取消已排期的自动轮次
        if (autoTask != null) {
            autoTask.cancel();
            autoTask = null;
        }
        startNewRound();
    }

    /**
     * 安排下一轮自动问答
     */
    private void scheduleNextRound() {
        if (!running) return;
        int interval = plugin.getQuizConfig().getRoundInterval();
        autoTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!running) return;
            startNewRound();
        }, interval * 20L);
    }

    /**
     * 开始新一轮问答
     */
    private void startNewRound() {
        // 检查在线玩家数
        int online = Bukkit.getOnlinePlayers().size();
        int minPlayers = plugin.getQuizConfig().getMinPlayers();
        if (online < minPlayers) {
            plugin.getLang().broadcast("not-enough-players", "min", String.valueOf(minPlayers));
            if (running) {
                scheduleNextRound();
            }
            return;
        }

        questionsRemaining = plugin.getQuizConfig().getQuestionsPerRound();
        plugin.getLang().broadcast("round-start", "count", String.valueOf(questionsRemaining));
        startNextQuestion();
    }

    /**
     * 开始本轮中的下一道题
     */
    private void startNextQuestion() {
        questionsRemaining--;

        Question question = plugin.getQuestionBank().nextQuestion();
        if (question == null) {
            plugin.getLang().broadcast("no-questions");
            onRoundFinished();
            return;
        }

        // 随机分配题型
        QuestionType type = random.nextBoolean() ? QuestionType.RACE : QuestionType.QUIZ;

        int answerTimeSeconds = plugin.getQuizConfig().getAnswerTime();
        int pts = type == QuestionType.RACE
                ? plugin.getQuizConfig().getRacePoints()
                : plugin.getQuizConfig().getQuizPoints();

        currentRound = new QuizRound(plugin, question, answerTimeSeconds, pts, type, this::onQuestionEnd);
        currentRound.start();
    }

    /**
     * 一道题结束后的回调 — 启动下一题或结束本轮
     */
    private void onQuestionEnd() {
        currentRound = null;

        if (questionsRemaining > 0) {
            int delay = plugin.getQuizConfig().getBetweenQuestionsDelay();
            Bukkit.getScheduler().runTaskLater(plugin, this::startNextQuestion, delay * 20L);
        } else {
            onRoundFinished();
        }
    }

    /**
     * 本轮全部题目结束
     */
    private void onRoundFinished() {
        plugin.getLang().broadcast("quiz-ended");

        if (running) {
            // 断线重连保护：先确保无残留自动任务，再排下一轮
            if (autoTask != null) {
                autoTask.cancel();
                autoTask = null;
            }
            scheduleNextRound();
        }
    }

    /**
     * 强制结束当前轮次（插件卸载时调用）
     */
    private void finishCurrentRound() {
        if (currentRound != null) {
            currentRound = null;
        }
        questionsRemaining = 0;
    }

    // ========= 玩家答题 & 分数查询 =========

    /**
     * 处理玩家答题 — 仅记录答案和分数，不提前公布结果
     */
    public void handleAnswer(UUID playerUuid, int questionId, int optionIndex) {
        if (currentRound == null || !currentRound.isActive()) return;
        if (currentRound.getQuestion().getId() != questionId) return;

        var player = Bukkit.getPlayer(playerUuid);
        if (player == null) return;

        var result = currentRound.submitAnswer(playerUuid, optionIndex);

        switch (result) {
            case CORRECT -> {
                int pts = currentRound.getPoints();
                scores.merge(playerUuid, pts, Integer::sum);
                database.saveScore(playerUuid, scores.get(playerUuid));
                plugin.getLang().send(player, "answer-submitted");
            }
            case WRONG -> {
                plugin.getLang().send(player, "answer-submitted");
            }
            case ALREADY_SUBMITTED -> {
                plugin.getLang().send(player, "already-answered");
            }
            case ROUND_ENDED -> {
                // 无操作
            }
        }
    }

    public int getScore(UUID playerUuid) {
        return scores.getOrDefault(playerUuid, 0);
    }

    /**
     * 直接设置指定玩家的分数并持久化
     */
    public void setScore(UUID playerUuid, int newScore) {
        if (newScore < 0) newScore = 0;
        scores.put(playerUuid, newScore);
        database.saveScore(playerUuid, newScore);
    }

    public Map<UUID, Integer> getScores() {
        return Map.copyOf(scores);
    }

    public QuizRound getCurrentRound() {
        return currentRound;
    }

    public boolean isRunning() {
        return running;
    }
}

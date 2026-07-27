package dev.onelili.minequiz.api;

import dev.onelili.minequiz.MineQuiz;
import dev.onelili.minequiz.config.QuestionBank;
import dev.onelili.minequiz.config.QuizConfig;
import dev.onelili.minequiz.game.QuizManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * MineQuiz 公开 API
 */
public final class MineQuizAPI {

    private static MineQuizAPI instance;
    private final MineQuiz plugin;

    private MineQuizAPI(MineQuiz plugin) {
        this.plugin = plugin;
    }

    public static synchronized void init(MineQuiz plugin) {
        if (instance == null) {
            instance = new MineQuizAPI(plugin);
        }
    }

    public static synchronized MineQuizAPI getInstance() {
        return instance;
    }

    public static synchronized void shutdown() {
        instance = null;
    }

    /**
     * 获取指定玩家的总得分
     */
    public int getPlayerScore(Player player) {
        QuizManager mgr = QuizManager.getInstance();
        return mgr != null ? mgr.getScore(player.getUniqueId()) : 0;
    }

    /**
     * 获取所有玩家得分
     */
    public Map<UUID, Integer> getAllScores() {
        QuizManager mgr = QuizManager.getInstance();
        return mgr != null ? mgr.getScores() : Map.of();
    }

    /**
     * 是否正在运行问答
     */
    public boolean isQuizRunning() {
        QuizManager mgr = QuizManager.getInstance();
        return mgr != null && mgr.isRunning();
    }

    /**
     * 强制开始一轮问答
     */
    public void forceStartQuiz() {
        QuizManager mgr = QuizManager.getInstance();
        if (mgr != null) mgr.forceStart();
    }

    /**
     * 重载配置
     */
    public void reload() {
        plugin.reloadQuiz();
    }

    /**
     * 获取题库大小
     */
    public int getQuestionCount() {
        return plugin.getQuestionBank().size();
    }

    /**
     * 获取当前配置
     */
    public QuizConfig getConfig() {
        return plugin.getQuizConfig();
    }

    /**
     * 获取题库
     */
    public QuestionBank getQuestionBank() {
        return plugin.getQuestionBank();
    }
}

package dev.onelili.minequiz.config;

import dev.onelili.minequiz.MineQuiz;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 插件主配置 — 读取 config.yml
 */
public class QuizConfig {

    private final MineQuiz plugin;
    private int quizInterval;
    private int answerTime;
    private int racePoints;
    private int quizPoints;
    private int betweenRoundsDelay;
    private int minPlayers;

    public QuizConfig(MineQuiz plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        quizInterval = cfg.getInt("quiz-interval", 5) * 60;
        answerTime = cfg.getInt("answer-time", 30);
        racePoints = cfg.getInt("race-points", 10);
        quizPoints = cfg.getInt("quiz-points", 5);
        betweenRoundsDelay = cfg.getInt("between-rounds-delay", 15);
        minPlayers = cfg.getInt("min-players", 2);
    }

    /** 定时问答间隔（配置文件中为分钟，转为秒存储） */
    public int getQuizInterval() { return quizInterval; }
    /** 每题答题时间（秒） */
    public int getAnswerTime() { return answerTime; }
    /** 抢答题每题得分 */
    public int getRacePoints() { return racePoints; }
    /** 问答题每题得分 */
    public int getQuizPoints() { return quizPoints; }
    /** 两轮之间间隔（秒） */
    public int getBetweenRoundsDelay() { return betweenRoundsDelay; }
    /** 开启答题的最低在线玩家数 */
    public int getMinPlayers() { return minPlayers; }
}

package dev.onelili.minequiz.config;

import dev.onelili.minequiz.MineQuiz;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 插件主配置 — 读取 config.yml
 */
public class QuizConfig {

    private final MineQuiz plugin;
    private int questionsPerRound;
    private int roundInterval;
    private int answerTime;
    private int betweenQuestionsDelay;
    private int racePoints;
    private int quizPoints;
    private int minPlayers;

    public QuizConfig(MineQuiz plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        questionsPerRound = cfg.getInt("questions-per-round", 5);
        roundInterval = cfg.getInt("round-interval", 30) * 60; // 分钟 → 秒
        answerTime = cfg.getInt("answer-time", 30);
        betweenQuestionsDelay = cfg.getInt("between-questions-delay", 10);
        racePoints = cfg.getInt("race-points", 10);
        quizPoints = cfg.getInt("quiz-points", 5);
        minPlayers = cfg.getInt("min-players", 2);
    }

    /** 每轮题目数量 */
    public int getQuestionsPerRound() { return questionsPerRound; }
    /** 轮次间隔（秒，配置文件中为分钟） */
    public int getRoundInterval() { return roundInterval; }
    /** 每题答题时间（秒） */
    public int getAnswerTime() { return answerTime; }
    /** 每题之间间隔（秒） */
    public int getBetweenQuestionsDelay() { return betweenQuestionsDelay; }
    /** 抢答题每题得分 */
    public int getRacePoints() { return racePoints; }
    /** 问答题每题得分 */
    public int getQuizPoints() { return quizPoints; }
    /** 开启答题的最低在线玩家数 */
    public int getMinPlayers() { return minPlayers; }
}

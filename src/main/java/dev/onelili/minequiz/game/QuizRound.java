package dev.onelili.minequiz.game;

import dev.onelili.minequiz.MineQuiz;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;

/**
 * 单轮问答游戏 — 管理一道题目的出题、作答、计时
 */
public class QuizRound {

    private final MineQuiz plugin;
    private final Question question;
    private final int answerTimeSeconds;
    private final int points;

    // 抢答题：已有人答对
    private boolean answered;
    // 问答题：已作答的玩家
    private final Set<UUID> answeredPlayers = new HashSet<>();

    // 本题当前状态
    private volatile boolean active = true;

    public QuizRound(MineQuiz plugin, Question question, int answerTimeSeconds, int points) {
        this.plugin = plugin;
        this.question = question;
        this.answerTimeSeconds = answerTimeSeconds;
        this.points = points;
    }

    /**
     * 开始出题 — 广播题目和可点击选项
     */
    public void start() {
        var lang = plugin.getLang();
        var server = plugin.getServer();

        // 广播题目头
        if (question.getType() == QuestionType.RACE) {
            lang.broadcast("race-announce",
                    "category", question.getCategory(),
                    "question", question.getQuestion());
        } else {
            lang.broadcast("quiz-announce",
                    "category", question.getCategory(),
                    "question", question.getQuestion());
        }

        // 广播选项（可点击）
        server.broadcast(lang.getNoPrefix("answer-hint"));
        List<String> options = question.getOptions();
        var labelBuilder = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) labelBuilder.append("   ");
            // 使用字母标签 A/B/C/D...
            char label = (char) ('A' + i);
            labelBuilder.append(label).append(". ").append(options.get(i));
        }
        String labels = labelBuilder.toString();

        // 构造可点击选项行
        var line = Component.text("  ");
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) {
                line = line.append(Component.text("    "));
            }
            char label = (char) ('A' + i);
            String clickCommand = "/quizanswer " + question.getId() + " " + i;
            var optionComponent = Component.text("[" + label + "] " + options.get(i))
                    .color(NamedTextColor.AQUA)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.runCommand(clickCommand));
            line = line.append(optionComponent);
        }
        server.broadcast(line);

        // 定时结束
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!active) return;
            active = false;
            endRound();
        }, answerTimeSeconds * 20L);
    }

    /**
     * 玩家提交答案
     * @return true 表示回答正确且有效（抢答题为第一个答对，问答题为首次答对）
     */
    public boolean submitAnswer(UUID playerUuid, int optionIndex) {
        if (!active) return false;

        if (question.getType() == QuestionType.RACE) {
            // 抢答题：仅第一位答对者得分
            if (answered) return false;
            if (!question.isCorrect(optionIndex)) return false;
            answered = true;
            active = false;
            return true;
        } else {
            // 问答题：所有答对者均得分，但每人只能答一次
            if (answeredPlayers.contains(playerUuid)) return false;
            if (!question.isCorrect(optionIndex)) return false;
            answeredPlayers.add(playerUuid);
            return true;
        }
    }

    /**
     * 检查该玩家是否已经答过本题（问答题防重复）
     */
    public boolean hasAnswered(UUID playerUuid) {
        return answeredPlayers.contains(playerUuid);
    }

    /**
     * 是否已有人答对（抢答题）
     */
    public boolean isAnswered() {
        return answered;
    }

    /**
     * 本轮是否仍在进行中
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 结束本轮，广播正确答案
     */
    private void endRound() {
        List<String> options = question.getOptions();
        int correctIdx = question.getAnswer();
        char correctLabel = (char) ('A' + correctIdx);
        String answerStr = correctLabel + ". " + options.get(correctIdx);

        String key = question.getType() == QuestionType.RACE ? "race-timeout" : "quiz-timeout";
        plugin.getLang().broadcast(key, "answer", answerStr);
    }

    public Question getQuestion() { return question; }
    public int getPoints() { return points; }
}

package dev.onelili.minequiz.game;

import dev.onelili.minequiz.MineQuiz;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;

/**
 * 单道题目的问答环节 — 管理一道题目的出题、作答、计时
 */
public class QuizRound {

    public enum SubmitResult {
        /** 答案正确，已记录 */
        CORRECT,
        /** 答案错误，已记录 */
        WRONG,
        /** 该玩家已经提交过答案 */
        ALREADY_SUBMITTED,
        /** 本轮答题已结束 */
        ROUND_ENDED
    }

    private final MineQuiz plugin;
    private final Question question;
    /** 随机分配的题目类型（不再使用 Question.type） */
    private final QuestionType randomizedType;
    private final int answerTimeSeconds;
    private final int points;
    /** 本道题结束后的回调（用于管理器启动下一题） */
    private final Runnable onEnd;

    /** 已提交答案的所有玩家（无论对错，用于"每人一次"限制） */
    private final Set<UUID> allSubmissions = new HashSet<>();
    /** 答对本题的玩家 */
    private final Set<UUID> allCorrectPlayers = new HashSet<>();
    /** 抢答题：最先答对的玩家 */
    private UUID firstCorrectPlayer;

    private volatile boolean active = true;
    /** 防止 endRound 被重复调用 */
    private boolean ended;

    public QuizRound(MineQuiz plugin, Question question, int answerTimeSeconds, int points,
                     QuestionType randomizedType, Runnable onEnd) {
        this.plugin = plugin;
        this.question = question;
        this.answerTimeSeconds = answerTimeSeconds;
        this.points = points;
        this.randomizedType = randomizedType;
        this.onEnd = onEnd;
    }

    /**
     * 开始出题 — 广播题目和可点击选项
     */
    public void start() {
        var lang = plugin.getLang();
        var server = plugin.getServer();

        // 广播题目头（使用随机分配的类型）
        if (randomizedType == QuestionType.RACE) {
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
     * 玩家提交答案（每人每轮仅限一次）
     * <p>
     * 抢答题：选中正确答案即立即结束本题，公布结果；答错则记录但不影响其他玩家继续抢答。
     * 问答题：答题时间结束后统一公布结果。
     */
    public SubmitResult submitAnswer(UUID playerUuid, int optionIndex) {
        if (!active) return SubmitResult.ROUND_ENDED;
        if (allSubmissions.contains(playerUuid)) return SubmitResult.ALREADY_SUBMITTED;

        allSubmissions.add(playerUuid);

        if (!question.isCorrect(optionIndex)) {
            return SubmitResult.WRONG;
        }

        allCorrectPlayers.add(playerUuid);

        if (randomizedType == QuestionType.RACE) {
            firstCorrectPlayer = playerUuid;
            // 抢答题：有人答对立即结束，延迟 1 tick 保证调用链返回后再执行 endRound
            active = false;
            plugin.getServer().getScheduler().runTaskLater(plugin, this::endRound, 1);
            return SubmitResult.CORRECT;
        }

        // 问答题：仅记录，等时间到统一公布
        return SubmitResult.CORRECT;
    }

    /**
     * 结束本题，公布正确答案和答对玩家
     */
    private void endRound() {
        if (ended) return;
        ended = true;
        List<String> options = question.getOptions();
        int correctIdx = question.getAnswer();
        char correctLabel = (char) ('A' + correctIdx);
        String answerStr = correctLabel + ". " + options.get(correctIdx);

        if (randomizedType == QuestionType.RACE) {
            if (firstCorrectPlayer != null) {
                plugin.getLang().broadcast("race-answer", "answer", answerStr);
                String name = plugin.getServer().getOfflinePlayer(firstCorrectPlayer).getName();
                if (name == null) name = firstCorrectPlayer.toString();
                plugin.getLang().broadcast("race-correct-first", "player", name, "points", String.valueOf(points));
            } else {
                plugin.getLang().broadcast("race-timeout", "answer", answerStr);
            }
        } else {
            if (!allCorrectPlayers.isEmpty()) {
                plugin.getLang().broadcast("quiz-answer", "answer", answerStr);
                for (UUID uuid : allCorrectPlayers) {
                    String name = plugin.getServer().getOfflinePlayer(uuid).getName();
                    if (name == null) name = uuid.toString();
                    plugin.getLang().broadcast("quiz-correct", "player", name, "points", String.valueOf(points));
                }
            } else {
                plugin.getLang().broadcast("quiz-timeout", "answer", answerStr);
            }
        }

        // 通知管理器继续下一题或结束本轮
        if (onEnd != null) {
            onEnd.run();
        }
    }

    public boolean hasAnswered(UUID playerUuid) {
        return allSubmissions.contains(playerUuid);
    }

    public boolean isAnswered() {
        return firstCorrectPlayer != null;
    }

    public boolean isActive() {
        return active;
    }

    public Question getQuestion() { return question; }
    public int getPoints() { return points; }
    public QuestionType getRandomizedType() { return randomizedType; }
}

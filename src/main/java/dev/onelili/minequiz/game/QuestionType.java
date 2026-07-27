package dev.onelili.minequiz.game;

/**
 * 题目类型枚举
 */
public enum QuestionType {
    /**
     * 抢答题 — 仅第一位回答正确的玩家得分
     */
    RACE,
    /**
     * 问答题 — 所有回答正确的玩家均得分
     */
    QUIZ
}

package dev.onelili.minequiz.game;

import java.util.List;

/**
 * 单道题目模型
 */
public class Question {
    private final int id;
    private final QuestionType type;
    private final String category;
    private final String question;
    private final List<String> options;
    private final int answer;

    public Question(int id, QuestionType type, String category, String question, List<String> options, int answer) {
        this.id = id;
        this.type = type;
        this.category = category;
        this.question = question;
        this.options = options;
        this.answer = answer;
    }

    public int getId() { return id; }
    public QuestionType getType() { return type; }
    public String getCategory() { return category; }
    public String getQuestion() { return question; }
    public List<String> getOptions() { return options; }
    public int getAnswer() { return answer; }

    /**
     * 检测指定选项索引是否为正确答案
     */
    public boolean isCorrect(int optionIndex) {
        return optionIndex == answer;
    }
}

package dev.onelili.minequiz;

import dev.onelili.minequiz.api.MineQuizAPI;
import dev.onelili.minequiz.command.QuizAnswerCommand;
import dev.onelili.minequiz.command.QuizCommand;
import dev.onelili.minequiz.command.QuizScoreCommand;
import dev.onelili.minequiz.config.QuestionBank;
import dev.onelili.minequiz.config.QuizConfig;
import dev.onelili.minequiz.game.QuizManager;
import dev.onelili.minequiz.util.LangUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicBoolean;

public final class MineQuiz extends JavaPlugin {

    private static MineQuiz instance;

    private QuizConfig quizConfig;
    private QuestionBank questionBank;
    private LangUtil lang;
    private QuizCommand commandExecutor;
    private QuizAnswerCommand answerCommandExecutor;
    private QuizScoreCommand scoreCommandExecutor;
    private final AtomicBoolean operational = new AtomicBoolean();

    @Override
    public void onEnable() {
        try {
            instance = this;
            operational.set(false);

            getLogger().info("MineQuiz 知识竞赛插件加载中...");

            // 配置
            quizConfig = new QuizConfig(this);

            // 语言
            lang = new LangUtil(this);

            // 题库
            questionBank = new QuestionBank(this);
            questionBank.load();

            // 注册主命令 /quiz
            commandExecutor = new QuizCommand(this);
            PluginCommand quizCmd = getCommand("quiz");
            if (quizCmd != null) {
                quizCmd.setExecutor(commandExecutor);
                quizCmd.setTabCompleter(commandExecutor);
            }

            // 注册内部答题命令 /quizanswer（不显示在帮助/tab补全中）
            answerCommandExecutor = new QuizAnswerCommand(this);
            PluginCommand answerCmd = getCommand("quizanswer");
            if (answerCmd != null) {
                answerCmd.setExecutor(answerCommandExecutor);
            }

            // 注册分数管理命令 /quizscore
            scoreCommandExecutor = new QuizScoreCommand(this);
            PluginCommand scoreCmd = getCommand("quizscore");
            if (scoreCmd != null) {
                scoreCmd.setExecutor(scoreCommandExecutor);
                scoreCmd.setTabCompleter(scoreCommandExecutor);
            }

            // 初始化管理器
            QuizManager.init(this);

            // 启动自动问答
            QuizManager.getInstance().startAutoQuiz();

            // 初始化 API
            MineQuizAPI.init(this);
            operational.set(true);

            getLogger().info("MineQuiz 知识竞赛插件已启用！题库共 " + questionBank.size() + " 道题目");
        } catch (Exception e) {
            operational.set(false);
            getLogger().severe("MineQuiz 初始化失败: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        operational.set(false);

        try {
            if (QuizManager.getInstance() != null) {
                QuizManager.getInstance().stop();
            }
        } catch (Exception e) {
            getLogger().warning("停止问答管理器时出错: " + e.getMessage());
        }

        cleanup("关闭公开 API", MineQuizAPI::shutdown);
        cleanup("清空 QuizManager", QuizManager::shutdown);

        // 清理命令
        PluginCommand quiz = getCommand("quiz");
        if (quiz != null) {
            quiz.setExecutor(null);
            quiz.setTabCompleter(null);
        }
        PluginCommand answer = getCommand("quizanswer");
        if (answer != null) {
            answer.setExecutor(null);
        }
        PluginCommand score = getCommand("quizscore");
        if (score != null) {
            score.setExecutor(null);
            score.setTabCompleter(null);
        }

        commandExecutor = null;
        answerCommandExecutor = null;
        scoreCommandExecutor = null;
        questionBank = null;
        quizConfig = null;
        lang = null;
        instance = null;

        getLogger().info("MineQuiz 知识竞赛插件已禁用");
    }

    /**
     * 重载配置和题库
     */
    public void reloadQuiz() {
        quizConfig.reload();
        questionBank.load();
    }

    private void cleanup(String action, Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException e) {
            getLogger().warning(action + " 失败: " + e.getMessage());
        }
    }

    public static MineQuiz getInstance() { return instance; }
    public QuizConfig getQuizConfig() { return quizConfig; }
    public QuestionBank getQuestionBank() { return questionBank; }
    public LangUtil getLang() { return lang; }
    public boolean isOperational() { return operational.get() && isEnabled(); }
}

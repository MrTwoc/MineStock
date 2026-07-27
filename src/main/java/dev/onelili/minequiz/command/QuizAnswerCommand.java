package dev.onelili.minequiz.command;

import dev.onelili.minequiz.MineQuiz;
import dev.onelili.minequiz.game.QuizManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 内部命令处理器 — 处理聊天框点击触发的 /quizanswer <questionId> <optionIndex>
 * 该命令不可手动输入，只通过聊天框可点击文本触发
 */
public class QuizAnswerCommand implements CommandExecutor {

    private final MineQuiz plugin;

    public QuizAnswerCommand(MineQuiz plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该命令只能由玩家执行");
            return true;
        }

        if (args.length < 2) return true;

        try {
            int questionId = Integer.parseInt(args[0]);
            int optionIndex = Integer.parseInt(args[1]);

            QuizManager mgr = QuizManager.getInstance();
            if (mgr == null) return true;

            mgr.handleAnswer(player.getUniqueId(), questionId, optionIndex);
        } catch (NumberFormatException ignored) {
            // 忽略无效参数
        }
        return true;
    }
}

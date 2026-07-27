package dev.onelili.minequiz.command;

import dev.onelili.minequiz.MineQuiz;
import dev.onelili.minequiz.game.QuizManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /quiz 管理命令处理器
 */
public class QuizCommand implements CommandExecutor, TabCompleter {

    private final MineQuiz plugin;

    public QuizCommand(MineQuiz plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("minequiz.admin")) {
            plugin.getLang().sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            plugin.getLang().sendMessage(sender, "quiz-usage");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (!(sender instanceof Player)) {
                    plugin.getLang().sendMessage(sender, "only-player");
                    return true;
                }
                QuizManager mgr = QuizManager.getInstance();
                if (mgr == null) {
                    sender.sendMessage("§cQuizManager 未初始化");
                    return true;
                }
                mgr.forceStart();
                plugin.getLang().broadcast("quiz-start",
                        "player", sender.getName());
            }
            case "reload" -> {
                plugin.reloadQuiz();
                int count = plugin.getQuestionBank().size();
                plugin.getLang().sendMessage(sender, "quiz-reloaded",
                        "count", String.valueOf(count));
            }
            default ->
                plugin.getLang().sendMessage(sender, "quiz-usage");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("minequiz.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return Arrays.asList("start", "reload");
        }
        return Collections.emptyList();
    }
}

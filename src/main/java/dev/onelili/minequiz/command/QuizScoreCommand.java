package dev.onelili.minequiz.command;

import dev.onelili.minequiz.MineQuiz;
import dev.onelili.minequiz.game.QuizManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /quizscore 分数管理命令
 */
public class QuizScoreCommand implements CommandExecutor, TabCompleter {

    private final MineQuiz plugin;

    public QuizScoreCommand(MineQuiz plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("minequiz.admin")) {
            plugin.getLang().sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 1) {
            plugin.getLang().sendMessage(sender, "quizscore-usage");
            return true;
        }

        // 查找目标玩家
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            plugin.getLang().sendMessage(sender, "quizscore-player-not-found", "player", args[0]);
            return true;
        }

        QuizManager mgr = QuizManager.getInstance();
        if (mgr == null) {
            sender.sendMessage("§cQuizManager 未初始化");
            return true;
        }

        // 仅查看分数
        if (args.length == 1) {
            int score = mgr.getScore(target.getUniqueId());
            String name = target.getName() != null ? target.getName() : args[0];
            plugin.getLang().sendMessage(sender, "quizscore-query",
                    "player", name,
                    "score", String.valueOf(score));
            return true;
        }

        if (args.length < 3) {
            plugin.getLang().sendMessage(sender, "quizscore-usage");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            plugin.getLang().sendMessage(sender, "quizscore-invalid-amount", "amount", args[2]);
            return true;
        }

        String name = target.getName() != null ? target.getName() : args[0];

        switch (args[1].toLowerCase()) {
            case "add" -> {
                int current = mgr.getScore(target.getUniqueId());
                mgr.setScore(target.getUniqueId(), current + amount);
                plugin.getLang().sendMessage(sender, "quizscore-add",
                        "player", name,
                        "amount", String.valueOf(amount),
                        "score", String.valueOf(mgr.getScore(target.getUniqueId())));
            }
            case "remove" -> {
                int current = mgr.getScore(target.getUniqueId());
                int newScore = Math.max(0, current - amount);
                mgr.setScore(target.getUniqueId(), newScore);
                plugin.getLang().sendMessage(sender, "quizscore-remove",
                        "player", name,
                        "amount", String.valueOf(amount),
                        "score", String.valueOf(newScore));
            }
            case "set" -> {
                mgr.setScore(target.getUniqueId(), amount);
                plugin.getLang().sendMessage(sender, "quizscore-set",
                        "player", name,
                        "score", String.valueOf(amount));
            }
            default ->
                plugin.getLang().sendMessage(sender, "quizscore-usage");
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
            // 返回在线玩家名
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return Arrays.asList("add", "remove", "set");
        }

        if (args.length == 3) {
            List<String> nums = new ArrayList<>();
            if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")) {
                nums.add("10");
                nums.add("50");
                nums.add("100");
            } else if (args[1].equalsIgnoreCase("set")) {
                nums.add("0");
                nums.add("100");
            }
            return nums;
        }

        return Collections.emptyList();
    }
}

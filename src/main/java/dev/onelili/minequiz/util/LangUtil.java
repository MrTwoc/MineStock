package dev.onelili.minequiz.util;

import dev.onelili.minequiz.MineQuiz;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LangUtil {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private FileConfiguration lang;
    private String prefix;

    public LangUtil(MineQuiz plugin) {
        File file = new File(plugin.getDataFolder(), "lang.yml");
        if (file.exists()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                YamlConfiguration test = new YamlConfiguration();
                test.loadFromString(content);
                lang = test;
            } catch (InvalidConfigurationException e) {
                plugin.getLogger().warning("[MineQuiz] lang.yml 语法错误，用默认文件覆盖: " + e.getMessage());
                plugin.saveResource("lang.yml", true);
                lang = YamlConfiguration.loadConfiguration(file);
            } catch (Exception e) {
                plugin.getLogger().warning("[MineQuiz] lang.yml 读取失败，用默认文件覆盖: " + e.getMessage());
                plugin.saveResource("lang.yml", true);
                lang = YamlConfiguration.loadConfiguration(file);
            }
        } else {
            plugin.saveResource("lang.yml", false);
            lang = YamlConfiguration.loadConfiguration(file);
        }
        InputStream defaultStream = plugin.getResource("lang.yml");
        if (defaultStream != null) {
            lang.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)));
        }
        prefix = lang.getString("prefix", "<gold>[MineQuiz]</gold> ");
    }

    public String getRaw(String key) {
        return lang.getString(key, "<red>Missing lang key: " + key + "</red>");
    }

    public Component get(String key, String... kvPairs) {
        String raw = prefix + replacePairs(getRaw(key), kvPairs);
        return MM.deserialize(raw);
    }

    public Component getNoPrefix(String key, String... kvPairs) {
        String raw = replacePairs(getRaw(key), kvPairs);
        return MM.deserialize(raw);
    }

    public void send(Player player, String key, String... kvPairs) {
        player.sendMessage(get(key, kvPairs));
    }

    public void sendNoPrefix(Player player, String key, String... kvPairs) {
        player.sendMessage(getNoPrefix(key, kvPairs));
    }

    public void sendMessage(CommandSender sender, String key, String... kvPairs) {
        String raw = prefix + replacePairs(getRaw(key), kvPairs);
        sender.sendMessage(MM.deserialize(raw));
    }

    public void broadcast(String key, String... kvPairs) {
        String raw = prefix + replacePairs(getRaw(key), kvPairs);
        MineQuiz plugin = MineQuiz.getInstance();
        Component component = MM.deserialize(raw);
        if (plugin.getQuizConfig().isLogToConsole()) {
            plugin.getServer().broadcast(component);
        } else {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendMessage(component);
            }
        }
    }

    public void broadcastNoPrefix(String key, String... kvPairs) {
        String raw = replacePairs(getRaw(key), kvPairs);
        MineQuiz plugin = MineQuiz.getInstance();
        Component component = MM.deserialize(raw);
        if (plugin.getQuizConfig().isLogToConsole()) {
            plugin.getServer().broadcast(component);
        } else {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendMessage(component);
            }
        }
    }

    /**
     * 广播一个已经构造好的 Component 对象，受 log-to-console 控制
     */
    public void broadcastComponent(Component component) {
        MineQuiz plugin = MineQuiz.getInstance();
        if (plugin.getQuizConfig().isLogToConsole()) {
            plugin.getServer().broadcast(component);
        } else {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendMessage(component);
            }
        }
    }

    private String replacePairs(String raw, String[] kvPairs) {
        for (int i = 0; i + 1 < kvPairs.length; i += 2) {
            raw = raw.replace("{" + kvPairs[i] + "}", kvPairs[i + 1]);
        }
        return raw;
    }

    public static Component parse(String miniMessage) {
        return MM.deserialize(miniMessage);
    }
}

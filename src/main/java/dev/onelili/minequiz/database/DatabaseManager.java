package dev.onelili.minequiz.database;

import dev.onelili.minequiz.MineQuiz;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SQLite 数据库管理器 — 负责玩家分数的持久化存储
 */
public class DatabaseManager {

    private final MineQuiz plugin;
    private Connection connection;

    public DatabaseManager(MineQuiz plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化数据库连接并创建表
     */
    public void init() throws SQLException {
        File dbFile = new File(plugin.getDataFolder(), "data.db");
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        createTables();
        plugin.getLogger().info("[MineQuiz] SQLite 数据库已连接: " + dbFile.getAbsolutePath());
    }

    /**
     * 创建 scores 表
     */
    private void createTables() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS scores (
                    uuid TEXT PRIMARY KEY,
                    score INTEGER NOT NULL DEFAULT 0
                )
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * 从数据库加载所有玩家分数
     */
    public Map<UUID, Integer> loadAllScores() {
        Map<UUID, Integer> result = new HashMap<>();
        String sql = "SELECT uuid, score FROM scores";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                int score = rs.getInt("score");
                result.put(uuid, score);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[MineQuiz] 加载分数数据失败", e);
        }
        return result;
    }

    /**
     * 保存单个玩家的分数（不存在则插入，存在则更新）
     */
    public void saveScore(UUID playerUuid, int score) {
        if (connection == null) return;
        String sql = "INSERT INTO scores(uuid, score) VALUES(?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET score = excluded.score";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, score);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[MineQuiz] 保存玩家分数失败: " + playerUuid, e);
        }
    }

    /**
     * 批量保存所有玩家分数
     */
    public void saveAllScores(Map<UUID, Integer> scores) {
        if (connection == null) return;
        String sql = "INSERT INTO scores(uuid, score) VALUES(?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET score = excluded.score";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Map.Entry<UUID, Integer> entry : scores.entrySet()) {
                ps.setString(1, entry.getKey().toString());
                ps.setInt(2, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[MineQuiz] 批量保存分数失败", e);
        }
    }

    /**
     * 关闭数据库连接
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[MineQuiz] 关闭数据库连接失败", e);
            }
        }
    }
}

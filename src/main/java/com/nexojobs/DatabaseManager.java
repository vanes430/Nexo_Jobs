package com.nexojobs;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.*;

public class DatabaseManager {
    
    private final NexoJobs plugin;
    private HikariDataSource dataSource;
    private final String tablePrefix;
    private boolean connected = false;
    
    public DatabaseManager(NexoJobs plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.tablePrefix = config.getString("database.table-prefix", "nexojobs_");
        
        setupDatabase();
        if (connected) {
            createTables();
        }
    }
    
    private void setupDatabase() {
        FileConfiguration config = plugin.getConfig();
        
        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String database = config.getString("database.database", "minecraft");
        String username = config.getString("database.username", "root");
        String password = config.getString("database.password", "");
        boolean useSSL = config.getBoolean("database.use-ssl", false);
        
        try {
            HikariConfig hikariConfig = new HikariConfig();
            
            String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8&useUnicode=true",
                host, port, database, useSSL
            );
            
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

            hikariConfig.setMaximumPoolSize(config.getInt("database.pool.maximum-pool-size", 10));
            hikariConfig.setMinimumIdle(config.getInt("database.pool.minimum-idle", 2));
            hikariConfig.setMaxLifetime(config.getLong("database.pool.max-lifetime", 1800000));
            hikariConfig.setConnectionTimeout(config.getLong("database.pool.connection-timeout", 30000));
            hikariConfig.setIdleTimeout(config.getLong("database.pool.idle-timeout", 600000));
            
            hikariConfig.setConnectionTestQuery("SELECT 1");
            hikariConfig.setValidationTimeout(5000);

            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
            hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
            hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
            hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
            hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
            hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
            
            dataSource = new HikariDataSource(hikariConfig);
            
            try (Connection conn = dataSource.getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    connected = true;
                    plugin.getLogger().info("Successfully connected to MySQL database!");
                    plugin.getLogger().info("Database: " + database + " | Host: " + host + ":" + port);
                }
            }
            
        } catch (Exception e) {
            connected = false;
            plugin.getLogger().severe("═══════════════════════════════════════════");
            plugin.getLogger().severe("FAILED TO CONNECT TO DATABASE!");
            plugin.getLogger().severe("Error: " + e.getMessage());
            plugin.getLogger().severe("═══════════════════════════════════════════");
            plugin.getLogger().severe("Please check your database configuration:");
            plugin.getLogger().severe("  - Host: " + host);
            plugin.getLogger().severe("  - Port: " + port);
            plugin.getLogger().severe("  - Database: " + database);
            plugin.getLogger().severe("  - Username: " + username);
            plugin.getLogger().severe("═══════════════════════════════════════════");
            e.printStackTrace();
        }
    }
    
    private void createTables() {
        String playerDataTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "player_data (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "player_name VARCHAR(16) NOT NULL," +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "INDEX idx_player_name (player_name)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        
        String jobProgressTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "job_progress (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "uuid VARCHAR(36) NOT NULL," +
                "job_id VARCHAR(50) NOT NULL," +
                "level INT NOT NULL DEFAULT 1," +
                "exp INT NOT NULL DEFAULT 0," +
                "is_active BOOLEAN NOT NULL DEFAULT FALSE," +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "UNIQUE KEY unique_player_job (uuid, job_id)," +
                "INDEX idx_uuid (uuid)," +
                "INDEX idx_active (uuid, is_active)," +
                "FOREIGN KEY (uuid) REFERENCES " + tablePrefix + "player_data(uuid) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(playerDataTable);
            stmt.executeUpdate(jobProgressTable);
            
            plugin.getLogger().info("Database tables created/verified successfully!");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource is not initialized or has been closed!");
        }
        return dataSource.getConnection();
    }

    public void savePlayerData(UUID uuid, String playerName) {
        if (!connected) return;
        
        String sql = "INSERT INTO " + tablePrefix + "player_data (uuid, player_name) " +
                "VALUES (?, ?) ON DUPLICATE KEY UPDATE player_name = ?, last_updated = CURRENT_TIMESTAMP";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setString(3, playerName);
            stmt.executeUpdate();
            
            if (plugin.getConfig().getBoolean("logging.log-player-saves", true)) {
                plugin.getLogger().info("Saved player data: " + playerName);
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save player data for " + playerName + ": " + e.getMessage());
        }
    }

    public void saveJobProgress(UUID uuid, String jobId, int level, int exp, boolean isActive) {
        if (!connected) return;
        
        String sql = "INSERT INTO " + tablePrefix + "job_progress (uuid, job_id, level, exp, is_active) " +
                "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE level = ?, exp = ?, is_active = ?, last_updated = CURRENT_TIMESTAMP";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid.toString());
            stmt.setString(2, jobId);
            stmt.setInt(3, level);
            stmt.setInt(4, exp);
            stmt.setBoolean(5, isActive);
            stmt.setInt(6, level);
            stmt.setInt(7, exp);
            stmt.setBoolean(8, isActive);
            stmt.executeUpdate();
            
            if (plugin.getConfig().getBoolean("logging.log-database-updates", true)) {
                plugin.getLogger().info("Updated job progress: " + jobId + " for UUID: " + uuid.toString().substring(0, 8) + "...");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save job progress: " + e.getMessage());
        }
    }

    public Map<String, PlayerJobData> loadPlayerJobData(UUID uuid) {
        Map<String, PlayerJobData> jobData = new HashMap<>();
        if (!connected) return jobData;
        
        String sql = "SELECT job_id, level, exp FROM " + tablePrefix + "job_progress WHERE uuid = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String jobId = rs.getString("job_id");
                int level = rs.getInt("level");
                int exp = rs.getInt("exp");
                jobData.put(jobId, new PlayerJobData(jobId, level, exp));
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load job data for " + uuid + ": " + e.getMessage());
        }
        
        return jobData;
    }

    public Set<String> loadActiveJobs(UUID uuid) {
        Set<String> activeJobs = new HashSet<>();
        if (!connected) return activeJobs;
        
        String sql = "SELECT job_id FROM " + tablePrefix + "job_progress WHERE uuid = ? AND is_active = TRUE";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                activeJobs.add(rs.getString("job_id"));
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load active jobs for " + uuid + ": " + e.getMessage());
        }
        
        return activeJobs;
    }

    public void setJobActive(UUID uuid, String jobId, boolean active) {
        if (!connected) return;
        
        String sql = "UPDATE " + tablePrefix + "job_progress SET is_active = ? WHERE uuid = ? AND job_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, active);
            stmt.setString(2, uuid.toString());
            stmt.setString(3, jobId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set job active status: " + e.getMessage());
        }
    }

    public boolean deleteJobProgress(UUID uuid, String jobId) {
        if (!connected) return false;
        
        String sql = "DELETE FROM " + tablePrefix + "job_progress WHERE uuid = ? AND job_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid.toString());
            stmt.setString(2, jobId);
            int affected = stmt.executeUpdate();
            return affected > 0;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete job progress: " + e.getMessage());
            return false;
        }
    }

    public int deleteAllProgress(UUID uuid) {
        if (!connected) return 0;
        
        String sql = "DELETE FROM " + tablePrefix + "job_progress WHERE uuid = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid.toString());
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete all progress: " + e.getMessage());
            return 0;
        }
    }

    public Map<UUID, Map<String, PlayerJobData>> loadAllPlayerData() {
        Map<UUID, Map<String, PlayerJobData>> allData = new HashMap<>();
        if (!connected) return allData;
        
        String sql = "SELECT uuid, job_id, level, exp FROM " + tablePrefix + "job_progress";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String jobId = rs.getString("job_id");
                int level = rs.getInt("level");
                int exp = rs.getInt("exp");
                
                allData.computeIfAbsent(uuid, k -> new HashMap<>())
                       .put(jobId, new PlayerJobData(jobId, level, exp));
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load all player data: " + e.getMessage());
        }
        
        return allData;
    }

    public Map<UUID, Set<String>> loadAllActiveJobs() {
        Map<UUID, Set<String>> allActiveJobs = new HashMap<>();
        if (!connected) return allActiveJobs;
        
        String sql = "SELECT uuid, job_id FROM " + tablePrefix + "job_progress WHERE is_active = TRUE";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String jobId = rs.getString("job_id");
                
                allActiveJobs.computeIfAbsent(uuid, k -> new HashSet<>()).add(jobId);
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load all active jobs: " + e.getMessage());
        }
        
        return allActiveJobs;
    }

    public void batchSaveJobProgress(Map<UUID, Map<String, PlayerJobData>> allData, 
                                     Map<UUID, Set<String>> activeJobs) {
        if (!connected) return;
        
        String sql = "INSERT INTO " + tablePrefix + "job_progress (uuid, job_id, level, exp, is_active) " +
                "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE level = ?, exp = ?, is_active = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);
            
            int batchCount = 0;
            for (Map.Entry<UUID, Map<String, PlayerJobData>> playerEntry : allData.entrySet()) {
                UUID uuid = playerEntry.getKey();
                Set<String> playerActiveJobs = activeJobs.getOrDefault(uuid, new HashSet<>());
                
                for (Map.Entry<String, PlayerJobData> jobEntry : playerEntry.getValue().entrySet()) {
                    String jobId = jobEntry.getKey();
                    PlayerJobData data = jobEntry.getValue();
                    boolean isActive = playerActiveJobs.contains(jobId);
                    
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, jobId);
                    stmt.setInt(3, data.getLevel());
                    stmt.setInt(4, data.getExp());
                    stmt.setBoolean(5, isActive);
                    stmt.setInt(6, data.getLevel());
                    stmt.setInt(7, data.getExp());
                    stmt.setBoolean(8, isActive);
                    stmt.addBatch();
                    
                    batchCount++;
                    
                    if (batchCount % 100 == 0) {
                        stmt.executeBatch();
                    }
                }
            }
            
            stmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
            
            if (plugin.getConfig().getBoolean("logging.log-auto-saves", false)) {
                plugin.getLogger().info("Batch saved " + batchCount + " job progress entries.");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to batch save job progress: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            connected = false;
            plugin.getLogger().info("Database connection closed.");
        }
    }

    public boolean isConnected() {
        if (!connected || dataSource == null) return false;
        
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    public DatabaseStats getDatabaseStats() {
        DatabaseStats stats = new DatabaseStats();
        if (!connected) return stats;
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            String playerCountSql = "SELECT COUNT(*) as count FROM " + tablePrefix + "player_data";
            ResultSet rs = stmt.executeQuery(playerCountSql);
            if (rs.next()) {
                stats.totalPlayers = rs.getInt("count");
            }

            String jobCountSql = "SELECT COUNT(*) as count FROM " + tablePrefix + "job_progress";
            rs = stmt.executeQuery(jobCountSql);
            if (rs.next()) {
                stats.totalJobEntries = rs.getInt("count");
            }

            String activeJobsSql = "SELECT COUNT(*) as count FROM " + tablePrefix + "job_progress WHERE is_active = TRUE";
            rs = stmt.executeQuery(activeJobsSql);
            if (rs.next()) {
                stats.activeJobs = rs.getInt("count");
            }

            String topLevelsSql = "SELECT job_id, MAX(level) as max_level FROM " + tablePrefix + "job_progress GROUP BY job_id";
            rs = stmt.executeQuery(topLevelsSql);
            while (rs.next()) {
                stats.topLevels.put(rs.getString("job_id"), rs.getInt("max_level"));
            }
            
            if (dataSource != null && !dataSource.isClosed()) {
                stats.poolSize = dataSource.getHikariPoolMXBean().getTotalConnections();
                stats.activeConnections = dataSource.getHikariPoolMXBean().getActiveConnections();
                stats.idleConnections = dataSource.getHikariPoolMXBean().getIdleConnections();
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get database stats: " + e.getMessage());
        }
        
        return stats;
    }
    
    public List<String> getAllJobDataFormatted() {
        List<String> formatted = new ArrayList<>();
        if (!connected) {
            formatted.add("§c✗ Database not connected!");
            return formatted;
        }
        
        String sql = "SELECT pd.player_name, jp.job_id, jp.level, jp.exp, jp.is_active " +
                    "FROM " + tablePrefix + "job_progress jp " +
                    "JOIN " + tablePrefix + "player_data pd ON jp.uuid = pd.uuid " +
                    "ORDER BY pd.player_name, jp.job_id";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            String currentPlayer = "";
            while (rs.next()) {
                String playerName = rs.getString("player_name");
                String jobId = rs.getString("job_id");
                int level = rs.getInt("level");
                int exp = rs.getInt("exp");
                boolean isActive = rs.getBoolean("is_active");
                
                if (!playerName.equals(currentPlayer)) {
                    if (!currentPlayer.isEmpty()) {
                        formatted.add("");
                    }
                    formatted.add("§e§l" + playerName + ":");
                    currentPlayer = playerName;
                }
                
                String status = isActive ? "§a§lACTIVE" : "§7PAUSED";
                formatted.add("  §8▸ §6" + jobId + " §7- Level: §e" + level + " §7| EXP: §e" + exp + " §7| " + status);
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get formatted job data: " + e.getMessage());
            formatted.add("§cError retrieving data from database!");
        }
        
        return formatted;
    }
    
    public static class DatabaseStats {
        public int totalPlayers = 0;
        public int totalJobEntries = 0;
        public int activeJobs = 0;
        public Map<String, Integer> topLevels = new HashMap<>();
        public int poolSize = 0;
        public int activeConnections = 0;
        public int idleConnections = 0;
    }
}
package com.nexojobs;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JobManager {

    private final NexoJobs plugin;
    private final DatabaseManager databaseManager;
    private final Map<String, Job> jobs;
    private final Map<UUID, Map<String, PlayerJobData>> playerJobData;
    private final Map<UUID, Set<String>> activeJobs;

    private final Set<UUID> dirtyPlayers;
    private final Map<UUID, Long> lastSaveTime; 
    public JobManager(NexoJobs plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.jobs = new HashMap<>();
        this.playerJobData = new ConcurrentHashMap<>();
        this.activeJobs = new ConcurrentHashMap<>();
        this.dirtyPlayers = ConcurrentHashMap.newKeySet();
        this.lastSaveTime = new ConcurrentHashMap<>();
        
        loadJobs();
        loadAllDataFromDatabase();

        startAutoSaveTask();
    }

    private void startAutoSaveTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            saveDirtyPlayers();
        }, 6000L, 6000L); 
    }

    public void loadJobs() {
        jobs.clear();
        ConfigurationSection section = plugin.getConfigManager().getJobsConfig().getConfigurationSection("jobs");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection jobSection = section.getConfigurationSection(id);
            if (jobSection == null) continue;

            String displayName = jobSection.getString("display-name", id);
            String description = jobSection.getString("description", "");
            Material icon = Material.valueOf(jobSection.getString("icon", "STONE"));
            int maxLevel = jobSection.getInt("max-level", 10);

            Job job = new Job(id, displayName, description, icon, maxLevel);

            ConfigurationSection levelsSection = jobSection.getConfigurationSection("levels");
            if (levelsSection != null) {
                for (String lvlStr : levelsSection.getKeys(false)) {
                    int lvl = Integer.parseInt(lvlStr);
                    int expReq = levelsSection.getInt(lvlStr + ".exp-required");
                    double reward = levelsSection.getDouble(lvlStr + ".money-reward");
                    job.addLevel(lvl, new JobLevel(lvl, expReq, reward));
                }
            }

            ConfigurationSection actionsSection = jobSection.getConfigurationSection("actions");
            if (actionsSection != null) {
                for (String action : actionsSection.getKeys(false)) {
                    job.addAction(action, actionsSection.getInt(action + ".exp"));
                }
            }

            jobs.put(id.toLowerCase(), job);
        }
    }

    private void loadAllDataFromDatabase() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            playerJobData.putAll(databaseManager.loadAllPlayerData());
            activeJobs.putAll(databaseManager.loadAllActiveJobs());
            plugin.getLogger().info("Loaded job data for " + playerJobData.size() + " players from database");
        });
    }

    public void reloadAll() {
        loadJobs();
    }

    public Job getJob(String id) {
        return jobs.get(id.toLowerCase());
    }

    public Collection<Job> getAllJobs() {
        return jobs.values();
    }

    public PlayerJobData getJobData(Player player, String jobId) {
        return getJobData(player.getUniqueId(), jobId);
    }

    public PlayerJobData getJobData(UUID uuid, String jobId) {
        Map<String, PlayerJobData> data = playerJobData.get(uuid);
        return data != null ? data.get(jobId.toLowerCase()) : null;
    }

    public Set<String> getActiveJobIds(Player player) {
        return activeJobs.getOrDefault(player.getUniqueId(), new HashSet<>());
    }

    public String getActiveJobId(Player player) {
        Set<String> active = getActiveJobIds(player);
        return active.isEmpty() ? null : active.iterator().next();
    }

    public PlayerJobData getActiveJobData(Player player) {
        String activeId = getActiveJobId(player);
        return activeId != null ? getJobData(player, activeId) : null;
    }

    public int getActiveJobCount(Player player) {
        return getActiveJobIds(player).size();
    }

    public boolean hasActiveJob(Player player) {
        return !getActiveJobIds(player).isEmpty();
    }

    public boolean hasJobActive(Player player, String jobId) {
        return getActiveJobIds(player).contains(jobId.toLowerCase());
    }

    public int getJobLimit(Player player) {
        if (player.hasPermission("nexojobs.usejobs.limit.unlimited")) return Integer.MAX_VALUE;
        
        for (int i = 10; i >= 1; i--) {
            if (player.hasPermission("nexojobs.usejobs.limit." + i)) return i;
        }
        
        return plugin.getConfig().getInt("default-job-limit", 1);
    }

    public boolean canJoinMoreJobs(Player player) {
        return getActiveJobCount(player) < getJobLimit(player);
    }

    public boolean joinJob(Player player, String jobId) {
        String id = jobId.toLowerCase();
        if (!jobs.containsKey(id)) return false;
        if (hasJobActive(player, id)) return false;
        if (!canJoinMoreJobs(player)) return false;

        UUID uuid = player.getUniqueId();
        
        Map<String, PlayerJobData> pData = playerJobData.computeIfAbsent(uuid, k -> new HashMap<>());
        if (!pData.containsKey(id)) {
            pData.put(id, new PlayerJobData(id, 1, 0));
        }

        activeJobs.computeIfAbsent(uuid, k -> new HashSet<>()).add(id);

        markDirty(uuid);
        savePlayerAsync(player, id);
        
        return true;
    }

    public boolean leaveJob(Player player, String jobId) {
        String id = jobId.toLowerCase();
        UUID uuid = player.getUniqueId();
        
        if (!hasJobActive(player, id)) return false;

        activeJobs.get(uuid).remove(id);

        markDirty(uuid);
        savePlayerAsync(player, id);
        
        return true;
    }

    public void leaveAllJobs(Player player) {
        UUID uuid = player.getUniqueId();
        Set<String> active = activeJobs.get(uuid);
        if (active == null) return;

        for (String jobId : new HashSet<>(active)) {
            leaveJob(player, jobId);
        }
    }

    public void addJobExp(Player player, String jobId, int expAmount) {
        UUID uuid = player.getUniqueId();
        PlayerJobData data = getJobData(uuid, jobId);
        
        if (data == null) return;
        
        data.addExp(expAmount);

        markDirty(uuid);
    }

    private void markDirty(UUID uuid) {
        dirtyPlayers.add(uuid);
    }

    private void savePlayerAsync(Player player, String jobId) {
        final UUID uuid = player.getUniqueId();
        final String finalJobId = jobId;
        final String playerName = player.getName();
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerJobData data = getJobData(uuid, finalJobId);
            if (data == null) return;
            
            databaseManager.savePlayerData(uuid, playerName);
            databaseManager.saveJobProgress(
                uuid, 
                finalJobId, 
                data.getLevel(), 
                data.getExp(), 
                activeJobs.getOrDefault(uuid, new HashSet<>()).contains(finalJobId)
            );
        });
    }

    public void saveDirtyPlayers() {
        if (dirtyPlayers.isEmpty()) return;
        
        Set<UUID> toSave = new HashSet<>(dirtyPlayers);
        dirtyPlayers.clear();
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<UUID, Map<String, PlayerJobData>> dataToSave = new HashMap<>();
            Map<UUID, Set<String>> activeToSave = new HashMap<>();
            
            for (UUID uuid : toSave) {
                Map<String, PlayerJobData> playerData = playerJobData.get(uuid);
                Set<String> playerActiveJobs = activeJobs.get(uuid);
                
                if (playerData != null) {
                    dataToSave.put(uuid, new HashMap<>(playerData));
                }
                if (playerActiveJobs != null) {
                    activeToSave.put(uuid, new HashSet<>(playerActiveJobs));
                }
            }
            
            if (!dataToSave.isEmpty()) {
                databaseManager.batchSaveJobProgress(dataToSave, activeToSave);
                
                if (plugin.getConfig().getBoolean("logging.log-auto-saves", false)) {
                    plugin.getLogger().info("Auto-saved " + toSave.size() + " players to database");
                }
            }
        });
    }

    public void savePlayerOnLogout(Player player) {
        final UUID uuid = player.getUniqueId();
        final String playerName = player.getName();

        dirtyPlayers.remove(uuid);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, PlayerJobData> pData = playerJobData.get(uuid);
            Set<String> active = activeJobs.get(uuid);
            
            if (pData != null) {
                databaseManager.savePlayerData(uuid, playerName);
                
                for (Map.Entry<String, PlayerJobData> entry : pData.entrySet()) {
                    String jobId = entry.getKey();
                    PlayerJobData data = entry.getValue();
                    boolean isActive = active != null && active.contains(jobId);
                    
                    databaseManager.saveJobProgress(uuid, jobId, data.getLevel(), data.getExp(), isActive);
                }
            }
        });
    }

    public void saveAllPlayers() {
        
        Map<UUID, Map<String, PlayerJobData>> dataToSave = new HashMap<>(playerJobData);
        Map<UUID, Set<String>> activeToSave = new HashMap<>(activeJobs);
        
        databaseManager.batchSaveJobProgress(dataToSave, activeToSave);
        dirtyPlayers.clear();
    }

    public boolean setPlayerJobProgress(UUID uuid, String name, String jobId, int level, int exp) {
        final String id = jobId.toLowerCase();
        if (!jobs.containsKey(id)) return false;

        Map<String, PlayerJobData> pData = playerJobData.computeIfAbsent(uuid, k -> new HashMap<>());
        PlayerJobData data = pData.get(id);
        
        if (data == null) {
            data = new PlayerJobData(id, level, exp);
            pData.put(id, data);
        } else {
            data.setLevel(level);
            data.setExp(exp);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            databaseManager.savePlayerData(uuid, name);
            databaseManager.saveJobProgress(uuid, id, level, exp, 
                activeJobs.getOrDefault(uuid, new HashSet<>()).contains(id));
        });
        
        return true;
    }

    public boolean setPlayerJobLevel(UUID uuid, String name, String jobId, int level) {
        return setPlayerJobProgress(uuid, name, jobId, level, 0);
    }

    public boolean resetJobProgress(Player player, String jobId) {
        final String id = jobId.toLowerCase();
        final UUID uuid = player.getUniqueId();
        
        Map<String, PlayerJobData> pData = playerJobData.get(uuid);
        if (pData != null) {
            pData.remove(id);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            databaseManager.deleteJobProgress(uuid, id);
        });
        
        return true;
    }

    public int resetAllProgress(UUID uuid) {
        playerJobData.remove(uuid);
        activeJobs.remove(uuid);
        dirtyPlayers.remove(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            databaseManager.deleteAllProgress(uuid);
        });
        
        return 0;
    }

    public void savePlayerJobData(Player player, String jobId) {
        savePlayerAsync(player, jobId);
    }
}
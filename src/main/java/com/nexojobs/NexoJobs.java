package com.nexojobs;

import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class NexoJobs extends JavaPlugin {
    
    private static NexoJobs instance;
    private JobManager jobManager;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private JobMenuGUI jobMenuGUI;
    private Economy economy = null;
    
    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        try {
            databaseManager = new DatabaseManager(this);
            getLogger().info("Database initialized successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize database! Plugin will be disabled.");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        jobManager = new JobManager(this, databaseManager);
        
        jobMenuGUI = new JobMenuGUI(this);
        Bukkit.getPluginManager().registerEvents(jobMenuGUI, this);
        Bukkit.getPluginManager().registerEvents(new JobListener(this), this);
        
        getCommand("jobs").setExecutor(new JobsCommand(this));
        getCommand("jobs").setTabCompleter(new JobsCommand(this));
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new JobsPlaceholder(this).register();
            getLogger().info("PlaceholderAPI hooked successfully!");
        }
        
        getLogger().info("================================");
        getLogger().info("NexoJobs v1.5.3 OPTIMIZED");
        getLogger().info("- Async Database Operations ✓");
        getLogger().info("- Smart Caching System ✓");
        getLogger().info("- Optimized Event Listeners ✓");
        getLogger().info("- Fixed Farmer Job ✓");
        getLogger().info("================================");
    } 
    
    @Override
    public void onDisable() {
        if (jobManager != null) {
            getLogger().info("Saving all player data...");
            
            jobManager.saveDirtyPlayers();
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            jobManager.saveAllPlayers();
            getLogger().info("Player data saved successfully!");
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("NexoJobs disabled!");
    }
    
    public Economy getEconomy() {
        return economy;
    }
    
    public static NexoJobs getInstance() {
        return instance;
    }
    
    public JobManager getJobManager() {
        return jobManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public JobMenuGUI getJobMenuGUI() {
        return jobMenuGUI;
    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', 
            getConfig().getString("prefix", "&b&lNEXOJOBS &8» &7"));
    }
    
    public String getMessage(String path) {
        String msg = getConfig().getString("messages." + path);
        if (msg == null) return "§cMessage missing: " + path;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
    
    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
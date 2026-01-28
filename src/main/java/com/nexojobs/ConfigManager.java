package com.nexojobs;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

public class ConfigManager {
    
    private final NexoJobs plugin;
    private File jobsFile;
    private FileConfiguration jobsConfig;
    
    public ConfigManager(NexoJobs plugin) {
        this.plugin = plugin;
        setupFiles();
    }
    
    private void setupFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        jobsFile = new File(plugin.getDataFolder(), "jobs.yml");
        if (!jobsFile.exists()) {
            plugin.saveResource("jobs.yml", false);
        }
        jobsConfig = YamlConfiguration.loadConfiguration(jobsFile);
    }
    
    public FileConfiguration getJobsConfig() {
        return jobsConfig;
    }
    
    public void saveJobsConfig() {
        try {
            jobsConfig.save(jobsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save jobs.yml!");
            e.printStackTrace();
        }
    }
    
    public void reloadConfigs() {

        plugin.reloadConfig();
        
        jobsConfig = YamlConfiguration.loadConfiguration(jobsFile);
        
        plugin.getLogger().info("Configuration files reloaded!");
    }
}
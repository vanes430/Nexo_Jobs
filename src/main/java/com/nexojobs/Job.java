package com.nexojobs;

import org.bukkit.Material;
import java.util.*;

public class Job {
    
    private final String id;
    private final String displayName;
    private final String description;
    private final Material icon;
    private final int maxLevel;
    private final Map<Integer, JobLevel> levels;
    private final Map<String, Integer> actions;
    
    public Job(String id, String displayName, String description, Material icon, int maxLevel) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.maxLevel = maxLevel;
        this.levels = new HashMap<>();
        this.actions = new HashMap<>();
    }
    
    public void addLevel(int level, JobLevel jobLevel) {
        levels.put(level, jobLevel);
    }
    
    public void addAction(String action, int exp) {
        actions.put(action, exp);
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    public int getMaxLevel() {
        return maxLevel;
    }
    
    public Map<Integer, JobLevel> getLevels() {
        return levels;
    }
    
    public JobLevel getLevel(int level) {
        return levels.get(level);
    }
    
    public Map<String, Integer> getActions() {
        return actions;
    }
    
    public int getExpForAction(String action) {
        return actions.getOrDefault(action, 0);
    }
}
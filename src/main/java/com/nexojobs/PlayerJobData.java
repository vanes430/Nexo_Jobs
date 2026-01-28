package com.nexojobs;

public class PlayerJobData {
    
    private final String jobId;
    private int level;
    private int exp;
    
    public PlayerJobData(String jobId, int level, int exp) {
        this.jobId = jobId;
        this.level = level;
        this.exp = exp;
    }
    
    public String getJobId() {
        return jobId;
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getExp() {
        return exp;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public void setExp(int exp) {
        this.exp = exp;
    }
    
    public void addExp(int exp) {
        this.exp += exp;
    }
}   
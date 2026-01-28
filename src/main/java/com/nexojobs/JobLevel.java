package com.nexojobs;

public class JobLevel {
    
    private final int level;
    private final int expRequired;
    private final double moneyReward;
    
    public JobLevel(int level, int expRequired, double moneyReward) {
        this.level = level;
        this.expRequired = expRequired;
        this.moneyReward = moneyReward;
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getExpRequired() {
        return expRequired;
    }
    
    public double getMoneyReward() {
        return moneyReward;
    }
}
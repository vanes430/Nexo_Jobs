// i literally have no idea about this part, this shit was made by AI ;v
// fr though, i don't understand PlaceholderAPI for shit

package com.nexojobs;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JobsPlaceholder extends PlaceholderExpansion {
    
    private final NexoJobs plugin;
    
    public JobsPlaceholder(NexoJobs plugin) {
        this.plugin = plugin;
    }
    
    @Override
    @NotNull
    public String getIdentifier() {
        return "nexojobs";
    }
    
    @Override
    @NotNull
    public String getAuthor() {
        return "grehista";
    }
    
    @Override
    @NotNull
    public String getVersion() {
        return "1.5.2";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        // %nexojobs_job% - Current active job name
        if (params.equals("job")) {
            String jobId = plugin.getJobManager().getActiveJobId(player);
            if (jobId == null) return "None";
            Job job = plugin.getJobManager().getJob(jobId);
            return job != null ? plugin.color(job.getDisplayName()) : "None";
        }
        
        // %nexojobs_job_id% - Current active job ID
        if (params.equals("job_id")) {
            String jobId = plugin.getJobManager().getActiveJobId(player);
            return jobId != null ? jobId : "none";
        }
        
        // %nexojobs_active_count% - Number of active jobs
        if (params.equals("active_count")) {
            return String.valueOf(plugin.getJobManager().getActiveJobCount(player));
        }
        
        // %nexojobs_job_limit% - Maximum jobs allowed
        if (params.equals("job_limit")) {
            int limit = plugin.getJobManager().getJobLimit(player);
            return limit == Integer.MAX_VALUE ? "Unlimited" : String.valueOf(limit);
        }
        
        // %nexojobs_status% - Job status (ACTIVE, PAUSED, NONE)
        if (params.equals("status")) {
            String jobId = plugin.getJobManager().getActiveJobId(player);
            if (jobId == null) return "None";
            return plugin.color("&a&lACTIVE");
        }
        
        // %nexojobs_has_job% - Returns true/false
        if (params.equals("has_job")) {
            return String.valueOf(plugin.getJobManager().hasActiveJob(player));
        }
        
        // %nexojobs_level% - Current active job level
        if (params.equals("level")) {
            PlayerJobData data = plugin.getJobManager().getActiveJobData(player);
            return data != null ? String.valueOf(data.getLevel()) : "0";
        }
        
        // %nexojobs_exp% - Current active job exp
        if (params.equals("exp")) {
            PlayerJobData data = plugin.getJobManager().getActiveJobData(player);
            return data != null ? String.valueOf(data.getExp()) : "0";
        }
        
        // %nexojobs_exp_required% - Exp required for next level
        if (params.equals("exp_required") || params.equals("required")) {
            PlayerJobData data = plugin.getJobManager().getActiveJobData(player);
            if (data == null) return "0";
            Job job = plugin.getJobManager().getJob(data.getJobId());
            if (job == null) return "0";
            JobLevel level = job.getLevel(data.getLevel());
            return level != null ? String.valueOf(level.getExpRequired()) : "0";
        }
        
        // %nexojobs_max_level% - Max level of current active job
        if (params.equals("max_level")) {
            PlayerJobData data = plugin.getJobManager().getActiveJobData(player);
            if (data == null) return "0";
            Job job = plugin.getJobManager().getJob(data.getJobId());
            return job != null ? String.valueOf(job.getMaxLevel()) : "0";
        }
        
        // %nexojobs_progress_bar% - Progress bar
        if (params.equals("progress") || params.equals("progress_bar")) {
            PlayerJobData data = plugin.getJobManager().getActiveJobData(player);
            if (data == null) return "▱▱▱▱▱▱▱▱▱▱";
            Job job = plugin.getJobManager().getJob(data.getJobId());
            if (job == null) return "▱▱▱▱▱▱▱▱▱▱";
            JobLevel level = job.getLevel(data.getLevel());
            if (level == null) return "▰▰▰▰▰▰▰▰▰▰";
            
            double percentage = (double) data.getExp() / level.getExpRequired();
            int bars = (int) (percentage * 10);
            
            StringBuilder progress = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                progress.append(i < bars ? "▰" : "▱");
            }
            return progress.toString();
        }
        
        // %nexojobs_percentage% - Progress as percentage
        if (params.equals("progress_percentage") || params.equals("percentage")) {
            PlayerJobData data = plugin.getJobManager().getActiveJobData(player);
            if (data == null) return "0";
            Job job = plugin.getJobManager().getJob(data.getJobId());
            if (job == null) return "0";
            JobLevel level = job.getLevel(data.getLevel());
            if (level == null) return "100";
            
            double percentage = ((double) data.getExp() / level.getExpRequired()) * 100;
            return String.format("%.1f", percentage);
        }
        
        // %nexojobs_exp_remaining% - EXP needed to level up
        if (params.equals("exp_remaining") || params.equals("remaining")) {
            PlayerJobData data = plugin.getJobManager().getActiveJobData(player);
            if (data == null) return "0";
            Job job = plugin.getJobManager().getJob(data.getJobId());
            if (job == null) return "0";
            JobLevel level = job.getLevel(data.getLevel());
            if (level == null) return "0";
            
            int remaining = level.getExpRequired() - data.getExp();
            return String.valueOf(Math.max(0, remaining));
        }
        
        // SPECIFIC JOB PLACEHOLDERS
        // Format: %nexojobs_<jobid>_<property>%
        
        // %nexojobs_<jobid>_status% - Status for specific job
        if (params.endsWith("_status")) {
            String jobId = params.replace("_status", "");
            return getJobStatus(player, jobId);
        }
        
        // %nexojobs_<jobid>_level% - Level for specific job
        if (params.endsWith("_level")) {
            String jobId = params.replace("_level", "");
            PlayerJobData data = plugin.getJobManager().getJobData(player, jobId);
            return data != null ? String.valueOf(data.getLevel()) : "0";
        }
        
        // %nexojobs_<jobid>_exp% - Exp for specific job
        if (params.endsWith("_exp")) {
            String jobId = params.replace("_exp", "");
            PlayerJobData data = plugin.getJobManager().getJobData(player, jobId);
            return data != null ? String.valueOf(data.getExp()) : "0";
        }
        
        // %nexojobs_<jobid>_max_level% - Max level for specific job
        if (params.endsWith("_max_level")) {
            String jobId = params.replace("_max_level", "");
            Job job = plugin.getJobManager().getJob(jobId);
            return job != null ? String.valueOf(job.getMaxLevel()) : "0";
        }
        
        // %nexojobs_<jobid>_required% - Required EXP for specific job
        if (params.endsWith("_required")) {
            String jobId = params.replace("_required", "");
            PlayerJobData data = plugin.getJobManager().getJobData(player, jobId);
            if (data == null) return "0";
            Job job = plugin.getJobManager().getJob(jobId);
            if (job == null) return "0";
            JobLevel level = job.getLevel(data.getLevel());
            return level != null ? String.valueOf(level.getExpRequired()) : "0";
        }
        
        // %nexojobs_<jobid>_percentage% - Progress percentage for specific job
        if (params.endsWith("_percentage")) {
            String jobId = params.replace("_percentage", "");
            PlayerJobData data = plugin.getJobManager().getJobData(player, jobId);
            if (data == null) return "0";
            Job job = plugin.getJobManager().getJob(jobId);
            if (job == null) return "0";
            JobLevel level = job.getLevel(data.getLevel());
            if (level == null) return "100";
            
            double percentage = ((double) data.getExp() / level.getExpRequired()) * 100;
            return String.format("%.1f", percentage);
        }
        
        // %nexojobs_<jobid>_is_active% - Returns true/false if job is active
        if (params.endsWith("_is_active")) {
            String jobId = params.replace("_is_active", "");
            return String.valueOf(plugin.getJobManager().hasJobActive(player, jobId));
        }
        
        // %nexojobs_<jobid>_has_started% - Returns true/false if player has started job
        if (params.endsWith("_has_started")) {
            String jobId = params.replace("_has_started", "");
            PlayerJobData data = plugin.getJobManager().getJobData(player, jobId);
            return String.valueOf(data != null);
        }
        
        return null;
    }
    
    private String getJobStatus(Player player, String jobId) {
        boolean isActive = plugin.getJobManager().hasJobActive(player, jobId);
        PlayerJobData data = plugin.getJobManager().getJobData(player, jobId);
        
        if (isActive) {
            return "ACTIVE";
        } else if (data != null) {
            return "PAUSED";
        } else {
            return "NEW";
        }
    }
}
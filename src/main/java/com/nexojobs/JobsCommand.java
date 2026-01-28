package com.nexojobs;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import java.util.*;

public class JobsCommand implements CommandExecutor, TabCompleter {
    
    private final NexoJobs plugin;
    
    public JobsCommand(NexoJobs plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /jobs reload
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("nexojobs.admin")) {
                sender.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }
            
            plugin.getJobManager().reloadAll();
            sender.sendMessage(plugin.getMessage("reload-success"));
            return true;
        }

        // /jobs data database check
        if (args.length >= 3 && args[0].equalsIgnoreCase("data") && 
            args[1].equalsIgnoreCase("database") && args[2].equalsIgnoreCase("check")) {
            
            if (!sender.hasPermission("nexojobs.admin")) {
                sender.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }
            
            DatabaseManager.DatabaseStats stats = plugin.getDatabaseManager().getDatabaseStats();
            boolean isConnected = plugin.getDatabaseManager().isConnected();
            
            sender.sendMessage("");
            sender.sendMessage(plugin.color("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            sender.sendMessage(plugin.color("  §b§lNexoJobs - 1.5.3"));
            sender.sendMessage(plugin.color("  §e§lDatabase Connection Status"));
            sender.sendMessage("");
            
            if (isConnected) {
                sender.sendMessage(plugin.color("  §a§l✔ CONNECTED TO DATABASE"));
            } else {
                sender.sendMessage(plugin.color("  §c§l✗ NOT CONNECTED TO DATABASE"));
            }
            
            sender.sendMessage("");
            sender.sendMessage(plugin.color("  §7Database Statistics:"));
            sender.sendMessage(plugin.color("  §8▸ §7Total Players: §e" + stats.totalPlayers));
            sender.sendMessage(plugin.color("  §8▸ §7Job Entries: §e" + stats.totalJobEntries));
            sender.sendMessage(plugin.color("  §8▸ §7Active Jobs: §e" + stats.activeJobs));
            sender.sendMessage("");
            sender.sendMessage(plugin.color("  §7Connection Pool:"));
            sender.sendMessage(plugin.color("  §8▸ §7Total Connections: §e" + stats.poolSize));
            sender.sendMessage(plugin.color("  §8▸ §7Active: §e" + stats.activeConnections));
            sender.sendMessage(plugin.color("  §8▸ §7Idle: §e" + stats.idleConnections));
            
            if (!stats.topLevels.isEmpty()) {
                sender.sendMessage("");
                sender.sendMessage(plugin.color("  §7Top Levels by Job:"));
                for (Map.Entry<String, Integer> entry : stats.topLevels.entrySet()) {
                    sender.sendMessage(plugin.color("  §8▸ §6" + entry.getKey() + " §7- Level §e" + entry.getValue()));
                }
            }
            sender.sendMessage("");
            sender.sendMessage(plugin.color("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            sender.sendMessage("");
            return true;
        }

        // /jobs data database jobs all
        if (args.length >= 4 && args[0].equalsIgnoreCase("data") && 
            args[1].equalsIgnoreCase("database") && args[2].equalsIgnoreCase("jobs") && 
            args[3].equalsIgnoreCase("all")) {
            
            if (!sender.hasPermission("nexojobs.admin")) {
                sender.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }
            
            List<String> jobData = plugin.getDatabaseManager().getAllJobDataFormatted();
            
            sender.sendMessage("");
            sender.sendMessage(plugin.color("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            sender.sendMessage(plugin.color("  §e§lAll Job Data from Database"));
            sender.sendMessage("");
            
            if (jobData.isEmpty()) {
                sender.sendMessage(plugin.color("  §7No job data found in database."));
            } else {
                for (String line : jobData) {
                    sender.sendMessage(line);
                }
            }
            
            sender.sendMessage(plugin.color("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            sender.sendMessage("");
            return true;
        }

        // /jobs set <player> <job> <level>
        if (args.length >= 4 && args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("nexojobs.admin")) {
                sender.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }
            
            String targetName = args[1];
            String jobId = args[2].toLowerCase();
            int level;
            
            try {
                level = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.color("§c§lERROR: §7Level must be a number!"));
                return true;
            }
            
            Job job = plugin.getJobManager().getJob(jobId);
            if (job == null) {
                sender.sendMessage(plugin.getMessage("job-not-found").replace("{job}", jobId));
                return true;
            }
            
            if (level < 1 || level > job.getMaxLevel()) {
                sender.sendMessage(plugin.color("§c§lERROR: §7Level must be between 1 and " + job.getMaxLevel() + "!"));
                return true;
            }
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(plugin.getMessage("player-not-found"));
                return true;
            }
            
            boolean success = plugin.getJobManager().setPlayerJobLevel(
                target.getUniqueId(), 
                target.getName(), 
                jobId, 
                level
            );
            
            if (success) {
                sender.sendMessage(plugin.color("§a§lSUCCESS: §7Set §e" + target.getName() + 
                    "§7's §e" + job.getDisplayName() + " §7level to §e" + level + "§7!"));
                
                if (target.isOnline()) {
                    Player onlineTarget = target.getPlayer();
                    onlineTarget.sendMessage(plugin.color("§e§lNOTICE: §7An admin has set your §e" + 
                        job.getDisplayName() + " §7level to §e" + level + "§7!"));
                }
            } else {
                sender.sendMessage(plugin.color("§c§lERROR: §7Failed to set job level!"));
            }
            
            return true;
        }

        // /jobs reset-progress <player> <job|all>
        if (args.length > 0 && args[0].equalsIgnoreCase("reset-progress")) {
            if (!sender.hasPermission("nexojobs.admin")) {
                sender.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }
            
            if (args.length < 3) {
                sender.sendMessage(plugin.color("§c§lUsage: §7/jobs reset-progress <player> <job|all>"));
                return true;
            }
            
            String targetName = args[1];
            String jobId = args[2].toLowerCase();
            
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                sender.sendMessage(plugin.getMessage("player-not-found"));
                return true;
            }
            
            if (jobId.equals("all")) {
                int resetCount = plugin.getJobManager().resetAllProgress(target.getUniqueId());
                sender.sendMessage(plugin.getMessage("reset-all-success")
                    .replace("{player}", target.getName())
                    .replace("{count}", String.valueOf(resetCount)));
                return true;
            }
            
            Job job = plugin.getJobManager().getJob(jobId);
            if (job == null) {
                sender.sendMessage(plugin.getMessage("job-not-found")
                    .replace("{job}", jobId));
                return true;
            }
            
            PlayerJobData data = plugin.getJobManager().getJobData(target, jobId);
            if (data == null) {
                sender.sendMessage(plugin.color("§cPlayer " + target.getName() + " does not have any progress in " + job.getDisplayName()));
                return true;
            }
            
            if (plugin.getJobManager().hasJobActive(target, jobId)) {
                plugin.getJobManager().leaveJob(target, jobId);
                target.sendMessage(plugin.color("§cYour active job §e" + job.getDisplayName() + " §chas been stopped by an admin for reset."));
            }
            
            boolean success = plugin.getJobManager().resetJobProgress(target, jobId);
            if (success) {
                sender.sendMessage(plugin.color("§aSuccessfully reset §e" + job.getDisplayName() + " §aprogress for §e" + target.getName() + "§a!"));
                target.sendMessage(plugin.color("§eYour progress in §c" + job.getDisplayName() + " §ehas been reset by an admin!"));
            } else {
                sender.sendMessage(plugin.color("§cFailed to reset progress. Please check console errors."));
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("only-players"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // /jobs join <job>
        if (args.length >= 2 && args[0].equalsIgnoreCase("join")) {
            String jobId = args[1].toLowerCase();
            Job job = plugin.getJobManager().getJob(jobId);
            
            if (job == null) {
                player.sendMessage(plugin.getMessage("job-not-found")
                    .replace("{job}", args[1]));
                return true;
            }

            if (plugin.getJobManager().hasJobActive(player, jobId)) {
                player.sendMessage(plugin.getMessage("already-have-job")
                    .replace("{job}", job.getDisplayName()));
                return true;
            }

            if (!plugin.getJobManager().canJoinMoreJobs(player)) {
                int current = plugin.getJobManager().getActiveJobCount(player);
                int limit = plugin.getJobManager().getJobLimit(player);
                player.sendMessage(plugin.getMessage("job-limit-reached")
                    .replace("{current}", String.valueOf(current))
                    .replace("{max}", limit == Integer.MAX_VALUE ? "Unlimited" : String.valueOf(limit)));
                return true;
            }

            if (plugin.getJobManager().joinJob(player, jobId)) {
                player.sendMessage(plugin.getMessage("job-joined")
                    .replace("{job}", job.getDisplayName()));
            } else {
                player.sendMessage(plugin.color("§cFailed to join job!"));
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("leave")) {
            if (args.length >= 2) {
                String jobId = args[1].toLowerCase();
                Job job = plugin.getJobManager().getJob(jobId);
                
                if (job == null) {
                    player.sendMessage(plugin.getMessage("job-not-found")
                        .replace("{job}", args[1]));
                    return true;
                }
                
                if (!plugin.getJobManager().hasJobActive(player, jobId)) {
                    player.sendMessage(plugin.color("§cYou don't have this job active!"));
                    return true;
                }
                
                if (plugin.getJobManager().leaveJob(player, jobId)) {
                    player.sendMessage(plugin.getMessage("job-left")
                        .replace("{job}", job.getDisplayName()));
                } else {
                    player.sendMessage(plugin.color("§cFailed to leave job!"));
                }
            } else {
                if (!plugin.getJobManager().hasActiveJob(player)) {
                    player.sendMessage(plugin.getMessage("no-active-job"));
                    return true;
                }
                
                plugin.getJobManager().leaveAllJobs(player);
                player.sendMessage(plugin.color("§b§lJOBS §9/ §aYou have left all your jobs."));
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("progress")) {
            Job job = null;
            
            if (args.length >= 2) {
                String jobId = args[1].toLowerCase();
                job = plugin.getJobManager().getJob(jobId);
                
                if (job == null) {
                    player.sendMessage(plugin.getMessage("job-not-found")
                        .replace("{job}", args[1]));
                    return true;
                }
                
                if (plugin.getJobManager().getJobData(player, jobId) == null) {
                    player.sendMessage(plugin.getMessage("no-job-data")
                        .replace("{job}", job.getDisplayName()));
                    return true;
                }
            } else {
                String activeJobId = plugin.getJobManager().getActiveJobId(player);
                if (activeJobId == null) {
                    player.sendMessage(plugin.getMessage("no-active-job"));
                    return true;
                }
                job = plugin.getJobManager().getJob(activeJobId);
            }
            
            if (job != null) {
                plugin.getJobMenuGUI().openProgressMenu(player, job);
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            if (args.length < 2) {
                player.sendMessage(plugin.color("§c§lUsage: §7/jobs info <jobname>"));
                return true;
            }
            
            String jobId = args[1].toLowerCase();
            Job job = plugin.getJobManager().getJob(jobId);
            
            if (job == null) {
                player.sendMessage(plugin.getMessage("job-not-found")
                    .replace("{job}", args[1]));
                return true;
            }
            
            sendJobInfo(player, job);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("stats")) {
            Player target = player;
            
            if (args.length >= 2) {
                if (!player.hasPermission("nexojobs.admin")) {
                    player.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.color("§cPlayer not found!"));
                    return true;
                }
            }
            
            sendPlayerStats(player, target);
            return true;
        }

        // /jobs list
        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            player.sendMessage("");
            player.sendMessage(plugin.color("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendMessage(plugin.color("  §e§lAvailable Jobs"));
            player.sendMessage("");
            
            Set<String> activeJobIds = plugin.getJobManager().getActiveJobIds(player);
            
            for (Job job : plugin.getJobManager().getAllJobs()) {
                PlayerJobData data = plugin.getJobManager().getJobData(player, job.getId());
                boolean isActive = activeJobIds.contains(job.getId());
                
                String status = isActive ? "§a§lACTIVE" : (data != null ? "§6§lPAUSED" : "§7§lNEW");
                String levelInfo = data != null ? " §7Level §e" + data.getLevel() : "";
                
                player.sendMessage(plugin.color("  §8▸ " + job.getDisplayName() + " " + status + levelInfo));
            }
            
            player.sendMessage("");
            int current = plugin.getJobManager().getActiveJobCount(player);
            int limit = plugin.getJobManager().getJobLimit(player);
            String limitText = limit == Integer.MAX_VALUE ? "Unlimited" : String.valueOf(limit);
            player.sendMessage(plugin.color("  §7Active Jobs: §e" + current + "§7/§e" + limitText));
            player.sendMessage(plugin.color("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendMessage("");
            return true;
        }

        // /jobs limit
        if (args.length > 0 && args[0].equalsIgnoreCase("limit")) {
            int current = plugin.getJobManager().getActiveJobCount(player);
            int limit = plugin.getJobManager().getJobLimit(player);
            String limitText = limit == Integer.MAX_VALUE ? "Unlimited" : String.valueOf(limit);
            
            player.sendMessage("");
            player.sendMessage(plugin.color("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendMessage(plugin.color("  §e§lJob Limit Information"));
            player.sendMessage("");
            player.sendMessage(plugin.color("  §7Current Active Jobs: §e" + current));
            player.sendMessage(plugin.color("  §7Maximum Jobs Allowed: §e" + limitText));
            player.sendMessage(plugin.color("  §7Available Slots: §e" + (limit == Integer.MAX_VALUE ? "Unlimited" : (limit - current))));
            player.sendMessage(plugin.color("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendMessage("");
            return true;
        }

        // /jobs help
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        plugin.getJobMenuGUI().openMainMenu(player);
        return true;
    }
    
    private void sendJobInfo(Player player, Job job) {
        player.sendMessage("");
        player.sendMessage(plugin.color("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(plugin.color("  " + job.getDisplayName()));
        player.sendMessage(plugin.color("  " + job.getDescription()));
        player.sendMessage("");
        player.sendMessage(plugin.color("  §7Max Level: §e" + job.getMaxLevel()));
        player.sendMessage(plugin.color("  §7Total Actions: §e" + job.getActions().size()));
        player.sendMessage("");
        
        PlayerJobData data = plugin.getJobManager().getJobData(player, job.getId());
        if (data != null) {
            player.sendMessage(plugin.color("  §7Your Level: §e" + data.getLevel()));
            player.sendMessage(plugin.color("  §7Your EXP: §e" + data.getExp()));
            
            JobLevel currentLevel = job.getLevel(data.getLevel());
            if (currentLevel != null) {
                player.sendMessage(plugin.color("  §7Required: §e" + currentLevel.getExpRequired()));
                double percentage = ((double) data.getExp() / currentLevel.getExpRequired()) * 100;
                player.sendMessage(plugin.color("  §7Progress: §e" + String.format("%.1f", percentage) + "%"));
            }
        } else {
            player.sendMessage(plugin.color("  §7You haven't started this job yet!"));
        }
        
        player.sendMessage(plugin.color("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage("");
    }
    
    private void sendPlayerStats(Player sender, Player target) {
        sender.sendMessage("");
        sender.sendMessage(plugin.color("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(plugin.color("  §e§l" + target.getName() + "'s Job Statistics"));
        sender.sendMessage("");
        
        Set<String> activeJobIds = plugin.getJobManager().getActiveJobIds(target);
        int limit = plugin.getJobManager().getJobLimit(target);
        String limitText = limit == Integer.MAX_VALUE ? "Unlimited" : String.valueOf(limit);
        
        sender.sendMessage(plugin.color("  §7Active Jobs: §e" + activeJobIds.size() + "§7/§e" + limitText));
        sender.sendMessage("");
        
        if (!activeJobIds.isEmpty()) {
            sender.sendMessage(plugin.color("  §7§lActive Jobs:"));
            for (String jobId : activeJobIds) {
                Job activeJob = plugin.getJobManager().getJob(jobId);
                PlayerJobData data = plugin.getJobManager().getJobData(target, jobId);
                
                if (activeJob != null && data != null) {
                    sender.sendMessage(plugin.color("  §8▸ " + activeJob.getDisplayName() + " §7Level §e" + data.getLevel() + "§7/§e" + activeJob.getMaxLevel()));
                }
            }
            sender.sendMessage("");
        }
        
        sender.sendMessage(plugin.color("  §7§lAll Jobs:"));
        for (Job job : plugin.getJobManager().getAllJobs()) {
            PlayerJobData data = plugin.getJobManager().getJobData(target, job.getId());
            if (data != null) {
                sender.sendMessage(plugin.color("  §5▸ " + job.getDisplayName() + " §7Level §e" + data.getLevel()));
            } else {
                sender.sendMessage(plugin.color("  §8▸ " + job.getDisplayName() + " §7Not started"));
            }
        }
        
        sender.sendMessage(plugin.color("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage("");
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage(plugin.color("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(plugin.color("  §b§lNexoJobs Commands"));
        player.sendMessage("");
        player.sendMessage(plugin.color("  §e/jobs §7- Open jobs menu"));
        player.sendMessage(plugin.color("  §e/jobs list §7- List all available jobs"));
        player.sendMessage(plugin.color("  §e/jobs join <job> §7- Join a job"));
        player.sendMessage(plugin.color("  §e/jobs leave [job] §7- Leave job(s)"));
        player.sendMessage(plugin.color("  §e/jobs progress [job] §7- View job progress"));
        player.sendMessage(plugin.color("  §e/jobs info <job> §7- View job information"));
        player.sendMessage(plugin.color("  §e/jobs stats [player] §7- View job statistics"));
        player.sendMessage(plugin.color("  §e/jobs limit §7- Check your job limit"));
        
        if (player.hasPermission("nexojobs.admin")) {
            player.sendMessage("");
            player.sendMessage(plugin.color("  §c§lAdmin Commands:"));
            player.sendMessage(plugin.color("  §e/jobs reload §7- Reload plugin"));
            player.sendMessage(plugin.color("  §e/jobs set <player> <job> <level> §7- Set job level"));
            player.sendMessage(plugin.color("  §e/jobs reset-progress <player> <job|all> §7- Reset progress"));
            player.sendMessage(plugin.color("  §e/jobs data database check §7- Check DB connection"));
            player.sendMessage(plugin.color("  §e/jobs data database jobs all §7- View all DB data"));
        }
        
        player.sendMessage(plugin.color("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage("");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("join");
            completions.add("leave");
            completions.add("progress");
            completions.add("info");
            completions.add("stats");
            completions.add("list");
            completions.add("limit");
            completions.add("help");
            if (sender.hasPermission("nexojobs.admin")) {
                completions.add("reload");
                completions.add("reset-progress");
                completions.add("set");
                completions.add("data");
            }
            
            String input = args[0].toLowerCase();
            completions.removeIf(s -> !s.toLowerCase().startsWith(input));
            
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("join") || 
                args[0].equalsIgnoreCase("progress") || 
                args[0].equalsIgnoreCase("info") ||
                args[0].equalsIgnoreCase("leave")) {
                
                for (Job job : plugin.getJobManager().getAllJobs()) {
                    completions.add(job.getId());
                }
                
                String input = args[1].toLowerCase();
                completions.removeIf(s -> !s.toLowerCase().startsWith(input));
                
            } else if (args[0].equalsIgnoreCase("stats") && sender.hasPermission("nexojobs.admin")) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    completions.add(p.getName());
                }
                String input = args[1].toLowerCase();
                completions.removeIf(s -> !s.toLowerCase().startsWith(input));
                
            } else if (args[0].equalsIgnoreCase("reset-progress") && sender.hasPermission("nexojobs.admin")) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    completions.add(p.getName());
                }
                String input = args[1].toLowerCase();
                completions.removeIf(s -> !s.toLowerCase().startsWith(input));
                
            } else if (args[0].equalsIgnoreCase("set") && sender.hasPermission("nexojobs.admin")) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    completions.add(p.getName());
                }
                String input = args[1].toLowerCase();
                completions.removeIf(s -> !s.toLowerCase().startsWith(input));
                
            } else if (args[0].equalsIgnoreCase("data") && sender.hasPermission("nexojobs.admin")) {
                completions.add("database");
                String input = args[1].toLowerCase();
                completions.removeIf(s -> !s.toLowerCase().startsWith(input));
            }
            
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("reset-progress") && sender.hasPermission("nexojobs.admin")) {
                completions.add("all");
                for (Job job : plugin.getJobManager().getAllJobs()) {
                    completions.add(job.getId());
                }
                
                String input = args[2].toLowerCase();
                completions.removeIf(s -> !s.toLowerCase().startsWith(input));
                
            } else if (args[0].equalsIgnoreCase("set") && sender.hasPermission("nexojobs.admin")) {
                for (Job job : plugin.getJobManager().getAllJobs()) {
                    completions.add(job.getId());
                }
                String input = args[2].toLowerCase();
                completions.removeIf(s -> !s.toLowerCase().startsWith(input));
                
            } else if (args[0].equalsIgnoreCase("data") && args[1].equalsIgnoreCase("database") && 
                       sender.hasPermission("nexojobs.admin")) {
                completions.add("check");
                completions.add("jobs");
                String input = args[2].toLowerCase();
                completions.removeIf(s -> !s.toLowerCase().startsWith(input));
            }
            
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("set") && sender.hasPermission("nexojobs.admin")) {
                completions.add("1");
                completions.add("10");
                completions.add("25");
                completions.add("50");
                
            } else if (args[0].equalsIgnoreCase("data") && args[1].equalsIgnoreCase("database") && 
                       args[2].equalsIgnoreCase("jobs") && sender.hasPermission("nexojobs.admin")) {
                completions.add("all");
            }
        }
        
        return completions;
    }
}
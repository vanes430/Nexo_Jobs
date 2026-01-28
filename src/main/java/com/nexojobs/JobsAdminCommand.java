package com.nexojobs;

import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class JobsAdminCommand implements CommandExecutor {
    
    private final NexoJobs plugin;
    
    public JobsAdminCommand(NexoJobs plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexojobs.admin")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            plugin.getConfigManager().reloadConfigs();
            plugin.getJobManager().loadJobs();
            sender.sendMessage(plugin.getMessage("reload-success"));
            return true;
        }

        if (sub.equals("enable") || sub.equals("disable")) {
            if (args.length < 2) {
                sender.sendMessage("Â§cUsage: /jobsadmin " + sub + " <money|items>");
                return true;
            }
            boolean value = sub.equals("enable");
            String type = args[1].toLowerCase();
            
            if (type.equals("money")) {
                plugin.getConfig().set("settings.rewards.money", value);
                plugin.saveConfig();
                sender.sendMessage(plugin.getPrefix() + "Â§aMoney rewards " + (value ? "enabled" : "disabled") + "!");
            } else if (type.equals("items")) {
                plugin.getConfig().set("settings.rewards.items", value);
                plugin.saveConfig();
                sender.sendMessage(plugin.getPrefix() + "Â§aItem rewards " + (value ? "enabled" : "disabled") + "!");
            } else {
                sender.sendMessage("Â§cUnknown type. Use money or items.");
            }
            return true;
        }

        if (sub.equals("reward-money")) {
            if (args.length < 4) {
                sender.sendMessage("Â§cUsage: /jobsadmin reward-money <job> <level> <amount>");
                return true;
            }
            
            String jobId = args[1];
            int level;
            double amount;
            
            try {
                level = Integer.parseInt(args[2]);
                amount = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("Â§cLevel and Amount must be numbers!");
                return true;
            }
            
            if (plugin.getJobManager().getJob(jobId) == null) {
                sender.sendMessage("Â§cJob not found!");
                return true;
            }
            plugin.getConfigManager().getJobsConfig().set("jobs." + jobId + ".levels." + level + ".rewards.money", amount);
            plugin.getConfigManager().saveJobsConfig();
            plugin.getJobManager().loadJobs();
            sender.sendMessage(plugin.getPrefix() + "Â§aSet money reward for " + jobId + " lvl " + level + " to $" + amount);
            return true;
        }

        if (sub.equals("reward-item")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Â§cOnly players can set item rewards (must hold item).");
                return true;
            }
            Player player = (Player) sender;
            
            if (args.length < 3) {
                player.sendMessage("Â§cUsage: /jobsadmin reward-item <job> <level>");
                return true;
            }
            
            String jobId = args[1];
            int level;
            try {
                level = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("Â§cLevel must be a number!");
                return true;
            }
            
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                player.sendMessage("Â§cYou must hold an item in your hand!");
                return true;
            }
            
            if (plugin.getJobManager().getJob(jobId) == null) {
                player.sendMessage("Â§cJob not found!");
                return true;
            }

            plugin.getConfigManager().getJobsConfig().set("jobs." + jobId + ".levels." + level + ".rewards.item", hand);
            plugin.getConfigManager().saveJobsConfig();
            
            plugin.getJobManager().loadJobs();
            player.sendMessage(plugin.getPrefix() + "Â§aItem reward set for " + jobId + " lvl " + level + "!");
            return true;
        }
        
        sendHelp(sender);
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("Â§8Â§m----------------------------");
        sender.sendMessage("Â§bÂ§lNexoJobs Admin Help");
        sender.sendMessage("Â§e/jobsadmin reload");
        sender.sendMessage("Â§e/jobsadmin enable <money|items>");
        sender.sendMessage("Â§e/jobsadmin disable <money|items>");
        sender.sendMessage("Â§e/jobsadmin reward-money <job> <level> <amount>");
        sender.sendMessage("Â§e/jobsadmin reward-item <job> <level> Â§7(Holds Item)");
        sender.sendMessage("Â§8Â§m----------------------------");
    }
}
package com.nexojobs;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import java.util.*;

public class JobMenuGUI implements Listener {
    
    private final NexoJobs plugin;
    private final Map<UUID, Integer> currentProgressPage;
    private final Map<UUID, Integer> currentMainMenuPage;
    
    public JobMenuGUI(NexoJobs plugin) {
        this.plugin = plugin;
        this.currentProgressPage = new HashMap<>();
        this.currentMainMenuPage = new HashMap<>();
    }
    
    
    public void openMainMenu(Player player) {
        openMainMenu(player, 1);
    }
    
    public void openMainMenu(Player player, int page) {
        currentMainMenuPage.put(player.getUniqueId(), page);
        
        String pageKey = "page-" + page;
        ConfigurationSection pageConfig = plugin.getConfig().getConfigurationSection("gui.main-menu." + pageKey);
        
        if (pageConfig == null) {
            player.sendMessage("§cInvalid page!");
            return;
        }

        int size = pageConfig.getInt("size", 54);
        
        String title = plugin.color(plugin.getConfig().getString("gui.main-menu.title", "Job Selection")
            .replace("{page}", String.valueOf(page)));
        
        Inventory inv = Bukkit.createInventory(null, size, title);

        ConfigurationSection fillersConfig = pageConfig.getConfigurationSection("fillers");
        if (fillersConfig != null && fillersConfig.getBoolean("enabled", false)) {
            ItemStack filler = createItemFromConfig(fillersConfig);
            for (int i = 0; i < size; i++) {
                inv.setItem(i, filler);
            }
        }
        
        ConfigurationSection jobsConfig = pageConfig.getConfigurationSection("jobs");
        if (jobsConfig != null) {
            for (String jobId : jobsConfig.getKeys(false)) {
                ConfigurationSection jobItemConfig = jobsConfig.getConfigurationSection(jobId);
                if (jobItemConfig != null && jobItemConfig.getBoolean("enabled", true)) {
                    Job job = plugin.getJobManager().getJob(jobId);
                    if (job != null) {
                        int slot = jobItemConfig.getInt("slot", 0);
                        ItemStack item = createJobItem(player, job, jobItemConfig);
                        if (slot >= 0 && slot < size) {
                            inv.setItem(slot, item);
                        }
                    }
                }
            }
        }

        ConfigurationSection buttonsConfig = pageConfig.getConfigurationSection("buttons");
        if (buttonsConfig != null) {
            for (String buttonKey : buttonsConfig.getKeys(false)) {
                ConfigurationSection buttonConfig = buttonsConfig.getConfigurationSection(buttonKey);
                if (buttonConfig != null && buttonConfig.getBoolean("enabled", true)) {
                    int slot = buttonConfig.getInt("slot", 0);
                    
                    ItemStack button;
                    if (buttonKey.equalsIgnoreCase("info")) {
                        button = createMainMenuInfoButton(player, buttonConfig);
                    } else {
                        button = createItemFromConfig(buttonConfig);
                    }
                    
                    if (slot >= 0 && slot < size) {
                        inv.setItem(slot, button);
                    }
                }
            }
        }
        
        player.openInventory(inv);
    }
    
    private ItemStack createMainMenuInfoButton(Player player, ConfigurationSection config) {
        Material material = Material.valueOf(config.getString("material", "BOOK"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        
        if (config.contains("name")) {
            meta.setDisplayName(plugin.color(config.getString("name")));
        }
        
        Set<String> activeJobIds = plugin.getJobManager().getActiveJobIds(player);
        int activeCount = activeJobIds.size();
        int maxJobs = plugin.getJobManager().getJobLimit(player);
        
        List<String> lore = new ArrayList<>();
        for (String line : config.getStringList("lore")) {
            String processed = line
                .replace("{active_count}", String.valueOf(activeCount))
                .replace("{max_jobs}", maxJobs == Integer.MAX_VALUE ? "Unlimited" : String.valueOf(maxJobs));
            
            if (line.contains("{active_jobs_list}")) {
                if (activeJobIds.isEmpty()) {
                    lore.add(plugin.color("  &7None"));
                } else {
                    for (String jobId : activeJobIds) {
                        Job job = plugin.getJobManager().getJob(jobId);
                        if (job != null) {
                            PlayerJobData data = plugin.getJobManager().getJobData(player, jobId);
                            String levelInfo = data != null ? " &7(Lvl &e" + data.getLevel() + "&7)" : "";
                            lore.add(plugin.color("  &8▸ " + job.getDisplayName() + levelInfo));
                        }
                    }
                }
                continue;
            }
            
            lore.add(plugin.color(processed));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void openProgressMenu(Player player, Job job) {
        openProgressMenu(player, job, 1);
    }
    
    public void openProgressMenu(Player player, Job job, int page) {
        ConfigurationSection guiConfig = plugin.getConfig().getConfigurationSection("gui.progress-menu");
        if (guiConfig == null) {
            player.sendMessage("§cGUI configuration error!");
            return;
        }
        
        currentProgressPage.put(player.getUniqueId(), page);
        
        String title = plugin.color(
            guiConfig.getString("title", "{job} Progress")
                .replace("{job}", job.getDisplayName())
                .replace("{page}", String.valueOf(page))
        );
        int size = guiConfig.getInt("size", 54);
        
        Inventory inv = Bukkit.createInventory(null, size, title);
        
        ConfigurationSection fillersConfig = guiConfig.getConfigurationSection("fillers");
        if (fillersConfig != null && fillersConfig.getBoolean("enabled", false)) {
            ItemStack filler = createItemFromConfig(fillersConfig);
            for (int i = 0; i < size; i++) {
                inv.setItem(i, filler);
            }
        }
        
        PlayerJobData data = plugin.getJobManager().getJobData(player, job.getId());
        int currentLevel = data != null ? data.getLevel() : 0;
        int currentExp = data != null ? data.getExp() : 0;
        
        int levelsPerPage = guiConfig.getInt("levels-per-page", 28);
        int startLevel = (page - 1) * levelsPerPage + 1;
        int endLevel = Math.min(startLevel + levelsPerPage - 1, job.getMaxLevel());
        int totalPages = (int) Math.ceil((double) job.getMaxLevel() / levelsPerPage);
        
        List<Integer> levelSlots = guiConfig.getIntegerList("level-slots");
        if (levelSlots.isEmpty()) {
            levelSlots = Arrays.asList(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
            );
        }
        
        ConfigurationSection levelsConfig = guiConfig.getConfigurationSection("level-items");
        int slotIndex = 0;
        
        for (int level = startLevel; level <= endLevel && slotIndex < levelSlots.size(); level++, slotIndex++) {
            int slot = levelSlots.get(slotIndex);
            if (slot >= 0 && slot < size) {
                ItemStack levelItem = createLevelItem(job, level, currentLevel, currentExp, levelsConfig);
                inv.setItem(slot, levelItem);
            }
        }
        
        ConfigurationSection buttonsConfig = guiConfig.getConfigurationSection("buttons");
        if (buttonsConfig != null) {
            ConfigurationSection backConfig = buttonsConfig.getConfigurationSection("back");
            if (backConfig != null && backConfig.getBoolean("enabled", true)) {
                int slot = backConfig.getInt("slot", 45);
                if (slot >= 0 && slot < size) {
                    inv.setItem(slot, createItemFromConfig(backConfig));
                }
            }
            
            if (page > 1) {
                ConfigurationSection prevConfig = buttonsConfig.getConfigurationSection("previous-page");
                if (prevConfig != null && prevConfig.getBoolean("enabled", true)) {
                    int slot = prevConfig.getInt("slot", 48);
                    if (slot >= 0 && slot < size) {
                        ItemStack prevItem = createItemFromConfig(prevConfig);
                        ItemMeta meta = prevItem.getItemMeta();
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        lore.add(plugin.color("&7Page &e" + (page - 1) + "&7/&e" + totalPages));
                        meta.setLore(lore);
                        prevItem.setItemMeta(meta);
                        inv.setItem(slot, prevItem);
                    }
                }
            }
            
            ConfigurationSection infoConfig = buttonsConfig.getConfigurationSection("info");
            if (infoConfig != null && infoConfig.getBoolean("enabled", true)) {
                int slot = infoConfig.getInt("slot", 49);
                if (slot >= 0 && slot < size) {
                    ItemStack infoItem = createJobInfoItem(player, job, page, totalPages, infoConfig);
                    inv.setItem(slot, infoItem);
                }
            }
            
            if (page < totalPages) {
                ConfigurationSection nextConfig = buttonsConfig.getConfigurationSection("next-page");
                if (nextConfig != null && nextConfig.getBoolean("enabled", true)) {
                    int slot = nextConfig.getInt("slot", 50);
                    if (slot >= 0 && slot < size) {
                        ItemStack nextItem = createItemFromConfig(nextConfig);
                        ItemMeta meta = nextItem.getItemMeta();
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        lore.add(plugin.color("&7Page &e" + (page + 1) + "&7/&e" + totalPages));
                        meta.setLore(lore);
                        nextItem.setItemMeta(meta);
                        inv.setItem(slot, nextItem);
                    }
                }
            }
            
            if (plugin.getJobManager().hasJobActive(player, job.getId())) {
                ConfigurationSection leaveConfig = buttonsConfig.getConfigurationSection("leave");
                if (leaveConfig != null && leaveConfig.getBoolean("enabled", true)) {
                    int slot = leaveConfig.getInt("slot", 53);
                    if (slot >= 0 && slot < size) {
                        inv.setItem(slot, createItemFromConfig(leaveConfig));
                    }
                }
            }
        }
        
        player.openInventory(inv);
    }

    private ItemStack createJobItem(Player player, Job job, ConfigurationSection config) {
        Material material = Material.valueOf(config.getString("material", job.getIcon().name()));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();


        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        
        String name = config.getString("name", job.getDisplayName());
        meta.setDisplayName(plugin.color(name));
        
        List<String> lore = new ArrayList<>();
        List<String> loreTemplate = config.getStringList("lore");
        
        PlayerJobData data = plugin.getJobManager().getJobData(player, job.getId());
        boolean isActive = plugin.getJobManager().hasJobActive(player, job.getId());
        
        for (String line : loreTemplate) {
            String processed = processPlaceholders(line, player, job, data, isActive);
            lore.add(plugin.color(processed));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createLevelItem(Job job, int level, int currentLevel, int currentExp, ConfigurationSection config) {
        boolean isCompleted = level < currentLevel;
        boolean isCurrent = level == currentLevel;
        boolean isLocked = level > currentLevel;
        
        String status = isCompleted ? "completed" : (isCurrent ? "current" : "locked");
        ConfigurationSection statusConfig = config != null ? config.getConfigurationSection(status) : null;
        
        Material material = Material.STONE;
        if (statusConfig != null) {
            material = Material.valueOf(statusConfig.getString("material", "STONE"));
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();


        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        
        String nameTemplate = statusConfig != null ? statusConfig.getString("name", "&7Level {level}") : "&7Level {level}";
        meta.setDisplayName(plugin.color(nameTemplate.replace("{level}", String.valueOf(level))));
        
        List<String> lore = new ArrayList<>();
        if (statusConfig != null) {
            List<String> loreTemplate = statusConfig.getStringList("lore");
            JobLevel jobLevel = job.getLevel(level);
            
            for (String line : loreTemplate) {
                String processed = line
                    .replace("{level}", String.valueOf(level))
                    .replace("{exp}", String.valueOf(currentExp))
                    .replace("{required}", jobLevel != null ? String.valueOf(jobLevel.getExpRequired()) : "0")
                    .replace("{money}", jobLevel != null ? String.valueOf(jobLevel.getMoneyReward()) : "0");
                
                if (isCurrent && line.contains("{progress_bar}")) {
                    int required = jobLevel != null ? jobLevel.getExpRequired() : 1;
                    double percentage = (double) currentExp / required;
                    int bars = (int) (percentage * 10);
                    StringBuilder progressBar = new StringBuilder();
                    for (int i = 0; i < 10; i++) {
                        progressBar.append(i < bars ? "▰" : "▱");
                    }
                    processed = processed.replace("{progress_bar}", progressBar.toString());
                }
                
                if (isCurrent && line.contains("{percentage}")) {
                    int required = jobLevel != null ? jobLevel.getExpRequired() : 1;
                    double percentage = ((double) currentExp / required) * 100;
                    processed = processed.replace("{percentage}", String.format("%.1f", percentage));
                }
                
                lore.add(plugin.color(processed));
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createJobInfoItem(Player player, Job job, int currentPage, int totalPages, ConfigurationSection config) {
        Material material = Material.valueOf(config.getString("material", "BOOK"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();


        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        
        String name = config.getString("name", "&e&lJob Information");
        meta.setDisplayName(plugin.color(name));
        
        List<String> lore = new ArrayList<>();
        List<String> loreTemplate = config.getStringList("lore");
        
        PlayerJobData data = plugin.getJobManager().getJobData(player, job.getId());
        
        for (String line : loreTemplate) {
            String processed = line
                .replace("{job}", job.getDisplayName())
                .replace("{level}", data != null ? String.valueOf(data.getLevel()) : "0")
                .replace("{max_level}", String.valueOf(job.getMaxLevel()))
                .replace("{exp}", data != null ? String.valueOf(data.getExp()) : "0")
                .replace("{page}", String.valueOf(currentPage))
                .replace("{total_pages}", String.valueOf(totalPages));
            
            if (data != null) {
                JobLevel currentLevel = job.getLevel(data.getLevel());
                if (currentLevel != null) {
                    processed = processed.replace("{required}", String.valueOf(currentLevel.getExpRequired()));
                    
                    double percentage = ((double) data.getExp() / currentLevel.getExpRequired()) * 100;
                    processed = processed.replace("{percentage}", String.format("%.1f", percentage));
                }
            }
            
            lore.add(plugin.color(processed));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createItemFromConfig(ConfigurationSection config) {
        Material material = Material.valueOf(config.getString("material", "STONE"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();


        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        
        if (config.contains("name")) {
            meta.setDisplayName(plugin.color(config.getString("name")));
        }
        
        if (config.contains("lore")) {
            List<String> lore = new ArrayList<>();
            for (String line : config.getStringList("lore")) {
                lore.add(plugin.color(line));
            }
            meta.setLore(lore);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    private String processPlaceholders(String text, Player player, Job job, PlayerJobData data, boolean isActive) {
        String result = text
            .replace("{job}", job.getDisplayName())
            .replace("{description}", job.getDescription())
            .replace("{max_level}", String.valueOf(job.getMaxLevel()));
        
        if (data != null) {
            result = result
                .replace("{level}", String.valueOf(data.getLevel()))
                .replace("{exp}", String.valueOf(data.getExp()));
            
            JobLevel currentLevel = job.getLevel(data.getLevel());
            if (currentLevel != null) {
                result = result.replace("{required}", String.valueOf(currentLevel.getExpRequired()));
            }
        } else {
            result = result
                .replace("{level}", "0")
                .replace("{exp}", "0")
                .replace("{required}", "0");
        }
        
        result = result.replace("{status}", isActive ? "&a&lACTIVE" : (data != null ? "&6&lPAUSED" : "&7&lNEW"));
        
        return result;
    }
    
    private void playConfigSound(Player player, ConfigurationSection config) {
        if (config != null && config.contains("sound")) {
            String soundName = config.getString("sound");
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, 1f, 1f);
            } catch (IllegalArgumentException e) {
            }
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        if (title.contains("Job Selection")) {
            event.setCancelled(true);
            handleMainMenuClick(player, event.getCurrentItem(), event.getSlot());
        }
        else if (title.contains("Progress")) {
            event.setCancelled(true);
            handleProgressMenuClick(player, event.getCurrentItem(), event.getSlot());
        }
    }
    
    private void handleMainMenuClick(Player player, ItemStack clicked, int clickedSlot) {
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        int currentPage = currentMainMenuPage.getOrDefault(player.getUniqueId(), 1);
        String pageKey = "page-" + currentPage;
        ConfigurationSection pageConfig = plugin.getConfig().getConfigurationSection("gui.main-menu." + pageKey + ".buttons");
        if (pageConfig != null) {
            ConfigurationSection nextConfig = pageConfig.getConfigurationSection("next-page");
            if (nextConfig != null && clickedSlot == nextConfig.getInt("slot", -1)) {
                playConfigSound(player, nextConfig);
                openMainMenu(player, currentPage + 1);
                return;
            }
            ConfigurationSection prevConfig = pageConfig.getConfigurationSection("previous-page");
            if (prevConfig != null && clickedSlot == prevConfig.getInt("slot", -1)) {
                playConfigSound(player, prevConfig);
                openMainMenu(player, currentPage - 1);
                return;
            }
            ConfigurationSection closeConfig = pageConfig.getConfigurationSection("close");
            if (closeConfig != null && clickedSlot == closeConfig.getInt("slot", -1)) {
                playConfigSound(player, closeConfig);
                player.closeInventory();
                return;
            }
            ConfigurationSection infoConfig = pageConfig.getConfigurationSection("info");
            if (infoConfig != null && clickedSlot == infoConfig.getInt("slot", -1)) {
                return;
            }
        }
        ConfigurationSection jobsConfig = plugin.getConfig().getConfigurationSection("gui.main-menu." + pageKey + ".jobs");
        if (jobsConfig != null) {
            for (String jobId : jobsConfig.getKeys(false)) {
                ConfigurationSection jobConfig = jobsConfig.getConfigurationSection(jobId);
                if (jobConfig != null && clickedSlot == jobConfig.getInt("slot", -1)) {
                    Job job = plugin.getJobManager().getJob(jobId);
                    if (job == null) return;
                    
                    playConfigSound(player, jobConfig);

                    if (plugin.getJobManager().hasJobActive(player, jobId)) {
                        openProgressMenu(player, job);
                        return;
                    }

                    if (!plugin.getJobManager().canJoinMoreJobs(player)) {
                        int current = plugin.getJobManager().getActiveJobCount(player);
                        int limit = plugin.getJobManager().getJobLimit(player);
                        player.sendMessage(plugin.getMessage("job-limit-reached")
                            .replace("{current}", String.valueOf(current))
                            .replace("{max}", limit == Integer.MAX_VALUE ? "Unlimited" : String.valueOf(limit)));
                        return;
                    }

                    if (plugin.getJobManager().joinJob(player, jobId)) {
                        player.sendMessage(plugin.getMessage("job-joined")
                            .replace("{job}", job.getDisplayName()));
                        player.closeInventory();
                    } else {
                        player.sendMessage(plugin.getMessage("already-have-job"));
                    }
                    return;
                }
            }
        }
    }
    
    private void handleProgressMenuClick(Player player, ItemStack clicked, int slot) {
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        ConfigurationSection buttonsConfig = plugin.getConfig().getConfigurationSection("gui.progress-menu.buttons");
        if (buttonsConfig == null) return;
        
        String activeJobId = plugin.getJobManager().getActiveJobId(player);
        Job job = activeJobId != null ? plugin.getJobManager().getJob(activeJobId) : null;
        
        if (job == null) {
            for (Job j : plugin.getJobManager().getAllJobs()) {
                if (plugin.getJobManager().getJobData(player, j.getId()) != null) {
                    job = j;
                    break;
                }
            }
        }
        
        if (job == null) return;
        
        ConfigurationSection backConfig = buttonsConfig.getConfigurationSection("back");
        if (backConfig != null && slot == backConfig.getInt("slot", 45)) {
            playConfigSound(player, backConfig);
            int lastPage = currentMainMenuPage.getOrDefault(player.getUniqueId(), 1);
            openMainMenu(player, lastPage);
            return;
        }
        
        ConfigurationSection prevConfig = buttonsConfig.getConfigurationSection("previous-page");
        if (prevConfig != null && slot == prevConfig.getInt("slot", 48)) {
            playConfigSound(player, prevConfig);
            int currentPage = this.currentProgressPage.getOrDefault(player.getUniqueId(), 1);
            if (currentPage > 1) {
                openProgressMenu(player, job, currentPage - 1);
            }
            return;
        }
        
        ConfigurationSection nextConfig = buttonsConfig.getConfigurationSection("next-page");
        if (nextConfig != null && slot == nextConfig.getInt("slot", 50)) {
            playConfigSound(player, nextConfig);
            int currentPage = this.currentProgressPage.getOrDefault(player.getUniqueId(), 1);
            int levelsPerPage = plugin.getConfig().getInt("gui.progress-menu.levels-per-page", 28);
            int totalPages = (int) Math.ceil((double) job.getMaxLevel() / levelsPerPage);
            if (currentPage < totalPages) {
                openProgressMenu(player, job, currentPage + 1);
            }
            return;
        }
        
        ConfigurationSection leaveConfig = buttonsConfig.getConfigurationSection("leave");
        if (leaveConfig != null && slot == leaveConfig.getInt("slot", 53)) {
            playConfigSound(player, leaveConfig);
            if (plugin.getJobManager().leaveJob(player, job.getId())) {
                player.sendMessage(plugin.getMessage("job-left")
                    .replace("{job}", job.getDisplayName()));
                player.closeInventory();
            }
            return;
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                InventoryView view = player.getOpenInventory();
                boolean isJobMenu = view.getTitle().contains("Job Selection") || view.getTitle().contains("Progress");
                if (!isJobMenu) {
                    currentProgressPage.remove(player.getUniqueId());
                    currentMainMenuPage.remove(player.getUniqueId());
                }
            }, 10L);
        }
    }
}
package com.nexojobs;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Set;

public class JobListener implements Listener {
    
    private final NexoJobs plugin;
    
    private static final Set<String> MINER_JOBS = Set.of("miner");
    private static final Set<String> BUILDER_JOBS = Set.of("builder");
    private static final Set<String> FARMER_JOBS = Set.of("farmer");
    private static final Set<String> HUNTER_JOBS = Set.of("hunter");
    private static final Set<String> FISHING_JOBS = Set.of("fishing");
    private static final Set<String> LUMBERJACK_JOBS = Set.of("lumberjack");
    private static final Set<String> ENCHANTER_JOBS = Set.of("enchanter");
    private static final Set<String> ALCHEMIST_JOBS = Set.of("alchemist");
    private static final Set<String> BLACKSMITH_JOBS = Set.of("blacksmith");
    private static final Set<String> DIGGER_JOBS = Set.of("digger");
    private static final Set<String> CHEF_JOBS = Set.of("chef");
    private static final Set<String> MURDERER_JOBS = Set.of("murderer");
    
    public JobListener(NexoJobs plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Set<String> activeJobs = plugin.getJobManager().getActiveJobIds(player);
        
        if (activeJobs.isEmpty()) return;
        
        Material blockType = event.getBlock().getType();
        String blockName = blockType.name().toLowerCase();
        
        boolean hasMiner = hasAnyJob(activeJobs, MINER_JOBS);
        boolean hasLumberjack = hasAnyJob(activeJobs, LUMBERJACK_JOBS);
        boolean hasDigger = hasAnyJob(activeJobs, DIGGER_JOBS);
        boolean hasFarmer = hasAnyJob(activeJobs, FARMER_JOBS);

        if (hasMiner && (blockName.contains("ore") || blockName.contains("stone") || 
            blockName.contains("cobblestone") || blockName.contains("deepslate") ||
            blockName.contains("granite") || blockName.contains("diorite") || 
            blockName.contains("andesite"))) {
            
            String action = "break_" + blockName;
            handleAction(player, action, "miner");
        }

        else if (hasLumberjack && (blockName.contains("log") || blockName.contains("wood") || 
            blockName.contains("mushroom"))) {
            
            String action = "break_" + blockName;
            handleAction(player, action, "lumberjack");
        }

        else if (hasDigger && (blockName.contains("dirt") || blockName.contains("grass") || 
            blockName.contains("sand") || blockName.contains("gravel") || 
            blockName.contains("clay") || blockName.contains("snow") || 
            blockName.contains("soul") || blockName.contains("mud"))) {
            
            String action = "break_" + blockName;
            handleAction(player, action, "digger");
        }

        if (hasFarmer) {
            Block block = event.getBlock();

            if (block.getBlockData() instanceof Ageable) {
                Ageable ageable = (Ageable) block.getBlockData();

                if (ageable.getAge() == ageable.getMaximumAge()) {
                    String cropType = blockName;

                    String action = null;
                    switch (blockType) {
                        case WHEAT:
                            action = "harvest_wheat";
                            break;
                        case CARROTS:
                            action = "harvest_carrots";
                            break;
                        case POTATOES:
                            action = "harvest_potatoes";
                            break;
                        case BEETROOTS:
                            action = "harvest_beetroots";
                            break;
                        case SWEET_BERRY_BUSH:
                            action = "harvest_sweet_berry_bush";
                            break;
                        case COCOA:
                            action = "harvest_cocoa";
                            break;
                        case NETHER_WART:
                            action = "harvest_nether_wart";
                            break;
                    }
                    
                    if (action != null) {
                        handleAction(player, action, "farmer");
                    }
                }
            }

            else if (blockType == Material.MELON) {
                handleAction(player, "harvest_melon", "farmer");
            } else if (blockType == Material.PUMPKIN) {
                handleAction(player, "harvest_pumpkin", "farmer");
            } else if (blockType == Material.SUGAR_CANE) {
                handleAction(player, "harvest_sugar_cane", "farmer");
            } else if (blockType == Material.CACTUS) {
                handleAction(player, "harvest_cactus", "farmer");
            } else if (blockType == Material.BAMBOO) {
                handleAction(player, "harvest_bamboo", "farmer");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Set<String> activeJobs = plugin.getJobManager().getActiveJobIds(player);

        if (!hasAnyJob(activeJobs, BUILDER_JOBS)) return;
        
        String action = "place_" + event.getBlock().getType().name().toLowerCase();
        handleAction(player, action, "builder");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player)) return;
        
        Player player = (Player) event.getBreeder();
        Set<String> activeJobs = plugin.getJobManager().getActiveJobIds(player);

        if (!hasAnyJob(activeJobs, FARMER_JOBS)) return;
        
        String action = "breed_" + event.getEntityType().name().toLowerCase();
        handleAction(player, action, "farmer");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        
        Player killer = event.getEntity().getKiller();
        Set<String> activeJobs = plugin.getJobManager().getActiveJobIds(killer);

        if (!hasAnyJob(activeJobs, HUNTER_JOBS)) return;
        
        String action = "kill_" + event.getEntityType().name().toLowerCase();
        handleAction(killer, action, "hunter");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        
        Player player = event.getPlayer();
        Set<String> activeJobs = plugin.getJobManager().getActiveJobIds(player);

        if (!hasAnyJob(activeJobs, FISHING_JOBS)) return;
        
        if (event.getCaught() instanceof Item) {
            Item caughtItem = (Item) event.getCaught();
            ItemStack itemStack = caughtItem.getItemStack();
            String action = "catch_" + itemStack.getType().name().toLowerCase();
            handleAction(player, action, "fishing");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        Set<String> activeJobs = plugin.getJobManager().getActiveJobIds(player);

        if (!hasAnyJob(activeJobs, ENCHANTER_JOBS)) return;
        
        handleAction(player, "enchant_item", "enchanter");
        
        int expLevel = event.getExpLevelCost();
        if (expLevel >= 30) {
            handleAction(player, "enchant_level_30", "enchanter");
        } else if (expLevel >= 25) {
            handleAction(player, "enchant_level_25", "enchanter");
        } else if (expLevel >= 20) {
            handleAction(player, "enchant_level_20", "enchanter");
        } else if (expLevel >= 15) {
            handleAction(player, "enchant_level_15", "enchanter");
        } else if (expLevel >= 10) {
            handleAction(player, "enchant_level_10", "enchanter");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvilUse(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Set<String> activeJobs = plugin.getJobManager().getActiveJobIds(player);

        if (!hasAnyJob(activeJobs, ENCHANTER_JOBS) && !hasAnyJob(activeJobs, BLACKSMITH_JOBS)) return;
        
        if (event.getInventory().getType() == InventoryType.ANVIL) {
            if (event.getSlotType() == InventoryType.SlotType.RESULT && event.getCurrentItem() != null) {
                ItemStack result = event.getCurrentItem();
                
                if (result.hasItemMeta() && result.getItemMeta().hasDisplayName()) {
                    handleAction(player, "use_anvil_rename", "enchanter");
                } else if (result.hasItemMeta() && result.getItemMeta().hasEnchants()) {
                    handleAction(player, "use_anvil_combine", "enchanter");
                } else {
                    handleAction(player, "use_anvil_repair", "enchanter");
                }
            }
        }
        
        if (event.getInventory().getType() == InventoryType.GRINDSTONE) {
            if (event.getSlotType() == InventoryType.SlotType.RESULT && event.getCurrentItem() != null) {
                handleAction(player, "use_grindstone", "enchanter");
            }
        }
        
        if (event.getInventory().getType() == InventoryType.SMITHING) {
            if (event.getSlotType() == InventoryType.SlotType.RESULT && event.getCurrentItem() != null) {
                handleAction(player, "use_smithing_table", "blacksmith");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (!(event.getContents().getHolder() instanceof org.bukkit.block.BrewingStand)) return;
        
        org.bukkit.block.BrewingStand stand = (org.bukkit.block.BrewingStand) event.getContents().getHolder();
        
        Player nearestPlayer = null;
        double nearestDistance = 5.0;
        
        for (Player player : stand.getWorld().getPlayers()) {
            Set<String> activeJobs = plugin.getJobManager().getActiveJobIds(player);

            if (!hasAnyJob(activeJobs, ALCHEMIST_JOBS)) continue;
            
            double distance = player.getLocation().distance(stand.getLocation());
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = player;
            }
        }
        
        if (nearestPlayer != null) {
            handleAction(nearestPlayer, "brew_awkward_potion", "alchemist");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        Set<String> activeJobs = plugin.getJobManager().getActiveJobIds(player);
        
        Material item = event.getItemType();
        String itemName = item.name().toLowerCase();

        if (hasAnyJob(activeJobs, BLACKSMITH_JOBS) && 
            (itemName.contains("ingot") || itemName.contains("scrap"))) {
            
            String action = "smelt_" + itemName;
            handleAction(player, action, "blacksmith");
        }

        if (hasAnyJob(activeJobs, CHEF_JOBS)) {
            if (item == Material.COOKED_BEEF || item == Material.COOKED_PORKCHOP ||
                item == Material.COOKED_CHICKEN || item == Material.COOKED_MUTTON ||
                item == Material.COOKED_RABBIT || item == Material.COOKED_COD ||
                item == Material.COOKED_SALMON || item == Material.BAKED_POTATO ||
                item == Material.DRIED_KELP) {
                
                String action = "cook_" + itemName.replace("cooked_", "").replace("baked_", "");
                handleAction(player, action, "chef");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Set<String> activeJobs = plugin.getJobManager().getActiveJobIds(player);
        
        if (activeJobs.isEmpty()) return;
        
        ItemStack result = event.getRecipe().getResult();
        String itemName = result.getType().name().toLowerCase();

        if (hasAnyJob(activeJobs, BLACKSMITH_JOBS) && 
            (itemName.contains("sword") || itemName.contains("pickaxe") || 
             itemName.contains("axe") || itemName.contains("shovel") || 
             itemName.contains("hoe") || itemName.contains("helmet") || 
             itemName.contains("chestplate") || itemName.contains("leggings") || 
             itemName.contains("boots") || itemName.contains("shield") || 
             itemName.contains("shears"))) {
            
            String action = "craft_" + itemName;
            handleAction(player, action, "blacksmith");
        }

        else if (hasAnyJob(activeJobs, CHEF_JOBS) && 
            (itemName.equals("bread") || itemName.equals("cake") || 
             itemName.equals("cookie") || itemName.contains("pie") || 
             itemName.contains("stew") || itemName.contains("soup"))) {
            
            String action = "craft_" + itemName;
            handleAction(player, action, "chef");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        if (killer == null || killer == victim) return;
        
        Set<String> activeJobs = plugin.getJobManager().getActiveJobIds(killer);

        if (!hasAnyJob(activeJobs, MURDERER_JOBS)) return;
        
        handleAction(killer, "kill_player", "murderer");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getJobManager().savePlayerOnLogout(event.getPlayer());
    }

    private boolean hasAnyJob(Set<String> playerJobs, Set<String> targetJobs) {
        for (String job : targetJobs) {
            if (playerJobs.contains(job)) {
                return true;
            }
        }
        return false;
    }

    private void handleAction(Player player, String actionName, String expectedJobType) {
        Set<String> activeJobIds = plugin.getJobManager().getActiveJobIds(player);
        
        if (activeJobIds.isEmpty()) return;

        for (String jobId : activeJobIds) {
            if (!jobId.equalsIgnoreCase(expectedJobType)) continue;
            
            PlayerJobData data = plugin.getJobManager().getJobData(player, jobId);
            if (data == null) continue;
            
            Job job = plugin.getJobManager().getJob(jobId);
            if (job == null) continue;
            
            int expGain = job.getExpForAction(actionName);
            if (expGain <= 0) continue;
            
            if (data.getLevel() >= job.getMaxLevel()) continue;

            plugin.getJobManager().addJobExp(player, jobId, expGain);
            
            JobLevel currentLevel = job.getLevel(data.getLevel());
            if (currentLevel != null && data.getExp() >= currentLevel.getExpRequired()) {
                levelUp(player, job, data);
            }
        }

    }
    
    private void levelUp(Player player, Job job, PlayerJobData data) {
        if (data.getLevel() >= job.getMaxLevel()) return;
        
        data.setLevel(data.getLevel() + 1);
        data.setExp(0);
        
        JobLevel newLevel = job.getLevel(data.getLevel());
        if (newLevel == null) return;
        
        if (newLevel.getMoneyReward() > 0 && plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(player, newLevel.getMoneyReward());
            player.sendMessage(plugin.color("&a&l+ $" + newLevel.getMoneyReward() + " &7(" + job.getDisplayName() + "&7)"));
        }
        
        String title = plugin.color(plugin.getConfig().getString("messages.level-up-title", "&a&lLEVEL UP!"));
        String subtitle = plugin.color(
            plugin.getConfig().getString("messages.level-up-subtitle", "&e{job} &7Level &b{level}")
                .replace("{job}", job.getDisplayName())
                .replace("{level}", String.valueOf(data.getLevel()))
        );
        
        player.sendTitle(title, subtitle, 10, 70, 20);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        
        String levelUpMsg = plugin.getMessage("level-up-broadcast")
            .replace("{prefix}", plugin.getPrefix())
            .replace("{player}", player.getName())
            .replace("{job}", job.getDisplayName())
            .replace("{level}", String.valueOf(data.getLevel()));
        player.sendMessage(levelUpMsg);
    }
}
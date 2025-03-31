package org.SolarSystem.zamanKiran;

import org.SolarSystem.zamanKiran.gui.SkillSelectionGUI;
import org.SolarSystem.zamanKiran.gui.WeaponSelectionGUI;
import org.SolarSystem.zamanKiran.items.ItemManager;
import org.SolarSystem.zamanKiran.skills.*;
import org.SolarSystem.zamanKiran.systems.HealthSystem;
import org.SolarSystem.zamanKiran.systems.TeamSystem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZamanKiran extends JavaPlugin implements Listener {
    private ItemManager itemManager;
    private HealthSystem healthSystem;
    private TeamSystem teamSystem;
    private SkillSelectionGUI skillGUI;
    private WeaponSelectionGUI weaponGUI;
    private Map<UUID, Map<Integer, Skill>> playerSkills;
    private ChronoSphere chronoSphere;
    private WaveCutting waveCutting;
    private BallLightning ballLightning;
    private WeaponThrow weaponThrow;
    private Portal portal;

    @Override
    public void onEnable() {
        itemManager = new ItemManager(this);
        healthSystem = new HealthSystem(this);
        teamSystem = new TeamSystem(this);
        skillGUI = new SkillSelectionGUI();
        weaponGUI = new WeaponSelectionGUI(this);
        playerSkills = new HashMap<>();
        
        chronoSphere = new ChronoSphere(this);
        waveCutting = new WaveCutting(this);
        ballLightning = new BallLightning(this);
        weaponThrow = new WeaponThrow(this);
        portal = new Portal(this);

        getServer().getPluginManager().registerEvents(this, this);

        getCommand("zamankiran").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player && args.length > 0 && args[0].equalsIgnoreCase("ver")) {
                Player player = (Player) sender;
                ItemStack weapon = itemManager.createWeapon(player);
                player.getInventory().addItem(weapon);
                healthSystem.initializeHealth(player);
                initializePlayerSkills(player);
                player.sendMessage(ChatColor.GREEN + "Zaman Kıran silahı verildi!");
                return true;
            }
            return false;
        });

        getCommand("zamankirangui").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                
                if (mainHand.getType() == Material.AIR) {
                    weaponGUI.openGUI(player);
                } else if (itemManager.isWeapon(mainHand)) {
                    skillGUI.openGUI(player);
                } else {
                    player.sendMessage(ChatColor.RED + "Zaman Kıran silahını tutarken veya eliniz boşken kullanın!");
                }
                return true;
            }
            return false;
        });

        startActionBarTask();

        getLogger().info("ZamanKiran plugin has been enabled!");
    }

    private void initializePlayerSkills(Player player) {
        Map<Integer, Skill> skills = new HashMap<>();
        skills.put(0, chronoSphere);
        skills.put(1, ballLightning);
        skills.put(2, waveCutting);
        skills.put(3, weaponThrow);
        playerSkills.put(player.getUniqueId(), skills);
    }

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (itemManager.isWeapon(mainHand)) {
                        int selectedSlot = skillGUI.getSelectedSlot(player);
                        Map<Integer, Skill> skills = playerSkills.get(player.getUniqueId());
                        if (skills != null) {
                            Skill selectedSkill = skills.get(selectedSlot);
                            if (selectedSkill != null) {
                                String skillInfo = String.format("§6Seçili Yetenek: §f%s §8| §eSlot: §f%d", 
                                    selectedSkill.getName(), selectedSlot);
                                sendActionBar(player, skillInfo);
                            }
                        }
                    } else {
                        sendActionBar(player, "§cSilah: §7Yok §8| §eHız: §70.0 §8| §eYön: §7↑");
                    }
                }
            }
        }.runTaskTimer(this, 0L, 5L);
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (itemManager.isWeapon(item)) {
            event.setCancelled(true);

            Map<Integer, Skill> skills = playerSkills.get(player.getUniqueId());
            if (skills == null) {
                initializePlayerSkills(player);
                skills = playerSkills.get(player.getUniqueId());
            }

            int selectedSlot = skillGUI.getSelectedSlot(player);
            Skill selectedSkill = skills.get(selectedSlot);
            if (selectedSkill == null) return;

            if (selectedSlot == 0) {
                if (event.getAction() == Action.LEFT_CLICK_AIR || 
                    event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    if (!player.isSneaking()) {
                    } else {
                        // TODO: Implement Döndürme Işınları
                    }
                }
                else if (event.getAction() == Action.RIGHT_CLICK_AIR || 
                         event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (!player.isSneaking()) {
                        // TODO: Implement Recursion
                    } else {
                        chronoSphere.cast(player);
                    }
                }
            }
            else if (selectedSlot == 1) {
                if (event.getAction() == Action.LEFT_CLICK_AIR || 
                    event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    if (player.isSneaking()) {
                        // TODO: Implement Zamanı Geriye Sar
                    }
                }
                else if (event.getAction() == Action.RIGHT_CLICK_AIR || 
                         event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (!player.isSneaking()) {
                        ballLightning.cast(player);
                    } else if (player.isSneaking()) {
                        portal.cast(player);
                    }
                }
            }
            else if (selectedSlot == 2) {
                if (event.getAction() == Action.LEFT_CLICK_AIR || 
                    event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    if (!player.isSneaking()) {
                        waveCutting.cast(player);
                    }
                }
                else if (event.getAction() == Action.RIGHT_CLICK_AIR || 
                         event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (!player.isSneaking()) {
                        weaponThrow.cast(player);
                    }
                }
            }
            else if (selectedSlot == 3) {
                if (event.getAction() == Action.RIGHT_CLICK_AIR || 
                    event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    // TODO: Implement Havuç
                }
            }

            itemManager.updateWeaponLore(item, player);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (itemManager.isWeapon(item) && event.getRightClicked() instanceof Player) {
            Player target = (Player) event.getRightClicked();
            if (player.getInventory().getHeldItemSlot() == 8) {
                teamSystem.addTeamMember(player, target);
                player.sendMessage(ChatColor.GREEN + target.getName() + " takıma eklendi!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (title.equals(ChatColor.DARK_PURPLE + "Zaman Kıran Yetenekleri")) {
            if (clicked != null && clicked.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                int rawSlot = event.getRawSlot();
                int skillSlot = skillGUI.convertGUISlotToSkillSlot(rawSlot);
                skillGUI.selectSlot(player, skillSlot);
                player.sendMessage(ChatColor.GREEN + "Yetenek slotu " + skillSlot + " seçildi!");
                skillGUI.openGUI(player);
            }
        } 
        else if (title.equals(ChatColor.GOLD + "Zaman Kıran Silahları")) {
            if (clicked != null && clicked.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                weaponGUI.handleClick(event);
            }
        }
        else if (title.equals(ChatColor.DARK_PURPLE + "Portal Oluştur")) {
            portal.handleAnvilClick(event);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (itemManager.isWeapon(item)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            
            weaponThrow.cast(player);
        }
    }

    @Override
    public void onDisable() {
        if (healthSystem != null) {
            getServer().getOnlinePlayers().forEach(player -> 
                healthSystem.removeBossBar(player));
        }
        
        if (playerSkills != null) {
            playerSkills.values().forEach(skillMap -> 
                skillMap.values().forEach(skill -> {
                    if (skill instanceof ChronoSphere) {
                        ((ChronoSphere) skill).endSkill();
                    }
                }));
        }
        
        getLogger().info("ZamanKiran plugin has been disabled!");
    }
} 
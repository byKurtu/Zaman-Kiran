package org.SolarSystem.zamanKiran.listeners;

import org.SolarSystem.zamanKiran.ZamanKiran;
import org.SolarSystem.zamanKiran.managers.WeaponManager;
import org.SolarSystem.zamanKiran.systems.CooldownSystem;
import org.SolarSystem.zamanKiran.skills.Skill;
import org.SolarSystem.zamanKiran.skills.ThrowableWeapon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

import java.util.logging.Level;

public class PlayerInteractListener implements Listener {
    private final ZamanKiran plugin;
    private final WeaponManager weaponManager;
    private final CooldownSystem cooldownSystem;

    public PlayerInteractListener(ZamanKiran plugin) {
        this.plugin = plugin;
        this.weaponManager = WeaponManager.getInstance(plugin);
        this.cooldownSystem = CooldownSystem.getInstance();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        try {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();

            // Early return if no item or not a weapon
            if (item == null || !weaponManager.isWeapon(item)) {
                return;
            }

            // Cancel vanilla interaction
            event.setCancelled(true);

            // Debug logging
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Weapon interaction detected for player: " + 
                    player.getName() + " with item: " + item.getType());
            }

            // Check cooldown
            if (cooldownSystem.isOnCooldown(player, "weapon_use")) {
                long remaining = cooldownSystem.getRemainingCooldown(player, "weapon_use");
                String formattedTime = cooldownSystem.formatRemainingTime(remaining);
                player.sendMessage(plugin.getConfig().getString("messages.cooldown")
                    .replace("%time%", formattedTime));
                return;
            }

            // Handle different click types
            if (event.getAction() == Action.LEFT_CLICK_AIR || 
                event.getAction() == Action.LEFT_CLICK_BLOCK) {
                handleLeftClick(player);
            } else if (event.getAction() == Action.RIGHT_CLICK_AIR || 
                      event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                handleRightClick(player);
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, 
                "Error handling player interaction: " + e.getMessage(), e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        try {
            Player player = event.getPlayer();
            ItemStack mainHand = event.getMainHandItem();

            if (mainHand != null && weaponManager.isWeapon(mainHand)) {
                event.setCancelled(true);
                
                if (player.isSneaking()) {
                    handleSpecialAbility(player);
                } else {
                    cycleWeaponMode(player);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, 
                "Error handling hand swap: " + e.getMessage(), e);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!weaponManager.isWeapon(item)) {
            return;
        }

        int selectedSlot = plugin.getSkillGUI().getSelectedSlot(player);
        if (selectedSlot < 0) {
            return;
        }

        Skill skill = plugin.getSkills().get(selectedSlot);
        if (skill instanceof ThrowableWeapon && event.isSneaking()) {
            ((ThrowableWeapon) skill).cycleThrowMode(player);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (weaponManager.isWeapon(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        
        if (item != null && weaponManager.isWeapon(item)) {
            int selectedSlot = plugin.getSkillGUI().getSelectedSlot(player);
            if (selectedSlot >= 0) {
                Skill skill = plugin.getSkills().get(selectedSlot);
                if (skill != null) {
                    skill.endSkill();
                }
            }
        }
    }

    private void handleLeftClick(Player player) {
        // Apply cooldown
        cooldownSystem.setCooldown(player, "weapon_use", 
            plugin.getConfig().getInt("weapons.time_breaker.cooldown", 5));
            
        // Execute primary ability
        int selectedSlot = plugin.getSkillGUI().getSelectedSlot(player);
        if (selectedSlot >= 0) {
            plugin.getSkills().get(selectedSlot).cast(player);
        }
    }

    private void handleRightClick(Player player) {
        // Handle secondary ability
        int selectedSlot = plugin.getSkillGUI().getSelectedSlot(player);
        if (selectedSlot >= 0) {
            plugin.getSkills().get(selectedSlot).cast(player);
        }
    }

    private void handleSpecialAbility(Player player) {
        // Handle special ability (Shift + F)
        int selectedSlot = plugin.getSkillGUI().getSelectedSlot(player);
        if (selectedSlot >= 0) {
            plugin.getSkills().get(selectedSlot).cast(player);
        }
    }

    private void cycleWeaponMode(Player player) {
        // Implement weapon mode cycling logic here
        player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
            "§eSilah modu değiştirildi!");
    }
} 
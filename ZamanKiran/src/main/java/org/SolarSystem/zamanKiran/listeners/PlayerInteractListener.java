package org.SolarSystem.zamanKiran.listeners;

import org.SolarSystem.zamanKiran.ZamanKiran;
import org.SolarSystem.zamanKiran.skills.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

public class PlayerInteractListener implements Listener {
    private final ZamanKiran plugin;

    public PlayerInteractListener(ZamanKiran plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Silah kontrolü
        if (item == null || !plugin.getItemManager().isWeapon(item)) {
            return;
        }

        event.setCancelled(true);

        // 1. Seçili slot kontrolü
        int slot = plugin.getSkillGUI().getSelectedSlot(player);
        if (slot < 0) {
            return;
        }

        // 2. Skill kontrolü
        Skill skill = plugin.getSkills().get(slot);
        if (skill == null) {
            return;
        }

        // 3. Eğilme durumu kontrolü
        boolean isShiftHeld = player.isSneaking();
        
        // 4. Tıklama türü kontrolü
        Action action = event.getAction();
        boolean isLeftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        boolean isRightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;

        // Skill aktivasyonu
        if (isShiftHeld) {
            if (isLeftClick && skill instanceof ShadowCouncil) {
                ((ShadowCouncil) skill).handleShiftLeftClick(player);
            }
        } else {
            if (isLeftClick) {
                if (skill instanceof ShadowCouncil) {
                    ((ShadowCouncil) skill).handleLeftClick(player);
                } else if (skill instanceof ThrowableWeapon) {
                    ((ThrowableWeapon) skill).cast(player);
                } else {
                    skill.cast(player);
                }
            } else if (isRightClick) {
                // Recursion sadece slot 0'da ve eğilmeden sağ tık ile çalışır
                if (slot == 0 && skill instanceof ThrowableWeapon) {
                    ((ThrowableWeapon) skill).castRecursion(player);
                } else if (skill instanceof ShadowCouncil) {
                    ((ShadowCouncil) skill).handleRightClick(player);
                } else {
                    skill.cast(player);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!plugin.getItemManager().isWeapon(item)) {
            return;
        }

        event.setCancelled(true);

        int selectedSlot = plugin.getSkillGUI().getSelectedSlot(player);
        if (selectedSlot < 0) {
            return;
        }

        Skill skill = plugin.getSkills().get(selectedSlot);
        if (skill instanceof ThrowableWeapon) {
            ((ThrowableWeapon) skill).cycleThrowMode(player);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!plugin.getItemManager().isWeapon(item)) {
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
        if (plugin.getItemManager().isWeapon(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        
        if (item != null && plugin.getItemManager().isWeapon(item)) {
            int selectedSlot = plugin.getSkillGUI().getSelectedSlot(player);
            if (selectedSlot >= 0) {
                Skill skill = plugin.getSkills().get(selectedSlot);
                if (skill != null) {
                    skill.endSkill();
                }
            }
        }
    }
} 
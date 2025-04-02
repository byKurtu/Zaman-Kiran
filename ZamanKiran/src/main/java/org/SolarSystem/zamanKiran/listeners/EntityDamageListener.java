package org.SolarSystem.zamanKiran.listeners;

import org.SolarSystem.zamanKiran.ZamanKiran;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class EntityDamageListener implements Listener {
    private final ZamanKiran plugin;

    public EntityDamageListener(ZamanKiran plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (plugin.getItemManager().isWeapon(item)) {
            Entity target = event.getEntity();
            event.setCancelled(true);
            int selectedSlot = plugin.getSkillGUI().getSelectedSlot(player);
            if (selectedSlot >= 0) {
                plugin.getSkills().get(selectedSlot).cast(player);
            }
        }
    }
} 
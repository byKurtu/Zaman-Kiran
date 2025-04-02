package org.SolarSystem.zamanKiran.listeners;

import org.SolarSystem.zamanKiran.ZamanKiran;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.SolarSystem.zamanKiran.skills.Portal;

public class InventoryClickListener implements Listener {
    private final ZamanKiran plugin;

    public InventoryClickListener(ZamanKiran plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals("Zaman Kıran Yetenekleri")) {
            event.setCancelled(true);
            plugin.getSkillGUI().handleClick(player, event.getSlot());
        } else if (title.equals("Zaman Kıran Silahları")) {
            event.setCancelled(true);
            plugin.getWeaponGUI().handleClick(event);
        } else if (title.equals("§5§lPortal Oluştur") || 
                  title.equals("§b§lOyuncu Seç") ||
                  title.equals("§a§lDünya Seç") ||
                  title.equals("§e§lKoordinat Gir")) {
            event.setCancelled(true);
            Portal portal = (Portal) plugin.getSkills().get(3); // Portal yeteneği slot 3'te
            portal.handleInventoryClick(event);
        }
    }
} 
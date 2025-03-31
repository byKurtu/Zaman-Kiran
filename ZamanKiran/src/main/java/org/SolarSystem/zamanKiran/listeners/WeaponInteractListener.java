package org.SolarSystem.zamanKiran.listeners;

import org.SolarSystem.zamanKiran.Core;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Player;

public class WeaponInteractListener implements Listener {
    private final Core weaponManager;

    public WeaponInteractListener(Core weaponManager) {
        this.weaponManager = weaponManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
    }
}
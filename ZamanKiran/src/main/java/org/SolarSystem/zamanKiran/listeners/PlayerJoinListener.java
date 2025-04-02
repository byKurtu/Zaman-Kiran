package org.SolarSystem.zamanKiran.listeners;

import org.SolarSystem.zamanKiran.ZamanKiran;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final ZamanKiran plugin;

    public PlayerJoinListener(ZamanKiran plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.initializePlayerSkills(player);
    }
} 
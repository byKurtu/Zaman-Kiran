package org.SolarSystem.zamanKiran.modules;



import org.bukkit.Bukkit;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;

public class HealthBossBar {
    private static final double MAX_HEALTH = 9200000.0;
    private final BossBar bossBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SEGMENTED_20);

    public void update(Player player, double currentHealth) {
        bossBar.setTitle(player.getName() + " " + (currentHealth / MAX_HEALTH * 100) + "%");
        bossBar.setProgress(currentHealth / MAX_HEALTH);
        player.getNearbyEntities(20, 20, 20).forEach(e -> {
            if (e instanceof Player) bossBar.addPlayer((Player) e);
        });
    }
}

package org.SolarSystem.zamanKiran.systems;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;

public class HealthSystem {
    private static final double MAX_HEALTH = 9200000.0;
    private final Map<UUID, Double> playerHealth = new HashMap<>();
    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();
    private final Plugin plugin;

    public HealthSystem(Plugin plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    public void initializeHealth(Player player) {
        UUID playerId = player.getUniqueId();
        playerHealth.put(playerId, MAX_HEALTH);
        updateBossBar(player);
    }

    public void updateBossBar(Player player) {
        try {
            UUID playerId = player.getUniqueId();
            double health = playerHealth.getOrDefault(playerId, MAX_HEALTH);
            double percentage = (health / MAX_HEALTH) * 100;

            String barTitle = ChatColor.translateAlternateColorCodes('&',
                player.getName() + " &c" + String.format("%.0f", health) + "&f/&c" + String.format("%.0f", MAX_HEALTH) + 
                " &6" + String.format("%.1f", percentage) + "%");

            BossBar bossBar = playerBossBars.get(playerId);
            if (bossBar == null) {
                bossBar = Bukkit.createBossBar(barTitle, BarColor.RED, BarStyle.SEGMENTED_20);
                playerBossBars.put(playerId, bossBar);
            }

            bossBar.setTitle(barTitle);
            bossBar.setProgress(Math.max(0, Math.min(1, health / MAX_HEALTH)));

            for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                if (nearbyPlayer.getLocation().distance(player.getLocation()) <= 20) {
                    if (!bossBar.getPlayers().contains(nearbyPlayer)) {
                        bossBar.addPlayer(nearbyPlayer);
                    }
                } else {
                    bossBar.removePlayer(nearbyPlayer);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error updating boss bar: " + e.getMessage());
        }
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (UUID playerId : new HashSet<>(playerHealth.keySet())) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            updateBossBar(player);
                            
                            double realHealthPercentage = (player.getHealth() / player.getMaxHealth()) * MAX_HEALTH;
                            if (Math.abs(getHealth(player) - realHealthPercentage) > 0.1) {
                                playerHealth.put(playerId, realHealthPercentage);
                            }
                        } else {
                            playerHealth.remove(playerId);
                            BossBar bossBar = playerBossBars.remove(playerId);
                            if (bossBar != null) {
                                bossBar.removeAll();
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error in health update task: " + e.getMessage());
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void damage(Player player, double damage) {
        UUID playerId = player.getUniqueId();
        double currentHealth = playerHealth.getOrDefault(playerId, MAX_HEALTH);
        double newHealth = Math.max(0, currentHealth - damage);
        playerHealth.put(playerId, newHealth);
        updateBossBar(player);

        player.playEffect(player.getLocation(), org.bukkit.Effect.STEP_SOUND, 152);
        
        double healthPercentage = newHealth / MAX_HEALTH;
        player.setHealth(Math.max(0.1, healthPercentage * player.getMaxHealth()));
    }

    public void heal(Player player, double amount) {
        UUID playerId = player.getUniqueId();
        double currentHealth = playerHealth.getOrDefault(playerId, MAX_HEALTH);
        double newHealth = Math.min(MAX_HEALTH, currentHealth + amount);
        playerHealth.put(playerId, newHealth);
        updateBossBar(player);

        player.spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0);
        
        double healthPercentage = newHealth / MAX_HEALTH;
        player.setHealth(Math.min(player.getMaxHealth(), healthPercentage * player.getMaxHealth()));
    }

    public double getHealth(Player player) {
        return playerHealth.getOrDefault(player.getUniqueId(), MAX_HEALTH);
    }

    public void removeBossBar(Player player) {
        try {
            if (player == null) return;
            
            UUID playerId = player.getUniqueId();
            BossBar bossBar = playerBossBars.remove(playerId);
            if (bossBar != null) {
                bossBar.removeAll();
            }
            playerHealth.remove(playerId);
        } catch (Exception e) {
            plugin.getLogger().warning("Error removing boss bar: " + e.getMessage());
        }
    }

    public boolean isAlive(Player player) {
        return getHealth(player) > 0;
    }

    public double getMaxHealth() {
        return MAX_HEALTH;
    }

    public double getHealthPercentage(Player player) {
        return (getHealth(player) / MAX_HEALTH) * 100;
    }
}


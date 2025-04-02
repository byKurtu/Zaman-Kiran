package org.SolarSystem.zamanKiran.skills;

import org.SolarSystem.zamanKiran.ZamanKiran;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TimeStop extends Skill {
    private static final double RADIUS = 8.0;
    private static final int DURATION = 10 * 20; // 10 seconds
    private final Set<Entity> frozenEntities = new HashSet<>();

    public TimeStop(ZamanKiran plugin) {
        super(plugin, "Zaman Durdurma", 60, 80);
    }

    @Override
    public void cast(Player caster) {
        Location center = caster.getLocation();
        Collection<Entity> nearbyEntities = center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS);

        // Visual effects at caster
        createTimeStopEffect(center);

        for (Entity entity : nearbyEntities) {
            if (entity != caster) {
                freezeEntity(entity);
            }
        }

        // Schedule unfreeze
        new BukkitRunnable() {
            @Override
            public void run() {
                unfreezeAll();
            }
        }.runTaskLater(plugin, DURATION);
    }

    private void freezeEntity(Entity entity) {
        if (frozenEntities.contains(entity)) return;

        // Store original velocity
        Vector velocity = entity.getVelocity();
        entity.setVelocity(new Vector(0, 0, 0));
        frozenEntities.add(entity);

        // Visual effects
        Location loc = entity.getLocation();
        entity.getWorld().spawnParticle(Particle.PORTAL, loc.add(0, 1, 0), 20, 0.5, 1, 0.5, 0.1);
        entity.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 2.0f);

        if (entity instanceof Player) {
            Player player = (Player) entity;
            player.sendTitle(
                ChatColor.AQUA + "ZAMAN DURDU!",
                ChatColor.GRAY + "Hareket edemiyorsun...",
                10, 40, 10
            );
        }

        // Particle effect task
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!frozenEntities.contains(entity)) {
                    this.cancel();
                    return;
                }

                Location particleLoc = entity.getLocation().add(0, 1, 0);
                entity.getWorld().spawnParticle(
                    Particle.END_ROD,
                    particleLoc,
                    3, 0.2, 0.2, 0.2, 0.02
                );
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void unfreezeAll() {
        for (Entity entity : frozenEntities) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                player.sendTitle(
                    ChatColor.GREEN + "ZAMAN AKIŞI NORMALE DÖNDÜ",
                    "",
                    10, 20, 10
                );
            }

            // Visual effects
            Location loc = entity.getLocation().add(0, 1, 0);
            entity.getWorld().spawnParticle(
                Particle.FLASH,
                loc,
                1, 0, 0, 0, 0
            );
            entity.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.5f);
        }
        frozenEntities.clear();
    }

    private void createTimeStopEffect(Location location) {
        new BukkitRunnable() {
            double radius = 0;
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 20) {
                    this.cancel();
                    return;
                }

                radius += 0.4;
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = location.clone().add(x, 0.1, z);

                    location.getWorld().spawnParticle(
                        Particle.END_ROD,
                        particleLoc,
                        1, 0, 0, 0, 0
                    );
                }

                if (ticks % 4 == 0) {
                    location.getWorld().playSound(
                        location,
                        Sound.BLOCK_BEACON_DEACTIVATE,
                        0.5f,
                        2.0f + (float)radius / 10
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
} 
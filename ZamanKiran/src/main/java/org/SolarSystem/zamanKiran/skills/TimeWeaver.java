package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class TimeWeaver extends Skill {
    private static final double RADIUS = 8.0;
    private static final double DAMAGE = 20.0;
    private static final int DURATION = 10 * 20; // 10 saniye
    private final Map<UUID, Set<Entity>> boundEntities = new HashMap<>();
    private final Map<UUID, Location> centerLocations = new HashMap<>();
    
    public TimeWeaver(Plugin plugin) {
        super(plugin, "Time Weaver", 20, 60);
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        Location center = caster.getLocation();
        Set<Entity> targets = new HashSet<>();
        boundEntities.put(caster.getUniqueId(), targets);
        centerLocations.put(caster.getUniqueId(), center);

        // Zaman iplikleri efekti
        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;
            List<Location> threadPoints = new ArrayList<>();

            @Override
            public void run() {
                if (!isActive || ticks++ >= DURATION) {
                    boundEntities.remove(caster.getUniqueId());
                    centerLocations.remove(caster.getUniqueId());
                    this.cancel();
                    return;
                }

                // İplik noktalarını güncelle
                threadPoints.clear();
                for (double a = 0; a < Math.PI * 2; a += Math.PI / 8) {
                    double x = Math.cos(a + angle) * RADIUS;
                    double z = Math.sin(a + angle) * RADIUS;
                    threadPoints.add(center.clone().add(x, 0, z));
                }

                // İplikleri çiz
                for (int i = 0; i < threadPoints.size(); i++) {
                    Location from = threadPoints.get(i);
                    Location to = threadPoints.get((i + 1) % threadPoints.size());
                    drawTimeLine(from, to);
                }

                // Hedefleri kontrol et ve bağla
                center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS).forEach(entity -> {
                    if (entity instanceof LivingEntity && entity != caster) {
                        if (!targets.contains(entity)) {
                            targets.add(entity);
                            bindEntity((LivingEntity) entity, center);
                        }
                    }
                });

                angle += Math.PI / 32;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawTimeLine(Location from, Location to) {
        Vector direction = to.clone().subtract(from).toVector();
        double distance = direction.length();
        direction.normalize();

        for (double d = 0; d < distance; d += 0.5) {
            Location point = from.clone().add(direction.clone().multiply(d));
            point.getWorld().spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                point,
                1, 0.1, 0.1, 0.1, 0
            );
        }
    }

    private void bindEntity(LivingEntity target, Location center) {
        Player caster = center.getWorld().getPlayers().stream()
            .filter(p -> boundEntities.containsKey(p.getUniqueId()) && 
                        centerLocations.get(p.getUniqueId()).equals(center))
            .findFirst()
            .orElse(null);

        new BukkitRunnable() {
            int ticks = 0;
            Location startLoc = target.getLocation().clone();
            
            @Override
            public void run() {
                if (!isActive || ticks++ >= 60 || !target.isValid()) {
                    this.cancel();
                    return;
                }

                // Bağlama efekti
                Location current = target.getLocation();
                Vector toCenter = center.clone().subtract(current).toVector();
                double distance = toCenter.length();

                if (distance > 2) {
                    toCenter.normalize().multiply(0.2);
                    target.setVelocity(target.getVelocity().add(toCenter));
                }

                // İplik efekti
                drawTimeLine(current.add(0, 1, 0), center);

                // Hasar
                if (ticks % 20 == 0) {
                    ((org.bukkit.entity.Damageable)target).damage(DAMAGE / 5, (Entity)null);
                    createDamageEffect(current);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void createDamageEffect(Location location) {
        location.getWorld().spawnParticle(
            Particle.SOUL,
            location.clone().add(0, 1, 0),
            20, 0.3, 0.3, 0.3, 0.1
        );
        
        playSound(location, Sound.ENTITY_VEX_HURT, 0.5f, 2.0f);
    }

    public void pullTargets(Player caster) {
        Set<Entity> targets = boundEntities.get(caster.getUniqueId());
        Location center = centerLocations.get(caster.getUniqueId());
        
        if (targets == null || center == null) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                targets.forEach(entity -> {
                    if (entity instanceof LivingEntity) {
                        LivingEntity target = (LivingEntity) entity;
                        Location loc = target.getLocation();
                        
                        Vector pull = center.clone().subtract(loc).toVector();
                        pull.normalize().multiply(2);
                        target.setVelocity(pull);
                        
                        createPullEffect(loc, center);
                    }
                });
            }
        }.runTask(plugin);
    }

    private void createPullEffect(Location from, Location to) {
        Vector direction = to.clone().subtract(from).toVector().normalize();
        
        for (double d = 0; d < from.distance(to); d += 0.5) {
            Location point = from.clone().add(direction.clone().multiply(d));
            point.getWorld().spawnParticle(
                Particle.SOUL,
                point,
                3, 0.1, 0.1, 0.1, 0.05
            );
        }
        
        playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
    }
} 
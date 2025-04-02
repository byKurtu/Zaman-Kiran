package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.*;

public class RealityShatter extends Skill {
    private static final double RADIUS = 5.0;
    private static final double DAMAGE = 10.0;
    private static final int DURATION = 10 * 20; // 10 saniye
    private final Map<Location, Set<Entity>> shatteredAreas = new HashMap<>();
    
    public RealityShatter(Plugin plugin) {
        super(plugin, "Reality Shatter", 20, 60);
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        Location target = caster.getTargetBlock(null, 30).getLocation();
        createShatterEffect(caster, target);
    }

    private void createShatterEffect(Player caster, Location center) {
        Set<Entity> affectedEntities = new HashSet<>();
        shatteredAreas.put(center, affectedEntities);

        // Ana patlama efekti
        center.getWorld().spawnParticle(
            Particle.WARPED_SPORE,
            center,
            100, 2, 2, 2, 0.1
        );
        
        playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 2.0f);

        // Sürekli efekt
        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;

            @Override
            public void run() {
                if (!isActive || ticks++ >= DURATION) {
                    shatteredAreas.remove(center);
                    this.cancel();
                    return;
                }

                // Parçalanma efekti
                for (int i = 0; i < 3; i++) {
                    double x = Math.cos(angle + (Math.PI * 2 * i / 3)) * RADIUS;
                    double z = Math.sin(angle + (Math.PI * 2 * i / 3)) * RADIUS;
                    Location particleLoc = center.clone().add(x, Math.sin(angle) * 2, z);
                    
                    center.getWorld().spawnParticle(
                        Particle.REVERSE_PORTAL,
                        particleLoc,
                        5, 0.2, 0.2, 0.2, 0.05
                    );
                }

                // Etkilenen varlıkları kontrol et
                center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS).forEach(entity -> {
                    if (entity instanceof LivingEntity && entity != caster) {
                        LivingEntity target = (LivingEntity) entity;
                        
                        if (!affectedEntities.contains(entity)) {
                            affectedEntities.add(entity);
                        }

                        // Yavaşlatma ve hasar
                        target.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOW,
                            40,
                            2,
                            false,
                            true
                        ));
                        
                        if (ticks % 20 == 0) { // Her saniye hasar
                            target.damage(DAMAGE, caster);
                            createDamageEffect(target.getLocation());
                        }
                    }
                });

                angle += Math.PI / 16;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void createDamageEffect(Location location) {
        location.getWorld().spawnParticle(
            Particle.CRIT_MAGIC,
            location.clone().add(0, 1, 0),
            15, 0.3, 0.3, 0.3, 0.2
        );
        
        playSound(location, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 2.0f);
    }

    public void mergeShatteredAreas(Player caster) {
        if (shatteredAreas.isEmpty()) return;

        // Merkez noktayı hesapla
        Location center = new Location(caster.getWorld(), 0, 0, 0);
        int count = 0;
        
        for (Location loc : shatteredAreas.keySet()) {
            center.add(loc);
            count++;
        }
        
        center.multiply(1.0 / count);

        // Büyük patlama efekti
        createMergeExplosion(caster, center);
        shatteredAreas.clear();
    }

    private void createMergeExplosion(Player caster, Location center) {
        new BukkitRunnable() {
            double radius = 0;
            
            @Override
            public void run() {
                if (radius >= RADIUS * 2) {
                    this.cancel();
                    return;
                }

                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = center.clone().add(x, radius / 2, z);
                    
                    center.getWorld().spawnParticle(
                        Particle.EXPLOSION_HUGE,
                        particleLoc,
                        1, 0, 0, 0, 0
                    );
                }

                // Hasar
                center.getWorld().getNearbyEntities(center, radius, radius, radius).forEach(entity -> {
                    if (entity instanceof LivingEntity && entity != caster) {
                        ((LivingEntity) entity).damage(DAMAGE * 2, caster);
                    }
                });

                playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                radius += 0.5;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
} 
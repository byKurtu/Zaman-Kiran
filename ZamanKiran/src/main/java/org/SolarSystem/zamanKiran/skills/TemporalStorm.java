package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.*;

public class TemporalStorm extends Skill {
    private static final double RADIUS = 6.0;
    private static final double DAMAGE = 12.0;
    private static final int DURATION = 15 * 20; // 15 saniye
    private final Map<UUID, Location> activeStorms = new HashMap<>();
    private final Random random = new Random();
    
    public TemporalStorm(Plugin plugin) {
        super(plugin, "Temporal Storm", 25, 70);
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        Location target = caster.getTargetBlock(null, 30).getLocation().add(0, 1, 0);
        createStorm(caster, target);
    }

    private void createStorm(Player caster, Location center) {
        activeStorms.put(caster.getUniqueId(), center);

        // Başlangıç efekti
        center.getWorld().strikeLightningEffect(center);
        playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;
            Set<Entity> affectedEntities = new HashSet<>();

            @Override
            public void run() {
                if (!isActive || ticks++ >= DURATION) {
                    activeStorms.remove(caster.getUniqueId());
                    this.cancel();
                    return;
                }

                // Fırtına efekti
                for (int i = 0; i < 5; i++) {
                    double height = Math.sin(angle + (Math.PI * 2 * i / 5)) * 3;
                    for (double a = 0; a < Math.PI * 2; a += Math.PI / 8) {
                        double x = Math.cos(a) * RADIUS;
                        double z = Math.sin(a) * RADIUS;
                        Location particleLoc = center.clone().add(x, height, z);
                        
                        center.getWorld().spawnParticle(
                            Particle.CLOUD,
                            particleLoc,
                            1, 0.2, 0.2, 0.2, 0
                        );
                    }
                }

                // Yıldırım efekti
                if (random.nextInt(20) == 0) {
                    double x = (random.nextDouble() - 0.5) * RADIUS * 2;
                    double z = (random.nextDouble() - 0.5) * RADIUS * 2;
                    Location strikeLocation = center.clone().add(x, 0, z);
                    strikeLocation.getWorld().strikeLightningEffect(strikeLocation);
                    
                    // Yakındaki düşmanlara hasar
                    strikeLocation.getWorld().getNearbyEntities(strikeLocation, 2, 2, 2).forEach(entity -> {
                        if (entity instanceof LivingEntity && entity != caster) {
                            ((LivingEntity) entity).damage(DAMAGE, caster);
                        }
                    });
                }

                // Etkilenen varlıkları kontrol et
                center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS).forEach(entity -> {
                    if (entity instanceof LivingEntity && entity != caster) {
                        LivingEntity target = (LivingEntity) entity;
                        
                        if (!affectedEntities.contains(entity)) {
                            affectedEntities.add(entity);
                            sendToDifferentTimeline(target);
                        }

                        // Merkeze doğru çek
                        Vector pull = center.toVector().subtract(target.getLocation().toVector());
                        pull.normalize().multiply(0.2);
                        target.setVelocity(target.getVelocity().add(pull));
                    }
                });

                // Ses efektleri
                if (ticks % 20 == 0) {
                    playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);
                }

                angle += Math.PI / 16;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void sendToDifferentTimeline(LivingEntity target) {
        // Zaman yolculuğu efekti
        Location loc = target.getLocation();
        
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.CONFUSION,
            100,
            1,
            false,
            true
        ));
        
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.BLINDNESS,
            20,
            0,
            false,
            true
        ));

        // Görsel efektler
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks++ >= 20) {
                    this.cancel();
                    return;
                }

                loc.getWorld().spawnParticle(
                    Particle.PORTAL,
                    loc.clone().add(0, 1, 0),
                    20, 0.5, 1, 0.5, 0.1
                );
                
                if (target instanceof Player) {
                    ((Player) target).playSound(
                        loc,
                        Sound.BLOCK_PORTAL_TRIGGER,
                        0.5f,
                        2.0f
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
} 
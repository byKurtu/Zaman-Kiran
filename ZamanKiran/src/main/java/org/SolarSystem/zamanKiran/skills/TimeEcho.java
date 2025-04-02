package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class TimeEcho extends Skill {
    private static final int RECORD_DURATION = 5 * 20; // 5 saniye
    private static final double DAMAGE = 15.0;
    private final Map<UUID, List<Location>> recordedLocations = new HashMap<>();
    private final Map<UUID, List<Float>> recordedRotations = new HashMap<>();
    
    public TimeEcho(Plugin plugin) {
        super(plugin, "Time Echo", 15, 50);
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        UUID playerId = caster.getUniqueId();
        
        // Kayıt başlat
        List<Location> locations = new ArrayList<>();
        List<Float> rotations = new ArrayList<>();
        recordedLocations.put(playerId, locations);
        recordedRotations.put(playerId, rotations);

        // Kayıt efekti
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!isActive || ticks >= RECORD_DURATION) {
                    this.cancel();
                    playEcho(caster, locations, rotations);
                    return;
                }

                // Konum ve rotasyon kaydet
                locations.add(caster.getLocation().clone());
                rotations.add(caster.getLocation().getYaw());

                // Kayıt efekti
                Location loc = caster.getLocation();
                loc.getWorld().spawnParticle(
                    Particle.DRAGON_BREATH,
                    loc.clone().add(0, 1, 0),
                    5, 0.2, 0.4, 0.2, 0.02
                );

                playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 2.0f);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void playEcho(Player caster, List<Location> locations, List<Float> rotations) {
        if (locations.isEmpty()) return;

        // Hayalet oluştur
        ArmorStand ghost = createArmorStand(locations.get(0), false, false);
        if (ghost == null) return;

        ghost.setGravity(false);
        ghost.setVisible(false);
        ghost.setSmall(false);
        ghost.setMarker(true);
        ghost.setCustomName("§b✧ Zaman Yankısı §b✧");
        ghost.setCustomNameVisible(true);
        ghost.setGlowing(true);

        // Hayalet efekti
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (!isActive || index >= locations.size()) {
                    ghost.remove();
                    this.cancel();
                    return;
                }

                Location loc = locations.get(index);
                float yaw = rotations.get(index);
                
                ghost.teleport(loc);
                ghost.setRotation(yaw, 0);

                // Hasar kontrolü
                loc.getWorld().getNearbyEntities(loc, 2, 2, 2).forEach(entity -> {
                    if (entity instanceof LivingEntity && entity != caster && entity != ghost) {
                        ((LivingEntity) entity).damage(DAMAGE, caster);
                        createHitEffect(entity.getLocation());
                    }
                });

                // Efektler
                loc.getWorld().spawnParticle(
                    Particle.SOUL,
                    loc.clone().add(0, 1, 0),
                    10, 0.2, 0.4, 0.2, 0.02
                );

                playSound(loc, Sound.ENTITY_VEX_AMBIENT, 0.5f, 1.5f);
                index++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void createHitEffect(Location location) {
        location.getWorld().spawnParticle(
            Particle.CRIT_MAGIC,
            location.clone().add(0, 1, 0),
            20, 0.3, 0.3, 0.3, 0.2
        );
        playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
    }
} 